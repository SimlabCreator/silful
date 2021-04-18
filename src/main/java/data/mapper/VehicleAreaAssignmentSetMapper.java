package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.SetEntity;
import data.entity.VehicleAreaAssignmentSet;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class VehicleAreaAssignmentSetMapper implements RowMapper<SetEntity> {
	
	public SetEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		VehicleAreaAssignmentSet ass = new VehicleAreaAssignmentSet();
		ass.setId(rs.getObject("vrs_id", Integer.class));
		ass.setDeliveryAreaSetId(rs.getObject("vrs_delivery_area_set", Integer.class));
		ass.setName(rs.getObject("vrs_name", String.class));
		return ass;
		
	}
}
