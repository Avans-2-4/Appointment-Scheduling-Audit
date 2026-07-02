package org.openmrs.module.appointmentscheduling.api;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Location;
import org.openmrs.Person;
import org.openmrs.PersonName;
import org.openmrs.Privilege;
import org.openmrs.Provider;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointmentscheduling.AppointmentType;
import org.openmrs.module.appointmentscheduling.AppointmentUtils;
import org.openmrs.module.appointmentscheduling.ProviderSchedule;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Regression test for the "View/Manage Provider Scedules" -> "View/Manage Provider Schedules"
 * privilege string fix in AppointmentUtils. Before the fix, no role could ever be granted the
 * privilege the @Authorized annotations actually checked for, because the seeded privilege in
 * config.xml has always been spelled correctly ("View/Manage Provider Schedules"). That made the
 * check effectively unsatisfiable for any real (non-superuser) account.
 *
 * The "should allow" tests grant SEEDED_VIEW/MANAGE_PROVIDER_SCHEDULES_PRIVILEGE - a literal
 * copy of what config.xml actually seeds - rather than AppointmentUtils.PRIV_VIEW/
 * MANAGE_PROVIDER_SCHEDULES. Granting the privilege the constant under test currently equals
 * would make the test pass no matter what that constant says, typo or not.
 *
 * These tests authenticate as a non-superuser, since AuthorizationAdvice is bypassed entirely for
 * super users (User.isSuperUser()) - testing with the default admin account would pass regardless
 * of whether the privilege string is correct.
 */
public class ProviderSchedulePrivilegeTest extends BaseModuleContextSensitiveTest {

	private static final String TEST_USER_SECRET = "Test12345678!";

	// Mirrors the <privilege> entries in omod/src/main/resources/config.xml, independently of
	// AppointmentUtils, so this test can't become tautological if that class regresses.
	private static final String SEEDED_VIEW_PROVIDER_SCHEDULES_PRIVILEGE = "View Provider Schedules";

	private static final String SEEDED_MANAGE_PROVIDER_SCHEDULES_PRIVILEGE = "Manage Provider Schedules";

	@Autowired
	private AppointmentService service;

	@Before
	public void before() throws Exception {
		executeDataSet("standardAppointmentTestDataset.xml");
	}

	@Test
	public void privilegeConstants_shouldMatchThePrivilegesSeededByConfigXml() {
		assertEquals(SEEDED_VIEW_PROVIDER_SCHEDULES_PRIVILEGE, AppointmentUtils.PRIV_VIEW_PROVIDER_SCHEDULES);
		assertEquals(SEEDED_MANAGE_PROVIDER_SCHEDULES_PRIVILEGE, AppointmentUtils.PRIV_MANAGE_PROVIDER_SCHEDULES);
	}

	@Test
	public void getAllProviderSchedules_shouldAllowUserGrantedTheSeededViewProviderSchedulesPrivilege() throws Exception {
		authenticateAsUserWithOnlyPrivilege(SEEDED_VIEW_PROVIDER_SCHEDULES_PRIVILEGE);

		service.getAllProviderSchedules(true);
	}

	@Test(expected = APIAuthenticationException.class)
	public void getAllProviderSchedules_shouldRejectUserWithoutViewProviderSchedulesPrivilege() throws Exception {
		authenticateAsUserWithOnlyPrivilege("Some Other Privilege");

		service.getAllProviderSchedules(true);
	}

	@Test
	public void saveProviderSchedule_shouldAllowUserGrantedTheSeededManageProviderSchedulesPrivilege() throws Exception {
		// built while still admin: fetching appointment types itself requires a separate privilege
		ProviderSchedule schedule = buildNewProviderSchedule();
		authenticateAsUserWithOnlyPrivilege(SEEDED_MANAGE_PROVIDER_SCHEDULES_PRIVILEGE);

		service.saveProviderSchedule(schedule);
	}

	@Test(expected = APIAuthenticationException.class)
	public void saveProviderSchedule_shouldRejectUserWithoutManageProviderSchedulesPrivilege() throws Exception {
		ProviderSchedule schedule = buildNewProviderSchedule();
		authenticateAsUserWithOnlyPrivilege("Some Other Privilege");

		service.saveProviderSchedule(schedule);
	}

	private ProviderSchedule buildNewProviderSchedule() throws Exception {
		Set<AppointmentType> appointmentTypes = service.getAllAppointmentTypes();
		Provider provider = Context.getProviderService().getProvider(1);
		return new ProviderSchedule(7,
				new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2019-05-05 00:00:00"),
				new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2119-05-05 00:00:00"),
				new Time(new SimpleDateFormat("HH:mm:ss").parse("07:00:00").getTime()),
				new Time(new SimpleDateFormat("HH:mm:ss").parse("10:00:00").getTime()),
				provider, new Location(1), appointmentTypes);
	}

	/**
	 * Creates and authenticates as a brand-new, non-superuser account whose only granted
	 * privilege is the given one. Runs as the default super user (already authenticated by
	 * BaseModuleContextSensitiveTest) to set this up.
	 */
	private void authenticateAsUserWithOnlyPrivilege(String privilegeName) {
		Privilege privilege = Context.getUserService().getPrivilege(privilegeName);
		if (privilege == null) {
			privilege = new Privilege(privilegeName, "test privilege");
			Context.getUserService().savePrivilege(privilege);
		}

		Role role = new Role("Role with only " + privilegeName, "test role");
		role.addPrivilege(privilege);
		Context.getUserService().saveRole(role);

		String username = "limited_" + System.nanoTime();
		User user = new User(new Person());
		user.setUsername(username);
		user.getPerson().setGender("M");
		user.addName(new PersonName("Test", "Limited", "User"));
		user.addRole(role);
		User savedUser = Context.getUserService().saveUser(user, TEST_USER_SECRET);
		assertNotNull(savedUser);

		Context.logout();
		Context.authenticate(username, TEST_USER_SECRET);
	}
}
