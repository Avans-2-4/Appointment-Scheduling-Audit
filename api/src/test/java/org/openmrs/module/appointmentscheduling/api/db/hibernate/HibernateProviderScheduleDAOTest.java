package org.openmrs.module.appointmentscheduling.api.db.hibernate;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Pure unit test (no Spring/Hibernate context) for the private {@code isSpecificTime(Date)}
 * method MA-05 extracted in {@code HibernateProviderScheduleDAO} to remove the duplicated
 * midnight-check condition previously inlined in getProviderScheduleByConstraints(). Called
 * directly via reflection since the method is private and has no public entry point that
 * isolates it from the DB-backed query logic around it.
 */
public class HibernateProviderScheduleDAOTest {

	private HibernateProviderScheduleDAO dao;

	private Method isSpecificTime;

	@Before
	public void setUp() throws Exception {
		dao = new HibernateProviderScheduleDAO();
		isSpecificTime = HibernateProviderScheduleDAO.class.getDeclaredMethod("isSpecificTime", Date.class);
		isSpecificTime.setAccessible(true);
	}

	private boolean isSpecificTime(Date date) throws Exception {
		return (Boolean) isSpecificTime.invoke(dao, date);
	}

	@Test
	public void isSpecificTime_shouldReturnFalseWhenDateIsNull() throws Exception {
		assertFalse(isSpecificTime(null));
	}

	@Test
	public void isSpecificTime_shouldReturnFalseWhenTimeIsExactlyMidnight() throws Exception {
		Date midnight = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2020-01-02 00:00:00");

		assertFalse(isSpecificTime(midnight));
	}

	@Test
	public void isSpecificTime_shouldReturnTrueWhenTimeIsNotMidnight() throws Exception {
		Date notMidnight = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2020-01-02 08:00:00");

		assertTrue(isSpecificTime(notMidnight));
	}

	@Test
	public void isSpecificTime_shouldReturnTrueWhenTimeIsOneSecondAfterMidnight() throws Exception {
		Date almostMidnight = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2020-01-02 00:00:01");

		assertTrue(isSpecificTime(almostMidnight));
	}
}
