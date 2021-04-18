package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.OrderContentType;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class OrderContentTypeMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		OrderContentType orderContentType = new OrderContentType();
		orderContentType.setId(rs.getInt("oct_id"));
		orderContentType.setName(rs.getString("oct_name"));
		return orderContentType;
	}
	
}
