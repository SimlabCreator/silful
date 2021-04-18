package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Control;
import data.entity.Entity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class ControlMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		Control control = new Control();
		control.setId(rs.getInt("con_id"));
		control.setSetId(rs.getInt("con_set"));
		control.setDeliveryAreaId(rs.getObject("con_delivery_area", Integer.class));
		control.setAlternativeId(rs.getObject("con_alternative", Integer.class));
		control.setControlNumber(rs.getObject("con_no", Integer.class));
		control.setValueBucketId(rs.getObject("con_value_bucket", Integer.class));
		return control;
	}
	
}
