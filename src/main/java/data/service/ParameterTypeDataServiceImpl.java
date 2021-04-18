package data.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;

import data.entity.Entity;
import data.entity.ParameterType;
import data.mapper.ParameterTypeMapper;
/**
 * Implementation for MySQL database
 * 
 * @author M. Lang
 *
 */
public class ParameterTypeDataServiceImpl extends ParameterTypeDataService{

	
	private ArrayList<Entity> parameters;
	
	public ArrayList<Entity> getAll() {
		
		if(parameters==null){
			
			parameters = DataLoadService.loadAllFromClass("parameter_type", new ParameterTypeMapper(), jdbcTemplate);

		}
		parameters.size();
		return parameters;
	}

	public Entity getById(Integer id) {
		
		Entity parameter = new ParameterType();
		
		if(parameters==null){
			parameter = DataLoadService.loadById("parameter_type", "par_id", id, new ParameterTypeMapper(), jdbcTemplate);
		}else{
			
			for(int i=0; i < parameters.size(); i++){
				if(((ParameterType) parameters.get(i)).getId()==id) {
					parameter=(ParameterType) parameters.get(i);
					return parameter;
				}
				
			}
			
		}
		
	    return parameter;
		
	}

	public Integer persist(Entity entity) {
		
		final ParameterType parameter = (ParameterType) entity;
		final String SQL = DataLoadService.buildInsertSQL("parameter_type", 2, "par_name, par_description");

		Integer id=DataLoadService.persist(
    	    new PreparedStatementCreator() {
    	        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
    	            PreparedStatement ps =
    	                con.prepareStatement(SQL, new String[] {"par_id"});
    	            ps.setString(1, parameter.getName());
    	            ps.setString(2, parameter.getDescription());
    	            return ps;
    	        }
    	    },
    	    jdbcTemplate);
		
		parameter.setId(id);
		parameters.add(parameter);
		
    	return id;
    	
		
	}

	public void persistAll(final ArrayList<Entity> entities) {
		
		DataLoadService.persistAll("parameter_type", 2, "par_name, par_description", new BatchPreparedStatementSetter() {
			
			
		    public int getBatchSize() {
		        return entities.size();
		    }

			public void setValues(PreparedStatement ps, int i) throws SQLException {
				
				ParameterType parameter = (ParameterType) entities.get(i);
				ps.setString(1, parameter.getName());
				ps.setString(2, parameter.getDescription());
				
			}
		  }, jdbcTemplate);

		parameters=null;
		getAll();
		
	}


	
	


}
