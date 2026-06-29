package org.openmrs.module.appointmentscheduling.reporting.data.evaluator;

import org.openmrs.Cohort;
import org.openmrs.annotation.Handler;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointmentscheduling.reporting.data.EvaluatedAppointmentData;
import org.openmrs.module.appointmentscheduling.reporting.data.definition.AppointmentDataDefinition;
import org.openmrs.module.appointmentscheduling.reporting.data.definition.PersonToAppointmentDataDefinition;
import org.openmrs.module.reporting.data.person.EvaluatedPersonData;
import org.openmrs.module.reporting.data.person.service.PersonDataService;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.EvaluationException;
import org.openmrs.module.reporting.evaluation.context.PersonEvaluationContext;
import org.openmrs.module.reporting.query.person.PersonIdSet;

import java.util.HashSet;
import java.util.Map;

@Handler(supports = PersonToAppointmentDataDefinition.class, order=50)
public class PersonToAppointmentDataEvaluator extends AbstractToAppointmentDataEvaluator {

    @Override
    protected void evaluateJoinedData(AppointmentDataDefinition definition, Map<Integer, Integer> convertedIds,
                                      EvaluationContext context, EvaluatedAppointmentData result) throws EvaluationException {
        PersonEvaluationContext personEvaluationContext = new PersonEvaluationContext();
        personEvaluationContext.setBaseCohort(new Cohort(convertedIds.values()));
        personEvaluationContext.setBasePersons(new PersonIdSet(new HashSet<Integer>(convertedIds.values())));

        PersonToAppointmentDataDefinition def = (PersonToAppointmentDataDefinition) definition;
        EvaluatedPersonData pd = Context.getService(PersonDataService.class).evaluate(def.getJoinedDefinition(), personEvaluationContext);

        for (Map.Entry<Integer, Integer> entry : convertedIds.entrySet()) {
            result.addData(entry.getKey(), pd.getData().get(entry.getValue()));
        }
    }
}
