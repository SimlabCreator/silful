package data.entity;

import java.util.ArrayList;

import data.utility.DataServiceProvider;

public class ServiceTimeSegmentWeighting extends WeightingEntity {

	private Integer id;
	private String name;
	private ArrayList<ServiceTimeSegmentWeight> serviceTimesWeights;
	private Integer serviceTimeSegmentSetId;
	private SetEntity serviceTimeSegmentSet;


	public ArrayList<ServiceTimeSegmentWeight> getWeights() {
		
		if(this.serviceTimesWeights==null){
			this.serviceTimesWeights = DataServiceProvider.getServiceTimeSegmentDataServiceImplInstance().getAllWeightsByWeightingId(this.id);
		}
		return serviceTimesWeights;
	}


	public void setWeights(ArrayList<ServiceTimeSegmentWeight> weights) {
		this.serviceTimesWeights = weights;

	}


	public void addWeight(ServiceTimeSegmentWeight weight) {
		if (this.serviceTimesWeights == null) {
			this.serviceTimesWeights = new ArrayList<ServiceTimeSegmentWeight>();
		}

		this.serviceTimesWeights.add(weight);
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
		return serviceTimeSegmentSetId;
	}

	public void setSetEntityId(Integer setEntityId) {
		this.serviceTimeSegmentSetId = setEntityId;
	}

	public SetEntity getSetEntity() {
		
		if(this.serviceTimeSegmentSet==null){
			this.serviceTimeSegmentSet=(ServiceTimeSegmentSet) DataServiceProvider.getServiceTimeSegmentDataServiceImplInstance().getSetById(this.serviceTimeSegmentSetId);
		}
		return serviceTimeSegmentSet;
	}

	public void setSetEntity(SetEntity setEntity) {
		this.serviceTimeSegmentSet = setEntity;
	}

	@Override
	public String toString() {
		
		return id+"; "+name;
	}

	

	

}
