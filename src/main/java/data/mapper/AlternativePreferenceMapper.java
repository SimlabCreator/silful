package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.AlternativePreference;
import data.entity.Entity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class AlternativePreferenceMapper implements RowMapper<Entity> {

	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		AlternativePreference alternativePref = new AlternativePreference();
		alternativePref.setAlternativeId(rs.getInt("re_alt_alt"));
		alternativePref.setOrderRequestId(rs.getInt("re_alt_re"));
		alternativePref.setUtilityValue(rs.getObject("utility", Double.class));
		return alternativePref;
	}

}
