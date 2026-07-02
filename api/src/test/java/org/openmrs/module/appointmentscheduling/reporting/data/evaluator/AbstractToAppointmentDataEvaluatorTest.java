package org.openmrs.module.appointmentscheduling.reporting.data.evaluator;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointmentscheduling.reporting.context.AppointmentEvaluationContext;
import org.openmrs.module.appointmentscheduling.reporting.data.EvaluatedAppointmentData;
import org.openmrs.module.appointmentscheduling.reporting.data.definition.AppointmentDataDefinition;
import org.openmrs.module.appointmentscheduling.reporting.data.definition.PatientToAppointmentDataDefinition;
import org.openmrs.module.appointmentscheduling.reporting.query.AppointmentIdSet;
import org.openmrs.module.reporting.data.patient.definition.PatientIdDataDefinition;
import org.openmrs.module.reporting.evaluation.service.EvaluationService;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Tests the confidentiality-filtering logic shared by MA-02 ({@code AbstractToAppointmentDataEvaluator#evaluate}),
 * isolated from any subclass's HQL join step. {@code PatientToAppointmentDataEvaluatorTest} and
 * {@code PersonToAppointmentDataEvaluatorTest} already cover this indirectly through their own
 * {@code evaluateJoinedData} implementations - this test uses a minimal recording subclass instead, so a
 * regression in the shared base class can't be masked by, or confused with, evaluator-specific join logic.
 */
public class AbstractToAppointmentDataEvaluatorTest extends BaseModuleContextSensitiveTest {

	private static class RecordingEvaluator extends AbstractToAppointmentDataEvaluator {

		@Override
		protected void evaluateJoinedData(AppointmentDataDefinition definition, Map<Integer, Integer> convertedIds,
				EvaluatedAppointmentData result) {
			for (Map.Entry<Integer, Integer> entry : convertedIds.entrySet()) {
				result.addData(entry.getKey(), entry.getValue());
			}
		}
	}

	@Autowired
	private EvaluationService evaluationService;

	private RecordingEvaluator evaluator;

	private AppointmentDataDefinition definition;

	@Before
	public void setup() throws Exception {
		executeDataSet("standardAppointmentTestDataset.xml");
		evaluator = new RecordingEvaluator();
		evaluator.evaluationService = evaluationService;
		definition = new PatientToAppointmentDataDefinition(new PatientIdDataDefinition());
	}

	@Test
	public void evaluate_shouldJoinAllAppointmentsWhenUserHasConfidentialPrivilege() throws Exception {
		AppointmentEvaluationContext context = new AppointmentEvaluationContext();
		context.setBaseAppointments(new AppointmentIdSet(1, 2, 4)); // 1 and 2 are confidential (type 1)

		EvaluatedAppointmentData result = evaluator.evaluate(definition, context);

		assertEquals(3, result.getData().size());
	}

	@Test
	@DirtiesContext
	public void evaluate_shouldExcludeConfidentialAppointmentsWhenUserLacksPrivilege() throws Exception {
		Context.becomeUser("butch");
		AppointmentEvaluationContext context = new AppointmentEvaluationContext();
		context.setBaseAppointments(new AppointmentIdSet(1, 2, 4)); // 1 and 2 are confidential (type 1)

		EvaluatedAppointmentData result = evaluator.evaluate(definition, context);

		assertEquals(1, result.getData().size());
		assertFalse(result.getData().containsKey(1));
		assertFalse(result.getData().containsKey(2));
	}

	@Test
	@DirtiesContext
	public void evaluate_shouldSkipJoinStepEntirelyWhenEveryAppointmentIsFilteredOut() throws Exception {
		Context.becomeUser("butch");
		AppointmentEvaluationContext context = new AppointmentEvaluationContext();
		context.setBaseAppointments(new AppointmentIdSet(1, 2)); // both confidential (type 1) -> no ids left to join

		EvaluatedAppointmentData result = evaluator.evaluate(definition, context);

		assertEquals(0, result.getData().size());
	}
}
