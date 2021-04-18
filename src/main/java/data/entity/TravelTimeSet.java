package data.entity;

import java.util.ArrayList;

import data.utility.DataServiceProvider;

public class TravelTimeSet extends SetEntity {
;
	private String name;
	private ArrayList<TravelTime> travelTimes;
	private Region region;
	private Integer regionId;

	@Override
	public ArrayList<TravelTime> getElements() {
		
		if(this.travelTimes==null){
			this.travelTimes =  DataServiceProvider.getTravelTimeDataServiceImplInstance().getAllElementsBySetId(this.id);
		}
		return travelTimes;
	}


	public void setElements(ArrayList<TravelTime> elements) {
		this.travelTimes =  elements;

	}



	

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		
		return id+"; "+name;
	}

	public Region getRegion() {
		if(this.region==null){
			this.region=(Region) DataServiceProvider.getRegionDataServiceImplInstance().getById(this.regionId);
		}
		return region;
	}

	public void setRegion(Region region) {
		this.region = region;
	}

	public Integer getRegionId() {
		return regionId;
	}

	public void setRegionId(Integer regionId) {
		this.regionId = regionId;
	}
	
	

}
