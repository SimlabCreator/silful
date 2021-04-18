package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.DeliverySet;
import data.entity.SetEntity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class DeliverySetMapper implements RowMapper<SetEntity> {
	
	public SetEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		DeliverySet deliverySet = new DeliverySet();
		deliverySet.setId(rs.getInt("des_id"));
		deliverySet.setName(rs.getObject("des_name", String.class));

		return deliverySet;
	}
	
}
