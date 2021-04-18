package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.ObjectiveWeight;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class ObjectiveWeightMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		ObjectiveWeight obj = new ObjectiveWeight();
		obj.setObjectiveTypeId(rs.getObject("exp_obj_obj", Integer.class));
		obj.setValue(rs.getObject("exp_obj_weight", Double.class));
		return obj;
	}
	
}
