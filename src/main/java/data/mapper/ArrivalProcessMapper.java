package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.ArrivalProcess;
import data.entity.Entity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class ArrivalProcessMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		ArrivalProcess arrivalProcess = new ArrivalProcess();
		arrivalProcess.setId(rs.getInt("arr_id"));
		arrivalProcess.setProbabilityDistributionId(rs.getObject("arr_pd", Integer.class));	
		arrivalProcess.setFactor(rs.getObject("arr_lambda_factor", Double.class));
		arrivalProcess.setName(rs.getObject("arr_name", String.class));
		return arrivalProcess;
	}
	
}
