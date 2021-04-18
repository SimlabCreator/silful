package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.DemandSegmentSet;
import data.entity.SetEntity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class DemandSegmentSetMapper implements RowMapper<SetEntity> {
	
	public SetEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		DemandSegmentSet demandSegmentSet = new DemandSegmentSet();
		demandSegmentSet.setId(rs.getInt("dss_id"));
		demandSegmentSet.setName(rs.getObject("dss_name", String.class));
		demandSegmentSet.setDemandModelTypeId(rs.getObject("dss_demand_model_type", Integer.class));
		demandSegmentSet.setResidenceAreaSetId(rs.getObject("dss_residence_area_set", Integer.class));
		demandSegmentSet.setPanel(rs.getObject("dss_panel", Boolean.class));
		demandSegmentSet.setAlternativeSetId(rs.getObject("dss_alternative_set", Integer.class));
		return demandSegmentSet;
	}
	
}
