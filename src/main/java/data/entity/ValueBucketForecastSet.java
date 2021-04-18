package data.entity;

import java.util.ArrayList;

import data.utility.DataServiceProvider;

public class ValueBucketForecastSet extends SetEntity{
	
	

	
	private String name;
	
	private ArrayList<ValueBucketForecast> forecasts;
	private Integer alternativeSetId;
	private AlternativeSet alternativeSet;
	private Integer deliveryAreaSetId;
	private DeliveryAreaSet deliveryAreaSet;
	private Integer valueBucketSetId;
	private ValueBucketSet valueBucketSet;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public ArrayList<ValueBucketForecast> getElements() {
		if(this.forecasts==null){
			this.forecasts=  DataServiceProvider.getValueBucketForecastDataServiceImplInstance().getAllElementsBySetId(this.id);
		}
		return this.forecasts;
	}


	public void setElements(ArrayList<ValueBucketForecast> elements) {
		this.forecasts= elements;
		
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
		return this.alternativeSet;
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
	
	@Override
	public boolean equals(Object o){
	   if(this==o){
	      return true;
	   }
	   if(o instanceof ValueBucketForecastSet){
		   ValueBucketForecastSet other = (ValueBucketForecastSet) o;
	       return this.id == other.getId();
	   }
	   return false;
	}
	
	@Override
	public int hashCode(){
	   return this.id;
	}
	

}
