package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.DeliveryArea;
import data.entity.Entity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class DeliveryAreaMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		DeliveryArea deliveryArea = new DeliveryArea();
		deliveryArea.setId(rs.getInt("da_id"));
		deliveryArea.setSetId(rs.getInt("da_set"));
		deliveryArea.setLat1(rs.getDouble("da_point1_lat"));
		deliveryArea.setLon1(rs.getDouble("da_point1_long"));
		deliveryArea.setLat2(rs.getDouble("da_point2_lat"));
		deliveryArea.setLon2(rs.getDouble("da_point2_long"));
		deliveryArea.setCenterLat(rs.getDouble("da_center_lat"));
		deliveryArea.setCenterLon(rs.getDouble("da_center_long"));
		deliveryArea.setSubsetId(rs.getInt("da_subset"));
		return deliveryArea;
	}
	
}
