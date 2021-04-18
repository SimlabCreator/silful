package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.IncentiveType;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class IncentiveTypeMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		IncentiveType incentive = new IncentiveType();
		incentive.setId(rs.getInt("it_id"));
		incentive.setName(rs.getString("it_name"));
		return incentive;
	}
	
}
