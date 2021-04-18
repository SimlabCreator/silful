package data.entity;

import data.utility.DataServiceProvider;

public class ResidenceAreaWeight extends WeightEntity {

	private Integer id;
	private Integer setId;
	private Integer residenceAreaId;
	private ResidenceArea residenceArea;
	private Double weight;

	

	public int getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getSetId() {
		return setId;
	}

	public void setSetId(Integer setId) {
		this.setId = setId;
	}


	

	public ResidenceArea getResidenceArea() {
		
		if(this.residenceArea==null){
			this.residenceArea= DataServiceProvider.getResidenceAreaDataServiceImplInstance().getElementById(this.residenceAreaId);
		}
		return residenceArea;
	}

	public void setResidenceArea(ResidenceArea residenceArea) {
		this.residenceArea = residenceArea;
	}

	public Double getWeight() {
		return weight;
	}

	public void setWeight(Double weight) {
		this.weight = weight;
	}


	@Override
	public Integer getElementId() {
		return residenceAreaId;
	}

	@Override
	public void setElementId(Integer elementId) {
		this.residenceAreaId = elementId;
		
	}
	
	@Override
	public boolean equals(Object o){
	   if(this==o){
	      return true;
	   }
	   if(o instanceof ResidenceAreaWeight){
		   ResidenceAreaWeight other = (ResidenceAreaWeight) o;
	       return this.id == other.getId();//TODO: Reconsider if areas are created on the fly (no id yet)
	   }
	   return false;
	}
	
	@Override
	public int hashCode(){
	   return this.id;
	}

}
