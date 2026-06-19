package org.openmrs.module.appointmentscheduling.web;

import org.junit.Assert;
import org.junit.Test;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DWRAppointmentServiceAuthorizationTest {

    private static final String SOURCE_PATH = "src/main/java/org/openmrs/module/appointmentscheduling/web/DWRAppointmentService.java";

    @Test
    public void getPatientDescription_shouldRequireViewAppointmentsPrivilege() throws Exception {
        String source = readSource();
        String methodBlock = getMethodBlock(source, "public PatientData getPatientDescription(Integer patientId)");

        Assert.assertTrue(methodBlock.contains("Context.requirePrivilege(AppointmentUtils.PRIV_VIEW_APPOINTMENTS);"));
    }

    @Test
    public void getAppointmentBlocksForCalendar_shouldRequireViewAppointmentBlocksPrivilege() throws Exception {
        String source = readSource();
        String methodBlock = getMethodBlock(source,
                "public List<AppointmentBlockData> getAppointmentBlocksForCalendar(Long fromDate, Long toDate, Integer locationId,");

        Assert.assertTrue(methodBlock.contains("Context.requirePrivilege(AppointmentUtils.PRIV_VIEW_APPOINTMENT_BLOCKS);"));
    }

    @Test
    public void getAppointmentBlocks_shouldRequireViewAppointmentBlocksPrivilege() throws Exception {
        String source = readSource();
        String methodBlock = getMethodBlock(source,
                "public List<AppointmentBlockData> getAppointmentBlocks(String fromDate, String toDate, Integer locationId,");

        Assert.assertTrue(methodBlock.contains("Context.requirePrivilege(AppointmentUtils.PRIV_VIEW_APPOINTMENT_BLOCKS);"));
    }

    @Test
    public void getAverageWaitingTimeByType_shouldRequireViewAppointmentsStatisticsPrivilege() throws Exception {
        String source = readSource();
        String methodBlock = getMethodBlock(source,
                "public Object[][] getAverageWaitingTimeByType(String fromDate, String toDate) throws ParseException");

        Assert.assertTrue(methodBlock.contains("Context.requirePrivilege(AppointmentUtils.PRIV_VIEW_APPOINTMENTS_STATISTICS);"));
    }

    @Test
    public void getAverageConsultationTimeByType_shouldRequireViewAppointmentsStatisticsPrivilege() throws Exception {
        String source = readSource();
        String methodBlock = getMethodBlock(source,
                "public Object[][] getAverageConsultationTimeByType(String fromDate, String toDate) throws ParseException");

        Assert.assertTrue(methodBlock.contains("Context.requirePrivilege(AppointmentUtils.PRIV_VIEW_APPOINTMENTS_STATISTICS);"));
    }

    private String readSource() throws IOException {
        return new String(Files.readAllBytes(Paths.get(SOURCE_PATH)), StandardCharsets.UTF_8);
    }

    private String getMethodBlock(String source, String methodSignaturePrefix) {
        int methodStart = source.indexOf(methodSignaturePrefix);
        Assert.assertTrue("Method signature not found: " + methodSignaturePrefix, methodStart >= 0);

        int nextMethodStart = source.indexOf("\n\tpublic ", methodStart + methodSignaturePrefix.length());
        if (nextMethodStart < 0) {
            nextMethodStart = source.length();
        }

        return source.substring(methodStart, nextMethodStart);
    }
}
