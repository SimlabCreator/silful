package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.DemandSegmentWeight;
import data.entity.WeightEntity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class DemandSegmentWeightMapper implements RowMapper<WeightEntity> {
	
	public WeightEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		DemandSegmentWeight demandSegmentWeight = new DemandSegmentWeight();
		demandSegmentWeight.setId(rs.getInt("dw_id"));
		demandSegmentWeight.setSetId(rs.getInt("dw_set"));	
		demandSegmentWeight.setWeight(rs.getObject("dw_weight", Double.class));
		demandSegmentWeight.setElementId(rs.getObject("dw_demand_segment", Integer.class));
		return demandSegmentWeight;
	}
	
}
