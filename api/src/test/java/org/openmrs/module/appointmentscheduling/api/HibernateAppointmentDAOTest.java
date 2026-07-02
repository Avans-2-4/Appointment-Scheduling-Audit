/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.appointmentscheduling.api;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.appointmentscheduling.Appointment;
import org.openmrs.module.appointmentscheduling.AppointmentBlock;
import org.openmrs.module.appointmentscheduling.AppointmentType;
import org.openmrs.module.appointmentscheduling.api.db.AppointmentDAO;
import org.openmrs.module.appointmentscheduling.api.db.hibernate.HibernateAppointmentDAO;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link AppointmentDAO#getAppointmentsByAppointmentBlockAndAppointmentTypes(AppointmentBlock, List)}
 * directly on {@link HibernateAppointmentDAO}. This method is not exposed on
 * {@link AppointmentService}, so the DAO is wired up here the same way
 * moduleApplicationContext.xml wires it for the service.
 */
public class HibernateAppointmentDAOTest extends BaseModuleContextSensitiveTest {

	private AppointmentService service;

	private AppointmentDAO dao;

	@Before
	public void before() throws Exception {
		service = Context.getService(AppointmentService.class);

		HibernateAppointmentDAO hibernateAppointmentDAO = new HibernateAppointmentDAO();
		hibernateAppointmentDAO.setSessionFactory(Context.getRegisteredComponent("dbSessionFactory", DbSessionFactory.class));
		dao = hibernateAppointmentDAO;

		executeDataSet("standardAppointmentTestDataset.xml");
	}

	@Test
	@Verifies(value = "should get only non-voided appointments of the given type in the given block", method = "getAppointmentsByAppointmentBlockAndAppointmentTypes(AppointmentBlock, List)")
	public void getAppointmentsByAppointmentBlockAndAppointmentTypes_shouldFilterByAppointmentType() {
		AppointmentBlock appointmentBlock = service.getAppointmentBlock(1);
		assertNotNull(appointmentBlock);

		AppointmentType appointmentType = service.getAppointmentType(1);
		assertNotNull(appointmentType);

		List<Appointment> appointments = dao.getAppointmentsByAppointmentBlockAndAppointmentTypes(
				appointmentBlock, Arrays.asList(appointmentType));

		assertEquals(2, appointments.size());
		for (Appointment appointment : appointments) {
			assertEquals(appointmentType, appointment.getAppointmentType());
			assertTrue(appointment.getTimeSlot().getAppointmentBlock().equals(appointmentBlock));
			assertEquals(false, appointment.getVoided());
		}
	}

	@Test
	@Verifies(value = "should get all non-voided appointments of a different type in the same block", method = "getAppointmentsByAppointmentBlockAndAppointmentTypes(AppointmentBlock, List)")
	public void getAppointmentsByAppointmentBlockAndAppointmentTypes_shouldFilterByAnotherAppointmentType() {
		AppointmentBlock appointmentBlock = service.getAppointmentBlock(1);
		assertNotNull(appointmentBlock);

		AppointmentType appointmentType = service.getAppointmentType(3);
		assertNotNull(appointmentType);

		List<Appointment> appointments = dao.getAppointmentsByAppointmentBlockAndAppointmentTypes(
				appointmentBlock, Arrays.asList(appointmentType));

		assertEquals(5, appointments.size());
	}

	@Test
	@Verifies(value = "should get all non-voided appointments in the block when no appointment types are given", method = "getAppointmentsByAppointmentBlockAndAppointmentTypes(AppointmentBlock, List)")
	public void getAppointmentsByAppointmentBlockAndAppointmentTypes_shouldReturnAllTypesWhenAppointmentTypesIsNull() {
		AppointmentBlock appointmentBlock = service.getAppointmentBlock(1);
		assertNotNull(appointmentBlock);

		List<Appointment> appointments = dao.getAppointmentsByAppointmentBlockAndAppointmentTypes(appointmentBlock, null);

		assertEquals(7, appointments.size());
	}

	@Test
	@Verifies(value = "should not include appointments from a different appointment block", method = "getAppointmentsByAppointmentBlockAndAppointmentTypes(AppointmentBlock, List)")
	public void getAppointmentsByAppointmentBlockAndAppointmentTypes_shouldScopeResultsToGivenBlock() {
		AppointmentBlock appointmentBlock = service.getAppointmentBlock(4);
		assertNotNull(appointmentBlock);

		AppointmentType appointmentType = service.getAppointmentType(1);
		assertNotNull(appointmentType);

		List<Appointment> appointments = dao.getAppointmentsByAppointmentBlockAndAppointmentTypes(
				appointmentBlock, Arrays.asList(appointmentType));

		assertEquals(4, appointments.size());
		for (Appointment appointment : appointments) {
			assertTrue(appointment.getTimeSlot().getAppointmentBlock().equals(appointmentBlock));
		}
	}
}
