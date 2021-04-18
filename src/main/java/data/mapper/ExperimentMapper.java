package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.Experiment;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class ExperimentMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		Experiment experiment = new Experiment();
		experiment.setId(rs.getInt("exp_id"));
		experiment.setDescription(rs.getObject("exp_description", String.class));
		experiment.setResponsible(rs.getObject("exp_responsible", String.class));
		experiment.setOccasion(rs.getObject("exp_occasion", String.class));
		experiment.setRegionId(rs.getObject("exp_region", Integer.class));
		experiment.setProcessTypeId(rs.getObject("exp_processType", Integer.class));
		experiment.setBookingPeriodLength(rs.getObject("exp_booking_period_length", Integer.class));
		experiment.setIncentiveTypeId(rs.getObject("exp_incentive_type", Integer.class));
		experiment.setBookingPeriodNumber(rs.getObject("exp_booking_period_no", Integer.class));
		experiment.setName(rs.getObject("exp_name", String.class));
		experiment.setDepotId(rs.getObject("exp_depot", Integer.class));
		experiment.setCopyExperimentId(rs.getObject("exp_copy_exp", Integer.class));
		return experiment;
	}
	
}
