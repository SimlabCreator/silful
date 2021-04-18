package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Delivery;
import data.entity.Entity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class DeliveryMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		Delivery delivery = new Delivery();
		delivery.setId(rs.getInt("del_id"));
		delivery.setRouteElementId(rs.getObject("del_re_id", Integer.class));
		delivery.setTravelTime(rs.getObject("del_travel_time", Integer.class));
		delivery.setArrivalTime(rs.getObject("del_arrival_time", Double.class));
		delivery.setWaitingTime(rs.getObject("del_waiting_time", Integer.class));
		delivery.setServiceBegin(rs.getObject("del_service_begin", Double.class));
		delivery.setServiceTime(rs.getObject("del_service_time", Integer.class));
		delivery.setBufferBefore(rs.getObject("del_buffer_before", Integer.class));		
		return delivery;
	}
	
}
