package data.service;

import java.util.ArrayList;

import data.entity.Entity;
import data.entity.ObjectiveType;
import data.mapper.ObjectiveTypeMapper;

/**
 * Implementation for MySQL database
 * 
 * @author M. Lang
 *
 */
public class ObjectiveTypeDataServiceImpl extends ObjectiveTypeDataService{

	private ArrayList<Entity> objectives;
	
	public ArrayList<Entity> getAll() {
		
		if(objectives==null){
			
			objectives = DataLoadService.loadAllFromClass("objective", new ObjectiveTypeMapper(), jdbcTemplate);

		}
		
		return objectives;
	}

	public Entity getById(Integer id) {

		
		Entity obj = new ObjectiveType();
		
		if(objectives==null){
			obj = DataLoadService.loadById("objective", "obj_id", id, new ObjectiveTypeMapper(), jdbcTemplate);
		}else{
			
			for(int i=0; i < objectives.size(); i++){
				if(((ObjectiveType) objectives.get(i)).getId()==id) {
					obj=(ObjectiveType) objectives.get(i);
					return obj;
				}
				
			}
			
		}
		
	    return obj;
	}

}
