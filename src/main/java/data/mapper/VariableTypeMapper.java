package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.VariableType;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class VariableTypeMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		VariableType vt = new VariableType();
		vt.setId(rs.getInt("var_id"));
		vt.setName(rs.getString("var_name"));
		return vt;
	}
	
}
