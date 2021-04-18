package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.TimeWindow;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class TimeWindowMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		TimeWindow timeWindow = new TimeWindow();
		timeWindow.setId(rs.getInt("tw_id"));
		timeWindow.setSetId(rs.getInt("tw_set"));
		timeWindow.setStartTime(rs.getObject("tw_start_time", Double.class));
		timeWindow.setEndTime(rs.getObject("tw_end_time", Double.class));
		return timeWindow;
	}
	
}
