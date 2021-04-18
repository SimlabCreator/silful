package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.ProbabilityDistributionType;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class ProbabilityDistributionTypeMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		ProbabilityDistributionType probDisType = new ProbabilityDistributionType();
		probDisType.setId(rs.getInt("pdt_id"));
		probDisType.setName(rs.getString("pdt_name"));
		return probDisType;
	}
}
