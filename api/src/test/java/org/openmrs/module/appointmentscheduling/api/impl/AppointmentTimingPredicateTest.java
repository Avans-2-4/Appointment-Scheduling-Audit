package org.openmrs.module.appointmentscheduling.api.impl;

import org.junit.Test;
import org.openmrs.Visit;
import org.openmrs.module.appointmentscheduling.Appointment;
import org.openmrs.module.appointmentscheduling.TimeSlot;

import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AppointmentTimingPredicateTest {

	private static final long SLOT_END_MILLIS = 1_000_000L;

	private Appointment appointmentWithVisitStart(long visitStartMillis) {
		TimeSlot timeSlot = new TimeSlot();
		timeSlot.setEndDate(new Date(SLOT_END_MILLIS));

		Visit visit = new Visit();
		visit.setStartDatetime(new Date(visitStartMillis));

		Appointment appointment = new Appointment();
		appointment.setTimeSlot(timeSlot);
		appointment.setVisit(visit);
		return appointment;
	}

	@Test
	public void earlyArrivalPredicate_shouldReturnTrueWhenVisitStartsBeforeSlotEnd() {
		Appointment appointment = appointmentWithVisitStart(SLOT_END_MILLIS - 1);

		assertTrue(new EarlyArrivalPredicate().test(appointment));
	}

	@Test
	public void earlyArrivalPredicate_shouldReturnFalseWhenVisitStartsAtSlotEnd() {
		Appointment appointment = appointmentWithVisitStart(SLOT_END_MILLIS);

		assertFalse(new EarlyArrivalPredicate().test(appointment));
	}

	@Test
	public void earlyArrivalPredicate_shouldReturnFalseWhenVisitStartsAfterSlotEnd() {
		Appointment appointment = appointmentWithVisitStart(SLOT_END_MILLIS + 1);

		assertFalse(new EarlyArrivalPredicate().test(appointment));
	}

	@Test
	public void lateArrivalPredicate_shouldReturnTrueWhenVisitStartsAfterSlotEnd() {
		Appointment appointment = appointmentWithVisitStart(SLOT_END_MILLIS + 1);

		assertTrue(new LateArrivalPredicate().test(appointment));
	}

	@Test
	public void lateArrivalPredicate_shouldReturnFalseWhenVisitStartsAtSlotEnd() {
		Appointment appointment = appointmentWithVisitStart(SLOT_END_MILLIS);

		assertFalse(new LateArrivalPredicate().test(appointment));
	}

	@Test
	public void lateArrivalPredicate_shouldReturnFalseWhenVisitStartsBeforeSlotEnd() {
		Appointment appointment = appointmentWithVisitStart(SLOT_END_MILLIS - 1);

		assertFalse(new LateArrivalPredicate().test(appointment));
	}
}
