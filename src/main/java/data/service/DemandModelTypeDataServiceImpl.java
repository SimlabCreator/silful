package data.service;

import java.util.ArrayList;

import data.entity.DemandModelType;
import data.entity.Entity;
import data.mapper.DemandModelTypeMapper;

/**
 * Implementation for MySQL database
 * 
 * @author M. Lang
 *
 */
public class DemandModelTypeDataServiceImpl extends DemandModelTypeDataService{

	private ArrayList<Entity> types;
	
	public ArrayList<Entity> getAll() {
		
		if(types==null){
			
			types = DataLoadService.loadAllFromClass("demand_model_type", new DemandModelTypeMapper(), jdbcTemplate);

		}
		
		return types;
	}

	public Entity getById(Integer id) {

		
		Entity demandModelType = new DemandModelType();
		
		if(types==null){
			demandModelType = DataLoadService.loadById("demand_model_type", "dmt_id", id, new DemandModelTypeMapper(), jdbcTemplate);
		}else{
			
			for(int i=0; i < types.size(); i++){
				if(((DemandModelType) types.get(i)).getId()==id) {
					demandModelType=(DemandModelType) types.get(i);
					return demandModelType;
				}
				
			}
			
		}
		
	    return demandModelType;
	}

}
