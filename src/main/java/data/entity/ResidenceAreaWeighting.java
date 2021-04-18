package data.entity;

import java.util.ArrayList;

import data.utility.DataServiceProvider;

public class ResidenceAreaWeighting extends WeightingEntity {

	private Integer id;
	private String name;
	private ArrayList<ResidenceAreaWeight> residenceAreaWeights;
	private Integer residenceAreaSetId;
	private SetEntity residenceAreaSet;

	
	public ArrayList<ResidenceAreaWeight> getWeights() {
		
		if(this.residenceAreaWeights==null){
			this.residenceAreaWeights = DataServiceProvider.getResidenceAreaDataServiceImplInstance().getAllWeightsByWeightingId(this.id);
		}
		return residenceAreaWeights;
	}


	public void setWeights(ArrayList<ResidenceAreaWeight> elements) {
		this.residenceAreaWeights = elements;

	}


	public void addWeight(ResidenceAreaWeight element) {
		if (this.residenceAreaWeights == null) {
			this.residenceAreaWeights = new ArrayList<ResidenceAreaWeight>();
		}

		this.residenceAreaWeights.add(element);
	}

	public int getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getSetEntityId() {
		return residenceAreaSetId;
	}

	public void setSetEntityId(Integer residenceAreaSetId) {
		this.residenceAreaSetId = residenceAreaSetId;
	}

	public SetEntity getSetEntity() {
		if(this.residenceAreaSet==null){
			this.residenceAreaSet = DataServiceProvider.getResidenceAreaDataServiceImplInstance().getSetById(this.residenceAreaSetId);
		}
		return residenceAreaSet;
	}

	public void setSetEntity(SetEntity setEntity) {
		this.residenceAreaSet = setEntity;
	}
	
	@Override
	public String toString() {
		
		return id+"; "+name;
	}

	@Override
	public boolean equals(Object o){
	   if(this==o){
	      return true;
	   }
	   if(o instanceof ResidenceAreaWeighting){
		   ResidenceAreaWeighting other = (ResidenceAreaWeighting) o;
	       return this.id == other.getId();
	   }
	   return false;
	}
	
	@Override
	public int hashCode(){
	   return this.id;
	}

}
