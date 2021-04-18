package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.Order;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class OrderMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		Order order = new Order();
		order.setId(rs.getInt("ord_id"));
		order.setSetId(rs.getInt("ord_set"));
		order.setOrderRequestId(rs.getInt("ord_order_request"));
		order.setTimeWindowFinalId(rs.getObject("ord_tw_final", Integer.class));
		order.setSelectedAlternativeId(rs.getObject("ord_alternative_selected", Integer.class));
		order.setAccepted(rs.getObject("ord_accepted", Boolean.class));
		order.setReasonRejection(rs.getObject("ord_reason_rejection", String.class));
		order.setAlternativeFee(rs.getObject("ord_alternative_fee", Double.class));
		order.setAssignedDeliveryAreaId(rs.getObject("ord_assigned_delivery_area", Integer.class));
		order.setAssignedValue(rs.getObject("ord_assigned_value", Double.class));
		return order;
	}
	
}
