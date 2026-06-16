package org.openmrs.module.appointmentscheduling.audit;

import org.openmrs.User;
import org.openmrs.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Centralized audit logger for patient data read-access events.
 * Implements NEN-7510 compliant read-access logging with metadata-only capture.
 * 
 * Logs only patient UUID and metadata, NOT sensitive patient data (name, DOB, identifiers).
 * This follows privacy best practices and data minimization principles.
 */
@Component
public class AppointmentReadAuditLogger {
	
	private static final Logger audit = LoggerFactory.getLogger("org.openmrs.audit.PatientDataAccess");
	private static final SimpleDateFormat ISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	
	/**
	 * Log a patient data read-access event.
	 * 
	 * @param methodName the service method name that accessed the data (e.g., "getAppointmentsByConstraints")
	 * @param patient the Patient entity that was accessed (may be null if access was denied)
	 * @param user the authenticated User performing the access
	 * @param action the action being performed (e.g., "GET", "SEARCH")
	 * @param resource the resource type being accessed (e.g., "appointment")
	 */
	public void logPatientReadAccess(String methodName, Patient patient, User user, String action, String resource) {
		if (patient == null || user == null) {
			return; // Don't log if required data is missing
		}
		
		String patientUuid = patient.getUuid() != null ? patient.getUuid() : "UNKNOWN";
		String userUuid = user.getUuid() != null ? user.getUuid() : "UNKNOWN";
		String userName = user.getUsername() != null ? user.getUsername() : "UNKNOWN";
		String timestamp = ISO8601.format(new Date());
		
		// NEN-7510 compliant format: structured audit log with metadata only, no PII
		String auditLog = String.format(
			"[AUDIT] READ event_type=PATIENT_DATA_ACCESS method=%s patient_uuid=%s user_uuid=%s user_name=%s timestamp=%s action=%s resource=%s",
			methodName,
			patientUuid,
			userUuid,
			userName,
			timestamp,
			action,
			resource
		);
		
		audit.info(auditLog);
	}
	
}
