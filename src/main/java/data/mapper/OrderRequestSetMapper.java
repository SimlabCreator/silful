package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.OrderRequestSet;
import data.entity.SetEntity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class OrderRequestSetMapper implements RowMapper<SetEntity> {
	
	public SetEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		OrderRequestSet orderRequestSet = new OrderRequestSet();
		orderRequestSet.setId(rs.getInt("ors_id"));
		orderRequestSet.setName(rs.getString("ors_name"));
		orderRequestSet.setCustomerSetId(rs.getObject("ors_customer_set", Integer.class));
		orderRequestSet.setBookingHorizon(rs.getObject("ors_booking_horizon", Integer.class));
		orderRequestSet.setPreferencesSampled(rs.getObject("ors_sampledPreferences", Boolean.class));
		return orderRequestSet;
	}
	
}
