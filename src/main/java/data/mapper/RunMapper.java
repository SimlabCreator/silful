package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.Run;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class RunMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		Run run = new Run();
		run.setId(rs.getInt("run_id"));
		//run.setDescription(rs.getObject("run_description", String.class));
		run.setDate(rs.getDate("run_datetime"));
		run.setExperimentId(rs.getInt("run_experiment"));
		run.setRuntime(rs.getInt("run_length"));
		//run.setResponsible(rs.getObject("run_responsible", String.class));
	//	run.setOccasion(rs.getObject("run_occasion", String.class));
		//run.setRegionId(rs.getObject("run_region", Integer.class));
	//	run.setProcessTypeId(rs.getObject("run_processType", Integer.class));
	//	run.setBookingPeriodLength(rs.getObject("run_booking_period_length", Integer.class));
	//	run.setIncentiveTypeId(rs.getObject("run_incentive_type", Integer.class));
	//	run.setBookingPeriodNumber(rs.getObject("run_booking_period_no", Integer.class));
		return run;
	}
	
}
