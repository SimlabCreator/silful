package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.DemandSegment;
import data.entity.Entity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class DemandSegmentMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		DemandSegment demandSegment = new DemandSegment();
		demandSegment.setId(rs.getInt("dem_id"));
		demandSegment.setSetId(rs.getInt("dem_set"));
		demandSegment.setPanel(rs.getObject("dem_panel", Boolean.class));	
		demandSegment.setBasketValueVolumeRatio(rs.getObject("dem_basket_volume_ratio", Integer.class));
		demandSegment.setBasketValueNoRatio(rs.getObject("dem_basket_no_ratio", Integer.class));
		demandSegment.setBasketValueDistributionId(rs.getObject("dem_basket_value_pd", Integer.class));
		demandSegment.setReturnProbabilityDistributionId(rs.getObject("dem_return_probability_pd", Integer.class));
		demandSegment.setResidenceAreaWeightingId(rs.getObject("dem_residence_area_weighting", Integer.class));
		demandSegment.setSocialImpactFactor(rs.getObject("dem_social_impact_factor", Double.class));
		demandSegment.setSegmentSpecificCoefficient(rs.getObject("dem_basic_utility", Double.class));
		demandSegment.setConsiderationSetId((Integer)rs.getObject("dem_consideration_set"));
		return demandSegment;
	}
	
}
