package data.entity;

import data.utility.DataServiceProvider;

public class DemandSegmentWeight extends WeightEntity {

	private int id;
	private Integer setId;
	private Integer demandSegmentId;
	private DemandSegment demandSegment;
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


	

	public DemandSegment getDemandSegment() {
		
		if(this.demandSegment==null){
			this.demandSegment=  DataServiceProvider.getDemandSegmentDataServiceImplInstance().getElementById(this.demandSegmentId);
		}
		return demandSegment;
	}

	public void setDemandSegment(DemandSegment demandSegment) {
		this.demandSegment = demandSegment;
	}

	public Double getWeight() {
		return weight;
	}

	public void setWeight(Double weight) {
		this.weight = weight;
	}


	@Override
	public Integer getElementId() {
		return demandSegmentId;
	}

	@Override
	public void setElementId(Integer elementId) {
		this.demandSegmentId = elementId;
		
	}
	
	@Override
	public boolean equals(Object o){
	   if(this==o){
	      return true;
	   }
	   if(o instanceof DemandSegmentWeight){
		   DemandSegmentWeight other = (DemandSegmentWeight) o;
	       return this.id == other.getId();
	   }
	   return false;
	}
	
	@Override
	public int hashCode(){
	   return this.id;
	}


}
