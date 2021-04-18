package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.ValueBucket;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class ValueBucketMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		ValueBucket valueBucket = new ValueBucket();
		valueBucket.setId(rs.getInt("vb_id"));
		valueBucket.setSetId(rs.getInt("vb_set"));
		valueBucket.setUpperBound(rs.getObject("vb_bound_upper", Double.class));
		valueBucket.setLowerBound(rs.getObject("vb_lower_bound", Double.class));
		return valueBucket;
	}
	
}
