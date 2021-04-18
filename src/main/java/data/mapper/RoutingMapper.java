package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.Routing;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class RoutingMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		Routing routing = new Routing();
		routing.setId(rs.getInt("rou_id"));
		routing.setPossiblyFinalRouting(rs.getBoolean("rou_possibly_final"));
		routing.setTimeWindowSetId(rs.getObject("rou_time_window_set", Integer.class));
		routing.setName(rs.getObject("rou_name", String.class));
		routing.setOrderSetId(rs.getObject("rou_order_set", Integer.class));
		routing.setDepotId(rs.getObject("rou_depot", Integer.class));
		routing.setAdditionalInformation(rs.getObject("rou_information", String.class));
		routing.setVehicleAreaAssignmentSetId(rs.getObject("rou_vehicle_area_assignment_set", Integer.class));
		routing.setPossiblyTarget(rs.getBoolean("rou_possibly_target"));
		routing.setAdditionalCosts(rs.getObject("rou_additional_costs", Double.class));
		routing.setAreaWeighting(rs.getObject("rou_area_weighting", String.class));
		routing.setAreaDsWeighting(rs.getObject("rou_area_ds_weighting",  String.class));
		return routing;
	}
	
}
