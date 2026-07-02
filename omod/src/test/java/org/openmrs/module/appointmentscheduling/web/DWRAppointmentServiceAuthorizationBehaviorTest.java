package org.openmrs.module.appointmentscheduling.web;

import org.junit.After;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ContextAuthenticationException;
import org.openmrs.api.context.UserContext;
import org.openmrs.module.appointmentscheduling.AppointmentUtils;

import static org.junit.Assert.fail;

/**
 * Behavioral counterpart to {@link DWRAppointmentServiceAuthorizationTest}: proves the
 * Context.requirePrivilege() calls actually deny/allow access, not just that the source
 * contains the right line.
 *
 * Uses a fake {@link UserContext} instead of a full Spring/database context, since
 * Context.requirePrivilege() only needs UserContext.hasPrivilege() to resolve - loading the
 * omod's real application context here would also drag in the webservices.rest module context,
 * which isn't fully wired for plain BaseModuleContextSensitiveTest in this module.
 */
public class DWRAppointmentServiceAuthorizationBehaviorTest {

	private static final String UNRELATED_PRIVILEGE = "Some Other Privilege";

	private static final String RANGE_START = "01/01/2020";

	private static final String RANGE_END = "01/01/2021";

	private final DWRAppointmentService dwrService = new DWRAppointmentService();

	@After
	public void tearDown() {
		Context.clearUserContext();
	}

	@Test(expected = ContextAuthenticationException.class)
	public void getPatientDescription_shouldRejectUserWithoutViewAppointmentsPrivilege() {
		grantOnly(UNRELATED_PRIVILEGE);
		dwrService.getPatientDescription(2);
	}

	@Test
	public void getPatientDescription_shouldNotRejectUserWithViewAppointmentsPrivilege() {
		grantOnly(AppointmentUtils.PRIV_VIEW_APPOINTMENTS);
		assertPrivilegeGateOpen(new Probe() {
			@Override
			public void run() {
				dwrService.getPatientDescription(2);
			}
		});
	}

	@Test(expected = ContextAuthenticationException.class)
	public void getAppointmentBlocksForCalendar_shouldRejectUserWithoutViewAppointmentBlocksPrivilege() throws Exception {
		grantOnly(UNRELATED_PRIVILEGE);
		dwrService.getAppointmentBlocksForCalendar(0L, 0L, null, null, null);
	}

	@Test
	public void getAppointmentBlocksForCalendar_shouldNotRejectUserWithViewAppointmentBlocksPrivilege() {
		grantOnly(AppointmentUtils.PRIV_VIEW_APPOINTMENT_BLOCKS);
		assertPrivilegeGateOpen(new Probe() {
			@Override
			public void run() throws Exception {
				dwrService.getAppointmentBlocksForCalendar(0L, 0L, null, null, null);
			}
		});
	}

	@Test(expected = ContextAuthenticationException.class)
	public void getAppointmentBlocks_shouldRejectUserWithoutViewAppointmentBlocksPrivilege() throws Exception {
		grantOnly(UNRELATED_PRIVILEGE);
		dwrService.getAppointmentBlocks("", "", null, null, null);
	}

	@Test
	public void getAppointmentBlocks_shouldNotRejectUserWithViewAppointmentBlocksPrivilege() {
		grantOnly(AppointmentUtils.PRIV_VIEW_APPOINTMENT_BLOCKS);
		assertPrivilegeGateOpen(new Probe() {
			@Override
			public void run() throws Exception {
				dwrService.getAppointmentBlocks("", "", null, null, null);
			}
		});
	}

	@Test(expected = ContextAuthenticationException.class)
	public void getAverageWaitingTimeByType_shouldRejectUserWithoutViewAppointmentsStatisticsPrivilege() throws Exception {
		grantOnly(UNRELATED_PRIVILEGE);
		dwrService.getAverageWaitingTimeByType(RANGE_START, RANGE_END);
	}

	@Test
	public void getAverageWaitingTimeByType_shouldNotRejectUserWithViewAppointmentsStatisticsPrivilege() {
		grantOnly(AppointmentUtils.PRIV_VIEW_APPOINTMENTS_STATISTICS);
		assertPrivilegeGateOpen(new Probe() {
			@Override
			public void run() throws Exception {
				dwrService.getAverageWaitingTimeByType(RANGE_START, RANGE_END);
			}
		});
	}

	@Test(expected = ContextAuthenticationException.class)
	public void getAverageConsultationTimeByType_shouldRejectUserWithoutViewAppointmentsStatisticsPrivilege() throws Exception {
		grantOnly(UNRELATED_PRIVILEGE);
		dwrService.getAverageConsultationTimeByType(RANGE_START, RANGE_END);
	}

	@Test
	public void getAverageConsultationTimeByType_shouldNotRejectUserWithViewAppointmentsStatisticsPrivilege() {
		grantOnly(AppointmentUtils.PRIV_VIEW_APPOINTMENTS_STATISTICS);
		assertPrivilegeGateOpen(new Probe() {
			@Override
			public void run() throws Exception {
				dwrService.getAverageConsultationTimeByType(RANGE_START, RANGE_END);
			}
		});
	}

	/**
	 * Installs a fake UserContext that reports having exactly one privilege, so the privilege
	 * check under test is the only thing that can make the difference between pass and fail.
	 */
	private void grantOnly(final String onlyGrantedPrivilege) {
		Context.setUserContext(new UserContext() {
			@Override
			public boolean hasPrivilege(String privilege) {
				return onlyGrantedPrivilege.equals(privilege);
			}
		});
	}

	/**
	 * Runs the probe and fails only if the privilege gate itself rejected the call. Anything
	 * past the gate (e.g. missing service beans, since there's no Spring context here) is
	 * irrelevant to what this test is checking. Expect a benign
	 * "serviceContext is null. Creating new ServiceContext()" ERROR line from OpenMRS core
	 * (Context.getServiceContext()) in the build log when these probes run - it's logged before
	 * the resulting APIException is thrown and swallowed below, and doesn't affect the outcome.
	 */
	private void assertPrivilegeGateOpen(Probe probe) {
		try {
			probe.run();
		} catch (ContextAuthenticationException e) {
			fail("User with the required privilege was rejected by the privilege check");
		} catch (Exception e) {
			// expected: no real service layer is wired up in this lightweight test
		}
	}

	private interface Probe {
		void run() throws Exception;
	}
}
