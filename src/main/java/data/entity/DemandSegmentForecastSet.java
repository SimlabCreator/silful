package data.entity;

import java.util.ArrayList;

import data.utility.DataServiceProvider;

public class DemandSegmentForecastSet extends SetEntity{
	
	

	
	private String name;
	
	private ArrayList<DemandSegmentForecast> forecasts;
	
	private Integer deliveryAreaSetId;
	private DeliveryAreaSet deliveryAreaSet;
	private Integer demandSegmentSetId;
	private DemandSegmentSet demandSegmentSet;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public ArrayList<DemandSegmentForecast> getElements() {
		if(this.forecasts==null){
			this.forecasts= DataServiceProvider.getDemandSegmentForecastDataServiceImplInstance().getAllElementsBySetId(this.id);
		}
		return this.forecasts;
	}


	public void setElements(ArrayList<DemandSegmentForecast> elements) {
		this.forecasts=elements;
		
	}

	
	@Override
	public String toString() {
		
		return id+"; "+name;
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

	public Integer getDemandSegmentSetId() {
		return demandSegmentSetId;
	}

	public void setDemandSegmentSetId(Integer demandSegmentSetId) {
		this.demandSegmentSetId = demandSegmentSetId;
	}

	public DemandSegmentSet getDemandSegmentSet() {
		if(this.demandSegmentSet==null){
			this.demandSegmentSet=(DemandSegmentSet) DataServiceProvider.getDemandSegmentDataServiceImplInstance().getSetById(this.demandSegmentSetId);
		}
		return demandSegmentSet;
	}

	public void setDemandSegmentSet(DemandSegmentSet demandSegmentSet) {
		this.demandSegmentSet = demandSegmentSet;
	}

	
	@Override
	public boolean equals(Object o){
	   if(this==o){
	      return true;
	   }
	   if(o instanceof DemandSegmentForecastSet){
		   DemandSegmentForecastSet other = (DemandSegmentForecastSet) o;
	       return (this.id == other.getId() && this.demandSegmentSetId==other.getDemandSegmentSetId() && this.deliveryAreaSetId==other.getDeliveryAreaSetId());
	   }
	   return false;
	}
	
	@Override
	public int hashCode(){
	   return this.id+this.demandSegmentSetId+this.deliveryAreaSetId;
	}
	
	

}
