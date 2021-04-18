package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.DemandSegmentForecast;
import data.entity.Entity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class DemandSegmentForecastMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		DemandSegmentForecast demandForecast = new DemandSegmentForecast();
		demandForecast.setId(rs.getInt("df_id"));
		demandForecast.setSetId(rs.getInt("df_set"));
		//demandForecast.setDemandSegmentId(rs.getObject("df_segment", Integer.class));
		demandForecast.setDemandNumber(rs.getObject("df_no", Integer.class));
		demandForecast.setDeliveryAreaId(rs.getObject("df_delivery_area", Integer.class));
		demandForecast.setDemandSegmentId(rs.getObject("df_demand_segment", Integer.class));
		return demandForecast;
	}
	
}
