package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.VariableCoefficient;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class VariableCoefficientMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		VariableCoefficient vc = new VariableCoefficient();
		vc.setDemandSegmentId(rs.getObject("dem_variable_dem",Integer.class));
		vc.setVariableTypeId(rs.getObject("dem_variable_var", Integer.class));
		vc.setCoefficientValue(rs.getObject("dem_variable_coefficient", Double.class));
		return vc;
	}
	
}
