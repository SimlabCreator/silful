package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.DemandSegmentWeighting;
import data.entity.WeightingEntity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class DemandSegmentWeightingMapper implements RowMapper<WeightingEntity> {
	
	public WeightingEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		DemandSegmentWeighting demandSegmentWeighting = new DemandSegmentWeighting();
		demandSegmentWeighting.setId(rs.getInt("dsw_id"));
		demandSegmentWeighting.setName(rs.getString("dsw_name"));
		demandSegmentWeighting.setSetEntityId(rs.getInt("dsw_segment_set"));
		return demandSegmentWeighting;
	}
	
}
