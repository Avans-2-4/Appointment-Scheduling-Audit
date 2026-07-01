package org.openmrs.module.appointmentscheduling.rest.resource.openmrs1_9;

import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointmentscheduling.AppointmentRequest;
import org.openmrs.module.appointmentscheduling.AppointmentType;
import org.openmrs.module.appointmentscheduling.api.AppointmentService;
import org.openmrs.module.appointmentscheduling.rest.controller.AppointmentRestController;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.DataDelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

@Resource(name = RestConstants.VERSION_1 + AppointmentRestController.APPOINTMENT_SCHEDULING_REST_NAMESPACE + "/appointmentrequest", supportedClass = AppointmentRequest.class,
    supportedOpenmrsVersions = {"1.9.*", "1.10.*", "1.11.*", "1.12.*", "2.0.*", "2.1.*", "2.2.*", "2.3.*", "2.4.*", "2.5.*"})
public class AppointmentRequestResource1_9 extends DataDelegatingCrudResource<AppointmentRequest> {

    @Override
    public DelegatingResourceDescription getRepresentationDescription(Representation representation) {
        if (representation instanceof DefaultRepresentation) {
            DelegatingResourceDescription description = new DelegatingResourceDescription();
            description.addProperty("uuid");
            description.addProperty("display", findMethod("getDisplayString"));
            description.addProperty(AppointmentRequest.FIELD_PATIENT, Representation.DEFAULT);
            description.addProperty(AppointmentRequest.FIELD_APPOINTMENT_TYPE, Representation.REF);
            description.addProperty(AppointmentRequest.FIELD_PROVIDER, Representation.DEFAULT);
            description.addProperty(AppointmentRequest.FIELD_REQUESTED_BY, Representation.DEFAULT);
            addSharedProperties(description);
            description.addSelfLink();
            description.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
            return description;
        } else if (representation instanceof FullRepresentation) {
            DelegatingResourceDescription description = new DelegatingResourceDescription();
            description.addProperty("uuid");
            description.addProperty("display", findMethod("getDisplayString"));
            description.addProperty(AppointmentRequest.FIELD_PATIENT, Representation.FULL);
            description.addProperty(AppointmentRequest.FIELD_APPOINTMENT_TYPE, Representation.FULL);
            description.addProperty(AppointmentRequest.FIELD_PROVIDER, Representation.FULL);
            description.addProperty(AppointmentRequest.FIELD_REQUESTED_BY, Representation.FULL);
            addSharedProperties(description);
            description.addProperty("auditInfo", findMethod("getAuditInfo"));
            description.addSelfLink();
            return description;
        }

        return null;
    }

    @Override
    public DelegatingResourceDescription getCreatableProperties() {
        return buildWriteDescription();
    }

    @Override
    public DelegatingResourceDescription getUpdatableProperties() {
        return buildWriteDescription();
    }

    private DelegatingResourceDescription buildWriteDescription() {
        DelegatingResourceDescription description = new DelegatingResourceDescription();
        description.addRequiredProperty(AppointmentRequest.FIELD_PATIENT);
        description.addRequiredProperty(AppointmentRequest.FIELD_APPOINTMENT_TYPE);
        description.addProperty(AppointmentRequest.FIELD_PROVIDER);
        description.addProperty(AppointmentRequest.FIELD_REQUESTED_BY);
        description.addRequiredProperty(AppointmentRequest.FIELD_REQUESTED_ON);
        description.addRequiredProperty(AppointmentRequest.FIELD_STATUS);
        description.addProperty(AppointmentRequest.FIELD_MIN_TIME_FRAME_VALUE);
        description.addProperty(AppointmentRequest.FIELD_MIN_TIME_FRAME_UNITS);
        description.addProperty(AppointmentRequest.FIELD_MAX_TIME_FRAME_VALUE);
        description.addProperty(AppointmentRequest.FIELD_MAX_TIME_FRAME_UNITS);
        description.addProperty(AppointmentRequest.FIELD_NOTES);
        return description;
    }

    private void addSharedProperties(DelegatingResourceDescription description) {
        description.addProperty(AppointmentRequest.FIELD_REQUESTED_ON);
        description.addProperty(AppointmentRequest.FIELD_STATUS);
        description.addProperty(AppointmentRequest.FIELD_MIN_TIME_FRAME_VALUE);
        description.addProperty(AppointmentRequest.FIELD_MIN_TIME_FRAME_UNITS);
        description.addProperty(AppointmentRequest.FIELD_MAX_TIME_FRAME_VALUE);
        description.addProperty(AppointmentRequest.FIELD_MAX_TIME_FRAME_UNITS);
        description.addProperty(AppointmentRequest.FIELD_NOTES);
        description.addProperty("voided");
    }

    @Override
    public AppointmentRequest getByUniqueId(String uuid) {
        return Context.getService(AppointmentService.class).getAppointmentRequestByUuid(uuid);
    }

    @Override
    protected void delete(AppointmentRequest appointmentRequest, String reason, RequestContext requestContext) throws ResponseException {
        if (appointmentRequest.isVoided()) {
            return;
        }
        Context.getService(AppointmentService.class).voidAppointmentRequest(appointmentRequest, reason);
    }

    @Override
    public AppointmentRequest newDelegate() {
        return new AppointmentRequest();
    }

    @Override
    public AppointmentRequest save(AppointmentRequest appointmentRequest) {
        return Context.getService(AppointmentService.class).saveAppointmentRequest(appointmentRequest);
    }

    @Override
    public void purge(AppointmentRequest appointmentRequest, RequestContext requestContext) throws ResponseException {
        if (appointmentRequest == null) {
            return;
        }
        Context.getService(AppointmentService.class).purgeAppointmentRequest(appointmentRequest);
    }

    @Override
    protected PageableResult doSearch(RequestContext context) {

        AppointmentType appointmentType = context.getParameter(AppointmentRequest.FIELD_APPOINTMENT_TYPE) != null ? Context.getService(
                AppointmentService.class).getAppointmentTypeByUuid(context.getParameter(AppointmentRequest.FIELD_APPOINTMENT_TYPE)) : null;

        Provider provider = context.getParameter(AppointmentRequest.FIELD_PROVIDER) != null ? Context.getProviderService().getProviderByUuid(
                context.getParameter(AppointmentRequest.FIELD_PROVIDER)) : null;

        Patient patient = context.getParameter(AppointmentRequest.FIELD_PATIENT) != null ? Context.getPatientService().getPatientByUuid(
                context.getParameter(AppointmentRequest.FIELD_PATIENT)) : null;

        AppointmentRequest.AppointmentRequestStatus status =  context.getParameter(AppointmentRequest.FIELD_STATUS) != null  ?
                AppointmentRequest.AppointmentRequestStatus.valueOf(context.getParameter(AppointmentRequest.FIELD_STATUS).toUpperCase())
                : null;

        return new NeedsPaging<AppointmentRequest>(Context.getService(AppointmentService.class).getAppointmentRequestsByConstraints(
                patient, appointmentType, provider, status), context);

    }

    @Override
    protected NeedsPaging<AppointmentRequest> doGetAll(RequestContext context) {
        return new NeedsPaging<AppointmentRequest>(Context.getService(AppointmentService.class).getAllAppointmentRequests(
                context.getIncludeAll()), context);
    }

    public String getDisplayString(AppointmentRequest appointmentRequest) {
        return appointmentRequest.getAppointmentType().getName() + " : " + appointmentRequest.getStatus();
    }
}
