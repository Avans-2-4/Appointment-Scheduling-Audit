package org.openmrs.module.appointmentscheduling.api.impl;

import org.openmrs.module.appointmentscheduling.Appointment;

public class LateArrivalPredicate implements AppointmentTimingPredicate {
	@Override
	public boolean test(Appointment appointment) {
		return appointment.getVisit().getStartDatetime().after(appointment.getTimeSlot().getEndDate());
	}
}
