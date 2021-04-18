package logic.entity;

/**
 * Insertion costs for each possible insertion position in preferred time window
 * @author C. Koehler
 *
 */

public class InsertionCosts {
	
	//TODO: Delete
	boolean test = false; 

	private double distanceIx;
	private double distanceXj;
	private double distanceIj;
	private double insertionCosts;
	private int insertAfterId; 
	private int checkFromHere;
	private int route; 
	
	
	public InsertionCosts() {
	}
	
	public InsertionCosts(double distanceIx, double distanceXj, double distanceIj, double insertionCosts, int insertAfterId, int checkFromHere, int route) {
		this.distanceIx = distanceIx;
		this.distanceXj = distanceXj;
		this.distanceIj = distanceIj;
		this.insertionCosts = insertionCosts;
        this.insertAfterId = insertAfterId;
        this.checkFromHere = checkFromHere;
        this.route = route;
	}
	
	public double getDistanceIx() {
		return distanceIx;
	}
	
	public void setDistanceIx(double distanceIx) {
		this.distanceIx = distanceIx;
	}
	
	public double getDistanceXj() {
		return distanceXj;
	}
	
	public void setDistanceXj(double distanceXj) {
		this.distanceXj = distanceXj;
	}
	
	public double getDistanceIj() {
		return distanceIj;
	}
	
	public void setDistanceIj(double distanceIj) {
		this.distanceIj = distanceIj;
	}
		
	public double getInsertionCosts() {
		return insertionCosts;
	}
	
	public void setInsertionCosts(double insertionCosts) {
		this.insertionCosts = insertionCosts;
	}
	
	public int getInsertAfterId() {
		return insertAfterId;
	}
	
	public void setInsertAfterId(int insertAfterId) {
		this.insertAfterId = insertAfterId;
	}
	
	public int getCheckFromHere() {
		return checkFromHere;
	}
	
	public void setCheckFromHere(int checkFromHere) {
		this.checkFromHere = checkFromHere;
	}
	
	public int getRoute() {
		return route;
	}
	
	public void setRoute(int route) {
		this.route = route;
	}
	
	
	
	
	
	
	
}
