package data.entity;

public class RoutingAssignment extends Entity{
	
	private int routingId;
	private Routing routing;
	private int period;
	private int t; //Time in the booking horizon it was created (if several updates after order acceptances)
	
	private int id;
	// No functionality, only for parent-class
	public int getId() {
		return id;
	}
	
	public int getRoutingId() {
		return routingId;
	}
	public void setRoutingId(int routingId) {
		this.routingId = routingId;
	}
	public Routing getRouting() {
		return routing;
	}
	public void setRouting(Routing routing) {
		this.routing = routing;
	}
	
	public int getPeriod() {
		return period;
	}
	public void setPeriod(int period) {
		this.period = period;
	}
	public int getT() {
		return t;
	}
	public void setT(int t) {
		this.t = t;
	}
	
	
	

}
