package data.entity;

import data.utility.DataServiceProvider;

public class ObjectiveWeight extends Entity {

	private int objectiveTypeId;
	private Double value;
	private ObjectiveType objectiveType;
	
	private int id;
	// No functionality, only for parent-class
	public int getId() {
		return id;
	}
	
	public int getObjectiveTypeId() {
		return objectiveTypeId;
	}
	public void setObjectiveTypeId(int objectiveTypeId) {
		this.objectiveTypeId = objectiveTypeId;
	}
	public Double getValue() {
		return value;
	}
	public void setValue(Double value) {
		this.value = value;
	}
	public ObjectiveType getObjectiveType() {
		if(this.objectiveType==null){
			this.objectiveType=(ObjectiveType) DataServiceProvider.getObjectiveTypeDataServiceImplInstance().getById(this.objectiveTypeId);
		}
		return objectiveType;
	}
	public void setObjectiveType(ObjectiveType objectiveType) {
		this.objectiveType = objectiveType;
	}
	
	

}
