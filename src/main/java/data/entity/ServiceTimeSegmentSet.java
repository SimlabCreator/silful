package data.entity;

import java.util.ArrayList;

import data.utility.DataServiceProvider;

public class ServiceTimeSegmentSet extends SetEntity {

	private String name;
	private ArrayList<ServiceTimeSegment> serviceTimes;

	@Override
	public ArrayList<ServiceTimeSegment> getElements() {
		
		if(this.serviceTimes==null){
			this.serviceTimes = (ArrayList<ServiceTimeSegment>) DataServiceProvider.getServiceTimeSegmentDataServiceImplInstance().getAllElementsBySetId(this.id);
		}
		return serviceTimes;
	}

	public void setElements(ArrayList<ServiceTimeSegment> elements) {
		this.serviceTimes =  elements;

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

}
