package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.SetEntity;
import data.entity.TravelTimeSet;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class TravelTimeSetMapper implements RowMapper<SetEntity> {
	
	public SetEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		TravelTimeSet travelTimeSet = new TravelTimeSet();
		travelTimeSet.setId(rs.getInt("tts_id"));
		travelTimeSet.setName(rs.getString("tts_name"));	
		travelTimeSet.setRegionId(rs.getInt("tts_region"));
		return travelTimeSet;
	}
	
}
