package data.entity;

import java.util.ArrayList;

import data.utility.DataServiceProvider;

public class DemandSegmentWeighting extends WeightingEntity {

	private Integer id;
	private String name;
	private ArrayList<DemandSegmentWeight> demandSegmentWeights;
	private Integer demandSegmmentSetId;
	private SetEntity demandSegmentSet;


	public ArrayList<DemandSegmentWeight> getWeights() {
		
		if(this.demandSegmentWeights==null){
			this.demandSegmentWeights = DataServiceProvider.getDemandSegmentDataServiceImplInstance().getAllWeightsByWeightingId(this.id);
		}
		return demandSegmentWeights;
	}


	public void setWeights(ArrayList<DemandSegmentWeight> elements) {
		this.demandSegmentWeights = elements;

	}


	public void addWeight(DemandSegmentWeight element) {
		if (this.demandSegmentWeights == null) {
			this.demandSegmentWeights = new ArrayList<DemandSegmentWeight>();
		}

		this.demandSegmentWeights.add(element);
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

	public Integer getSetEntityId() {
		return demandSegmmentSetId;
	}

	public void setSetEntityId(Integer setEntityId) {
		this.demandSegmmentSetId = setEntityId;
	}

	public SetEntity getSetEntity() {
		
		if(this.demandSegmentSet==null){
			this.demandSegmentSet=(DemandSegmentSet) DataServiceProvider.getDemandSegmentDataServiceImplInstance().getSetById(this.demandSegmmentSetId);
		}
		return demandSegmentSet;
	}

	public void setSetEntity(SetEntity setEntity) {
		this.demandSegmentSet = setEntity;
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
	   if(o instanceof DemandSegmentWeighting){
		   DemandSegmentWeighting other = (DemandSegmentWeighting) o;
	       return this.id == other.getId();
	   }
	   return false;
	}
	
	@Override
	public int hashCode(){
	   return this.id;
	}

}
