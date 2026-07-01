package org.openmrs.module.appointmentscheduling.api.db.hibernate;

import org.hibernate.Query;
import org.openmrs.Location;
import org.openmrs.Provider;
import org.openmrs.api.db.DAOException;
import org.openmrs.module.appointmentscheduling.ProviderSchedule;
import org.openmrs.module.appointmentscheduling.api.db.ProviderScheduleDAO;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class HibernateProviderScheduleDAO extends HibernateSingleClassDAO
        implements ProviderScheduleDAO {

    private static final String TIME_FORMAT = "HH:mm:ss";

    /**
     * You must call this before using any of the data access methods, since it's not actually
     * possible to write them all with compile-time class information.
     *
     * @param
     */
    public HibernateProviderScheduleDAO() {
        super(ProviderSchedule.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProviderSchedule> getProviderScheduleByConstraints(Location location, Provider provider, Date appointmentDate) throws DAOException {

        if (location != null) {
			String stringQuery = "SELECT providerSchedule FROM ProviderSchedule AS providerSchedule WHERE providerSchedule.voided = false";

            stringQuery += " AND providerSchedule.location=:location";
            if (provider != null)
                stringQuery += " AND providerSchedule.provider=:provider";
            if (isSpecificTime(appointmentDate)) {
                stringQuery += " AND :appointmentTime >= providerSchedule.startTime AND :appointmentTime <= providerSchedule.endTime";
            }
            Query query = super.sessionFactory.getCurrentSession().createQuery(
                    stringQuery);

            query.setParameter("location", location);
            if (provider != null)
                query.setParameter("provider", provider);
            if (isSpecificTime(appointmentDate)) {
                query.setParameter("appointmentTime", getTimeFromDate(appointmentDate));
            }

            return (List<ProviderSchedule>) query.list();

        } else {
            throw new DAOException("location must have a value");
        }
    }

    private boolean isSpecificTime(Date date) {
        return date != null && !new SimpleDateFormat(TIME_FORMAT).format(date).equals("00:00:00");
    }

    private Time getTimeFromDate(Date date) {
        try {
            return new Time(new SimpleDateFormat(TIME_FORMAT)
                    .parse(new SimpleDateFormat(TIME_FORMAT).format(date)).getTime());
        } catch (ParseException e) {
            // e.printStackTrace(); // disabled bc of possible logging of medical data, and a possible extra attack vector.
            return null;
        }
    }
}