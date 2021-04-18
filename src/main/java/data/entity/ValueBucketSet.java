package data.entity;

import java.util.ArrayList;

import data.utility.DataServiceProvider;

public class ValueBucketSet extends SetEntity{
	
	private String name;
	private ArrayList<ValueBucket> valueBuckets;
	
	@Override
	public ArrayList<ValueBucket> getElements() {
		if(this.valueBuckets==null){
			this.valueBuckets=DataServiceProvider.getValueBucketDataServiceImplInstance().getAllElementsBySetId(this.id);
		}
		return this.valueBuckets;
	}

	
	public void setElements(ArrayList<ValueBucket> elements) {
		this.valueBuckets= elements;
		
	}
	
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		
		return id+"; "+name;
	}
	@Override
	public boolean equals(Object o){
	   if(this==o){
	      return true;
	   }
	   if(o instanceof ValueBucketSet){
		   ValueBucketSet other = (ValueBucketSet) o;
	       return this.id == other.getId();
	   }
	   return false;
	}
	
	@Override
	public int hashCode(){
	   return this.id;
	}

}
