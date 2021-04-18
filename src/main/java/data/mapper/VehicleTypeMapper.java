package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.VehicleType;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class VehicleTypeMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		VehicleType vehicle = new VehicleType();
		vehicle.setId(rs.getInt("veh_id"));
		vehicle.setCapacityNumber(rs.getInt("veh_capacity_no"));
		vehicle.setCapacityVolume(rs.getObject("veh_capacity_volume", Double.class));
		vehicle.setCooling(rs.getBoolean("veh_cooling"));
		vehicle.setFreezer(rs.getBoolean("veh_freezer"));
		return vehicle;
	}
	
}
