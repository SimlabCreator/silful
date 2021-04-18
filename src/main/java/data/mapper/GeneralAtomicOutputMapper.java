package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.GeneralAtomicOutputValue;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class GeneralAtomicOutputMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		GeneralAtomicOutputValue parameter = new GeneralAtomicOutputValue();
		parameter.setParameterTypeId(rs.getInt("run_parameter_parameter"));
		parameter.setValue(rs.getObject("run_parameter_value", Double.class));
		parameter.setRunId(rs.getInt("run_parameter_run"));
		parameter.setPeriodNo(rs.getObject("run_period", Integer.class));
		return parameter;
	}
}
