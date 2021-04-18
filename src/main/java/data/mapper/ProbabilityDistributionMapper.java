package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.ProbabilityDistribution;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class ProbabilityDistributionMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		ProbabilityDistribution probDis = new ProbabilityDistribution();
		probDis.setId(rs.getInt("pd_id"));
		probDis.setName(rs.getString("pd_name"));
		probDis.setProbabilityDistributionTypeId(rs.getObject("pd_type", Integer.class));
		return probDis;
	}
}
