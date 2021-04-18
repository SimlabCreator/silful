package logic.entity;

import data.entity.Alternative;
import data.entity.DeliveryArea;
import data.entity.ValueBucket;
import data.utility.DataServiceProvider;

/**
 * Helper to buffer sold units and estimated demand per delivery area, time window, and value bucket
 * @author M. Lang
 *
 */
public class SoldUnits{

	private Integer id;
	private Integer setId;
	private Integer deliveryAreaId;
	private DeliveryArea deliveryArea;
	private Integer valueBucketId;
	private ValueBucket valueBucket;
	private Integer alternativeId;
	private Alternative alternative;
	private Integer soldNumber;
	private Integer estimatedNumber; //Estimation after unconstraining
	
	public Integer getId() {
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
//	public Integer getDemandSegmentId() {
//		return demandSegmentId;
//	}
//	public void setDemandSegmentId(Integer demandSegmentId) {
//		this.demandSegmentId = demandSegmentId;
//	}
	public Integer getSoldNumber() {
		return soldNumber;
	}
	public void setSoldNumber(Integer soldNumber) {
		this.soldNumber = soldNumber;
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
	public Integer getEstimatedNumber() {
		return estimatedNumber;
	}
	public void setEstimatedNumber(Integer estimatedNumber) {
		this.estimatedNumber = estimatedNumber;
	}
	public Integer getAlternativeId() {
		return alternativeId;
	}
	public void setAlternativeId(Integer alternativeId) {
		this.alternativeId = alternativeId;
	}
	public Alternative getAlternative() {
		if(this.alternative==null){
			this.alternative= DataServiceProvider.getAlternativeDataServiceImplInstance().getElementById(this.alternativeId);
		}
		return alternative;
	}
	public void setAlternative(Alternative alternative) {
		this.alternative = alternative;
	}

	
}
