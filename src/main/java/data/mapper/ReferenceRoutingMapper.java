package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.ReferenceRouting;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class ReferenceRoutingMapper implements RowMapper<ReferenceRouting> {
	
	public ReferenceRouting mapRow(ResultSet rs, int rowNum) throws SQLException {
		ReferenceRouting rr = new ReferenceRouting();
		rr.setDeliveryAreaId(rs.getObject("rr_delivery_area", Integer.class));
		rr.setOrderSetId(rs.getObject("rr_order_set", Integer.class));
		rr.setRoutingId(rs.getObject("rr_routing", Integer.class));
		rr.setRemainingCap(rs.getObject("rr_left_over", Integer.class));
		rr.setNumberOfTheftsSpatial(rs.getObject("rr_theft_spatial", Integer.class));
		rr.setNumberOfTheftsSpatialAdvanced(rs.getObject("rr_theft_advanced", Integer.class));
		rr.setNumberOfTheftsTime(rs.getObject("rr_theft_time", Integer.class));
		rr.setFirstTheftsSpatial(rs.getObject("rr_theft_spatial_first", Integer.class));
		rr.setFirstTheftsSpatialAdvanced(rs.getObject("rr_theft_advanced_first", Integer.class));
		rr.setFirstTheftsTime(rs.getObject("rr_theft_time_first", Integer.class));
		return rr;
	}
	
}
