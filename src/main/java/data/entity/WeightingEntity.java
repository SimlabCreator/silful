package data.entity;

import java.util.ArrayList;

public abstract class WeightingEntity extends Entity{


	public abstract int getId();
	
	public abstract void setId(Integer id);
	
	public abstract Integer getSetEntityId();

	public abstract void setSetEntityId(Integer setEntityId);
	
	public abstract SetEntity getSetEntity();
	
	public abstract void setSetEntity(SetEntity setEntity);
	
	public abstract String getName();
	
	public abstract void setName(String name);

	
	public abstract ArrayList<? extends WeightEntity> getWeights();
	
	public abstract String toString();
}
