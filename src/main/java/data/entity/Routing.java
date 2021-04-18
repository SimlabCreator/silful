package data.entity;

import java.util.ArrayList;

import data.utility.DataServiceProvider;

public class Routing extends Entity{

	private Integer id;
	private String name;
	private boolean possiblyFinalRouting;
	private ArrayList<Route> routes;
	private Integer timeWindowSetId;
	private TimeWindowSet timeWindowSet;
	private Integer orderSetId;
	private OrderSet orderSet;
	private Depot depot;
	private Integer depotId;
	private String additionalInformation;
	private Integer vehicleAreaAssignmentSetId;
	private VehicleAreaAssignmentSet vehicleAreaAssignmentSet;
	private boolean possiblyTarget; 
	private double additionalCosts;
	private String areaWeighting;
	private String areaDsWeighting;
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	

	public boolean isPossiblyFinalRouting() {
		return possiblyFinalRouting;
	}

	public void setPossiblyFinalRouting(boolean possiblyFinalRouting) {
		this.possiblyFinalRouting = possiblyFinalRouting;
	}


	

	public ArrayList<Route> getRoutes() {
		
		if(this.routes==null){
			this.routes=DataServiceProvider.getRoutingDataServiceImplInstance().getAllRoutesByRoutingId(this.id);
		}
		return routes;
	}

	public void setRoutes(ArrayList<Route> entities) {
		this.routes = entities;
	}

	public void addStartRoutingElement(Route route){
		if(this.routes==null){
			this.routes=new ArrayList<Route>();
			
		}
		
		this.routes.add(route);
	}

	


	public Integer getTimeWindowSetId() {
		return timeWindowSetId;
	}

	public void setTimeWindowSetId(Integer timeWindowSetId) {
		this.timeWindowSetId = timeWindowSetId;
	}

	public TimeWindowSet getTimeWindowSet() {
		if(this.timeWindowSet==null){
			this.timeWindowSet=(TimeWindowSet) DataServiceProvider.getTimeWindowDataServiceImplInstance().getSetById(this.timeWindowSetId);
		}
		return timeWindowSet;
	}

	public void setTimeWindowSet(TimeWindowSet timeWindowSet) {
		this.timeWindowSet = timeWindowSet;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String toString(){
		return this.id+"; "+this.name;
	}

	public Integer getOrderSetId() {
		return orderSetId;
	}

	public void setOrderSetId(Integer orderSetId) {
		this.orderSetId = orderSetId;
	}

	public OrderSet getOrderSet() {
		if(this.orderSet==null){
			this.orderSet=(OrderSet) DataServiceProvider.getOrderDataServiceImplInstance().getSetById(this.orderSetId);
		}
		return orderSet;
	}

	public void setOrderSet(OrderSet orderSet) {
		this.orderSet = orderSet;
	}

	public Depot getDepot() {
		if(this.depot==null){
			this.depot = (Depot) DataServiceProvider.getDepotDataServiceImplInstance().getById(this.depotId);
		}
		return depot;
	}

	public void setDepot(Depot depot) {
		this.depot = depot;
	}

	public Integer getDepotId() {
		return depotId;
	}

	public void setDepotId(Integer depotId) {
		this.depotId = depotId;
	}

	public String getAdditionalInformation() {
		return additionalInformation;
	}

	public void setAdditionalInformation(String additionalInformation) {
		this.additionalInformation = additionalInformation;
	}
	
	@Override
	public boolean equals(Object o){
	   if(this==o){
	      return true;
	   }
	   if(o instanceof Routing){
		   Routing other = (Routing) o;
	       return this.id == other.getId();
	   }
	   return false;
	}
	
	@Override
	public int hashCode(){
	   return this.id;
	}

	public VehicleAreaAssignmentSet getVehicleAreaAssignmentSet() {
		if(this.vehicleAreaAssignmentSet==null){
			this.vehicleAreaAssignmentSet = (VehicleAreaAssignmentSet) DataServiceProvider.getVehicleAssignmentDataServiceImplInstance().getSetById(this.vehicleAreaAssignmentSetId);
		}
		return vehicleAreaAssignmentSet;
	}

	public void setVehicleAreaAssignmentSet(VehicleAreaAssignmentSet vehicleAreaAssignmentSet) {
		this.vehicleAreaAssignmentSet = vehicleAreaAssignmentSet;
	}

	public Integer getVehicleAreaAssignmentSetId() {
		return vehicleAreaAssignmentSetId;
	}

	public void setVehicleAreaAssignmentSetId(Integer vehicleAreaAssignmentSetId) {
		this.vehicleAreaAssignmentSetId = vehicleAreaAssignmentSetId;
	}

	public boolean isPossiblyTarget() {
		return possiblyTarget;
	}

	public void setPossiblyTarget(boolean possiblyTarget) {
		this.possiblyTarget = possiblyTarget;
	}

	public double getAdditionalCosts() {
		return additionalCosts;
	}

	public void setAdditionalCosts(double additionalCosts) {
		this.additionalCosts = additionalCosts;
	}

	public String getAreaWeighting() {
		return areaWeighting;
	}

	public void setAreaWeighting(String areaWeighting) {
		this.areaWeighting = areaWeighting;
	}

	public String getAreaDsWeighting() {
		return areaDsWeighting;
	}

	public void setAreaDsWeighting(String areaDsWeighting) {
		this.areaDsWeighting = areaDsWeighting;
	}
	
	
}
