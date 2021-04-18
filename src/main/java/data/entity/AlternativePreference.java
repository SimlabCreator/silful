package data.entity;

import data.utility.DataServiceProvider;

public class AlternativePreference extends Entity{

	private int id;
	private Integer alternativeId;
	private Alternative alternative;


	private Integer orderRequestId;
	private OrderRequest orderRequest;
	private Double utilityValue; //Can be final utility or preference rank in preference list
	
	//No functionality, only for parent-class
	public int getId(){
		return id;
	}
	
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
	public OrderRequest getOrderRequest() {
		if(this.orderRequest==null){
			this.orderRequest= DataServiceProvider.getOrderRequestDataServiceImplInstance().getElementById(this.orderRequestId);
		}
		return orderRequest;
	}
	public void setOrderRequest(OrderRequest orderRequest) {
		this.orderRequest = orderRequest;
	}

	public Double getUtilityValue() {
		return utilityValue;
	}

	public void setUtilityValue(Double utilityValue) {
		this.utilityValue = utilityValue;
	}
	
	
	public Integer getOrderRequestId() {
		return orderRequestId;
	}

	public void setOrderRequestId(Integer orderRequestId) {
		this.orderRequestId = orderRequestId;
	}
}
