package org.openmrs.module.appointmentscheduling.audit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointmentscheduling.Appointment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AOP aspect for capturing patient data read-access events.
 * Intercepts service layer methods to log all patient data access attempts.
 * 
 * Covers all access layers:
 * - REST API (/rest/v1/appointmentscheduling/*)
 * - Legacy MVC Controllers (/module/appointmentscheduling/*)
 * - DWR backend services
 * 
 * Logs after successful service execution to capture the accessed patient identity.
 */
@Aspect
@Component
public class AppointmentReadAccessAspect {
	
	@Autowired
	private AppointmentReadAuditLogger auditLogger;
	
	/**
	 * Pointcut for getAppointmentsByConstraints method (all overloads).
	 * This is the primary search method used across all access layers.
	 */
	@Pointcut("execution(* org.openmrs.module.appointmentscheduling.api.AppointmentService.getAppointmentsByConstraints(..))")
	public void getAppointmentsByConstraints() {}
	
	/**
	 * Pointcut for getScheduledAppointmentsForPatient.
	 * Directly queries appointments for a specific patient.
	 */
	@Pointcut("execution(* org.openmrs.module.appointmentscheduling.api.AppointmentService.getScheduledAppointmentsForPatient(..))")
	public void getScheduledAppointmentsForPatient() {}
	
	/**
	 * Pointcut for getTimeSlotsByConstraints (all overloads).
	 * Used for searching available time slots with optional patient filter.
	 */
	@Pointcut("execution(* org.openmrs.module.appointmentscheduling.api.AppointmentService.getTimeSlotsByConstraints(..))")
	public void getTimeSlotsByConstraints() {}
	
	/**
	 * Pointcut for getAppointmentRequestsByConstraints.
	 * Searches appointment requests with patient filter.
	 */
	@Pointcut("execution(* org.openmrs.module.appointmentscheduling.api.AppointmentService.getAppointmentRequestsByConstraints(..))")
	public void getAppointmentRequestsByConstraints() {}
	
	/**
	 * Pointcut for getLastAppointment.
	 * Retrieves last appointment for a patient.
	 */
	@Pointcut("execution(* org.openmrs.module.appointmentscheduling.api.AppointmentService.getLastAppointment(..))")
	public void getLastAppointment() {}
	
	/**
	 * Pointcut for getAppointment by id.
	 * Single appointment lookup (may retrieve patient data).
	 */
	@Pointcut("execution(* org.openmrs.module.appointmentscheduling.api.AppointmentService.getAppointment(..))")
	public void getAppointment() {}
	
	/**
	 * Combined pointcut for all read-access methods.
	 */
	@Pointcut("getAppointmentsByConstraints() || getScheduledAppointmentsForPatient() || " +
	          "getTimeSlotsByConstraints() || getAppointmentRequestsByConstraints() || " +
	          "getLastAppointment() || getAppointment()")
	public void allReadMethods() {}
	
	/**
	 * Advice around read-access methods.
	 * Logs patient data access after successful method execution.
	 */
	@Around("allReadMethods()")
	public Object logReadAccess(ProceedingJoinPoint joinPoint) throws Throwable {
		Object result = joinPoint.proceed();

		try {
			User authenticatedUser = getAuthenticatedUser();
			String methodName = joinPoint.getSignature().getName();

			Map<String, Patient> patients = extractPatients(joinPoint.getArgs(), result);
			if (authenticatedUser != null) {
				for (Patient patient : patients.values()) {
					auditLogger.logPatientReadAccess(methodName, patient, authenticatedUser, "GET", "appointment");
				}
			}
			
		} catch (Exception e) {
			// Silently fail - don't let audit logging break business logic
			// Logging may fail if Context is not available or user is not authenticated
		}
		
		return result;
	}

	protected User getAuthenticatedUser() {
		return Context.getAuthenticatedUser();
	}
	
	/**
	 * Extract accessed patients from method arguments and return value.
	 * Uses UUID-keyed map to avoid duplicate logs for the same patient in one invocation.
	 * 
	 * @param args method arguments to search
	 * @param result return value from intercepted method
	 * @return map of unique patients keyed by UUID
	 */
	private Map<String, Patient> extractPatients(Object[] args, Object result) {
		Map<String, Patient> patients = new LinkedHashMap<String, Patient>();

		if (args != null && args.length > 0) {
			for (Object arg : args) {
				addPatientIfPresent(patients, arg);
			}
		}

		addPatientIfPresent(patients, result);

		return patients;
	}

	private void addPatientIfPresent(Map<String, Patient> patients, Object value) {
		if (value instanceof Patient) {
			Patient patient = (Patient) value;
			String key = patient.getUuid() != null ? patient.getUuid() : "id:" + String.valueOf(patient.getPatientId());
			patients.put(key, patient);
			return;
		}

		if (value instanceof Appointment) {
			Appointment appointment = (Appointment) value;
			addPatientIfPresent(patients, appointment.getPatient());
			return;
		}

		if (value instanceof Collection) {
			for (Object item : (Collection) value) {
				addPatientIfPresent(patients, item);
			}
		}
	}
}
