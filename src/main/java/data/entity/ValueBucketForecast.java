package data.entity;

import data.utility.DataServiceProvider;

public class ValueBucketForecast extends Entity {

	private Integer id;
	private Integer setId;
	//private Integer demandSegmentId;
//	private DemandSegment demandSegment;
	private Integer deliveryAreaId;
	private DeliveryArea deliveryArea;
	private Integer valueBucketId;
	private ValueBucket valueBucket;
	private Integer alternativeId;
	private Alternative alternative;
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
//	public Integer getDemandSegmentId() {
//		return demandSegmentId;
//	}
//	public void setDemandSegmentId(Integer demandSegmentId) {
//		this.demandSegmentId = demandSegmentId;
//	}
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
	
	public ValueBucket getValueBucket() {
		
		if(this.valueBucket==null){
			this.valueBucket= DataServiceProvider.getValueBucketDataServiceImplInstance().getElementById(this.valueBucketId);
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
//	public DemandSegment getDemandSegment() {
//		
//		if(this.demandSegment==null){
//			this.demandSegment=(DemandSegment) DataServiceProvider.getDemandSegmentDataServiceImplInstance().getElementById(this.demandSegmentId);
//		}
//		return demandSegment;
//	}
//	public void setDemandSegment(DemandSegment demandSegment) {
//		this.demandSegment = demandSegment;
//	}
	public Integer getAlternativeId() {
		return alternativeId;
	}
	public void setAlternativeId(Integer alternativeId) {
		this.alternativeId = alternativeId;
	}
	public Alternative getAlternative() {
		if(this.alternative==null){
			this.alternative=DataServiceProvider.getAlternativeDataServiceImplInstance().getElementById(this.alternativeId);
		}
		return alternative;
	}
	public void setAlternative(Alternative alternative) {
		this.alternative = alternative;
	}

	@Override
	public boolean equals(Object o){
	   if(this==o){
	      return true;
	   }
	   if(o instanceof ValueBucketForecast){
		   ValueBucketForecast other = (ValueBucketForecast) o;
	       return this.id == other.getId();
	   }
	   return false;
	}
	
	@Override
	public int hashCode(){
	   return this.id;
	}
	
}
