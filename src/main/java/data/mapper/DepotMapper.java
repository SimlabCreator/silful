package data.mapper;


import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Depot;
import data.entity.Entity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class DepotMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		Depot depot = new Depot();
		depot.setId(rs.getInt("dep_id"));
		depot.setLon(rs.getObject("dep_long", Double.class));
		depot.setLat(rs.getObject("dep_lat", Double.class));
		depot.setRegionId(rs.getObject("dep_region", Integer.class));
		return depot;
	}
	
}
