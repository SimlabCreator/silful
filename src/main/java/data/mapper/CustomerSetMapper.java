package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.CustomerSet;
import data.entity.SetEntity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class CustomerSetMapper implements RowMapper<SetEntity> {
	
	public SetEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		CustomerSet customerSet = new CustomerSet();
		customerSet.setId(rs.getInt("cs_id"));
		customerSet.setName(rs.getString("cs_name"));
		customerSet.setPanel(rs.getBoolean("cs_panel"));
		customerSet.setExtension(rs.getBoolean("cs_extension"));
		customerSet.setOriginalDemandSegmentSetId(rs.getObject("cs_original_demand_segment_set", Integer.class));
		return customerSet;
	}
	
}
