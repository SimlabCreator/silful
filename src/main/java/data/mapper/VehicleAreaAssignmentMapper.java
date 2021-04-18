package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.VehicleAreaAssignment;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class VehicleAreaAssignmentMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		VehicleAreaAssignment ass = new VehicleAreaAssignment();
		ass.setId(rs.getObject("vaa_id", Integer.class));
		ass.setSetId(rs.getObject("vaa_set",Integer.class));
		ass.setVehicleNo(rs.getObject("vaa_vehicle_no", Integer.class));
		ass.setDeliveryAreaId(rs.getObject("vaa_area", Integer.class));
		ass.setVehicleTypeId(rs.getObject("vaa_vehicle_type", Integer.class));
		ass.setStartingLocationLat(rs.getObject("vaa_starting_location_lat", Double.class));
		ass.setStartingLocationLon(rs.getObject("vaa_starting_location_lon", Double.class));
		ass.setEndingLocationLat(rs.getObject("vaa_ending_location_lat", Double.class));
		ass.setEndingLocationLon(rs.getObject("vaa_ending_location_lon", Double.class));
		ass.setStartTime(rs.getObject("vaa_start_time", Double.class));
		ass.setEndTime(rs.getObject("vaa_end_time", Double.class));
		return ass;
		
	}
}
