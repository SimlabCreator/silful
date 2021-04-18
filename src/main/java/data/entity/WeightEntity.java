package data.entity;

public abstract class WeightEntity extends Entity{


	public abstract int getId();
	
	public abstract void setId(Integer id);
	
	public abstract Integer getSetId();

	public abstract void setSetId(Integer setId);
	
	public abstract Integer getElementId();
	
	public abstract void setElementId(Integer elementId);
	
	public abstract Double getWeight();
	
	public abstract void setWeight(Double weight);
}
