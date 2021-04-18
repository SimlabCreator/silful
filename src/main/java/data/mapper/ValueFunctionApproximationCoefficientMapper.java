package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.ValueFunctionApproximationCoefficient;
import logic.entity.ValueFunctionCoefficientType;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class ValueFunctionApproximationCoefficientMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		ValueFunctionApproximationCoefficient valueFunctionApproximationCoefficient = new ValueFunctionApproximationCoefficient();
		valueFunctionApproximationCoefficient.setModelId(rs.getObject("vfc_model", Integer.class));
		valueFunctionApproximationCoefficient.setDeliveryAreaId(rs.getObject("vfc_delivery_area", Integer.class));
		valueFunctionApproximationCoefficient.setTimeWindowId(rs.getObject("vfc_time_window", Integer.class));
		valueFunctionApproximationCoefficient.setCoefficient(rs.getObject("vfc_coefficient", Double.class));
		valueFunctionApproximationCoefficient.setSquared(rs.getObject("vfc_squared", Boolean.class));
		valueFunctionApproximationCoefficient.setCosts(rs.getObject("vfc_costs", Boolean.class));
		valueFunctionApproximationCoefficient.setCoverage(rs.getObject("vfc_coverage", Boolean.class));
		valueFunctionApproximationCoefficient.setDemandCapacityRatio(rs.getObject("vfc_demand_capacity_ratio", Boolean.class));
		valueFunctionApproximationCoefficient.setType(ValueFunctionCoefficientType.valueOf(rs.getObject("vfc_type", String.class).toUpperCase()));
		return valueFunctionApproximationCoefficient;
	}
	
}
