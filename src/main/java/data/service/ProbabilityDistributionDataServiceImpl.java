package data.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;

import data.entity.DistributionParameterValue;
import data.entity.Entity;
import data.entity.ProbabilityDistribution;
import data.mapper.DistributionParameterValueMapper;
import data.mapper.ProbabilityDistributionMapper;
/**
 * Implementation for MySQL database
 * 
 * @author M. Lang
 *
 */
public class ProbabilityDistributionDataServiceImpl extends ProbabilityDistributionDataService{

	
	private ArrayList<Entity> probabilityDistributions;
	
	public ArrayList<Entity> getAll() {
		
		if(probabilityDistributions==null){
			
			probabilityDistributions = DataLoadService.loadAllFromClass("probability_distribution", new ProbabilityDistributionMapper(), jdbcTemplate);

		}
		
		return probabilityDistributions;
	}

	public Entity getById(Integer id) {
		
		Entity entity = new ProbabilityDistribution();
		
		if(probabilityDistributions==null){
			entity = DataLoadService.loadById("probability_distribution", "pd_id", id, new ProbabilityDistributionMapper(), jdbcTemplate);
		}else{
			
			for(int i=0; i < probabilityDistributions.size(); i++){
				if(((ProbabilityDistribution) probabilityDistributions.get(i)).getId()==id) {
					entity=(ProbabilityDistribution) probabilityDistributions.get(i);
					return entity;
				}
				
			}
			
		}
		
	    return entity;
		
	}


	@Override
	public ArrayList<DistributionParameterValue> getParameterValuesByProbabilityDistributionId(int probabilityDistributionId) {

		ArrayList<DistributionParameterValue> entities = (ArrayList<DistributionParameterValue>) DataLoadService.loadMultipleRowsBySelectionId("distribution_parameter_value", "dpv_probability_distribution", probabilityDistributionId, new DistributionParameterValueMapper(), jdbcTemplate);
		return entities;
	}

	@Override
	public ArrayList<Entity> getProbabilityDistributionsByProbabilityDistributionTypeId(
			int probabilityDistributionTypeId) {

		ArrayList<Entity> entities = new ArrayList<Entity>();
		if(probabilityDistributions!=null){
			for(int i=0; i < probabilityDistributions.size();i++){
				if(((ProbabilityDistribution)probabilityDistributions.get(i)).getProbabilityDistributionTypeId()==probabilityDistributionTypeId) entities.add(probabilityDistributions.get(i));
			}
		}else{
			entities = (ArrayList<Entity>) DataLoadService.loadMultipleRowsBySelectionId("probability_distribution", "pd_type", probabilityDistributionTypeId, new ProbabilityDistributionMapper(), jdbcTemplate);
		}
		
		return entities;
	}

	@Override
	public int persistProbabilityDistribution(ProbabilityDistribution probabilityDistribution) {
		final ProbabilityDistribution probabilityDistributionToSave = probabilityDistribution;
		
		//Persist distribution
		final String SQL = DataLoadService.buildInsertSQL("probability_distribution", 2,
				"pd_name, pd_type");

		
		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "pd_id" });
				ps.setObject(1, probabilityDistributionToSave.getName(), Types.VARCHAR);
				ps.setObject(2, probabilityDistributionToSave.getProbabilityDistributionTypeId(), Types.INTEGER);


				return ps;
			}
		}, jdbcTemplate);
		
		final int probabilityDistributionId = id;
		

		//Persist parameter values
		DataLoadService.persistAll("distribution_parameter_value", 3,
				"dpv_probability_distribution, dpv_parameter_type, dpv_value",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return probabilityDistributionToSave.getParameterValues().size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						DistributionParameterValue paraValue =  probabilityDistributionToSave.getParameterValues().get(i);
						ps.setInt(1, probabilityDistributionId);
						ps.setInt(2, paraValue.getParameterTypeId());
						ps.setObject(3, paraValue.getValue(), Types.FLOAT);

					}
				}, jdbcTemplate);
		

		return id;
	}
	
	
	


}
