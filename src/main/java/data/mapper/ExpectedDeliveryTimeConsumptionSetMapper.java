package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.ExpectedDeliveryTimeConsumptionSet;
import data.entity.SetEntity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class ExpectedDeliveryTimeConsumptionSetMapper implements RowMapper<SetEntity> {
	
	public SetEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		ExpectedDeliveryTimeConsumptionSet expectedDeliveryTimeConsumptionSet = new ExpectedDeliveryTimeConsumptionSet();
		expectedDeliveryTimeConsumptionSet.setId(rs.getInt("eds_id"));
		expectedDeliveryTimeConsumptionSet.setName(rs.getObject("eds_name", String.class));
		expectedDeliveryTimeConsumptionSet.setRoutingId(rs.getObject("eds_delivery_area_set", Integer.class));
		expectedDeliveryTimeConsumptionSet.setDeliveryAreaSetId(rs.getObject("eds_tw_set", Integer.class));
		expectedDeliveryTimeConsumptionSet.setTimeWindowSetId(rs.getObject("eds_routing", Integer.class));
		return expectedDeliveryTimeConsumptionSet;
	}
	
}
