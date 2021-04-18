package logic.algorithm.vr.ALNS;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import org.apache.commons.math3.util.Pair;

import data.entity.RouteElement;
import data.entity.TimeWindowSet;

/**
 *
 * Adaptive Large Neighborhood Search core class
 * 
 * @author J. Haferkamp
 *
 */
public class ALNS {

	private Random random = new Random();
	private int numberOfInsertionHeuristics=5;
	private int numberOfRemovalHeuristics=6;
	private TimeWindowSet timeWindowSet;
	private DistanceCalculator distanceCalculator;
	private ArrayList<ArrayList<RouteElement>> bestrouting;
	private ArrayList<RouteElement> bestRequestBank;
	private ArrayList<RouteElement> requestBank = new ArrayList<RouteElement>();
	private boolean emptyRequestBank;
	private boolean minimizeTravelTime;
	private boolean adaptivSearch;
	private int iterations;
	private int maxWithoutImprovement;
	private int requestBankCosts = 1000;
	private double temperatureControlParameter;
	private double coolingRate;
	private int noOfOrders;

	public ALNS(TimeWindowSet timeWindowSet, DistanceCalculator distanceCalculator, int noOfOrders) {

		this.timeWindowSet = timeWindowSet;
		this.distanceCalculator = distanceCalculator;
		this.noOfOrders=noOfOrders;
	}

	// minimize number of routes
	public ArrayList<ArrayList<RouteElement>> minimizeRoutes(ArrayList<ArrayList<RouteElement>> routing,
			boolean adaptivSearch, int iterations, int maxWithoutImprovement) {

		this.bestrouting = copyRouting(routing);
		this.bestRequestBank= new ArrayList<RouteElement>();
		this.emptyRequestBank = false;
		this.adaptivSearch = adaptivSearch;
		this.iterations = iterations;
		this.maxWithoutImprovement = maxWithoutImprovement;
		this.temperatureControlParameter = 0.35;
		this.coolingRate = 0.9999;
		this.minimizeTravelTime=false;
		
		// Go through individual routes of the current routing and check if it
		// can be deleted
		for (int i = 0; i < bestrouting.size(); i++) {
			if(requestBank.size()>0)
				System.out.println("size of request bank"+requestBank.size());
			// Add all the route elements of the deleted route to the request
			// bank
			requestBank.clear();
			requestBank.addAll(bestrouting.get(i));
			requestBank.remove(0);
			requestBank.remove(requestBank.size() - 1);
			bestRequestBank.addAll(requestBank);
			bestrouting.remove(i);

			// Try to accommodate all requests in the available left-over routes
			performALNS();

			if (bestRequestBank.isEmpty()) {
				// If the request bank is empty, than there is a valid routing
				// with all route elements included
				// The routing is updated to current status (for buffer before
				// next removals)
				// the next route can be tried (which has now the index of the
				// last deleted route)
				i--;
				routing = copyRouting(bestrouting);
			} else {
				// If there are still requests in the request bank, this route
				// cannot be deleted
				// the currently best routing is set back to the last status
				// before route deletion and the next route can be tried
				bestrouting = copyRouting(routing);
				requestBank.clear();
				bestRequestBank.clear();
			}
		}
		
//		int currentSize=bestRequestBank.size();
//		for(ArrayList<RouteElement> r: bestrouting) {
//			currentSize+=r.size();
//		}
//		if(currentSize>noOfOrders+2*bestrouting.size())
//			System.out.println("Something wrong");
		return bestrouting;
	}

	// minimize number of routes
	public Pair<ArrayList<ArrayList<RouteElement>>, ArrayList<RouteElement>> minimizeRoutes(ArrayList<ArrayList<RouteElement>> routing,
			ArrayList<RouteElement> requestBank, int allowedVehicleNo, boolean adaptivSearch, int iterations,
			int maxWithoutImprovement) {

		this.bestrouting = copyRouting(routing);
		this.emptyRequestBank = false;
		this.adaptivSearch = adaptivSearch;
		this.iterations = iterations;
		this.maxWithoutImprovement = maxWithoutImprovement;
		this.temperatureControlParameter = 0.35;
		this.coolingRate = 0.9999;
		this.requestBank = copyRequestBank(requestBank);
		this.bestRequestBank = copyRequestBank(requestBank);

		// As long as there are too many vehicles, reduce the best option
		while (this.bestrouting.size() > allowedVehicleNo) {
			// Best result after deletion of one
			ArrayList<ArrayList<RouteElement>> bestRoutingAfterOneDeletion = null;
			ArrayList<RouteElement> bestRequestBankAfterOneDeletion = null;
			double bestPerformanceAfterOneDeletion = Double.MAX_VALUE;
			// Go through individual routes of the current routing and check
			// performance for deletion
			int oldSize=this.bestrouting.size();
			for (int i = 0; i < this.bestrouting.size(); i++) {

				// Add all the route elements of the deleted route to the
				// request bank
				int oldLengthRequestBank = this.requestBank.size();
				this.requestBank.addAll(this.bestrouting.get(i));
				this.requestBank.remove(oldLengthRequestBank); // Remove dummy depot element
				this.requestBank.remove(this.requestBank.size() - 1); //Remove dummy depot element
				this.bestRequestBank.clear();
				this.bestRequestBank.addAll(requestBank);
				this.bestrouting.remove(i);

				// Try to accommodate all requests in the available left-over
				// routes
				performALNS();

				if (this.bestRequestBank.isEmpty()) {
					// If the request bank is empty (usually not the case but
					// nice), than there is a valid routing with all route
					// elements included
					// The routing is updated to current status (for buffer
					// before next removals)
					routing = copyRouting(bestrouting);
					requestBank = copyRequestBank(this.bestRequestBank);
					break;
				} else {
					double performance = performance(bestrouting) + this.bestRequestBank.size() * requestBankCosts;
					if (performance < bestPerformanceAfterOneDeletion) {
						bestRoutingAfterOneDeletion = copyRouting(this.bestrouting);
						bestRequestBankAfterOneDeletion = copyRequestBank(this.bestRequestBank);
						bestPerformanceAfterOneDeletion=performance;
					}
					this.bestrouting = copyRouting(routing);
					this.bestRequestBank = copyRequestBank(requestBank);
					this.requestBank = copyRequestBank(requestBank);
				}
			}
			
			if(!this.bestRequestBank.isEmpty() || (this.bestRequestBank.isEmpty() && this.bestrouting.size()==oldSize)){
				this.bestrouting = copyRouting(bestRoutingAfterOneDeletion);
				this.requestBank = copyRequestBank(bestRequestBankAfterOneDeletion);
				this.bestRequestBank = copyRequestBank(bestRequestBankAfterOneDeletion);
			}
			

		}
//		int currentSize=this.requestBank.size();
//		for(ArrayList<RouteElement> r: bestrouting) {
//			currentSize+=r.size();
//		}
//		if(currentSize>noOfOrders+2*bestrouting.size())
//			System.out.println("Something wrong");
		return new Pair<ArrayList<ArrayList<RouteElement>>, ArrayList<RouteElement>> (this.bestrouting, this.bestRequestBank);
	}

	// minimize travel times
	public ArrayList<ArrayList<RouteElement>> minimizeTravelTimes(ArrayList<ArrayList<RouteElement>> routing,
			boolean adaptivSearch, int iterations, int maxWithoutImprovement) {

		this.bestrouting = routing;
		this.emptyRequestBank = true;
		this.minimizeTravelTime=true;
		this.adaptivSearch = adaptivSearch;
		this.iterations = iterations;
		this.maxWithoutImprovement = maxWithoutImprovement;
		this.temperatureControlParameter = 0.05;
		this.coolingRate = 0.99975;


		performALNS();
		return bestrouting;
	}
	
	// minimize travel times
	public Pair<ArrayList<ArrayList<RouteElement>>, ArrayList<RouteElement>> minimizeTravelTimes(ArrayList<ArrayList<RouteElement>> routing, ArrayList<RouteElement> requestBank,
			boolean adaptivSearch, int iterations, int maxWithoutImprovement) {

		this.bestrouting = routing;
		this.bestRequestBank=requestBank;
		this.requestBank= new ArrayList<RouteElement>();
		this.requestBank.addAll(requestBank);
		this.emptyRequestBank = false;
		this.minimizeTravelTime=true;
		this.adaptivSearch = adaptivSearch;
		this.iterations = iterations;
		this.maxWithoutImprovement = maxWithoutImprovement;
		this.temperatureControlParameter = 0.05;
		this.coolingRate = 0.99975;


		performALNS();
		return new Pair<ArrayList<ArrayList<RouteElement>>, ArrayList<RouteElement>> (this.bestrouting, this.bestRequestBank);
	}

	// use ALNS to add new element
	public ArrayList<ArrayList<RouteElement>> addElement(ArrayList<ArrayList<RouteElement>> routing,
			RouteElement newElement, boolean adaptivSearch, int iterations, int maxWithoutImprovement) {

		this.bestrouting = copyRouting(routing);
		this.emptyRequestBank = false;
		this.adaptivSearch = adaptivSearch;
		this.iterations = iterations;
		this.maxWithoutImprovement = maxWithoutImprovement;
		this.temperatureControlParameter = 0.35;
		this.coolingRate = 0.9999;

		requestBank.add(newElement);
		performALNS();
		if (requestBank.isEmpty()) {
			return (bestrouting);
		} else {
			requestBank.clear();
			return routing;
		}
	}

	// core ALNS function
	private void performALNS() {

		HashSet<Double> acceptedSolutions = new HashSet<Double>();

		RemovalHeuristics removalHeuristics = new RemovalHeuristics(timeWindowSet, distanceCalculator);
		InsertionHeuristics insertionHeuristics = new InsertionHeuristics(distanceCalculator, timeWindowSet);

		RouletteWheelSelection removalHeuristicsSelection = new RouletteWheelSelection(numberOfRemovalHeuristics);
		RouletteWheelSelection insertionHeuristicsSelection = new RouletteWheelSelection(numberOfInsertionHeuristics);

		ArrayList<ArrayList<RouteElement>> acceptedrouting = copyRouting(bestrouting);
		ArrayList<ArrayList<RouteElement>> currentrouting = copyRouting(bestrouting);

		ArrayList<RouteElement> acceptedRequestBank = new ArrayList<RouteElement>();
		if (!emptyRequestBank)
			acceptedRequestBank.addAll(requestBank);

		double bestPerformance = performance(bestrouting);
		double temperature = bestPerformance * temperatureControlParameter / Math.log(2);

		bestPerformance = bestPerformance + requestBank.size() * requestBankCosts;
		double acceptedPerformance = bestPerformance;
		double currentPerformance = bestPerformance;

		int withoutImprovement = 0;
		int reasonForAcceptance;

		for (int iteration = 1; iteration <= iterations; iteration++) {

			reasonForAcceptance = 0;
			withoutImprovement++;

			removalHeuristics.removeElements(removalHeuristicsSelection.selectHeuristic(), currentrouting, requestBank);
			insertionHeuristics.insertElements(insertionHeuristicsSelection.selectHeuristic(), currentrouting,
					requestBank, emptyRequestBank);

			// acceptance criterion
			currentPerformance = performance(currentrouting) + requestBankCosts * requestBank.size();
			
//			int currentSize=requestBank.size();
//			for(ArrayList<RouteElement> r: currentrouting) {
//				currentSize+=r.size();
//			}
//			if(currentSize>noOfOrders+2*currentrouting.size())
//				System.out.println("Something wrong");
			if (requestBank.isEmpty() || !emptyRequestBank) {
				if (currentPerformance < bestPerformance) {
					bestrouting = copyRouting(currentrouting);
					bestRequestBank = new ArrayList<RouteElement>();
					bestRequestBank.addAll(requestBank);
					bestPerformance = currentPerformance;
					reasonForAcceptance = 1;
					withoutImprovement = 0;
					if (!emptyRequestBank && requestBank.isEmpty() && !minimizeTravelTime)
						break;
				} else if (currentPerformance < acceptedPerformance)
					reasonForAcceptance = 2;
				else if (Math.exp(-(currentPerformance - acceptedPerformance) / temperature) > random.nextDouble())
					reasonForAcceptance = 3;
			}

			// update after Iteration
			if (reasonForAcceptance > 0) {
				if (!emptyRequestBank) {
					acceptedRequestBank.clear();
					acceptedRequestBank.addAll(requestBank);
				}
				acceptedrouting = copyRouting(currentrouting);
				acceptedPerformance = currentPerformance;
				if (adaptivSearch && !acceptedSolutions.contains(currentPerformance)) {
					acceptedSolutions.add(currentPerformance);
					removalHeuristicsSelection.updateScore(reasonForAcceptance);
					insertionHeuristicsSelection.updateScore(reasonForAcceptance);
				}
			} else {
				requestBank.clear();
				if (!emptyRequestBank)
					requestBank.addAll(acceptedRequestBank);
				currentrouting = copyRouting(acceptedrouting);
			}

			if (withoutImprovement > maxWithoutImprovement)
				break;
			if (adaptivSearch && iteration % 100 == 0) {
				removalHeuristicsSelection.updateWheel();
				insertionHeuristicsSelection.updateWheel();
			}
			temperature = temperature * coolingRate;
		}
	}

	// generates copy of a given solution
	public ArrayList<ArrayList<RouteElement>> copyRouting(ArrayList<ArrayList<RouteElement>> routing) {
		ArrayList<ArrayList<RouteElement>> routingCopy = new ArrayList<ArrayList<RouteElement>>();
		for (ArrayList<RouteElement> currentRoute : routing) {
			ArrayList<RouteElement> routeCopy = new ArrayList<RouteElement>();
			routingCopy.add(routeCopy);
			for (int i = 0; i < currentRoute.size(); i++) {
				routeCopy.add(currentRoute.get(i).copyElement());
				routeCopy.get(i).setTempALNSRoute(routeCopy);
			}
		}
		return routingCopy;
	}

	// generates copy of request bank
	public ArrayList<RouteElement> copyRequestBank(ArrayList<RouteElement> requestBank) {
		ArrayList<RouteElement> copy = new ArrayList<RouteElement>();

		for (RouteElement re : requestBank) {
			copy.add(re.copyElement());
		}

		return copy;
	}

	// computes current performance
	public double performance(ArrayList<ArrayList<RouteElement>> routing) {
		double overallTravelTime = 0;
		for (ArrayList<RouteElement> currentRoute : routing) {
			for (RouteElement currentElement : currentRoute) {
				overallTravelTime = overallTravelTime + currentElement.getTravelTimeTo();
			}
		}
		return overallTravelTime;
	}
	
	public double requestBankCosts(ArrayList<RouteElement> requestBank){
		return requestBank.size()*this.requestBankCosts;
	}
}
