package org.openmrs.module.appointmentscheduling.audit;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.module.appointmentscheduling.Appointment;
import org.springframework.test.util.ReflectionTestUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AppointmentReadAccessAspect behavior.
 */
public class AppointmentReadAccessAspectTest {

	@Test
	public void shouldLogPatientFromArgumentsAndReturnOriginalResult() throws Throwable {
		AppointmentReadAuditLogger auditLogger = mock(AppointmentReadAuditLogger.class);
		User user = buildUser("user-1", "auditor");
		AppointmentReadAccessAspect aspect = aspectWith(user, auditLogger);

		Patient patient = buildPatient("patient-1", 11);
		Object result = new Object();
		ProceedingJoinPoint joinPoint = mockJoinPoint("getScheduledAppointmentsForPatient", new Object[] { patient }, result);

		Object actual = aspect.logReadAccess(joinPoint);

		assertSame(result, actual);
		InOrder order = inOrder(joinPoint, auditLogger);
		order.verify(joinPoint).proceed();
		order.verify(auditLogger).logPatientReadAccess("getScheduledAppointmentsForPatient", patient, user, "GET", "appointment");
	}

	@Test
	public void shouldLogPatientFromReturnedAppointmentWhenArgsDoNotContainPatient() throws Throwable {
		AppointmentReadAuditLogger auditLogger = mock(AppointmentReadAuditLogger.class);
		User user = buildUser("user-2", "auditor-2");
		AppointmentReadAccessAspect aspect = aspectWith(user, auditLogger);

		Patient patient = buildPatient("patient-2", 12);
		Appointment appointment = new Appointment();
		appointment.setPatient(patient);
		ProceedingJoinPoint joinPoint = mockJoinPoint("getAppointment", new Object[] { Integer.valueOf(5) }, appointment);

		Object actual = aspect.logReadAccess(joinPoint);

		assertSame(appointment, actual);
		verify(auditLogger).logPatientReadAccess("getAppointment", patient, user, "GET", "appointment");
	}

	@Test
	public void shouldLogEachDistinctPatientOnlyOnceFromCollectionResult() throws Throwable {
		AppointmentReadAuditLogger auditLogger = mock(AppointmentReadAuditLogger.class);
		User user = buildUser("user-3", "auditor-3");
		AppointmentReadAccessAspect aspect = aspectWith(user, auditLogger);

		Patient patient1a = buildPatient("patient-3", 13);
		Patient patient1b = buildPatient("patient-3", 13);
		Patient patient2 = buildPatient("patient-4", 14);

		Appointment appt1 = new Appointment();
		appt1.setPatient(patient1a);
		Appointment appt2 = new Appointment();
		appt2.setPatient(patient1b);
		Appointment appt3 = new Appointment();
		appt3.setPatient(patient2);

		List<Appointment> result = Arrays.asList(appt1, appt2, appt3);
		ProceedingJoinPoint joinPoint = mockJoinPoint("getAppointmentsByConstraints", new Object[] { null }, result);

		aspect.logReadAccess(joinPoint);

		ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
		verify(auditLogger, times(2)).logPatientReadAccess(eq("getAppointmentsByConstraints"), patientCaptor.capture(), eq(user), eq("GET"), eq("appointment"));
		List<Patient> loggedPatients = patientCaptor.getAllValues();
		assertEquals(2, loggedPatients.size());
		assertEquals("patient-3", loggedPatients.get(0).getUuid());
		assertEquals("patient-4", loggedPatients.get(1).getUuid());
	}

	@Test
	public void shouldNotLogWhenNoAuthenticatedUser() throws Throwable {
		AppointmentReadAuditLogger auditLogger = mock(AppointmentReadAuditLogger.class);
		AppointmentReadAccessAspect aspect = aspectWith(null, auditLogger);

		Patient patient = buildPatient("patient-5", 15);
		ProceedingJoinPoint joinPoint = mockJoinPoint("getLastAppointment", new Object[] { patient }, new Appointment());

		aspect.logReadAccess(joinPoint);

		verifyZeroInteractions(auditLogger);
	}

	@Test
	public void shouldNotFailBusinessMethodWhenAuditLoggerThrows() throws Throwable {
		AppointmentReadAuditLogger auditLogger = mock(AppointmentReadAuditLogger.class);
		doThrow(new RuntimeException("audit write failed")).when(auditLogger)
				.logPatientReadAccess(any(String.class), any(Patient.class), any(User.class), any(String.class), any(String.class));

		User user = buildUser("user-6", "auditor-6");
		AppointmentReadAccessAspect aspect = aspectWith(user, auditLogger);

		Patient patient = buildPatient("patient-6", 16);
		Object result = new Object();
		ProceedingJoinPoint joinPoint = mockJoinPoint("getScheduledAppointmentsForPatient", new Object[] { patient }, result);

		Object actual = aspect.logReadAccess(joinPoint);

		assertSame(result, actual);
		verify(joinPoint).proceed();
	}

	@Test(expected = IllegalStateException.class)
	public void shouldPropagateBusinessExceptionAndSkipAuditWhenProceedFails() throws Throwable {
		AppointmentReadAuditLogger auditLogger = mock(AppointmentReadAuditLogger.class);
		User user = buildUser("user-7", "auditor-7");
		AppointmentReadAccessAspect aspect = aspectWith(user, auditLogger);

		Patient patient = buildPatient("patient-7", 17);
		ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
		when(joinPoint.proceed()).thenThrow(new IllegalStateException("service failed"));
		when(joinPoint.getArgs()).thenReturn(new Object[] { patient });

		try {
			aspect.logReadAccess(joinPoint);
		} finally {
			verifyZeroInteractions(auditLogger);
		}
	}

	private AppointmentReadAccessAspect aspectWith(final User user, AppointmentReadAuditLogger auditLogger) {
		AppointmentReadAccessAspect aspect = new AppointmentReadAccessAspect() {
			@Override
			protected User getAuthenticatedUser() {
				return user;
			}
		};

		ReflectionTestUtils.setField(aspect, "auditLogger", auditLogger);
		return aspect;
	}

	private ProceedingJoinPoint mockJoinPoint(String methodName, Object[] args, Object result) throws Throwable {
		ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
		Signature signature = mock(Signature.class);

		when(joinPoint.proceed()).thenReturn(result);
		when(joinPoint.getArgs()).thenReturn(args);
		when(joinPoint.getSignature()).thenReturn(signature);
		when(signature.getName()).thenReturn(methodName);

		return joinPoint;
	}

	private User buildUser(String uuid, String username) {
		User user = new User();
		user.setUuid(uuid);
		user.setUsername(username);
		return user;
	}

	private Patient buildPatient(String uuid, int id) {
		Patient patient = new Patient();
		patient.setUuid(uuid);
		patient.setPatientId(id);
		return patient;
	}
}
