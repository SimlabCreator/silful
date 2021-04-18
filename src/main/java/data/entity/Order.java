package data.entity;

import java.util.ArrayList;

import data.utility.DataServiceProvider;

public class Order extends Entity{
	
	private int id;
	private int setId;
	private Integer orderRequestId;
	private OrderRequest orderRequest;
	private Integer timeWindowFinalId;
	private TimeWindow timeWindowFinal;
	private Integer finalTimeWindowTempId;
	private Integer selectedAlternativeId;
	private Alternative selectedAlternative;

	private Boolean accepted;
	private String reasonRejection;
	private Double alternativeFee;
	private ArrayList<Alternative> availableAlternatives;
	private ArrayList<AlternativeOffer> offeredAlternatives;
	private Integer assignedDeliveryAreaId;
	private Double assignedValue;
	private Double costEstimation;//For estimation on acceptance
	
	
	public Alternative getSelectedAlternative() {
		if(this.selectedAlternative==null){
			this.selectedAlternative=DataServiceProvider.getAlternativeDataServiceImplInstance().getElementById(this.selectedAlternativeId); 
		}
		return selectedAlternative;
	}
	public void setSelectedAlternative(Alternative selectedAlternative) {
		this.selectedAlternative = selectedAlternative;
	}
	
	/**
	 * Returns a list of AlternativeOffers
	 * @return List of AlternativeOffers
	 */
	public ArrayList<AlternativeOffer> getOfferedAlternatives() {
		if(this.offeredAlternatives==null){
			this.offeredAlternatives= DataServiceProvider.getAlternativeDataServiceImplInstance().getOfferedAlternativesByOrderId(this.id);
		}
		return offeredAlternatives;
	}
	public void setOfferedAlternatives(ArrayList<AlternativeOffer> offeredAlternatives) {
		this.offeredAlternatives = offeredAlternatives;
	}
	
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
	public Integer getOrderRequestId() {
		return orderRequestId;
	}
	public void setOrderRequestId(Integer orderRequestId) {
		this.orderRequestId = orderRequestId;
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
	public Integer getTimeWindowFinalId() {
		return timeWindowFinalId;
	}
	public void setTimeWindowFinalId(Integer timeWindowFinalId) {
		this.timeWindowFinalId = timeWindowFinalId;
	}
	public Integer getSelectedAlternativeId() {
		return selectedAlternativeId;
	}
	public void setSelectedAlternativeId(Integer selectedAlternativeId) {
		this.selectedAlternativeId = selectedAlternativeId;
	}
	public Boolean getAccepted() {
		return accepted;
	}
	public void setAccepted(Boolean accepted) {
		this.accepted = accepted;
	}
	public String getReasonRejection() {
		return reasonRejection;
	}
	public void setReasonRejection(String reasonRejection) {
		this.reasonRejection = reasonRejection;
	}
	public Double getAlternativeFee() {
		return alternativeFee;
	}
	public void setAlternativeFee(Double alternativeFee) {
		this.alternativeFee = alternativeFee;
	}
	public ArrayList<Alternative> getAvailableAlternatives() {
		if(this.availableAlternatives==null){
			this.availableAlternatives= DataServiceProvider.getAlternativeDataServiceImplInstance().getAvailableAlternativesByOrderId(this.id);
		}
		return availableAlternatives;
	}
	public void setAvailableAlternatives(ArrayList<Alternative> availableAlternatives) {
		this.availableAlternatives = availableAlternatives;
	}
	
	public TimeWindow getTimeWindowFinal() {
		if(this.timeWindowFinal==null){
			this.timeWindowFinal= DataServiceProvider.getTimeWindowDataServiceImplInstance().getElementById(this.timeWindowFinalId);
		}
		return timeWindowFinal;
	}
	public void setTimeWindowFinal(TimeWindow timeWindowFinal) {
		this.timeWindowFinal = timeWindowFinal;
	}
	public Integer getAssignedDeliveryAreaId() {
		return assignedDeliveryAreaId;
	}
	public void setAssignedDeliveryAreaId(Integer assignedDeliveryAreaId) {
		this.assignedDeliveryAreaId = assignedDeliveryAreaId;
	}
	public Double getAssignedValue() {
		return assignedValue;
	}
	public void setAssignedValue(Double assignedValue) {
		this.assignedValue = assignedValue;
	}
	public Double getCostEstimation() {
		return costEstimation;
	}
	public void setCostEstimation(Double costEstimation) {
		this.costEstimation = costEstimation;
	}
	public Integer getFinalTimeWindowTempId() {
		return finalTimeWindowTempId;
	}
	public void setFinalTimeWindowTempId(Integer finalTimeWindowTempId) {
		this.finalTimeWindowTempId = finalTimeWindowTempId;
	}
	
	
	
	

}
