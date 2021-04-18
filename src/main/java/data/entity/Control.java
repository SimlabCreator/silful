package data.entity;

import data.utility.DataServiceProvider;

public class Control extends Entity {

	private Integer id;
	private Integer setId;
	private Integer alternativeId;
	private Alternative alternative;
	private Integer deliveryAreaId;
	private DeliveryArea deliveryArea;
	private Integer controlNumber;
	private ValueBucket valueBucket;
	private Integer valueBucketId;
	private Integer tempRemaining;

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

	public Integer getAlternativeId() {
		return alternativeId;
	}

	public void setAlternativeId(Integer alternativeId) {
		this.alternativeId = alternativeId;
	}

	public Alternative getAlternative() {

		if (this.alternative == null) {
			this.alternative = DataServiceProvider.getAlternativeDataServiceImplInstance()
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
			this.deliveryArea = DataServiceProvider.getDeliveryAreaDataServiceImplInstance()
					.getElementById(this.deliveryAreaId);
		}
		return deliveryArea;
	}

	public void setDeliveryArea(DeliveryArea deliveryArea) {
		this.deliveryArea = deliveryArea;
	}

	public Integer getControlNumber() {
		return controlNumber;
	}

	public void setControlNumber(Integer controlNumber) {
		this.controlNumber = controlNumber;
	}

	public ValueBucket getValueBucket() {
		
		if(this.valueBucket==null){
			this.valueBucket=DataServiceProvider.getValueBucketDataServiceImplInstance().getElementById(this.valueBucketId);
		}
		return valueBucket;
	}

	public void setValueBucket(ValueBucket valueBucket) {
		this.valueBucket = valueBucket;
	}

	public Integer getValueBucketId() {
		return valueBucketId;
	}

	public void setValueBucketId(Integer valueBucketId) {
		this.valueBucketId = valueBucketId;
	}

	public Integer getTempRemaining() {
		return tempRemaining;
	}

	public void setTempRemaining(Integer tempRemaining) {
		this.tempRemaining = tempRemaining;
	}

	@Override
	public boolean equals(Object o){
	   if(this==o){
	      return true;
	   }
	   if(o instanceof Control){
		   Control other = (Control) o;
	       return (this.id == other.getId() && this.alternativeId==other.getAlternativeId() && this.deliveryAreaId==other.getDeliveryAreaId());
	   }
	   return false;
	}
	
	@Override
	public int hashCode(){
	   return this.id+this.alternativeId+this.deliveryAreaId;
	}
}
