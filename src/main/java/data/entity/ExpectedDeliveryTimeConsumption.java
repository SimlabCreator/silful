package data.entity;

import data.utility.DataServiceProvider;

public class ExpectedDeliveryTimeConsumption extends Entity {

	private Integer id;
	private Integer setId;
	private Integer timeWindowId;
	private TimeWindow timeWindow;
	private Integer deliveryAreaId;
	private DeliveryArea deliveryArea;
	private Double deliveryTime;

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

	public Double getDeliveryTime() {
		return deliveryTime;
	}

	public void setDeliveryTime(Double deliveryTime) {
		this.deliveryTime = deliveryTime;
	}


}
