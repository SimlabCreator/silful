package logic.entity;

public class FeasibleTimeWindows {

	public FeasibleTimeWindows() {

	}

	private double begin;
	private double end;
	private double insertionCosts;
	private double travelTimeTo;
	private double travelTimeFrom;
	private double earliestServiceBegin;
	private double latestServiceBegin;
	private double spanServiceBegin;
	private double serviceTime;
	private int insertAfter; 
	private int route; 

	
	
	public FeasibleTimeWindows(double begin, double end, double insertionCosts, double travelTimeTo, double travelTimeFrom, double earliestServiceBegin, double latestServiceBegin, double spanServiceBegin, double serviceTime, int insertAfter, int route) {

		this.begin = begin;
		this.end = end;
		this.insertionCosts = insertionCosts;
        this.travelTimeTo = travelTimeTo;
        this.travelTimeFrom = travelTimeFrom;
        this.earliestServiceBegin = earliestServiceBegin;
        this.latestServiceBegin = latestServiceBegin;
        this.serviceTime = serviceTime;
        this.insertAfter = insertAfter;
        this.route = route; 

    }
	
	public double getBegin(){
		return begin;
	}
	
	public void setBegin(double begin){
		this.begin = begin;
	}
	
	public double getEnd(){
		return end;
	}
	
	public void setEnd(double end){
		this.end = end;
	}
	
	
	public double getInsertionCosts(){
		return insertionCosts;
	}
	
	public void setInsertionCosts(double insertionCosts){
		this.insertionCosts = insertionCosts;
	}
	
	public double getTravelTimeTo(){
		return travelTimeTo;
	}
	
	public void setTravelTimeTo(double travelTimeTo){
		this.travelTimeTo = travelTimeTo;
	}
	
	public double getTravelTimeFrom(){
		return travelTimeFrom;
	}
	
	public void setTravelTimeFrom(double travelTimeFrom){
		this.travelTimeFrom = travelTimeFrom;
	}


	public double getEarliestServiceBegin() {
		return earliestServiceBegin;
	}

	public void setEarliestServiceBegin(double earliestServiceBegin) {
		this.earliestServiceBegin = earliestServiceBegin;
	}
	
	public double getLatestServiceBegin() {
		return latestServiceBegin;
	}

	public void setLatestServiceBegin(double latestServiceBegin) {
		this.latestServiceBegin = latestServiceBegin;
	}
	
	public double getServiceTime() {
		return serviceTime;
	}

	public void setServiceTime(double d) {
		this.serviceTime = d;
	}

	public int getInsertAfter(){
		return insertAfter;
	}

	public void setInsertAfter(int insertAfter){
		this.insertAfter = insertAfter;
	}
	public int getRoute(){
		return route;
	}

	public void setRoute(int route){
		this.route = route;
	}
	
	public double getSpanServiceBegin(){
		return spanServiceBegin;
	}

	public void setSpanServiceBegin(double spanServiceBegin){
		this.spanServiceBegin = spanServiceBegin;
	}

	
	
}
