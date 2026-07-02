package org.openmrs.module.appointmentscheduling.rest.resource.openmrs1_9;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.appointmentscheduling.AppointmentRequest;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;

import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the private {@code addSharedProperties()} method MA-04 extracted in
 * {@code AppointmentRequestResource1_9} to remove duplication between the Default and Full
 * branches of getRepresentationDescription(). Needs a real OpenMRS context because
 * {@code RestConstants}'s static initializer calls {@code Context.getAdministrationService()}.
 */
public class AppointmentRequestResource1_9SharedPropertiesTest extends BaseModuleWebContextSensitiveTest {

	private AppointmentRequestResource1_9 resource;

	@Before
	public void setUp() {
		resource = new AppointmentRequestResource1_9();
	}

	@Test
	public void addSharedProperties_shouldAddIdenticalScalarPropertiesToBothDefaultAndFullRepresentation() {
		Set<String> defaultProps = resource.getRepresentationDescription(new DefaultRepresentation())
				.getProperties().keySet();
		Set<String> fullProps = resource.getRepresentationDescription(new FullRepresentation())
				.getProperties().keySet();

		String[] sharedScalarProperties = { AppointmentRequest.FIELD_REQUESTED_ON, AppointmentRequest.FIELD_STATUS,
				AppointmentRequest.FIELD_MIN_TIME_FRAME_VALUE, AppointmentRequest.FIELD_MIN_TIME_FRAME_UNITS,
				AppointmentRequest.FIELD_MAX_TIME_FRAME_VALUE, AppointmentRequest.FIELD_MAX_TIME_FRAME_UNITS,
				AppointmentRequest.FIELD_NOTES, "voided" };

		for (String property : sharedScalarProperties) {
			assertTrue("Default representation missing " + property, defaultProps.contains(property));
			assertTrue("Full representation missing " + property, fullProps.contains(property));
		}
	}

	@Test
	public void getRepresentationDescription_shouldOnlyIncludeAuditInfoInFullRepresentation() {
		Set<String> defaultProps = resource.getRepresentationDescription(new DefaultRepresentation())
				.getProperties().keySet();
		Set<String> fullProps = resource.getRepresentationDescription(new FullRepresentation())
				.getProperties().keySet();

		assertFalse(defaultProps.contains("auditInfo"));
		assertTrue(fullProps.contains("auditInfo"));
	}
}
