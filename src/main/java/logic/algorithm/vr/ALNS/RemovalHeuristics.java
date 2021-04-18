package logic.algorithm.vr.ALNS;

import java.util.ArrayList;
import java.util.Random;


import data.entity.RouteElement;
import data.entity.TimeWindow;
import data.entity.TimeWindowSet;
import logic.service.support.LocationService;

/**
*
* Removal heuristics
* @author J. Haferkamp
*
*/

public class RemovalHeuristics {
	
	private Random random = new Random();
	private TimeWindowSet timeWindowSet;
	private DistanceCalculator distanceCalculator;
	private ArrayList<ArrayList<RouteElement>> routing;
	private ArrayList<RouteElement> requestBank;
	
	public RemovalHeuristics(TimeWindowSet timeWindowSet, DistanceCalculator distanceCalculator) {	
		
		this.timeWindowSet = timeWindowSet;
		this.distanceCalculator = distanceCalculator;
	}
	
	public void removeElements(int removalHeuristic, ArrayList<ArrayList<RouteElement>> routing, ArrayList<RouteElement> requestBank) {
		this.routing = routing;
		this.requestBank = requestBank;
		switch (removalHeuristic) {
        	case 0: randomRemoval();
        			break;
        	case 1: timeWindowRemoval();
					break;
        	case 2: worstRemoval();
        			break;
        	case 3: historicalRemoval();
					break;
        	case 4: clusterRemoval();
					break;
        	case 5: shawRemoval();
        			break;	
		}
	}
	
	private void randomRemoval() {
		
		ArrayList<RouteElement> route; 
		ArrayList<RouteElement> selectableElements = new ArrayList<RouteElement>();	
		int numberToRemove;
		int randomNumber;
		RouteElement currentElement;
		int indexOfCurrentElement;
		
		for (ArrayList<RouteElement> currentRoute : routing) {	
			for (int i = 1; i < currentRoute.size()-1; i++) {
				selectableElements.add(currentRoute.get(i));
			}
		}	
		
		// specifies how many elements are removed 
		if (selectableElements.size()*0.4 > 60) numberToRemove = random.nextInt(30) + 30; 
		else numberToRemove = (int)(selectableElements.size()*(random.nextDouble() * (0.4 - 0.1) + 0.1)); 
		
		// removes random elements from routing 
		for (int i = 0; i<numberToRemove; i++){
			randomNumber = random.nextInt(selectableElements.size());
		    currentElement = selectableElements.get(randomNumber);
		    selectableElements.remove(randomNumber);
		    requestBank.add(currentElement);
		    route = currentElement.getTempALNSRoute();
		    indexOfCurrentElement = route.indexOf(currentElement); 
		    route.remove(indexOfCurrentElement);
		    currentElement.setTempALNSRoute(null);   
		    updateRoute(route, indexOfCurrentElement, timeWindowSet);
		}
	}	

	
	private void timeWindowRemoval() {
		
		ArrayList<RouteElement> route; 
		ArrayList<RouteElement> selectableElements = new ArrayList<RouteElement>();
		int numberToRemove;
		int indexOfcurrentElement;
		
		for (ArrayList<RouteElement> currentRoute : routing) {	
			for (int i = 1; i < currentRoute.size()-1; i++) {
				selectableElements.add(currentRoute.get(i));
			}
		}	
		
		// specifies how many elements are removed 
		if (selectableElements.size()*0.4 > 60) numberToRemove = random.nextInt(30) + 30; 
		else numberToRemove = (int)(selectableElements.size()*(random.nextDouble() * (0.4 - 0.1) + 0.1)); 
		
		// selects random time window 
		int selectedTimeWindow = random.nextInt(timeWindowSet.getElements().size()); 
		TimeWindow timeWindow  = (TimeWindow)  timeWindowSet.getElements().get(selectedTimeWindow);
		double  selectedTimeWindowBegin = timeWindow.getStartTime()*60;
			
		for (int i = 0; i < selectableElements.size(); i++) {	
			if (selectedTimeWindowBegin != selectableElements.get(i).getStartTimeWindow()) {
				selectableElements.remove(i);	
				i--;
			}
		}
		
		while (numberToRemove < selectableElements.size() && numberToRemove > 0) selectableElements.remove(random.nextInt(selectableElements.size()-1));
		// removes random elements from routing 
		for (RouteElement currentElement :  selectableElements){
		    requestBank.add(currentElement);
		    route = currentElement.getTempALNSRoute(); 
		    indexOfcurrentElement =  route.indexOf(currentElement); 
		    route.remove(indexOfcurrentElement);
		    currentElement.setTempALNSRoute(null);;   
		    updateRoute(route, indexOfcurrentElement,timeWindowSet);   
		}
	}	
	

	private void worstRemoval() {
		
		ArrayList<RouteElement> route; 
		ArrayList<RouteElement> selectableElements = new ArrayList<RouteElement>();
		RouteElement currentElement;
		int numberToRemove;
		int indexOfCurrentElement;
		int chosenIndex;
		
		// Computes current travel time for all elements and sort them in a descending list 
		int j;
		for (ArrayList<RouteElement> currentRoute : routing) {	
			for (int i = 1; i < currentRoute.size()-1; i++) {
				currentElement = currentRoute.get(i);
				currentElement.setTempRegretValue(currentElement.getTravelTimeTo() + currentElement.getTravelTimeFrom()); 
				if (random.nextBoolean()) currentElement.setTempRegretValue((double) (currentElement.getTempRegretValue()*(random.nextDouble()*0.25f + 1f)));
				j = 0;
				while (j < selectableElements.size() && selectableElements.get(j).getTempRegretValue() > currentElement.getTempRegretValue()) j++;
				if (j == selectableElements.size()) selectableElements.add(currentElement);
				else selectableElements.add(j, currentElement);	
			}
		}	
		
		// specifies how many elements are removed 
		if (selectableElements.size()*0.4 > 60) numberToRemove = random.nextInt(30) + 30; 
		else numberToRemove = (int)(selectableElements.size()*(random.nextDouble() * (0.4 - 0.1) + 0.1)); 
		
		// removes elements from routing via calculated list
		for (int i = 0; i<numberToRemove; i++){
			chosenIndex = (int) Math.pow(random.nextDouble(), 3.0)*selectableElements.size();		
			currentElement = selectableElements.get(chosenIndex);
			selectableElements.remove(chosenIndex);
		    requestBank.add(currentElement);
		    route = currentElement.getTempALNSRoute();  
		    currentElement.setTempALNSRoute(null);;   
		    indexOfCurrentElement = route.indexOf(currentElement); 
		    route.remove(indexOfCurrentElement);
		    updateRoute(route, indexOfCurrentElement,timeWindowSet);   
		}
	}	
	
	private void historicalRemoval() {
		
		ArrayList<RouteElement> route; 
		ArrayList<RouteElement> selectableElements = new ArrayList<RouteElement>();
		RouteElement currentElement;
		int numberToRemove;
		int indexOfCurrentElement;
		int chosenIndex;
		
		// computes difference between current and best travel time for all elements 
		// sort results in a descending list 
		int j;
		for (ArrayList<RouteElement> currentRoute : routing) {	
			for (int i = 1; i < currentRoute.size()-1; i++) {
				currentElement = currentRoute.get(i);
				currentElement.setTempRegretValue((currentElement.getTravelTimeTo() + currentElement.getTravelTimeFrom())-currentElement.getTempHistoricalTravelTime()); 
				j = 0;
				while (j < selectableElements.size() && selectableElements.get(j).getTempRegretValue() > currentElement.getTempRegretValue()) j++;
				if (j == selectableElements.size()) selectableElements.add(currentElement);
				else selectableElements.add(j, currentElement);
			}
		}    

		// specifies how many elements are removed
		if (selectableElements.size()*0.4 > 60) numberToRemove = random.nextInt(30) + 30; 
		else numberToRemove = (int)(selectableElements.size()*(random.nextDouble() * (0.4 - 0.1) + 0.1)); 
		
		// removes elements from routing via calculated list  
		for (int i = 0; i < numberToRemove; i++) {
			chosenIndex = (int) Math.pow(random.nextDouble(), 3.0)*selectableElements.size();		
			currentElement = selectableElements.get(chosenIndex);
			selectableElements.remove(chosenIndex);
		    requestBank.add(currentElement);
		    route = currentElement.getTempALNSRoute(); 
		    currentElement.setTempALNSRoute(null);;   
		    indexOfCurrentElement =  route.indexOf(currentElement); 
		    route.remove(indexOfCurrentElement);
		    updateRoute(route, indexOfCurrentElement,timeWindowSet);   
		}
	}	
		
	public void clusterRemoval() {
		
		ArrayList<RouteElement> selectedRoute;
		ArrayList<ArrayList<RouteElement>> selectableRoutes = new ArrayList<ArrayList<RouteElement>> ();
		ArrayList<RouteElement> selectableElements = new ArrayList<RouteElement>();
		int randomRoute;
		int randomElement;
		RouteElement selectedElement; 
		int indexOfCurrentElement;
		int numberOfElements = 0;	
		int numberToRemove;
		
		for (ArrayList<RouteElement> currentRoute : routing) {	
			if (currentRoute.size() > 3) {
				selectableRoutes.add(currentRoute); 
				numberOfElements = numberOfElements + currentRoute.size() - 2;
			}	
		}
		
		// specifies how many elements are removed
		if (numberOfElements*0.4 > 60) numberToRemove = random.nextInt(30) + 30; 
		else numberToRemove = (int)(numberOfElements*(random.nextDouble() * (0.4 - 0.1) + 0.1)); 
	
		while (numberToRemove > 0 && selectableRoutes.size() > 0) {
		 
			// selects random route 
			if(selectableRoutes.size() == 1) randomRoute = 0;
			else randomRoute = random.nextInt(selectableRoutes.size()-1);
			selectedRoute = selectableRoutes.get(randomRoute);
			selectableRoutes.remove(randomRoute);
			
			//selects random element within selected route 
			randomElement = (random.nextInt(selectedRoute.size()-3))+1;
			selectedElement = selectedRoute.get(randomElement);
			requestBank.add(selectedElement);
			selectedElement.setTempALNSRoute(null);;  
			selectedRoute.remove(randomElement);
			updateRoute(selectedRoute, randomElement,timeWindowSet);   
		
			// computes distance to selected element within selected route 
			// sort results in a ascending list 
			int l;
			RouteElement currentElement;
			for (int i = 1; i < selectedRoute.size()-1; i++) {	
				currentElement = selectedRoute.get(i);
				currentElement.setTempRegretValue(distanceCalculator.calculateDistance(selectedElement, currentElement));
				l = 0;
				while (l < numberToRemove && l < selectableElements.size() && selectableElements.get(l).getTempRegretValue() < currentElement.getTempRegretValue()) l++;
				if (l == selectableElements.size() || l == numberToRemove) selectableElements.add(currentElement);
				else selectableElements.add(l, currentElement);
			}  
				
			// removes half of the elements from selected route via calculated list  
			int chosenIndex;
			for (int j = 0; j < (selectableElements.size()/2); j++){
				chosenIndex = (int) Math.pow(random.nextDouble(), 3.0)*selectableElements.size();		
				currentElement = selectableElements.get(chosenIndex);
				selectableElements.remove(chosenIndex);
			    requestBank.add(currentElement);
			    currentElement.setTempALNSRoute(null);;  
			    indexOfCurrentElement =  selectedRoute.indexOf(currentElement); 
			    selectedRoute.remove(indexOfCurrentElement);
			    updateRoute(selectedRoute, indexOfCurrentElement,timeWindowSet); 
			    numberToRemove--;
			    if (numberToRemove <= 0) break;
			}
			selectableElements.clear();
		}
	}
		
	public void shawRemoval() {
		
		ArrayList<RouteElement> selectableElements = new ArrayList<RouteElement>();
		ArrayList<RouteElement> allElements = new ArrayList<RouteElement>();
		ArrayList<RouteElement> route; 
		int numberToRemove;	
		int indexOfCurrentElement;
		
		for (ArrayList<RouteElement> currentRoute : routing) {	
			for (int i = 1; i < currentRoute.size()-1; i++) {
				allElements.add(currentRoute.get(i));
			}
		}	
		
		// Specifies how many elements are removed
		if (allElements.size()*0.4 > 60) numberToRemove = random.nextInt(30) + 30; 
		else numberToRemove = (int)(allElements.size()*(random.nextDouble() * (0.4 - 0.1) + 0.1)); 
		 
		// selects random element 
		int randomElement = random.nextInt(allElements.size());
		RouteElement selectedElement = allElements.get(randomElement);
		allElements.remove(randomElement);
		requestBank.add(selectedElement);
		route = selectedElement.getTempALNSRoute(); 
		selectedElement.setTempALNSRoute(null);;  
		indexOfCurrentElement =  route.indexOf(selectedElement); 
		route.remove(indexOfCurrentElement);
		updateRoute(route, indexOfCurrentElement,timeWindowSet);   
		
		double travelTime;
		double serviceTime;
		double minServiceTime = Double.MAX_VALUE;
		double maxServiceTime = 0;
		double minTravelTime = Double.MAX_VALUE; 
		double maxTravelTime = 0;
		
		// compute min. and max. values 
		for (RouteElement currentElement : allElements) {	
		
			travelTime = distanceCalculator.calculateDistance(selectedElement, currentElement);
			serviceTime =  Math.abs(selectedElement.getServiceBegin() - currentElement.getServiceBegin());
			
			if (minServiceTime > serviceTime) minServiceTime = serviceTime;
			if (maxServiceTime < serviceTime) maxServiceTime = serviceTime;
			if (minTravelTime > travelTime) minTravelTime = travelTime;
			if (maxTravelTime < travelTime) maxTravelTime = travelTime;
		}
		
		// compute similarity from all elements to selected elements 
		// sort results in a ascending list 
		int l;
		for (RouteElement currentElement : allElements) {				
			travelTime = (distanceCalculator.calculateDistance(selectedElement, currentElement)-minTravelTime)/(maxTravelTime-minTravelTime);
			serviceTime =  (Math.abs(selectedElement.getServiceBegin() - currentElement.getServiceBegin())-minServiceTime)/(maxServiceTime-minServiceTime);
			currentElement.setTempRegretValue(travelTime + serviceTime);
			l = 0;
			while (l < numberToRemove && l < selectableElements.size() && selectableElements.get(l).getTempRegretValue() < currentElement.getTempRegretValue()) l++;
			if (l == selectableElements.size() || l == numberToRemove) selectableElements.add(currentElement);
			else selectableElements.add(l, currentElement);
		}  
		
		// removes elements from routes via calculated list  
		RouteElement currentElement; 
		int chosenIndex;
		for (int j = 0; j < numberToRemove; j++){
			chosenIndex = (int) Math.pow(random.nextDouble(), 3.0)*selectableElements.size();	
			currentElement = selectableElements.get(chosenIndex);
			selectableElements.remove(chosenIndex);
		    requestBank.add(currentElement);
		    route = currentElement.getTempALNSRoute(); 
		    currentElement.setTempALNSRoute(null);;  
		    indexOfCurrentElement =  route.indexOf(currentElement); 
		    route.remove(indexOfCurrentElement);
		    updateRoute(route, indexOfCurrentElement,timeWindowSet);   
		}  
	}

	// update route
	private void updateRouteWithoutSlack(ArrayList<RouteElement> route, int checkFromHere) {
		
		RouteElement fromElement = route.get(checkFromHere-1);
		RouteElement toElement = route.get(checkFromHere);
	
		double travelTime = distanceCalculator.calculateDistance(fromElement, toElement);    	
		fromElement.setTravelTimeFrom(travelTime);
		toElement.setTravelTimeTo(travelTime);
		
		for(int i = checkFromHere; i<route.size(); i++) {
			if(route.get(i).updateElement(route.get(i-1), false)) break; 
		}

	}
	
	private void updateRoute(ArrayList<RouteElement> route, int checkFromHere, TimeWindowSet timeWindowSet){
		// Forward: Update waiting time and service time begin

				/// Calculate travel time from first unchanged to the first that
				/// needs to be shifted forward.
				double travelTimeTo=distanceCalculator.calculateDistance(route.get(checkFromHere - 1), route.get(checkFromHere));   
				
				route.get(checkFromHere - 1).setTravelTimeFrom(travelTimeTo);
				route.get(checkFromHere).setTravelTime(travelTimeTo);
				route.get(checkFromHere).setTravelTimeTo(travelTimeTo);

				for (int i = checkFromHere; i < route.size(); i++) {
					RouteElement eBefore = route.get(i - 1);
					RouteElement eNew = route.get(i);

					// Update service begin and waiting time

					double arrivalTime;
					// If it is the first element, the service can begin at the
					// start of the time window
					if (i == 1) {
						arrivalTime = timeWindowSet.getTempStartOfDeliveryPeriod()*60.0;
					} else {
						arrivalTime = eBefore.getServiceEnd()+ eBefore.getTravelTimeFrom();
					}
					double oldServiceBegin = eNew.getServiceBegin();
					double newServiceBegin;
					if (i < route.size() - 1) {
						newServiceBegin = Math.max(arrivalTime, eNew.getStartTimeWindow());
					} else {
						// For end depot
						newServiceBegin = arrivalTime;
					}

					eNew.setServiceBegin(newServiceBegin);
					eNew.setServiceEnd(newServiceBegin+eNew.getServiceTime());
					eNew.setWaitingTime(newServiceBegin - arrivalTime);

					// If the service begin does not change, the following elements
					// can stay as before
					if (oldServiceBegin == newServiceBegin)
						break;
				}

				/// Calculate travel time from last to depot.
				
				travelTimeTo = distanceCalculator.calculateDistance(route.get(route.size()-2), route.get(route.size()-1));   

				route.get(route.size() - 2).setTravelTimeFrom(travelTimeTo);
				route.get(route.size() - 1).setTravelTime(travelTimeTo);
				route.get(route.size() - 1).setTravelTimeTo(travelTimeTo);

				// Backward: Update maximum shift (slack)

//				/// For end depot
				route.get(route.size() - 1).setSlack(route.get(route.size()-1).getEndTimeWindow()
						- route.get(route.size()-1).getServiceBegin());

				/// For others
				for (int i = route.size() - 2; i > 0; i--) {
					Double maxShift = Math.min(
						route.get(i).getEndTimeWindow()
									- route.get(i).getServiceBegin(),
							route.get(i + 1).getWaitingTime() + route.get(i + 1).getSlack());
					route.get(i).setSlack(maxShift);
					if (route.get(i).getSlack() < 0)
						System.out.println("Strange");
				}
	}
}