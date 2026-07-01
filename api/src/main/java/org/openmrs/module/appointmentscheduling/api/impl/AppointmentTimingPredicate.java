package org.openmrs.module.appointmentscheduling.api.impl;

import org.openmrs.module.appointmentscheduling.Appointment;

public interface AppointmentTimingPredicate {
	boolean test(Appointment appointment);
}
