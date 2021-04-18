package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.ServiceTimeSegmentWeighting;
import data.entity.WeightingEntity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class ServiceTimeSegmentWeightingMapper implements RowMapper<WeightingEntity> {
	
	public WeightingEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		ServiceTimeSegmentWeighting serviceTimeSegmentWeighting = new ServiceTimeSegmentWeighting();
		serviceTimeSegmentWeighting.setId(rs.getInt("sws_id"));
		serviceTimeSegmentWeighting.setName(rs.getString("sws_name"));
		serviceTimeSegmentWeighting.setSetEntityId(rs.getInt("sws_segment_set"));
		return serviceTimeSegmentWeighting;
	}
	
}
