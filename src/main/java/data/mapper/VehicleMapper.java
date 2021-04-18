package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.Vehicle;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class VehicleMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		Vehicle vehicle = new Vehicle();
		vehicle.setVehicleNo(rs.getObject("exp_vehicle_no", Integer.class));
		vehicle.setVehicleTypeId(rs.getObject("exp_vehicle_veh", Integer.class));
		vehicle.setExperimentId(rs.getObject("exp_vehicle_exp", Integer.class));
		return vehicle;
		
	}
}
