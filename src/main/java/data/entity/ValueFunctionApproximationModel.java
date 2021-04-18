package data.entity;

import java.util.ArrayList;
import java.util.HashMap;

import data.utility.DataServiceProvider;

public class ValueFunctionApproximationModel extends Entity{

	private int id;
	private int setId;
	private DeliveryArea deliveryArea;
	private Integer deliveryAreaId;
	private ArrayList<ValueFunctionApproximationCoefficient> coefficients;
	private Double basicCoefficient;
	private Double timeCoefficient;
	private Double timeCapacityInteractionCoefficient;
	private HashMap<Integer, ArrayList<Double>> objectiveFunctionValueLog;
	private HashMap<Integer, ArrayList<String>> weightsLog;
	private String subAreaModel; 
	private Double remainingCapacityCoefficient;
	private Double areaPotentialCoefficient;
	private Double acceptedOverallCostCoefficient;
	private Integer acceptedOverallCostType;
	private String complexModelJSON;
	
	public Double getRemainingCapacityCoefficient() {
		return remainingCapacityCoefficient;
	}
	public void setRemainingCapacityCoefficient(Double remainingCapacityCoefficient) {
		this.remainingCapacityCoefficient = remainingCapacityCoefficient;
	}
	public Double getAreaPotentialCoefficient() {
		return areaPotentialCoefficient;
	}
	public void setAreaPotentialCoefficient(Double areaPotentialCoefficient) {
		this.areaPotentialCoefficient = areaPotentialCoefficient;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getSetId() {
		return setId;
	}
	public void setSetId(int setId) {
		this.setId = setId;
	}
	public DeliveryArea getDeliveryArea() {
		if(this.deliveryArea==null) this.deliveryArea=DataServiceProvider.getDeliveryAreaDataServiceImplInstance().getElementById(this.deliveryAreaId);
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

	public ArrayList<ValueFunctionApproximationCoefficient> getCoefficients() {
		if(this.coefficients==null) this.coefficients = DataServiceProvider.getValueFunctionApproximationDataServiceImplInstance().getAllCoefficients(this.id);
		return coefficients;
	}


	public void setCoefficients(ArrayList<ValueFunctionApproximationCoefficient> coefficients) {
		this.coefficients = coefficients;
	}
	public Double getBasicCoefficient() {
		return basicCoefficient;
	}
	public void setBasicCoefficient(Double basicCoefficient) {
		this.basicCoefficient = basicCoefficient;
	}
	public Double getTimeCoefficient() {
		return timeCoefficient;
	}
	public void setTimeCoefficient(Double timeCoefficient) {
		this.timeCoefficient = timeCoefficient;
	}
	
	@Override
	public boolean equals(Object o){
	   if(this==o){
	      return true;
	   }
	   if(o instanceof ValueFunctionApproximationModel){
		   ValueFunctionApproximationModel other = (ValueFunctionApproximationModel) o;
	       return this.id == other.getId();
	   }
	   return false;
	}
	
	@Override
	public int hashCode(){
	   return this.id;
	}
	public HashMap<Integer, ArrayList<Double>> getObjectiveFunctionValueLog() {
		return objectiveFunctionValueLog;
	}
	public void setObjectiveFunctionValueLog(HashMap<Integer, ArrayList<Double>> objectiveFunctionValueLog) {
		this.objectiveFunctionValueLog = objectiveFunctionValueLog;
	}
	public HashMap<Integer, ArrayList<String>> getWeightsLog() {
		return weightsLog;
	}
	public void setWeightsLog(HashMap<Integer, ArrayList<String>> weightsLog) {
		this.weightsLog = weightsLog;
	}
	public Double getTimeCapacityInteractionCoefficient() {
		return timeCapacityInteractionCoefficient;
	}
	public void setTimeCapacityInteractionCoefficient(Double timeCapacityInteractionCoefficient) {
		this.timeCapacityInteractionCoefficient = timeCapacityInteractionCoefficient;
	}
	public String getSubAreaModel() {
		return subAreaModel;
	}
	public void setSubAreaModel(String subAreaModel) {
		this.subAreaModel = subAreaModel;
	}
	public Double getAcceptedOverallCostCoefficient() {
		return acceptedOverallCostCoefficient;
	}
	public void setAcceptedOverallCostCoefficient(Double acceptedOverallCostCoefficient) {
		this.acceptedOverallCostCoefficient = acceptedOverallCostCoefficient;
	}
	public String getComplexModelJSON() {
		return complexModelJSON;
	}
	public void setComplexModelJSON(String complexModelJSON) {
		this.complexModelJSON = complexModelJSON;
	}
	public Integer getAcceptedOverallCostType() {
		return acceptedOverallCostType;
	}
	public void setAcceptedOverallCostType(Integer acceptedOverallCostType) {
		this.acceptedOverallCostType = acceptedOverallCostType;
	}
	
	

}
