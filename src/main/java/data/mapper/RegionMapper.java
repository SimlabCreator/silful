package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.Region;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class RegionMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		Region region = new Region();
		region.setId(rs.getInt("reg_id"));
		region.setName(rs.getString("reg_name"));
		region.setLat1(rs.getFloat("reg_point1_lat"));
		region.setLon1(rs.getFloat("reg_point1_long"));
		region.setLat2(rs.getFloat("reg_point2_lat"));
		region.setLon2(rs.getFloat("reg_point2_long"));
		region.setAverageKmPerHour(rs.getFloat("reg_average_km_per_hour"));
		return region;
	}
}
