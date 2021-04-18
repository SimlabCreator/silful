package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Customer;
import data.entity.Entity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class CustomerMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		Customer customer = new Customer();
		customer.setId(rs.getInt("cus_id"));
		customer.setSetId(rs.getInt("cus_set"));
		customer.setLat(rs.getObject("cus_lat", Double.class));
		customer.setLon(rs.getObject("cus_long", Double.class));
		customer.setFloor(rs.getObject("cus_floor", Integer.class));
		customer.setClosestNodeId(rs.getObject("cus_closest_node", Long.class));
		customer.setDistanceClosestNode(rs.getObject("cus_closest_node_distance", Double.class));
		customer.setServiceTimeSegmentId(rs.getObject("cus_service_time_segment", Integer.class));
		customer.setOriginalDemandSegmentId(rs.getObject("cus_segment_original", Integer.class));
		customer.setReturnProbability(rs.getObject("cus_return_probability", Double.class));
		customer.setTempT(rs.getObject("cus_temp_t", Integer.class));
		return customer;
	}
	
}
