package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.OrderSet;
import data.entity.SetEntity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class OrderSetMapper implements RowMapper<SetEntity> {
	
	public SetEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		OrderSet orderSet = new OrderSet();
		orderSet.setId(rs.getInt("os_id"));
		orderSet.setName(rs.getString("os_name"));
		orderSet.setOrderRequestSetId(rs.getObject("os_order_request_set", Integer.class));
		orderSet.setControlSetId(rs.getObject("os_control_set", Integer.class));
		return orderSet;
	}
	
}
