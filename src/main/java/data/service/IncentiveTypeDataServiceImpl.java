package data.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;

import data.entity.Entity;
import data.entity.IncentiveType;
import data.mapper.IncentiveTypeMapper;

/**
 * Implementation for MySQL database
 * 
 * @author M. Lang
 *
 */
public class IncentiveTypeDataServiceImpl extends IncentiveTypeDataService{

	private ArrayList<Entity> incentives;
	
	public ArrayList<Entity> getAll() {
		
		if(incentives==null){
			
			incentives = DataLoadService.loadAllFromClass("incentive_type", new IncentiveTypeMapper(), jdbcTemplate);

		}
		
		return incentives;
	}

	public Entity getById(Integer id) {
			
		Entity incentive = new IncentiveType();
		
		if(incentives==null){
			incentive = DataLoadService.loadById("incentive_type", "it_id", id, new IncentiveTypeMapper(), jdbcTemplate);
		}else{
			
			for(int i=0; i < incentives.size(); i++){
				if(((IncentiveType) incentives.get(i)).getId()==id) {
					incentive=(IncentiveType) incentives.get(i);
					return incentive;
				}
				
			}
			
		}
	    return incentive;
	}

	public Integer persist(Entity entity) {
		
		final IncentiveType incentive = (IncentiveType) entity;
		final String SQL = DataLoadService.buildInsertSQL("incentive_type", 1, "it_name");

		Integer id=DataLoadService.persist(
    	    new PreparedStatementCreator() {
    	        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
    	            PreparedStatement ps =
    	                con.prepareStatement(SQL, new String[] {"it_id"});
    	            ps.setString(1, incentive.getName());
    	            return ps;
    	        }
    	    },
    	    jdbcTemplate);
		
		incentive.setId(id);
		incentives.add(incentive);
		
    	return id;
		
	}

	public void persistAll(final ArrayList<Entity> entities){

		DataLoadService.persistAll("incentive_type", 1, "it_name", new BatchPreparedStatementSetter() {
			
			
						    public int getBatchSize() {
						        return entities.size();
						    }
			
							public void setValues(PreparedStatement ps, int i) throws SQLException {
								
								IncentiveType incentiveType = (IncentiveType) entities.get(i);
								ps.setString(1, incentiveType.getName());
								
							}
						  }, jdbcTemplate);
		
		incentives=null;
		getAll();
	}
	

}
