package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.AlternativeSet;
import data.entity.SetEntity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class AlternativeSetMapper implements RowMapper<SetEntity> {
	
	public SetEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		AlternativeSet alternativeSet = new AlternativeSet();
		alternativeSet.setId(rs.getInt("as_id"));
		alternativeSet.setName(rs.getString("as_name"));
		alternativeSet.setTimeWindowSetId(rs.getInt("as_tws"));
		return alternativeSet;
	}
	
}
