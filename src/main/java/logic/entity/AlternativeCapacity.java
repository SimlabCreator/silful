package logic.entity;

import data.entity.Alternative;
import data.entity.DeliveryArea;
import data.utility.DataServiceProvider;

public class AlternativeCapacity {

	private Integer id;
private Integer alternativeId;
private Alternative alternative;
	private Integer deliveryAreaId;
	private DeliveryArea deliveryArea;
	private Integer capacityNumber;
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public Integer getAlternativeId() {
		return alternativeId;
	}
	public void setAlternativeId(Integer alternativeId) {
		this.alternativeId = alternativeId;
	}
	public Alternative getAlternative() {
		if (this.alternative == null) {
			this.alternative =  DataServiceProvider.getAlternativeDataServiceImplInstance()
					.getElementById(this.alternativeId);
		}
		return alternative;
	}
	public void setAlternative(Alternative alternative) {
		this.alternative = alternative;
	}
	public Integer getDeliveryAreaId() {
		return deliveryAreaId;
	}
	public void setDeliveryAreaId(Integer deliveryAreaId) {
		this.deliveryAreaId = deliveryAreaId;
	}
	public DeliveryArea getDeliveryArea() {
		if (this.deliveryArea == null) {
			this.deliveryArea =  DataServiceProvider.getDeliveryAreaDataServiceImplInstance()
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
	
	

	
}
