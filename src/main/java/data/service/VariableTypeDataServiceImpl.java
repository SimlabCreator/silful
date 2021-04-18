package data.service;

import java.util.ArrayList;

import data.entity.Entity;
import data.entity.VariableType;
import data.mapper.VariableTypeMapper;

/**
 * Implementation for MySQL database
 * 
 * @author M. Lang
 *
 */
public class VariableTypeDataServiceImpl extends VariableTypeDataService{

	private ArrayList<Entity> types;
	
	public ArrayList<Entity> getAll() {
		
		if(types==null){
			
			types = DataLoadService.loadAllFromClass("variable_type", new VariableTypeMapper(), jdbcTemplate);

		}
		
		return types;
	}

	public Entity getById(Integer id) {

		
		Entity variableType = new VariableType();
		
		if(types==null){
			variableType = DataLoadService.loadById("variable_type", "var_id", id, new VariableTypeMapper(), jdbcTemplate);
		}else{
			
			for(int i=0; i < types.size(); i++){
				if(((VariableType) types.get(i)).getId()==id) {
					variableType=(VariableType) types.get(i);
					return variableType;
				}
				
			}
			
		}
		
	    return variableType;
	}

}
