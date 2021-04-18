package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.ValueBucketForecast;
import data.entity.Entity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class ValueBucketForecastMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		ValueBucketForecast demandForecast = new ValueBucketForecast();
		demandForecast.setId(rs.getInt("vf_id"));
		demandForecast.setSetId(rs.getInt("vf_set"));
		//demandForecast.setDemandSegmentId(rs.getObject("df_segment", Integer.class));
		demandForecast.setDemandNumber(rs.getObject("vf_no", Integer.class));
		demandForecast.setDeliveryAreaId(rs.getObject("vf_delivery_area", Integer.class));
		demandForecast.setAlternativeId(rs.getObject("vf_alternative", Integer.class));
		demandForecast.setValueBucketId(rs.getObject("vf_value_bucket", Integer.class));
		return demandForecast;
	}
	
}
