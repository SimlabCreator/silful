package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.RouteElement;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class RouteElementMapper implements RowMapper<Entity> {

	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		RouteElement routeElement = new RouteElement();
		routeElement.setId(rs.getInt("re_id"));
		routeElement.setRouteId(rs.getInt("re_route"));
		routeElement.setPosition(rs.getObject("re_position", Integer.class));
		routeElement.setTimeWindowId(rs.getObject("re_tw", Integer.class));
		routeElement.setDeliveryAreaId(rs.getObject("re_area", Integer.class));
		routeElement.setOrderId(rs.getObject("re_order", Integer.class));
		routeElement.setTravelTime(rs.getObject("re_travel_time", Double.class));
		routeElement.setWaitingTime(rs.getObject("re_waiting_time", Double.class));
		routeElement.setServiceBegin(rs.getObject("re_service_begin", Double.class));
		routeElement.setServiceTime(rs.getObject("re_service_time", Double.class));
		routeElement.setSlack(rs.getObject("re_slack", Double.class));

		return routeElement;
	}

}
