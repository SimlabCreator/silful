package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.Route;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class RouteMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		Route route = new Route();
		route.setId(rs.getInt("route_id"));
		route.setRoutingId(rs.getInt("route_routing"));
		route.setVehicleTypeId(rs.getObject("route_vehicle", Integer.class));
		route.setVehicleAreaAssignmentId(rs.getObject("route_vehicle_area_assignment", Integer.class));
		return route;
	}
	
}
