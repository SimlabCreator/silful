package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.RoutingAssignment;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class RoutingAssignmentMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		RoutingAssignment routingAssignment = new RoutingAssignment();
		routingAssignment.setPeriod(rs.getInt("exp_rou_period"));
		routingAssignment.setRoutingId(rs.getInt("exp_rou_rou"));
		routingAssignment.setT(rs.getInt("exp_rou_t"));
		
		return routingAssignment;
	}
}
