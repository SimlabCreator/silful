package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.KpiType;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class KpiTypeMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		KpiType kpi = new KpiType();
		kpi.setId(rs.getInt("kpi_id"));
		kpi.setName(rs.getString("kpi_name"));
		kpi.setDescription(rs.getString("kpi_description"));
		return kpi;
	}
	
}
