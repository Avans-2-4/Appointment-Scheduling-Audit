package org.openmrs.module.appointmentscheduling.reporting.data.evaluator;

import org.openmrs.Cohort;
import org.openmrs.annotation.Handler;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointmentscheduling.reporting.data.EvaluatedAppointmentData;
import org.openmrs.module.appointmentscheduling.reporting.data.definition.AppointmentDataDefinition;
import org.openmrs.module.appointmentscheduling.reporting.data.definition.PatientToAppointmentDataDefinition;
import org.openmrs.module.reporting.data.patient.EvaluatedPatientData;
import org.openmrs.module.reporting.data.patient.service.PatientDataService;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.EvaluationException;

import java.util.Map;

@Handler(supports = PatientToAppointmentDataDefinition.class, order=50)
public class PatientToAppointmentDataEvaluator extends AbstractToAppointmentDataEvaluator {

    @Override
    protected void evaluateJoinedData(AppointmentDataDefinition definition, Map<Integer, Integer> convertedIds,
                                      EvaluationContext context, EvaluatedAppointmentData result) throws EvaluationException {
        EvaluationContext patientEvaluationContext = new EvaluationContext();
        patientEvaluationContext.setBaseCohort(new Cohort(convertedIds.values()));

        PatientToAppointmentDataDefinition def = (PatientToAppointmentDataDefinition) definition;
        EvaluatedPatientData pd = Context.getService(PatientDataService.class).evaluate(def.getJoinedDefinition(), patientEvaluationContext);

        for (Map.Entry<Integer, Integer> entry : convertedIds.entrySet()) {
            result.addData(entry.getKey(), pd.getData().get(entry.getValue()));
        }
    }
}
