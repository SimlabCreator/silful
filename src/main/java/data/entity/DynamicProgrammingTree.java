package data.entity;

import java.util.HashMap;

import data.utility.DataServiceProvider;

public class DynamicProgrammingTree extends Entity{
	
	private Integer id;
	private String name;
	private Integer capacitySetId;
	private Integer deliveryAreaSetId;
	private Integer demandSegmentWeightingId;
	private HashMap<Integer, String> trees;
	private Integer t;
	private Integer arrivalProcessId;
	public int getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public Integer getCapacitySetId() {
		return capacitySetId;
	}
	public void setCapacitySetId(Integer capacitySetId) {
		this.capacitySetId = capacitySetId;
	}
	public Integer getDeliveryAreaSetId() {
		return deliveryAreaSetId;
	}
	public void setDeliveryAreaSetId(Integer deliveryAreaSetId) {
		this.deliveryAreaSetId = deliveryAreaSetId;
	}
	public Integer getDemandSegmentWeightingId() {
		return demandSegmentWeightingId;
	}
	public void setDemandSegmentWeightingId(Integer demandSegmentWeightingId) {
		this.demandSegmentWeightingId = demandSegmentWeightingId;
	}
	public HashMap<Integer, String> getTrees() {
		if(this.trees==null){
			this.trees=DataServiceProvider.getControlDataServiceImplInstance().getAllTreesByDynamicProgrammingTreeId(this.id);
		}
		return trees;
	}
	public void setTrees(HashMap<Integer, String> trees) {
		this.trees = trees;
	}
	public Integer getT() {
		return t;
	}
	public void setT(Integer t) {
		this.t = t;
	}
	public Integer getArrivalProcessId() {
		return arrivalProcessId;
	}
	public void setArrivalProcessId(Integer arrivalProcessId) {
		this.arrivalProcessId = arrivalProcessId;
	}
	
	public String toString(){
		return this.id+"; "+this.name;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	

}
