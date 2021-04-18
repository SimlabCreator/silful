package data.entity;

import data.utility.DataServiceProvider;
import logic.entity.ValueFunctionCoefficientType;

public class ValueFunctionApproximationCoefficient extends Entity{

	private Integer modelId;
	private ValueFunctionApproximationModel model;
	private DeliveryArea deliveryArea;
	private Integer deliveryAreaId;
	private Integer timeWindowId;
	private TimeWindow timeWindow;
	private double coefficient;
	private boolean squared;
	private boolean costs;
	private boolean coverage; 
	private boolean demandCapacityRatio;
	private ValueFunctionCoefficientType type;

	
	public Integer getModelId() {
		return modelId;
	}
	public void setModelId(Integer modelId) {
		this.modelId = modelId;
	}
	public ValueFunctionApproximationModel getModel() {
		if(this.model==null) this.model = DataServiceProvider.getValueFunctionApproximationDataServiceImplInstance().getElementById(this.modelId);
		return model;
	}
	public void setModel(ValueFunctionApproximationModel model) {
		this.model = model;
	}
	public DeliveryArea getDeliveryArea() {
		if(this.deliveryArea==null) this.deliveryArea = DataServiceProvider.getDeliveryAreaDataServiceImplInstance().getElementById(this.deliveryAreaId);
		return deliveryArea;
	}
	public void setDeliveryArea(DeliveryArea deliveryArea) {
		this.deliveryArea = deliveryArea;
	}
	public Integer getDeliveryAreaId() {
		return deliveryAreaId;
	}
	public void setDeliveryAreaId(Integer deliveryAreaId) {
		this.deliveryAreaId = deliveryAreaId;
	}
	public Integer getTimeWindowId() {
		return timeWindowId;
	}
	public void setTimeWindowId(Integer timeWindowId) {
		this.timeWindowId = timeWindowId;
	}
	public TimeWindow getTimeWindow() {
		if(this.timeWindow==null) this.timeWindow = DataServiceProvider.getTimeWindowDataServiceImplInstance().getElementById(this.timeWindowId);
		return timeWindow;
	}
	public void setTimeWindow(TimeWindow timeWindow) {
		this.timeWindow = timeWindow;
	}
	public double getCoefficient() {
		return coefficient;
	}
	public void setCoefficient(double coefficient) {
		this.coefficient = coefficient;
	}
	
	
	@Override
	public int getId() { //TODO no functionality
		// TODO Auto-generated method stub
		return 0;
	}
	public boolean isSquared() {
		return squared;
	}
	public void setSquared(boolean squared) {
		this.squared = squared;
	}
	public boolean isCosts() {
		return costs;
	}
	public void setCosts(boolean costs) {
		this.costs = costs;
	}
	public boolean isCoverage() {
		return coverage;
	}
	public void setCoverage(boolean coverage) {
		this.coverage = coverage;
	}
	public boolean isDemandCapacityRatio() {
		return demandCapacityRatio;
	}
	public void setDemandCapacityRatio(boolean demandCapacityRatio) {
		this.demandCapacityRatio = demandCapacityRatio;
	}
	public ValueFunctionCoefficientType getType() {
		return type;
	}
	public void setType(ValueFunctionCoefficientType type) {
		this.type = type;
	}
	
	
	
	

}
