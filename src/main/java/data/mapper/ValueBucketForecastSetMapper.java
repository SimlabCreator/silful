package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.ValueBucketForecastSet;
import data.entity.SetEntity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class ValueBucketForecastSetMapper implements RowMapper<SetEntity> {
	
	public SetEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		ValueBucketForecastSet demandForecastSet = new ValueBucketForecastSet();
		demandForecastSet.setId(rs.getInt("vfs_id"));
		demandForecastSet.setName(rs.getObject("vfs_name", String.class));
		demandForecastSet.setDeliveryAreaSetId(rs.getObject("vfs_delivery_area_set", Integer.class));
		demandForecastSet.setAlternativeSetId(rs.getObject("vfs_alternative_set", Integer.class));
		demandForecastSet.setValueBucketSetId(rs.getObject("vfs_value_bucket_set", Integer.class));
		return demandForecastSet;
	}
	
}
