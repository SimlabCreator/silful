package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Alternative;
import data.entity.Entity;
import logic.entity.ValueFunctionCoefficientType;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class AlternativeMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		Alternative alternative = new Alternative();
		alternative.setId(rs.getInt("alt_id"));
		alternative.setSetId(rs.getInt("alt_set"));
		alternative.setTimeOfDay(rs.getObject("alt_time_of_day", String.class));
		alternative.setNoPurchaseAlternative(rs.getObject("alt_no_purchase_alternative", Boolean.class));
		return alternative;
	}
	
}
