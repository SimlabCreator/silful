package logic.algorithm.vr.ALNS;

import java.util.ArrayList;

import java.util.Random;
import data.entity.RouteElement;
import data.entity.TimeWindowSet;

/**
*
* Greedy and Regret-k insertion Heuristics
* @author J. Haferkamp
*
*/

public class InsertionHeuristics {
	 
	private TimeWindowSet timeWindowSet;
	private Random random = new Random();
	private ArrayList<ArrayList<RouteElement>> routing;
	private ArrayList<RouteElement> requestBank;
	private InsertionMethods insertionMethods;

	private boolean emptyRequestBank; 
	
	public InsertionHeuristics(DistanceCalculator distanceCalculator,TimeWindowSet timeWindowSet) {
		
		insertionMethods = new InsertionMethods(distanceCalculator);
		this.timeWindowSet=timeWindowSet;
	}
	
	// performs selected heuristic 
	public ArrayList<ArrayList<RouteElement>> insertElements(int insertionHeuristic, ArrayList<ArrayList<RouteElement>> routing, ArrayList<RouteElement> requestBank, boolean emptyRequestBank) {
		this.routing = routing;
		this.requestBank = requestBank;
		this.emptyRequestBank = emptyRequestBank;
	
		switch (insertionHeuristic) {
   		case 0: basicGreedyInsertion();
   				break;
   		case 1:	if(routing.size() <= 1) regretInsertion(routing.size()-1, true);
   				else regretInsertion(random.nextInt(routing.size()-1), true);
   				break;
//    	case 2: regretInsertion(0, false);
//    			break;
    	case 2:if(routing.size() <= 1) regretInsertion(routing.size()-1, false);
    			else regretInsertion(1, false);
    			break;
    	case 3: if(routing.size() <= 2) regretInsertion(routing.size()-1, false);
				else regretInsertion(2, false);
    			break;
    	case 4: if(routing.size() <= 3) regretInsertion(routing.size()-1, true);
    			else regretInsertion(3, false);
    			break;
		}
		return this.routing;
	}

	// regret-k heuristic
	private void regretInsertion(int regretNumber, boolean noise) {
		
		ArrayList<RouteElement> currentRequestBank = new ArrayList<RouteElement>();
		RouteElement currentElement; 
		ArrayList<InsertionPosition> currentInsertionPositions;
		InsertionPosition newPosition;
		ArrayList<RouteElement> updatedRoute;
		int i;
		
		// compute insertion positions for all removed Elements
		for(int j = 0; j < requestBank.size(); j++) {
			currentElement = requestBank.get(j);
			insertionMethods.computeCostsForAllRoutes(currentElement, routing, timeWindowSet);
			
		
			if (currentElement.getTempInsertionPositions().get(0).getTravelTime() != Double.MAX_VALUE) {
				currentElement.setTempRegretValue(currentElement.getTempInsertionPositions().get(regretNumber).getTravelTime()-currentElement.getTempInsertionPositions().get(0).getTravelTime());  
				
				i = currentRequestBank.size();
				while (i > 0 && (currentElement.getTempRegretValue() > currentRequestBank.get(i-1).getTempRegretValue() || 
						(currentElement.getTempRegretValue() == currentRequestBank.get(i-1).getTempRegretValue() && 
						currentElement.getTempInsertionPositions().get(0).getTravelTime() < 
						currentRequestBank.get(i-1).getTempInsertionPositions().get(0).getTravelTime()))) i--;
				currentRequestBank.add(i, currentElement);	
				
			}
			else if (emptyRequestBank) {
				routing = null;
				return;
			}
		}

		// insert Elements and update positions 
		int chosenIndex; 
		while (!currentRequestBank.isEmpty()) {
			if(noise) chosenIndex = (int) Math.pow(random.nextDouble(), 6.0)*currentRequestBank.size();
			else chosenIndex = 0;
			
			currentElement = currentRequestBank.get(chosenIndex);	
			
			currentRequestBank.remove(chosenIndex);
			
			requestBank.remove(currentElement);
			
			
			insertionMethods.insertElement(currentElement);
			updatedRoute = currentElement.getTempALNSRoute();
			
			// update insertion positions 
			for(int j = 0; j < currentRequestBank.size(); j++)  {
				currentElement = currentRequestBank.get(j);
				currentInsertionPositions = currentElement.getTempInsertionPositions();
				for (int k = 0; k < currentInsertionPositions.size(); k++) {
					if (!currentInsertionPositions.get(k).isFeasible()) break;	
					if (currentInsertionPositions.get(k).getRoute() == updatedRoute) {
						currentInsertionPositions.remove(k);
						newPosition = insertionMethods.getCheapestInsertionPositionByOrderAndRoute(updatedRoute, currentElement,timeWindowSet); 
						if(newPosition.isFeasible()) {
							i = 0;
							while (i < currentInsertionPositions.size() && currentInsertionPositions.get(i).getTravelTime() < newPosition.getTravelTime()) i++;
							currentInsertionPositions.add(i, newPosition);		
						}
						else if (!currentInsertionPositions.isEmpty() && currentInsertionPositions.get(0).isFeasible()) {	
							currentInsertionPositions.add(newPosition);	
						}
						else if (!emptyRequestBank) {
							currentRequestBank.remove(j);
							j--;
							break;
						}
						else {
							routing = null;
							return;
						}
						currentElement.setTempRegretValue(currentElement.getTempInsertionPositions().get(regretNumber).getTravelTime() - currentElement.getTempInsertionPositions().get(0).getTravelTime());						
						break;
					}
				}	
			}
			
			// update insertion order 	
			for (int k = 1; k < currentRequestBank.size(); k++) {
				currentElement = currentRequestBank.get(k);
				i = k;
				while (i > 0 && (currentElement.getTempRegretValue() > currentRequestBank.get(i-1).getTempRegretValue() || 
						(currentElement.getTempRegretValue() == currentRequestBank.get(i-1).getTempRegretValue() && 
						currentElement.getTempInsertionPositions().get(0).getTravelTime() <
						currentRequestBank.get(i-1).getTempInsertionPositions().get(0).getTravelTime()))) i--;

				if (i != k) {
					currentRequestBank.remove(currentElement);
					currentRequestBank.add(i, currentElement);
				}
			}
		}
	}
	
	// basic greedy heuristic
	private void basicGreedyInsertion() {
	
		Random random = new Random();
		ArrayList<RouteElement> currentRequestBank = new ArrayList<RouteElement>();
		RouteElement currentElement;
		int randomElement;
		
		currentRequestBank.addAll(requestBank);
		while (!currentRequestBank.isEmpty()) {
			if (currentRequestBank.size() > 1) randomElement = random.nextInt(currentRequestBank.size()-1);
			else randomElement = 0;
			currentElement = currentRequestBank.get(randomElement);
			currentRequestBank.remove(randomElement);
			insertionMethods.computeCostsForAllRoutes(currentElement, routing, timeWindowSet);
			if (currentElement.getTempInsertionPositions().get(0).isFeasible()) {
				insertionMethods.insertElement(currentElement);
				requestBank.remove(currentElement);
			}
			else if (emptyRequestBank) {
				routing = null;
				return;
			}
		}	
	}
}
