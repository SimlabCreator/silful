package data.entity;

import java.util.ArrayList;

import data.utility.DataServiceProvider;

public class ResidenceAreaSet extends SetEntity{

	private String name;
	private String description;
	private Integer regionId;
	private Region region;
	private ArrayList<ResidenceArea> residenceAreas;


	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getRegionId() {
		if(this.regionId==null){
			this.regionId=this.region.getId();
		}
		return this.regionId;
	}

	public void setRegionId(int regionId) {
		this.regionId = regionId;
	}

	public Region getRegion() {
		if(region==null){
			region = (Region) DataServiceProvider.getRegionDataServiceImplInstance().getById(regionId);
		}
		return region;
	}

	public void setRegion(Region region) {
		this.region = region;
		this.regionId= this.region.getId();
	}



	@Override
	public ArrayList<ResidenceArea> getElements() {
		if(residenceAreas==null){
			residenceAreas= DataServiceProvider.getResidenceAreaDataServiceImplInstance().getAllElementsBySetId(this.id);
		}
		return residenceAreas;
	}


	public void setElements(ArrayList<ResidenceArea> elements) {
		this.residenceAreas =  elements;
		
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
	   if(o instanceof ResidenceAreaSet){
		   ResidenceAreaSet other = (ResidenceAreaSet) o;
	       return this.id == other.getId();//TODO: Reconsider if areas are created on the fly (no id yet)
	   }
	   return false;
	}
	
	@Override
	public int hashCode(){
	   return this.id;
	}


}
