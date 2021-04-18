package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.ServiceTimeSegmentSet;
import data.entity.SetEntity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class ServiceTimeSegmentSetMapper implements RowMapper<SetEntity> {
	
	public SetEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		ServiceTimeSegmentSet serviceTimeSegmentSet = new ServiceTimeSegmentSet();
		serviceTimeSegmentSet.setId(rs.getInt("sss_id"));
		serviceTimeSegmentSet.setName(rs.getString("sss_name"));		
		return serviceTimeSegmentSet;
	}
	
}
