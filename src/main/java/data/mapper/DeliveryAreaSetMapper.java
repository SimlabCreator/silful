package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.DeliveryAreaSet;
import data.entity.SetEntity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class DeliveryAreaSetMapper implements RowMapper<SetEntity> {
	
	public SetEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		DeliveryAreaSet deliveryAreaSet = new DeliveryAreaSet();
		deliveryAreaSet.setId(rs.getInt("das_id"));
		deliveryAreaSet.setName(rs.getString("das_name"));
		deliveryAreaSet.setDescription(rs.getString("das_description"));
		deliveryAreaSet.setRegionId(rs.getInt("das_region"));
		deliveryAreaSet.setPredefined(rs.getBoolean("das_predefined"));
		deliveryAreaSet.setReasonableNumberOfAreas(rs.getObject("das_reasonable_area_no", Integer.class));
		return deliveryAreaSet;
	}
	
}
