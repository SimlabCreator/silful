package data.entity;

import data.utility.DataServiceProvider;

public class ServiceTimeSegmentWeight extends WeightEntity {

	private Integer id;
	private Integer setId;
	private Integer serviceTimeSegmentId;
	private ServiceTimeSegment serviceTimeSegment;
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


	public Integer getElementId() {
		return serviceTimeSegmentId;
	}

	public void setElementId(Integer elementId) {
		this.serviceTimeSegmentId = elementId;
	}

	public ServiceTimeSegment getServiceTimeSegment() {
		
		if(this.serviceTimeSegment==null){
			this.serviceTimeSegment= (ServiceTimeSegment) DataServiceProvider.getServiceTimeSegmentDataServiceImplInstance().getElementById(this.serviceTimeSegmentId);
		}
		return serviceTimeSegment;
	}

	public void setServiceTimeSegment(ServiceTimeSegment serviceTimeSegment) {
		this.serviceTimeSegment = serviceTimeSegment;
	}

	public Double getWeight() {
		return weight;
	}

	public void setWeight(Double weight) {
		this.weight = weight;
	}
	
	

}
