package data.service;

import java.util.ArrayList;

import data.entity.Entity;
import data.entity.ProbabilityDistributionType;
import data.mapper.ProbabilityDistributionTypeMapper;
/**
 * Implementation for MySQL database
 * 
 * @author M. Lang
 *
 */
public class ProbabilityDistributionTypeDataServiceImpl extends ProbabilityDistributionTypeDataService{

	
	private ArrayList<Entity> probabilityDistributionTypes;
	
	public ArrayList<Entity> getAll() {
		
		if(probabilityDistributionTypes==null){
			
			probabilityDistributionTypes = DataLoadService.loadAllFromClass("probability_distribution_type", new ProbabilityDistributionTypeMapper(), jdbcTemplate);

		}
		probabilityDistributionTypes.size();
		
		return probabilityDistributionTypes;
	}

	public Entity getById(Integer id) {
		
		Entity entity = new ProbabilityDistributionType();
		
		if(probabilityDistributionTypes==null){
			entity = DataLoadService.loadById("probability_distribution_type", "pdt_id", id, new ProbabilityDistributionTypeMapper(), jdbcTemplate);
		}else{
			
			for(int i=0; i < probabilityDistributionTypes.size(); i++){
				if(((ProbabilityDistributionType) probabilityDistributionTypes.get(i)).getId()==id) {
					entity=(ProbabilityDistributionType) probabilityDistributionTypes.get(i);
					return entity;
				}
				
			}
			
		}
		
	    return entity;
		
	}

//	@Override
//	public ArrayList<Entity> getParameterTypesByProbabilityDistributionTypeId(int probabilityDistributionTypeId) {
//		String sql = "SELECT parameter_type.par_id AS par_id, parameter_type.name AS par_name FROM (SELECT pp.pdt_parameter_par AS para FROM "+SettingsProvider.database+".r_pd_type_v_parameter_type AS pp WHERE pp.pdt_parameter_pd=?) AS paraId"+
//				"LEFT JOIN "+SettingsProvider.database+".parameter_type ON(parameter_type.par_id=paraId.para);";
//
//		ArrayList<Entity> entities = DataLoadService.loadComplexPreparedStatementMultipleEntities(sql, new Object[]{probabilityDistributionTypeId},
//		new ParameterTypeMapper(), jdbcTemplate);
//		return entities;
//	}
	
	


}
