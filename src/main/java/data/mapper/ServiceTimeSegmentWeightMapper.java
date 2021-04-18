package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.ServiceTimeSegmentWeight;
import data.entity.WeightEntity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class ServiceTimeSegmentWeightMapper implements RowMapper<WeightEntity> {
	
	public WeightEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		ServiceTimeSegmentWeight serviceTimeSegmentWeight = new ServiceTimeSegmentWeight();
		serviceTimeSegmentWeight.setId(rs.getInt("ssw_id"));
		serviceTimeSegmentWeight.setSetId(rs.getInt("ssw_set"));	
		serviceTimeSegmentWeight.setWeight(rs.getObject("ssw_weight", Double.class));
		serviceTimeSegmentWeight.setElementId(rs.getObject("ssw_service_segment", Integer.class));
		return serviceTimeSegmentWeight;
	}
	
}
