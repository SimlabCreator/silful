package logic.algorithm.vr.routingBasedAcceptance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import data.entity.Alternative;
import data.entity.AlternativeOffer;
import data.entity.ConsiderationSetAlternative;
import data.entity.Customer;
import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
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
import logic.entity.AcceptedCustomersOnRouteAfter;
import logic.entity.AcceptedCustomersOnRouteBefore;
import logic.entity.InsertionCosts;
import logic.service.support.CustomerDemandService;
import logic.service.support.LocationService;
import logic.service.support.ProbabilityDistributionService;
import logic.service.support.RoutingService;
import logic.utility.comparator.OrderRequestArrivalTimeDescComparator;
import logic.utility.comparator.TimeWindowStartAscComparator;
import logic.utility.exceptions.ParameterUnknownException;

/**
 * Accepts order requests if feasible in routing
 *
 */
public class RoutingBasedAcceptance implements RoutingAlgorithm {

	boolean test = false; // TODO: DELETE
	private static double TIME_MULTIPLIER=60.0;
	private OrderSet orderSet;
	private Routing routing;
	private OrderRequestSet orderRequestSet;
	private TimeWindowSet timeWindowSet;
	private ArrayList<NodeDistance> distances;
	private ArrayList<Vehicle> vehicles;
	private Depot depot;
	private ArrayList<Node> nodes;
	private DeliveryAreaSet deliveryAreaSet;
	private boolean timeWindowCount3;
	double[][] timeDependentWeighting = { { 0, 0.95, 0.915, 0.87, 0.715, 0.84, 1.075 },
			{ 0, 0.79, 0.795, 0.725, 0.615, 0.775, 1.1 }, { 0, 0.725, 0.795, 0.79, 0.725, 0.915, 1.41 },
			{ 0, 0.79, 0.795, 0.725, 0.615, 0.775, 1.1 } };
	private DeliveryArea centerArea;
	// check if customer was inserted in route
	boolean insertionSuccessful = false;
	private HashMap<Long, HashMap<Long, Double>> distancesNodes;
	private boolean directDistances;
	private boolean timeDependentTravelTimes;
	private boolean usepreferencesSampled;
	private Region region;

	private static String[] paras = new String[] { "time_dependent_travel_times", "directDistances", "samplePreferences"};

	public RoutingBasedAcceptance(OrderRequestSet orderRequestSet, TimeWindowSet timeWindowSet, Depot depot,
			ArrayList<Vehicle> vehicles, ArrayList<NodeDistance> distances, ArrayList<Node> nodes,
			DeliveryAreaSet deliveryAreaSet, boolean runtimeComparison, double directDistances,
			double timeDependentTravelTimes, double samplePreferences, Region region) {
		this.orderRequestSet = orderRequestSet;
		this.timeWindowSet = timeWindowSet;
		this.distances = distances;
		this.vehicles = vehicles;
		this.depot = depot;
		this.nodes = nodes;
		this.region=region;
		this.deliveryAreaSet = deliveryAreaSet;
		if(directDistances==1.0){
			this.directDistances=true;
		}else{
			this.directDistances=false;
		}
		if(timeDependentTravelTimes==1.0){
			this.timeDependentTravelTimes = true;
		}else{
			this.timeDependentTravelTimes=false;
		}
		
		if(samplePreferences==1.0){
			this.usepreferencesSampled = true;
		}else{
			this.usepreferencesSampled=false;
		}
		
		

	}

	public void start() {
		ArrayList<ArrayList<ArrayList<RouteElement>>> vehicleRoutes = new ArrayList<ArrayList<ArrayList<RouteElement>>>();

		// Prepare list of time windows and sort by start
		ArrayList<TimeWindow> timeWindows = timeWindowSet.getElements();

		if (timeWindows.size() == 3) {// TODO: delete later, only hard - coded
										// if 3 time windows for travel times!!!
			timeWindowCount3 = true;
		} else {
			timeWindowCount3 = false;
		}
		Collections.sort(timeWindows, new TimeWindowStartAscComparator());

		// Sort order requests (descending arrival time)
		ArrayList<OrderRequest> orderRequests = this.orderRequestSet.getElements();
		Collections.sort(orderRequests, new OrderRequestArrivalTimeDescComparator());

		// Closest node to depot (needed for dummy depot-elements)
		Node depotNode = null;
		if (!this.directDistances)
			depotNode=LocationService.findClosestNode(nodes, depot.getLat(), depot.getLon());
		this.centerArea = LocationService.assignLocationToDeliveryArea(deliveryAreaSet, depot.getLat(), depot.getLon());
		
		// Calculate distance matrix between nodes
		if (!this.directDistances)
			distancesNodes = LocationService.getDistanceMatrixBetweenNodes(distances);

		// Create as many routes as vehicles
		for (int vehicleTypeNo = 0; vehicleTypeNo < vehicles.size(); vehicleTypeNo++) {
			for (int vehicleNo = 0; vehicleNo < vehicles.get(vehicleTypeNo).getVehicleNo(); vehicleNo++) {
				ArrayList<ArrayList<RouteElement>> vehicleRoute = new ArrayList<ArrayList<RouteElement>>();

				// two extra timeWindows for Depot at Begin and end
				for (int twID = 0; twID < (timeWindows.size() + 2); twID++) {

					ArrayList<RouteElement> elementsList = new ArrayList<RouteElement>();

					// If it is the first or last time window, the route is
					// initialized with a dummy element representing the depot
					if (twID == 0) {

						RouteElement elementBeginning = new RouteElement();
						elementBeginning.setPosition(0);
						elementBeginning.setId(111111);
						elementBeginning.setTimeWindowId(11);

						elementBeginning.setServiceBegin(550.0);
						// elementBeginning.setServiceBegin(timeWindows.get(twID).getStartTime()*TIME_MULTIPLIER);
						elementBeginning.setServiceTime(0.0);
						elementBeginning.setSlack(0.0);
						elementBeginning.setWaitingTime(0.0);
						elementBeginning.setTravelTime(0.0);
						Order order = new Order();
						order.setId(-1);
						OrderRequest request = new OrderRequest();
						order.setOrderRequest(request);
						Customer customer = new Customer();
						if(!this.directDistances){
							customer.setClosestNode(depotNode);
							customer.setClosestNodeId(depotNode.getLongId());
						}else{
							customer.setLat(depot.getLat());
							customer.setLon(depot.getLon());
						}
						request.setCustomer(customer);
						elementBeginning.setOrder(order);
						elementsList.add(elementBeginning);
					}

					// if (twID == timeWindows.size() - 1) {
					if (twID == timeWindows.size() + 1) {
						RouteElement elementBeginning = new RouteElement();
						elementBeginning.setPosition(0);
						elementBeginning.setId(999999);
						elementBeginning.setTimeWindowId(10);
						// begin of depot set early to lever out time window
						// constraint for depot
						elementBeginning.setServiceBegin(1400.0);

						elementBeginning.setServiceTime(0.0);
						elementBeginning.setSlack(10000.0);
						elementBeginning.setWaitingTime(0.0);
						elementBeginning.setTravelTime(0.0);
						Order order = new Order();
						order.setId(-2);
						OrderRequest request = new OrderRequest();
						order.setOrderRequest(request);
						Customer customer = new Customer();
						if(!this.directDistances){
							customer.setClosestNode(depotNode);
							customer.setClosestNodeId(depotNode.getLongId());
						}else{
							customer.setLat(depot.getLat());
							customer.setLon(depot.getLon());
						}
						request.setCustomer(customer);
						elementBeginning.setOrder(order);
						elementsList.add(elementBeginning);
					}

					vehicleRoute.add(elementsList);

				}
				vehicleRoutes.add(vehicleRoute);
			}
		}

		// Prepare order set
		orderSet = new OrderSet();
		orderSet.setOrderRequestSetId(this.orderRequestSet.getId());
		orderSet.setOrderRequestSet(this.orderRequestSet);

		// Go through all requests and decide for offer set based on routing
		ArrayList<Order> orders = new ArrayList<Order>();
		for (OrderRequest request : orderRequests) {

			Order order = new Order();
			order.setOrderRequest(request);
			order.setOrderRequestId(request.getId());

			if (!this.directDistances) {
				Node closestNode = LocationService.findClosestNode(nodes, request.getCustomer().getLat(),
						request.getCustomer().getLon());
				order.getOrderRequest().getCustomer().setClosestNode(closestNode);
				order.getOrderRequest().getCustomer().setClosestNodeId(closestNode.getLongId());
			}

			// Go through consideration set of customer and check for each
			// alternative, if feasible
			ArrayList<ConsiderationSetAlternative> considerationSetAlternatives = request.getCustomer()
					.getOriginalDemandSegment().getConsiderationSet();
			ArrayList<AlternativeOffer> alternativeOffers = new ArrayList<AlternativeOffer>();
			for (int i = 0; i < considerationSetAlternatives.size(); i++) {

				/// Not no purchase alternative?
				ConsiderationSetAlternative cAlt = considerationSetAlternatives.get(i);
				if (!cAlt.getAlternative().getNoPurchaseAlternative()) {
					// Feasible to offer? (Do not add to route, only check)
					int preferredTimeWindowId = 0;

					for (int twID = 0; twID < timeWindows.size(); twID++) {
						if ((cAlt.getAlternative().getTimeWindows().get(0)).getId() == timeWindows.get(twID).getId()) {
							preferredTimeWindowId = preferredTimeWindowId + twID;
							break;
						}
					}

					// preferredTimeWindowId starts at list 1, list 0 is
					// reserved for depot
					preferredTimeWindowId = preferredTimeWindowId + 1;

					ArrayList<AcceptedCustomersOnRouteBefore> before = RoutingService
							.findLastCustomerBeforePreferredTimeWindow(vehicleRoutes, preferredTimeWindowId);
					ArrayList<AcceptedCustomersOnRouteAfter> after = RoutingService
							.findFirstCustomerAfterPreferredTimeWindow(vehicleRoutes, preferredTimeWindowId);

					// Determine insertion costs
					ArrayList<InsertionCosts> insertionCosts;
					try {
						insertionCosts = this.computeAndSortInsertionCosts(preferredTimeWindowId, vehicleRoutes,
								timeWindows, order, before, after, this.distances);

						// Insert into routing
						// Double cheapestInsertionCosts =
						this.tryCheapestInsertionPrecalculate(vehicleRoutes, preferredTimeWindowId, timeWindows,
								insertionCosts, order, before, after);

					} catch (ParameterUnknownException e) {
						e.printStackTrace();
						System.exit(0);
					}

					if (insertionSuccessful == true) {// It is feasible, add
														// to offer set
						// If yes, add to offered alternatives
						AlternativeOffer offer = new AlternativeOffer();
						offer.setAlternative(cAlt.getAlternative());
						offer.setAlternativeId(cAlt.getAlternativeId());
						alternativeOffers.add(offer);

					}
					this.insertionSuccessful = false;

				}
			}

			// All feasible alternatives are offered
			ArrayList<Alternative> availableAlt = new ArrayList<Alternative>();
			for (int aAltID = 0; aAltID < alternativeOffers.size(); aAltID++) {
				availableAlt.add(alternativeOffers.get(aAltID).getAlternative());
			}
			order.setAvailableAlternatives(availableAlt);
			order.setOfferedAlternatives(alternativeOffers);
			if (alternativeOffers.size() > 0) {
				AlternativeOffer offerSelected;
				if(this.usepreferencesSampled){
					offerSelected = CustomerDemandService.selectCustomerDemandBasedOnSampledPreferences(alternativeOffers, order,this.orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getAlternativeSet().getNoPurchaseAlternative());
				}else{
					offerSelected = CustomerDemandService.sampleCustomerDemand(alternativeOffers, order);
				}
				if (offerSelected == null) {
					order.setAccepted(false);
					order.setReasonRejection("Customer chose no-purchase option");
				} else {
					order.setAccepted(true);
					// Add order to route

					// Do insertion as before
					TimeWindow preferredTimeWindow = order.getSelectedAlternative().getTimeWindows().get(0);
					int preferredTimeWindowId = 0;
					for (int twID = 0; twID < timeWindows.size(); twID++) {
						if (preferredTimeWindow.getId() == timeWindows.get(twID).getId()) {
							preferredTimeWindowId = preferredTimeWindowId + twID;
							break;
						}
					}

					// preferredTimeWindowId starts at list 1, list 0 is
					// reserved
					// for depot
					preferredTimeWindowId = preferredTimeWindowId + 1;

					ArrayList<AcceptedCustomersOnRouteBefore> before = RoutingService
							.findLastCustomerBeforePreferredTimeWindow(vehicleRoutes, preferredTimeWindowId);
					ArrayList<AcceptedCustomersOnRouteAfter> after = RoutingService
							.findFirstCustomerAfterPreferredTimeWindow(vehicleRoutes, preferredTimeWindowId);

					// Determine insertion costs
					ArrayList<InsertionCosts> insertionCosts;
					try {
						insertionCosts = this.computeAndSortInsertionCosts(preferredTimeWindowId, vehicleRoutes,
								timeWindows, order, before, after, this.distances);

						// Insert into routing - really
						if (test == true)
							System.out.println("Insertion costs:" + insertionCosts.get(0).getInsertionCosts());
						this.tryCheapestInsertion(vehicleRoutes, preferredTimeWindowId, timeWindows, insertionCosts,
								order, before, after);

					} catch (ParameterUnknownException e) {
						e.printStackTrace();
						System.exit(0);
					}
					

					order.setTimeWindowFinalId(timeWindows.get(preferredTimeWindowId-1).getId()); 
					
					// Reset boolean for next request
					this.insertionSuccessful = false;
				}
			} else {
				order.setAccepted(false);
				order.setReasonRejection("No time window available");
			}
			
			orders.add(order);
		}

		orderSet.setElements(orders);
		routing= new Routing();
		ArrayList<Route> finalRoutes = new ArrayList<Route>();
		for(ArrayList<ArrayList<RouteElement>> route: vehicleRoutes){
			
			Route r = new Route();
			//Go through time windows (first and last are only dummies)
			ArrayList<RouteElement> elements = new ArrayList<RouteElement>();
			for(int twId=1; twId < route.size()-1; twId++){
				elements.addAll(route.get(twId));
			}
			r.setRouteElements(elements);
			finalRoutes.add(r);
		}
	routing.setRoutes(finalRoutes);
	routing.setOrderSet(orderSet);
	routing.setDepotId(depot.getId());
	routing.setPossiblyFinalRouting(false);
	routing.setTimeWindowSetId(timeWindowSet.getId());

	}

	private ArrayList<InsertionCosts> computeAndSortInsertionCosts(int preferredTimeWindow,
			ArrayList<ArrayList<ArrayList<RouteElement>>> routes, ArrayList<TimeWindow> timeWindows, Order order,
			ArrayList<AcceptedCustomersOnRouteBefore> before, ArrayList<AcceptedCustomersOnRouteAfter> after,
			ArrayList<NodeDistance> distances) throws ParameterUnknownException {

		ArrayList<InsertionCosts> insertionCosts = new ArrayList<InsertionCosts>();

		// System.out.println("ForecastedOrderRequest Id: " +
		// forecastedOrderRequest.getId());

		// compute for each route
		for (int r = 0; r < routes.size(); r++) {

			// insertion costs: no customer in preferred time window, costs for
			// insertion between customer before and after time window needed
			if (routes.get(r).get(preferredTimeWindow).isEmpty()) {
				computeInsertionCostsEmpty(r, routes, preferredTimeWindow, order, distances, insertionCosts, before,
						after);

				// compute insertion costs for each possible insertion position
			} else {
				for (int c = 0; c < routes.get(r).get(preferredTimeWindow).size(); c++) {

					// insertion costs: customer is inserted at begin (first
					// customer) of preferred time window
					if (routes.get(r).get(preferredTimeWindow).get(c).getId() == routes.get(r).get(preferredTimeWindow)
							.get(0).getId()) {
						computeInsertionCostsInsertionBegin(r, routes, preferredTimeWindow, order, distances,
								insertionCosts, before, after);
					}

					// insertion costs: customer is inserted at end (last
					// customer) of preferred time window
					if (routes.get(r).get(preferredTimeWindow).get(c).getId() == routes.get(r).get(preferredTimeWindow)
							.get(routes.get(r).get(preferredTimeWindow).size() - 1).getId()) {
						computeInsertionCostsInsertionEnd(r, routes, preferredTimeWindow, order, distances,
								insertionCosts, before, after);

						// insertion costs: customer is inserted within (between
						// customers) of preferred time window
					} else {
						computeInsertionCostsInsertionWithin(r, routes, preferredTimeWindow, order, distances,
								insertionCosts, c, before, after);
					}
				}
			}
		}
		return sortInsertionCosts(routes, preferredTimeWindow, timeWindows, insertionCosts, order, before, after);
	}

	private void computeInsertionCostsEmpty(int r, ArrayList<ArrayList<ArrayList<RouteElement>>> routes,
			int preferredTimeWindow, Order order, ArrayList<NodeDistance> distances,
			ArrayList<InsertionCosts> insertionCosts, ArrayList<AcceptedCustomersOnRouteBefore> before,
			ArrayList<AcceptedCustomersOnRouteAfter> after) throws ParameterUnknownException {

		double insertionCosts_i;
		InsertionCosts insertion = new InsertionCosts();

		double distanceIx; // distance customer before (i) and order request (x)
		double distanceXj; // distance order request (x) and customer after (j)
		double distanceIj; // distance customer before (i) and customer after
							// (j)
		int directionIx; // travel direction customer before (i) and order
							// request (x)
		int directionXj; // travel direction order request (x) and customer
							// after (j)
		int directionIj; // travel direction order request (i) and customer
							// after (j)
		if(!this.directDistances){
		distanceIx = distancesNodes
				.get(routes.get(r).get(preferredTimeWindow - before.get(r).getBefore())
						.get(routes.get(r).get(preferredTimeWindow - before.get(r).getBefore()).size() - 1).getOrder()
						.getOrderRequest().getCustomer().getClosestNodeId())
				.get(order.getOrderRequest().getCustomer().getClosestNodeId());

		distanceXj = distancesNodes.get(order.getOrderRequest().getCustomer().getClosestNodeId())
				.get(routes.get(r).get(preferredTimeWindow + after.get(r).getAfter()).get(0).getOrder()
						.getOrderRequest().getCustomer().getClosestNodeId());

		distanceIj = distancesNodes
				.get(routes.get(r).get(preferredTimeWindow - before.get(r).getBefore())
						.get(routes.get(r).get(preferredTimeWindow - before.get(r).getBefore()).size() - 1).getOrder()
						.getOrderRequest().getCustomer().getClosestNodeId())
				.get(routes.get(r).get(preferredTimeWindow + after.get(r).getAfter()).get(0).getOrder()
						.getOrderRequest().getCustomer().getClosestNodeId());
		}else{
			distanceIx = LocationService.calculateHaversineDistanceBetweenCustomers(routes.get(r).get(preferredTimeWindow - before.get(r).getBefore())
					.get(routes.get(r).get(preferredTimeWindow - before.get(r).getBefore()).size() - 1).getOrder()
					.getOrderRequest().getCustomer(), order.getOrderRequest().getCustomer())/ region.getAverageKmPerHour() * TIME_MULTIPLIER;
			distanceXj =LocationService.calculateHaversineDistanceBetweenCustomers(order.getOrderRequest().getCustomer(), routes.get(r).get(preferredTimeWindow + after.get(r).getAfter()).get(0).getOrder()
					.getOrderRequest().getCustomer())/ region.getAverageKmPerHour() * TIME_MULTIPLIER;
			distanceIj = LocationService.calculateHaversineDistanceBetweenCustomers(routes.get(r).get(preferredTimeWindow - before.get(r).getBefore())
					.get(routes.get(r).get(preferredTimeWindow - before.get(r).getBefore()).size() - 1).getOrder()
					.getOrderRequest().getCustomer(), routes.get(r).get(preferredTimeWindow + after.get(r).getAfter()).get(0).getOrder()
					.getOrderRequest().getCustomer())/ region.getAverageKmPerHour() * TIME_MULTIPLIER;
		}
		
	
		// adjust travel time with time dependent weighting
		int relevantWindow = preferredTimeWindow;
		if (this.timeDependentTravelTimes) {
			directionIx = LocationService.getDirection(
					routes.get(r).get(preferredTimeWindow - before.get(r).getBefore())
							.get(routes.get(r).get(preferredTimeWindow - before.get(r).getBefore()).size() - 1).getOrder()
							.getOrderRequest().getCustomer(),
					order.getOrderRequest().getCustomer(), deliveryAreaSet, centerArea, this.directDistances);

			directionXj = LocationService.getDirection(order.getOrderRequest().getCustomer(),
					routes.get(r).get(preferredTimeWindow + after.get(r).getAfter()).get(0).getOrder().getOrderRequest()
							.getCustomer(),
					deliveryAreaSet, centerArea, this.directDistances);
			directionIj = LocationService.getDirection(
					routes.get(r).get(preferredTimeWindow - before.get(r).getBefore())
							.get(routes.get(r).get(preferredTimeWindow - before.get(r).getBefore()).size() - 1).getOrder()
							.getOrderRequest().getCustomer(),
					routes.get(r).get(preferredTimeWindow + after.get(r).getAfter()).get(0).getOrder().getOrderRequest()
							.getCustomer(),
					deliveryAreaSet, centerArea, this.directDistances);

			if (timeWindowCount3) { // TODO: adjust!! Only now hard-coded for
									// travel
									// times with 3 time windows
				relevantWindow++;
			}
			distanceIx = distanceIx / timeDependentWeighting[directionIx][relevantWindow];
			distanceXj = distanceXj / timeDependentWeighting[directionXj][relevantWindow];
			distanceIj = distanceIj / timeDependentWeighting[directionIj][relevantWindow];
		}
		
		// insertion Costs: new distance - old distance + serviceTime
		insertionCosts_i = distanceIx + distanceXj - distanceIj
				+ ProbabilityDistributionService.getMeanByProbabilityDistribution(
						order.getOrderRequest().getCustomer().getServiceTimeSegment().getProbabilityDistribution());

		insertion.setDistanceIx(distanceIx);
		insertion.setDistanceXj(distanceXj);
		insertion.setDistanceIj(distanceIj);
		insertion.setInsertionCosts(insertionCosts_i);

		insertion.setInsertAfterId(routes.get(r).get(preferredTimeWindow - before.get(r).getBefore())
				.get(routes.get(r).get(preferredTimeWindow - before.get(r).getBefore()).size() - 1).getId());

		// might causes changes starting at first element (0) in next time
		// window
		insertion.setCheckFromHere(0);

		insertion.setRoute(r);

		insertionCosts.add(insertion);
	}

	private void computeInsertionCostsInsertionBegin(int r, ArrayList<ArrayList<ArrayList<RouteElement>>> routes,
			int preferredTimeWindow, Order order, ArrayList<NodeDistance> distances,
			ArrayList<InsertionCosts> insertionCosts, ArrayList<AcceptedCustomersOnRouteBefore> before,
			ArrayList<AcceptedCustomersOnRouteAfter> after) throws ParameterUnknownException {
		double insertionCosts_i;
		InsertionCosts insertion = new InsertionCosts();

		double distanceIx; // distance customer before (i) and order request (x)
		double distanceXj; // distance order request (x) and customer after (j)
		double distanceIj; // distance customer before (i) and customer after
							// (j)

		int directionIx; // travel direction customer before (i) and order
							// request (x)
		int directionXj; // travel direction order request (x) and customer
							// after (j)
		int directionIj; // travel direction order request (i) and customer
							// after (j)

		if(!this.directDistances){
		distanceIx = distancesNodes
				.get(routes.get(r).get(preferredTimeWindow - before.get(r).getBefore())
						.get(routes.get(r).get(preferredTimeWindow - before.get(r).getBefore()).size() - 1).getOrder()
						.getOrderRequest().getCustomer().getClosestNodeId())
				.get(order.getOrderRequest().getCustomer().getClosestNodeId());

		distanceXj = distancesNodes.get(order.getOrderRequest().getCustomer().getClosestNodeId()).get(routes.get(r)
				.get(preferredTimeWindow).get(0).getOrder().getOrderRequest().getCustomer().getClosestNodeId());

		distanceIj = distancesNodes
				.get(routes.get(r).get(preferredTimeWindow - before.get(r).getBefore())
						.get(routes.get(r).get(preferredTimeWindow - before.get(r).getBefore()).size() - 1).getOrder()
						.getOrderRequest().getCustomer().getClosestNodeId())
				.get(routes.get(r).get(preferredTimeWindow).get(0).getOrder().getOrderRequest().getCustomer()
						.getClosestNodeId());
		}else{
			
			distanceIx =LocationService.calculateHaversineDistanceBetweenCustomers(routes.get(r).get(preferredTimeWindow - before.get(r).getBefore())
					.get(routes.get(r).get(preferredTimeWindow - before.get(r).getBefore()).size() - 1).getOrder()
					.getOrderRequest().getCustomer(), order.getOrderRequest().getCustomer())/ region.getAverageKmPerHour() * TIME_MULTIPLIER;
			distanceXj =LocationService.calculateHaversineDistanceBetweenCustomers(order.getOrderRequest().getCustomer(), routes.get(r)
					.get(preferredTimeWindow).get(0).getOrder().getOrderRequest().getCustomer())/ region.getAverageKmPerHour() * TIME_MULTIPLIER;
			distanceIj =LocationService.calculateHaversineDistanceBetweenCustomers(routes.get(r).get(preferredTimeWindow - before.get(r).getBefore())
					.get(routes.get(r).get(preferredTimeWindow - before.get(r).getBefore()).size() - 1).getOrder()
					.getOrderRequest().getCustomer(), routes.get(r).get(preferredTimeWindow).get(0).getOrder().getOrderRequest().getCustomer())/ region.getAverageKmPerHour() * TIME_MULTIPLIER;
		}
		
		

		// adjust travel time with time dependent weighting
		int relevantWindow = preferredTimeWindow;
		if (this.timeDependentTravelTimes) {
			directionIx = LocationService.getDirection(
					routes.get(r).get(preferredTimeWindow - before.get(r).getBefore())
							.get(routes.get(r).get(preferredTimeWindow - before.get(r).getBefore()).size() - 1).getOrder()
							.getOrderRequest().getCustomer(),
					order.getOrderRequest().getCustomer(), deliveryAreaSet, centerArea, this.directDistances);

			directionXj = LocationService
					.getDirection(
							order.getOrderRequest().getCustomer(), routes.get(r).get(preferredTimeWindow)
									.get(0).getOrder().getOrderRequest().getCustomer(),
							deliveryAreaSet, centerArea, this.directDistances);
			directionIj = LocationService.getDirection(
					routes.get(r).get(preferredTimeWindow - before.get(r).getBefore())
							.get(routes.get(r).get(preferredTimeWindow - before.get(r).getBefore()).size() - 1).getOrder()
							.getOrderRequest().getCustomer(),
					routes.get(r).get(preferredTimeWindow).get(0).getOrder().getOrderRequest().getCustomer(),
					deliveryAreaSet, centerArea, this.directDistances);
			
			if (timeWindowCount3) { // TODO: adjust!! Only now hard-coded for
									// travel
									// times with 3 time windows
				relevantWindow++;
			}
			distanceIx = distanceIx / timeDependentWeighting[directionIx][relevantWindow];
			distanceXj = distanceXj / timeDependentWeighting[directionXj][relevantWindow];
			distanceIj = distanceIj / timeDependentWeighting[directionIj][relevantWindow];
		}
		// insertion Costs: new distance - old distance + serviceTime
		insertionCosts_i = distanceIx + distanceXj - distanceIj
				+ ProbabilityDistributionService.getMeanByProbabilityDistribution(
						order.getOrderRequest().getCustomer().getServiceTimeSegment().getProbabilityDistribution());

		insertion.setDistanceIx(distanceIx);
		insertion.setDistanceXj(distanceXj);
		insertion.setDistanceIj(distanceIj);
		insertion.setInsertionCosts(insertionCosts_i);

		// after which customer order insertion costs were computed
		insertion.setInsertAfterId(routes.get(r).get(preferredTimeWindow - before.get(r).getBefore())
				.get(routes.get(r).get(preferredTimeWindow - before.get(r).getBefore()).size() - 1).getId());

		// causes changes starting at first element (0) in same time window
		insertion.setCheckFromHere(0);
		insertion.setRoute(r);

		insertionCosts.add(insertion);
	}

	private ArrayList<InsertionCosts> sortInsertionCosts(ArrayList<ArrayList<ArrayList<RouteElement>>> routes,
			int preferredTimeWindow, ArrayList<TimeWindow> timeWindows, ArrayList<InsertionCosts> insertionCosts,
			Order order, ArrayList<AcceptedCustomersOnRouteBefore> before,
			ArrayList<AcceptedCustomersOnRouteAfter> after) {

		InsertionCosts i1;
		InsertionCosts i2;

		// sort insertion costs for each insertion position from low to high
		for (int n = insertionCosts.size(); n > 1; n--) {
			for (int i = 0; i < insertionCosts.size() - 1; i++) {
				if (insertionCosts.get(i).getInsertionCosts() > insertionCosts.get(i + 1).getInsertionCosts()) {
					i1 = insertionCosts.get(i);
					i2 = insertionCosts.get(i + 1);
					insertionCosts.set(i, i2);
					insertionCosts.set((i + 1), i1);
				}

			}

		}

		if (test == true) {
			// for (InsertionCosts i : insertionCosts) {
			// //System.out.println("RequestID: " +
			// forecastedOrderRequest.getId() + " Costs: " +
			// i.getInsertionCosts() + " Route: " + i.getRoute() + " ID: " +
			// i.getInsertAfterId());
			// }

		}
		return insertionCosts;

	}

	private void computeInsertionCostsInsertionEnd(int r, ArrayList<ArrayList<ArrayList<RouteElement>>> routes,
			int preferredTimeWindow, Order order, ArrayList<NodeDistance> distances,
			ArrayList<InsertionCosts> insertionCosts, ArrayList<AcceptedCustomersOnRouteBefore> before,
			ArrayList<AcceptedCustomersOnRouteAfter> after) throws ParameterUnknownException {
		double insertionCosts_i;
		InsertionCosts insertion = new InsertionCosts();

		double distanceIx; // distance customer before (i) and order request (x)
		double distanceXj; // distance order request (x) and customer after (j)
		double distanceIj; // distance customer before (i) and customer after
							// (j)
		int directionIx; // travel direction customer before (i) and order
							// request (x)
		int directionXj; // travel direction order request (x) and customer
							// after (j)
		int directionIj; // travel direction order request (i) and customer
							// after (j)

		if(!this.directDistances){
		distanceIx = distancesNodes
				.get(routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size() - 1)
						.getOrder().getOrderRequest().getCustomer().getClosestNodeId())
				.get(order.getOrderRequest().getCustomer().getClosestNodeId());

		distanceXj = distancesNodes.get(order.getOrderRequest().getCustomer().getClosestNodeId())
				.get(routes.get(r).get(preferredTimeWindow + after.get(r).getAfter()).get(0).getOrder()
						.getOrderRequest().getCustomer().getClosestNodeId());

		distanceIj = distancesNodes
				.get(routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size() - 1)
						.getOrder().getOrderRequest().getCustomer().getClosestNodeId())
				.get(routes.get(r).get(preferredTimeWindow + after.get(r).getAfter()).get(0).getOrder()
						.getOrderRequest().getCustomer().getClosestNodeId());
		}else{
			distanceIx =LocationService.calculateHaversineDistanceBetweenCustomers(routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size() - 1)
					.getOrder().getOrderRequest().getCustomer(), order.getOrderRequest().getCustomer())/ region.getAverageKmPerHour() * TIME_MULTIPLIER;
			distanceXj =LocationService.calculateHaversineDistanceBetweenCustomers(order.getOrderRequest().getCustomer(), routes.get(r).get(preferredTimeWindow + after.get(r).getAfter()).get(0).getOrder()
					.getOrderRequest().getCustomer())/ region.getAverageKmPerHour() * TIME_MULTIPLIER;
			distanceIj =LocationService.calculateHaversineDistanceBetweenCustomers(routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size() - 1)
					.getOrder().getOrderRequest().getCustomer(), routes.get(r).get(preferredTimeWindow + after.get(r).getAfter()).get(0).getOrder()
					.getOrderRequest().getCustomer())/ region.getAverageKmPerHour() * TIME_MULTIPLIER;
		}
	

		// adjust travel time with time dependent weighting
		int relevantWindow = preferredTimeWindow;
		if (this.timeDependentTravelTimes) {
			
			directionIx = LocationService.getDirection(
					routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size() - 1).getOrder()
							.getOrderRequest().getCustomer(),
					order.getOrderRequest().getCustomer(), deliveryAreaSet, centerArea, this.directDistances);
			directionXj = LocationService.getDirection(order.getOrderRequest().getCustomer(),
					routes.get(r).get(preferredTimeWindow + after.get(r).getAfter()).get(0).getOrder().getOrderRequest()
							.getCustomer(),
					deliveryAreaSet, centerArea, this.directDistances);
			directionIj = LocationService.getDirection(
					routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size() - 1).getOrder()
							.getOrderRequest().getCustomer(),
					routes.get(r).get(preferredTimeWindow + after.get(r).getAfter()).get(0).getOrder().getOrderRequest()
							.getCustomer(),
					deliveryAreaSet, centerArea, this.directDistances);
			
			if (timeWindowCount3) { // TODO: adjust!! Only now hard-coded for
									// travel
									// times with 3 time windows
				relevantWindow++;
			}
			distanceIx = distanceIx / timeDependentWeighting[directionIx][relevantWindow];
			distanceXj = distanceXj / timeDependentWeighting[directionXj][relevantWindow];
			distanceIj = distanceIj / timeDependentWeighting[directionIj][relevantWindow];
		}
		
		// insertion Costs: new distance - old distance + serviceTime
		insertionCosts_i = distanceIx + distanceXj - distanceIj
				+ ProbabilityDistributionService.getMeanByProbabilityDistribution(
						order.getOrderRequest().getCustomer().getServiceTimeSegment().getProbabilityDistribution());

		insertion.setDistanceIx(distanceIx);
		insertion.setDistanceXj(distanceXj);
		insertion.setDistanceIj(distanceIj);
		insertion.setInsertionCosts(insertionCosts_i);

		// after which customer order insertion costs were computed
		insertion.setInsertAfterId(
				routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size() - 1).getId());

		// might causes changes starting at first element (0) in next time
		// window
		insertion.setCheckFromHere(0);
		insertion.setRoute(r);

		// insertion after depot not possible
		// if (routes.get(r).size() - 1 != preferredTimeWindow &&
		// insertion.getInsertAfterId() !=
		// routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size()
		// - 1).getId()) {
		insertionCosts.add(insertion);
		// }
	}

	private void computeInsertionCostsInsertionWithin(int r, ArrayList<ArrayList<ArrayList<RouteElement>>> routes,
			int preferredTimeWindow, Order order, ArrayList<NodeDistance> distances,
			ArrayList<InsertionCosts> insertionCosts, int c, ArrayList<AcceptedCustomersOnRouteBefore> before,
			ArrayList<AcceptedCustomersOnRouteAfter> after) throws ParameterUnknownException {
		double insertionCosts_i;
		InsertionCosts insertion = new InsertionCosts();

		double distanceIx; // distance customer before (i) and order request (x)
		double distanceXj; // distance order request (x) and customer after (j)
		double distanceIj; // distance customer before (i) and customer after
							// (j)
		int directionIx; // travel direction customer before (i) and order
							// request (x)
		int directionXj; // travel direction order request (x) and customer
							// after (j)
		int directionIj; // travel direction order request (i) and customer
							// after (j)

		if (c < routes.get(r).get(preferredTimeWindow).size() - 1) {

			if(!this.directDistances){
			distanceIx = distancesNodes.get(routes.get(r).get(preferredTimeWindow).get(c).getOrder().getOrderRequest()
					.getCustomer().getClosestNodeId()).get(order.getOrderRequest().getCustomer().getClosestNodeId());

			distanceXj = distancesNodes.get(order.getOrderRequest().getCustomer().getClosestNodeId()).get(routes.get(r)
					.get(preferredTimeWindow).get(c + 1).getOrder().getOrderRequest().getCustomer().getClosestNodeId());

			distanceIj = distancesNodes
					.get(routes.get(r).get(preferredTimeWindow).get(c).getOrder().getOrderRequest().getCustomer()
							.getClosestNodeId())
					.get(routes.get(r).get(preferredTimeWindow).get(c + 1).getOrder().getOrderRequest().getCustomer()
							.getClosestNodeId());
			}else{
				distanceIx =LocationService.calculateHaversineDistanceBetweenCustomers(routes.get(r).get(preferredTimeWindow).get(c).getOrder().getOrderRequest()
						.getCustomer(), order.getOrderRequest().getCustomer())/ region.getAverageKmPerHour() * TIME_MULTIPLIER;
				distanceXj =LocationService.calculateHaversineDistanceBetweenCustomers(order.getOrderRequest().getCustomer(),routes.get(r)
						.get(preferredTimeWindow).get(c + 1).getOrder().getOrderRequest().getCustomer())/ region.getAverageKmPerHour() * TIME_MULTIPLIER;
				distanceIj =LocationService.calculateHaversineDistanceBetweenCustomers(routes.get(r).get(preferredTimeWindow).get(c).getOrder().getOrderRequest().getCustomer(), routes.get(r).get(preferredTimeWindow).get(c + 1).getOrder().getOrderRequest().getCustomer())/ region.getAverageKmPerHour() * TIME_MULTIPLIER;
			}
		

			// adjust travel time with time dependent weighting
			int relevantWindow = preferredTimeWindow;
			if (this.timeDependentTravelTimes) {
				
				directionIx = LocationService.getDirection(
						routes.get(r).get(preferredTimeWindow).get(c).getOrder().getOrderRequest().getCustomer(),
						order.getOrderRequest().getCustomer(), deliveryAreaSet, centerArea, this.directDistances);
				directionXj = LocationService.getDirection(
						order.getOrderRequest().getCustomer(), routes.get(r).get(preferredTimeWindow)
								.get(c + 1).getOrder().getOrderRequest().getCustomer(),
						deliveryAreaSet, centerArea, this.directDistances);
				directionIj = LocationService.getDirection(
						routes.get(r).get(preferredTimeWindow).get(c).getOrder().getOrderRequest().getCustomer(),
						routes.get(r).get(preferredTimeWindow).get(c + 1).getOrder().getOrderRequest().getCustomer(),
						deliveryAreaSet, centerArea, this.directDistances);
				
				if (timeWindowCount3) { // TODO: adjust!! Only now hard-coded
										// for
										// travel times with 3 time windows
					relevantWindow++;
				}
				distanceIx = distanceIx / timeDependentWeighting[directionIx][relevantWindow];
				distanceXj = distanceXj / timeDependentWeighting[directionXj][relevantWindow];
				distanceIj = distanceIj / timeDependentWeighting[directionIj][relevantWindow];
			}

			// insertion Costs: new distance - old distance + serviceTime
			insertionCosts_i = distanceIx + distanceXj - distanceIj
					+ ProbabilityDistributionService.getMeanByProbabilityDistribution(
							order.getOrderRequest().getCustomer().getServiceTimeSegment().getProbabilityDistribution());
			insertion.setDistanceIx(distanceIx);
			insertion.setDistanceXj(distanceXj);
			insertion.setDistanceIj(distanceIj);
			insertion.setInsertionCosts(insertionCosts_i);

			// after which customer order insertion costs were computed
			insertion.setInsertAfterId(routes.get(r).get(preferredTimeWindow).get(c).getId());

			// causes changes starting at route element (c+1) in same time
			// window
			insertion.setCheckFromHere(c + 1);

			insertion.setRoute(r);

			insertionCosts.add(insertion);
		}
	}

	/**
	 * CheapestInsertion Costs for Precalculation
	 * 
	 * @param routes
	 * @param preferredTimeWindow
	 * @param timeWindow
	 * @param insertionCosts
	 * @param forecastedOrderRequest
	 * @param before
	 * @param after
	 * @throws ParameterUnknownException
	 */
	private Double tryCheapestInsertionPrecalculate(ArrayList<ArrayList<ArrayList<RouteElement>>> routes,
			int preferredTimeWindow, ArrayList<TimeWindow> timeWindow, ArrayList<InsertionCosts> insertionCosts,
			Order order, ArrayList<AcceptedCustomersOnRouteBefore> before,
			ArrayList<AcceptedCustomersOnRouteAfter> after) throws ParameterUnknownException {

		// while order request could not be inserted
		InsertionCosts currentlyBest = null;

		while (insertionSuccessful == false) {

			if (insertionCosts.size() > 0) {
				currentlyBest = insertionCosts.get(0);
				// tryInsertion for first element (0) of insertion costs (=
				// cheapest insertion costs)
				tryInsertionPrecalculate(insertionCosts.get(0).getRoute(), routes, preferredTimeWindow,
						insertionCosts.get(0).getInsertAfterId(), timeWindow, insertionCosts, order,
						before.get(insertionCosts.get(0).getRoute()).getBefore(),
						after.get(insertionCosts.get(0).getRoute()).getAfter());

				// remove cheapest insertion costs, if insertion was not
				// successful insertion is tried for next cheapest costs
				insertionCosts.remove(0);
			} else {
				if (test == true) {

					System.out.println("Insertion for forecasted order request id " + order.getOrderRequestId()
							+ " and time window " + preferredTimeWindow + " not possible. ");

					// System.out.println("Insertion for forecasted order
					// request id " + forecastedOrderRequest.getId()
					// + " not possible. ");
				}

				return null;
			}
		}

		return currentlyBest.getInsertionCosts();
	}

	/**
	 * Only sets boolean. Does not really insert successfully. Way to determine
	 * the respective insertion costs that are feasible
	 * 
	 * @param r
	 * @param routes
	 * @param preferredTimeWindow
	 * @param insertionPosition
	 * @param timeWindow
	 * @param insertionCosts
	 * @param forecastedOrderRequest
	 * @param before
	 * @param after
	 * @throws ParameterUnknownException
	 */
	private void tryInsertionPrecalculate(int r, ArrayList<ArrayList<ArrayList<RouteElement>>> routes,
			int preferredTimeWindow, int insertionPosition, ArrayList<TimeWindow> timeWindow,
			ArrayList<InsertionCosts> insertionCosts, Order order, int before, int after)
			throws ParameterUnknownException {

		int checkFromHere;
		checkFromHere = insertionCosts.get(0).getCheckFromHere();

		if (checkFromHere == 0) {

			// checkFromHere==0 & preferred time window empty: customer is
			// inserted in empty time window
			if (routes.get(r).get(preferredTimeWindow).isEmpty()) {
				if (insertionSuccessful == true) {
				} else {
					// System.out.println("Precalculation:
					// insertCustomerEndTimeWindowNoChanges");
					insertionSuccessful = true;
				}
			} else { // TODO: Check, added this else. Before: always goes to
						// next if

				if (routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size() - 1)
						.getId() == insertionCosts.get(0).getInsertAfterId()) {
					if (routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size() - 1)
							.getServiceBegin()
							+ routes.get(r).get(preferredTimeWindow)
									.get(routes.get(r).get(preferredTimeWindow).size() - 1).getServiceTime()
							+ insertionCosts.get(0).getDistanceIx() <= timeWindow.get(preferredTimeWindow - 1)
									.getEndTime() * TIME_MULTIPLIER
							&& routes.get(r).get(preferredTimeWindow)
									.get(routes.get(r).get(preferredTimeWindow).size() - 1).getServiceBegin()
									+ routes.get(r).get(preferredTimeWindow)
											.get(routes.get(r).get(preferredTimeWindow).size() - 1).getServiceTime()
									+ insertionCosts.get(0).getDistanceIx()
									+ ProbabilityDistributionService
											.getMeanByProbabilityDistribution(order.getOrderRequest().getCustomer()
													.getServiceTimeSegment().getProbabilityDistribution())
									+ insertionCosts.get(0).getDistanceXj() <= routes.get(r)
											.get(preferredTimeWindow + after).get(checkFromHere).getServiceBegin()) {

						if (insertionSuccessful == true) {
						} else {
							if (insertionSuccessful == false) {
								// insertCustomerEndTimeWindowNoChanges
								// System.out.println("Precalculation:
								// insertCustomerEndTimeWindowNoChanges");
								// customer is inserted at end of time window,
								// no other customers affected
								// no further checks needed
								insertionSuccessful = true;
							}
						}
					} else {
						if (insertionSuccessful == false) {

							// insertCustomerEndTimeWindowChanges
							// System.out.println("Precalculation:
							// insertCustomerEndTimeWindowChanges");
							// checkFromHere==0 & customer is inserted at end of
							// preferred time window, service begin of next
							// customer
							// is affected

							// further checks needed if insertion can be
							// accomplished
							double insertionCosts_Waiting = insertionCosts.get(0).getInsertionCosts()
									- routes.get(r).get(preferredTimeWindow + after).get(0).getWaitingTime();

							int check = 0;
							int check2 = 0;

							for (int j = preferredTimeWindow + after; j < routes.get(r).size(); j++) {
								for (int i = 0; i < routes.get(r).get(j).size(); i++) {
									if (insertionCosts_Waiting > 0) {

										if (routes.get(r).get(preferredTimeWindow)
												.get(routes.get(r).get(preferredTimeWindow).size() - 1)
												.getServiceBegin()
												+ routes.get(r).get(preferredTimeWindow)
														.get(routes.get(r).get(preferredTimeWindow).size() - 1)
														.getServiceTime()
												+ insertionCosts.get(0).getDistanceIx() <= timeWindow
														.get(preferredTimeWindow - 1).getEndTime() * TIME_MULTIPLIER
												&& insertionCosts_Waiting <= routes.get(r).get(j).get(i).getSlack()
														+ routes.get(r).get(j).get(i).getWaitingTime()) {

											check2++;
										} else {
											check++;
											break;
										}
									}

								}
							}

							if (check == 0 && check2 != 0) {
								insertionSuccessful = true; // customer can be
															// inserted
							} else {
								insertionSuccessful = false;
							}
						}
					}
				} else {
					if (insertionSuccessful == false) {

						// insertCustomerBeginTimeWindow
						// System.out.println("Precalculation:
						// insertCustomerBeginTimeWindow");
						// customer is inserted at begin of
						// preferred time window
						// further checks needed
						if (routes.get(r).get(preferredTimeWindow - before)
								.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceBegin()
								+ routes.get(r).get(preferredTimeWindow - before)
										.get(routes.get(r).get(preferredTimeWindow - before).size() - 1)
										.getServiceTime()
								+ insertionCosts.get(0).getDistanceIx() < timeWindow.get(preferredTimeWindow - 1)
										.getStartTime() * TIME_MULTIPLIER) {

							// customer is inserted at begin of time window,
							// start of service same like start of time window
							double insertionCosts_Waiting;

							if (routes.get(r).get(preferredTimeWindow).get(0).getServiceBegin() == routes.get(r)
									.get(preferredTimeWindow).get(0).getTimeWindow().getStartTime() * TIME_MULTIPLIER) {
								insertionCosts_Waiting = insertionCosts.get(0).getDistanceXj()
										+ ProbabilityDistributionService
												.getMeanByProbabilityDistribution(order.getOrderRequest().getCustomer()
														.getServiceTimeSegment().getProbabilityDistribution());

							} else {
								insertionCosts_Waiting = insertionCosts.get(0).getDistanceXj()
										+ ProbabilityDistributionService
												.getMeanByProbabilityDistribution(order.getOrderRequest().getCustomer()
														.getServiceTimeSegment().getProbabilityDistribution())
										- routes.get(r).get(preferredTimeWindow).get(0).getServiceBegin()
										+ routes.get(r).get(preferredTimeWindow).get(0).getTimeWindow().getStartTime()
												* TIME_MULTIPLIER;
							}

							int check = 0;
							int setCheckFromHereForLoop = 0;

							for (int j = preferredTimeWindow; j < routes.get(r).size(); j++) {

								if (j == preferredTimeWindow) {
									setCheckFromHereForLoop = checkFromHere;
								} else {
									setCheckFromHereForLoop = 0;
								}

								for (int i = setCheckFromHereForLoop; i < routes.get(r).get(j).size(); i++) {
									if (insertionCosts_Waiting > 0) {

										if (routes.get(r).get(preferredTimeWindow)
												.get(routes.get(r).get(preferredTimeWindow).size() - 1)
												.getServiceBegin()
												+ routes.get(r).get(preferredTimeWindow)
														.get(routes.get(r).get(preferredTimeWindow).size() - 1)
														.getServiceTime()
												+ insertionCosts.get(0).getDistanceIx() <= timeWindow
														.get(preferredTimeWindow - 1).getEndTime() * TIME_MULTIPLIER
												&& insertionCosts_Waiting <= routes.get(r).get(j).get(i).getSlack()
														+ routes.get(r).get(j).get(i).getWaitingTime()) {
										} else {

											check++;
											break;

										}
									}
									insertionCosts_Waiting = insertionCosts_Waiting
											- routes.get(r).get(j).get(i).getWaitingTime();
								}
							}

							if (check == 0) {
								insertionSuccessful = true;
							} else {
								insertionSuccessful = false;
							}

						} else {
							// customer is inserted at begin of time window,
							// start of service after start of time window
							double insertionCosts_Waiting;
							int check = 0;
							int setCheckFromHereForLoop = 0;

							insertionCosts_Waiting = routes.get(r).get(preferredTimeWindow - before)
									.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceBegin()
									+ routes.get(r).get(preferredTimeWindow - before)
											.get(routes.get(r).get(preferredTimeWindow - before).size() - 1)
											.getServiceTime()
									+ insertionCosts.get(0).getDistanceIx()
									- timeWindow.get(preferredTimeWindow - 1).getStartTime() * TIME_MULTIPLIER
									+ ProbabilityDistributionService
											.getMeanByProbabilityDistribution(order.getOrderRequest().getCustomer()
													.getServiceTimeSegment().getProbabilityDistribution())
									+ insertionCosts.get(0).getDistanceXj();

							for (int j = preferredTimeWindow; j < routes.get(r).size(); j++) {

								if (j == preferredTimeWindow) {
									setCheckFromHereForLoop = checkFromHere;
								} else {
									setCheckFromHereForLoop = 0;
								}

								for (int i = setCheckFromHereForLoop; i < routes.get(r).get(j).size(); i++) {
									if (insertionCosts_Waiting > 0) {
										if (routes.get(r).get(preferredTimeWindow)
												.get(routes.get(r).get(preferredTimeWindow).size() - 1)
												.getServiceBegin()
												+ routes.get(r).get(preferredTimeWindow)
														.get(routes.get(r).get(preferredTimeWindow).size() - 1)
														.getServiceTime()
												+ insertionCosts.get(0).getDistanceIx() <= timeWindow
														.get(preferredTimeWindow - 1).getEndTime() * TIME_MULTIPLIER
												&& insertionCosts_Waiting <= routes.get(r).get(j).get(i).getSlack()
														+ routes.get(r).get(j).get(i).getWaitingTime()) {
										} else {
											check++;
											break;
										}
									}
									insertionCosts_Waiting = insertionCosts_Waiting
											- routes.get(r).get(j).get(i).getWaitingTime();
								}
							}

							if (check == 0) {
								insertionSuccessful = true;
							} else {
								insertionSuccessful = false;
							}

						}

					}
				}
			}
		} else {
			if (insertionSuccessful == false) {

				// inserCustomerWithinTimeWindow
				// System.out.println("Precalculation:
				// inserCustomerWithinTimeWindow");
				// checkFromHere!=0 & customer is inserted within preferred time
				// window
				// further checks needed

				// insertionCosts
				double insertionCosts_Waiting = 0;
				insertionCosts_Waiting = insertionCosts.get(0).getInsertionCosts();

				// check if insertion is possible
				int check = 0;
				int setCheckFromHereForLoop = 0;

				for (int j = preferredTimeWindow; j < routes.get(r).size(); j++) {
					if (j == preferredTimeWindow) {
						setCheckFromHereForLoop = checkFromHere;
					} else {
						setCheckFromHereForLoop = 0;
					}
					for (int i = setCheckFromHereForLoop; i < routes.get(r).get(j).size(); i++) {

						if (insertionCosts_Waiting > 0) {
							if (routes.get(r).get(preferredTimeWindow)
									.get(routes.get(r).get(preferredTimeWindow).size() - 1).getServiceBegin()
									+ routes.get(r).get(preferredTimeWindow)
											.get(routes.get(r).get(preferredTimeWindow).size() - 1).getServiceTime()
									+ insertionCosts.get(0).getDistanceIx() <= timeWindow.get(preferredTimeWindow - 1)
											.getEndTime() * TIME_MULTIPLIER
									&& insertionCosts_Waiting <= routes.get(r).get(j).get(i).getSlack()
											+ routes.get(r).get(j).get(i).getWaitingTime()) {

							} else {
								check++;
							}
						}
						if (j > preferredTimeWindow) {
							insertionCosts_Waiting = insertionCosts_Waiting
									- routes.get(r).get(j).get(0).getWaitingTime();
						}
					}
				}
				if (check == 0) {
					insertionSuccessful = true;
				} else {
					insertionSuccessful = false;
				}

			}
		}
	}

	private void tryCheapestInsertion(ArrayList<ArrayList<ArrayList<RouteElement>>> routes, int preferredTimeWindow,
			ArrayList<TimeWindow> timeWindow, ArrayList<InsertionCosts> insertionCosts, Order order,
			ArrayList<AcceptedCustomersOnRouteBefore> before, ArrayList<AcceptedCustomersOnRouteAfter> after)
			throws ParameterUnknownException {

		// while order request could not be inserted
		while (insertionSuccessful == false) {

			if (insertionCosts.size() > 0) {
				// tryInsertion for first element (0) of insertion costs (=
				// cheapest insertion costs)
				tryInsertion(insertionCosts.get(0).getRoute(), routes, preferredTimeWindow,
						insertionCosts.get(0).getInsertAfterId(), timeWindow, insertionCosts, order,
						before.get(insertionCosts.get(0).getRoute()).getBefore(),
						after.get(insertionCosts.get(0).getRoute()).getAfter());

				// remove cheapest insertion costs, if insertion was not
				// successful insertion is tried for next cheapest costs
				insertionCosts.remove(0);
			} else {
				if (test == true) {
					System.out.println("Insertion for forecasted order request id " + order.getOrderRequestId()
							+ " not possible. ");
				}
				break;
			}
		}
	}

	private void tryInsertion(int r, ArrayList<ArrayList<ArrayList<RouteElement>>> routes, int preferredTimeWindow,
			int insertionPosition, ArrayList<TimeWindow> timeWindow, ArrayList<InsertionCosts> insertionCosts,
			Order order, int before, int after) throws ParameterUnknownException {

		int checkFromHere;
		checkFromHere = insertionCosts.get(0).getCheckFromHere();

		if (checkFromHere == 0) {

			// checkFromHere==0 & preferred time window empty: customer is
			// inserted in empty time window
			if (routes.get(r).get(preferredTimeWindow).isEmpty()) {
				if (insertionSuccessful == true) {
				} else {
					insertCustomerEmptyTimeWindow(r, routes, timeWindow, preferredTimeWindow, insertionCosts, order,
							before, after);
				}
			}

			if (routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size() - 1)
					.getId() == insertionCosts.get(0).getInsertAfterId()) {
				if (routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size() - 1)
						.getServiceBegin()
						+ routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size() - 1)
								.getServiceTime()
						+ insertionCosts.get(0).getDistanceIx() <= timeWindow.get(preferredTimeWindow - 1).getEndTime()
								* TIME_MULTIPLIER
						&& routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size() - 1)
								.getServiceBegin()
								+ routes.get(r).get(preferredTimeWindow)
										.get(routes.get(r).get(preferredTimeWindow).size() - 1).getServiceTime()
								+ insertionCosts.get(0).getDistanceIx()
								+ ProbabilityDistributionService
										.getMeanByProbabilityDistribution(order.getOrderRequest().getCustomer()
												.getServiceTimeSegment().getProbabilityDistribution())
								+ insertionCosts.get(0).getDistanceXj() <= routes.get(r)
										.get(preferredTimeWindow + after).get(checkFromHere).getServiceBegin()) {

					if (insertionSuccessful == true) {
					} else {
						if (insertionSuccessful == false) {

							// checkFromHere==0 & customer is inserted at end of
							// preferred time window, service begin of next
							// customer is not affected
							insertCustomerEndTimeWindowNoChanges(r, routes, preferredTimeWindow, timeWindow,
									insertionCosts, order, after);
						}
					}
				} else {
					if (insertionSuccessful == false) {

						// checkFromHere==0 & customer is inserted at end of
						// preferred time window, service begin of next customer
						// is affected
						insertCustomerEndTimeWindowChanges(r, routes, preferredTimeWindow, timeWindow, insertionCosts,
								order, after);
					}
				}
			} else {
				if (insertionSuccessful == false) {

					// checkFromHere==0 & customer is inserted at begin of
					// preferred time window

					insertCustomerBeginTimeWindow(r, routes, preferredTimeWindow, timeWindow, insertionCosts, order,
							before, after, checkFromHere);
				}
			}
		} else {
			if (insertionSuccessful == false) {

				// checkFromHere!=0 & customer is inserted within preferred time
				// window
				insertCustomerWithinTimeWindow(r, routes, preferredTimeWindow, timeWindow, insertionCosts, order,
						checkFromHere);
			}
		}
	}

	public void insertCustomerEmptyTimeWindow(int r, ArrayList<ArrayList<ArrayList<RouteElement>>> routes,
			ArrayList<TimeWindow> timeWindows, int preferredTimeWindow, ArrayList<InsertionCosts> insertionCosts,
			Order order, int before, int after) throws ParameterUnknownException {

		RouteElement element = new RouteElement();

		element.setOrder(order);
		element.setOrderId(order.getId());
		element.setId(order.getOrderRequest().getId());

		element.setServiceTime(ProbabilityDistributionService.getMeanByProbabilityDistribution(
				order.getOrderRequest().getCustomer().getServiceTimeSegment().getProbabilityDistribution()));

		// request service begin = start of preferred time window (service
		// begin, service time & travel time to route element < begin of
		// preferred time window)
		if (routes.get(r).get(preferredTimeWindow - before)
				.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceBegin()
				+ routes.get(r).get(preferredTimeWindow - before)
						.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceTime()
				+ insertionCosts.get(0).getDistanceIx() <= timeWindows.get(preferredTimeWindow - 1).getStartTime()
						* TIME_MULTIPLIER) {
			element.setServiceBegin(timeWindows.get(preferredTimeWindow - 1).getStartTime() * TIME_MULTIPLIER);

			// request service begin > start of preferred time window (service
			// begin, service time & travel time to route element > begin of
			// preferred time window)
		} else {
			element.setServiceBegin(routes.get(r).get(preferredTimeWindow - before)
					.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceBegin()
					+ routes.get(r).get(preferredTimeWindow - before)
							.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceTime()
					+ insertionCosts.get(0).getDistanceIx());
		}

		// request waiting time: 0 if service begin > start preferred time
		// window OR compute waiting time
		element.setWaitingTime(Math.max(0.0,
				timeWindows.get(preferredTimeWindow - 1).getStartTime() * TIME_MULTIPLIER
						- routes.get(r).get(preferredTimeWindow - before)
								.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceBegin()
						- routes.get(r).get(preferredTimeWindow - before)
								.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceTime()
						- insertionCosts.get(0).getDistanceIx()));

		element.setTravelTime(insertionCosts.get(0).getDistanceIx());
		element.setTimeWindow(timeWindows.get(preferredTimeWindow - 1));
		element.setTimeWindowId(timeWindows.get(preferredTimeWindow - 1).getId());
		element.setSlack(
				Math.max(0, timeWindows.get(preferredTimeWindow - 1).getEndTime() * TIME_MULTIPLIER - element.getServiceBegin()));

		// changes in route

		routes.get(r).get(preferredTimeWindow + after).get(0).setWaitingTime(
				(routes.get(r).get(preferredTimeWindow + after).get(0).getTimeWindow().getStartTime() * TIME_MULTIPLIER
						- element.getServiceBegin() - element.getServiceTime()
						- insertionCosts.get(0).getDistanceXj()));

		routes.get(r).get(preferredTimeWindow + after).get(0).setTravelTime(insertionCosts.get(0).getDistanceXj());

		// insertion
		routes.get(r).get(preferredTimeWindow).add(element);

		routes.get(r).get(preferredTimeWindow + after).get(0).setTravelTime(insertionCosts.get(0).getDistanceXj());
		insertionSuccessful = true;

		if (test == true) {

			for (int k = 0; k <= routes.get(0).size() - 1; k++) {
				for (int i = 0; i <= routes.get(0).get(k).size() - 1; i++) {

					System.out.println(routes.get(0).get(k).get(i).getId() + "       TravelTime: "
							+ routes.get(0).get(k).get(i).getTravelTime() + "       B: "
							+ routes.get(0).get(k).get(i).getServiceBegin() + "       E: " + k + "       S: "
							+ routes.get(0).get(k).get(i).getSlack() + "       W: "
							+ routes.get(0).get(k).get(i).getWaitingTime());

				}
			}
			System.out.println("\n");
		}

	}

	private void insertCustomerWithinTimeWindow(int r, ArrayList<ArrayList<ArrayList<RouteElement>>> routes,
			int preferredTimeWindow, ArrayList<TimeWindow> timeWindows, ArrayList<InsertionCosts> insertionCosts,
			Order order, int checkFromHere) throws ParameterUnknownException {

		// insertionCosts
		double insertionCosts_Waiting = 0;
		insertionCosts_Waiting = insertionCosts.get(0).getInsertionCosts();

		// check if insertion is possible
		int check = 0;
		int setCheckFromHereForLoop = 0;

		for (int j = preferredTimeWindow; j < routes.get(r).size(); j++) {
			if (j == preferredTimeWindow) {
				setCheckFromHereForLoop = checkFromHere;
			} else {
				setCheckFromHereForLoop = 0;
			}
			for (int i = setCheckFromHereForLoop; i < routes.get(r).get(j).size(); i++) {

				if (insertionCosts_Waiting > 0) {
					if (routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size() - 1)
							.getServiceBegin()
							+ routes.get(r).get(preferredTimeWindow)
									.get(routes.get(r).get(preferredTimeWindow).size() - 1).getServiceTime()
							+ insertionCosts.get(0).getDistanceIx() <= timeWindows.get(preferredTimeWindow - 1)
									.getEndTime() * TIME_MULTIPLIER
							&& insertionCosts_Waiting <= routes.get(r).get(j).get(i).getSlack()
									+ routes.get(r).get(j).get(i).getWaitingTime()) {

					} else {
						check++;
					}
				}
				if (j > preferredTimeWindow) {
					insertionCosts_Waiting = insertionCosts_Waiting - routes.get(r).get(j).get(0).getWaitingTime();
				}
			}
		}

		// changes in route
		if (check == 0) {

			// changes in same time window only for elements after insertion
			setCheckFromHereForLoop = 0;
			insertionCosts_Waiting = insertionCosts.get(0).getInsertionCosts();
			for (int j = preferredTimeWindow; j < routes.get(r).size(); j++) {
				if (j == preferredTimeWindow) {
					setCheckFromHereForLoop = checkFromHere;
				} else {
					setCheckFromHereForLoop = 0;
				}
				for (int i = setCheckFromHereForLoop; i < routes.get(r).get(j).size(); i++) {
					if (routes.get(r).get(j).get(0).getWaitingTime() >= insertionCosts_Waiting
							&& j > preferredTimeWindow) {
						routes.get(r).get(j).get(0).setWaitingTime(
								(routes.get(r).get(j).get(0).getWaitingTime() - insertionCosts_Waiting));
						insertionCosts_Waiting = 0;
					}
					if (routes.get(r).get(j).get(0).getWaitingTime() < insertionCosts_Waiting
							&& j > preferredTimeWindow) {
						insertionCosts_Waiting = Math.max(0,
								insertionCosts_Waiting - routes.get(r).get(j).get(0).getWaitingTime());
						routes.get(r).get(j).get(0).setWaitingTime(0.0);
					}

					routes.get(r).get(j).get(i)
							.setServiceBegin((routes.get(r).get(j).get(i).getServiceBegin() + insertionCosts_Waiting));
					routes.get(r).get(j).get(i)
							.setSlack(Math.max(0, routes.get(r).get(j).get(i).getTimeWindow().getEndTime() * TIME_MULTIPLIER
									- routes.get(r).get(j).get(i).getServiceBegin()));
					routes.get(r).get(preferredTimeWindow).get(checkFromHere).setWaitingTime(0.0);
				}
			}

			RouteElement element = new RouteElement();
			element.setOrder(order);
			element.setOrderId(order.getId());
			element.setId(order.getOrderRequest().getId());
			element.setServiceTime(ProbabilityDistributionService.getMeanByProbabilityDistribution(
					order.getOrderRequest().getCustomer().getServiceTimeSegment().getProbabilityDistribution()));

			// insertion
			routes.get(r).get(preferredTimeWindow).get(checkFromHere)
					.setTravelTime(insertionCosts.get(0).getDistanceXj());
			routes.get(r).get(preferredTimeWindow).add(checkFromHere, element);

			element.setServiceBegin((routes.get(r).get(preferredTimeWindow).get(checkFromHere - 1).getServiceBegin()
					+ routes.get(r).get(preferredTimeWindow).get(checkFromHere - 1).getServiceTime()
					+ insertionCosts.get(0).getDistanceIx()));
			element.setTravelTime(insertionCosts.get(0).getDistanceIx());
			element.setWaitingTime(0.0);
			element.setTimeWindow(timeWindows.get(preferredTimeWindow - 1));
			element.setTimeWindowId(timeWindows.get(preferredTimeWindow - 1).getId());
			element.setSlack(Math.max(0,
					timeWindows.get(preferredTimeWindow - 1).getEndTime() * TIME_MULTIPLIER - element.getServiceBegin()));

			insertionSuccessful = true;

			if (test == true) {

				for (int k = 0; k <= routes.get(0).size() - 1; k++) {
					for (int i = 0; i <= routes.get(0).get(k).size() - 1; i++) {
						System.out.println(routes.get(0).get(k).get(i).getId() + "       TravelTime: "
								+ routes.get(0).get(k).get(i).getTravelTime() + "       B: "
								+ routes.get(0).get(k).get(i).getServiceBegin() + "       E: " + k + "       S: "
								+ routes.get(0).get(k).get(i).getSlack() + "       W: "
								+ routes.get(0).get(k).get(i).getWaitingTime());
					}
				}
				System.out.println("\n");
			}

		}
	}

	private void insertCustomerBeginTimeWindow(int r, ArrayList<ArrayList<ArrayList<RouteElement>>> routes,
			int preferredTimeWindow, ArrayList<TimeWindow> timeWindows, ArrayList<InsertionCosts> insertionCosts,
			Order order, int before, int after, int checkFromHere) throws ParameterUnknownException {

		// request service begin = start of preferred time window (service
		// begin, service time & travel time to route element < begin of
		// preferred time window)
		if (routes.get(r).get(preferredTimeWindow - before)
				.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceBegin()
				+ routes.get(r).get(preferredTimeWindow - before)
						.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceTime()
				+ insertionCosts.get(0).getDistanceIx() < timeWindows.get(preferredTimeWindow - 1).getStartTime()
						* TIME_MULTIPLIER) {

			insertCustomerBeginTimeWindowStartE(r, routes, preferredTimeWindow, timeWindows, insertionCosts, order,
					before, after, checkFromHere);

			// request service begin > start of preferred time window (service
			// begin, service time & travel time to route element > begin of
			// preferred time window)
		} else {
			insertCustomerBeginTimeWindowStartAfterE(r, routes, preferredTimeWindow, timeWindows, insertionCosts, order,
					before, checkFromHere);
		}
	}

	private void insertCustomerBeginTimeWindowStartAfterE(int r, ArrayList<ArrayList<ArrayList<RouteElement>>> routes,
			int preferredTimeWindow, ArrayList<TimeWindow> timeWindows, ArrayList<InsertionCosts> insertionCosts,
			Order order, int before, int checkFromHere) throws ParameterUnknownException {

		double insertionCosts_Waiting;
		int check = 0;
		int setCheckFromHereForLoop = 0;

		// reduction of insertionCosts when waiting time buffers exist
		insertionCosts_Waiting = routes.get(r).get(preferredTimeWindow - before)
				.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceBegin()
				+ routes.get(r).get(preferredTimeWindow - before)
						.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceTime()
				+ insertionCosts.get(0).getDistanceIx() - timeWindows.get(preferredTimeWindow - 1).getStartTime() * TIME_MULTIPLIER
				+ ProbabilityDistributionService.getMeanByProbabilityDistribution(
						order.getOrderRequest().getCustomer().getServiceTimeSegment().getProbabilityDistribution())
				+ insertionCosts.get(0).getDistanceXj();

		// check if insertion is possible
		for (int j = preferredTimeWindow; j < routes.get(r).size(); j++) {

			// changes in same time window only for elements after insertion
			if (j == preferredTimeWindow) {
				setCheckFromHereForLoop = checkFromHere;
			} else {
				setCheckFromHereForLoop = 0;
			}

			for (int i = setCheckFromHereForLoop; i < routes.get(r).get(j).size(); i++) {
				if (insertionCosts_Waiting > 0) {
					if (routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size() - 1)
							.getServiceBegin()
							+ routes.get(r).get(preferredTimeWindow)
									.get(routes.get(r).get(preferredTimeWindow).size() - 1).getServiceTime()
							+ insertionCosts.get(0).getDistanceIx() <= timeWindows.get(preferredTimeWindow - 1)
									.getEndTime() * TIME_MULTIPLIER
							&& insertionCosts_Waiting <= routes.get(r).get(j).get(i).getSlack()
									+ routes.get(r).get(j).get(i).getWaitingTime()) {
					} else {
						check++;
						break;
					}
				}
				insertionCosts_Waiting = insertionCosts_Waiting - routes.get(r).get(j).get(i).getWaitingTime();
			}
		}

		// customer & changes in route
		if (check == 0) {

			setCheckFromHereForLoop = 0;

			RouteElement element = new RouteElement();
			element.setOrder(order);
			element.setOrderId(order.getId());
			element.setId(order.getOrderRequest().getId());
			element.setServiceTime(ProbabilityDistributionService.getMeanByProbabilityDistribution(
					order.getOrderRequest().getCustomer().getServiceTimeSegment().getProbabilityDistribution()));

			element.setServiceBegin((routes.get(r).get(preferredTimeWindow - before)
					.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceBegin()
					+ routes.get(r).get(preferredTimeWindow - before)
							.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceTime()
					+ insertionCosts.get(0).getDistanceIx()));

			if (routes.get(r).get(preferredTimeWindow).get(0)
					.getServiceBegin() != routes.get(r).get(preferredTimeWindow).get(0).getTimeWindow().getStartTime()
							* TIME_MULTIPLIER) {
				insertionCosts_Waiting = Math.max(0,
						routes.get(r).get(preferredTimeWindow - before)
								.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceBegin()
								+ routes.get(r).get(preferredTimeWindow - before)
										.get(routes.get(r).get(preferredTimeWindow - before).size() - 1)
										.getServiceTime()
								+ insertionCosts.get(0).getDistanceIx()
								- timeWindows.get(preferredTimeWindow - 1).getStartTime() * TIME_MULTIPLIER
								+ ProbabilityDistributionService
										.getMeanByProbabilityDistribution(order.getOrderRequest().getCustomer()
												.getServiceTimeSegment().getProbabilityDistribution())
								+ insertionCosts.get(0).getDistanceXj()
								- routes.get(r).get(preferredTimeWindow).get(0).getServiceBegin()
								+ routes.get(r).get(preferredTimeWindow).get(0).getTimeWindow().getStartTime() * TIME_MULTIPLIER);
			} else {
				insertionCosts_Waiting = Math.max(0,
						routes.get(r).get(preferredTimeWindow - before)
								.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceBegin()
								+ routes.get(r).get(preferredTimeWindow - before)
										.get(routes.get(r).get(preferredTimeWindow - before).size() - 1)
										.getServiceTime()
								+ insertionCosts.get(0).getDistanceIx()
								- timeWindows.get(preferredTimeWindow - 1).getStartTime() * TIME_MULTIPLIER
								+ ProbabilityDistributionService
										.getMeanByProbabilityDistribution(order.getOrderRequest().getCustomer()
												.getServiceTimeSegment().getProbabilityDistribution())
								+ insertionCosts.get(0).getDistanceXj());
			}

			for (int j = preferredTimeWindow; j < routes.get(r).size(); j++) {
				if (j == preferredTimeWindow) {
					setCheckFromHereForLoop = checkFromHere;
				} else {
					setCheckFromHereForLoop = 0;
				}
				for (int i = setCheckFromHereForLoop; i < routes.get(r).get(j).size(); i++) {
					if (j > preferredTimeWindow) {

						insertionCosts_Waiting = Math.max(0,
								insertionCosts_Waiting - routes.get(r).get(j).get(i).getWaitingTime());
					}
					routes.get(r).get(j).get(i)
							.setServiceBegin((routes.get(r).get(j).get(i).getServiceBegin() + insertionCosts_Waiting));
					routes.get(r).get(j).get(i)
							.setSlack(Math.max(0f, routes.get(r).get(j).get(i).getTimeWindow().getEndTime() * TIME_MULTIPLIER
									- routes.get(r).get(j).get(i).getServiceBegin()));

					double w = 100;
					w = insertionCosts_Waiting;

					if (i == 0 && j > preferredTimeWindow) {

						routes.get(r).get(j).get(i)
								.setWaitingTime(Math.max(0, routes.get(r).get(j).get(0).getWaitingTime() - w));
					}

					routes.get(r).get(preferredTimeWindow).get(0).setWaitingTime(0.0);

				}
			}

			element.setTravelTime(insertionCosts.get(0).getDistanceIx());
			element.setWaitingTime(0.0);
			element.setTimeWindow(timeWindows.get(preferredTimeWindow - 1));
			element.setTimeWindowId(timeWindows.get(preferredTimeWindow - 1).getId());
			element.setSlack(Math.max(0f,
					timeWindows.get(preferredTimeWindow - 1).getEndTime() * TIME_MULTIPLIER - element.getServiceBegin()));

			// insertion
			routes.get(r).get(preferredTimeWindow).add(0, element);
			insertionSuccessful = true;

			routes.get(r).get(preferredTimeWindow).get(1).setTravelTime(insertionCosts.get(0).getDistanceXj());

			if (test == true) {

				for (int k = 0; k <= routes.get(0).size() - 1; k++) {
					for (int i = 0; i <= routes.get(0).get(k).size() - 1; i++) {
						System.out.println(routes.get(0).get(k).get(i).getId() + "       TravelTime: "
								+ routes.get(0).get(k).get(i).getTravelTime() + "       B: "
								+ routes.get(0).get(k).get(i).getServiceBegin() + "       E: " + k + "       S: "
								+ routes.get(0).get(k).get(i).getSlack() + "       W: "
								+ routes.get(0).get(k).get(i).getWaitingTime());
					}
				}
				System.out.println("\n");
			}
		}

	}

	private void insertCustomerBeginTimeWindowStartE(int r, ArrayList<ArrayList<ArrayList<RouteElement>>> routes,
			int preferredTimeWindow, ArrayList<TimeWindow> timeWindows, ArrayList<InsertionCosts> insertionCosts,
			Order order, int before, int after, int checkFromHere) throws ParameterUnknownException {

		// insertionCosts
		double insertionCosts_Waiting;

		// if service begin = begin of time window: insertionCosts_Waiting xj +
		// serviceTime
		if (routes.get(r).get(preferredTimeWindow).get(0)
				.getServiceBegin() == routes.get(r).get(preferredTimeWindow).get(0).getTimeWindow().getStartTime()
						* TIME_MULTIPLIER) {
			insertionCosts_Waiting = insertionCosts.get(0).getDistanceXj()
					+ ProbabilityDistributionService.getMeanByProbabilityDistribution(
							order.getOrderRequest().getCustomer().getServiceTimeSegment().getProbabilityDistribution());

			// if service begin > begin of time window: insertionCosts_Waiting
			// xj + serviceTime + service begin - time window begin
		} else {
			insertionCosts_Waiting = insertionCosts.get(0).getDistanceXj()
					+ ProbabilityDistributionService.getMeanByProbabilityDistribution(
							order.getOrderRequest().getCustomer().getServiceTimeSegment().getProbabilityDistribution())
					- routes.get(r).get(preferredTimeWindow).get(0).getServiceBegin()
					+ routes.get(r).get(preferredTimeWindow).get(0).getTimeWindow().getStartTime() * TIME_MULTIPLIER;
		}

		// check
		int check = 0;
		int setCheckFromHereForLoop = 0;

		for (int j = preferredTimeWindow; j < routes.get(r).size(); j++) {

			if (j == preferredTimeWindow) {
				setCheckFromHereForLoop = checkFromHere;
			} else {
				setCheckFromHereForLoop = 0;
			}

			for (int i = setCheckFromHereForLoop; i < routes.get(r).get(j).size(); i++) {
				if (insertionCosts_Waiting > 0) {

					if (routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size() - 1)
							.getServiceBegin()
							+ routes.get(r).get(preferredTimeWindow)
									.get(routes.get(r).get(preferredTimeWindow).size() - 1).getServiceTime()
							+ insertionCosts.get(0).getDistanceIx() <= timeWindows.get(preferredTimeWindow - 1)
									.getEndTime() * TIME_MULTIPLIER
							&& insertionCosts_Waiting <= routes.get(r).get(j).get(i).getSlack()
									+ routes.get(r).get(j).get(i).getWaitingTime()) {
					} else {

						check++;
						break;

					}
				}
				insertionCosts_Waiting = insertionCosts_Waiting - routes.get(r).get(j).get(i).getWaitingTime();
			}
		}

		// customer & changes in route
		if (check == 0) {

			RouteElement element = new RouteElement();
			element.setOrder(order);
			element.setOrderId(order.getId());
			element.setId(order.getOrderRequest().getId());

			element.setServiceBegin(timeWindows.get(preferredTimeWindow - 1).getStartTime() * TIME_MULTIPLIER);
			element.setServiceTime(ProbabilityDistributionService.getMeanByProbabilityDistribution(
					order.getOrderRequest().getCustomer().getServiceTimeSegment().getProbabilityDistribution()));

			setCheckFromHereForLoop = 0;

			if (routes.get(r).get(preferredTimeWindow).get(0)
					.getServiceBegin() == routes.get(r).get(preferredTimeWindow).get(0).getTimeWindow().getStartTime()
							* TIME_MULTIPLIER) {
				insertionCosts_Waiting = insertionCosts.get(0).getDistanceXj()
						+ ProbabilityDistributionService.getMeanByProbabilityDistribution(order.getOrderRequest()
								.getCustomer().getServiceTimeSegment().getProbabilityDistribution());
			} else {
				insertionCosts_Waiting = insertionCosts.get(0).getDistanceXj()
						+ ProbabilityDistributionService.getMeanByProbabilityDistribution(order.getOrderRequest()
								.getCustomer().getServiceTimeSegment().getProbabilityDistribution())
						- routes.get(r).get(preferredTimeWindow).get(0).getServiceBegin()
						+ routes.get(r).get(preferredTimeWindow).get(0).getTimeWindow().getStartTime() * TIME_MULTIPLIER;
			}

			for (int j = preferredTimeWindow; j < routes.get(r).size(); j++) {

				if (j == preferredTimeWindow) {
					setCheckFromHereForLoop = checkFromHere;
				} else {
					setCheckFromHereForLoop = 0;
				}

				for (int i = setCheckFromHereForLoop; i < routes.get(r).get(j).size(); i++) {

					if (insertionCosts_Waiting > 0) {
						routes.get(r).get(preferredTimeWindow).get(0).setWaitingTime(0.0);

						double w = insertionCosts_Waiting;

						if (j > preferredTimeWindow) {
							insertionCosts_Waiting = Math.max(0,
									insertionCosts_Waiting - routes.get(r).get(j).get(i).getWaitingTime());
						}
						if (i == 0 && j > preferredTimeWindow) {
							routes.get(r).get(j).get(i)
									.setWaitingTime(Math.max(0, routes.get(r).get(j).get(0).getWaitingTime() - w));
						}
						routes.get(r).get(j).get(i).setServiceBegin(
								(routes.get(r).get(j).get(i).getServiceBegin() + insertionCosts_Waiting));
						routes.get(r).get(j).get(i)
								.setSlack(Math.max(0, routes.get(r).get(j).get(i).getTimeWindow().getEndTime() * TIME_MULTIPLIER
										- routes.get(r).get(j).get(i).getServiceBegin()));

					} else {
						break;
					}
				}
			}
			element.setTravelTime(insertionCosts.get(0).getDistanceIx());
			element.setWaitingTime((timeWindows.get(preferredTimeWindow - 1).getStartTime() * TIME_MULTIPLIER
					- routes.get(r).get(preferredTimeWindow - before)
							.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceBegin()
					- routes.get(r).get(preferredTimeWindow - before)
							.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceTime()
					- insertionCosts.get(0).getDistanceIx()));
			element.setTimeWindow(timeWindows.get(preferredTimeWindow - 1));
			element.setTimeWindowId(timeWindows.get(preferredTimeWindow - 1).getId());
			element.setSlack(Math.max(0,
					timeWindows.get(preferredTimeWindow - 1).getEndTime() * TIME_MULTIPLIER - element.getServiceBegin()));

			// insertion
			routes.get(r).get(preferredTimeWindow).add(0, element);
			insertionSuccessful = true;

			routes.get(r).get(preferredTimeWindow).get(1).setTravelTime(insertionCosts.get(0).getDistanceXj());

			if (test == true) {

				for (int k = 0; k <= routes.get(0).size() - 1; k++) {
					for (int i = 0; i <= routes.get(0).get(k).size() - 1; i++) {
						System.out.println(routes.get(0).get(k).get(i).getId() + "       TravelTime: "
								+ routes.get(0).get(k).get(i).getTravelTime() + "       B: "
								+ routes.get(0).get(k).get(i).getServiceBegin() + "       E: " + k + "       S: "
								+ routes.get(0).get(k).get(i).getSlack() + "       W: "
								+ routes.get(0).get(k).get(i).getWaitingTime());
					}
				}
				System.out.println("\n");
			}
		}
	}

	private void insertCustomerEndTimeWindowChanges(int r, ArrayList<ArrayList<ArrayList<RouteElement>>> routes,
			int preferredTimeWindow, ArrayList<TimeWindow> timeWindows, ArrayList<InsertionCosts> insertionCosts,
			Order order, int after) throws ParameterUnknownException {

		// insertionCosts
		double insertionCosts_Waiting = insertionCosts.get(0).getInsertionCosts()
				- routes.get(r).get(preferredTimeWindow + after).get(0).getWaitingTime();

		// check
		int check = 0;
		int check2 = 0;

		for (int j = preferredTimeWindow + after; j < routes.get(r).size(); j++) {
			for (int i = 0; i < routes.get(r).get(j).size(); i++) {
				if (insertionCosts_Waiting > 0) {

					if (routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size() - 1)
							.getServiceBegin()
							+ routes.get(r).get(preferredTimeWindow)
									.get(routes.get(r).get(preferredTimeWindow).size() - 1).getServiceTime()
							+ insertionCosts.get(0).getDistanceIx() <= timeWindows.get(preferredTimeWindow - 1)
									.getEndTime() * TIME_MULTIPLIER
							&& insertionCosts_Waiting <= routes.get(r).get(j).get(i).getSlack()
									+ routes.get(r).get(j).get(i).getWaitingTime()) {

						check2++;
					} else {
						check++;
						break;
					}
				}

			}
		}

		// customer & changes in route
		if (check == 0 && check2 != 0) {

			for (int j = preferredTimeWindow + after; j < routes.get(r).size(); j++) {
				for (int i = 0; i < routes.get(r).get(j).size(); i++) {
					routes.get(r).get(j).get(i)
							.setServiceBegin((routes.get(r).get(j).get(i).getServiceBegin() + insertionCosts_Waiting));
					routes.get(r).get(j).get(i)
							.setSlack(Math.max(0, routes.get(r).get(j).get(i).getTimeWindow().getEndTime() * TIME_MULTIPLIER
									- routes.get(r).get(j).get(i).getServiceBegin()));

					insertionCosts_Waiting = insertionCosts_Waiting - routes.get(r).get(j).get(i).getWaitingTime();
				}
			}

			routes.get(r).get(preferredTimeWindow + after).get(0).setTravelTime(insertionCosts.get(0).getDistanceXj());
			routes.get(r).get(preferredTimeWindow + after).get(0).setWaitingTime(0.0);

			RouteElement element = new RouteElement();
			element.setOrder(order);
			element.setOrderId(order.getId());
			element.setId(order.getOrderRequest().getId());

			element.setServiceTime(ProbabilityDistributionService.getMeanByProbabilityDistribution(
					order.getOrderRequest().getCustomer().getServiceTimeSegment().getProbabilityDistribution()));
			element.setServiceBegin((routes.get(r).get(preferredTimeWindow)
					.get(routes.get(r).get(preferredTimeWindow).size() - 1).getServiceBegin()
					+ routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size() - 1)
							.getServiceTime()
					+ insertionCosts.get(0).getDistanceIx()));

			element.setTravelTime(insertionCosts.get(0).getDistanceIx());
			element.setWaitingTime(0.0);
			element.setTimeWindow(timeWindows.get(preferredTimeWindow - 1));
			element.setTimeWindowId(timeWindows.get(preferredTimeWindow - 1).getId());
			element.setSlack(Math.max(0,
					timeWindows.get(preferredTimeWindow - 1).getEndTime() * TIME_MULTIPLIER - element.getServiceBegin()));

			// insertion
			routes.get(r).get(preferredTimeWindow).add(element);
			insertionSuccessful = true;
			if (test == true) {

				for (int k = 0; k <= routes.get(0).size() - 1; k++) {
					for (int i = 0; i <= routes.get(0).get(k).size() - 1; i++) {
						System.out.println(routes.get(0).get(k).get(i).getId() + "       TravelTime: "
								+ routes.get(0).get(k).get(i).getTravelTime() + "       B: "
								+ routes.get(0).get(k).get(i).getServiceBegin() + "       E: " + k + "       S: "
								+ routes.get(0).get(k).get(i).getSlack() + "       W: "
								+ routes.get(0).get(k).get(i).getWaitingTime());
					}
				}
				System.out.println("\n");
			}
		}
	}

	private void insertCustomerEndTimeWindowNoChanges(int r, ArrayList<ArrayList<ArrayList<RouteElement>>> routes,
			int preferredTimeWindow, ArrayList<TimeWindow> timeWindows, ArrayList<InsertionCosts> insertionCosts,
			Order order, int after) throws ParameterUnknownException {

		// Routing element
		RouteElement element = new RouteElement();
		element.setOrder(order);
		element.setOrderId(order.getId());
		element.setId(order.getOrderRequest().getId());

		element.setServiceTime(ProbabilityDistributionService.getMeanByProbabilityDistribution(
				order.getOrderRequest().getCustomer().getServiceTimeSegment().getProbabilityDistribution()));
		element.setServiceBegin((routes.get(r).get(preferredTimeWindow)
				.get(routes.get(r).get(preferredTimeWindow).size() - 1).getServiceBegin()
				+ routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size() - 1)
						.getServiceTime()
				+ insertionCosts.get(0).getDistanceIx()));

		element.setTravelTime(insertionCosts.get(0).getDistanceIx());
		element.setWaitingTime(0.0);
		element.setTimeWindow(timeWindows.get(preferredTimeWindow - 1));
		element.setTimeWindowId(timeWindows.get(preferredTimeWindow - 1).getId());
		element.setSlack(
				Math.max(0, timeWindows.get(preferredTimeWindow - 1).getEndTime() * TIME_MULTIPLIER - element.getServiceBegin()));

		// changes in route
		routes.get(r).get(preferredTimeWindow + after).get(0).setWaitingTime(
				routes.get(r).get(preferredTimeWindow + after).get(0).getTimeWindow().getStartTime() * TIME_MULTIPLIER
						- element.getServiceBegin() - element.getServiceTime() - insertionCosts.get(0).getDistanceXj());
		routes.get(r).get(preferredTimeWindow + after).get(0).setTravelTime(insertionCosts.get(0).getDistanceXj());

		// insertion
		routes.get(r).get(preferredTimeWindow).add(element);
		insertionSuccessful = true;

		if (test == true) {

			for (int k = 0; k <= routes.get(0).size() - 1; k++) {
				for (int i = 0; i <= routes.get(0).get(k).size() - 1; i++) {
					System.out.println(routes.get(0).get(k).get(i).getId() + "       TravelTime: "
							+ routes.get(0).get(k).get(i).getTravelTime() + "       B: "
							+ routes.get(0).get(k).get(i).getServiceBegin() + "       E: " + k + "       S: "
							+ routes.get(0).get(k).get(i).getSlack() + "       W: "
							+ routes.get(0).get(k).get(i).getWaitingTime());
				}
			}
			System.out.println("\n");
		}

	}

	public Routing getResult() {
		return this.routing;
	}

	public static String[] getParameterSetting() {
		return paras;
	}
}
