package data.entity;

import java.util.HashMap;

import data.utility.DataServiceProvider;

public class OrderRequest extends Entity {

	private int id;
	private Integer setId;
	private Integer customerId;
	private Customer customer;
	private Integer orderContentTypeId;
	private OrderContentType orderContentType;
	private Double basketValue;
	private Double basketVolume;
	private Integer packageno;
	private Integer arrivalTime;
	private HashMap<Integer, Double> alternativePreferences;
	private Integer bestAlternative=null;
	private DemandSegment tempOriginalSegment;
	
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

	public Integer getCustomerId() {
		return customerId;
	}

	public void setCustomerId(Integer customerId) {
		this.customerId = customerId;
	}

	public Customer getCustomer() {
		if (customer == null) {
			customer = DataServiceProvider.getCustomerDataServiceImplInstance().getElementById(customerId);
		}
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public Integer getOrderContentTypeId() {
		return orderContentTypeId;
	}

	public void setOrderContentTypeId(Integer orderContentTypeId) {
		this.orderContentTypeId = orderContentTypeId;
	}

	public Double getBasketValue() {
		return basketValue;
	}

	public void setBasketValue(Double basketValue) {
		this.basketValue = basketValue;
	}

	public Double getBasketVolume() {
		return basketVolume;
	}

	public void setBasketVolume(Double basketVolume) {
		this.basketVolume = basketVolume;
	}

	public Integer getPackageno() {
		return packageno;
	}

	public void setPackageno(Integer packageno) {
		this.packageno = packageno;
	}

	public OrderContentType getOrderContentType() {

		if (this.orderContentType == null) {
			this.orderContentType = (OrderContentType) DataServiceProvider.getOrderContentTypeDataServiceImplInstance()
					.getById(this.orderContentTypeId);
		}
		return orderContentType;
	}

	public void setOrderContentType(OrderContentType orderContentType) {
		this.orderContentType = orderContentType;
	}

	public Integer getArrivalTime() {
		return arrivalTime;
	}

	public void setArrivalTime(Integer arrivalTime) {
		this.arrivalTime = arrivalTime;
	}

	public HashMap<Integer, Double> getAlternativePreferences() {
		if (this.alternativePreferences == null) {
			this.alternativePreferences = DataServiceProvider.getOrderRequestDataServiceImplInstance()
					.getSampledPreferencesByElement(this.id);
		}
		return alternativePreferences;
	}

	public void setAlternativePreferences(HashMap<Integer, Double> alternativePreferences) {
		this.alternativePreferences = alternativePreferences;
	}

	public Integer getAlternativeIdWithHighestPreference(){
		
		if(bestAlternative==null){
			Integer highest=null;
			Double highestValue=null;
			this.getAlternativePreferences();
			for(Integer id: this.alternativePreferences.keySet()){
				if(highestValue==null|| highestValue<this.alternativePreferences.get(id)){
					highest=id;
					highestValue= this.alternativePreferences.get(id);
				}
			}
			bestAlternative=highest;
		}
		
		return bestAlternative;
	}
	public DemandSegment getTempOriginalSegment() {
		return tempOriginalSegment;
	}

	public void setTempOriginalSegment(DemandSegment tempOriginalSegment) {
		this.tempOriginalSegment = tempOriginalSegment;
	}
}
