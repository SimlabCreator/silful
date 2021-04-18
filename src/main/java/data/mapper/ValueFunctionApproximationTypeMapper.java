package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.ValueFunctionApproximationType;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class ValueFunctionApproximationTypeMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		ValueFunctionApproximationType type = new ValueFunctionApproximationType();
		type.setId(rs.getInt("vft_id"));
		type.setName(rs.getString("vft_name"));
		return type;
	}
	
}
