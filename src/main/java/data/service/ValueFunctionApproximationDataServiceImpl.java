package data.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;

import data.entity.Entity;
import data.entity.SetEntity;
import data.entity.ValueFunctionApproximationCoefficient;
import data.entity.ValueFunctionApproximationModel;
import data.entity.ValueFunctionApproximationModelSet;
import data.entity.ValueFunctionApproximationType;
import data.mapper.ValueFunctionApproximationCoefficientMapper;
import data.mapper.ValueFunctionApproximationModelMapper;
import data.mapper.ValueFunctionApproximationModelSetMapper;
import data.mapper.ValueFunctionApproximationTypeMapper;

public class ValueFunctionApproximationDataServiceImpl extends ValueFunctionApproximationDataService{

	private ValueFunctionApproximationModelSet currentSet;
	
	@Override
	public ArrayList<SetEntity> getAllSetsByDeliveryAreaSetAndTimeWindowSetId(Integer deliveryAreaSetId,
			Integer timeWindowSetId, Integer modelTypeId) {
		ArrayList<SetEntity> sets  =DataLoadService.loadSetsByMultipleSelectionIds("value_function_approximation_model_set", new String[]{"vfs_time_window_set", "vfs_delivery_area_set", "vfs_type"}, new Integer[]{timeWindowSetId, deliveryAreaSetId, modelTypeId}, new ValueFunctionApproximationModelSetMapper(), jdbcTemplate);
		return sets;
	}

	@Override
	public ArrayList<ValueFunctionApproximationCoefficient> getAllCoefficients(int modelId) {
		ArrayList<ValueFunctionApproximationCoefficient> entities = (ArrayList<ValueFunctionApproximationCoefficient>) DataLoadService.loadMultipleRowsBySelectionId("value_function_approximation_coefficient", "vfc_model", modelId,
				new ValueFunctionApproximationCoefficientMapper(), jdbcTemplate);
		return entities;
	}

	@Override
	public ValueFunctionApproximationType getModelTypeById(int modelTypeId) {
		ValueFunctionApproximationType type = (ValueFunctionApproximationType) DataLoadService.loadById("value_function_approximation_type", "vft_id", modelTypeId, new ValueFunctionApproximationTypeMapper(),
				jdbcTemplate);
		
		return type;
	}

	

	@Override
	public ArrayList<ValueFunctionApproximationModel> getAllElementsBySetId(int setId) {
		ArrayList<ValueFunctionApproximationModel> entities = (ArrayList<ValueFunctionApproximationModel>) DataLoadService.loadMultipleRowsBySelectionId("value_function_approximation_model", "vfa_set", setId,
				new ValueFunctionApproximationModelMapper(), jdbcTemplate);
		this.currentSet=(ValueFunctionApproximationModelSet) this.getSetById(setId);
		this.currentSet.setElements(entities);
		return entities;
	}


	@Override
	public ValueFunctionApproximationModel getElementById(int entityId) {
		ValueFunctionApproximationModel model = (ValueFunctionApproximationModel) DataLoadService.loadById("value_function_approximation_model", "vfa_id", entityId, new ValueFunctionApproximationModelMapper(),
				jdbcTemplate);
		
		return model;
	}


	@Override
	public ArrayList<SetEntity> getAllSets() {
		if (entitySets == null) {

			entitySets = DataLoadService.loadAllFromSetClass("value_function_approximation_model_set", new ValueFunctionApproximationModelSetMapper(), jdbcTemplate);

		}

		return entitySets;
	}


	@Override
	public SetEntity getSetById(int id) {
		SetEntity valueFunctionApproximationModelSet = new ValueFunctionApproximationModelSet();

		if (entitySets == null) {
			valueFunctionApproximationModelSet = DataLoadService.loadBySetId("value_function_approximation_model_set", "vfs_id", id, new ValueFunctionApproximationModelSetMapper(),
					jdbcTemplate);
		} else {

			for (int i = 0; i < entitySets.size(); i++) {
				if ((entitySets.get(i)).getId() == id) {
					valueFunctionApproximationModelSet =  entitySets.get(i);
					return valueFunctionApproximationModelSet;
				}

			}

		}
		return valueFunctionApproximationModelSet;
	}


	@Override
	public Integer persistElement(Entity entity) {
		final ValueFunctionApproximationModel valueFunctionApproximationModel = (ValueFunctionApproximationModel) entity;
		final String SQL = DataLoadService.buildInsertSQL("value_function_approximation_model", 11,
				"vfa_set, vfa_delivery_area, vfa_basic_coefficient, vfa_time_coefficient, "
				+ "vfa_time_capacity_interaction_coefficient,vfs_subarea_model, "
				+ "vfa_area_potential_coefficient, vfa_remaining_capacity_coefficient,vfa_overall_cost_coefficient, vfa_overall_cost_type, vfa_complex_JSON");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "vfa_id" });
				ps.setObject(1, valueFunctionApproximationModel.getSetId(), Types.INTEGER);
				ps.setObject(2, valueFunctionApproximationModel.getDeliveryAreaId(), Types.INTEGER);
				ps.setObject(3, valueFunctionApproximationModel.getBasicCoefficient(), Types.DOUBLE);
				ps.setObject(4, valueFunctionApproximationModel.getTimeCoefficient(), Types.DOUBLE);
				ps.setObject(5, valueFunctionApproximationModel.getTimeCapacityInteractionCoefficient(), Types.DOUBLE);
				ps.setObject(6, valueFunctionApproximationModel.getSubAreaModel(), Types.LONGVARCHAR);
				ps.setObject(7, valueFunctionApproximationModel.getAreaPotentialCoefficient(),Types.DOUBLE);
				ps.setObject(8, valueFunctionApproximationModel.getRemainingCapacityCoefficient(),Types.DOUBLE);
				ps.setObject(9, valueFunctionApproximationModel.getAcceptedOverallCostCoefficient(), Types.DOUBLE);
				ps.setObject(10, valueFunctionApproximationModel.getAcceptedOverallCostType(), Types.INTEGER);
				ps.setObject(11, valueFunctionApproximationModel.getComplexModelJSON(), Types.LONGVARCHAR);
				return ps;
			}
		}, jdbcTemplate);
		
		valueFunctionApproximationModel.setId(id);
		this.persistCoefficients(valueFunctionApproximationModel.getCoefficients(), id);
		valueFunctionApproximationModel.setCoefficients(null);
		return id;
	}


	@Override
	public Integer persistCompleteEntitySet(SetEntity setEntity) {
		
		final ArrayList<ValueFunctionApproximationModel> modelsFinal = ((ValueFunctionApproximationModelSet) setEntity).getElements();
		
		final int setId = this.persistEntitySet(setEntity);

		for(ValueFunctionApproximationModel model: modelsFinal){
			model.setSetId(setId);
			int modelId=this.persistElement(model);
			
			try {
				this.persistLog(modelId, model.getObjectiveFunctionValueLog(), model.getWeightsLog());
			} catch (Exception e) {
			
				System.out.println("Could not save log!");
			}
		}

		return setId;
	}
	
	private void persistCoefficients(ArrayList<ValueFunctionApproximationCoefficient> coefficients, final int modelId){
		final ArrayList<ValueFunctionApproximationCoefficient> coefficientsFinal = coefficients;
		DataLoadService.persistAll("value_function_approximation_coefficient", 9,
				"vfc_model,vfc_delivery_area,vfc_time_window,vfc_coefficient, vfc_squared, vfc_costs,vfc_coverage, vfc_demand_capacity_ratio, vfc_type",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return coefficientsFinal.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						ValueFunctionApproximationCoefficient valueFunctionApproximationCoefficient = coefficientsFinal.get(i);
						ps.setInt(1, modelId);
						ps.setObject(3, valueFunctionApproximationCoefficient.getTimeWindowId(), Types.INTEGER);
						ps.setObject(2, valueFunctionApproximationCoefficient.getDeliveryAreaId(), Types.INTEGER);
						ps.setObject(4, valueFunctionApproximationCoefficient.getCoefficient(), Types.DOUBLE);
						ps.setObject(5, valueFunctionApproximationCoefficient.isSquared(), Types.BOOLEAN);
						ps.setObject(6, valueFunctionApproximationCoefficient.isCosts(), Types.BOOLEAN);
						ps.setObject(7, valueFunctionApproximationCoefficient.isCoverage(), Types.BOOLEAN);
						ps.setObject(8, valueFunctionApproximationCoefficient.isDemandCapacityRatio(), Types.BOOLEAN);
						ps.setObject(9, valueFunctionApproximationCoefficient.getType(), Types.VARCHAR);
			

					}
				}, jdbcTemplate);
	}


	@Override
	protected Integer persistEntitySet(SetEntity setEntity) {
		final ValueFunctionApproximationModelSet set = (ValueFunctionApproximationModelSet) setEntity;
		
		final String SQL = DataLoadService.buildInsertSQL("value_function_approximation_model_set", 7,
				"vfs_name, vfs_type, vfs_time_window_set, vfs_delivery_area_set, vfs_number_boolean, vfs_committed_boolean, vfs_area_weights_boolean");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "vfs_id" });
				ps.setObject(1, set.getName(), Types.VARCHAR);
				ps.setObject(2, set.getTypeId(), Types.INTEGER);
				ps.setObject(3, set.getTimeWindowSetId(), Types.INTEGER);
				ps.setObject(4, set.getDeliveryAreaSetId(), Types.INTEGER);
				ps.setObject(5, set.getIsNumber(), Types.BOOLEAN);
				ps.setObject(6, set.getIsCommitted(), Types.BOOLEAN);
				ps.setObject(7, set.getIsAreaSpecific(), Types.BOOLEAN);
				return ps;
			}
		}, jdbcTemplate);

		set.setId(id);
		set.setElements(null);
		
		return id;
	}
	
	private void persistLog(int modelId, HashMap<Integer, ArrayList<Double>> objectiveFunctionValueLog, HashMap<Integer, ArrayList<String>> weightsLog){
		
		final int modelIdFinal = modelId;
		
		final ArrayList<Integer> repetitionsFinal = new ArrayList<Integer>();
		
		
		for(Integer rId:objectiveFunctionValueLog.keySet()){
			for(int i=0; i < objectiveFunctionValueLog.get(rId).size(); i++){
				repetitionsFinal.add(rId);
			}
			
		}
		final int overallCount=repetitionsFinal.size()-1;
		final int amountPerId = overallCount/objectiveFunctionValueLog.keySet().size();
		final HashMap<Integer, ArrayList<Double>> objectiveFunctionValueLogFinal = objectiveFunctionValueLog;
		final HashMap<Integer, ArrayList<String>> weightsLogFinal = weightsLog;
		
		DataLoadService.persistAll("model_training_log", 4,
				"mtl_model,mtl_repetition,mtl_objective_function_value,mtl_coefficients",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return overallCount;
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						ps.setInt(1, modelIdFinal);
						ps.setObject(2, repetitionsFinal.get(i), Types.INTEGER);
						ps.setObject(3, objectiveFunctionValueLogFinal.get(repetitionsFinal.get(i)).get(i%amountPerId), Types.DOUBLE);
						ps.setObject(4, weightsLogFinal.get(repetitionsFinal.get(i)).get(i%amountPerId), Types.VARCHAR);

					}
				}, jdbcTemplate);
	}
}
