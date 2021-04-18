package logic.algorithm.vr.orienteering;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DecimalFormat;
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
import logic.entity.FeasibleInsertionPositions;
import logic.entity.FeasibleTimeWindows;
import logic.entity.ForecastedOrderRequest;
import logic.entity.InsertionCosts;
import logic.entity.InsertionCriterion;
import logic.service.support.LocationService;
import logic.utility.comparator.TimeWindowStartAscComparator;


/**
 * Orienteering_FCFS
 */

public class Orienteering_FCFS   implements RoutingAlgorithm{
	
	boolean test = false; 
	
	private static double MULTIPLIER = 60.0;
	
	//contains values for insertion criterion to decide which customer to insert next
	private ArrayList<InsertionCriterion> insertionCriterion = new ArrayList <InsertionCriterion>();

	//contains all requests that could be feasibly inserted into route 
	private ArrayList<ForecastedOrderRequest> possibleRequestsForInsertion = new ArrayList <ForecastedOrderRequest>();

	//contains all forecasted OrderRequests
	private ArrayList<ForecastedOrderRequest> requests;
	
	//contains all requests that are not accepted / not part of delivery routes
	private ArrayList<ForecastedOrderRequest> notAcceptedRequests;
	
	//best Solution found
	private ArrayList<ArrayList<RouteElement>> bestSolutionRoutes = new ArrayList<ArrayList<RouteElement>>();	
	
	// Create ArrayList which contains all routes
	ArrayList<ArrayList<RouteElement>> vehicleRoutes = new ArrayList<ArrayList<RouteElement>>();
	
	// check if customer was inserted in route
	boolean insertionSuccessful = true;

	private TimeWindowSet timeWindowSet;
	private Depot depot;
	// private TravelTimeSet travelTimeSet;
	private ArrayList<Vehicle> vehicles;
	private ArrayList<NodeDistance> distances;
	private ArrayList<Node> nodes;
	private DeliveryAreaSet deliveryAreaSet;
	private double[][] distanceAreas;
	private Routing routing;
	private static String[] paras = new String[] { "Constant_service_time" };
	private DeliveryArea centerArea;
	private int numberOfTimeWindows = 3;
	private boolean timeWindowCount3;

	// 0: in -> in
	// 1: in -> out
	// 2: out -> in
	// 3: out -> out
	
	//dummy 0 in beginning, preferredTimeWindow starts with 1
	double[][] timeDependentWeighting = 	{			
			{ 0, 0.95, 0.915, 0.87, 0.715, 0.84, 1.075 },
			{ 0, 0.79, 0.795, 0.725, 0.615, 0.775, 1.1 }, 
			{ 0, 0.725, 0.795, 0.79, 0.725, 0.915, 1.41 },
			{ 0, 0.79, 0.795, 0.725, 0.615, 0.775, 1.1 } };
	
	
	public Orienteering_FCFS(ArrayList<ForecastedOrderRequest> requests, TimeWindowSet timeWindowSet, Depot depot, ArrayList<Vehicle> vehicles, ArrayList<NodeDistance> distances, ArrayList<Node> nodes, TravelTimeSet travelTimeSet, DeliveryAreaSet deliveryAreaSet) {
		this.requests = requests;
		this.timeWindowSet = timeWindowSet;
		this.distances = distances;
		this.vehicles = vehicles;
		this.depot = depot;
		this.nodes = nodes;
		this.deliveryAreaSet = deliveryAreaSet;
		this.notAcceptedRequests = new ArrayList<ForecastedOrderRequest>();
	}

	public void start (){
		


		
		// Calculate distance matrix between delivery areas
		distanceAreas = LocationService.computeDistancesBetweenAreas(nodes, distances, deliveryAreaSet);

		// Closest node to depot (needed for dummy depot-elements)
		Node depotNode = LocationService.findClosestNode(nodes, depot.getLat(), depot.getLon());
		this.centerArea = LocationService.assignNodeToDeliveryArea(deliveryAreaSet, depotNode);
		// Create as many routes as vehicles
		for (int vehicleTypeNo = 0; vehicleTypeNo < vehicles.size(); vehicleTypeNo++) {
			for (int vehicleNo = 0; vehicleNo < (vehicles.get(vehicleTypeNo)).getVehicleNo(); vehicleNo++) {

				ArrayList<RouteElement> vehicleRoute = new ArrayList<RouteElement>();

				// Initialize each route with a depot at the beginning of each route
				RouteElement elementBeginning = new RouteElement();
				elementBeginning.setPosition(0);
				elementBeginning.setId(111111);
				elementBeginning.setServiceBegin(550.0);
				elementBeginning.setServiceTime(0.0);
				elementBeginning.setSlack(0.0);
				elementBeginning.setWaitingTime(0.0);
				elementBeginning.setTravelTime(0.0);
				elementBeginning.setTravelTimeFrom(0.0);
				elementBeginning.setTravelTimeTo(0.0);
				ForecastedOrderRequest request = new ForecastedOrderRequest();
				request.setClosestNode(depotNode);
				elementBeginning.setForecastedOrderRequest(request);

				// Add to each route
				vehicleRoute.add(elementBeginning);

				// Initialize each route with a depot at the end of each route
				RouteElement elementEnd = new RouteElement();
				elementEnd.setPosition(0);
				elementEnd.setId(999999);
				// begin of depot set early to lever out time window constraint for depot
				elementEnd.setServiceBegin(2000.0);
				elementEnd.setServiceTime(0.0);
				elementEnd.setSlack(0.0);
				elementEnd.setWaitingTime(0.0);
				elementEnd.setTravelTime(0.0);
				elementEnd.setTravelTimeFrom(0.0);
				elementEnd.setTravelTimeTo(0.0);
				ForecastedOrderRequest request2 = new ForecastedOrderRequest();
				request2.setClosestNode(depotNode);
				elementEnd.setForecastedOrderRequest(request2);

				// Add to each route
				vehicleRoute.add(elementEnd);

				// Add each route to routes
				vehicleRoutes.add(vehicleRoute);
			}

		}
		
		

		
		//number of times no improvements are identified
		int numberOfTimesNoImprovement = 0;
		
		//best solution so far
		double bestSolutionScore = 0;
		
		double solutionScore = 0;

		

	
		
		for (ForecastedOrderRequest request : requests){	

			notAcceptedRequests.add(request);
		}
		

		//number of iterations as in vansteenwegen
		
		
		//TODO
		while(numberOfTimesNoImprovement < 1){

			//find first Solution
			start(vehicleRoutes, notAcceptedRequests, distances);
					
			solutionScore = findSolutionScore(vehicleRoutes);
			
			if(test==true){
				//printRoute(vehicleRoutes);
				//System.out.println(solutionScore);
				}
			
			//if new route has better score in total -> update
			if (solutionScore > bestSolutionScore) {
				
				bestSolutionScore = solutionScore;

				
				bestSolutionRoutes.clear();		

				
				for(int i=0; i<vehicleRoutes.size(); i++){
					
					ArrayList<RouteElement> bestSolutionRoute = new ArrayList<RouteElement>();	
					for(int j=0; j<vehicleRoutes.get(i).size(); j++){
					
					RouteElement bestSolutionRouteElement = vehicleRoutes.get(i).get(j).copyElement();
					bestSolutionRoute.add(bestSolutionRouteElement);

				}
			
					bestSolutionRoutes.add(bestSolutionRoute);

				}

				numberOfTimesNoImprovement = 0;

			} else {
				

				numberOfTimesNoImprovement++;
				
				

			

			}
			
		}
		
		if(test==true){
			
//			printRoute(vehicleRoutes);
//			
//			System.out.println(" " );
//
		printRoute(bestSolutionRoutes);
		System.out.println("$$$ BEST: " + bestSolutionScore);
			
		
		

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

						//// Set vehicle type. Does not make sense here yet because no difference in types
						route.setVehicleType(((Vehicle) vehicles.get(vehicleTypeNo)).getVehicleType());
						route.setVehicleTypeId(((Vehicle) vehicles.get(vehicleTypeNo)).getVehicleTypeId());

						//// Go through respective route elements and add to route
						//// Assumption: VehicleRoute-ArrayLists are sorted regarding time window and route elements
						ArrayList<RouteElement> routeElements = new ArrayList<RouteElement>();
						int currentPosition = 1;
						for (int twID = 0; twID < bestSolutionRoutes.get(vehicleNo).size(); twID++) {
							for (int routeElementID = 0; routeElementID < bestSolutionRoutes.get(vehicleNo).size(); routeElementID++) {
								RouteElement element = bestSolutionRoutes.get(vehicleNo).get(routeElementID);

								// Do not add the dummy elements for the depot. They had a position assigned.
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
	
	public static RouteElement deepCopy(RouteElement routeElement) throws Exception
	{
	  ByteArrayOutputStream baos = new ByteArrayOutputStream();
	  new ObjectOutputStream( baos ).writeObject(routeElement);

	  ByteArrayInputStream bais = new ByteArrayInputStream( baos.toByteArray() );
	  Object bestSolutionRouteElement= new ObjectInputStream( bais ).readObject();

	  return (RouteElement) bestSolutionRouteElement;
	}
	
	public void start(ArrayList<ArrayList<RouteElement>> vehicleRoutes, ArrayList<ForecastedOrderRequest> notAcceptedRequests, ArrayList<NodeDistance> distances) {

		

		// Prepare list of time windows and sort by start
		ArrayList<TimeWindow> timeWindows = timeWindowSet.getElements();
		
		if (timeWindows.size() == 3) {// TODO: delete later, only hard - coded
			// if 3 time windows for travel times!!!
			timeWindowCount3 = true;
		} else {
			timeWindowCount3 = false;
		}
	
		Collections.sort(timeWindows, new TimeWindowStartAscComparator());
		
		
		
		ArrayList<ForecastedOrderRequest> requests = new ArrayList<ForecastedOrderRequest>();

		for(int notAccepted = 0; notAccepted < notAcceptedRequests.size(); notAccepted++){
			notAcceptedRequests.get(notAccepted).setAccepted(false);
			requests.add(notAcceptedRequests.get(notAccepted));
		}

		notAcceptedRequests.clear();
		

		// Go through all potential requests and perform routing
		for (int requestId=0; requestId < requests.size(); requestId++) {
			
			//System.out.println("ACCEPTED: " + requests.get(requestId).isAccepted());
			
			ForecastedOrderRequest request = requests.get(requestId);
			


			if(request.isAccepted() == false){


				
				boolean insertionSuccessful = false;
				
			// ArrayLists
			
			// contains insertionCosts for all possible insertionPosition for a request
			ArrayList<InsertionCosts> insertionCosts = new ArrayList<InsertionCosts>();

			// contains only insertionPositions that are feasible for a request
			ArrayList<FeasibleInsertionPositions> feasibleInsertionPositions = new ArrayList<FeasibleInsertionPositions>();

			// matches insertionPositions with timeWindows such that feasible time windows are known
			ArrayList<FeasibleTimeWindows> feasibleTimeWindows = new ArrayList<FeasibleTimeWindows>();

			
			// Routing Methods
			computeInsertionCosts(insertionCosts, vehicleRoutes, request, distances);
			sortInsertionCosts(insertionCosts);
			findFeasibleInsertionPositions(vehicleRoutes, insertionCosts, request, feasibleInsertionPositions);
			findFeasibleTimeWindows(vehicleRoutes, insertionCosts, request, feasibleInsertionPositions, feasibleTimeWindows, timeWindows);
			assignRequestToTimeWindow(vehicleRoutes, insertionCosts, request, feasibleTimeWindows, timeWindows, insertionSuccessful);

			
			
			}}
			
			// Reset boolean for next request
			//this.insertionSuccessful = false;
			
			
			if (insertionCriterion.size() > 0) {	
				insertRequestIntoRoute(vehicleRoutes, possibleRequestsForInsertion.get(insertionCriterion.get(0).getPosition()), timeWindows);	
			}
			
			//after successfully inserting a request empty list and start all over again with remaining requests
			insertionCriterion.clear();
			possibleRequestsForInsertion.clear();
			
			
			
		
		
		for(int z = 0; z < requests.size(); z++){
			if(requests.get(z).isAccepted() == false){
				notAcceptedRequests.add(requests.get(z));
			}
		}	

		
		
		
	}

	public Routing getResult() {
		return routing;
	}

	public static String[] getParameterSetting() {
		return paras;
	}

	public void computeInsertionCosts(ArrayList<InsertionCosts> insertionCosts, ArrayList<ArrayList<RouteElement>> vehicleRoutes, ForecastedOrderRequest request, ArrayList<NodeDistance> distances) {

		// Compute Insertion Costs for all  insertion positions for a request and all already inserted customers 
		for (int r = 0; r < vehicleRoutes.size(); r++) {

			for (int c = 0; c < vehicleRoutes.get(r).size() - 1; c++) {

				double insertionCosts_i;
				InsertionCosts insertion = new InsertionCosts();

				
				double distanceIx; // Ix: AlreadyAddedCustomer1 -> Request
				double distanceXj; // Xj: Request -> AlreadyAddedCustomer2
				double distanceIj; // Ij: AlreadyAddedCustomer1 -> AlreadyAddedCustomer2
				
				int directionIx ; //travel direction customer before (i) and order request (x)
				int directionXj; // travel direction order request (x) and customer after (j)
				int directionIj; // travel direction order request (i) and customer after (j)




				// get distances between two nodes
				 distanceIx = LocationService.getDistanceBetweenAreas(distances, vehicleRoutes.get(r).get(c).getForecastedOrderRequest().getClosestNode(), request.getClosestNode(), deliveryAreaSet, distanceAreas);
				 distanceXj = LocationService.getDistanceBetweenAreas(distances, request.getClosestNode(), vehicleRoutes.get(r).get(c + 1).getForecastedOrderRequest().getClosestNode(), deliveryAreaSet, distanceAreas);
				 distanceIj = LocationService.getDistanceBetweenAreas(distances, vehicleRoutes.get(r).get(c).getForecastedOrderRequest().getClosestNode(), vehicleRoutes.get(r).get(c + 1).getForecastedOrderRequest().getClosestNode(), deliveryAreaSet, distanceAreas);

				 directionIx = LocationService.getDirection(
						vehicleRoutes.get(r).get(c).getForecastedOrderRequest().getClosestNode(), 
						request.getClosestNode(), 
						deliveryAreaSet, 
						centerArea);
				
				 directionXj = LocationService.getDirection(
						request.getClosestNode(), 
						vehicleRoutes.get(r).get(c + 1).getForecastedOrderRequest().getClosestNode(), 
					deliveryAreaSet, centerArea);
				
				directionIj = LocationService.getDirection(
						vehicleRoutes.get(r).get(c).getForecastedOrderRequest().getClosestNode(), 
						vehicleRoutes.get(r).get(c + 1).getForecastedOrderRequest().getClosestNode(), 
						deliveryAreaSet, centerArea);
				
				
				//adjust travel time with time dependent weighting
				int currentTimeWindowId=1;
				if(c==0){
					currentTimeWindowId=1;
				}else if(c== vehicleRoutes.get(r).size()-1){
					currentTimeWindowId= numberOfTimeWindows;
				}else{
					if(vehicleRoutes.get(r).get(c).getServiceBegin() < 720){currentTimeWindowId = 1;}
					if(vehicleRoutes.get(r).get(c).getServiceBegin() >= 720 && vehicleRoutes.get(r).get(c).getServiceBegin() < 840){currentTimeWindowId = 2;}
					if(vehicleRoutes.get(r).get(c).getServiceBegin() >= 840 && vehicleRoutes.get(r).get(c).getServiceBegin() < 2000){currentTimeWindowId = 3;}
				
				}
				
				
				int relevantWindow = currentTimeWindowId;
				if (timeWindowCount3) { // TODO: adjust!! Only now hard-coded for travel
										// times with 3 time windows
					relevantWindow++;
				}
				

				distanceIx = distanceIx/timeDependentWeighting[directionIx][relevantWindow];
				distanceIj = distanceIj/timeDependentWeighting[directionIj][relevantWindow];
				
				if(c+1==0){
					currentTimeWindowId=1;
				}else if(c+1== vehicleRoutes.get(r).size()-1){
					currentTimeWindowId= numberOfTimeWindows;
				}else{
					
					if(vehicleRoutes.get(r).get(c).getServiceBegin() < 720){currentTimeWindowId = 1;}
					if(vehicleRoutes.get(r).get(c).getServiceBegin() >= 720 && vehicleRoutes.get(r).get(c).getServiceBegin() < 840){currentTimeWindowId = 2;}
					if(vehicleRoutes.get(r).get(c).getServiceBegin() >= 840 && vehicleRoutes.get(r).get(c).getServiceBegin() < 2000){currentTimeWindowId = 3;}			
				}

				
				distanceXj = distanceXj/timeDependentWeighting[directionXj][relevantWindow];
				

				
				// compute insertion costs = Ix + Xj - Ij + serviceTime
				insertionCosts_i = distanceIx + distanceXj - distanceIj + request.getServiceTime();

				insertion.setDistanceIx(distanceIx);
				insertion.setDistanceXj(distanceXj);
				insertion.setDistanceIj(distanceIj);
				insertion.setInsertionCosts(insertionCosts_i);
				insertion.setInsertAfterId(c);

				// from where we have to test insertion, all customers after insertion might be affected
				insertion.setCheckFromHere(c + 1);
				insertion.setRoute(r);

				// if a route is still empty, initialize with customer
				for(int route=0; route<vehicleRoutes.size(); route++){
					
					if(vehicleRoutes.get(route).size() == 2 && r == route){
						insertionCosts.add(insertion);						
					}else if(vehicleRoutes.get(route).size()!= 2){
						insertionCosts.add(insertion);
					}
					
					
				}
			}
		}
	}

	public void sortInsertionCosts(ArrayList<InsertionCosts> insertionCosts) {

		// orders insertion costs ascending to allow for possible insertion with minimal costs
		InsertionCosts i1;
		InsertionCosts i2;

		for (int n = insertionCosts.size(); n > 1; n = n - 1) {
			for (int i = 0; i < insertionCosts.size() - 1; i++) {
				if (insertionCosts.get(i).getInsertionCosts() > insertionCosts.get(i + 1).getInsertionCosts()) {
					i1 = insertionCosts.get(i);
					i2 = insertionCosts.get(i + 1);
					insertionCosts.set(i, i2);
					insertionCosts.set((i + 1), i1);
				}
			}
		}
	}

	public void findFeasibleInsertionPositions(ArrayList<ArrayList<RouteElement>> vehicleRoutes, ArrayList<InsertionCosts> insertionCosts, ForecastedOrderRequest request, ArrayList<FeasibleInsertionPositions> feasibleInsertionPositions) {
		// finds all feasible insertion positions for new request, such that no
		// time window constraints of other customers are violated

		for (int i = 0; i < insertionCosts.size(); i++) {

			FeasibleInsertionPositions feasible = new FeasibleInsertionPositions();

			int routeInsertion = insertionCosts.get(i).getRoute();
			int insertAfter = insertionCosts.get(i).getInsertAfterId();

			// how much can we postpone service begin of already accepted customers without violating time window constraints
			double maxPostponing = findMaxPostponing(vehicleRoutes, insertionCosts, request, i); 
			
			// span of when we can arrive at the new request
			// earliest: earliest time we can start service
			// latest: latest time we can start service
			double earliestServiceBegin = (vehicleRoutes.get(routeInsertion).get(insertAfter).getServiceBegin() + vehicleRoutes.get(routeInsertion).get(insertAfter).getServiceTime()) + insertionCosts.get(i).getDistanceIx();
			double latestServiceBegin = vehicleRoutes.get(routeInsertion).get(insertAfter + 1).getServiceBegin() - insertionCosts.get(i).getDistanceXj() - request.getServiceTime() + maxPostponing;

			
			// only feasible if earliest <= latest
			if (earliestServiceBegin <= latestServiceBegin) {
				
				// add to feasible insertion positions
				feasible.setInsertAfter(insertAfter);
				feasible.setInsertionCosts(insertionCosts.get(i).getInsertionCosts());
				feasible.setRoute(routeInsertion);
				feasible.setEarliestServiceBegin(earliestServiceBegin);
				feasible.setLatestServiceBegin(latestServiceBegin);
				feasible.setSpanServiceBegin(latestServiceBegin - earliestServiceBegin);
				feasible.setServiceTime(request.getServiceTime());
				feasible.setTravelTimeTo(insertionCosts.get(i).getDistanceIx());
				feasible.setTravelTimeFrom(insertionCosts.get(i).getDistanceXj());
			

				feasibleInsertionPositions.add(feasible);
			}
		}
	}

	public double findMaxPostponing(ArrayList<ArrayList<RouteElement>> vehicleRoutes, ArrayList<InsertionCosts> insertionCosts, ForecastedOrderRequest request, int i) {
		// checks slacks (remaining time to end of time window) for all
		// customers to find out how much we can postpone service begin

		int routeInsertion = insertionCosts.get(i).getRoute();
		double maxPostponing = vehicleRoutes.get(routeInsertion).get(1).getSlack();
		int checkInsertion = insertionCosts.get(i).getCheckFromHere();

		double currentSlack = 0;

		for (int c = checkInsertion; c < vehicleRoutes.get(routeInsertion).size() - 1; c++) {

			
			currentSlack = vehicleRoutes.get(routeInsertion).get(c).getSlack() - vehicleRoutes.get(routeInsertion).get(c).getWaitingTime();
			
			if (currentSlack > vehicleRoutes.get(routeInsertion).get(c + 1).getWaitingTime()) {
				if (currentSlack < maxPostponing) {
					maxPostponing = currentSlack;
				}
			}
		}
		return maxPostponing;
	}

	public void findFeasibleTimeWindows(ArrayList<ArrayList<RouteElement>> vehicleRoutes, ArrayList<InsertionCosts> insertionCosts, ForecastedOrderRequest request, ArrayList<FeasibleInsertionPositions> feasibleInsertionPositions, ArrayList<FeasibleTimeWindows> feasibleTimeWindows,
			ArrayList<TimeWindow> timeWindows) {
		
		// for all feasible insertion positions match with prescribed time
		// windows
		// note: span of feasible insertion positions can be smaller or bigger
		// than a time window, such that it can either correspond to no time
		// window, one time window or more than one time window

		for (TimeWindow t : timeWindows) {
			for (FeasibleInsertionPositions f : feasibleInsertionPositions) {

				FeasibleTimeWindows feasible = new FeasibleTimeWindows();

				if ((f.getEarliestServiceBegin() >= t.getStartTime()*MULTIPLIER && f.getEarliestServiceBegin() <= t.getEndTime()*MULTIPLIER) 
						|| (f.getLatestServiceBegin() >= t.getStartTime()*MULTIPLIER && f.getLatestServiceBegin() <= t.getEndTime()*MULTIPLIER)
						|| (f.getEarliestServiceBegin() <= t.getStartTime()*MULTIPLIER && f.getLatestServiceBegin() >= t.getEndTime()*MULTIPLIER)) {

					
					feasible.setBegin(t.getStartTime()*MULTIPLIER);
					feasible.setEnd(t.getEndTime()*MULTIPLIER);
					feasible.setEarliestServiceBegin(f.getEarliestServiceBegin());
					feasible.setLatestServiceBegin(f.getLatestServiceBegin());
					feasible.setInsertAfter(f.getInsertAfter());
					feasible.setInsertionCosts(f.getInsertionCosts());
					feasible.setRoute(f.getRoute());
					feasible.setServiceTime(f.getServiceTime());
					feasible.setSpanServiceBegin(f.getSpanServiceBegin());
					feasible.setTravelTimeTo(f.getTravelTimeTo());
					feasible.setTravelTimeFrom(f.getTravelTimeFrom());

					feasibleTimeWindows.add(feasible);
					

				}
			}
		}		
	}

	public void assignRequestToTimeWindow(ArrayList<ArrayList<RouteElement>> routes, ArrayList<InsertionCosts> insertionCosts, ForecastedOrderRequest request, ArrayList<FeasibleTimeWindows> feasibleTimeWindows, ArrayList<TimeWindow> timeWindows, boolean insertionSuccessful) {

//		System.out.println("ASSIGNREQUEST TO TIME WINDOW: " + request.getId());
//		
//		System.out.println("ROUTENGR�SSE: " + routes.get(0).size());

		
		
		
		double insertionCosts_help = 0;
		
		for(TimeWindow t : timeWindows){
			t.setAvailable(false);
		}
		
		
		// check which timeWindows are feasible
		for (FeasibleTimeWindows f : feasibleTimeWindows) {
			for (TimeWindow t : timeWindows) {
				if (t.getStartTime()*MULTIPLIER == f.getBegin()) {
					t.setAvailable(true);
				}
			}
		}
		
		
		
		// get preferred time window of customer
		TimeWindow preferredTimeWindow = request.getAlternativePreferenceList().get(1).getTimeWindows().get(0);
		int preferredTimeWindowId = 0;
		for (int twID = 0; twID < timeWindows.size(); twID++) {
			if (preferredTimeWindow.getId() == timeWindows.get(twID).getId()) {
				preferredTimeWindowId = twID;
				break;
			}
		}


		
		// update information for request before inserting
		for (FeasibleTimeWindows f : feasibleTimeWindows) {
			
			
//			System.out.println("TRUE? : " + timeWindows.get(preferredTimeWindowId).getAvailable());
//			System.out.println("f.getBegin()" + f.getBegin());
//			
//			System.out.println("timeWindows.get(preferredTimeWindowId).getStartTime()*MULTIPLIER" + timeWindows.get(preferredTimeWindowId).getStartTime()*MULTIPLIER);
	
			
			if (timeWindows.get(preferredTimeWindowId).getAvailable() == true && f.getBegin() == timeWindows.get(preferredTimeWindowId).getStartTime()*MULTIPLIER){
				
//				System.out.println("FIRST IF: " + request.getId());

				if (insertionSuccessful == false) {
					
					request.setId(request.getId());

					request.setDeliveryArea(request.getDeliveryArea());
					request.setDeliveryAreaId(request.getDeliveryAreaId());
					request.setServiceTime(request.getServiceTime());
					request.setTimeWindow(timeWindows.get(preferredTimeWindowId));
					request.setTimeWindowId(timeWindows.get(preferredTimeWindowId).getId());
					request.setTravelTimeTo( f.getTravelTimeTo());
					request.setTravelTimeFrom(f.getTravelTimeFrom());
					request.setServiceBegin( Math.max(timeWindows.get(preferredTimeWindowId).getStartTime()*MULTIPLIER, f.getEarliestServiceBegin()));
					
					request.setStartTimeWindow(timeWindows.get(preferredTimeWindowId).getStartTime()*MULTIPLIER);
					request.setEndTimeWindow(timeWindows.get(preferredTimeWindowId).getEndTime()*MULTIPLIER);
					
					
					request.setSlack( request.getTimeWindow().getEndTime()*MULTIPLIER - request.getServiceBegin());
					
					
					request.setEstimatedValue(request.getEstimatedValue());
					request.setServiceTime(request.getServiceTime());

					
					//waitingTime can either be >0 if serviceBegin is at begin of time window, if not, waitingTime is 0
					if (request.getServiceBegin() == timeWindows.get(preferredTimeWindowId).getStartTime()*MULTIPLIER) {

						double waitingTime = (timeWindows.get(preferredTimeWindowId).getStartTime()*MULTIPLIER - (routes.get(f.getRoute()).get(f.getInsertAfter()).getServiceBegin() + routes.get(f.getRoute()).get(f.getInsertAfter()).getServiceTime()) - f.getTravelTimeTo());
						request.setWaitingTime(waitingTime);

						
					} else {
						request.setWaitingTime(0.0);
					}


					request.setInsertAfter(f.getInsertAfter());
					request.setRouteNo(f.getRoute());
					
					insertionCosts_help = f.getInsertionCosts();
					insertionSuccessful = true;

				}


				
				
				
					//add to insertionCriterionList for comparing scores of all requests
					InsertionCriterion criterion = new InsertionCriterion();
					criterion.setInsertionCosts(insertionCosts_help);
					criterion.setPosition(possibleRequestsForInsertion.size());
					criterion.setScore(request.getEstimatedValue()*100);	
						
					//compute criterion for next customer to insert
					
					if(criterion.getInsertionCosts() > 0){
						criterion.setCriterion((criterion.getScore()*criterion.getScore())/criterion.getInsertionCosts());
					}else{
						criterion.setCriterion(criterion.getScore()*criterion.getScore());
					}
					

					//System.out.println("HIER MIT REQUEST ID " + request.getId());
					insertionCriterion.add(criterion);				
					possibleRequestsForInsertion.add(request);
					

					//System.out.println("############### ANZAHL: " + possibleRequestsForInsertion.size());

			}
		}
		
	}
	
	public void compareScore(ArrayList<InsertionCriterion> insertionCriterion){
		
		//Collections.sort(insertionCriterion, new ScoreComparator());
		
		
	//	 orders insertion criterion ascending to allow for possible insertion with minimal costs
		InsertionCriterion i1;
		InsertionCriterion i2;

		for (int n = insertionCriterion.size(); n > 1; n = n - 1) {
			for (int i = 0; i < insertionCriterion.size() - 1; i++) {
				if (insertionCriterion.get(i).getCriterion() > insertionCriterion.get(i + 1).getCriterion()) {
					i1 = insertionCriterion.get(i);
					i2 = insertionCriterion.get(i + 1);
					insertionCriterion.set(i, i2);
					insertionCriterion.set((i + 1), i1);
				}
			}
		}
		
		
	}

	public void insertRequestIntoRoute(ArrayList<ArrayList<RouteElement>> routes, ForecastedOrderRequest request, ArrayList<TimeWindow> timeWindows) {
		
		
//		System.out.println("HIER: " + request.getEstimatedValue());
		
		// insert request into route		
		RouteElement element = new RouteElement();
				
		element.setForecastedOrderRequest(request);		
		element.setId(request.getId());
		element.setDeliveryArea(request.getDeliveryArea());
		element.setDeliveryAreaId(request.getDeliveryAreaId());
		element.setServiceTime(request.getServiceTime());
		element.setTimeWindow(request.getTimeWindow());
		element.setTimeWindowId(request.getTimeWindowId());
		element.setTravelTimeTo(request.getTravelTimeTo());
		element.setTravelTimeFrom(request.getTravelTimeFrom());
		element.setServiceBegin(request.getServiceBegin());
		element.setSlack(request.getSlack());
		element.setWaitingTime(request.getWaitingTime());
		element.setEstimatedValue(request.getEstimatedValue());
		element.setEndTimeWindow(request.getEndTimeWindow());
		element.setStartTimeWindow(request.getStartTimeWindow());

		

		

		

		int route = request.getRouteNo();
		int position = request.getInsertAfter()+1;
		

		
		routes.get(route).add(position, element);

		
		request.setAccepted(true);
		
		updateRoute(routes, element, timeWindows, request.getInsertAfter(), request.getRouteNo());

	}

	public void updateRoute(ArrayList<ArrayList<RouteElement>> routes, RouteElement element, ArrayList<TimeWindow> timeWindows, 
			int insertAfter, int route) {


		// where to start to check insertion causes changes
		int checkFromHere = insertAfter + 2;

		// how much to change is caused
		double postpone = 0;

		// next ServiceBegin is not affected, no postponing needed

		if ((element.getServiceBegin() + element.getServiceTime()) + element.getTravelTimeFrom() < routes.get(route).get(checkFromHere).getServiceBegin()) { 
		
			
		postpone = 0;	
		// postponing is difference of old ServiceBegin and new ServiceBegin (ServiceEnd before and travelTime)
		} else {
			postpone = Math.max(0, (element.getServiceBegin() + element.getServiceTime()) + element.getTravelTimeFrom() - routes.get(route).get(checkFromHere).getServiceBegin()); 
		}

		
		for (int c = checkFromHere; c < routes.get(route).size(); c++) {
			if (c != checkFromHere) {
				// postponing absorbed by waiting time
				postpone = Math.max(0, postpone - routes.get(route).get(c).getWaitingTime()); 
			}

			routes.get(route).get(c).setServiceBegin( routes.get(route).get(c).getServiceBegin() + postpone);
			routes.get(route).get(c).setSlack(Math.max(0, routes.get(route).get(c).getEndTimeWindow() - routes.get(route).get(c).getServiceBegin()));
			routes.get(route).get(c).setWaitingTime(Math.max(0, (

			routes.get(route).get(c).getStartTimeWindow() - (routes.get(route).get(c - 1).getServiceBegin() + routes.get(route).get(c - 1).getServiceTime()) - routes.get(route).get(c - 1).getTravelTimeFrom())));

			
			// if element from beginning of time window has to be postponed there is no waiting time anymore
			if (routes.get(route).get(c).getServiceBegin() != routes.get(route).get(c).getStartTimeWindow()) { 
				routes.get(route).get(c).setWaitingTime(0.0);
			}

			// change of travelTimes due to new inserted customer
			routes.get(route).get(checkFromHere - 2).setTravelTimeFrom(element.getTravelTimeTo()); 
			routes.get(route).get(checkFromHere).setTravelTimeTo(element.getTravelTimeFrom());
		}
		
	}
	
	public double findSolutionScore (ArrayList<ArrayList<RouteElement>> routes){
		
		double solutionScore = 0;
		
		for(int route = 0; route < routes.size(); route++){
			for(int c = 0; c < routes.get(route).size(); c++){
				solutionScore = solutionScore + routes.get(route).get(c).getEstimatedValue();
			}
		}
		
		return solutionScore;
	}
	
	public void printRoute(ArrayList<ArrayList<RouteElement>> routes){
	

		DecimalFormat two = new DecimalFormat("0.0");
	for (int route = 0; route < routes.size(); route++) {

		System.out.println("");
		System.out.println("Route " + route);

		for (int c = 0; c < routes.get(route).size(); c++) {
			System.out.println(

					
					
					routes.get(route).get(c).getId() + " " +  
					"     ;W;   " + (two.format(routes.get(route).get(c).getWaitingTime())) + 
					"     ;To;    " + (two.format(routes.get(route).get(c).getTravelTimeTo())) + 
					"     ;B;   " + (two.format(routes.get(route).get(c).getServiceBegin())) + 
					"     ;From;    " + (two.format(routes.get(route).get(c).getTravelTimeFrom())) + 
					"     ;S;    " + (two.format(routes.get(route).get(c).getSlack())) + 
					"     ;TW;   " + (two.format(routes.get(route).get(c).getStartTimeWindow())) + 
					"-" + (two.format(routes.get(route).get(c).getEndTimeWindow())) + 
					"  ; SCORE; " + (two.format(routes.get(route).get(c).getEstimatedValue())));

			
		}
		
	
	}
	}


}
