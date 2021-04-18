package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.CapacitySet;
import data.entity.SetEntity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class CapacitySetMapper implements RowMapper<SetEntity> {
	
	public SetEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		CapacitySet capacitySet = new CapacitySet();
		capacitySet.setId(rs.getInt("cas_id"));
		capacitySet.setName(rs.getObject("cas_name", String.class));
		Integer routingId = rs.getObject("cas_routing", Integer.class);
		if(routingId==0){
			routingId=null;
		}
		capacitySet.setRoutingId(routingId);
		capacitySet.setDeliveryAreaSetId(rs.getObject("cas_delivery_area_set", Integer.class));
		capacitySet.setTimeWindowSetId(rs.getObject("cas_tw_set", Integer.class));
		capacitySet.setWeight(rs.getObject("cas_weight", Double.class));
		return capacitySet;
	}
	
}
