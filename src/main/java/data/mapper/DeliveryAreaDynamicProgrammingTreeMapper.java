package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class DeliveryAreaDynamicProgrammingTreeMapper implements RowMapper<Entity> {
	HashMap<Integer, String>trees;
	
	public DeliveryAreaDynamicProgrammingTreeMapper(HashMap<Integer, String>trees){
		this.trees=trees;
	}
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		trees.put(rs.getInt("dpt_da_da"), rs.getObject("dpt_da_tree",String.class));
		return null;
	}
	
}
