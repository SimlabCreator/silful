package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.SetEntity;
import data.entity.ValueBucketSet;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class ValueBucketSetMapper implements RowMapper<SetEntity> {
	
	public SetEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		ValueBucketSet valueBucketSet = new ValueBucketSet();
		valueBucketSet.setId(rs.getInt("vbs_id"));
		valueBucketSet.setName(rs.getString("vbs_name"));
		return valueBucketSet;
	}
	
}
