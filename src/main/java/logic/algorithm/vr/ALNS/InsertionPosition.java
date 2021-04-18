package logic.algorithm.vr.ALNS;

import java.util.ArrayList;

import data.entity.RouteElement;

/**
 * 
 * Insertion position for route element 
 * @author J. Haferkamp
 *
 */

public class InsertionPosition {

	public InsertionPosition() {
	}
	
	private double distanceIx;
	private double distanceXj;
	private double travelTime = Double.MAX_VALUE;
	private int insertBevor;
	private ArrayList<RouteElement> route;
	private boolean feasible = true;
	private double tempShift;
	
	public InsertionPosition(double distanceIx, double distanceXj, double travelTime, int insertBevor, ArrayList<RouteElement> route) {
		this.distanceIx = distanceIx;
		this.distanceXj = distanceXj;
		this.travelTime = travelTime;
        this.insertBevor = insertBevor;
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
		
	public double getTravelTime() {
		return travelTime;
	}
	
	public void setTravelTime(double travelTime) {
		this.travelTime = travelTime;
	}

	public int getInsertBevor() {
		return insertBevor;
	}
	
	public void setInsertBevor(int insertBevor) {
		this.insertBevor = insertBevor;
	}
	
	public ArrayList<RouteElement> getRoute() {
		return route;
	}
	
	public void setRoute(ArrayList<RouteElement> route) {
		this.route = route;
	}

	public boolean isFeasible() {
		return feasible;
	}

	public void setFeasible(boolean feasible) {
		this.feasible = feasible;
	}

	public double getTempShift() {
		return tempShift;
	}

	public void setTempShift(double tempShift) {
		this.tempShift = tempShift;
	}
}
