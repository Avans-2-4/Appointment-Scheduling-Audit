package org.openmrs.module.appointmentscheduling.rest.resource.openmrs1_9;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.appointmentscheduling.AppointmentRequest;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Pure unit test (no Spring/Hibernate context) for the private {@code buildWriteDescription()}
 * method MA-04 extracted in {@code AppointmentRequestResource1_9} to remove the duplicated
 * 11-line body previously shared by getCreatableProperties() and getUpdatableProperties().
 */
public class AppointmentRequestResource1_9WriteDescriptionTest {

	private AppointmentRequestResource1_9 resource;

	@Before
	public void setUp() {
		resource = new AppointmentRequestResource1_9();
	}

	@Test
	public void getCreatableProperties_shouldReturnSamePropertiesAsGetUpdatableProperties() {
		Set<String> creatable = resource.getCreatableProperties().getProperties().keySet();
		Set<String> updatable = resource.getUpdatableProperties().getProperties().keySet();

		assertEquals(creatable, updatable);
	}

	@Test
	public void buildWriteDescription_shouldMarkPatientAppointmentTypeRequestedOnAndStatusAsRequired() {
		DelegatingResourceDescription description = resource.getCreatableProperties();

		assertTrue(description.getProperties().get(AppointmentRequest.FIELD_PATIENT).isRequired());
		assertTrue(description.getProperties().get(AppointmentRequest.FIELD_APPOINTMENT_TYPE).isRequired());
		assertTrue(description.getProperties().get(AppointmentRequest.FIELD_REQUESTED_ON).isRequired());
		assertTrue(description.getProperties().get(AppointmentRequest.FIELD_STATUS).isRequired());
	}

	@Test
	public void buildWriteDescription_shouldMarkProviderAndNotesAsOptional() {
		DelegatingResourceDescription description = resource.getCreatableProperties();

		assertFalse(description.getProperties().get(AppointmentRequest.FIELD_PROVIDER).isRequired());
		assertFalse(description.getProperties().get(AppointmentRequest.FIELD_NOTES).isRequired());
	}
}
