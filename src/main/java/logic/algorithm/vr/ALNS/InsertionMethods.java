package logic.algorithm.vr.ALNS;

import java.util.ArrayList;

import data.entity.Order;
import data.entity.OrderRequest;
import data.entity.Region;
import data.entity.RouteElement;
import data.entity.TimeWindow;
import data.entity.TimeWindowSet;
import data.entity.VehicleAreaAssignment;
import logic.service.support.LocationService;

/**
 * 
 * Insertion methods
 * @author J. Haferkamp
 *
 */

public class InsertionMethods {
	
	private DistanceCalculator distanceCalculator;
	
	public InsertionMethods(DistanceCalculator distanceCalculator)  {	
		this.distanceCalculator = distanceCalculator;
	}
	
	// compute insertion costs of an route element for all routing
	// compute insertion costs of an route element for all routing
		public void computeCostsForAllRoutes(RouteElement currentElement, ArrayList<ArrayList<RouteElement>> routing, TimeWindowSet timeWindowSet) {
			ArrayList<InsertionPosition> currentInsertionPositions = new ArrayList<InsertionPosition>();		
			currentElement.setTempInsertionPositions(currentInsertionPositions);
			//Per route one insertion position -> add to insertion positions (sorted)
			InsertionPosition insertionPosition;
			for (ArrayList<RouteElement> currentRoute : routing) {
				insertionPosition = this.getCheapestInsertionPositionByOrderAndRoute(currentRoute, currentElement, timeWindowSet);  
				if (insertionPosition.isFeasible()) {
					int i = 0;
					while (i < currentInsertionPositions.size() && currentInsertionPositions.get(i).getTravelTime() < insertionPosition.getTravelTime()) i++;
					currentInsertionPositions.add(i, insertionPosition);
				}
				else currentInsertionPositions.add(insertionPosition);
				
				if(currentElement.getTempInsertionPositions().get(0).isFeasible() && currentElement.getTempInsertionPositions().get(0).getTempShift()==0){
					System.out.println("HM");
				}
			}
		}
	
	// compute insertion costs of an route element for one route
	public InsertionPosition computeInsertionCosts(ArrayList<RouteElement> route, RouteElement currentElement) {
		
		InsertionPosition insertionPosition = new InsertionPosition();
		insertionPosition.setRoute(route);
		double currentElementStartTimeWindow = currentElement.getStartTimeWindow();
		double distanceIx;
		double distanceXj;
		double travelTime;
		int insertBevor = 0; 
		
		do {
			insertBevor++;
			if (route.get(insertBevor).getEndTimeWindow() > currentElementStartTimeWindow || insertBevor == route.size()-1) {
				distanceIx = distanceCalculator.calculateDistance(route.get(insertBevor-1), currentElement);
				distanceXj = distanceCalculator.calculateDistance(currentElement, route.get(insertBevor));	
				travelTime = distanceIx + distanceXj - route.get(insertBevor).getTravelTimeTo(); 
				if (travelTime < insertionPosition.getTravelTime()) {
					insertionPosition.setInsertBevor(insertBevor);
					insertionPosition.setTravelTime(travelTime);
					insertionPosition.setDistanceIx(distanceIx);
					insertionPosition.setDistanceXj(distanceXj);
				}
			}
		} while (insertBevor < route.size()-1 && route.get(insertBevor).getStartTimeWindow() < currentElement.getEndTimeWindow()); 											
		if(!checkFeasibility(currentElement, insertionPosition)) {
			insertionPosition.setFeasible(false);
			insertionPosition.setTravelTime(Double.MAX_VALUE);
		}
		return insertionPosition;
	}
	
	public InsertionPosition getCheapestInsertionPositionByOrderAndRoute(ArrayList<RouteElement> route, RouteElement currentElement, TimeWindowSet timeWindowSet) {
		InsertionPosition insertionPosition = new InsertionPosition();
		insertionPosition.setRoute(route);
		insertionPosition.setFeasible(false);
		insertionPosition.setTravelTime(Double.MAX_VALUE);

		// Check for all positions (after and before depot)
		for (int position = 1; position < route.size(); position++) {

			RouteElement eBefore = route.get(position - 1);
			RouteElement eAfter = route.get(position);

			// Within final time window?
			if (eBefore.getServiceBegin() > currentElement.getEndTimeWindow())
				break;

			if (eAfter.getServiceBegin()
					+ eAfter.getSlack() >= currentElement.getStartTimeWindow()) {


				double travelTimeTo= distanceCalculator.calculateDistance(eBefore, currentElement);
				double travelTimeFrom= distanceCalculator.calculateDistance(currentElement,eAfter);
				double travelTimeOld = distanceCalculator.calculateDistance(eBefore,eAfter);

				Double maximumPushOfNext = route.get(position).getWaitingTime() + route.get(position).getSlack();
				double shiftWithoutWait;
				if (position == 1 && (position == route.size() - 1)) {
					shiftWithoutWait = currentElement.getServiceTime();
					maximumPushOfNext += currentElement.getServiceTime() - 1;
				} else if (position == 1) {
					// If we are at first position after depot, the travel time
					// to takes nothing from the capacity
					shiftWithoutWait = travelTimeFrom + currentElement.getServiceTime();
				} else if (position == route.size() - 1) {
					// If we are at last position before depot, the travel time
					// from takes nothing from the capacity
					shiftWithoutWait = travelTimeTo + currentElement.getServiceTime();

					// And the depot arrival can be shifted by the service time
					// (service time only needs to start before end)
					maximumPushOfNext += currentElement.getServiceTime() - 1;
				} else {
					shiftWithoutWait = travelTimeTo + travelTimeFrom + currentElement.getServiceTime() - travelTimeOld;
				}

				// If shift without wait is already larger than the allowed
				// push, do not consider position further
				if (maximumPushOfNext >= shiftWithoutWait) {

					double earliestStart;
					if (position == 1) {
						earliestStart = timeWindowSet.getTempStartOfDeliveryPeriod()*60.0;
					} else {
						earliestStart = eBefore.getServiceBegin() + eBefore.getServiceTime() + travelTimeTo;
					}

					double latestStart = eAfter.getServiceBegin() - travelTimeFrom + eAfter.getSlack()
							- currentElement.getServiceTime();
					if (position == route.size() - 1) {
						latestStart = timeWindowSet.getTempEndOfDeliveryPeriod()*60-1.0;
					}

					if ((earliestStart < currentElement.getEndTimeWindow())
							&& (latestStart >= currentElement.getStartTimeWindow())) {
						double wait = Math.max(0.0,currentElement.getStartTimeWindow()- earliestStart);
						double shift = shiftWithoutWait + wait;
						if (maximumPushOfNext >= shift) { // Feasible with
							// regard to push of
							// next?

							// Update insertion position if better
							if (insertionPosition.getTravelTime()> shiftWithoutWait-currentElement.getServiceTime()) {
								
//								currentElement.setWaitingTime(wait);
//								currentElement.setServiceBegin((earliestStart + wait));
//								currentElement.setServiceEnd(currentElement.getServiceTime());
								insertionPosition.setFeasible(true);
								insertionPosition.setTravelTime(shiftWithoutWait-currentElement.getServiceTime());
								insertionPosition.setInsertBevor(position);
								insertionPosition.setDistanceIx(travelTimeTo);
								insertionPosition.setDistanceXj(travelTimeFrom);
								insertionPosition.setTempShift(shift);
							}
							
//							if(insertionPosition.isFeasible() && insertionPosition.getTempShift()==0){
//								System.out.println("HM");
//							}

						}
					}
				}
			}
		}
		
//		if(insertionPosition.isFeasible() && insertionPosition.getTempShift()==0){
//			System.out.println("HM");
//		}

		return insertionPosition;
	}
	
	// checks if insert is valid for insertion position
	public boolean checkFeasibility(RouteElement currentElement, InsertionPosition insertionPosition) {
		
		ArrayList<RouteElement> currentRoute = insertionPosition.getRoute();
		
		double newServiceBegin = currentRoute.get(insertionPosition.getInsertBevor()-1).getServiceEnd() + insertionPosition.getDistanceIx();
		
		if (newServiceBegin > currentElement.getEndTimeWindow()) 
			return false; 
		else if (newServiceBegin >= currentElement.getStartTimeWindow()) newServiceBegin = newServiceBegin + currentElement.getServiceTime() + insertionPosition.getDistanceXj();
		else newServiceBegin = currentElement.getStartTimeWindow() + currentElement.getServiceTime() + insertionPosition.getDistanceXj();
		
		for(int i = insertionPosition.getInsertBevor(); i < currentRoute.size()-1; i++) {
			if (newServiceBegin <= currentRoute.get(i).getServiceBegin()) break;
			else if (newServiceBegin > currentRoute.get(i).getEndTimeWindow()) 
				return false; 
			else newServiceBegin = newServiceBegin + currentRoute.get(i).getTravelTimeFrom() + currentRoute.get(i).getServiceTime();
		}
		return true;
	}	
	
	// insert route element into route 
	public void insertElement(RouteElement currentElement) {
				
		InsertionPosition insertionPosition = currentElement.getTempInsertionPositions().get(0);
		ArrayList<RouteElement> route = insertionPosition.getRoute();
		int insertBevor = insertionPosition.getInsertBevor();
//		if(currentElement.getTempInsertionPositions().get(0).isFeasible() && currentElement.getTempInsertionPositions().get(0).getTempShift()==0){
//			System.out.println("HM");
//		}
		currentElement.setTempRegretValue(Double.MAX_VALUE);
		currentElement.setTempInsertionPositions(null);
		if(currentElement.getTempHistoricalTravelTime() > insertionPosition.getTravelTime()) {
			currentElement.setTempHistoricalTravelTime(insertionPosition.getTravelTime());
		}
		route.add(insertBevor, currentElement);
		currentElement.setTempALNSRoute(route);
		currentElement.setTravelTimeTo(insertionPosition.getDistanceIx());
		currentElement.setTravelTime(insertionPosition.getDistanceIx());
		currentElement.setTravelTimeFrom(insertionPosition.getDistanceXj());
		currentElement.setWaitingTime(insertionPosition.getTempShift()-insertionPosition.getTravelTime()-currentElement.getServiceTime());
		if(insertBevor==1){
			currentElement.setServiceBegin(currentElement.getStartTimeWindow());
			currentElement.setServiceEnd(currentElement.getStartTimeWindow()+currentElement.getServiceTime());
		}else{
			currentElement.setServiceBegin(route.get(insertBevor-1).getServiceEnd()+insertionPosition.getDistanceIx()+currentElement.getWaitingTime());
			currentElement.setServiceEnd(currentElement.getServiceBegin()+currentElement.getServiceTime());
		}
		
		route.get(insertBevor-1).setTravelTimeFrom(insertionPosition.getDistanceIx());	
		route.get(insertBevor+1).setTravelTimeTo(insertionPosition.getDistanceXj());
		route.get(insertBevor+1).setTravelTime(insertionPosition.getDistanceXj());
		


		// Update the following elements
		double currentShift = insertionPosition.getTempShift();
		for (int k = insertBevor + 1; k < route.size(); k++) {
			if (currentShift == 0)
				break;
			double oldWaitingTime = route.get(k).getWaitingTime();
			route.get(k).setWaitingTime(Math.max(0, oldWaitingTime - currentShift));
			currentShift = Math.max(0, currentShift - oldWaitingTime);
			//if (k != route.size() - 1) {
				route.get(k).setServiceBegin(route.get(k).getServiceBegin() + currentShift);
				route.get(k).setServiceEnd(route.get(k).getServiceEnd() + currentShift);
				route.get(k).setSlack(route.get(k).getSlack() - currentShift);
		//	}
			if (route.get(k).getSlack() < 0)
				System.out.println("Strange");
		}

		// Update slack from current element and the ones before
		for (int k = insertBevor; k > 0; k--) {
			double maxShift = Math.min(
					route.get(k).getEndTimeWindow()
							- route.get(k).getServiceBegin(),
					route.get(k + 1).getWaitingTime() + route.get(k + 1).getSlack());
			if (k == route.size() - 2) {
				maxShift =route.get(k).getEndTimeWindow()- route.get(k).getServiceBegin();
			} 
			route.get(k).setSlack(maxShift);
			if (route.get(k).getSlack() < 0)
				System.out.println("Strange");
		}
	}
}
