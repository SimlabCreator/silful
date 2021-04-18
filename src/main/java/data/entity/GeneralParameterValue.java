package data.entity;

import data.utility.DataServiceProvider;

public class GeneralParameterValue extends Entity{
	
	private int parameterTypeId;
	private Double value;
	private ParameterType parameterType;
	private int experimentId;
	
	private int id;
	// No functionality, only for parent-class
	public int getId() {
		return id;
	}
	
	public int getParameterTypeId() {
		return parameterTypeId;
	}
	public void setParameterTypeId(int parameterTypeId) {
		this.parameterTypeId = parameterTypeId;
	}
	public Double getValue() {
		return value;
	}
	public void setValue(Double value) {
		this.value = value;
	}
	
	public ParameterType getParameterType() {
		if(parameterType==null){
			parameterType = (ParameterType) DataServiceProvider.getParameterTypeDataServiceImplInstance().getById(this.parameterTypeId);
		}
		return parameterType;
	}
	
	public void setParameterType(ParameterType parameterType) {
		this.parameterType = parameterType;
	}
	public int getExperimentId() {
		return experimentId;
	}
	public void setExperimentId(int experimentId) {
		this.experimentId = experimentId;
	}

	

}
