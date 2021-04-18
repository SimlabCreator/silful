package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Capacity;
import data.entity.Entity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class CapacityMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		Capacity capacity = new Capacity();
		capacity.setId(rs.getInt("cap_id"));
		capacity.setSetId(rs.getInt("cap_set"));
		capacity.setDeliveryAreaId(rs.getObject("cap_delivery_area", Integer.class));
		capacity.setTimeWindowId(rs.getObject("cap_tw", Integer.class));
		capacity.setCapacityNumber(rs.getObject("cap_no", Integer.class));
		return capacity;
	}
	
}
