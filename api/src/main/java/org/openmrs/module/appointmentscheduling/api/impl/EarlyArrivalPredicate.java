package org.openmrs.module.appointmentscheduling.api.impl;

import org.openmrs.module.appointmentscheduling.Appointment;

public class EarlyArrivalPredicate implements AppointmentTimingPredicate {
	@Override
	public boolean test(Appointment appointment) {
		return appointment.getVisit().getStartDatetime().before(appointment.getTimeSlot().getEndDate());
	}
}
