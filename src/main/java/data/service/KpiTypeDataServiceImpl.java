package data.service;

import java.util.ArrayList;

import data.entity.Entity;
import data.entity.KpiType;
import data.mapper.KpiTypeMapper;

/**
 * Implementation for MySQL database
 * 
 * @author M. Lang
 *
 */
public class KpiTypeDataServiceImpl extends KpiTypeDataService{

	private ArrayList<Entity> kpis;
	
	public ArrayList<Entity> getAll() {
		
		if(kpis==null){
			
			kpis = DataLoadService.loadAllFromClass("kpi", new KpiTypeMapper(), jdbcTemplate);

		}
		
		return kpis;
	}

	public Entity getById(Integer id) {

		
		Entity kpi = new KpiType();
		
		if(kpis==null){
			kpi = DataLoadService.loadById("kpi", "kpi_id", id, new KpiTypeMapper(), jdbcTemplate);
		}else{
			
			for(int i=0; i < kpis.size(); i++){
				if(((KpiType) kpis.get(i)).getId()==id) {
					kpi=(KpiType) kpis.get(i);
					return kpi;
				}
				
			}
			
		}
		
	    return kpi;
	}

}
