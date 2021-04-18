package logic.entity;

public class AcceptedCustomersOnRouteBefore {
	
	private int before;
	private int route;
	
	
	public AcceptedCustomersOnRouteBefore() {
	}

	
	public AcceptedCustomersOnRouteBefore(int before, int route) {
		this.before = before;
		this.route = route;
	}
	
	
	public int getBefore() {
		return before;
	}
	
	public void setBefore(int before) {
		this.before = before;
	}
	
	
	public int getRoute() {
		return route;
	}
	
	public void setRoute(int route) {
		this.route = route;
	}
	
	
	


}
