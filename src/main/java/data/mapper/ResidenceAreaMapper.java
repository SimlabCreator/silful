package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.ResidenceArea;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class ResidenceAreaMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		ResidenceArea residenceArea = new ResidenceArea();
		residenceArea.setId(rs.getInt("res_id"));
		residenceArea.setSetId(rs.getInt("res_residence_area_set"));
		residenceArea.setLat1(rs.getDouble("res_point1_lat"));
		residenceArea.setLon1(rs.getDouble("res_point1_long"));
		residenceArea.setLat2(rs.getDouble("res_point2_lat"));
		residenceArea.setLon2(rs.getDouble("res_point2_long"));
		residenceArea.setReasonableSubareaNumber(rs.getObject("res_reasonable_subarea_no", Integer.class));
		return residenceArea;
	}
	
}
