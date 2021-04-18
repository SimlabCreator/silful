package logic.algorithm.vr.construction;

import java.util.ArrayList;
import java.util.Collections;

import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.Depot;
import data.entity.Node;
import data.entity.NodeDistance;
import data.entity.Route;
import data.entity.RouteElement;
import data.entity.Routing;
import data.entity.TimeWindow;
import data.entity.TimeWindowSet;
import data.entity.TravelTimeSet;
import data.entity.Vehicle;
import logic.algorithm.vr.RoutingAlgorithm;
import logic.entity.AcceptedCustomersOnRouteAfter;
import logic.entity.AcceptedCustomersOnRouteBefore;
import logic.entity.ForecastedOrderRequest;
import logic.entity.InsertionCosts;
import logic.service.support.LocationService;
import logic.service.support.RoutingService;
import logic.utility.comparator.ForecastedOrderRequestValueDescComparator;
import logic.utility.comparator.TimeWindowStartAscComparator;

/**
 * Computes insertion costs for every forecasted request
 * Inserts requests into route if feasible
 * 
 * @author C. K�hler
 *
 */
public class Old_InsertionConstructionHeuristic implements RoutingAlgorithm {

	boolean test = false; // TODO: DELETE

	// check if customer was inserted in route
	boolean insertionSuccessful = false;

	private ArrayList<ForecastedOrderRequest> requests;

	private TimeWindowSet timeWindowSet;
	private Depot depot;
	//private TravelTimeSet travelTimeSet; 
	private ArrayList<Vehicle> vehicles;
	private ArrayList<NodeDistance> distances;
	private ArrayList<Node> nodes;
	private DeliveryAreaSet deliveryAreaSet;
	private DeliveryArea centerArea;
	private double[][] distanceAreas;

	//TODO 
	// 0: in -> in
	// 1: in -> out
	// 2: out -> in
	// 3: out -> out
	
	//dummy 0 in beginning, preferredTimeWindow starts with 1
	double[][] timeDependentWeighting = 	{			
		{0, 0.95, 0.915, 0.87, 0.715, 0.84, 1.075},	
		{0, 0.79, 0.795, 0.725, 0.615, 0.775, 1.1},	
		{0, 1.1, 0.775, 0.615, 0.725, 0.795, 0.79},	
		{0, 0.79, 0.795, 0.725, 0.615, 0.775, 1.1}};	
	



	
	private Routing routing;
	private static String[] paras = new String[] { "Constant_service_time" };

	public Old_InsertionConstructionHeuristic(ArrayList<ForecastedOrderRequest> requests, TimeWindowSet timeWindowSet,
			Depot depot, ArrayList<Vehicle> vehicles, ArrayList<NodeDistance> distances, ArrayList<Node> nodes, TravelTimeSet travelTimeSet, DeliveryAreaSet deliveryAreaSet) {
		this.requests = requests;
		this.timeWindowSet = timeWindowSet;
		this.distances = distances;
		this.vehicles = vehicles;
		this.depot = depot;
		this.nodes = nodes;
		//this.travelTimeSet = travelTimeSet; TODO
		this.deliveryAreaSet = deliveryAreaSet;
	}


	public void start() {
			
		
		ArrayList<ArrayList<ArrayList<RouteElement>>> vehicleRoutes = new ArrayList<ArrayList<ArrayList<RouteElement>>>();

		// Prepare list of time windows and sort by start
		ArrayList<TimeWindow> timeWindows = timeWindowSet.getElements();
		
		Collections.sort(timeWindows, new TimeWindowStartAscComparator());

		// Sort forecasted order requests
		Collections.sort(requests, new ForecastedOrderRequestValueDescComparator());
		for(int i=0; i < requests.size(); i++){
			if(requests.get(i).getAlternativePreferenceList().get(1).getId()==31 &&requests.get(i).getDeliveryAreaId()==16){
				System.out.println("Position "+i+" value "+requests.get(i).getEstimatedValue());
			}
		}
		
		//Calculate distance matrix between delivery areas
		distanceAreas=LocationService.computeDistancesBetweenAreas(nodes, distances, deliveryAreaSet);

		// Closest node to depot (needed for dummy depot-elements)
		Node depotNode = LocationService.findClosestNode(nodes, depot.getLat(), depot.getLon());
		this.centerArea = LocationService.assignNodeToDeliveryArea(deliveryAreaSet, depotNode);

		// Create as many routes as vehicles
		for (int vehicleTypeNo = 0; vehicleTypeNo < vehicles.size(); vehicleTypeNo++) {
			for (int vehicleNo = 0; vehicleNo < ((Vehicle) vehicles.get(vehicleTypeNo)).getVehicleNo(); vehicleNo++) {
				ArrayList<ArrayList<RouteElement>> vehicleRoute = new ArrayList<ArrayList<RouteElement>>();
				
				//two extra timeWindows for Depot at Begin and end
				for (int twID = 0; twID < (timeWindows.size()+2); twID++) {
					
					ArrayList<RouteElement> elementsList = new ArrayList<RouteElement>();
					
					// If it is the first or last time window, the route is
					// initialized with a dummy element representing the depot
					if (twID == 0) {

						RouteElement elementBeginning = new RouteElement();
						elementBeginning.setPosition(0);
						elementBeginning.setId(111111);
						elementBeginning.setTimeWindowId(11);
						
						elementBeginning.setServiceBegin(550.0);
						//elementBeginning.setServiceBegin(timeWindows.get(twID).getStartTime()*60);
						elementBeginning.setServiceTime( 0.0);
						elementBeginning.setSlack( 0.0);
						elementBeginning.setWaitingTime( 0.0);
						elementBeginning.setTravelTime(0.0);
						ForecastedOrderRequest request = new ForecastedOrderRequest();
						request.setClosestNode(depotNode);
						elementBeginning.setForecastedOrderRequest(request);
						elementsList.add(elementBeginning);
					}

					//if (twID == timeWindows.size() - 1) {
					if (twID == timeWindows.size() +1) {
						RouteElement elementBeginning = new RouteElement();
						elementBeginning.setPosition(0);
						elementBeginning.setId(999999);
						elementBeginning.setTimeWindowId(10);
						//begin of depot set early to lever out time window constraint for depot
						elementBeginning.setServiceBegin(1400.0);
						
						
						elementBeginning.setServiceTime(0.0);
						elementBeginning.setSlack(10000.0);
						elementBeginning.setWaitingTime(0.0);
						elementBeginning.setTravelTime(0.0);
						ForecastedOrderRequest request = new ForecastedOrderRequest();
						request.setClosestNode(depotNode);
						elementBeginning.setForecastedOrderRequest(request);
						elementsList.add(elementBeginning);
					}

					vehicleRoute.add(elementsList);

				}
				vehicleRoutes.add(vehicleRoute);
			}
		}

		// Go through all potential requests and perform routing
		for (ForecastedOrderRequest request : requests) {

			// Determine preferred window
			// TODO: At the moment, only works with one possible alternative
			// that refers to exactly one time window
			TimeWindow preferredTimeWindow = request.getAlternativePreferenceList().get(1).getTimeWindows()
					.get(0);
			int preferredTimeWindowId = 0;
			for (int twID = 0; twID < timeWindows.size(); twID++) {
				if (preferredTimeWindow.getId() == timeWindows.get(twID).getId()) {
					preferredTimeWindowId = twID;
					break;
				}
			}
			
			//preferredTimeWindowId starts at list 1, list 0 is reserved for depot
			preferredTimeWindowId = preferredTimeWindowId + 1;

			
			ArrayList<AcceptedCustomersOnRouteBefore> before = RoutingService
					.findLastCustomerBeforePreferredTimeWindow(vehicleRoutes, preferredTimeWindowId);
			ArrayList<AcceptedCustomersOnRouteAfter> after = RoutingService
					.findFirstCustomerAfterPreferredTimeWindow(vehicleRoutes, preferredTimeWindowId);
			

			// Determine insertion costs
			ArrayList<InsertionCosts> insertionCosts = this.computeAndSortInsertionCosts(preferredTimeWindowId,
					vehicleRoutes, timeWindows, request, before, after, this.distances);

			// Insert into routing
			this.tryCheapestInsertion(vehicleRoutes, preferredTimeWindowId, timeWindows, insertionCosts, request, before,
					after);

			// Reset boolean for next request
			this.insertionSuccessful = false;

		}

		// Transfer routing into final format

		/// Build overall routing object
		this.routing = new Routing();
		this.routing.setDepot(this.depot);
		this.routing.setDepotId(this.depot.getId());
		this.routing.setPossiblyFinalRouting(false);
		this.routing.setTimeWindowSet(this.timeWindowSet);
		this.routing.setTimeWindowSetId(this.timeWindowSet.getId());

		/// Define the routes (per vehicle)
		ArrayList<Route> finalRoutes = new ArrayList<Route>();
		for (int vehicleTypeNo = 0; vehicleTypeNo < vehicles.size(); vehicleTypeNo++) {
			for (int vehicleNo = 0; vehicleNo < ((Vehicle) vehicles.get(vehicleTypeNo)).getVehicleNo(); vehicleNo++) {

				Route route = new Route();

				//// Set vehicle type. Does not make sense here yet because no
				//// difference in types
				route.setVehicleType(((Vehicle) vehicles.get(vehicleTypeNo)).getVehicleType());
				route.setVehicleTypeId(((Vehicle) vehicles.get(vehicleTypeNo)).getVehicleTypeId());

				//// Go through respective route elements and add to route
				//// TODO: Assumption: VehicleRoute-ArrayLists are sorted
				//// regarding
				//// time window and route elements
				ArrayList<RouteElement> routeElements = new ArrayList<RouteElement>();
				int currentPosition = 1;
				for (int twID = 0; twID < vehicleRoutes.get(vehicleNo).size(); twID++) {
					for (int routeElementID = 0; routeElementID < vehicleRoutes.get(vehicleNo).get(twID)
							.size(); routeElementID++) {
						RouteElement element = vehicleRoutes.get(vehicleNo).get(twID).get(routeElementID);
						

						// Do not add the dummy elements for the depot. They had
						// a
						// position assigned.
						if (element.getPosition() == null) {
							element.setPosition(currentPosition++);
							routeElements.add(element);
						}

					}
				}
				route.setRouteElements(routeElements);
				finalRoutes.add(route);
			}

			/// Add routes to the overall routing
			this.routing.setRoutes(finalRoutes);
		}
	}

	public Routing getResult() {
		return routing;
	}

	public static String[] getParameterSetting() {

		return paras;
	}

	private ArrayList<InsertionCosts> computeAndSortInsertionCosts(int preferredTimeWindow,
			ArrayList<ArrayList<ArrayList<RouteElement>>> routes, ArrayList<TimeWindow> timeWindows,
			ForecastedOrderRequest forecastedOrderRequest, ArrayList<AcceptedCustomersOnRouteBefore> before,
			ArrayList<AcceptedCustomersOnRouteAfter> after, ArrayList<NodeDistance> distances) {

		ArrayList<InsertionCosts> insertionCosts = new ArrayList<InsertionCosts>();
		

		// compute for each route
		for (int r = 0; r < routes.size(); r++) {

			// insertion costs: no customer in preferred time window, costs for
			// insertion between customer before and after time window needed
			if (routes.get(r).get(preferredTimeWindow).isEmpty()) {
				computeInsertionCostsEmpty(r, routes, preferredTimeWindow, forecastedOrderRequest, distances, insertionCosts, before, after);

				// compute insertion costs for each possible insertion position
			} else {
				for (int c = 0; c < routes.get(r).get(preferredTimeWindow).size(); c++) {

					// insertion costs: customer is inserted at begin (first
					// customer) of preferred time window
					if (routes.get(r).get(preferredTimeWindow).get(c).getId() == routes.get(r).get(preferredTimeWindow).get(0).getId()) {
						computeInsertionCostsInsertionBegin(r, routes, preferredTimeWindow, forecastedOrderRequest, distances, insertionCosts, before, after);
					}

					// insertion costs: customer is inserted at end (last
					// customer) of preferred time window
					if (routes.get(r).get(preferredTimeWindow).get(c).getId() == routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size() - 1).getId()) {
						computeInsertionCostsInsertionEnd(r, routes, preferredTimeWindow, forecastedOrderRequest, distances, insertionCosts, before, after);

						// insertion costs: customer is inserted within (between
						// customers) of preferred time window
					} else {
						computeInsertionCostsInsertionWithin(r, routes, preferredTimeWindow, forecastedOrderRequest, distances, insertionCosts, c, before, after);
					}
				}
			}
		}
		return sortInsertionCosts(routes, preferredTimeWindow, timeWindows, insertionCosts, forecastedOrderRequest,
				before, after);
	}

	private void computeInsertionCostsEmpty(int r, ArrayList<ArrayList<ArrayList<RouteElement>>> routes,
			int preferredTimeWindow, ForecastedOrderRequest forecastedOrderRequest, ArrayList<NodeDistance> distances,
			ArrayList<InsertionCosts> insertionCosts, ArrayList<AcceptedCustomersOnRouteBefore> before,
			ArrayList<AcceptedCustomersOnRouteAfter> after) {
		

		double insertionCosts_i;
		InsertionCosts insertion = new InsertionCosts();

		double distanceIx; // distance customer before (i) and order request (x)
		double distanceXj; // distance order request (x) and customer after (j)
		double distanceIj; // distance customer before (i) and customer after (j)
		int directionIx ; //travel direction customer before (i) and order request (x)
		int directionXj; // travel direction order request (x) and customer after (j)
		int directionIj; // travel direction order request (i) and customer after (j)

	
		distanceIx = LocationService.getDistanceBetweenAreas(distances, routes.get(r).get(preferredTimeWindow - before.get(r).getBefore()).get(routes.get(r).get(preferredTimeWindow - before.get(r).getBefore()).size() - 1).getForecastedOrderRequest().getClosestNode(),
				forecastedOrderRequest.getClosestNode(), deliveryAreaSet, distanceAreas );
		
		distanceXj = LocationService.getDistanceBetweenAreas(distances, forecastedOrderRequest.getClosestNode(),
				routes.get(r).get(preferredTimeWindow + after.get(r).getAfter()).get(0).getForecastedOrderRequest()
						.getClosestNode(), deliveryAreaSet, distanceAreas );
		distanceIj = LocationService.getDistanceBetweenAreas(distances,
				routes.get(r).get(preferredTimeWindow - before.get(r).getBefore())
						.get(routes.get(r).get(preferredTimeWindow - before.get(r).getBefore()).size() - 1)
						.getForecastedOrderRequest().getClosestNode(),
				routes.get(r).get(preferredTimeWindow + after.get(r).getAfter()).get(0).getForecastedOrderRequest()
						.getClosestNode(), deliveryAreaSet, distanceAreas );
		
		directionIx = LocationService.getDirection( routes.get(r).get(preferredTimeWindow - before.get(r).getBefore()).get(routes.get(r).get(preferredTimeWindow - before.get(r).getBefore()).size() - 1).getForecastedOrderRequest().getClosestNode(),
				forecastedOrderRequest.getClosestNode(), deliveryAreaSet, centerArea );
		
		directionXj = LocationService.getDirection(forecastedOrderRequest.getClosestNode(),
				routes.get(r).get(preferredTimeWindow + after.get(r).getAfter()).get(0).getForecastedOrderRequest()
						.getClosestNode(), deliveryAreaSet, centerArea );
		directionIj = LocationService.getDirection(
				routes.get(r).get(preferredTimeWindow - before.get(r).getBefore())
						.get(routes.get(r).get(preferredTimeWindow - before.get(r).getBefore()).size() - 1)
						.getForecastedOrderRequest().getClosestNode(),
				routes.get(r).get(preferredTimeWindow + after.get(r).getAfter()).get(0).getForecastedOrderRequest()
						.getClosestNode(), deliveryAreaSet, centerArea );
		
		//adjust travel time with time dependent weighting
		distanceIx = distanceIx/timeDependentWeighting[directionIx][preferredTimeWindow];
		distanceXj = distanceXj/timeDependentWeighting[directionXj][preferredTimeWindow];
		distanceIj = distanceIj/timeDependentWeighting[directionIj][preferredTimeWindow];
		

		// insertion Costs: new distance - old distance + serviceTime
		insertionCosts_i = distanceIx + distanceXj - distanceIj + forecastedOrderRequest.getServiceTime();

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
	
	private void computeInsertionCostsInsertionWithin(int r, ArrayList<ArrayList<ArrayList<RouteElement>>> routes,
			int preferredTimeWindow, ForecastedOrderRequest forecastedOrderRequest, ArrayList<NodeDistance> distances,
			ArrayList<InsertionCosts> insertionCosts, int c, ArrayList<AcceptedCustomersOnRouteBefore> before,
			ArrayList<AcceptedCustomersOnRouteAfter> after) {
		double insertionCosts_i;
		InsertionCosts insertion = new InsertionCosts();

		double distanceIx; // distance customer before (i) and order request (x)
		double distanceXj; // distance order request (x) and customer after (j)
		double distanceIj; // distance customer before (i) and customer after (j)
		int directionIx ; //travel direction customer before (i) and order request (x)
		int directionXj; // travel direction order request (x) and customer after (j)
		int directionIj; // travel direction order request (i) and customer after (j)

		
		if (c < routes.get(r).get(preferredTimeWindow).size() - 1) {
			
			distanceIx = LocationService.getDistanceBetweenAreas(distances,
					routes.get(r).get(preferredTimeWindow).get(c).getForecastedOrderRequest().getClosestNode(),
					forecastedOrderRequest.getClosestNode(), deliveryAreaSet, distanceAreas );
			distanceXj = LocationService.getDistanceBetweenAreas(distances, forecastedOrderRequest.getClosestNode(),
					routes.get(r).get(preferredTimeWindow).get(c + 1).getForecastedOrderRequest().getClosestNode(), deliveryAreaSet, distanceAreas );
			distanceIj = LocationService.getDistanceBetweenAreas(distances,
					routes.get(r).get(preferredTimeWindow).get(c).getForecastedOrderRequest().getClosestNode(),
					routes.get(r).get(preferredTimeWindow).get(c + 1).getForecastedOrderRequest().getClosestNode(), deliveryAreaSet, distanceAreas );
			
			directionIx = LocationService.getDirection(
					routes.get(r).get(preferredTimeWindow).get(c).getForecastedOrderRequest().getClosestNode(),
					forecastedOrderRequest.getClosestNode(), deliveryAreaSet, centerArea );
			directionXj = LocationService.getDirection( forecastedOrderRequest.getClosestNode(),
					routes.get(r).get(preferredTimeWindow).get(c + 1).getForecastedOrderRequest().getClosestNode(), deliveryAreaSet, centerArea );
			directionIj = LocationService.getDirection(
					routes.get(r).get(preferredTimeWindow).get(c).getForecastedOrderRequest().getClosestNode(),
					routes.get(r).get(preferredTimeWindow).get(c + 1).getForecastedOrderRequest().getClosestNode(), deliveryAreaSet, centerArea );
			
			


			//adjust travel time with time dependent weighting
			distanceIx = distanceIx/timeDependentWeighting[directionIx][preferredTimeWindow];
			distanceXj = distanceXj/timeDependentWeighting[directionXj][preferredTimeWindow];
			distanceIj = distanceIj/timeDependentWeighting[directionIj][preferredTimeWindow];
			

			// insertion Costs: new distance - old distance + serviceTime
			insertionCosts_i = distanceIx + distanceXj - distanceIj + forecastedOrderRequest.getServiceTime();
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

	private void computeInsertionCostsInsertionEnd(int r, ArrayList<ArrayList<ArrayList<RouteElement>>> routes,
			int preferredTimeWindow, ForecastedOrderRequest forecastedOrderRequest, ArrayList<NodeDistance> distances,
			ArrayList<InsertionCosts> insertionCosts, ArrayList<AcceptedCustomersOnRouteBefore> before,
			ArrayList<AcceptedCustomersOnRouteAfter> after) {
		double insertionCosts_i;
		InsertionCosts insertion = new InsertionCosts();

		double distanceIx; // distance customer before (i) and order request (x)
		double distanceXj; // distance order request (x) and customer after (j)
		double distanceIj; // distance customer before (i) and customer after (j)
		int directionIx ; //travel direction customer before (i) and order request (x)
		int directionXj; // travel direction order request (x) and customer after (j)
		int directionIj; // travel direction order request (i) and customer after (j)	


		distanceIx = LocationService.getDistanceBetweenAreas(distances, routes.get(r).get(preferredTimeWindow)
				.get(routes.get(r).get(preferredTimeWindow).size() - 1).getForecastedOrderRequest().getClosestNode(),
				forecastedOrderRequest.getClosestNode(), deliveryAreaSet, distanceAreas );		
		distanceXj = LocationService.getDistanceBetweenAreas(distances, forecastedOrderRequest.getClosestNode(),
				routes.get(r).get(preferredTimeWindow + after.get(r).getAfter()).get(0).getForecastedOrderRequest()
						.getClosestNode(), deliveryAreaSet, distanceAreas );
		distanceIj = LocationService.getDistanceBetweenAreas(distances,
				routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size() - 1)
						.getForecastedOrderRequest().getClosestNode(),
				routes.get(r).get(preferredTimeWindow + after.get(r).getAfter()).get(0).getForecastedOrderRequest()
						.getClosestNode(), deliveryAreaSet, distanceAreas );
		
		
		directionIx = LocationService.getDirection( routes.get(r).get(preferredTimeWindow)
				.get(routes.get(r).get(preferredTimeWindow).size() - 1).getForecastedOrderRequest().getClosestNode(),
				forecastedOrderRequest.getClosestNode(), deliveryAreaSet, centerArea );	
		directionXj = LocationService.getDirection( forecastedOrderRequest.getClosestNode(),
				routes.get(r).get(preferredTimeWindow + after.get(r).getAfter()).get(0).getForecastedOrderRequest()
						.getClosestNode(), deliveryAreaSet, centerArea );	
		directionIj = LocationService.getDirection(
				routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size() - 1)
						.getForecastedOrderRequest().getClosestNode(),
				routes.get(r).get(preferredTimeWindow + after.get(r).getAfter()).get(0).getForecastedOrderRequest()
						.getClosestNode(), deliveryAreaSet, centerArea );	

		
		//adjust travel time with time dependent weighting
		distanceIx = distanceIx/timeDependentWeighting[directionIx][preferredTimeWindow];
		distanceXj = distanceXj/timeDependentWeighting[directionXj][preferredTimeWindow];
		distanceIj = distanceIj/timeDependentWeighting[directionIj][preferredTimeWindow];
		
		
		// insertion Costs: new distance - old distance + serviceTime
		insertionCosts_i = distanceIx + distanceXj - distanceIj + forecastedOrderRequest.getServiceTime();

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
	//	if (routes.get(r).size() - 1 != preferredTimeWindow && insertion.getInsertAfterId() != routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size() - 1).getId()) {
			insertionCosts.add(insertion);
		//}
	}


	private void computeInsertionCostsInsertionBegin(int r, ArrayList<ArrayList<ArrayList<RouteElement>>> routes,
			int preferredTimeWindow, ForecastedOrderRequest forecastedOrderRequest, ArrayList<NodeDistance> distances,
			ArrayList<InsertionCosts> insertionCosts, ArrayList<AcceptedCustomersOnRouteBefore> before,
			ArrayList<AcceptedCustomersOnRouteAfter> after) {
		double insertionCosts_i;
		InsertionCosts insertion = new InsertionCosts();

		double distanceIx; // distance customer before (i) and order request (x)
		double distanceXj; // distance order request (x) and customer after (j)
		double distanceIj; // distance customer before (i) and customer after
							// (j)
		
		
		int directionIx ; //travel direction customer before (i) and order request (x)
		int directionXj; // travel direction order request (x) and customer after (j)
		int directionIj; // travel direction order request (i) and customer after (j)	
		
		
		
		distanceIx = LocationService.getDistanceBetweenAreas(distances,
				routes.get(r).get(preferredTimeWindow - before.get(r).getBefore())
						.get(routes.get(r).get(preferredTimeWindow - before.get(r).getBefore()).size() - 1)
						.getForecastedOrderRequest().getClosestNode(),
				forecastedOrderRequest.getClosestNode(), deliveryAreaSet, distanceAreas );	
		
		distanceXj = LocationService.getDistanceBetweenAreas(distances, forecastedOrderRequest.getClosestNode(),
				routes.get(r).get(preferredTimeWindow).get(0).getForecastedOrderRequest().getClosestNode(), deliveryAreaSet, distanceAreas );	
		distanceIj = LocationService.getDistanceBetweenAreas(distances,
				routes.get(r).get(preferredTimeWindow - before.get(r).getBefore())
						.get(routes.get(r).get(preferredTimeWindow - before.get(r).getBefore()).size() - 1)
						.getForecastedOrderRequest().getClosestNode(),
				routes.get(r).get(preferredTimeWindow).get(0).getForecastedOrderRequest().getClosestNode(), deliveryAreaSet, distanceAreas );	
		
		
		directionIx = LocationService.getDirection(
				routes.get(r).get(preferredTimeWindow - before.get(r).getBefore())
						.get(routes.get(r).get(preferredTimeWindow - before.get(r).getBefore()).size() - 1)
						.getForecastedOrderRequest().getClosestNode(),
				forecastedOrderRequest.getClosestNode(), deliveryAreaSet, centerArea );	
		
		directionXj = LocationService.getDirection(forecastedOrderRequest.getClosestNode(),
				routes.get(r).get(preferredTimeWindow).get(0).getForecastedOrderRequest().getClosestNode(), deliveryAreaSet, centerArea );	
		directionIj = LocationService.getDirection(
				routes.get(r).get(preferredTimeWindow - before.get(r).getBefore())
						.get(routes.get(r).get(preferredTimeWindow - before.get(r).getBefore()).size() - 1)
						.getForecastedOrderRequest().getClosestNode(),
				routes.get(r).get(preferredTimeWindow).get(0).getForecastedOrderRequest().getClosestNode(), deliveryAreaSet, centerArea );	
		
		//adjust travel time with time dependent weighting
		distanceIx = distanceIx/timeDependentWeighting[directionIx][preferredTimeWindow];
		distanceXj = distanceXj/timeDependentWeighting[directionXj][preferredTimeWindow];
		distanceIj = distanceIj/timeDependentWeighting[directionIj][preferredTimeWindow];
		

		// insertion Costs: new distance - old distance + serviceTime
		insertionCosts_i = distanceIx + distanceXj - distanceIj + forecastedOrderRequest.getServiceTime();

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
			ForecastedOrderRequest forecastedOrderRequest, ArrayList<AcceptedCustomersOnRouteBefore> before,
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
//			for (InsertionCosts i : insertionCosts) {
//				//System.out.println("RequestID: " + forecastedOrderRequest.getId() + "  Costs: " + i.getInsertionCosts() + "    Route: " + i.getRoute() + "     ID: " + i.getInsertAfterId());
//			}

		}
		return insertionCosts;

	}

	
	

	private void tryCheapestInsertion(ArrayList<ArrayList<ArrayList<RouteElement>>> routes, int preferredTimeWindow,
			ArrayList<TimeWindow> timeWindow, ArrayList<InsertionCosts> insertionCosts,
			ForecastedOrderRequest forecastedOrderRequest, ArrayList<AcceptedCustomersOnRouteBefore> before,
			ArrayList<AcceptedCustomersOnRouteAfter> after) {

		// while order request could not be inserted
		while (insertionSuccessful == false) {

			if (insertionCosts.size() > 0) {
				// tryInsertion for first element (0) of insertion costs (=
				// cheapest insertion costs)
				tryInsertion(insertionCosts.get(0).getRoute(), routes, preferredTimeWindow,
						insertionCosts.get(0).getInsertAfterId(), timeWindow, insertionCosts, forecastedOrderRequest,
						before.get(insertionCosts.get(0).getRoute()).getBefore(),
						after.get(insertionCosts.get(0).getRoute()).getAfter());

				// remove cheapest insertion costs, if insertion was not
				// successful insertion is tried for next cheapest costs
				insertionCosts.remove(0);
			} else {
				if (test == true) {
					System.out.println("Insertion for forecasted order request id " + forecastedOrderRequest.getId()
							+ " not possible. ");
				}
				break;
			}
		}
	}

	
	private void tryInsertion(int r, ArrayList<ArrayList<ArrayList<RouteElement>>> routes, int preferredTimeWindow,
			int insertionPosition, ArrayList<TimeWindow> timeWindow, ArrayList<InsertionCosts> insertionCosts,
			ForecastedOrderRequest forecastedOrderRequest, int before, int after) {

		int checkFromHere;
		checkFromHere = insertionCosts.get(0).getCheckFromHere();


		if (checkFromHere == 0) {

			// checkFromHere==0 & preferred time window empty: customer is
			// inserted in empty time window
			if (routes.get(r).get(preferredTimeWindow).isEmpty()) {
				if (insertionSuccessful == true) {
				} else {
					insertCustomerEmptyTimeWindow(r, routes, timeWindow, preferredTimeWindow, insertionCosts,
							forecastedOrderRequest, before, after);
				}
			}
			

			
			if (routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size() - 1)
					.getId() == insertionCosts.get(0).getInsertAfterId()) {
				if (routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size() - 1)
						.getServiceBegin()
						+ routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size() - 1)
								.getServiceTime()
						+ insertionCosts.get(0).getDistanceIx() <= timeWindow.get(preferredTimeWindow-1).getEndTime()*60
						&& routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size() - 1)
								.getServiceBegin()
								+ routes.get(r).get(preferredTimeWindow)
										.get(routes.get(r).get(preferredTimeWindow).size() - 1).getServiceTime()
								+ insertionCosts.get(0).getDistanceIx() + forecastedOrderRequest.getServiceTime()
								+ insertionCosts.get(0).getDistanceXj() <= routes.get(r)
										.get(preferredTimeWindow + after).get(checkFromHere).getServiceBegin()) {

					if (insertionSuccessful == true) {
					} else {
						if (insertionSuccessful == false) {

							// checkFromHere==0 & customer is inserted at end of
							// preferred time window, service begin of next
							// customer is not affected
							insertCustomerEndTimeWindowNoChanges(r, routes, preferredTimeWindow, timeWindow,
									insertionCosts, forecastedOrderRequest, after);
						}
					}
				} else {
					if (insertionSuccessful == false) {

						// checkFromHere==0 & customer is inserted at end of
						// preferred time window, service begin of next customer
						// is affected
						insertCustomerEndTimeWindowChanges(r, routes, preferredTimeWindow, timeWindow, insertionCosts,
								forecastedOrderRequest, after);
					}
				}
			} else {
				if (insertionSuccessful == false) {

					// checkFromHere==0 & customer is inserted at begin of
					// preferred time window
					
					insertCustomerBeginTimeWindow(r, routes, preferredTimeWindow, timeWindow, insertionCosts,
							forecastedOrderRequest, before, after, checkFromHere);
				}
			}
		} else {
			if (insertionSuccessful == false) {

				// checkFromHere!=0 & customer is inserted within preferred time
				// window
				insertCustomerWithinTimeWindow(r, routes, preferredTimeWindow, timeWindow, insertionCosts,
						forecastedOrderRequest, checkFromHere);
			}
		}
	}

	
	public void insertCustomerEmptyTimeWindow(int r, ArrayList<ArrayList<ArrayList<RouteElement>>> routes,
			ArrayList<TimeWindow> timeWindows, int preferredTimeWindow, ArrayList<InsertionCosts> insertionCosts,
			ForecastedOrderRequest forecastedOrderRequest, int before, int after) {


		RouteElement element = new RouteElement();
		


		element.setForecastedOrderRequest(forecastedOrderRequest);
		element.setId(forecastedOrderRequest.getId());
		
		element.setDeliveryArea(forecastedOrderRequest.getDeliveryArea());
		element.setDeliveryAreaId(forecastedOrderRequest.getDeliveryAreaId());
		element.setServiceTime(forecastedOrderRequest.getServiceTime());

		// request service begin = start of preferred time window (service
		// begin, service time & travel time to route element < begin of
		// preferred time window)
		if (routes.get(r).get(preferredTimeWindow - before)
				.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceBegin()
				+ routes.get(r).get(preferredTimeWindow - before)
						.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceTime()
				+ insertionCosts.get(0).getDistanceIx() <= timeWindows.get(preferredTimeWindow-1).getStartTime()*60) {
			element.setServiceBegin(timeWindows.get(preferredTimeWindow-1).getStartTime()*60);

			// request service begin > start of preferred time window (service
			// begin, service time & travel time to route element > begin of
			// preferred time window)
		} else {
			element.setServiceBegin(routes.get(r).get(preferredTimeWindow - before)
					.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceBegin()
					+ routes.get(r).get(preferredTimeWindow - before)
							.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceTime()
					+  insertionCosts.get(0).getDistanceIx());
		}

		// request waiting time: 0 if service begin > start preferred time
		// window OR compute waiting time
		element.setWaitingTime( Math.max(0,
				timeWindows.get(preferredTimeWindow-1).getStartTime()*60
						- routes.get(r).get(preferredTimeWindow - before)
								.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceBegin()
						- routes.get(r).get(preferredTimeWindow - before)
								.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceTime()
						- insertionCosts.get(0).getDistanceIx()));
		

		element.setTravelTime( insertionCosts.get(0).getDistanceIx());
		element.setTimeWindow(timeWindows.get(preferredTimeWindow-1));
		element.setTimeWindowId(timeWindows.get(preferredTimeWindow-1).getId());
		element.setSlack(Math.max(0, timeWindows.get(preferredTimeWindow-1).getEndTime()*60 - element.getServiceBegin()));

		// changes in route

		routes.get(r).get(preferredTimeWindow + after).get(0).setWaitingTime(
				(routes.get(r).get(preferredTimeWindow + after).get(0).getTimeWindow().getStartTime()*60
						- element.getServiceBegin() - element.getServiceTime()
						- insertionCosts.get(0).getDistanceXj()));
		
		routes.get(r).get(preferredTimeWindow + after).get(0)
				.setTravelTime( insertionCosts.get(0).getDistanceXj());

		// insertion	
		routes.get(r).get(preferredTimeWindow).add(element);

		routes.get(r).get(preferredTimeWindow + after).get(0)
				.setTravelTime(insertionCosts.get(0).getDistanceXj());
		insertionSuccessful = true;

		
		if (test == true) {
				
				for (int k = 0; k <= routes.get(0).size()-1; k++) {
					for (int i = 0; i <= routes.get(0).get(k).size()-1; i++) {
					
						System.out.println(routes.get(0).get(k).get(i).getId() + 
								"       TravelTime: " + routes.get(0).get(k).get(i).getTravelTime() + 
								"       B: " + routes.get(0).get(k).get(i).getServiceBegin() +
								"       E: " + k +
								"       S: " + routes.get(0).get(k).get(i).getSlack() + 
								"       W: " + routes.get(0).get(k).get(i).getWaitingTime());
								
					}
				}
				System.out.println("\n");
		}
		
		
	

	}

	
	private void insertCustomerWithinTimeWindow(int r, ArrayList<ArrayList<ArrayList<RouteElement>>> routes,
			int preferredTimeWindow, ArrayList<TimeWindow> timeWindows, ArrayList<InsertionCosts> insertionCosts,
			ForecastedOrderRequest forecastedOrderRequest, int checkFromHere) {



		// insertionCosts
		double insertionCosts_Waiting = 0;
		insertionCosts_Waiting = insertionCosts.get(0).getInsertionCosts();

		// check if insertion is possible
		int check = 0;
		int setCheckFromHereForLoop = 0;

		for (int j = preferredTimeWindow; j < routes.get(r).size(); j++) {
			if (j == preferredTimeWindow) {
				setCheckFromHereForLoop = checkFromHere;
			}else{
				setCheckFromHereForLoop = 0;
			}
			for (int i = setCheckFromHereForLoop; i < routes.get(r).get(j).size(); i++) {
				
				if (insertionCosts_Waiting > 0) {
					if (routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size() - 1)
							.getServiceBegin()
							+ routes.get(r).get(preferredTimeWindow)
									.get(routes.get(r).get(preferredTimeWindow).size() - 1).getServiceTime()
							+ insertionCosts.get(0).getDistanceIx() <= timeWindows.get(preferredTimeWindow-1).getEndTime()*60
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
				}else{
					setCheckFromHereForLoop = 0;
				}
				for (int i = setCheckFromHereForLoop; i < routes.get(r).get(j).size(); i++) {
					if (routes.get(r).get(j).get(0).getWaitingTime() >= insertionCosts_Waiting
							&& j > preferredTimeWindow) {
						routes.get(r).get(j).get(0)
								.setWaitingTime(( routes.get(r).get(j).get(0).getWaitingTime()
										- insertionCosts_Waiting));
						insertionCosts_Waiting = 0;
					}
					if (routes.get(r).get(j).get(0).getWaitingTime() < insertionCosts_Waiting
							&& j > preferredTimeWindow) {
						insertionCosts_Waiting = Math.max(0,
								insertionCosts_Waiting - routes.get(r).get(j).get(0).getWaitingTime());
						routes.get(r).get(j).get(0).setWaitingTime( 0.0);
					}
					
					
					routes.get(r).get(j).get(i).setServiceBegin(
							(routes.get(r).get(j).get(i).getServiceBegin() + insertionCosts_Waiting));
					routes.get(r).get(j).get(i).setSlack(Math.max(0, routes.get(r).get(j).get(i).getTimeWindow().getEndTime()*60
							- routes.get(r).get(j).get(i).getServiceBegin()));
					routes.get(r).get(preferredTimeWindow).get(checkFromHere).setWaitingTime(0.0);
				}
			}

			RouteElement element = new RouteElement();
			element.setForecastedOrderRequest(forecastedOrderRequest);
			element.setId(forecastedOrderRequest.getId());
			element.setDeliveryArea(forecastedOrderRequest.getDeliveryArea());
			element.setDeliveryAreaId(forecastedOrderRequest.getDeliveryAreaId());
			element.setServiceTime(forecastedOrderRequest.getServiceTime());

			// insertion
			routes.get(r).get(preferredTimeWindow).get(checkFromHere)
					.setTravelTime( insertionCosts.get(0).getDistanceXj());
			routes.get(r).get(preferredTimeWindow).add(checkFromHere, element);

			element.setServiceBegin(
					 (routes.get(r).get(preferredTimeWindow).get(checkFromHere - 1).getServiceBegin()
							+ routes.get(r).get(preferredTimeWindow).get(checkFromHere - 1).getServiceTime()
							+ insertionCosts.get(0).getDistanceIx()));
			element.setTravelTime(insertionCosts.get(0).getDistanceIx());
			element.setWaitingTime( 0.0);
			element.setTimeWindow(timeWindows.get(preferredTimeWindow-1));
			element.setTimeWindowId(timeWindows.get(preferredTimeWindow-1).getId());
			element.setSlack(Math.max(0, timeWindows.get(preferredTimeWindow-1).getEndTime()*60 - element.getServiceBegin()));

			insertionSuccessful = true;
			
		if (test == true) {
				
				for (int k = 0; k <= routes.get(0).size()-1; k++) {
					for (int i = 0; i <= routes.get(0).get(k).size()-1; i++) {
						System.out.println(routes.get(0).get(k).get(i).getId() + 
								"       TravelTime: " + routes.get(0).get(k).get(i).getTravelTime() + 
								"       B: " + routes.get(0).get(k).get(i).getServiceBegin() +
								"       E: " + k +
								"       S: " + routes.get(0).get(k).get(i).getSlack() + 
								"       W: " + routes.get(0).get(k).get(i).getWaitingTime());
					}
				}
				System.out.println("\n");
		}

		}
	}

	private void insertCustomerBeginTimeWindow(int r, ArrayList<ArrayList<ArrayList<RouteElement>>> routes,
			int preferredTimeWindow, ArrayList<TimeWindow> timeWindows, ArrayList<InsertionCosts> insertionCosts,
			ForecastedOrderRequest forecastedOrderRequest, int before, int after, int checkFromHere) {
		


		// request service begin = start of preferred time window (service
		// begin, service time & travel time to route element < begin of
		// preferred time window)
		if (routes.get(r).get(preferredTimeWindow - before)
				.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceBegin()
				+ routes.get(r).get(preferredTimeWindow - before)
						.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceTime()
				+ insertionCosts.get(0).getDistanceIx() < timeWindows.get(preferredTimeWindow-1).getStartTime()*60) {
			
			insertCustomerBeginTimeWindowStartE(r, routes, preferredTimeWindow, timeWindows, insertionCosts,
					forecastedOrderRequest, before, after, checkFromHere);

			
			
			// request service begin > start of preferred time window (service
			// begin, service time & travel time to route element > begin of
			// preferred time window)
		} else {
			insertCustomerBeginTimeWindowStartAfterE(r, routes, preferredTimeWindow, timeWindows, insertionCosts,
					forecastedOrderRequest, before, checkFromHere);
		}
	}

	private void insertCustomerBeginTimeWindowStartAfterE(int r, ArrayList<ArrayList<ArrayList<RouteElement>>> routes,
			int preferredTimeWindow, ArrayList<TimeWindow> timeWindows, ArrayList<InsertionCosts> insertionCosts,
			ForecastedOrderRequest forecastedOrderRequest, int before, int checkFromHere) {


		double insertionCosts_Waiting;
		int check = 0;
		int setCheckFromHereForLoop = 0;

		// reduction of insertionCosts when waiting time buffers exist
		insertionCosts_Waiting = routes.get(r).get(preferredTimeWindow - before)
				.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceBegin()
				+ routes.get(r).get(preferredTimeWindow - before)
						.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceTime()
				+ insertionCosts.get(0).getDistanceIx() - timeWindows.get(preferredTimeWindow-1).getStartTime()*60
				+ forecastedOrderRequest.getServiceTime() + insertionCosts.get(0).getDistanceXj();

		// check if insertion is possible
		for (int j = preferredTimeWindow; j < routes.get(r).size(); j++) {

			// changes in same time window only for elements after insertion
			if (j == preferredTimeWindow) {
				setCheckFromHereForLoop = checkFromHere;
			}else{
				setCheckFromHereForLoop = 0;
			}

			for (int i = setCheckFromHereForLoop; i < routes.get(r).get(j).size(); i++) {
				if (insertionCosts_Waiting > 0) {
					if (routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size() - 1)
							.getServiceBegin()
							+ routes.get(r).get(preferredTimeWindow)
									.get(routes.get(r).get(preferredTimeWindow).size() - 1).getServiceTime()
							+ insertionCosts.get(0).getDistanceIx() <= timeWindows.get(preferredTimeWindow-1).getEndTime()*60
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
			element.setForecastedOrderRequest(forecastedOrderRequest);
			element.setId(forecastedOrderRequest.getId());
			element.setDeliveryArea(forecastedOrderRequest.getDeliveryArea());
			element.setDeliveryAreaId(forecastedOrderRequest.getDeliveryAreaId());
			element.setServiceTime(forecastedOrderRequest.getServiceTime());

			element.setServiceBegin((routes.get(r).get(preferredTimeWindow - before)
					.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceBegin()
					+ routes.get(r).get(preferredTimeWindow - before)
							.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceTime()
					+ insertionCosts.get(0).getDistanceIx()));

			if (routes.get(r).get(preferredTimeWindow).get(0).getServiceBegin() != routes.get(r)
					.get(preferredTimeWindow).get(0).getTimeWindow().getStartTime()*60) {
				insertionCosts_Waiting = Math.max(0,
						routes.get(r).get(preferredTimeWindow - before)
								.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceBegin()
								+ routes.get(r).get(preferredTimeWindow - before)
										.get(routes.get(r).get(preferredTimeWindow - before).size() - 1)
										.getServiceTime()
								+ insertionCosts.get(0).getDistanceIx()
								- timeWindows.get(preferredTimeWindow-1).getStartTime()*60
								+ forecastedOrderRequest.getServiceTime() + insertionCosts.get(0).getDistanceXj()
								- routes.get(r).get(preferredTimeWindow).get(0).getServiceBegin()
								+ routes.get(r).get(preferredTimeWindow).get(0).getTimeWindow().getStartTime()*60);
			} else {
				insertionCosts_Waiting = Math.max(0,
						routes.get(r).get(preferredTimeWindow - before)
								.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceBegin()
								+ routes.get(r).get(preferredTimeWindow - before)
										.get(routes.get(r).get(preferredTimeWindow - before).size() - 1)
										.getServiceTime()
								+ insertionCosts.get(0).getDistanceIx()
								- timeWindows.get(preferredTimeWindow-1).getStartTime()*60
								+ forecastedOrderRequest.getServiceTime() + insertionCosts.get(0).getDistanceXj());
			}

			for (int j = preferredTimeWindow; j < routes.get(r).size(); j++) {
				if (j == preferredTimeWindow) {
					setCheckFromHereForLoop = checkFromHere;
				}else{
					setCheckFromHereForLoop = 0;
				}
				for (int i = setCheckFromHereForLoop; i < routes.get(r).get(j).size(); i++) {
					if (j > preferredTimeWindow) {
						
						
						

						
						
						
						
						
						
						
						insertionCosts_Waiting = Math.max(0,
								insertionCosts_Waiting - routes.get(r).get(j).get(i).getWaitingTime());
					}
					routes.get(r).get(j).get(i).setServiceBegin(
							 (routes.get(r).get(j).get(i).getServiceBegin() + insertionCosts_Waiting));
					routes.get(r).get(j).get(i).setSlack(Math.max(0f, routes.get(r).get(j).get(i).getTimeWindow().getEndTime()*60
							- routes.get(r).get(j).get(i).getServiceBegin()));

	
					double w = 100;
					w = insertionCosts_Waiting;
					
					if (i == 0 && j > preferredTimeWindow) {

						routes.get(r).get(j).get(i)
								.setWaitingTime( Math.max(0, routes.get(r).get(j).get(0).getWaitingTime() - w));
					}
					
					routes.get(r).get(preferredTimeWindow).get(0).setWaitingTime(0.0);

				}
			}

			element.setTravelTime( insertionCosts.get(0).getDistanceIx());
			element.setWaitingTime( 0.0);
			element.setTimeWindow(timeWindows.get(preferredTimeWindow-1));
			element.setTimeWindowId(timeWindows.get(preferredTimeWindow-1).getId());
			element.setSlack(Math.max(0f, timeWindows.get(preferredTimeWindow-1).getEndTime()*60 - element.getServiceBegin()));

			// insertion
			routes.get(r).get(preferredTimeWindow).add(0, element);
			insertionSuccessful = true;

			routes.get(r).get(preferredTimeWindow).get(1).setTravelTime( insertionCosts.get(0).getDistanceXj());


			
		if (test == true) {
				
				for (int k = 0; k <= routes.get(0).size()-1; k++) {
					for (int i = 0; i <= routes.get(0).get(k).size()-1; i++) {
						System.out.println(routes.get(0).get(k).get(i).getId() + 
								"       TravelTime: " + routes.get(0).get(k).get(i).getTravelTime() + 
								"       B: " + routes.get(0).get(k).get(i).getServiceBegin() +
								"       E: " + k +
								"       S: " + routes.get(0).get(k).get(i).getSlack() + 
								"       W: " + routes.get(0).get(k).get(i).getWaitingTime());
					}
				}
				System.out.println("\n");
		}
		}
	}

	private void insertCustomerBeginTimeWindowStartE(int r, ArrayList<ArrayList<ArrayList<RouteElement>>> routes,
			int preferredTimeWindow, ArrayList<TimeWindow> timeWindows, ArrayList<InsertionCosts> insertionCosts,
			ForecastedOrderRequest forecastedOrderRequest, int before, int after, int checkFromHere) {



		
		
		// insertionCosts
		double insertionCosts_Waiting;

		// if service begin = begin of time window: insertionCosts_Waiting xj +
		// serviceTime
		if (routes.get(r).get(preferredTimeWindow).get(0).getServiceBegin() == routes.get(r).get(preferredTimeWindow)
				.get(0).getTimeWindow().getStartTime()*60) {
			insertionCosts_Waiting = insertionCosts.get(0).getDistanceXj() + forecastedOrderRequest.getServiceTime();

			// if service begin > begin of time window: insertionCosts_Waiting
			// xj + serviceTime + service begin - time window begin
		} else {
			insertionCosts_Waiting = insertionCosts.get(0).getDistanceXj() + forecastedOrderRequest.getServiceTime()
					- routes.get(r).get(preferredTimeWindow).get(0).getServiceBegin()
					+ routes.get(r).get(preferredTimeWindow).get(0).getTimeWindow().getStartTime()*60;
		}

		// check
		int check = 0;
		int setCheckFromHereForLoop = 0;

		for (int j = preferredTimeWindow; j < routes.get(r).size(); j++) {

			if (j == preferredTimeWindow) {
				setCheckFromHereForLoop = checkFromHere;
			}else{
				setCheckFromHereForLoop = 0;
			}

			for (int i = setCheckFromHereForLoop; i < routes.get(r).get(j).size(); i++) {
				if (insertionCosts_Waiting > 0) {

						if (routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size() - 1)
							.getServiceBegin()
							+ routes.get(r).get(preferredTimeWindow)
									.get(routes.get(r).get(preferredTimeWindow).size() - 1).getServiceTime()
							+ insertionCosts.get(0).getDistanceIx() <= timeWindows.get(preferredTimeWindow-1).getEndTime()*60
							&& insertionCosts_Waiting <= routes.get(r).get(j).get(i).getSlack()
									+ routes.get(r).get(j).get(i).getWaitingTime()
									) {
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
			element.setForecastedOrderRequest(forecastedOrderRequest);
			element.setId(forecastedOrderRequest.getId());
			element.setDeliveryArea(forecastedOrderRequest.getDeliveryArea());
			element.setDeliveryAreaId(forecastedOrderRequest.getDeliveryAreaId());
			element.setServiceBegin(timeWindows.get(preferredTimeWindow-1).getStartTime()*60);
			element.setServiceTime(forecastedOrderRequest.getServiceTime());

			setCheckFromHereForLoop = 0;

			if (routes.get(r).get(preferredTimeWindow).get(0).getServiceBegin() == routes.get(r)
					.get(preferredTimeWindow).get(0).getTimeWindow().getStartTime()*60) {
				insertionCosts_Waiting = insertionCosts.get(0).getDistanceXj()
						+ forecastedOrderRequest.getServiceTime();
			} else {
				insertionCosts_Waiting = insertionCosts.get(0).getDistanceXj() + forecastedOrderRequest.getServiceTime()
						- routes.get(r).get(preferredTimeWindow).get(0).getServiceBegin()
						+ routes.get(r).get(preferredTimeWindow).get(0).getTimeWindow().getStartTime()*60;
			}

			for (int j = preferredTimeWindow; j < routes.get(r).size(); j++) {

				if (j == preferredTimeWindow) {
					setCheckFromHereForLoop = checkFromHere;
				}else{
					setCheckFromHereForLoop = 0;
				}

				for (int i = setCheckFromHereForLoop; i < routes.get(r).get(j).size(); i++) {

					if (insertionCosts_Waiting > 0) {
						routes.get(r).get(preferredTimeWindow).get(0).setWaitingTime( 0.0);

						double w = insertionCosts_Waiting;

						if (j > preferredTimeWindow) {
							insertionCosts_Waiting = Math.max(0,
									insertionCosts_Waiting - routes.get(r).get(j).get(i).getWaitingTime());
						}
						if (i == 0 && j > preferredTimeWindow) {
							routes.get(r).get(j).get(i).setWaitingTime(
									 Math.max(0, routes.get(r).get(j).get(0).getWaitingTime() - w));
						}
						routes.get(r).get(j).get(i).setServiceBegin(
								 (routes.get(r).get(j).get(i).getServiceBegin() + insertionCosts_Waiting));
						routes.get(r).get(j).get(i).setSlack(Math.max(0, routes.get(r).get(j).get(i).getTimeWindow().getEndTime()*60
								- routes.get(r).get(j).get(i).getServiceBegin()));

					} else {
						break;
					}
				}
			}
			element.setTravelTime( insertionCosts.get(0).getDistanceIx());
			element.setWaitingTime( (timeWindows.get(preferredTimeWindow-1).getStartTime()*60
					- routes.get(r).get(preferredTimeWindow - before)
							.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceBegin()
					- routes.get(r).get(preferredTimeWindow - before)
							.get(routes.get(r).get(preferredTimeWindow - before).size() - 1).getServiceTime()
					- insertionCosts.get(0).getDistanceIx()));
			element.setTimeWindow(timeWindows.get(preferredTimeWindow-1));
			element.setTimeWindowId(timeWindows.get(preferredTimeWindow-1).getId());
			element.setSlack(Math.max(0, timeWindows.get(preferredTimeWindow-1).getEndTime()*60 - element.getServiceBegin()));

			// insertion
			routes.get(r).get(preferredTimeWindow).add(0, element);
			insertionSuccessful = true;

			routes.get(r).get(preferredTimeWindow).get(1).setTravelTime( insertionCosts.get(0).getDistanceXj());

			if (test == true) {

				for (int k = 0; k <= routes.get(0).size() - 1; k++) {
					for (int i = 0; i <= routes.get(0).get(k).size() - 1; i++) {
						System.out.println(routes.get(0).get(k).get(i).getId() + "       TravelTime: " + routes.get(0).get(k).get(i).getTravelTime() + "       B: "
								+ routes.get(0).get(k).get(i).getServiceBegin() + "       E: " + k + "       S: " + routes.get(0).get(k).get(i).getSlack() + "       W: " + routes.get(0).get(k).get(i).getWaitingTime());
					}
				}
				System.out.println("\n");
			}
		}
	}

	private void insertCustomerEndTimeWindowChanges(int r, ArrayList<ArrayList<ArrayList<RouteElement>>> routes,
			int preferredTimeWindow, ArrayList<TimeWindow> timeWindows, ArrayList<InsertionCosts> insertionCosts,
			ForecastedOrderRequest forecastedOrderRequest, int after) {



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
							+ insertionCosts.get(0).getDistanceIx() <= timeWindows.get(preferredTimeWindow-1).getEndTime()*60
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
					routes.get(r).get(j).get(i).setServiceBegin(
							 (routes.get(r).get(j).get(i).getServiceBegin() + insertionCosts_Waiting));
					routes.get(r).get(j).get(i).setSlack(Math.max(0,routes.get(r).get(j).get(i).getTimeWindow().getEndTime()*60
							- routes.get(r).get(j).get(i).getServiceBegin()));

					insertionCosts_Waiting = insertionCosts_Waiting - routes.get(r).get(j).get(i).getWaitingTime();
				}
			}

			routes.get(r).get(preferredTimeWindow + after).get(0)
					.setTravelTime( insertionCosts.get(0).getDistanceXj());
			routes.get(r).get(preferredTimeWindow + after).get(0).setWaitingTime(0.0);

			RouteElement element = new RouteElement();
			element.setForecastedOrderRequest(forecastedOrderRequest);
			element.setId(forecastedOrderRequest.getId());
			element.setDeliveryArea(forecastedOrderRequest.getDeliveryArea());
			element.setDeliveryAreaId(forecastedOrderRequest.getDeliveryAreaId());
			element.setServiceTime(forecastedOrderRequest.getServiceTime());
			element.setServiceBegin((routes.get(r).get(preferredTimeWindow)
					.get(routes.get(r).get(preferredTimeWindow).size() - 1).getServiceBegin()
					+ routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size() - 1)
							.getServiceTime()
					+ insertionCosts.get(0).getDistanceIx()));

			element.setTravelTime( insertionCosts.get(0).getDistanceIx());
			element.setWaitingTime(0.0);
			element.setTimeWindow(timeWindows.get(preferredTimeWindow-1));
			element.setTimeWindowId(timeWindows.get(preferredTimeWindow-1).getId());
			element.setSlack(Math.max(0, timeWindows.get(preferredTimeWindow-1).getEndTime()*60 - element.getServiceBegin()));

			// insertion
			routes.get(r).get(preferredTimeWindow).add(element);
			insertionSuccessful = true;
			if (test == true) {
				
				for (int k = 0; k <= routes.get(0).size()-1; k++) {
					for (int i = 0; i <= routes.get(0).get(k).size()-1; i++) {
						System.out.println(routes.get(0).get(k).get(i).getId() + 
								"       TravelTime: " + routes.get(0).get(k).get(i).getTravelTime() + 
								"       B: " + routes.get(0).get(k).get(i).getServiceBegin() +
								"       E: " + k +
								"       S: " + routes.get(0).get(k).get(i).getSlack() + 
								"       W: " + routes.get(0).get(k).get(i).getWaitingTime());
					}
				}
				System.out.println("\n");
		}
		}
	}

	private void insertCustomerEndTimeWindowNoChanges(int r, ArrayList<ArrayList<ArrayList<RouteElement>>> routes,
			int preferredTimeWindow, ArrayList<TimeWindow> timeWindows, ArrayList<InsertionCosts> insertionCosts,
			ForecastedOrderRequest forecastedOrderRequest, int after) {



		// Routing element
		RouteElement element = new RouteElement();
		element.setForecastedOrderRequest(forecastedOrderRequest);
		element.setId(forecastedOrderRequest.getId());
		element.setDeliveryArea(forecastedOrderRequest.getDeliveryArea());
		element.setDeliveryAreaId(forecastedOrderRequest.getDeliveryAreaId());
		element.setServiceTime(forecastedOrderRequest.getServiceTime());
		element.setServiceBegin( (routes.get(r).get(preferredTimeWindow)
				.get(routes.get(r).get(preferredTimeWindow).size() - 1).getServiceBegin()
				+ routes.get(r).get(preferredTimeWindow).get(routes.get(r).get(preferredTimeWindow).size() - 1)
						.getServiceTime()
				+ insertionCosts.get(0).getDistanceIx()));

		element.setTravelTime(insertionCosts.get(0).getDistanceIx());
		element.setWaitingTime( 0.0);
		element.setTimeWindow(timeWindows.get(preferredTimeWindow-1));
		element.setTimeWindowId(timeWindows.get(preferredTimeWindow-1).getId());
		element.setSlack(Math.max(0, timeWindows.get(preferredTimeWindow-1).getEndTime()*60 - element.getServiceBegin()));

		// changes in route
		routes.get(r).get(preferredTimeWindow + after).get(0).setWaitingTime(
				(routes.get(r).get(preferredTimeWindow + after).get(0).getTimeWindow().getStartTime()*60
						- element.getServiceBegin() - element.getServiceTime()
						- insertionCosts.get(0).getDistanceXj()));
		routes.get(r).get(preferredTimeWindow + after).get(0)
				.setTravelTime(insertionCosts.get(0).getDistanceXj());

		// insertion
		routes.get(r).get(preferredTimeWindow).add(element);
		insertionSuccessful = true;

		if (test == true) {
			
			for (int k = 0; k <= routes.get(0).size()-1; k++) {
				for (int i = 0; i <= routes.get(0).get(k).size()-1; i++) {
					System.out.println(routes.get(0).get(k).get(i).getId() + 
							"       TravelTime: " + routes.get(0).get(k).get(i).getTravelTime() + 
							"       B: " + routes.get(0).get(k).get(i).getServiceBegin() +
							"       E: " + k +
							"       S: " + routes.get(0).get(k).get(i).getSlack() + 
							"       W: " + routes.get(0).get(k).get(i).getWaitingTime());
				}
			}
			System.out.println("\n");
	}
		
	}
}
