package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.DynamicProgrammingTree;
import data.entity.Entity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class DynamicProgrammingTreeMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		DynamicProgrammingTree tree = new DynamicProgrammingTree();
		tree.setId(rs.getObject("dpt_id", Integer.class));
		tree.setArrivalProcessId(rs.getObject("dpt_arrival_process", Integer.class));
		tree.setCapacitySetId(rs.getObject("dpt_capacity_set", Integer.class));
		tree.setDeliveryAreaSetId(rs.getObject("dpt_delivery_area_set", Integer.class));
		tree.setT(rs.getObject("dpt_t", Integer.class));
		tree.setDemandSegmentWeightingId(rs.getObject("dpt_demand_segment_weighting", Integer.class));
		tree.setName(rs.getObject("dpt_name", String.class));
		return tree;
	}
	
}
