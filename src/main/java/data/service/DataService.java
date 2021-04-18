package data.service;

import java.util.ArrayList;

import org.springframework.jdbc.core.JdbcTemplate;

import data.entity.ConsiderationSetAlternative;

/**
 * Interface for all data services. Ensures common requests.
 * 
 * @author M. Lang
 *
 */
public abstract class DataService {

	protected JdbcTemplate jdbcTemplate;

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;

	}

	


	
	
}