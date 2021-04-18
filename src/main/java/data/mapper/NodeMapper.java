package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.Node;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class NodeMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		Node node = new Node();
		node.setId(rs.getLong("nod_id"));
		node.setRegionId(rs.getInt("nod_region"));
		node.setLat(rs.getObject("nod_lat", Double.class));
		node.setLon(rs.getObject("nod_long", Double.class));
		return node;
	}
	
}
