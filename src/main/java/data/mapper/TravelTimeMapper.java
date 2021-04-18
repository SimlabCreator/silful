package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.TravelTime;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class TravelTimeMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		TravelTime travelTime = new TravelTime();
		travelTime.setId(rs.getInt("tra_id"));
		travelTime.setSetId(rs.getInt("tra_set"));
		travelTime.setStart(rs.getObject("tra_start", Double.class));
		travelTime.setEnd(rs.getObject("tra_end", Double.class));
		travelTime.setProbabilityDistributionId(rs.getObject("tra_pd", Integer.class));		
		return travelTime;
	}
	
}
