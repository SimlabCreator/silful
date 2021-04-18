package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.ResidenceAreaSet;
import data.entity.SetEntity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class ResidenceAreaSetMapper implements RowMapper<SetEntity> {
	
	public SetEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		ResidenceAreaSet residenceAreaSet = new ResidenceAreaSet();
		residenceAreaSet.setId(rs.getInt("ras_id"));
		residenceAreaSet.setName(rs.getString("ras_name"));
		residenceAreaSet.setDescription(rs.getString("ras_description"));
		residenceAreaSet.setRegionId(rs.getInt("ras_region"));
		return residenceAreaSet;
	}
	
}
