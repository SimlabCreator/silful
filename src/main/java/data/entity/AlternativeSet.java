package data.entity;

import java.util.ArrayList;

import data.utility.DataServiceProvider;

public class AlternativeSet extends SetEntity{
	
	private Integer id;
	private String name;
	private Integer timeWindowSetId;
	private TimeWindowSet timeWindowSet;
	private ArrayList<Alternative> alternatives;
	
	
	@Override
	public ArrayList<Alternative> getElements() {
		if(this.alternatives==null){
			this.alternatives=DataServiceProvider.getAlternativeDataServiceImplInstance().getAllElementsBySetId(this.id);
		}
		return this.alternatives;
	}

	public void setElements(ArrayList<Alternative> elements) {
		this.alternatives=elements;
		
	}
	
	public int getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Integer getTimeWindowSetId() {
		return timeWindowSetId;
	}
	public void setTimeWindowSetId(Integer timeWindowSetId) {
		this.timeWindowSetId = timeWindowSetId;
	}
	public TimeWindowSet getTimeWindowSet() {
		if(this.timeWindowSet==null){
			this.timeWindowSet = (TimeWindowSet) DataServiceProvider.getTimeWindowDataServiceImplInstance().getSetById(this.timeWindowSetId);
		}
		return timeWindowSet;
	}
	public void setTimeWindowSet(TimeWindowSet timeWindowSet) {
		this.timeWindowSet = timeWindowSet;
	}
	@Override
	public String toString() {
		
		return id+"; "+name;
	}
	
	public Alternative getAlternativeById(int id){
		for(Alternative a: this.getElements()){
			if(a.getId()==id) return a;
		}
		return null; //TODO: Produce respective exception handler
	}
	
	public Alternative getNoPurchaseAlternative(){	
		for(Alternative e: this.getElements()){
			if(e.getNoPurchaseAlternative()) return e;
		}
		
		return null;
	}

	@Override
	public boolean equals(Object o){
	   if(this==o){
	      return true;
	   }
	   if(o instanceof AlternativeSet){
		   AlternativeSet other = (AlternativeSet) o;
	       return (this.id == other.getId() && this.timeWindowSetId == other.getTimeWindowSetId());
	   }
	   return false;
	}
	
	@Override
	public int hashCode(){
		int result = 17;
		result = 31 * result + this.id;
        result = 31 * result + this.timeWindowSetId;
	   return result;
	}
}
