package data.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;

import data.entity.Entity;
import data.entity.ProcessType;
import data.mapper.ProcessTypeMapper;

/**
 * Implementation for MySQL database
 * 
 * @author M. Lang
 *
 */
public class ProcessTypeDataServiceImpl extends ProcessTypeDataService{

	private ArrayList<Entity> processes;
	
	public ArrayList<Entity> getAll() {
		
		if(processes==null){
			
			processes = DataLoadService.loadAllFromClass("process", new ProcessTypeMapper(), jdbcTemplate);

		}
		
		return processes;
	}

	public Entity getById(Integer id) {

		
		Entity process = new ProcessType();
		
		if(processes==null){
			process = DataLoadService.loadById("process", "pro_id", id, new ProcessTypeMapper(), jdbcTemplate);
		}else{
			
			for(int i=0; i < processes.size(); i++){
				if(((ProcessType) processes.get(i)).getId()==id) {
					process=(ProcessType) processes.get(i);
					return process;
				}
				
			}
			
		}
		
	    return process;
	}

	public Integer persist(Entity entity) {
		
		final ProcessType process = (ProcessType) entity;
		final String SQL = DataLoadService.buildInsertSQL("process", 2, "pro_name, pro_description");

		Integer id=DataLoadService.persist(
    	    new PreparedStatementCreator() {
    	        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
    	            PreparedStatement ps =
    	                con.prepareStatement(SQL, new String[] {"pro_id"});
    	            ps.setString(1, process.getName());
    	            ps.setString(2, process.getDescription());
    	            return ps;
    	        }
    	    },
    	    jdbcTemplate);
		
		process.setId(id);
		processes.add(process);
		
    	return id;
		
	}

	public void persistAll(final ArrayList<Entity> entities){

		DataLoadService.persistAll("process", 2, "pro_name, pro_description", new BatchPreparedStatementSetter() {
			
			
						    public int getBatchSize() {
						        return entities.size();
						    }
			
							public void setValues(PreparedStatement ps, int i) throws SQLException {
								
								ProcessType processType = (ProcessType) entities.get(i);
								ps.setString(1, processType.getName());
								ps.setString(2, processType.getDescription());
								
							}
						  }, jdbcTemplate);
		
		processes=null;
		getAll();
	}
	

}
