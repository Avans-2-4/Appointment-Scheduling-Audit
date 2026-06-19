package org.openmrs.module.appointmentscheduling.web.controller;

import org.openmrs.Location;
import org.openmrs.Provider;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointmentscheduling.Appointment;
import org.openmrs.module.appointmentscheduling.AppointmentDailyCount;
import org.openmrs.module.appointmentscheduling.AppointmentUtils;
import org.openmrs.module.appointmentscheduling.api.AppointmentService;
import org.openmrs.module.appointmentscheduling.rest.controller.AppointmentRestController;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * REST Controller for retrieving daily appointment count aggregates.
 * 
 * SECURITY ARCHITECTURE: Defense in Depth
 * - Level 1 (Endpoint): @Authorized annotation enforces privilege at controller entry point
 * - Level 2 (Service): Service layer methods have additional @Authorized annotations
 * - This prevents privilege bypass if service delegation is compromised.
 * 
 * NEN-7510-2: Requirement 3.2 - Authorization is enforced at multiple layers.
 * Requirement 5.1 - Sensitive operational statistics are restricted by privilege.
 */
@Controller
@RequestMapping("/rest/" + RestConstants.VERSION_1 + AppointmentRestController.APPOINTMENT_SCHEDULING_REST_NAMESPACE +"/dailyappointmentcount")
@Authorized(AppointmentUtils.PRIV_VIEW_APPOINTMENTS_STATISTICS)
public class AppointmentDailyCountController {

    /**
     * Retrieves daily appointment count aggregates for the specified date range.
     * 
     * SECURITY: Method-level @Authorized provides secondary authorization check
     * and documents the specific operation requiring View Appointments Statistics privilege.
     * 
     * @param fromDate required start date for aggregation range
     * @param toDate required end date for aggregation range
     * @param location optional location filter
     * @param provider optional provider filter
     * @param status optional appointment status filter
     * @return list of daily appointment count aggregates
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    @Authorized(AppointmentUtils.PRIV_VIEW_APPOINTMENTS_STATISTICS)
    public List<AppointmentDailyCount> getDailyAggregates(
            @RequestParam(value = "fromDate", required = true) String fromDate,
            @RequestParam(value = "toDate", required = true) String toDate,
            @RequestParam(value = "location", required = false) Location location,
            @RequestParam(value = "provider", required = false) Provider provider,
            @RequestParam(value = "status", required = false) Appointment.AppointmentStatus status
    )
    {
        List<AppointmentDailyCount> dailyCounts = Context.getService(AppointmentService.class)
                .getAppointmentDailyCount(fromDate, toDate, location, provider, status);
        return dailyCounts;
    }

}