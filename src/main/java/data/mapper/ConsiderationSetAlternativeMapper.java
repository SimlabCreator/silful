package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.ConsiderationSetAlternative;
import data.entity.Entity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class ConsiderationSetAlternativeMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		ConsiderationSetAlternative considerationSetAlternative = new ConsiderationSetAlternative();
		considerationSetAlternative.setId(rs.getInt("csa_id"));
		considerationSetAlternative.setSetId(rs.getObject("csa_set", Integer.class));
		considerationSetAlternative.setDemandSegmentId(rs.getObject("csa_demand_segment", Integer.class));
		considerationSetAlternative.setAlternativeId(rs.getObject("csa_alternative", Integer.class));
		considerationSetAlternative.setPredecessorId(rs.getObject("csa_predecessor", Integer.class));
		considerationSetAlternative.setWeight(rs.getObject("csa_weight", Double.class));
		considerationSetAlternative.setCoefficient(rs.getObject("csa_coefficient", Double.class));	
		return considerationSetAlternative;
	}
	
}
