package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.ResidenceAreaWeighting;
import data.entity.WeightingEntity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class ResidenceAreaWeightingMapper implements RowMapper<WeightingEntity> {
	
	public WeightingEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		ResidenceAreaWeighting raWeighting = new ResidenceAreaWeighting();
		raWeighting.setId(rs.getInt("rws_id"));
		raWeighting.setName(rs.getString("rws_name"));
		raWeighting.setSetEntityId(rs.getInt("rws_residence_set"));
		return raWeighting;
	}
	
}
