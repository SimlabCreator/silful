package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.DemandModelType;
import data.entity.Entity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class DemandModelTypeMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		DemandModelType dmt = new DemandModelType();
		dmt.setId(rs.getInt("dmt_id"));
		dmt.setName(rs.getString("dmt_name"));
		dmt.setParametric(rs.getBoolean("dmt_parametric"));
		dmt.setIndepdentent(rs.getBoolean("dmt_independent"));
		return dmt;
	}
	
}
