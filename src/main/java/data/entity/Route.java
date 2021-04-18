package data.entity;

import java.util.ArrayList;

import data.utility.DataServiceProvider;

public class Route extends Entity{

	private Integer id;
	private Integer routingId;
	private Routing routing;
	private Integer vehicleTypeId;
	private VehicleType vehicleType;
	private ArrayList<RouteElement> routeElements;
	private Integer vehicleAreaAssignmentId;
	private VehicleAreaAssignment vehicleAssignment;
	
	public int getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public Integer getRoutingId() {
		return routingId;
	}
	public void setRoutingId(Integer routingId) {
		this.routingId = routingId;
	}
	public Routing getRouting() {
		
		if(this.routing==null){
			this.routing=  DataServiceProvider.getRoutingDataServiceImplInstance().getRoutingById(this.routingId);
		}
		return routing;
	}
	public void setRouting(Routing routing) {
		this.routing = routing;
	}
	public Integer getVehicleTypeId() {
		return vehicleTypeId;
	}
	public void setVehicleTypeId(Integer vehicleTypeId) {
		this.vehicleTypeId = vehicleTypeId;
	}
	public VehicleType getVehicleType() {
		
		if(this.vehicleType==null){
			this.vehicleType= (VehicleType) DataServiceProvider.getVehicleTypeDataServiceImplInstance().getById(this.vehicleTypeId);
		}
		return vehicleType;
	}
	public void setVehicleType(VehicleType vehicleType) {
		this.vehicleType = vehicleType;
	}
	@SuppressWarnings("unchecked")
	public ArrayList<RouteElement> getRouteElements() {
		
		if(this.routeElements==null){
			this.routeElements= DataServiceProvider.getRoutingDataServiceImplInstance().getAllRouteElementsByRouteId(this.id);
		}
		return routeElements;
	}
	public void setRouteElements(ArrayList<RouteElement> routeElements) {
		this.routeElements = routeElements;
	}
	
	public void addRoutingElement(RouteElement routeElement){
		if(this.routeElements==null){
			this.routeElements=new ArrayList<RouteElement>();
			
		}
		this.routeElements.add(routeElement);
	}
	public VehicleAreaAssignment getVehicleAssignment() {
		if(this.vehicleAssignment==null){
			this.vehicleAssignment = DataServiceProvider.getVehicleAssignmentDataServiceImplInstance().getElementById(this.vehicleAreaAssignmentId);
		}
		return vehicleAssignment;
	}
	
	public void setVehicleAssignment(VehicleAreaAssignment vehicleAssignment) {

		this.vehicleAssignment = vehicleAssignment;
	}
	public Integer getVehicleAreaAssignmentId() {
		return vehicleAreaAssignmentId;
	}
	public void setVehicleAreaAssignmentId(Integer vehicleAreaAssignmentId) {
		this.vehicleAreaAssignmentId = vehicleAreaAssignmentId;
	}
	
	
}
