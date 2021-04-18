package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.ParameterType;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class ParameterTypeMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		ParameterType parameter = new ParameterType();
		parameter.setId(rs.getInt("par_id"));
		parameter.setName(rs.getString("par_name"));
		parameter.setDescription(rs.getString("par_description"));
		return parameter;
	}
}
