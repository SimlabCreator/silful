package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.DistributionParameterValue;
import data.entity.Entity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class DistributionParameterValueMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		DistributionParameterValue para = new DistributionParameterValue();
		para.setProbabilityDistributionId(rs.getInt("dpv_probability_distribution"));
		para.setParameterTypeId(rs.getInt("dpv_parameter_type"));
		para.setValue(rs.getObject("dpv_value", Double.class));
		return para;
	}
}
