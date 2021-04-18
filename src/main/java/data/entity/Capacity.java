package data.entity;

import data.utility.DataServiceProvider;

public class Capacity extends Entity {

	private int id;
	private Integer setId;
	private Integer timeWindowId;
	private TimeWindow timeWindow;
	private Integer deliveryAreaId;
	private DeliveryArea deliveryArea;
	private Integer capacityNumber;

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

	public Integer getTimeWindowId() {
		return timeWindowId;
	}

	public void setTimeWindowId(Integer timeWindowId) {
		this.timeWindowId = timeWindowId;
	}

	public TimeWindow getTimeWindow() {

		if (this.timeWindow == null) {
			this.timeWindow =  DataServiceProvider.getTimeWindowDataServiceImplInstance()
					.getElementById(this.timeWindowId);
		}
		return timeWindow;
	}

	public void setTimeWindow(TimeWindow timeWindow) {
		this.timeWindow = timeWindow;
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

	public Integer getCapacityNumber() {
		return capacityNumber;
	}

	public void setCapacityNumber(Integer capacityNumber) {
		this.capacityNumber = capacityNumber;
	}

	@Override
	public boolean equals(Object o){
	   if(this==o){
	      return true;
	   }
	   if(o instanceof Capacity){
		   Capacity other = (Capacity) o;
	       return (this.id == other.getId() && this.timeWindowId==other.getTimeWindowId() && this.deliveryAreaId==other.getDeliveryAreaId());
	   }
	   return false;
	}
	
	@Override
	public int hashCode(){
	   return this.id+this.timeWindowId+this.deliveryAreaId;
	}
	
	public Capacity copy(){
		 Capacity c=new Capacity();
		 c.setId(this.getId());
		 c.setCapacityNumber(this.getCapacityNumber());
		 c.setDeliveryAreaId(this.getDeliveryAreaId());
		 c.setTimeWindowId(this.getTimeWindowId());
		 c.setSetId(this.getSetId());
		 return c;
	}
}
