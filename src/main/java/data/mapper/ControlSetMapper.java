package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.ControlSet;
import data.entity.SetEntity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class ControlSetMapper implements RowMapper<SetEntity> {
	
	public SetEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		ControlSet controlSet = new ControlSet();
		controlSet.setId(rs.getInt("cos_id"));
		controlSet.setName(rs.getObject("cos_name", String.class));
		controlSet.setDeliveryAreaSetId(rs.getObject("cos_delivery_area_set", Integer.class));
		controlSet.setAlternativeSetId(rs.getObject("cos_alternative_set", Integer.class));
		controlSet.setValueBucketSetId(rs.getObject("cos_value_bucket_set", Integer.class));
		return controlSet;
	}
	
}
