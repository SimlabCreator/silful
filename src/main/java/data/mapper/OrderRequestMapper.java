package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.OrderRequest;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class OrderRequestMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		OrderRequest orderRequest = new OrderRequest();
		orderRequest.setId(rs.getInt("orr_id"));
		orderRequest.setSetId(rs.getInt("orr_set"));
		orderRequest.setCustomerId(rs.getInt("orr_customer"));
		orderRequest.setOrderContentTypeId(rs.getObject("orr_content_type", Integer.class));
		orderRequest.setBasketValue(rs.getObject("orr_basket_value", Double.class));
		orderRequest.setBasketVolume(rs.getObject("orr_basket_volume", Double.class));
		orderRequest.setPackageno(rs.getObject("orr_basket_packageno", Integer.class));
		orderRequest.setArrivalTime(rs.getObject("orr_t", Integer.class));
		return orderRequest;
	}
	
}
