package data.service;

import java.util.ArrayList;

import data.entity.Entity;
import data.entity.VehicleType;
import data.mapper.VehicleTypeMapper;

/**
 * Implementation for MySQL database
 * 
 * @author M. Lang
 *
 */
public class VehicleTypeDataServiceImpl extends VehicleTypeDataService{

	private ArrayList<Entity> vehicleTypes;
	
	public ArrayList<Entity> getAll() {
		
		if(vehicleTypes==null){
			
			vehicleTypes = DataLoadService.loadAllFromClass("vehicle_type", new VehicleTypeMapper(), jdbcTemplate);

		}
		
		return vehicleTypes;
	}

	public Entity getById(Integer id) {
			
		Entity vehicleType = new VehicleType();
		
		if(vehicleTypes==null){
			vehicleType = DataLoadService.loadById("vehicle_type", "veh_id", id, new VehicleTypeMapper(), jdbcTemplate);
		}else{
			
			for(int i=0; i < vehicleTypes.size(); i++){
				if(((VehicleType) vehicleTypes.get(i)).getId()==id) {
					vehicleType=(VehicleType) vehicleTypes.get(i);
					return vehicleType;
				}
				
			}
			
		}
	    return vehicleType;
	}

}
