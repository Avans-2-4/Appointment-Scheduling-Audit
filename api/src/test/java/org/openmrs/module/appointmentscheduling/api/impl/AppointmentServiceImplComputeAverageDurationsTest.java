package org.openmrs.module.appointmentscheduling.api.impl;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Provider;
import org.openmrs.module.appointmentscheduling.Appointment;
import org.openmrs.module.appointmentscheduling.Appointment.AppointmentStatus;
import org.openmrs.module.appointmentscheduling.AppointmentBlock;
import org.openmrs.module.appointmentscheduling.AppointmentStatusHistory;
import org.openmrs.module.appointmentscheduling.AppointmentType;
import org.openmrs.module.appointmentscheduling.TimeSlot;
import org.openmrs.module.appointmentscheduling.api.db.AppointmentStatusHistoryDAO;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;

/**
 * Unit tests for the shared {@code computeAverageDurations} method extracted for MA-01
 * (see AppointmentServiceImpl#getAverageHistoryDurationByConditions /
 * #getAverageHistoryDurationByConditionsPerProvider). The method itself is private, so it is
 * exercised through its two public entry points, but the DAO is mocked so no Spring/Hibernate
 * context is involved - this isolates the shared aggregation logic rather than the DAO query.
 */
public class AppointmentServiceImplComputeAverageDurationsTest {

	private AppointmentServiceImpl service;

	private AppointmentStatusHistoryDAO appointmentStatusHistoryDAO;

	private AppointmentType typeA;

	private AppointmentType typeB;

	private Provider providerA;

	private Provider providerB;

	@Before
	public void setUp() {
		service = new AppointmentServiceImpl();
		appointmentStatusHistoryDAO = mock(AppointmentStatusHistoryDAO.class);
		service.setAppointmentStatusHistoryDAO(appointmentStatusHistoryDAO);

		typeA = new AppointmentType("Type A", "description", 30);
		typeB = new AppointmentType("Type B", "description", 30);
		providerA = new Provider(1);
		providerB = new Provider(2);
	}

	private Appointment appointmentOf(AppointmentType type, Provider provider) {
		AppointmentBlock block = new AppointmentBlock();
		block.setProvider(provider);

		TimeSlot timeSlot = new TimeSlot();
		timeSlot.setAppointmentBlock(block);

		Appointment appointment = new Appointment();
		appointment.setAppointmentType(type);
		appointment.setTimeSlot(timeSlot);
		return appointment;
	}

	private AppointmentStatusHistory historyOfMinutes(Appointment appointment, int minutes) {
		Date start = new Date(0);
		Date end = new Date(start.getTime() + minutes * 60000L);
		return new AppointmentStatusHistory(appointment, AppointmentStatus.COMPLETED, start, end);
	}

	@Test
	public void getAverageHistoryDurationByConditions_shouldAverageDurationsPerAppointmentType() {
		// identical durations per type keep the variance at 0, so confidenceInterval()
		// short-circuits to MIN/MAX and every entry is kept - deterministic without
		// needing to reason about the t-distribution cutoff
		List<AppointmentStatusHistory> histories = Arrays.asList(
				historyOfMinutes(appointmentOf(typeA, providerA), 10),
				historyOfMinutes(appointmentOf(typeA, providerB), 10),
				historyOfMinutes(appointmentOf(typeB, providerA), 20));
		when(appointmentStatusHistoryDAO.getHistoriesByInterval(
				(Date) anyObject(), (Date) anyObject(), (AppointmentStatus) anyObject())).thenReturn(histories);

		Map<AppointmentType, Double> result = service.getAverageHistoryDurationByConditions(
				new Date(), new Date(), AppointmentStatus.COMPLETED);

		assertEquals(10.0, result.get(typeA), 0.0001);
		assertEquals(20.0, result.get(typeB), 0.0001);
	}

	@Test
	public void getAverageHistoryDurationByConditionsPerProvider_shouldAverageDurationsPerProvider() {
		List<AppointmentStatusHistory> histories = Arrays.asList(
				historyOfMinutes(appointmentOf(typeA, providerA), 10),
				historyOfMinutes(appointmentOf(typeB, providerA), 10),
				historyOfMinutes(appointmentOf(typeA, providerB), 20));
		when(appointmentStatusHistoryDAO.getHistoriesByInterval(
				(Date) anyObject(), (Date) anyObject(), (AppointmentStatus) anyObject())).thenReturn(histories);

		Map<Provider, Double> result = service.getAverageHistoryDurationByConditionsPerProvider(
				new Date(), new Date(), AppointmentStatus.COMPLETED);

		assertEquals(10.0, result.get(providerA), 0.0001);
		assertEquals(20.0, result.get(providerB), 0.0001);
	}

	@Test
	public void getAverageHistoryDurationByConditions_shouldReturnEmptyMapWhenNoHistoriesExist() {
		when(appointmentStatusHistoryDAO.getHistoriesByInterval(
				(Date) anyObject(), (Date) anyObject(), (AppointmentStatus) anyObject()))
				.thenReturn(Collections.<AppointmentStatusHistory> emptyList());

		Map<AppointmentType, Double> result = service.getAverageHistoryDurationByConditions(
				new Date(), new Date(), AppointmentStatus.COMPLETED);

		assertEquals(0, result.size());
	}
}
