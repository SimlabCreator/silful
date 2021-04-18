package data.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;

import data.entity.Entity;
import data.entity.OrderContentType;
import data.mapper.OrderContentTypeMapper;

/**
 * Implementation for MySQL database
 * 
 * @author M. Lang
 *
 */
public class OrderContentTypeDataServiceImpl extends OrderContentTypeDataService{

	private ArrayList<Entity> orderContentTypes;
	
	public ArrayList<Entity> getAll() {
		
		if(orderContentTypes==null){
			
			orderContentTypes = DataLoadService.loadAllFromClass("order_content_type", new OrderContentTypeMapper(), jdbcTemplate);

		}
		
		return orderContentTypes;
	}

	public Entity getById(Integer id) {
			
		Entity orderContentType = new OrderContentType();
		
		if(orderContentTypes==null){
			orderContentType = DataLoadService.loadById("order_content_type", "oct_id", id, new OrderContentTypeMapper(), jdbcTemplate);
		}else{
			
			for(int i=0; i < orderContentTypes.size(); i++){
				if(((OrderContentType) orderContentTypes.get(i)).getId()==id) {
					orderContentType=(OrderContentType) orderContentTypes.get(i);
					return orderContentType;
				}
				
			}
			
		}
	    return orderContentType;
	}

	public Integer persist(Entity entity) {
		
		final OrderContentType orderContentType = (OrderContentType) entity;
		final String SQL = DataLoadService.buildInsertSQL("order_content_type", 1, "oct_name");

		Integer id=DataLoadService.persist(
    	    new PreparedStatementCreator() {
    	        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
    	            PreparedStatement ps =
    	                con.prepareStatement(SQL, new String[] {"oct_id"});
    	            ps.setString(1, orderContentType.getName());
    	            return ps;
    	        }
    	    },
    	    jdbcTemplate);
		
		orderContentType.setId(id);
		orderContentTypes.add(orderContentType);
		
    	return id;
		
	}

	public void persistAll(final ArrayList<Entity> entities){

		DataLoadService.persistAll("order_content_type", 1, "oct_name", new BatchPreparedStatementSetter() {
			
			
						    public int getBatchSize() {
						        return entities.size();
						    }
			
							public void setValues(PreparedStatement ps, int i) throws SQLException {
								
								OrderContentType orderContentType = (OrderContentType) entities.get(i);
								ps.setString(1, orderContentType.getName());
								
							}
						  }, jdbcTemplate);
		
		orderContentTypes=null;
		getAll();
	}
	

}
