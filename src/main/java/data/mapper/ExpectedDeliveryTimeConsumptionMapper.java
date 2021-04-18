package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.ExpectedDeliveryTimeConsumption;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class ExpectedDeliveryTimeConsumptionMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		ExpectedDeliveryTimeConsumption expectedDeliveryTimeConsumption = new ExpectedDeliveryTimeConsumption();
		expectedDeliveryTimeConsumption.setId(rs.getInt("edt_id"));
		expectedDeliveryTimeConsumption.setSetId(rs.getInt("edt_set"));
		expectedDeliveryTimeConsumption.setDeliveryAreaId(rs.getObject("edt_delivery_area", Integer.class));
		expectedDeliveryTimeConsumption.setTimeWindowId(rs.getObject("edt_tw", Integer.class));
		expectedDeliveryTimeConsumption.setDeliveryTime(rs.getObject("edt_time", Double.class));
		return expectedDeliveryTimeConsumption;
	}
	
}
