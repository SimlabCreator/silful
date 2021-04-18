package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.ResidenceAreaWeight;
import data.entity.WeightEntity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class ResidenceAreaWeightMapper implements RowMapper<WeightEntity> {
	
	public WeightEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		ResidenceAreaWeight residenceAreaWeight = new ResidenceAreaWeight();
		residenceAreaWeight.setId(rs.getInt("raw_id"));
		residenceAreaWeight.setSetId(rs.getInt("raw_set"));	
		residenceAreaWeight.setWeight(rs.getObject("raw_weight", Double.class));
		residenceAreaWeight.setElementId(rs.getObject("raw_residence_area", Integer.class));
		return residenceAreaWeight;
	}
	
}
