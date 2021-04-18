package logic.algorithm.vr.finalFeasibilityCheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.Depot;
import data.entity.Order;
import data.entity.OrderSet;
import data.entity.Region;
import data.entity.Route;
import data.entity.RouteElement;
import data.entity.Routing;
import data.entity.TimeWindowSet;
import data.entity.VehicleAreaAssignment;
import data.entity.VehicleAreaAssignmentSet;
import logic.algorithm.vr.RoutingAlgorithm;
import logic.service.support.DynamicRoutingHelperService;
import logic.service.support.LocationService;
import logic.utility.comparator.RouteElementInsertionValueDescComparator;
import logic.utility.comparator.RouteElementTempShiftValueAscComparator;

public class ILSWithGRASPForFinalRouting implements RoutingAlgorithm {

	private static double TIME_MULTIPLIER = 60.0;
	private TimeWindowSet timeWindowSet;
	private Routing finalRouting;
	private OrderSet orderSet;
	private ArrayList<Order> orders;
	private VehicleAreaAssignmentSet vehicleAreaAssignmentSet;
	private DeliveryAreaSet deliveryAreaSet;
	private HashMap<Integer, ArrayList<VehicleAreaAssignment>> vehicleAssignmentsPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, VehicleAreaAssignment>> vasPerDeliveryAreaAndVehicleNo;
	private Region region;
	private int includeDriveFromStartingPosition;
	private int bestSolutionInfeasibleNo; // Value of the best solution
	private Double bestSolutionProfit; // Value of the best solution
	private Double greedinessUpperBound;
	private Double greedinessLowerBound;
	private Double greedinessStepsize;
	private int maximumRoundsWithoutImprovement;
	private int numberOfProducedSolutions;
	private int maximumNumberOfSolutions;
	private double expectedServiceTime;
	private boolean bestCompletelyShaked;
	private HashMap<Integer, HashMap<Integer, ArrayList<RouteElement>>> bestSolution;
	private int sizeOfLongestRouteBestSolution;
	private ArrayList<Order> ordersLeftOverBest;
	private String lastUpdateInformation;
	private boolean onlyCostBased;
	private boolean squaredValue;
	private Depot depot;

	private static String[] paras = new String[] { "maximumRoundsWithoutImprovement", "greediness_upperBound",
			"greediness_lowerBound", "greediness_stepsize", "includeDriveFromStartingPosition",
			"maximumNumberOfSolutions", "Constant_service_time", "only_cost_based", "squaredValue" };

	public ILSWithGRASPForFinalRouting(Region region, Depot depot, OrderSet orderSet, Double greedinessUpperBound,
			Double greedinessLowerBound, Double greedinessStepsize, Double maximumRoundsWithoutImprovement,
			Double maximumNumberOfSolutions, VehicleAreaAssignmentSet vehicleAreaAssignmentSet,
			Double expectedServiceTime, Double includeDriveFromStartingPosition, Double onlyCostBased,
			Double squaredValue) {

		this.orderSet = orderSet;
		this.depot = depot;
		this.vehicleAreaAssignmentSet = vehicleAreaAssignmentSet;
		this.deliveryAreaSet = this.vehicleAreaAssignmentSet.getDeliveryAreaSet();
		this.timeWindowSet = orderSet.getOrderRequestSet().getCustomerSet().getOriginalDemandSegmentSet()
				.getAlternativeSet().getTimeWindowSet();
		this.region = region;
		this.includeDriveFromStartingPosition = includeDriveFromStartingPosition.intValue();
		this.greedinessUpperBound = greedinessUpperBound;
		this.greedinessLowerBound = greedinessLowerBound;
		this.greedinessStepsize = greedinessStepsize;
		this.maximumNumberOfSolutions = maximumNumberOfSolutions.intValue();
		this.numberOfProducedSolutions = 0;
		this.expectedServiceTime = expectedServiceTime;
		this.maximumRoundsWithoutImprovement = maximumRoundsWithoutImprovement.intValue();

		if (onlyCostBased == 1.0) {
			this.onlyCostBased = true;
		} else {
			this.onlyCostBased = false;
		}

		if (squaredValue == 1.0) {
			this.squaredValue = true;
		} else {
			this.squaredValue = false;
		}
	}

	public void start() {

		// Initialise vehicle assignments
		this.vehicleAssignmentsPerDeliveryArea = new HashMap<Integer, ArrayList<VehicleAreaAssignment>>();
		this.vasPerDeliveryAreaAndVehicleNo = new HashMap<Integer, HashMap<Integer, VehicleAreaAssignment>>();
		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			this.vehicleAssignmentsPerDeliveryArea.put(area.getId(), new ArrayList<VehicleAreaAssignment>());
			if (area.getSubsetId() != 0) {
				this.vasPerDeliveryAreaAndVehicleNo.put(area.getSubsetId(),
						new HashMap<Integer, VehicleAreaAssignment>());
			} else {
				this.vasPerDeliveryAreaAndVehicleNo.put(area.getSetId(), new HashMap<Integer, VehicleAreaAssignment>());
			}
		}

		for (VehicleAreaAssignment ass : this.vehicleAreaAssignmentSet.getElements()) {
			this.vehicleAssignmentsPerDeliveryArea.get(ass.getDeliveryAreaId()).add(ass);
			if (ass.getDeliveryArea().getSubsetId() != 0) {
				this.vasPerDeliveryAreaAndVehicleNo.get(ass.getDeliveryArea().getSubsetId()).put(ass.getVehicleNo(),
						ass);
			} else {
				this.vasPerDeliveryAreaAndVehicleNo.get(ass.getDeliveryArea().getSetId()).put(ass.getVehicleNo(), ass);
			}
		}

		ArrayList<Order> ordersE = this.orderSet.getElements();
		this.orders = new ArrayList<Order>();

		for (Order order : ordersE) {
			if (order.getAccepted())
				orders.add(order);
		}

		// Initialise routes (per delivery area set, to cover delivery area
		// hierarchies)
		HashMap<Integer, HashMap<Integer, ArrayList<RouteElement>>> routesPerDeliveryAreaSet = new HashMap<Integer, HashMap<Integer, ArrayList<RouteElement>>>();
		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			if (area.getSubsetId() != 0) {
				routesPerDeliveryAreaSet.put(area.getSubsetId(),
						DynamicRoutingHelperService.initialiseRoutes(area, vehicleAssignmentsPerDeliveryArea,
								timeWindowSet, TIME_MULTIPLIER, region,  (includeDriveFromStartingPosition==1)));
			} else {
				routesPerDeliveryAreaSet.put(area.getSetId(),
						DynamicRoutingHelperService.initialiseRoutes(area, vehicleAssignmentsPerDeliveryArea,
								timeWindowSet, TIME_MULTIPLIER, region,  (includeDriveFromStartingPosition==1)));
			}

		}

		// Initialise best value
		this.bestSolutionInfeasibleNo = this.orders.size();

		// Outer loop: decrease greediness
		Double currentGreediness = this.greedinessUpperBound;
		while (currentGreediness >= this.greedinessLowerBound) {

			// Double currentGreediness = this.greedinessLowerBound;
			// while (currentGreediness <= this.greedinessUpperBound) {

			int noRepetitionsWithoutImprovement = 0;
			int numberOfRemovalsPerRoute = 1;

			HashMap<Integer, HashMap<Integer, Integer>> startPositions = new HashMap<Integer, HashMap<Integer, Integer>>();

			// Initialise start positions with 1 (depot cannot be removed)
			for (Integer i : routesPerDeliveryAreaSet.keySet()) {
				HashMap<Integer, Integer> r = new HashMap<Integer, Integer>();
				for (Integer j : routesPerDeliveryAreaSet.get(i).keySet()) {
					r.put(j, 1);

				}
				startPositions.put(i, r);
			}
			boolean noRestart = true;
			while (noRestart) {
				// Fill up routes with unassigned requests
				if (this.maximumNumberOfSolutions <= this.numberOfProducedSolutions)
					break;

				if (this.bestSolutionInfeasibleNo < 1)
					break;

				this.fillUpRoutes(currentGreediness, false, routesPerDeliveryAreaSet, vasPerDeliveryAreaAndVehicleNo);

				this.numberOfProducedSolutions++;

				// Check for improvement and update best solution as well as
				// parameters
				int newInfeasibleNo = this.orders.size();
				double bestValue = this.evaluateSolution(routesPerDeliveryAreaSet);
				boolean improvement = this.updateBestSolution(routesPerDeliveryAreaSet, newInfeasibleNo, bestValue,
						currentGreediness);

				if (improvement) {
					noRepetitionsWithoutImprovement = 0;
					numberOfRemovalsPerRoute = 1;

					// -> new start is best solution

				} else {
					noRepetitionsWithoutImprovement++;
				}

				// Caution: adaption of original algorithm, 1.) change value of
				// startPosition based on filled up routes, 2.) adapt all
				// individual S such that feasible
				int lengthOfLongestRoute = 0;

				for (Integer i : startPositions.keySet()) {
					for (Integer j : startPositions.get(i).keySet()) {
						if (startPositions.get(i).get(j) >= routesPerDeliveryAreaSet.get(i).get(j).size() - 1) {
							startPositions.get(i).put(j,
									startPositions.get(i).get(j) % (routesPerDeliveryAreaSet.get(i).get(j).size() - 1)
											+ 1);
						}

						if (routesPerDeliveryAreaSet.get(i).get(j).size() - 2 > lengthOfLongestRoute) {
							lengthOfLongestRoute = routesPerDeliveryAreaSet.get(i).get(j).size() - 2;
						}
					}
				}
				// Complete routes where exchanged since last improvement? Or
				// maximum rounds without improvement reached? Then stop
				if (maximumRoundsWithoutImprovement < 1 && numberOfRemovalsPerRoute > lengthOfLongestRoute) {
					System.out.println("Start next round because already exchanged all. " + numberOfRemovalsPerRoute);
					noRestart = false;
				} else if (maximumRoundsWithoutImprovement > 0
						&& noRepetitionsWithoutImprovement > maximumRoundsWithoutImprovement) {
					noRestart = false;
				} else {
					// Attention: Change, R cannot be larger than the length of
					// the
					// longest route (would have no effect)
					if (numberOfRemovalsPerRoute > lengthOfLongestRoute) {
						numberOfRemovalsPerRoute = 1;
					}
				}

				this.removeRouteElements(numberOfRemovalsPerRoute, startPositions, routesPerDeliveryAreaSet,
						vasPerDeliveryAreaAndVehicleNo);

				for (Integer da : startPositions.keySet()) {
					for (Integer r : startPositions.get(da).keySet()) {
						startPositions.get(da).put(r, startPositions.get(da).get(r) + numberOfRemovalsPerRoute);
					}
				}

				numberOfRemovalsPerRoute++;

			}
			if (this.maximumNumberOfSolutions <= this.numberOfProducedSolutions)
				break;
			currentGreediness -= this.greedinessStepsize;
			// currentGreediness += this.greedinessStepsize;
		}

		System.out.println("Number of infeasible: " + this.bestSolutionInfeasibleNo);
		// The best solution found is returned as routing
		ArrayList<Route> finalRoutes = new ArrayList<Route>();

		for (Integer daId : bestSolution.keySet()) {
			for (Integer routeId : bestSolution.get(daId).keySet()) {
				Route route = new Route();

				// Delete dummy depot elements
				bestSolution.get(daId).get(routeId).remove(bestSolution.get(daId).get(routeId).size() - 1);
				bestSolution.get(daId).get(routeId).remove(0);

				for (RouteElement e : bestSolution.get(daId).get(routeId)) {
					e.setTimeWindowId(e.getOrder().getTimeWindowFinalId());
					e.setTravelTime(e.getTravelTimeTo());
				}

				route.setRouteElements(bestSolution.get(daId).get(routeId));
				route.setVehicleAreaAssignmentId(vasPerDeliveryAreaAndVehicleNo.get(daId).get(routeId).getId());
				finalRoutes.add(route);
			}
		}
		double additionalCosts = 0.0;
		if (this.bestSolutionInfeasibleNo > 0) {
			for (Order order : this.ordersLeftOverBest) {
				additionalCosts += LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(
						order.getOrderRequest().getCustomer().getLat(), order.getOrderRequest().getCustomer().getLon(),
						depot.getLat(), depot.getLon());
			}
		}
		
		additionalCosts = additionalCosts/ region.getAverageKmPerHour()
				* TIME_MULTIPLIER;
		this.finalRouting = new Routing();
		this.finalRouting.setPossiblyFinalRouting(true);
		this.finalRouting.setAdditionalCosts(additionalCosts);
		this.finalRouting.setVehicleAreaAssignmentSetId(this.vehicleAreaAssignmentSet.getId());
		this.finalRouting.setRoutes(finalRoutes);
		this.finalRouting.setTimeWindowSet(timeWindowSet);
		this.finalRouting.setTimeWindowSetId(timeWindowSet.getId());
		this.finalRouting.setOrderSetId(orderSet.getId());
		this.finalRouting.setAdditionalInformation(this.lastUpdateInformation);

	}

	private void fillUpRoutes(Double greedinessFactor, boolean again,
			HashMap<Integer, HashMap<Integer, ArrayList<RouteElement>>> routesPerDeliveryAreaSet,
			HashMap<Integer, HashMap<Integer, VehicleAreaAssignment>> vasPerDeliveryAreaAndVehicleNo) {

		// Assign unassigned requests to the routes. Stop if no requests are
		// left
		// over (or if no assignment feasible)
		while (this.orders.size() > 0) {

			// Insertion options
			ArrayList<RouteElement> insertionOptions = new ArrayList<RouteElement>();

			// Go through unassigned requests and define cheapest insertion per
			// request

			for (Order order : this.orders) {
				// Determine respective delivery area
				DeliveryArea rArea = LocationService.assignCustomerToDeliveryAreaConsideringHierarchy(
						this.deliveryAreaSet, order.getOrderRequest().getCustomer());
				RouteElement optionalElement = DynamicRoutingHelperService.getCheapestInsertionElementByOrder(order,
						routesPerDeliveryAreaSet.get(rArea.getSetId()),
						vasPerDeliveryAreaAndVehicleNo.get(rArea.getSetId()), region, TIME_MULTIPLIER,
						this.expectedServiceTime, timeWindowSet,  (includeDriveFromStartingPosition==1), false);

				if (optionalElement != null) {
					optionalElement.setDeliveryArea(rArea);
					insertionOptions.add(optionalElement);
				}
			}

			// Stop if no feasible insertions exist
			if (insertionOptions.size() == 0) {
				return;
			}

			// Sort regarding value to find maximum and minimum value
			int chosenIndex = 0;
			int borderElement = 0; // The first element is definitely higher
			if (this.onlyCostBased) {
				Collections.sort(insertionOptions, new RouteElementTempShiftValueAscComparator());

				// Calculate value border based on greediness factor
				double min = insertionOptions.get(0).getTempShift();

				double max = insertionOptions.get(insertionOptions.size() - 1).getTempShift();

				double valueBorder = max - (max - min) * greedinessFactor;
				// Find index of element with lowest value below the border

				while (borderElement < insertionOptions.size() - 1) {

					double elementValue = insertionOptions.get(borderElement + 1).getTempShift();

					if (elementValue <= valueBorder) {
						borderElement++;
					} else {
						break; // Stop because list is sorted and all afterwards
								// will not be higher
					}
				}
			} else {
				Collections.sort(insertionOptions,
						new RouteElementInsertionValueDescComparator(true, this.squaredValue));

				// Calculate value border based on greediness factor
				double max = this.getShiftValue(insertionOptions.get(0));

				double min = this.getShiftValue(insertionOptions.get(insertionOptions.size() - 1));

				double valueBorder = min + (max - min) * greedinessFactor;
				// Find index of element with lowest value below the border

				while (borderElement < insertionOptions.size() - 1) {

					double elementValue = this.getShiftValue(insertionOptions.get(borderElement + 1));

					if (elementValue >= valueBorder) {
						borderElement++;
					} else {
						break; // Stop because list is sorted and all afterwards
								// will not be higher
					}
				}
			}

			// Choose a random number between 0 and the borderElement
			Random randomGenerator = new Random();
			chosenIndex = randomGenerator.nextInt(borderElement + 1);

			// Insert the respective Element in the route
			RouteElement toInsert = insertionOptions.get(chosenIndex);
			DynamicRoutingHelperService.insertRouteElement(toInsert,
					routesPerDeliveryAreaSet.get(toInsert.getDeliveryArea().getSetId()),
					vasPerDeliveryAreaAndVehicleNo.get(toInsert.getDeliveryArea().getSetId()), timeWindowSet,
					TIME_MULTIPLIER, (includeDriveFromStartingPosition==1));
			// Delete the respective order from the unassigned orders
			this.orders.remove(insertionOptions.get(chosenIndex).getOrder());

		}
	}

	private Boolean updateBestSolution(HashMap<Integer, HashMap<Integer, ArrayList<RouteElement>>> newSolution,
			int newInfeasibleNo, double newValue, double currentGreediness) {
		Boolean improvement = false;
		if ((newInfeasibleNo < bestSolutionInfeasibleNo)
				|| ((newInfeasibleNo == bestSolutionInfeasibleNo) && this.bestSolutionProfit < newValue)) {
			this.bestSolutionInfeasibleNo = newInfeasibleNo;
			this.bestSolutionProfit = newValue;
			improvement = true;
			this.bestCompletelyShaked = false;
			HashMap<Integer, HashMap<Integer, ArrayList<RouteElement>>> newBestSolution = this
					.copySolution(newSolution);

			this.bestSolution = newBestSolution;

			int lengthOfLongest = 0;
			for (int key : newBestSolution.keySet()) {
				if (newBestSolution.get(key).size() > lengthOfLongest) {
					lengthOfLongest = newBestSolution.get(key).size();
				}
			}
			this.sizeOfLongestRouteBestSolution = lengthOfLongest - 2;
			this.ordersLeftOverBest = (ArrayList<Order>) this.orders.clone();

			this.lastUpdateInformation = "Value: " + this.bestSolutionInfeasibleNo + "; Greediness: "
					+ currentGreediness + "; No. solutions: " + this.numberOfProducedSolutions;
		}

		return improvement;
	}

	private HashMap<Integer, HashMap<Integer, ArrayList<RouteElement>>> copySolution(
			HashMap<Integer, HashMap<Integer, ArrayList<RouteElement>>> toCopy) {
		HashMap<Integer, HashMap<Integer, ArrayList<RouteElement>>> newSolution = new HashMap<Integer, HashMap<Integer, ArrayList<RouteElement>>>();

		for (Integer daId : toCopy.keySet()) {
			HashMap<Integer, ArrayList<RouteElement>> perDa = new HashMap<Integer, ArrayList<RouteElement>>();
			for (Integer routeId : toCopy.get(daId).keySet()) {
				ArrayList<RouteElement> elements = toCopy.get(daId).get(routeId);
				ArrayList<RouteElement> newElements = new ArrayList<RouteElement>();
				for (RouteElement e : elements) {
					RouteElement eCopy = e.copyElement();
					eCopy.setOrder(e.getOrder());
					newElements.add(eCopy);
				}
				perDa.put(routeId, newElements);
			}
			newSolution.put(daId, perDa);
		}
		return newSolution;
	}

	/**
	 * Helper method that removes elements from the routes
	 * 
	 * @param routes
	 *            Old status of the routes
	 * @param numberOfRemovalsPerRoute
	 * @param startPositions
	 */
	private void removeRouteElements(int numberOfRemovalsPerRoute,
			HashMap<Integer, HashMap<Integer, Integer>> startPositions,
			HashMap<Integer, HashMap<Integer, ArrayList<RouteElement>>> routesPerDeliveryAreaSet,
			HashMap<Integer, HashMap<Integer, VehicleAreaAssignment>> vasPerDeliveryAreaAndVehicleNo) {

		for (Integer i : startPositions.keySet()) {
			for (Integer j : startPositions.get(i).keySet()) {
				this.removeRouteElementsOfRoute(routesPerDeliveryAreaSet.get(i).get(j),
						vasPerDeliveryAreaAndVehicleNo.get(i).get(j), numberOfRemovalsPerRoute,
						startPositions.get(i).get(j), true);
			}
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
	private void removeRouteElementsOfRoute(ArrayList<RouteElement> route, VehicleAreaAssignment vaa,
			int numberOfRemovalsPerRoute, int startPosition, boolean addToUnassigned) {

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

			removedElement = route.remove(positionToRemove);

			if (addToUnassigned) {

				this.orders.add(removedElement.getOrder());

			}

		}

		// Forward: Update waiting time and service time begin

		/// Calculate travel time from first unchanged to the first that
		/// needs to be shifted forward.
		double travelTimeTo;

		travelTimeTo = LocationService.calculateHaversineDistanceBetweenCustomers(
				route.get(positionToRemove - 1).getOrder().getOrderRequest().getCustomer(),
				route.get(positionToRemove).getOrder().getOrderRequest().getCustomer())
				/ this.region.getAverageKmPerHour() * TIME_MULTIPLIER;

		route.get(positionToRemove - 1).setTravelTimeFrom(travelTimeTo);
		route.get(positionToRemove).setTravelTime(travelTimeTo);

		for (int i = positionToRemove; i < route.size(); i++) {
			RouteElement eBefore = route.get(i - 1);
			RouteElement eNew = route.get(i);

			// Update service begin and waiting time

			double arrivalTime;
			// If it is the first element, the service can begin at the
			// start of the time window
			if (i == 1 && this.includeDriveFromStartingPosition != 1) {
				arrivalTime = (vaa.getStartTime() - this.timeWindowSet.getTempStartOfDeliveryPeriod())
						* TIME_MULTIPLIER;
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
				newServiceBegin = eNew.getServiceBegin();
			}

			eNew.setServiceBegin(newServiceBegin);
			eNew.setWaitingTime(newServiceBegin - arrivalTime);
			if (i == route.size() - 1) {
				eNew.setWaitingTime(Math.max(0, newServiceBegin - arrivalTime));
			}

			// If the service begin does not change, the following elements
			// can stay as before
			if (oldServiceBegin == newServiceBegin)
				break;
		}

		/// Calculate travel time from last to depot.

		travelTimeTo = LocationService.calculateHaversineDistanceBetweenCustomers(
				route.get(route.size() - 2).getOrder().getOrderRequest().getCustomer(),
				route.get(route.size() - 1).getOrder().getOrderRequest().getCustomer())
				/ this.region.getAverageKmPerHour() * TIME_MULTIPLIER;

		route.get(route.size() - 2).setTravelTimeFrom(travelTimeTo);
		route.get(route.size() - 1).setTravelTime(travelTimeTo);

		// Backward: Update maximum shift (slack)

		/// For last before end depot (if it is not the begin depot)
		if (route.size() > 2) {
			double lastEndTime = (vaa.getEndTime() - this.timeWindowSet.getTempStartOfDeliveryPeriod())
					* TIME_MULTIPLIER;
			if (this.includeDriveFromStartingPosition == 1) {
				lastEndTime -= route.get(route.size() - 2).getTravelTimeFrom();
			}

			Double maxShiftEnd = Math.min(lastEndTime - route.get(route.size() - 2).getServiceBegin(),
					(route.get(route.size() - 2).getOrder().getTimeWindowFinal().getEndTime()
							- this.timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER
							- route.get(route.size() - 2).getServiceBegin());

			if (this.includeDriveFromStartingPosition == 1) {
				maxShiftEnd = Math.min(
						lastEndTime - route.get(route.size() - 2).getServiceBegin() - expectedServiceTime,
						(route.get(route.size() - 2).getOrder().getTimeWindowFinal().getEndTime()
								- this.timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER
								- route.get(route.size() - 2).getServiceBegin());
			}

			route.get(route.size() - 2).setSlack(maxShiftEnd);
		}

		/// For others
		for (int i = route.size() - 3; i > 0; i--) {
			Double maxShift = Math.min(
					(route.get(i).getOrder().getTimeWindowFinal().getEndTime()
							- this.timeWindowSet.getTempStartOfDeliveryPeriod()) * TIME_MULTIPLIER
							- route.get(i).getServiceBegin(),
					route.get(i + 1).getWaitingTime() + route.get(i + 1).getSlack());
			route.get(i).setSlack(maxShift);
		}

	}

	public static String[] getParameterSetting() {

		return paras;
	}

	public Routing getResult() {
		// TODO Auto-generated method stub
		return finalRouting;
	}

	/**
	 * Evaluates a routing based on profit
	 * 
	 * @param solution
	 *            Routing
	 * @return Overall revenue
	 */

	private Double evaluateSolution(HashMap<Integer, HashMap<Integer, ArrayList<RouteElement>>> solution) {

		Double value = 0.0;
		for (Integer daId : solution.keySet()) {
			for (Integer routeId : solution.get(daId).keySet()) {
				ArrayList<RouteElement> elements = solution.get(daId).get(routeId);
				for (int elementId = 1; elementId < elements.size() - 1; elementId++) {
					if (!this.onlyCostBased) {
						value += elements.get(elementId).getOrder().getOrderRequest().getBasketValue();
					} else {
						if ((this.includeDriveFromStartingPosition == 0.0) && elementId == 1) {
							value -= elements.get(elementId).getServiceTime();
						} else {
							value -= (elements.get(elementId).getTravelTimeTo()
									+ elements.get(elementId).getServiceTime());
						}
						if ((this.includeDriveFromStartingPosition == 1.0) && elementId == elements.size() - 2) {
							value -= elements.get(elementId).getTravelTimeFrom();
						}
					}
				}
			}
		}
		return value;
	}

	private double getShiftValue(RouteElement insertionOption) {
		double insertionValue = insertionOption.getOrder().getOrderRequest().getBasketValue();

		if (this.squaredValue) {
			insertionValue = Math.sqrt(insertionValue);
		}
		return insertionValue / insertionOption.getTempShift();
	}

}
