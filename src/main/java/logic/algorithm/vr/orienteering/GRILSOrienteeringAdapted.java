package logic.algorithm.vr.orienteering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

import data.entity.Alternative;
import data.entity.Customer;
import data.entity.Depot;
import data.entity.Node;
import data.entity.NodeDistance;
import data.entity.Order;
import data.entity.OrderRequest;
import data.entity.OrderRequestSet;
import data.entity.OrderSet;
import data.entity.Region;
import data.entity.Route;
import data.entity.RouteElement;
import data.entity.Routing;
import data.entity.TimeWindow;
import data.entity.TimeWindowSet;
import data.entity.Vehicle;
import logic.algorithm.vr.RoutingAlgorithm;
import logic.service.support.LocationService;
import logic.service.support.ProbabilityDistributionService;
import logic.service.support.RoutingService;
import logic.utility.comparator.OrderRequestDistanceComparator;
import logic.utility.comparator.OrderRequestNodeDistanceComparator;
import logic.utility.comparator.RouteElementInsertionValueDescComparator;
import logic.utility.exceptions.ParameterUnknownException;

/**
 * Orienteering algorithm that allows additional constraints (TODO) and can
 * consider multiple time window preferences per order request (Adaption of
 * Souffriau, W., Vansteenwegen, P., Vanden Berghe, G., & Van Oudheusden, D.
 * (2013). The multiconstraint team orienteering problem with multiple time
 * windows. Transportation Science, 47(1), 53-63)
 * 
 * @author M. Lang
 *
 */


//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//TODO: Consider that sampled alternative preferences do not depend on price etc yet. only constant part!!!
//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!





public class GRILSOrienteeringAdapted implements RoutingAlgorithm {

	private OrderRequestSet orderRequestSet;
	private Double expectedServiceTime;
	private TimeWindowSet timeWindowSet;
	private final static double TIME_MULTIPLIER = 60f;
	private ArrayList<Vehicle> vehicles;
	private Integer numberOfVehicles;
	private ArrayList<Node> nodes;
	private ArrayList<NodeDistance> distances;
	private HashMap<Long, HashMap<Long, Double>> distanceMatrix;
	private int directDistances;
	private Depot depot;
	private Region region;
	private Double greedinessUpperBound;
	private Double greedinessLowerBound;
	private Double greedinessStepsize;
	private int maximumRoundsWithoutImprovement;
	private int maximumRoundWithoutImprovementLocalSearch;
	private int alternativeRandomizationApproach;
	private HashMap<Integer, ArrayList<RouteElement>> bestSolution;
	private Double bestValue; // Value of the best solution
	private int sizeOfLongestRouteBestSolution;
	private boolean bestCompletelyShaked;
	private String lastUpdateInformation;
	private int numberOfProducedSolutions;
	private int maximumNumberOfSolutions;
	private int maximumNumberOfSolutionsLocalSearch;
	private int squaredValue;
	private int actualBasketValue;
	private int twSelectionValue;
	private int thresholdAcceptance;
	private Double locationClusterProbability;
	private Routing finalRouting;
	private ArrayList<OrderRequest> orderRequests;
	private HashMap<Integer, ArrayList<RouteElement>> routes;
	private double runtimeFirstFillup=0;
	private double averageRuntimeFillup=0;
	private int counterFillup=0;
	private double averageRuntimeLocalSearch=0;
	private int counterLocalSearch=0;
	private int counterInsert=0;
	private double averageRuntimeInsert=0;
	private int counterCheckbyRoute=0;
	private double averageRuntimeCheckByRoute=0;
	private double averageRuntimeCheckByRouteFirst=0;
	private int counterCheckbyRouteFirst=0;


	private static String[] paras = new String[] { "greediness_upperBound", "greediness_lowerBound",
			"greediness_stepsize", "maximumRoundsWithoutImprovement", "maximumRoundsWithoutImprovementLocalSearch",
			"Constant_service_time", "alternative_randomization_approach_(boolean)", "maximumNumberOfSolutions",
			"maximumNumberOfSolutionsLocalSearch", "squaredValue", "actualBasketValue",
			"twSelectionOption_(0:greedy,1:random,2:popularity)", "locationClusterProbability", "thresholdAcceptance",
			"directDistances" };

	public GRILSOrienteeringAdapted(Region region, OrderRequestSet orderRequestSet, TimeWindowSet timeWindowSet,
			ArrayList<Vehicle> vehicles, ArrayList<Node> nodes, ArrayList<NodeDistance> distances, Depot depot,
			Double greedinessUpperBound, Double greedinessLowerBound, Double greedinessStepsize,
			Double maximumRoundsWithoutImprovement, Double expectedServiceTime, Double alternativeRandomizationApproach,
			Double maximumNumberOfSolutions, Double maximumRoundWithoutImprovementLocalSearch,
			Double maximumNumberOfSolutionsLocalSearch, Double squaredValue, Double actualBasketValue,
			Double twSelectionValue, Double locationClusterProbability, Double thresholdAcceptance,
			Double directDistances) {
		this.region = region;
		this.orderRequestSet = orderRequestSet;
		this.timeWindowSet = timeWindowSet;
		this.expectedServiceTime = expectedServiceTime;
		this.nodes = nodes;
		this.distances = distances;
		this.depot = depot;
		this.greedinessUpperBound = greedinessUpperBound;
		this.greedinessLowerBound = greedinessLowerBound;
		this.greedinessStepsize = greedinessStepsize;
		this.maximumRoundsWithoutImprovement = maximumRoundsWithoutImprovement.intValue();
		this.maximumRoundWithoutImprovementLocalSearch = maximumRoundWithoutImprovementLocalSearch.intValue();
		this.vehicles = vehicles;
		this.maximumNumberOfSolutions = maximumNumberOfSolutions.intValue();
		this.maximumNumberOfSolutionsLocalSearch = maximumNumberOfSolutionsLocalSearch.intValue();
		this.alternativeRandomizationApproach = alternativeRandomizationApproach.intValue();
		this.squaredValue = squaredValue.intValue();
		this.actualBasketValue = actualBasketValue.intValue();
		this.twSelectionValue = twSelectionValue.intValue();
		this.locationClusterProbability = locationClusterProbability;
		this.thresholdAcceptance = thresholdAcceptance.intValue();
		this.directDistances = directDistances.intValue();
	}

	public void start() {

		// Initialise the closest nodes for the requests
		ArrayList<OrderRequest> orderRequestsE = this.orderRequestSet.getElements();
		this.orderRequests = new ArrayList<OrderRequest>();

		for (OrderRequest request : orderRequestsE) {
			if (this.directDistances != 1) {
				Node closestNode = LocationService.findClosestNode(nodes, request.getCustomer().getLat(),
						request.getCustomer().getLon());
				request.getCustomer().setClosestNodeId(closestNode.getLongId());
				request.getCustomer().setClosestNode(closestNode);
				// TODO: Add distance to closest node
			}
			orderRequests.add(request);
		}

		if (this.directDistances != 1) {
			// Initialise the distances between nodes
			this.distanceMatrix = LocationService.getDistanceMatrixBetweenNodes(this.distances);
		}
		// Initialise the routes per vehicle
		Node depotNode = null;
		if (this.directDistances != 1) {
			// Closest node to depot (needed for dummy depot-elements)
			depotNode = LocationService.findClosestNode(nodes, depot.getLat(), depot.getLon());

		}
		this.routes = this.initialiseRoutes(depotNode);

		// Initialise best value
		this.bestValue = 0.0;

		// Outer loop: decrease greediness
		Double currentGreediness = this.greedinessUpperBound;
		while (currentGreediness >= this.greedinessLowerBound) {

			// Double currentGreediness = this.greedinessLowerBound;
			// while (currentGreediness <= this.greedinessUpperBound) {

			int noRepetitionsWithoutImprovement = 0;
			int numberOfRemovalsPerRoute = 2;
			if (this.maximumNumberOfSolutionsLocalSearch < 1) {
				numberOfRemovalsPerRoute = 1;
			}

			int[] startPositions = new int[this.numberOfVehicles];

			// Initialise start positions with 1 (depot cannot be removed)
			for (int i = 0; i < this.numberOfVehicles; i++) {
				startPositions[i] = 1;
			}
			boolean noRestart=true;
			while (noRestart) {

				// if(this.numberOfProducedSolutions%50==0){
				// System.out.println("Produced another 50");
				// }
				// Fill up routes with unassigned requests
				if (this.maximumNumberOfSolutions <= this.numberOfProducedSolutions)
					break;
				long startTime = System.currentTimeMillis();
				this.fillUpRoutes(currentGreediness);
				long endTime   = System.currentTimeMillis();
				long totalTime = endTime - startTime;
				if(counterFillup==0) runtimeFirstFillup=totalTime;
				averageRuntimeFillup=(averageRuntimeFillup*this.counterFillup+totalTime)/++counterFillup;
				this.numberOfProducedSolutions++;

				// Perform local search
				startTime = System.currentTimeMillis();
				this.performLocalSearch(this.maximumRoundWithoutImprovementLocalSearch,
						this.maximumNumberOfSolutionsLocalSearch);
				endTime   = System.currentTimeMillis();
				totalTime = endTime - startTime;
				averageRuntimeLocalSearch=(averageRuntimeLocalSearch*this.counterLocalSearch+totalTime)/++counterLocalSearch;
				// Check for improvement and update best solution as well as
				// parameters
				Double newValue = this.evaluateSolution(this.routes);
				boolean improvement = this.updateBestSolution(this.routes, newValue, currentGreediness);
				
				
				if (improvement) {
					noRepetitionsWithoutImprovement = 0;
					numberOfRemovalsPerRoute = 2;
					if (this.maximumNumberOfSolutionsLocalSearch < 1) {
						numberOfRemovalsPerRoute = 1;
					}
					// -> new start is best solution

				} else {
					noRepetitionsWithoutImprovement++;

					if (this.thresholdAcceptance == 1) {

						if (numberOfRemovalsPerRoute >= this.sizeOfLongestRouteBestSolution)
							this.bestCompletelyShaked = true;
						// If we use a threshold, the best solution is used as
						// starting point as long as the new solution is more
						// than 10% worse than
						double thresholdMultiplier = numberOfRemovalsPerRoute / this.sizeOfLongestRouteBestSolution;
						if (this.bestCompletelyShaked)
							thresholdMultiplier = 1;
						if ((this.bestValue - newValue) / this.bestValue > 0.1 * thresholdMultiplier) {
							// If the new solution is worse, begin again from
							// the old
							this.routes = this.copySolution(this.bestSolution);
						}
					}
				}

				// Caution: adaption of original algorithm, 1.) change value of
				// startPosition based on filled up routes, 2.) adapt all
				// individual S such that feasible
				int lengthOfLongestRoute = 0;
				for (int i = 0; i < startPositions.length; i++) {
					if (startPositions[i] >= routes.get(i).size() - 1) {
						startPositions[i] = startPositions[i] % (routes.get(i).size() - 1) + 1;
					}

					if (routes.get(i).size() - 2 > lengthOfLongestRoute) {// -2
						// for
						// depot
						// elements
						lengthOfLongestRoute = routes.get(i).size() - 2;
					}
				}




				
				//Complete routes where exchanged since last improvement? Or maximum rounds without improvement reached? Then stop
				if(maximumRoundsWithoutImprovement<1 && numberOfRemovalsPerRoute>lengthOfLongestRoute){
					System.out.println("Start next round because already exchanged all. "+numberOfRemovalsPerRoute);
					noRestart=false;
				}else if (maximumRoundsWithoutImprovement>0&& noRepetitionsWithoutImprovement>maximumRoundsWithoutImprovement){
					noRestart=false;
				}else{
					// Attention: Change, R cannot be larger than the length of the
					// longest route (would have no effect)
					if (numberOfRemovalsPerRoute > lengthOfLongestRoute) {
						numberOfRemovalsPerRoute = 2;
						if (this.maximumNumberOfSolutionsLocalSearch < 1) {
							numberOfRemovalsPerRoute = 1;
						}
					}
				}
				this.removeRouteElements(numberOfRemovalsPerRoute, startPositions);

				// Add first new based on neighborhood, if randomly drawn
				if (new Random().nextDouble() <= this.locationClusterProbability) {
					RouteElement toInsert = this
							.getRouteElementforRequestWithBestNeighborhood(numberOfRemovalsPerRoute);
					this.insertRouteElement(toInsert);
					this.orderRequests.remove(toInsert.getOrder().getOrderRequest());
				}

				for (int i = 0; i < startPositions.length; i++) {
					startPositions[i] = startPositions[i] + numberOfRemovalsPerRoute;
				}

				numberOfRemovalsPerRoute++;
				
				
			}
			if (this.maximumNumberOfSolutions <= this.numberOfProducedSolutions)
				break;
			currentGreediness -= this.greedinessStepsize;
			// currentGreediness += this.greedinessStepsize;
		}

		// The best solution found is returned as routing

		ArrayList<Route> finalRoutes = new ArrayList<Route>();
		for (Integer routeId : bestSolution.keySet()) {
			Route route = new Route();

			// Delete dummy depot elements
			bestSolution.get(routeId).remove(bestSolution.get(routeId).size() - 1);
			bestSolution.get(routeId).remove(0);

			for (RouteElement e : bestSolution.get(routeId)) {
				e.setTimeWindowId(e.getOrder().getTimeWindowFinalId());
				e.setTravelTime(e.getTravelTimeTo());
			}
			
			route.setRouteElements(bestSolution.get(routeId));
			finalRoutes.add(route);
		}

		this.finalRouting = new Routing();
		this.finalRouting.setDepot(this.depot);
		this.finalRouting.setDepotId(this.depot.getId());
		this.finalRouting.setPossiblyFinalRouting(true);
		this.finalRouting.setRoutes(finalRoutes);
		this.finalRouting.setTimeWindowSet(timeWindowSet);
		this.finalRouting.setTimeWindowSetId(timeWindowSet.getId());
		OrderSet orderSet = new OrderSet();
		orderSet.setOrderRequestSet(orderRequestSet);
		orderSet.setOrderRequestSetId(orderRequestSet.getId());
		ArrayList<Order> orders = new ArrayList<Order>();
		for(Route route: this.finalRouting.getRoutes()){
			for(RouteElement e: route.getRouteElements()){
				Order order = e.getOrder();
				order.setAccepted(true);
				orders.add(order);
			}
		}
		for(OrderRequest or: this.orderRequests){
			Order order = new Order();
			order.setAccepted(false);
			order.setOrderRequestId(or.getId());
			orders.add(order);
			
		}
		orderSet.setElements(orders);
		this.finalRouting.setOrderSet(orderSet);
		this.finalRouting
				.setAdditionalInformation(this.lastUpdateInformation + "; Overall: " + this.numberOfProducedSolutions);
		System.out.println("Average runtime fillup: "+this.averageRuntimeFillup);
		System.out.println("Runtime first fillup: "+this.runtimeFirstFillup);
		System.out.println("Average runtime local search: "+this.averageRuntimeLocalSearch);
		System.out.println("Average runtime insert: "+this.averageRuntimeInsert);
		System.out.println("Average runtime check by route"+averageRuntimeCheckByRoute);
		System.out.println("Average runtime check by route first fillup"+averageRuntimeCheckByRouteFirst);
	}

	/**
	 * Performs a first move local search strategy
	 *
	 */
	@SuppressWarnings("unchecked")
	private void performLocalSearch(int iterationsWithoutImprovement, int maxNumberOfIterations) {

		HashMap<Integer, ArrayList<RouteElement>> routesCopy;
		ArrayList<OrderRequest> unassignedRequestsCopy;
		int noNoImprovement = 0;
		int numberOfIterations = 0;

		while (noNoImprovement <= iterationsWithoutImprovement) {
			numberOfIterations++;
			if (numberOfIterations >= maxNumberOfIterations)
				break;

			routesCopy = this.copySolution(this.routes);
			unassignedRequestsCopy = (ArrayList<OrderRequest>) this.orderRequests.clone();
			// Choose random route
			int routeId = new Random().nextInt(this.routes.size());

			// Choose randomly if worst element or random removal
			double randomNumber = new Random().nextDouble();
			int elementToRemove = -1;

			// Delete worst
			if (randomNumber < 0.5) {
				// Find position with largest shift
				double largestShift = 0f;
				// RouteElement worstElement;
				int worstElementPosition = -1;
				for (int reId = 1; reId < this.routes.get(routeId).size() - 1; reId++) {
					RouteElement re = this.routes.get(routeId).get(reId);
					double costs = re.getWaitingTime() + re.getServiceTime() + re.getTravelTimeFrom()
							+ re.getTravelTimeTo();

					if (this.directDistances != 1) {
						costs -= this.distanceMatrix
								.get(this.routes.get(routeId).get(reId - 1).getOrder().getOrderRequest().getCustomer()
										.getClosestNodeId())
								.get(this.routes.get(routeId).get(reId + 1).getOrder().getOrderRequest().getCustomer()
										.getClosestNodeId());
					} else {
						costs -= LocationService.calculateHaversineDistanceBetweenCustomers(
								this.routes.get(routeId).get(reId - 1).getOrder().getOrderRequest().getCustomer(),
								this.routes.get(routeId).get(reId + 1).getOrder().getOrderRequest().getCustomer())
								/ this.region.getAverageKmPerHour() * TIME_MULTIPLIER;
					}

					if (costs > largestShift) {
						// worstElement = re;
						worstElementPosition = reId;
						largestShift = costs;
					}
				}
				elementToRemove = worstElementPosition;

			} else {
				// Delete randomly
				int routePosition = new Random().nextInt(this.routes.get(routeId).size() - 2);
				elementToRemove = routePosition + 1;
			}

			this.removeRouteElementsOfRoute(this.routes.get(routeId), 1, elementToRemove, true);

			for (int routeId2 = 0; routeId2 < this.routes.size(); routeId2++) {

				for (int x = 1; x < this.routes.get(routeId2).size() - 1; x++) {
					int noInRoute = 0;
					String ids = "";
					for (int routeId3 = 0; routeId3 < this.routes.size(); routeId3++) {
						for (int y = 1; y < this.routes.get(routeId3).size() - 1; y++) {
							// if(route.get(x).getOrder().getOrderRequestId()==this.routes.get(routeId).get(y).getOrder().getOrderRequestId())
							// noInRoute++;
							if (this.routes.get(routeId2).get(x).getOrder().getOrderRequestId()
									.equals(this.routes.get(routeId3).get(y).getOrder().getOrderRequestId()))
								noInRoute++;
							if (noInRoute > 1)
								ids += routeId3 + "; " + y + "; ";
						}
					}
					if (noInRoute > 1) {
						System.out.println("twice in different routes " + ids);
					}
				}

			}

			// Go through other routes and check if cheaper insertion in route
			// than in their own one
			ArrayList<RouteElement> possibleInsertions = new ArrayList<RouteElement>();
			for (int currentRoute : this.routes.keySet()) {
				if (currentRoute != routeId) {
					for (int currentElementToCheck = 1; currentElementToCheck < this.routes.get(currentRoute).size()
							- 1; currentElementToCheck++) {
						HashMap<Integer, RouteElement> cheapestInsertionElementsList = this
								.getCheapestInsertionElementByRoute(this.routes.get(routeId), routeId, this.routes
										.get(currentRoute).get(currentElementToCheck).getOrder().getOrderRequest());

						if (cheapestInsertionElementsList.size() > 0) {
							RouteElement cheapestInsertionElement = null;
							double bestValue = Double.MAX_VALUE;
							for (int insertionElementId : cheapestInsertionElementsList.keySet()) {
								if (cheapestInsertionElementsList.get(insertionElementId).getTempShift() < bestValue) {
									cheapestInsertionElement = cheapestInsertionElementsList.get(insertionElementId);
									bestValue = cheapestInsertionElementsList.get(insertionElementId).getTempShift();
								}
							}

							// Should be better than current insertion value of
							// that element in its current route
							double costsInOldRoute = this.routes.get(currentRoute).get(currentElementToCheck - 1)
									.getTravelTimeFrom()
									+ this.routes.get(currentRoute).get(currentElementToCheck + 1).getTravelTimeTo()
									+ this.routes.get(currentRoute).get(currentElementToCheck).getServiceTime()
									+ this.routes.get(currentRoute).get(currentElementToCheck).getWaitingTime();

							if (this.directDistances != 1) {
								costsInOldRoute -= this.distanceMatrix
										.get(this.routes.get(currentRoute).get(currentElementToCheck - 1).getOrder()
												.getOrderRequest().getCustomer().getClosestNodeId())
										.get(this.routes.get(currentRoute).get(currentElementToCheck + 1).getOrder()
												.getOrderRequest().getCustomer().getClosestNodeId());
							} else {
								costsInOldRoute -= LocationService.calculateHaversineDistanceBetweenCustomers(
										this.routes.get(currentRoute).get(currentElementToCheck - 1).getOrder()
												.getOrderRequest().getCustomer(),
										this.routes.get(currentRoute).get(currentElementToCheck + 1).getOrder()
												.getOrderRequest().getCustomer())
										/ this.region.getAverageKmPerHour() * TIME_MULTIPLIER;
							}
							if (costsInOldRoute >= cheapestInsertionElement.getTempShift()) {
								cheapestInsertionElement.setTempCurrentlyInOtherRoute(currentRoute);
								cheapestInsertionElement.setTempCurrentlyInOtherRoutePosition(currentElementToCheck);
								possibleInsertions.add(cheapestInsertionElement);
							}
						}
					}

				}
			}

			// Go through unassigned to get insertion costs
			for (OrderRequest unassignedRequest : this.orderRequests) {

				HashMap<Integer, RouteElement> cheapestInsertionElementsList = this
						.getCheapestInsertionElementByRoute(this.routes.get(routeId), routeId, unassignedRequest);

				if (cheapestInsertionElementsList.size() > 0) {
					RouteElement bestInsertionElement = null;
					double bestValue = Double.MAX_VALUE;
					for (int insertionElementId : cheapestInsertionElementsList.keySet()) {
						if (cheapestInsertionElementsList.get(insertionElementId).getTempShift() < bestValue) {
							bestInsertionElement = cheapestInsertionElementsList.get(insertionElementId);
							bestValue = cheapestInsertionElementsList.get(insertionElementId).getTempShift();
						}
					}

					if (bestInsertionElement != null)
						possibleInsertions.add(bestInsertionElement);
				}

			}

			if (possibleInsertions.size() > 0) {
				// Get cheapest

				Collections.sort(possibleInsertions,
						new RouteElementInsertionValueDescComparator(this.actualBasketValue, this.squaredValue));

				if (possibleInsertions.get(0).getTempCurrentlyInOtherRoute() != null) {
					this.removeRouteElementsOfRoute(
							this.routes.get(possibleInsertions.get(0).getTempCurrentlyInOtherRoute()), 1,
							possibleInsertions.get(0).getTempCurrentlyInOtherRoutePosition(), true);
					this.insertRouteElement(possibleInsertions.get(0));

					this.orderRequests.remove(possibleInsertions.get(0).getOrder().getOrderRequest());

				} else {
					this.insertRouteElement(possibleInsertions.get(0));
					this.orderRequests.remove(possibleInsertions.get(0).getOrder().getOrderRequest());

				}

			}

			// Check if further holes can be filled up
			this.fillUpRoutes(1.0);
			this.numberOfProducedSolutions++;

			// If the new solution is worse, reset to old
			if (this.evaluateSolution(this.routes) < this.evaluateSolution(routesCopy)) {
				this.routes = routesCopy;
				this.orderRequests = unassignedRequestsCopy;
				noNoImprovement++;
			} else {
				noNoImprovement = 0;
			}

		}

	}

	/**
	 * Fills up existing routes with unassigned requests
	 * 
	 * @param routes
	 *            Current status of the routes
	 * @param greedinessFactor
	 *            Measure of greediness to chose the next insertion option
	 * @param unassignedRequests
	 *            Requests that are not in the routing until now
	 */
	private void fillUpRoutes(Double greedinessFactor) {

		// Assign unassigned requests to the routes. Stop if no requests are
		// left
		// over (or if no assignment feasible)
		while (this.orderRequests.size() > 0) {

			// Insertion options
			ArrayList<RouteElement> insertionOptions = new ArrayList<RouteElement>();

			// Go through unassigned requests and define cheapest insertion per
			// request

			if (this.alternativeRandomizationApproach == 1) {
				/// Define how many to evaluate
				int numberToEvaluate = (int) (this.orderRequests.size() * greedinessFactor);
				Collections.shuffle(this.orderRequests);

				for (int requestId = 0; requestId < numberToEvaluate; requestId++) {
					RouteElement optionalElement = this.findCheapestInsertionOption(this.orderRequests.get(requestId));
					if (optionalElement != null) {
						insertionOptions.add(optionalElement);
					}
				}

				// If no feasible insertions, than go through whole list
				if (insertionOptions.size() == 0) {
					for (int requestId = numberToEvaluate; requestId < this.orderRequests.size(); requestId++) {
						RouteElement optionalElement = this
								.findCheapestInsertionOption(this.orderRequests.get(requestId));
						if (optionalElement != null) {
							insertionOptions.add(optionalElement);
						}
					}
				}
			} else {
				for (OrderRequest request : this.orderRequests) {
					RouteElement optionalElement = this.findCheapestInsertionOption(request);
					if (optionalElement != null) {
						insertionOptions.add(optionalElement);
					}
				}
			}

			// Stop if no feasible insertions exist
			if (insertionOptions.size() == 0) {
				return;
			}

			// Sort regarding value to find maximum and minimum value

			Collections.sort(insertionOptions,
					new RouteElementInsertionValueDescComparator(this.actualBasketValue, this.squaredValue));

			int chosenIndex = 0;
			if (this.alternativeRandomizationApproach != 1) {
				// Calculate value border based on greediness factor
				double max = this.getShiftValue(insertionOptions.get(0));

				double min = this.getShiftValue(insertionOptions.get(insertionOptions.size() - 1));

				double valueBorder = (max - min) * greedinessFactor + min;
				// Find index of element with lowest value above the border
				int borderElement = 0; // The first element is definitely higher
				while (borderElement < insertionOptions.size() - 1) {

					double elementValue = this.getShiftValue(insertionOptions.get(borderElement + 1));

					if (elementValue > valueBorder) {
						borderElement++;
					} else {
						break; // Stop because list is sorted and all afterwards
								// will not be higher
					}
				}
				if(borderElement>0 && greedinessFactor.equals(1)){
					System.out.println("Several with same value?");
				}
				if(borderElement>0 && greedinessFactor==1){
					System.out.println("Several with same value?");
				}
				
				// Choose a random number between 0 and the borderElement
				Random randomGenerator = new Random();
				chosenIndex = randomGenerator.nextInt(borderElement + 1);

			}
			// Insert the respective Element in the route
			this.insertRouteElement(insertionOptions.get(chosenIndex));
			// Delete the respective order request from the unassigned requests
			this.orderRequests.remove(insertionOptions.get(chosenIndex).getOrder().getOrderRequest());

		}
	}

	/**
	 * Finds the cheapest insertion option and returns a route element with the
	 * respective information. Returns null if no insertion is feasible. Ties
	 * are always solved randomly.
	 * 
	 * @param request
	 *            Respective order request to insert in the routes
	 * @param routes
	 *            Current status of the routes
	 * @return route element with all information. Null if no feasible
	 *         insertion.
	 */
	private RouteElement findCheapestInsertionOption(OrderRequest request) {

		// TODO: Attention! Shift is here without value

		// Collect best elements per tw over all routes
		HashMap<Integer, RouteElement> bestElementPerTw = new HashMap<Integer, RouteElement>();

		// Check for all routes
		for (Integer routeId : this.routes.keySet()) {

			// Get best for route
			ArrayList<RouteElement> route = this.routes.get(routeId);
			long startTime = System.currentTimeMillis();
			HashMap<Integer, RouteElement> best = this.getCheapestInsertionElementByRoute(route, routeId, request);
			long endTime   = System.currentTimeMillis();
			long totalTime = endTime - startTime;
			if(this.counterFillup==0){
				averageRuntimeCheckByRouteFirst=(averageRuntimeCheckByRouteFirst*this.counterCheckbyRouteFirst+totalTime)/++counterCheckbyRouteFirst;
			}
			averageRuntimeCheckByRoute=(averageRuntimeCheckByRoute*this.counterCheckbyRoute+totalTime)/++counterCheckbyRoute;

			// Update overall best
			for (int key : best.keySet()) {
				if (!bestElementPerTw.containsKey(key)
						|| (bestElementPerTw.get(key).getTempShift() > best.get(key).getTempShift())
						|| ((bestElementPerTw.get(key).getTempShift() == best.get(key).getTempShift())
								&& new Random().nextBoolean())) {
					
					
					bestElementPerTw.put(key, best.get(key));
				}
			}

		}

		// No feasible insertion?
		if (bestElementPerTw.size() == 0)
			return null;

		// Choose time window according to time window selection strategy
		if (this.twSelectionValue == 0) {
			// Choose greedy
			RouteElement newElement = new RouteElement();
			double bestValue = Double.MAX_VALUE;
			for (int key : bestElementPerTw.keySet()) {
				if (bestElementPerTw.get(key).getTempShift() < bestValue) {
					newElement = bestElementPerTw.get(key);
					bestValue = bestElementPerTw.get(key).getTempShift();
				}
			}

			return newElement;
		} else if (this.twSelectionValue == 1) {
			// Choose randomly
			int selectedTw = new Random().nextInt(bestElementPerTw.keySet().size());

			int currentId = 0;
			for (int key : bestElementPerTw.keySet()) {
				if (currentId == selectedTw)
					return bestElementPerTw.get(key);
				currentId++;
			}

			return null;
		} else {

			return this.getBestElementByTwPopularityRatio(bestElementPerTw);

		}

	}

	/**
	 * Chooses the feasible time window with the best popularity ratio
	 * (demand/capacity)
	 * 
	 * @param bestElementPerTw
	 *            Hashmap with key: tw id, entry: best Route element
	 * @return Route element with best popularity ratio * shift
	 */
	private RouteElement getBestElementByTwPopularityRatio(HashMap<Integer, RouteElement> bestElementPerTw) {

		HashMap<Integer, Integer> timeWindowPopularity = new HashMap<Integer, Integer>();
		HashMap<Integer, Double> timeWindowCapacity = new HashMap<Integer, Double>();

		// Get popularity by time window
		for (OrderRequest or : this.orderRequests) {
			// Possible time windows for request
			Alternative noPurchaseAlternative = this.orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet()
					.getAlternativeSet().getNoPurchaseAlternative();

			for (int altId : or.getAlternativePreferences().keySet()) {

				if (or.getAlternativePreferences().get(altId) > or.getAlternativePreferences()
						.get(noPurchaseAlternative.getId())) { //TODO Will crash if no no-purchase altnerative
					for (TimeWindow tw : this.orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet()
							.getAlternativeSet().getAlternativeById(altId).getTimeWindows()) {
						int twId = tw.getId();
						if (timeWindowPopularity.containsKey(twId)) {
							timeWindowPopularity.put(twId, timeWindowPopularity.get(twId) + 1);
						} else {
							timeWindowPopularity.put(twId, 1);
						}
					}
					;
				}
			}
			// TODO: Delete duplicate TW (in case of no 1:1 mapping between tw and alternative)
		}

		// Calculate free capacity per tw
		for (TimeWindow e : this.timeWindowSet.getElements()) {

			double freeCapacity = 0;
			double timeWindowStart = (e.getStartTime()-this.timeWindowSet.getTempStartOfDeliveryPeriod())*TIME_MULTIPLIER;
			double timeWindoWEnd = (e.getEndTime()-this.timeWindowSet.getTempStartOfDeliveryPeriod())*TIME_MULTIPLIER;

			for (int routeId : this.routes.keySet()) {
				ArrayList<RouteElement> route = this.routes.get(routeId);
				int firstElementAfter = route.size() - 1;
				int lastElementInside = 0;
				// Find first after start
				for (int elementId = 1; elementId < route.size() - 1; elementId++) {
					if (route.get(elementId).getServiceBegin()
							+ route.get(elementId).getServiceTime() >= timeWindowStart) {
						firstElementAfter = elementId;
						break;
					}
				}

				if (firstElementAfter == route.size() - 1) {
					freeCapacity = timeWindoWEnd - timeWindowStart;
				} else {

					// Find last within tw
					for (int elementId = route.size() - 2; elementId > firstElementAfter - 1; elementId--) {

						// If element cannot be pushed outside the time
						// window, it is relevant for the free capacity
						// calculation
						if (route.get(elementId).getServiceBegin() + route.get(elementId).getSlack() <= timeWindoWEnd) {
							lastElementInside = elementId;
							break;
						}
					}

					if (lastElementInside == 0) {
						freeCapacity = timeWindoWEnd - timeWindowStart;
					} else {

						freeCapacity = timeWindoWEnd - timeWindowStart;
						for (int inside = firstElementAfter + 1; inside < lastElementInside; inside++) {
							freeCapacity -= route.get(inside).getServiceTime() - route.get(inside).getTravelTimeFrom();
						}

						// Last element:
						double timeInsideTw = Math.min(
								timeWindoWEnd - (route.get(lastElementInside).getServiceBegin()
										+ route.get(lastElementInside).getSlack()),
								route.get(lastElementInside).getServiceTime()
										+ route.get(lastElementInside).getTravelTimeFrom());
						freeCapacity -= timeInsideTw;

						// First element:
						timeInsideTw = route.get(firstElementAfter).getServiceBegin()
								+ route.get(firstElementAfter).getServiceTime()
								+ route.get(firstElementAfter).getTravelTimeFrom() - timeWindowStart;
						if (route.get(firstElementAfter).getServiceBegin() > timeWindowStart) {
							freeCapacity -= Math.min(route.get(firstElementAfter).getTravelTimeTo(),
									route.get(firstElementAfter).getServiceBegin() - timeWindowStart);
							freeCapacity -= route.get(firstElementAfter).getServiceTime()
									- route.get(firstElementAfter).getTravelTimeFrom();
						} else {
							freeCapacity -= timeInsideTw;
						}
					}
				}
				// Add to overall free capacity
				if (timeWindowCapacity.containsKey(e.getId())) {
					timeWindowCapacity.put(e.getId(),
							timeWindowCapacity.get(e.getId()) + freeCapacity);
				} else {
					timeWindowCapacity.put(e.getId(), freeCapacity);
				}

			}

		}

		// Calculate ratio by time window and select best
		double bestRatio = Double.MAX_VALUE;
		RouteElement bestElement = null;

		for (int twKey : bestElementPerTw.keySet()) {
			if (bestElementPerTw.get(twKey).getTempShift() * timeWindowPopularity.get(twKey)
					/ timeWindowCapacity.get(twKey) < bestRatio) {
				bestRatio = bestElementPerTw.get(twKey).getTempShift() * timeWindowPopularity.get(twKey)
						/ timeWindowCapacity.get(twKey);
				bestElement = bestElementPerTw.get(twKey);

			}
		}

		return bestElement;
	}

	/**
	 * Helper method: Finds the cheapest insertion option of a specific route
	 * and returns a route element with the respective information. Returns null
	 * if no insertion is feasible. Ties are always solved randomly.
	 * 
	 * @param route
	 *            Route to insert the request into
	 * @param routeId
	 *            Respective route id
	 * @param request
	 *            Request that has to be inserted
	 * @return
	 */
	private HashMap<Integer, RouteElement> getCheapestInsertionElementByRoute(ArrayList<RouteElement> route,
			int routeId, OrderRequest request) {

		// Possible time windows for request
		// No purchase alternative
		Alternative noPurchaseAlternative = this.orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet()
				.getAlternativeSet().getNoPurchaseAlternative();
		ArrayList<TimeWindow> timeWindows = new ArrayList<TimeWindow>();
		for (int altId : request.getAlternativePreferences().keySet()) {
			if (request.getAlternativePreferences().get(altId) > request.getAlternativePreferences()
					.get(noPurchaseAlternative.getId())) {
				timeWindows.addAll(this.orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet()
						.getAlternativeSet().getAlternativeById(altId).getTimeWindows());
			}
		}

		HashMap<Integer, RouteElement> bestOptionsPerTW = new HashMap<Integer, RouteElement>();

		// Check for all positions (after and before depot)
		for (int position = 1; position < route.size(); position++) {

			RouteElement eBefore = route.get(position - 1);
			RouteElement eAfter = route.get(position);

			double travelTimeTo;
			double travelTimeFrom;
			double travelTimeOld;
			if (this.directDistances != 1) {
				travelTimeTo = this.distanceMatrix
						.get(eBefore.getOrder().getOrderRequest().getCustomer().getClosestNodeId())
						.get(request.getCustomer().getClosestNodeId());
				travelTimeFrom = this.distanceMatrix.get(request.getCustomer().getClosestNodeId())
						.get(eAfter.getOrder().getOrderRequest().getCustomer().getClosestNodeId());
				travelTimeOld = this.distanceMatrix
						.get(eBefore.getOrder().getOrderRequest().getCustomer().getClosestNodeId())
						.get(eAfter.getOrder().getOrderRequest().getCustomer().getClosestNodeId());
			} else {
				travelTimeTo = LocationService.calculateHaversineDistanceBetweenCustomers(
						eBefore.getOrder().getOrderRequest().getCustomer(), request.getCustomer())
						/ this.region.getAverageKmPerHour() * TIME_MULTIPLIER;
				travelTimeFrom = LocationService.calculateHaversineDistanceBetweenCustomers(request.getCustomer(),
						eAfter.getOrder().getOrderRequest().getCustomer()) / this.region.getAverageKmPerHour()
						* TIME_MULTIPLIER;
				travelTimeOld = LocationService.calculateHaversineDistanceBetweenCustomers(
						eBefore.getOrder().getOrderRequest().getCustomer(),
						eAfter.getOrder().getOrderRequest().getCustomer()) / this.region.getAverageKmPerHour()
						* TIME_MULTIPLIER;
			}

			Double maximumPushOfNext = route.get(position).getWaitingTime() + route.get(position).getSlack();
			double shiftWithoutWait;
			if (position == 1) {
				// If we are at first position after depot, the travel time
				// to takes nothing from the capacity
				shiftWithoutWait = travelTimeFrom + expectedServiceTime;
			} else if (position == route.size() - 1) {
				// If we are at last position before depot, the travel time
				// from takes nothing from the capacity
				shiftWithoutWait = travelTimeTo + expectedServiceTime;
			} else {
				shiftWithoutWait = travelTimeTo + travelTimeFrom + expectedServiceTime - travelTimeOld;
			}

			// If shift without wait is already larger than the allowed
			// push, do not consider position further
			if (maximumPushOfNext >= shiftWithoutWait) {
				double earliestStart;
				if (position == 1) {
					earliestStart = 0f;
				} else {
					earliestStart = eBefore.getServiceBegin() + eBefore.getServiceTime() + travelTimeTo;
				}
				double latestStart = Math.min(
						eAfter.getServiceBegin() - travelTimeFrom + eAfter.getSlack() - this.expectedServiceTime,
						timeWindowSet.getTempLengthOfDeliveryPeriod() - 1);

				// Go through possible time windows and check if best value for
				// time window -> update

				for (TimeWindow tw : timeWindows) {

					if (((tw.getEndTime() - this.timeWindowSet.getTempStartOfDeliveryPeriod())
							* TIME_MULTIPLIER > earliestStart)
							&& ((tw.getStartTime() - this.timeWindowSet.getTempStartOfDeliveryPeriod())
									* TIME_MULTIPLIER < latestStart)) {
						double wait = Math.max(0f,
								(tw.getStartTime() - this.timeWindowSet.getTempStartOfDeliveryPeriod())
										* TIME_MULTIPLIER - earliestStart);
						double shift = shiftWithoutWait + wait;
						if (maximumPushOfNext >= shift) { // Feasible with
							// regard to push of
							// next?

							// Update element for time windows if better
							if (!bestOptionsPerTW.containsKey(tw.getId())
									|| shift < bestOptionsPerTW.get(tw.getId()).getTempShift()
									|| ((shift == bestOptionsPerTW.get(tw.getId()).getTempShift())
											&& new Random().nextBoolean())) {
								RouteElement newElement = new RouteElement();
								Order newOrder = new Order();
								newOrder.setOrderRequest(request);
								newOrder.setOrderRequestId(request.getId());
								newElement.setOrder(newOrder);
								newElement.setServiceTime(this.expectedServiceTime);
								newElement.setTempRoute(routeId);
								newElement.setTempPosition(position);
								newElement.getOrder().setTimeWindowFinalId(tw.getId());
								newElement.setTempShift(shift);

								newElement.setTravelTimeTo( travelTimeTo);
								newElement.setTravelTimeFrom( travelTimeFrom);
								newElement.setWaitingTime( wait);
								newElement.setServiceBegin( (earliestStart + wait));
								bestOptionsPerTW.put(tw.getId(), newElement);
							}
						}

					}
				}
			}

		}

		return bestOptionsPerTW;

	}

	private ArrayList<RouteElement> getFeasibleInsertionElementsByRoute(ArrayList<RouteElement> route, int routeId,
			OrderRequest request) {

		// Possible time windows for request
		Alternative noPurchaseAlternative = this.orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet()
				.getAlternativeSet().getNoPurchaseAlternative();
		ArrayList<TimeWindow> timeWindows = new ArrayList<TimeWindow>();
		for (int altId : request.getAlternativePreferences().keySet()) {
			if (request.getAlternativePreferences().get(altId) > request.getAlternativePreferences()
					.get(noPurchaseAlternative.getId())) {
				timeWindows.addAll(this.orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet()
						.getAlternativeSet().getAlternativeById(altId).getTimeWindows());
			}
		}

		ArrayList<RouteElement> possibleInsertions = new ArrayList<RouteElement>();

		// Check for all positions (after and before depot)
		for (int position = 1; position < route.size(); position++) {

			RouteElement eBefore = route.get(position - 1);
			RouteElement eAfter = route.get(position);

			double travelTimeTo;
			double travelTimeFrom;
			double travelTimeOld;
			if (this.directDistances != 1) {
				travelTimeTo = this.distanceMatrix
						.get(eBefore.getOrder().getOrderRequest().getCustomer().getClosestNodeId())
						.get(request.getCustomer().getClosestNodeId());
				travelTimeFrom = this.distanceMatrix.get(request.getCustomer().getClosestNodeId())
						.get(eAfter.getOrder().getOrderRequest().getCustomer().getClosestNodeId());
				travelTimeOld = this.distanceMatrix
						.get(eBefore.getOrder().getOrderRequest().getCustomer().getClosestNodeId())
						.get(eAfter.getOrder().getOrderRequest().getCustomer().getClosestNodeId());
			} else {
				travelTimeTo = LocationService.calculateHaversineDistanceBetweenCustomers(
						eBefore.getOrder().getOrderRequest().getCustomer(), request.getCustomer())
						/ this.region.getAverageKmPerHour() * TIME_MULTIPLIER;
				travelTimeFrom = LocationService.calculateHaversineDistanceBetweenCustomers(request.getCustomer(),
						eAfter.getOrder().getOrderRequest().getCustomer()) / this.region.getAverageKmPerHour()
						* TIME_MULTIPLIER;
				travelTimeOld = LocationService.calculateHaversineDistanceBetweenCustomers(
						eBefore.getOrder().getOrderRequest().getCustomer(),
						eAfter.getOrder().getOrderRequest().getCustomer()) / this.region.getAverageKmPerHour()
						* TIME_MULTIPLIER;
			}

			Double maximumPushOfNext = route.get(position).getWaitingTime() + route.get(position).getSlack();
			double shiftWithoutWait;
			if (position == 1) {
				// If we are at first position after depot, the travel time
				// to takes nothing from the capacity
				shiftWithoutWait = travelTimeFrom + expectedServiceTime;
			} else if (position == route.size() - 1) {
				// If we are at last position before depot, the travel time
				// from takes nothing from the capacity
				shiftWithoutWait = travelTimeTo + expectedServiceTime;
			} else {
				shiftWithoutWait = travelTimeTo + travelTimeFrom + expectedServiceTime - travelTimeOld;
			}

			// If shift without wait is already larger than the allowed
			// push, do not consider position further
			if (maximumPushOfNext >= shiftWithoutWait) {

				// Free time
				double earliestStart;
				if (position == 1) {
					earliestStart = 0f;
				} else {
					earliestStart = eBefore.getServiceBegin() + eBefore.getServiceTime() + travelTimeTo;
				}
				double latestEnd = Math.min(eAfter.getServiceBegin() - travelTimeFrom + eAfter.getSlack(),
						timeWindowSet.getTempLengthOfDeliveryPeriod() - 1 + this.expectedServiceTime);

				// Possible time window insight? Choose first
				TimeWindow firstTimeWindow = null;
				double firstStart = this.timeWindowSet.getTempEndOfDeliveryPeriod();
				double wait = 0;
				for (TimeWindow tw : timeWindows) {
					if ((tw.getStartTime() - this.timeWindowSet.getTempStartOfDeliveryPeriod())
							* TIME_MULTIPLIER < latestEnd - this.expectedServiceTime) {
						if ((tw.getEndTime() - this.timeWindowSet.getTempStartOfDeliveryPeriod())
								* TIME_MULTIPLIER > earliestStart) {
							if (tw.getStartTime() < firstStart) {
								wait = Math.max(0f,
										(tw.getStartTime() - this.timeWindowSet.getTempStartOfDeliveryPeriod())
												* TIME_MULTIPLIER - earliestStart);
								if (this.expectedServiceTime + wait < latestEnd - earliestStart) {
									firstTimeWindow = tw;
									firstStart = tw.getStartTime();
								}

							}
						}
					}
				}
				if (firstTimeWindow != null) {

					double shift = shiftWithoutWait + wait;

					RouteElement newElement = new RouteElement();
					Order newOrder = new Order();
					newOrder.setTimeWindowFinal(firstTimeWindow);
					newOrder.setTimeWindowFinalId(firstTimeWindow.getId());
					newOrder.setOrderRequest(request);
					newOrder.setOrderRequestId(request.getId());
					newElement.setOrder(newOrder);
					newElement.setServiceTime(this.expectedServiceTime);
					newElement.setTempRoute(routeId);
					newElement.setTempPosition(position);
					newElement.setTempShift(shift);
					newElement.setTravelTimeTo( travelTimeTo);
					newElement.setTravelTimeFrom( travelTimeFrom);
					newElement.setServiceBegin( (earliestStart + wait));
					newElement.setWaitingTime( wait);
					newElement.setTempSpaceAroundStart(earliestStart);
					newElement.setTempSpaceAroundEnd(latestEnd);
					possibleInsertions.add(newElement);
				}
			}

		}

		return possibleInsertions;

	}

	/**
	 * Helper function that inserts new route element and updates others
	 * 
	 * @param element
	 *            the new route element
	 * @param routes
	 *            The old status of the routes
	 */
	public void insertRouteElement(RouteElement element) {
		long startTime = System.currentTimeMillis();

		
		ArrayList<RouteElement> route = this.routes.get(element.getTempRoute());
		route.add(element.getTempPosition(), element);

		// Update travel times
		route.get(element.getTempPosition() + 1).setTravelTimeTo(element.getTravelTimeFrom());
		route.get(element.getTempPosition() - 1).setTravelTimeFrom(element.getTravelTimeTo());

		// Update the following elements
		double currentShift = element.getTempShift();
		for (int k = element.getTempPosition() + 1; k < route.size(); k++) {
			if (currentShift == 0)
				break;
			double oldWaitingTime = route.get(k).getWaitingTime();
			route.get(k).setWaitingTime( Math.max(0, oldWaitingTime - currentShift));
			currentShift = Math.max(0, currentShift - oldWaitingTime);
			route.get(k).setServiceBegin( (route.get(k).getServiceBegin() + currentShift));
			route.get(k).setSlack( (route.get(k).getSlack() - currentShift));
		}

		// Update maxShift from current element and the ones before
		for (int k = element.getTempPosition(); k > 0; k--) {
			double maxShift = Math.min(
					(route.get(k).getOrder().getTimeWindowFinal().getEndTime()
							- this.timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER
							- route.get(k).getServiceBegin(),
					route.get(k + 1).getWaitingTime() + route.get(k + 1).getSlack());
			route.get(k).setSlack(maxShift);
		}

		long endTime   = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		averageRuntimeInsert=(averageRuntimeInsert*this.counterInsert+totalTime)/++counterInsert;
		
	}

	/**
	 * Helper method that removes elements from the routes
	 * 
	 * @param routes
	 *            Old status of the routes
	 * @param numberOfRemovalsPerRoute
	 * @param startPositions
	 */
	private void removeRouteElements(int numberOfRemovalsPerRoute, int[] startPositions) {

		for (Integer routeId : this.routes.keySet()) {
			this.removeRouteElementsOfRoute(this.routes.get(routeId), numberOfRemovalsPerRoute, startPositions[routeId],
					true);
		}
	}

	/**
	 * Helper method that removes elements from a specific route
	 * 
	 * @param routes
	 *            Old status of the route
	 * @param numberOfRemovalsPerRoute
	 * @param startPosition
	 * @param addToUnassigned
	 *            If the element is already planned for another root, it should
	 *            not be added to unassigned
	 */
	private void removeRouteElementsOfRoute(ArrayList<RouteElement> route, int numberOfRemovalsPerRoute,
			int startPosition, boolean addToUnassigned) {

		int positionToRemove = startPosition;
		RouteElement removedElement = new RouteElement();
		boolean stopNow = false;
		

		for (int i = 0; i < numberOfRemovalsPerRoute; i++) {
			// Start at beginning if end of route is reached. Do not delete
			// depots.
			if (positionToRemove == route.size() - 1) {
				// Begin at start
				positionToRemove = 1;

				// If route is empty, stop removing
				if (route.size() == 2)
					break;

			}

			if (stopNow) {
				System.out.println("");
			}
			removedElement = route.remove(positionToRemove);

			if (addToUnassigned) {

				this.orderRequests.add(removedElement.getOrder().getOrderRequest());

			}

		}

		// Forward: Update waiting time and service time begin

		/// Calculate travel time from first unchanged to the first that
		/// needs to be shifted forward.
		double travelTimeTo;
		if (this.directDistances != 1) {
			travelTimeTo = this.distanceMatrix
					.get(route.get(positionToRemove - 1).getOrder().getOrderRequest().getCustomer().getClosestNodeId())
					.get(route.get(positionToRemove).getOrder().getOrderRequest().getCustomer().getClosestNodeId());
		} else {
			travelTimeTo = LocationService.calculateHaversineDistanceBetweenCustomers(
					route.get(positionToRemove - 1).getOrder().getOrderRequest().getCustomer(),
					route.get(positionToRemove).getOrder().getOrderRequest().getCustomer())
					/ this.region.getAverageKmPerHour() * TIME_MULTIPLIER;
		}

		route.get(positionToRemove - 1).setTravelTimeFrom( travelTimeTo);
		route.get(positionToRemove).setTravelTime( travelTimeTo);

		for (int i = positionToRemove; i < route.size(); i++) {
			RouteElement eBefore = route.get(i - 1);
			RouteElement eNew = route.get(i);

			// Update service begin and waiting time

			double arrivalTime;
			// If it is the first element, the service can begin at the
			// start of the time window
			if (i == 1) {
				arrivalTime = 0f;
			} else {
				arrivalTime = eBefore.getServiceBegin() + eBefore.getServiceTime() + eBefore.getTravelTimeFrom();
			}
			double oldServiceBegin = eNew.getServiceBegin();
			double newServiceBegin;
			if (i < route.size() - 1) {
				newServiceBegin = Math.max(arrivalTime, (eNew.getOrder().getTimeWindowFinal().getStartTime()
						- this.timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER);
			} else {
				// For end depot
				newServiceBegin = arrivalTime;
			}

			eNew.setServiceBegin(newServiceBegin);
			eNew.setWaitingTime(newServiceBegin - arrivalTime);

			// If the service begin does not change, the following elements
			// can stay as before
			if (oldServiceBegin == newServiceBegin)
				break;
		}

		/// Calculate travel time from last to depot.
		if (this.directDistances != 1) {
			travelTimeTo = this.distanceMatrix
					.get(route.get(route.size() - 2).getOrder().getOrderRequest().getCustomer().getClosestNodeId())
					.get(route.get(route.size() - 1).getOrder().getOrderRequest().getCustomer().getClosestNodeId());
		} else {
			travelTimeTo = LocationService.calculateHaversineDistanceBetweenCustomers(
					route.get(route.size() - 2).getOrder().getOrderRequest().getCustomer(),
					route.get(route.size() - 1).getOrder().getOrderRequest().getCustomer())
					/ this.region.getAverageKmPerHour() * TIME_MULTIPLIER;

		}
		route.get(route.size() - 2).setTravelTimeFrom( travelTimeTo);
		route.get(route.size() - 1).setTravelTime( travelTimeTo);

		// Backward: Update maximum shift (slack)

		/// For end depot
		route.get(route.size() - 1).setSlack(
				this.timeWindowSet.getTempLengthOfDeliveryPeriod() * 2 - route.get(route.size() - 1).getServiceBegin());

		/// For others
		for (int i = route.size() - 2; i > 0; i--) {
			Double maxShift = Math.min(
					(route.get(i).getOrder().getTimeWindowFinal().getEndTime()
							- this.timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER
							- route.get(i).getServiceBegin(),
					route.get(i + 1).getWaitingTime() + route.get(i + 1).getSlack());
			route.get(i).setSlack(maxShift);
		}

	}

	/**
	 * Initialises one list of route elements per vehicle and summarizes routes
	 * in a hashmap (with vehicle id as key)
	 * 
	 * @param depotNode
	 * @return
	 */
	private HashMap<Integer, ArrayList<RouteElement>> initialiseRoutes(Node depotNode) {

		RoutingService.getPossibleDurationOfRouteByTimeWindowSet(timeWindowSet, TIME_MULTIPLIER);

		HashMap<Integer, ArrayList<RouteElement>> routes = new HashMap<Integer, ArrayList<RouteElement>>();

		// Create as many routes as vehicles
		this.numberOfVehicles = 0;
		for (int vehicleTypeNo = 0; vehicleTypeNo < vehicles.size(); vehicleTypeNo++) {
			for (int vehicleNo = 0; vehicleNo < ((Vehicle) vehicles.get(vehicleTypeNo)).getVehicleNo(); vehicleNo++) {

				/// Create a route
				ArrayList<RouteElement> route = new ArrayList<RouteElement>();
				/// Add start and end in depot
				RouteElement startE = new RouteElement();
				startE.setServiceBegin(0.0);
				startE.setServiceTime(0.0);
				startE.setSlack(0.0);
				startE.setWaitingTime(0.0);
				startE.setTravelTimeTo(0.0);
				OrderRequest startR = new OrderRequest();
				Customer startCustomer = new Customer();
				if (depotNode != null) {
					startCustomer.setClosestNode(depotNode);
					startCustomer.setClosestNodeId(depotNode.getLongId());
				}
				startCustomer.setLat(this.depot.getLat());
				startCustomer.setLon(this.depot.getLon());
				startR.setCustomer(startCustomer);
				Order order = new Order();
				order.setOrderRequest(startR);
				startE.setOrder(order);
				route.add(startE);

				RouteElement endE = new RouteElement();
				endE.setServiceBegin(0.0);
				endE.setServiceTime(0.0);
				// Dummy. Begin can be pushed back because service at end of
				// time window is allowed.
				endE.setSlack(this.timeWindowSet.getTempLengthOfDeliveryPeriod() * 2);
				endE.setWaitingTime(0.0);
				endE.setTravelTimeTo(0.0);
				OrderRequest endR = new OrderRequest();
				Customer endCustomer = new Customer();
				if (depotNode != null) {
					endCustomer.setClosestNode(depotNode);
					endCustomer.setClosestNodeId(depotNode.getLongId());
				}

				endCustomer.setLat(this.depot.getLat());
				endCustomer.setLon(this.depot.getLon());

				endR.setCustomer(endCustomer);
				Order endOrder = new Order();
				endOrder.setOrderRequest(endR);
				endE.setOrder(endOrder);
				route.add(endE);

				routes.put(numberOfVehicles++, route);

			}
		}
		return routes;
	}

	private RouteElement getRouteElementforRequestWithBestNeighborhood(int neighborhoodSize) {

		RouteElement bestElement = null;
		double bestValue = 0;
		for (OrderRequest request : this.orderRequests) {
			RouteElement possibleElement = this.calculateNeighborhoodValueOfRequest(request, neighborhoodSize);
			if (possibleElement != null && possibleElement.getTempNeihborhoodValue() > bestValue) {
				bestElement = possibleElement;
				bestValue = possibleElement.getTempNeihborhoodValue();
			}
			;
		}
		return bestElement;

	}

	private RouteElement calculateNeighborhoodValueOfRequest(OrderRequest request, int neighborhoodSize) {

		// Find the closest unassigned requests
		@SuppressWarnings("unchecked")
		ArrayList<OrderRequest> neighbors = (ArrayList<OrderRequest>) this.orderRequests.clone();

		if (this.directDistances != 1) {
			Collections.sort(neighbors, new OrderRequestNodeDistanceComparator(request, this.distanceMatrix));
		} else {
			Collections.sort(neighbors, new OrderRequestDistanceComparator(request));

		}
		// Get insertion costs and slack for feasible insertion positions
		ArrayList<RouteElement> possibleInsertions = new ArrayList<RouteElement>();

		for (int routeId : this.routes.keySet()) {
			possibleInsertions
					.addAll(this.getFeasibleInsertionElementsByRoute(this.routes.get(routeId), routeId, request));
		}

		// Calculate value of neighbors for the insertion options. Return best.
		double bestValue = 0;
		RouteElement bestElement = null;
		int currentOptionNumber = 0;
		for (RouteElement option : possibleInsertions) {
			int feasibleNeighbors = 0;
			double neighborhoodValue = 0;
			int currentNeighbornumber = 0;
			currentOptionNumber++;
			for (OrderRequest neighbor : neighbors) {
				currentNeighbornumber++;
				if (feasibleNeighbors >= neighborhoodSize)
					break;

				// Possible time windows for request
				Alternative noPurchaseAlternative = this.orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet()
						.getAlternativeSet().getNoPurchaseAlternative();
				ArrayList<TimeWindow> timeWindows = new ArrayList<TimeWindow>();
				for (int altId : request.getAlternativePreferences().keySet()) {
					if (request.getAlternativePreferences().get(altId) > request.getAlternativePreferences()
							.get(noPurchaseAlternative.getId())) {
						timeWindows.addAll(this.orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet()
								.getAlternativeSet().getAlternativeById(altId).getTimeWindows());
					}
				}

				boolean isPossible = false;
				for (TimeWindow tw : timeWindows) {
					
					if ((tw.getEndTime() - this.timeWindowSet.getTempStartOfDeliveryPeriod())
							* TIME_MULTIPLIER >= option.getTempSpaceAroundStart()
							&& (tw.getStartTime() - this.timeWindowSet.getTempStartOfDeliveryPeriod())
									* TIME_MULTIPLIER <= option.getTempSpaceAroundEnd()) {
						isPossible = true;
						break;
					}
				}
				if (neighbors.size() > 10 && currentNeighbornumber > 5 && bestValue == 0 && currentOptionNumber > 1) {
					System.out.println("Not likely");
				}
				if (isPossible) {
					feasibleNeighbors++;

					// Add value of neighbor (score (2) / (distance + service
					// time))
					double distance;
					if (this.directDistances != 1) {
						distance = this.distanceMatrix.get(request.getCustomer().getClosestNodeId())
								.get(neighbor.getCustomer().getClosestNodeId());
						distance += this.distanceMatrix.get(neighbor.getCustomer().getClosestNodeId())
								.get(request.getCustomer().getClosestNodeId());
						distance = distance / 2;
					} else {
						distance = LocationService.calculateHaversineDistanceBetweenCustomers(request.getCustomer(),
								neighbor.getCustomer()) / this.region.getAverageKmPerHour() * TIME_MULTIPLIER;
					}

					double score = 0;
					if (this.actualBasketValue == 1) {
						score = neighbor.getBasketValue();
					} else {
						try {
							score = ProbabilityDistributionService.getMeanByProbabilityDistribution(
									neighbor.getCustomer().getOriginalDemandSegment().getBasketValueDistribution());
						} catch (ParameterUnknownException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
							System.exit(0);

						}
					}

					double timeCosts = distance / 2 + this.expectedServiceTime;
					if (this.squaredValue == 1) {
						neighborhoodValue += Math.sqrt(score) / timeCosts;
					} else {
						neighborhoodValue += score / timeCosts;
					}

				}
			}

			if (bestValue < neighborhoodValue) {
				bestElement = option;
				bestValue = neighborhoodValue;
			}
		}

		// No feasible neighborhood?
		if (bestElement == null)
			return null;
		// Return insertion option with highest valued neighborhood
		bestElement.setTempNeighborhoodValue(bestValue);
		return bestElement;
	}

	/**
	 * Checks if new solution is better than currently best solution and if yes,
	 * best solution is updated
	 * 
	 * @param newSolution
	 * @return Boolean if best solution was updated
	 */
	private Boolean updateBestSolution(HashMap<Integer, ArrayList<RouteElement>> newSolution, double newValue,
			double currentGreediness) {
		Boolean improvement = false;
		if (newValue > bestValue) {
			bestValue = newValue;
			improvement = true;
			this.bestCompletelyShaked = false;
			HashMap<Integer, ArrayList<RouteElement>> newBestSolution = this.copySolution(newSolution);

			this.bestSolution = newBestSolution;

			int lengthOfLongest = 0;
			for (int key : newBestSolution.keySet()) {
				if (newBestSolution.get(key).size() > lengthOfLongest) {
					lengthOfLongest = newBestSolution.get(key).size();
				}
			}
			this.sizeOfLongestRouteBestSolution = lengthOfLongest - 2;

			this.lastUpdateInformation = "Value: " + this.bestValue + "; Greediness: " + currentGreediness
					+ "; No. solutions: " + this.numberOfProducedSolutions;
		}

		return improvement;
	}

	private HashMap<Integer, ArrayList<RouteElement>> copySolution(HashMap<Integer, ArrayList<RouteElement>> toCopy) {
		HashMap<Integer, ArrayList<RouteElement>> newSolution = new HashMap<Integer, ArrayList<RouteElement>>();
		for (Integer routeId : toCopy.keySet()) {
			ArrayList<RouteElement> elements = toCopy.get(routeId);
			ArrayList<RouteElement> newElements = new ArrayList<RouteElement>();
			for (RouteElement e : elements) {
				RouteElement eCopy = e.copyElement();
				eCopy.setOrder(e.getOrder()); // As the order does not have
												// an id, it is not copied
												// before
				newElements.add(eCopy);
			}
			newSolution.put(routeId, newElements);
		}

		return newSolution;
	}

	private double getShiftValue(RouteElement insertionOption) {
		double insertionValue = 0;
		if (this.actualBasketValue == 1) {
			insertionValue = insertionOption.getOrder().getOrderRequest().getBasketValue();
		} else {

			try {
				insertionValue = ProbabilityDistributionService
						.getMeanByProbabilityDistribution(insertionOption.getOrder().getOrderRequest().getCustomer()
								.getOriginalDemandSegment().getBasketValueDistribution());
			} catch (ParameterUnknownException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(0);
			}
		}

		if (this.squaredValue == 1) {
			insertionValue = Math.sqrt(insertionValue);
		}
		return insertionValue / insertionOption.getTempShift();
	}

	/**
	 * Evaluates a routing based on the revenue
	 * 
	 * @param solution
	 *            Routing
	 * @return Overall revenue
	 */
	private Double evaluateSolution(HashMap<Integer, ArrayList<RouteElement>> solution) {

		Double value = 0.0;
		for (Integer routeId : solution.keySet()) {
			ArrayList<RouteElement> elements = solution.get(routeId);
			for (int elementId = 1; elementId < elements.size() - 1; elementId++) {
				if (this.actualBasketValue == 1) {
					value += elements.get(elementId).getOrder().getOrderRequest().getBasketValue();
				} else {
					try {
						value += ProbabilityDistributionService
								.getMeanByProbabilityDistribution(elements.get(elementId).getOrder().getOrderRequest()
										.getCustomer().getOriginalDemandSegment().getBasketValueDistribution());
					} catch (ParameterUnknownException e) {
						e.printStackTrace();
						System.exit(0);
					}
				}
			}
		}

		return value;
	}

	public static String[] getParameterSetting() {
		return paras;
	}

	public Routing getResult() {
		return finalRouting;
	}
}
