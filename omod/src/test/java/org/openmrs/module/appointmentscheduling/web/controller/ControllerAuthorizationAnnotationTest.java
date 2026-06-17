package org.openmrs.module.appointmentscheduling.web.controller;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.Location;
import org.openmrs.Provider;
import org.openmrs.annotation.Authorized;
import org.openmrs.module.appointmentscheduling.Appointment;
import org.openmrs.module.appointmentscheduling.AppointmentRequisition;
import org.openmrs.module.appointmentscheduling.AppointmentUtils;

import java.lang.reflect.Method;
import java.util.Arrays;

public class ControllerAuthorizationAnnotationTest {

    @Test
    public void appointmentRequisitionController_shouldBeAuthorizedAtClassAndMethodLevel() throws Exception {
        Authorized classAuthorized = AppointmentRequisitionController.class.getAnnotation(Authorized.class);
        Assert.assertNotNull(classAuthorized);
        Assert.assertTrue(Arrays.asList(classAuthorized.value()).contains(AppointmentUtils.PRIV_SCHEDULE_APPOINTMENTS));

        Method createAppointment = AppointmentRequisitionController.class.getMethod("createAppointment", AppointmentRequisition.class);
        Authorized methodAuthorized = createAppointment.getAnnotation(Authorized.class);
        Assert.assertNotNull(methodAuthorized);
        Assert.assertTrue(Arrays.asList(methodAuthorized.value()).contains(AppointmentUtils.PRIV_SCHEDULE_APPOINTMENTS));
    }

    @Test
    public void appointmentDailyCountController_shouldBeAuthorizedAtClassAndMethodLevel() throws Exception {
        Authorized classAuthorized = AppointmentDailyCountController.class.getAnnotation(Authorized.class);
        Assert.assertNotNull(classAuthorized);
        Assert.assertTrue(Arrays.asList(classAuthorized.value()).contains(AppointmentUtils.PRIV_VIEW_APPOINTMENTS_STATISTICS));

        Method getDailyAggregates = AppointmentDailyCountController.class.getMethod(
                "getDailyAggregates",
                String.class,
                String.class,
                Location.class,
                Provider.class,
                Appointment.AppointmentStatus.class
        );
        Authorized methodAuthorized = getDailyAggregates.getAnnotation(Authorized.class);
        Assert.assertNotNull(methodAuthorized);
        Assert.assertTrue(Arrays.asList(methodAuthorized.value()).contains(AppointmentUtils.PRIV_VIEW_APPOINTMENTS_STATISTICS));
    }
}
