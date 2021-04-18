package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.SetEntity;
import data.entity.TimeWindowSet;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class TimeWindowSetMapper implements RowMapper<SetEntity> {
	
	public SetEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		TimeWindowSet timeWindowSet = new TimeWindowSet();
		timeWindowSet.setId(rs.getInt("tws_id"));
		timeWindowSet.setName(rs.getString("tws_name"));
		timeWindowSet.setOverlapping(rs.getBoolean("tws_overlapping"));
		timeWindowSet.setSameLength(rs.getBoolean("tws_same_length"));
		timeWindowSet.setContinuous(rs.getBoolean("tws_continuous"));
		return timeWindowSet;
	}
	
}
