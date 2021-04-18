package data.entity;

import java.util.ArrayList;

import data.utility.DataServiceProvider;

public class VehicleAreaAssignmentSet extends SetEntity {

	private Integer id;
	private String name;

	private Integer deliveryAreaSetId;
	private DeliveryAreaSet deliveryAreaSet;
	private ArrayList<VehicleAreaAssignment> assignments;

	public int getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getDeliveryAreaSetId() {
		return deliveryAreaSetId;
	}

	public void setDeliveryAreaSetId(Integer deliveryAreaSetId) {
		this.deliveryAreaSetId = deliveryAreaSetId;
	}

	public DeliveryAreaSet getDeliveryAreaSet() {
		if (this.deliveryAreaSet == null) {
			this.deliveryAreaSet = (DeliveryAreaSet) DataServiceProvider.getDeliveryAreaDataServiceImplInstance()
					.getSetById(this.deliveryAreaSetId);
		}
		return deliveryAreaSet;
	}

	public void setDeliveryAreaSet(DeliveryAreaSet deliveryAreaSet) {
		this.deliveryAreaSet = deliveryAreaSet;
	}

	@Override
	public ArrayList<VehicleAreaAssignment> getElements() {
		if (this.assignments == null) {
			this.assignments = DataServiceProvider.getVehicleAssignmentDataServiceImplInstance()
					.getAllElementsBySetId(this.id);
		}
		return this.assignments;
	}

	@Override
	public String toString() {
		return id + "; " + name;
	}

	public void setElements(ArrayList<VehicleAreaAssignment> assignments) {
		this.assignments = assignments;

	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object o){
	   if(this==o){
	      return true;
	   }
	   if(o instanceof VehicleAreaAssignmentSet){
		   VehicleAreaAssignmentSet other = (VehicleAreaAssignmentSet) o;
	       return this.id == other.getId();
	   }
	   return false;
	}
	
	@Override
	public int hashCode(){
	   return this.id;
	}
}
