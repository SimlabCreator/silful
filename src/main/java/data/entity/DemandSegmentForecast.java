package data.entity;

import data.utility.DataServiceProvider;

public class DemandSegmentForecast extends Entity {

	private Integer id;
	private Integer setId;
	private Integer demandSegmentId;
	private DemandSegment demandSegment;
	private Integer deliveryAreaId;
	private DeliveryArea deliveryArea;
	private Integer demandNumber;
	
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
	public Integer getDemandSegmentId() {
		return demandSegmentId;
	}
	public void setDemandSegmentId(Integer demandSegmentId) {
		this.demandSegmentId = demandSegmentId;
	}
	public Integer getDemandNumber() {
		return demandNumber;
	}
	public void setDemandNumber(Integer demandNumber) {
		this.demandNumber = demandNumber;
	}


	public Integer getDeliveryAreaId() {

		return deliveryAreaId;
	}

	public void setDeliveryAreaId(Integer deliveryAreaId) {
		this.deliveryAreaId = deliveryAreaId;
	}

	public DeliveryArea getDeliveryArea() {

		if (this.deliveryArea == null) {
			this.deliveryArea = DataServiceProvider.getDeliveryAreaDataServiceImplInstance()
					.getElementById(this.deliveryAreaId);
		}
		return deliveryArea;
	}

	public void setDeliveryArea(DeliveryArea deliveryArea) {
		this.deliveryArea = deliveryArea;
	}
	
	
	public DemandSegment getDemandSegment() {
		
		if(this.demandSegment==null){
			this.demandSegment= DataServiceProvider.getDemandSegmentDataServiceImplInstance().getElementById(this.demandSegmentId);
		}
		return demandSegment;
	}
	public void setDemandSegment(DemandSegment demandSegment) {
		this.demandSegment = demandSegment;
	}
	
	@Override
	public boolean equals(Object o){
	   if(this==o){
	      return true;
	   }
	   if(o instanceof DemandSegmentForecast){
		   DemandSegmentForecast other = (DemandSegmentForecast) o;
	       return (this.id == other.getId() && this.demandSegmentId==other.getDemandSegmentId() && this.deliveryAreaId==other.getDeliveryAreaId());
	   }
	   return false;
	}
	
	@Override
	public int hashCode(){
	   return this.id+this.demandSegmentId+this.deliveryAreaId;
	}
	
}
