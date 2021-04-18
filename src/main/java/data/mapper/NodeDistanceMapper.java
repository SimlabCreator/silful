package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.NodeDistance;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class NodeDistanceMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		NodeDistance nodeDistance = new NodeDistance();
		nodeDistance.setNode1Id(rs.getLong("dis_node1"));
		nodeDistance.setNode2Id(rs.getLong("dis_node2"));
		nodeDistance.setDistance(rs.getObject("dis_value", Double.class));
		nodeDistance.setRegionId(rs.getObject("dis_region", Integer.class));
		return nodeDistance;
	}
	
}
