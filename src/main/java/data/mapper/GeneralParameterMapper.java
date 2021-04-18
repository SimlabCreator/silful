package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.GeneralParameterValue;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class GeneralParameterMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		GeneralParameterValue parameter = new GeneralParameterValue();
		parameter.setParameterTypeId(rs.getInt("exp_parameter_parameter"));
		parameter.setValue(rs.getObject("exp_parameter_value", Double.class));
		parameter.setExperimentId(rs.getInt("exp_parameter_exp"));
		return parameter;
	}
}
