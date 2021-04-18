package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.ObjectiveType;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class ObjectiveTypeMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		ObjectiveType obj = new ObjectiveType();
		obj.setId(rs.getInt("obj_id"));
		obj.setName(rs.getString("obj_name"));
		obj.setDescription(rs.getString("obj_description"));
		return obj;
	}
	
}
