package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.ProcessType;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class ProcessTypeMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		ProcessType process = new ProcessType();
		process.setId(rs.getInt("pro_id"));
		process.setName(rs.getString("pro_name"));
		process.setDescription(rs.getString("pro_description"));
		return process;
	}
	
}
