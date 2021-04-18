package data.entity;

import java.util.ArrayList;

import data.utility.DataServiceProvider;

public class ValueFunctionApproximationModelSet extends SetEntity{

	private int id;
	private String name;
	private Integer typeId;
	private ValueFunctionApproximationType type;
	private Integer timeWindowSetId;
	private TimeWindowSet timeWindowSet;
	private Integer deliveryAreaSetId;
	private DeliveryAreaSet deliveryAreaSet;
	private Boolean isNumber; //Boolean if we look at capacity number or time consumption
	private Boolean isCommitted; //Boolean if we look at already commited or too come
	private Boolean isAreaSpecific; //Boolean if we weight coefficients with area value
	private ArrayList<ValueFunctionApproximationModel> models;
	

	
	public String toString(){
		return id+"; "+name;
	}


	public int getId() {
		return id;
	}


	public void setId(int id) {
		this.id = id;
	}

	@Override
	public ArrayList<ValueFunctionApproximationModel> getElements() {
		if(this.models==null) this.models = DataServiceProvider.getValueFunctionApproximationDataServiceImplInstance().getAllElementsBySetId(this.id);
		return models;
	}
	
	public void setElements(ArrayList<ValueFunctionApproximationModel> models){
		this.models=models;
	}

	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public Integer getTypeId() {
		return typeId;
	}


	public void setTypeId(Integer typeId) {
		this.typeId = typeId;
	}


	public ValueFunctionApproximationType getType() {
		if(this.type==null) this.type= DataServiceProvider.getValueFunctionApproximationDataServiceImplInstance().getModelTypeById(this.typeId);
		return type;
	}


	public void setType(ValueFunctionApproximationType type) {
		this.type = type;
	}


	public Integer getTimeWindowSetId() {
		return timeWindowSetId;
	}


	public void setTimeWindowSetId(Integer timeWindowSetId) {
		this.timeWindowSetId = timeWindowSetId;
	}


	public TimeWindowSet getTimeWindowSet() {
		if(this.timeWindowSet==null) this.timeWindowSet=(TimeWindowSet) DataServiceProvider.getTimeWindowDataServiceImplInstance().getSetById(this.timeWindowSetId);
		return timeWindowSet;
	}


	public void setTimeWindowSet(TimeWindowSet timeWindowSet) {
		this.timeWindowSet = timeWindowSet;
	}


	public Integer getDeliveryAreaSetId() {
		return deliveryAreaSetId;
	}


	public void setDeliveryAreaSetId(Integer deliveryAreaSetId) {
		this.deliveryAreaSetId = deliveryAreaSetId;
	}


	public DeliveryAreaSet getDeliveryAreaSet() {
		if(this.deliveryAreaSet==null) this.deliveryAreaSet = (DeliveryAreaSet) DataServiceProvider.getDeliveryAreaDataServiceImplInstance().getSetById(this.deliveryAreaSetId);
		return deliveryAreaSet;
	}


	public void setDeliveryAreaSet(DeliveryAreaSet deliveryAreaSet) {
		this.deliveryAreaSet = deliveryAreaSet;
	}


	public Boolean getIsNumber() {
		return isNumber;
	}


	public void setIsNumber(Boolean isNumber) {
		this.isNumber = isNumber;
	}


	public Boolean getIsCommitted() {
		return isCommitted;
	}


	public void setIsCommitted(Boolean isCommitted) {
		this.isCommitted = isCommitted;
	}


	@Override
	public boolean equals(Object o){
	   if(this==o){
	      return true;
	   }
	   if(o instanceof ValueFunctionApproximationModelSet){
		   ValueFunctionApproximationModelSet other = (ValueFunctionApproximationModelSet) o;
	       return this.id == other.getId();
	   }
	   return false;
	}
	
	@Override
	public int hashCode(){
	   return this.id;
	}


	public Boolean getIsAreaSpecific() {
		return isAreaSpecific;
	}


	public void setIsAreaSpecific(Boolean isAreaSpecific) {
		this.isAreaSpecific = isAreaSpecific;
	}



	

}
