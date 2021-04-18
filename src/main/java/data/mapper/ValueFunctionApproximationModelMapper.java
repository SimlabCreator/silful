package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.ValueFunctionApproximationModel;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class ValueFunctionApproximationModelMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		ValueFunctionApproximationModel valueFunctionApproximationModel = new ValueFunctionApproximationModel();
		valueFunctionApproximationModel.setId(rs.getInt("vfa_id"));
		valueFunctionApproximationModel.setSetId(rs.getInt("vfa_set"));
		valueFunctionApproximationModel.setDeliveryAreaId((Integer) rs.getObject("vfa_delivery_area"));
		valueFunctionApproximationModel.setBasicCoefficient((Double) rs.getObject("vfa_basic_coefficient"));
		valueFunctionApproximationModel.setTimeCoefficient((Double) rs.getObject("vfa_time_coefficient"));
		valueFunctionApproximationModel.setTimeCapacityInteractionCoefficient((Double) rs.getObject("vfa_time_capacity_interaction_coefficient"));
		valueFunctionApproximationModel.setSubAreaModel(rs.getObject("vfs_subarea_model", String.class));
		valueFunctionApproximationModel.setAreaPotentialCoefficient((Double) rs.getObject("vfa_area_potential_coefficient"));
		valueFunctionApproximationModel.setRemainingCapacityCoefficient((Double) rs.getObject("vfa_remaining_capacity_coefficient"));
		valueFunctionApproximationModel.setAcceptedOverallCostCoefficient((Double) rs.getObject("vfa_overall_cost_coefficient"));
		valueFunctionApproximationModel.setAcceptedOverallCostType((Integer) rs.getObject("vfa_overall_cost_type"));	
		valueFunctionApproximationModel.setComplexModelJSON(rs.getObject("vfa_complex_JSON", String.class));
		return valueFunctionApproximationModel;
	}
	
}
