package data.service;

import java.util.ArrayList;

import data.entity.Depot;
import data.entity.Entity;
import data.mapper.DepotMapper;

/**
 * Implementation for MySQL database
 * 
 * @author M. Lang
 *
 */
public class DepotDataServiceImpl extends DepotDataService{

	private ArrayList<Entity> depots;
	
	public ArrayList<Entity> getAll() {
		
		if(depots==null){
			
			depots = DataLoadService.loadAllFromClass("depot", new DepotMapper(), jdbcTemplate);

		}
		
		return depots;
	}

	public Entity getById(Integer id) {

		Entity depot = new Depot();
		
		if(depots==null){
			depot = DataLoadService.loadById("depot", "dep_id", id, new DepotMapper(), jdbcTemplate);
		}else{
			
			for(int i=0; i < depots.size(); i++){
				if(depots.get(i).getId()==id) {
					depot=depots.get(i);
					return depot;
				}
				
			}
			
		}
		
	    return depot;
	}

	@Override
	public ArrayList<Depot> getAllDepotsByRegionId(Integer regionId) {
		
		ArrayList<Depot> depots = (ArrayList<Depot>) DataLoadService.loadMultipleRowsBySelectionId("depot", "dep_region", regionId, new DepotMapper(), jdbcTemplate);
		return depots;
	}


}
