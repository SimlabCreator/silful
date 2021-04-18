
	package logic.algorithm.vr.construction;

	import java.util.ArrayList;
import java.util.Collections;
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
import logic.service.support.LocationService;
import logic.utility.comparator.ForecastedOrderRequestValueDescComparator;
import logic.utility.comparator.TimeWindowStartAscComparator;
	
	/**
	 * Computes insertion costs for every forecasted request
	 * Inserts requests into route if feasible in a GREEDY way, such that customer is inserted with short-term minimal costs 
	 * 
	 * @author C. K�hler
	 *
	 */

public class InsertionConstructionHeuristicGreedy implements RoutingAlgorithm {


		boolean test = false; 

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
		private double[][] distanceAreas;

		
		private Routing routing;
		private static String[] paras = new String[] { "Constant_service_time" };

		public InsertionConstructionHeuristicGreedy(ArrayList<ForecastedOrderRequest> requests, TimeWindowSet timeWindowSet,
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
			
	
			// Prepare list of time windows and sort by start
			ArrayList<TimeWindow> timeWindows = timeWindowSet.getElements();
	
			Collections.sort(timeWindows, new TimeWindowStartAscComparator());
			
						
//			// Sort forecasted order requests
						Collections.sort(requests, new ForecastedOrderRequestValueDescComparator());
						for(int i=0; i < requests.size(); i++){
							if(requests.get(i).getAlternativePreferenceList().get(1).getId()==31 &&requests.get(i).getDeliveryAreaId()==16){
								System.out.println("Position "+i+" value "+requests.get(i).getEstimatedValue());
							}
						}
				
			//Create ArrayList which contains all routes 
			ArrayList<ArrayList<RouteElement>> vehicleRoutes = new ArrayList<ArrayList<RouteElement>>();
			
			//Calculate distance matrix between delivery areas
			distanceAreas=LocationService.computeDistancesBetweenAreas(nodes, distances, deliveryAreaSet);

			// Closest node to depot (needed for dummy depot-elements)
			Node depotNode = LocationService.findClosestNode(nodes, depot.getLat(), depot.getLon());

			// Create as many routes as vehicles
			for (int vehicleTypeNo = 0; vehicleTypeNo < vehicles.size(); vehicleTypeNo++) {
				for (int vehicleNo = 0; vehicleNo < ((Vehicle) vehicles.get(vehicleTypeNo)).getVehicleNo(); vehicleNo++) {
					
					ArrayList<RouteElement> vehicleRoute = new ArrayList<RouteElement>();
					
					
					//initialize each route with a depot at the beginning of each route
							RouteElement elementBeginning = new RouteElement();
							elementBeginning.setPosition(0);
							elementBeginning.setId(111111);
							//elementBeginning.setTimeWindowId(11);
							elementBeginning.setServiceBegin(550.0);
							elementBeginning.setServiceTime( 0.0);
							elementBeginning.setSlack(0.0);
							elementBeginning.setWaitingTime( 0.0);
							elementBeginning.setTravelTime(0.0);
							ForecastedOrderRequest request = new ForecastedOrderRequest();
							request.setClosestNode(depotNode);
							elementBeginning.setForecastedOrderRequest(request);
							
							
							//add to each route
							vehicleRoute.add(elementBeginning);
						
					//initialize each route with a depot at the end of each route
							RouteElement elementEnd = new RouteElement();
							elementEnd.setPosition(0);
							elementEnd.setId(999999);
							//elementEnd.setTimeWindowId(10);
							//begin of depot set early to lever out time window constraint for depot
							elementEnd.setServiceBegin(2000.0);
							elementEnd.setServiceTime( 0.0);
							elementEnd.setSlack(10000.0);
							elementEnd.setWaitingTime(0.0);
							elementEnd.setTravelTime( 0.0);
							request.setClosestNode(depotNode);
							elementEnd.setForecastedOrderRequest(request);


							
							//add to each route
							vehicleRoute.add(elementEnd);
							
							//add each route to routes							
							vehicleRoutes.add(vehicleRoute);
					}
					
				}
			


			int loop = 0;
			
			// Go through all potential requests and perform routing
			for (ForecastedOrderRequest request : requests) {
				
				//Node closestNode = LocationService.findClosestNode(nodes, request.getClosestNode().getLat(), request.getClosestNode().getLon());
				//request.setClosestNode(closestNode);
				
								
				//request.setClosestNodeId(closestNode.getLongId());

				
				loop++;
				
				//insertionCosts for all insertionPositions
				ArrayList<InsertionCosts> insertionCosts = new ArrayList<InsertionCosts>();
				
				//contains only insertionPositions that are feasible 
				ArrayList<FeasibleInsertionPositions> feasibleInsertionPositions = new ArrayList<FeasibleInsertionPositions>();
				
				//matches insertionPositions with timeWindows such that it only contains feasible timeWindows 
				ArrayList<FeasibleTimeWindows> feasibleTimeWindows = new ArrayList<FeasibleTimeWindows>();

				
				computeInsertionCosts(loop, insertionCosts, vehicleRoutes, request, distances);
				sortInsertionCosts(insertionCosts);
				findFeasibleInsertionPositions(vehicleRoutes, insertionCosts, request, feasibleInsertionPositions);		
				findFeasibleTimeWindows(vehicleRoutes, insertionCosts, request, feasibleInsertionPositions, feasibleTimeWindows, timeWindows);
				assignRequestToTimeWindow(vehicleRoutes, insertionCosts, request, feasibleTimeWindows, timeWindows, insertionSuccessful);
				

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
						for (int routeElementID = 0; routeElementID < vehicleRoutes.get(vehicleNo).size(); routeElementID++) {
							RouteElement element = vehicleRoutes.get(vehicleNo).get(routeElementID);
							

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
				
				
				
				if(test==true){
					printRoute(vehicleRoutes);
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

		public void computeInsertionCosts(int loop, ArrayList<InsertionCosts> insertionCosts, ArrayList<ArrayList<RouteElement>> vehicleRoutes, 
				ForecastedOrderRequest request, ArrayList<NodeDistance> distances) {
			
			//Compute Insertion Costs for between all customers already on route
			for (int r = 0; r < vehicleRoutes.size(); r++) {


				for (int c = 0; c < vehicleRoutes.get(r).size() - 1; c++) {
					
					double timeDependency[] = {0.725,0.615,0.775,1.1};

					
					double insertionCosts_i;
					InsertionCosts insertion = new InsertionCosts();
					
					//Ix: Old -> New, Xj: New -> Old, Ij: Old -> Old
					double distanceIx;
					double distanceXj;
					double distanceIj;


					
					//time dependent factor
					int timeDependentFactor;
					
					
					//TODO HARD CODED TIMES 
					if((vehicleRoutes.get(r).get(c).getServiceBegin() + vehicleRoutes.get(r).get(c).getServiceTime()) < 960){timeDependentFactor = 0;}else{
					if((vehicleRoutes.get(r).get(c).getServiceBegin() + vehicleRoutes.get(r).get(c).getServiceTime()) >= 960 && (vehicleRoutes.get(r).get(c).getServiceBegin() + vehicleRoutes.get(r).get(c).getServiceTime()) < 1080){timeDependentFactor = 1;}else{
					if((vehicleRoutes.get(r).get(c).getServiceBegin() + vehicleRoutes.get(r).get(c).getServiceTime()) >= 1080 && (vehicleRoutes.get(r).get(c).getServiceBegin() + vehicleRoutes.get(r).get(c).getServiceTime()) < 1200){timeDependentFactor = 2;}else{
					timeDependentFactor = 3;}}}

// TODO DEPOT 


				
					
				distanceIx = LocationService.getDistanceBetweenAreas(distances, vehicleRoutes.get(r).get(c).getForecastedOrderRequest().getClosestNode(), request.getClosestNode(), deliveryAreaSet, distanceAreas);
				distanceXj = LocationService.getDistanceBetweenAreas(distances, request.getClosestNode(), vehicleRoutes.get(r).get(c + 1).getForecastedOrderRequest().getClosestNode(), deliveryAreaSet, distanceAreas);				
				distanceIj = LocationService.getDistanceBetweenAreas(distances, vehicleRoutes.get(r).get(c).getForecastedOrderRequest().getClosestNode(), vehicleRoutes.get(r).get(c + 1).getForecastedOrderRequest().getClosestNode(), deliveryAreaSet, distanceAreas);
				
//				System.out.println(distanceIx);
//				System.out.println(distanceXj);
//				System.out.println(distanceIj);
				
			
				
				distanceIx = distanceIx/timeDependency[timeDependentFactor];
				distanceXj = distanceXj/timeDependency[timeDependentFactor];
				distanceIj = distanceIj/timeDependency[timeDependentFactor];
					

					//Insertion Costs = Ix + Xj - Ij + serviceTime
					insertionCosts_i = distanceIx + distanceXj - distanceIj + request.getServiceTime();
					
					insertion.setDistanceIx(distanceIx);
					insertion.setDistanceXj(distanceXj);				
					insertion.setDistanceIj(distanceIj);
					insertion.setInsertionCosts(insertionCosts_i);
					insertion.setInsertAfterId(c);
					
					
					//from where we have to test insertion, all customers after insertion might be affected 
					insertion.setCheckFromHere(c + 1);
					insertion.setRoute(r);

					
					//TODO HARDCODED
					//initialize all routes with one customer
					 for(int j = 0; j < vehicleRoutes.size(); j++){
						 if(loop == j && r == j){
							 insertionCosts.add(insertion);
						 }
						 if(loop > vehicleRoutes.size()){
							 insertionCosts.add(insertion);
						 }
					 }

				}

			}

		}

		public void sortInsertionCosts(ArrayList<InsertionCosts> insertionCosts) {

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

		for (int i = 0; i < insertionCosts.size(); i++) {

			FeasibleInsertionPositions feasible = new FeasibleInsertionPositions();

			int routeInsertion = insertionCosts.get(i).getRoute();
			int insertAfter = insertionCosts.get(i).getInsertAfterId();

			double maxPostponing = findMaxPostponing(vehicleRoutes, insertionCosts, request, i);

			double earliestServiceBegin = (vehicleRoutes.get(routeInsertion).get(insertAfter).getServiceBegin() + vehicleRoutes.get(routeInsertion).get(insertAfter).getServiceTime()) + insertionCosts.get(i).getDistanceIx();
			double latestServiceBegin = vehicleRoutes.get(routeInsertion).get(insertAfter + 1).getServiceBegin() - insertionCosts.get(i).getDistanceXj() - request.getServiceTime() + maxPostponing;

			if (earliestServiceBegin <= latestServiceBegin) {

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
			
	public void findFeasibleTimeWindows(ArrayList<ArrayList<RouteElement>> vehicleRoutes, ArrayList<InsertionCosts> insertionCosts, ForecastedOrderRequest request,
			ArrayList<FeasibleInsertionPositions> feasibleInsertionPositions, ArrayList<FeasibleTimeWindows> feasibleTimeWindows, ArrayList<TimeWindow> timeWindows) {
		
			for(TimeWindow t : timeWindows){
				
			for (FeasibleInsertionPositions f : feasibleInsertionPositions) {

				FeasibleTimeWindows feasible = new FeasibleTimeWindows();

				
				if ((f.getEarliestServiceBegin() >= (t.getStartTime()*60) && f.getEarliestServiceBegin() <= (t.getEndTime())*60) || (f.getLatestServiceBegin() >= t.getStartTime()*60 && f.getLatestServiceBegin() <= (t.getEndTime()*60))
						|| (f.getEarliestServiceBegin() <= (t.getStartTime()*60) && f.getLatestServiceBegin() >= (t.getEndTime()*60))) {

					feasible.setBegin((t.getStartTime()*60));
					feasible.setEnd((t.getEndTime()*60));

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
		
		
	public void assignRequestToTimeWindow(ArrayList<ArrayList<RouteElement>> routes, ArrayList<InsertionCosts> insertionCosts, ForecastedOrderRequest request, ArrayList<FeasibleTimeWindows> feasibleTimeWindows, ArrayList<TimeWindow> timeWindows,
			boolean insertionSuccessful) {

		// check which timeWindows are feasible
		for (FeasibleTimeWindows f : feasibleTimeWindows) {
			for (TimeWindow t : timeWindows) {
				if ((t.getStartTime()*60) == f.getBegin()) {
					t.setAvailable(true);
				}
			}
		}

		int insertAfter;
		int route;

		TimeWindow preferredTimeWindow = request.getAlternativePreferenceList().get(1).getTimeWindows().get(0);
		int preferredTimeWindowId = 0;
		for (int twID = 0; twID < timeWindows.size(); twID++) {
			if (preferredTimeWindow.getId() == timeWindows.get(twID).getId()) {
				preferredTimeWindowId = twID;
				break;
			}
		}

		for (FeasibleTimeWindows f : feasibleTimeWindows) {
			if (f.getBegin() == (timeWindows.get(preferredTimeWindowId).getStartTime()*60)) {

				if (insertionSuccessful == false) {

					RouteElement element = new RouteElement();

					element.setForecastedOrderRequest(request);
					element.setId(request.getId());

					element.setDeliveryArea(request.getDeliveryArea());
					element.setDeliveryAreaId(request.getDeliveryAreaId());
					element.setServiceTime(request.getServiceTime());

					element.setTimeWindow(timeWindows.get(preferredTimeWindowId));
					element.setTimeWindowId(timeWindows.get(preferredTimeWindowId).getId());
					
					element.setStartTimeWindow(timeWindows.get(preferredTimeWindowId).getStartTime()*60);
					element.setEndTimeWindow(timeWindows.get(preferredTimeWindowId).getEndTime()*60);
					
					element.setTravelTimeTo( f.getTravelTimeTo());
					element.setTravelTimeFrom(f.getTravelTimeFrom());

					element.setServiceBegin( Math.max((element.getStartTimeWindow()), f.getEarliestServiceBegin()));
					element.setSlack((element.getEndTimeWindow()) - element.getServiceBegin());
				

					if (element.getServiceBegin() == element.getStartTimeWindow()) {
						element.setWaitingTime(((element.getStartTimeWindow()) - (routes.get(f.getRoute()).get(f.getInsertAfter()).getServiceBegin() + routes.get(f.getRoute()).get(f.getInsertAfter()).getServiceTime())
								- f.getTravelTimeTo()));
					} else {
						element.setWaitingTime(0.0);
					}

					insertAfter = f.getInsertAfter();
					route = f.getRoute();

					
					insertionSuccessful = true;

					insertRequestIntoRoute(routes, insertionCosts, element, feasibleTimeWindows, timeWindows, insertAfter, route);
				}
			}
		}

	}
	
	
	public void insertRequestIntoRoute(ArrayList<ArrayList<RouteElement>> routes, ArrayList<InsertionCosts> insertionCosts, RouteElement element, ArrayList<FeasibleTimeWindows> feasibleTimeWindows, ArrayList<TimeWindow> timeWindows,
			int insertAfter, int route) {

		
		routes.get(route).add((insertAfter + 1), element);
		updateRoute(routes, insertionCosts, element, feasibleTimeWindows, timeWindows, insertAfter, route);
		
	}
	
	
	
	
	public void updateRoute(ArrayList<ArrayList<RouteElement>> routes, ArrayList<InsertionCosts> insertionCosts, RouteElement element, ArrayList<FeasibleTimeWindows> feasibleTimeWindows, ArrayList<TimeWindow> timeWindows, 
			int insertAfter, int route) {

		// where to start to check insertion causes changes
		int checkFromHere = insertAfter + 2;

		// how much to change is caused
		double postpone;

		if ((element.getServiceBegin() + element.getServiceTime()) + element.getTravelTimeFrom() < routes.get(route).get(checkFromHere).getServiceBegin()) { // next ServiceBegin is not affected, no postponing needed

			postpone = 0;
			
		} else {
			postpone = Math.max(0, (element.getServiceBegin() + element.getServiceTime()) + element.getTravelTimeFrom() - routes.get(route).get(checkFromHere).getServiceBegin()); // postponing is difference of old ServiceBegin and new ServiceBegin (ServiceEnd Before and travelTime)

		}


		for (int c = checkFromHere; c < routes.get(route).size(); c++) {
			if (c != checkFromHere) {
				postpone = Math.max(0, postpone - routes.get(route).get(c).getWaitingTime()); // postpone absorbed by waiting time
			}

			routes.get(route).get(c).setServiceBegin( routes.get(route).get(c).getServiceBegin() + postpone);
			routes.get(route).get(c).setSlack(Math.max( 0.0,  routes.get(route).get(c).getEndTimeWindow() - routes.get(route).get(c).getServiceBegin()));
			routes.get(route).get(c).setWaitingTime( Math.max(0, (routes.get(route).get(c).getStartTimeWindow() - (routes.get(route).get(c - 1).getServiceBegin() + routes.get(route).get(c - 1).getServiceTime()) - routes.get(route).get(c - 1).getTravelTimeFrom())));

			if (routes.get(route).get(c).getServiceBegin() != routes.get(route).get(c).getStartTimeWindow()) { //if element from beginning of time window has to be postponed there is no waiting time anymore
				routes.get(route).get(c).setWaitingTime(0.0);
			}

			routes.get(route).get(checkFromHere - 2).setTravelTimeFrom(element.getTravelTimeTo()); //change of travelTimes due to new inserted customer
			routes.get(route).get(checkFromHere).setTravelTimeTo(element.getTravelTimeFrom());

		}
		
	}

	public void printRoute(ArrayList<ArrayList<RouteElement>> routes) {
		
		//DecimalFormat two = new DecimalFormat("0.00");
		System.out.println("");
		for (int route = 0; route < routes.size(); route++) {

			System.out.println("");
			System.out.println("Route " + route);

			for (int c = 0; c < routes.get(route).size(); c++) {
				System.out.println(

						routes.get(route).get(c).getId() + 
						"     ;W;   " + routes.get(route).get(c).getWaitingTime() +
						"     ;To;    " + routes.get(route).get(c).getTravelTimeTo() + 
						"     ;B;   " + routes.get(route).get(c).getServiceBegin() + 
						"     ;From;    " + routes.get(route).get(c).getTravelTimeFrom() + 
						"     ;S;    " + routes.get(route).get(c).getSlack() + 
						"     ;TW;   " + routes.get(route).get(c).getStartTimeWindow() + 
						"-" + routes.get(route).get(c).getEndTimeWindow());
			}
		}

	}
	

	}




	




