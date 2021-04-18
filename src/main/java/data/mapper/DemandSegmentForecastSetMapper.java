package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.DemandSegmentForecastSet;
import data.entity.SetEntity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class DemandSegmentForecastSetMapper implements RowMapper<SetEntity> {
	
	public SetEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		DemandSegmentForecastSet demandForecastSet = new DemandSegmentForecastSet();
		demandForecastSet.setId(rs.getInt("dfs_id"));
		demandForecastSet.setName(rs.getObject("dfs_name", String.class));
		demandForecastSet.setDeliveryAreaSetId(rs.getObject("dfs_delivery_area_set", Integer.class));
		demandForecastSet.setDemandSegmentSetId(rs.getObject("dfs_demand_segment_set", Integer.class));
		return demandForecastSet;
	}
	
}
