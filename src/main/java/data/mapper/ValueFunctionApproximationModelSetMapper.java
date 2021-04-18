package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.SetEntity;
import data.entity.ValueFunctionApproximationModelSet;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class ValueFunctionApproximationModelSetMapper implements RowMapper<SetEntity> {
	
	public SetEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		ValueFunctionApproximationModelSet valueFunctionApproximationModelSet = new ValueFunctionApproximationModelSet();
		valueFunctionApproximationModelSet.setId(rs.getInt("vfs_id"));
		valueFunctionApproximationModelSet.setName(rs.getObject("vfs_name", String.class));
		valueFunctionApproximationModelSet.setDeliveryAreaSetId(rs.getObject("vfs_delivery_area_set", Integer.class));
		valueFunctionApproximationModelSet.setTimeWindowSetId(rs.getObject("vfs_time_window_set", Integer.class));
		valueFunctionApproximationModelSet.setTypeId(rs.getObject("vfs_type", Integer.class));
		valueFunctionApproximationModelSet.setIsNumber(rs.getObject("vfs_number_boolean", Boolean.class));
		valueFunctionApproximationModelSet.setIsCommitted(rs.getObject("vfs_committed_boolean", Boolean.class));
		valueFunctionApproximationModelSet.setIsAreaSpecific(rs.getObject("vfs_area_weights_boolean", Boolean.class));
		
		return valueFunctionApproximationModelSet;
	}
	
}
