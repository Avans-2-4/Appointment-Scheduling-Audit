package org.openmrs.module.appointmentscheduling.reporting.data.evaluator;

import org.openmrs.api.context.Context;
import org.openmrs.module.appointmentscheduling.Appointment;
import org.openmrs.module.appointmentscheduling.AppointmentSchedulingConstants;
import org.openmrs.module.appointmentscheduling.reporting.data.AppointmentDataUtil;
import org.openmrs.module.appointmentscheduling.reporting.data.EvaluatedAppointmentData;
import org.openmrs.module.appointmentscheduling.reporting.data.definition.AppointmentDataDefinition;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.EvaluationException;
import org.openmrs.module.reporting.evaluation.querybuilder.HqlQueryBuilder;
import org.openmrs.module.reporting.evaluation.service.EvaluationService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public abstract class AbstractToAppointmentDataEvaluator implements AppointmentDataEvaluator {

    private static final String HQL_APPOINTMENT_ID = "a.appointmentId";

    @Autowired
    protected EvaluationService evaluationService;

    @Override
    public EvaluatedAppointmentData evaluate(AppointmentDataDefinition definition, EvaluationContext context) throws EvaluationException {

        EvaluatedAppointmentData result = new EvaluatedAppointmentData(definition, context);

        HqlQueryBuilder q = new HqlQueryBuilder();
        q.select(HQL_APPOINTMENT_ID, "a.patient.patientId");
        q.from(Appointment.class, "a");
        if (context != null) {
            Set<Integer> appointmentIds = AppointmentDataUtil.getAppointmentIdsForContext(context, true);
            q.whereIn(HQL_APPOINTMENT_ID, appointmentIds);
        }
        Map<Integer, Integer> convertedIds = evaluationService.evaluateToMap(q, Integer.class, Integer.class, context);

        if (!Context.hasPrivilege(AppointmentSchedulingConstants.PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS)) {
            HqlQueryBuilder confidentialQuery = new HqlQueryBuilder();
            confidentialQuery.select(HQL_APPOINTMENT_ID, "case a.appointmentType.confidential when 0 then false else true end");
            confidentialQuery.from(Appointment.class, "a");
            if (context != null) {
                Set<Integer> appointmentIds = AppointmentDataUtil.getAppointmentIdsForContext(context, true);
                confidentialQuery.whereIn(HQL_APPOINTMENT_ID, appointmentIds);
            }
            Map<Integer, Boolean> confidentialMap = evaluationService.evaluateToMap(confidentialQuery, Integer.class, Boolean.class, context);
            for (Iterator<Map.Entry<Integer, Integer>> iterator = convertedIds.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<Integer, Integer> entry = iterator.next();
                if (Boolean.TRUE.equals(confidentialMap.get(entry.getKey()))) {
                    iterator.remove();
                }
            }
        }

        if (!convertedIds.isEmpty()) {
            evaluateJoinedData(definition, convertedIds, result);
        }

        return result;
    }

    protected abstract void evaluateJoinedData(AppointmentDataDefinition definition, Map<Integer, Integer> convertedIds,
                                               EvaluatedAppointmentData result) throws EvaluationException;
}
