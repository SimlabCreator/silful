package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.ServiceTimeSegment;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class ServiceTimeSegmentMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		ServiceTimeSegment serviceTimeSegment = new ServiceTimeSegment();
		serviceTimeSegment.setId(rs.getInt("sse_id"));
		serviceTimeSegment.setSetId(rs.getInt("sse_set"));
		serviceTimeSegment.setProbabilityDistributionId(rs.getObject("sse_pd", Integer.class));		
		return serviceTimeSegment;
	}
	
}
