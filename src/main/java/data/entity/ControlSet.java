package data.entity;

import java.util.ArrayList;

import data.utility.DataServiceProvider;
import logic.entity.AlternativeCapacity;

public class ControlSet extends SetEntity{
	
	

	
	private String name;
	
	private Integer alternativeSetId;
	private AlternativeSet alternativeSet;
	private Integer deliveryAreaSetId;
	private DeliveryAreaSet deliveryAreaSet;
	private Integer valueBucketSetId;
	private ValueBucketSet valueBucketSet;
	
	private ArrayList<Control> controls;
	private ArrayList<AlternativeCapacity> alternativeCapacities; //Only helper;


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public ArrayList<Control> getElements() {
		if(this.controls==null){
			this.controls= DataServiceProvider.getControlDataServiceImplInstance().getAllElementsBySetId(this.id);
		}
		return this.controls;
	}


	public void setElements(ArrayList<Control> elements) {
		this.controls=elements;
		
	}
	
	@Override
	public String toString() {
		
		return id+"; "+name;
	}
	
	public Integer getAlternativeSetId() {
		return alternativeSetId;
	}

	public void setAlternativeSetId(Integer alternativeSetId) {
		this.alternativeSetId = alternativeSetId;
	}

	public AlternativeSet getAlternativeSet() {
		
		if(this.alternativeSet==null){
			this.alternativeSet=(AlternativeSet) DataServiceProvider.getAlternativeDataServiceImplInstance().getSetById(this.alternativeSetId);
		}
		return alternativeSet;
	}

	public void setAlternativeSet(AlternativeSet alternativeSet) {
		this.alternativeSet = alternativeSet;
	}

	public Integer getDeliveryAreaSetId() {
		return deliveryAreaSetId;
	}

	public void setDeliveryAreaSetId(Integer deliveryAreaSetId) {
		this.deliveryAreaSetId = deliveryAreaSetId;
	}

	public DeliveryAreaSet getDeliveryAreaSet() {
		if(this.deliveryAreaSet==null){
			this.deliveryAreaSet=(DeliveryAreaSet) DataServiceProvider.getDeliveryAreaDataServiceImplInstance().getSetById(this.deliveryAreaSetId);
		}
		return deliveryAreaSet;
	}

	public void setDeliveryAreaSet(DeliveryAreaSet deliveryAreaSet) {
		this.deliveryAreaSet = deliveryAreaSet;
	}

	public Integer getValueBucketSetId() {
		return valueBucketSetId;
	}

	public void setValueBucketSetId(Integer valueBucketSetId) {
		this.valueBucketSetId = valueBucketSetId;
	}

	public ValueBucketSet getValueBucketSet() {
		if(this.valueBucketSet==null){
			this.valueBucketSet=(ValueBucketSet) DataServiceProvider.getValueBucketDataServiceImplInstance().getSetById(this.valueBucketSetId);
		}
		return valueBucketSet;
	}

	public void setValueBucketSet(ValueBucketSet valueBucketSet) {
		this.valueBucketSet = valueBucketSet;
	}

	public ArrayList<AlternativeCapacity> getAlternativeCapacities() {
		return alternativeCapacities;
	}

	public void setAlternativeCapacities(ArrayList<AlternativeCapacity> alternativeCapacities) {
		this.alternativeCapacities = alternativeCapacities;
	}
		
	@Override
	public boolean equals(Object o){
	   if(this==o){
	      return true;
	   }
	   if(o instanceof ControlSet){
		   ControlSet other = (ControlSet) o;
	       return (this.id == other.getId() && this.alternativeSetId==other.getAlternativeSetId() && this.deliveryAreaSetId==other.getDeliveryAreaSetId());
	   }
	   return false;
	}
	
	@Override
	public int hashCode(){
	   return this.id+this.alternativeSetId+this.deliveryAreaSetId;
	}

}
