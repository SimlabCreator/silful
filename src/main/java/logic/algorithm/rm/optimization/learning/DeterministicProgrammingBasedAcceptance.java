package logic.algorithm.rm.optimization.learning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math3.util.Pair;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import data.entity.Alternative;
import data.entity.AlternativeSet;
import data.entity.ConsiderationSetAlternative;
import data.entity.Customer;
import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.DemandSegment;
import data.entity.DemandSegmentSet;
import data.entity.DemandSegmentWeight;
import data.entity.DemandSegmentWeighting;
import data.entity.DistributionParameterValue;
import data.entity.Entity;
import data.entity.Order;
import data.entity.OrderRequest;
import data.entity.OrderRequestSet;
import data.entity.OrderSet;
import data.entity.ReferenceRouting;
import data.entity.Region;
import data.entity.Route;
import data.entity.RouteElement;
import data.entity.Routing;
import data.entity.TimeWindow;
import data.entity.TimeWindowSet;
import data.entity.VehicleAreaAssignment;
import data.entity.VehicleAreaAssignmentSet;
import logic.algorithm.rm.optimization.acceptance.AcceptanceAlgorithm;
import logic.service.support.ArrivalProcessService;
import logic.service.support.CustomerDemandService;
import logic.service.support.DynamicRoutingHelperService;
import logic.service.support.LinearProgrammingService;
import logic.service.support.LocationService;
import logic.service.support.OrienteeringAcceptanceHelperService;
import logic.service.support.ProbabilityDistributionService;
import logic.service.support.RoutingService;
import logic.utility.comparator.OrderRequestArrivalTimeDescComparator;
import logic.utility.exceptions.ParameterUnknownException;

/**
 * 
 * @author M. Lang
 *
 */
public class DeterministicProgrammingBasedAcceptance implements AcceptanceAlgorithm {
	private static int numberOfThreads=1;
	private boolean referenceInformationPrepared = false;
	private static double TIME_MULTIPLIER = 60.0;
	private TimeWindowSet timeWindowSet;
	private Region region;
	private DeliveryAreaSet deliveryAreaSet;
	private AlternativeSet alternativeSet;
	private VehicleAreaAssignmentSet vehicleAreaAssignmentSet;
	private OrderRequestSet orderRequestSet;
	private OrderSet orderSet;
	private HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors;
	private RConnection connection;

	private int includeDriveFromStartingPosition;

	private ArrayList<Routing> targetRoutingResults;

	private Double expectedServiceTime;
	private int numberRoutingCandidates;
	private int numberInsertionCandidates;

	private HashMap<Integer, ArrayList<VehicleAreaAssignment>> vehicleAssignmentsPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, VehicleAreaAssignment>> vasPerDeliveryAreaAndVehicleNo;
	private HashMap<Integer, Double> overallCapacityPerDeliveryArea;

	private boolean usepreferencesSampled;
	private boolean dynamicFeasibilityCheck;
	private boolean theftBased;
	private boolean theftBasedAdvanced;

	private HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>> aggregatedReferenceInformationNo;

	private HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, HashMap<DemandSegment, HashMap<Integer, Double>>>>>> assignedCapacitiesPerRouting;
	private HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>> bufferPerRouting;
	private HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Double>>>> openForAdvancedThefting;
	private HashMap<Integer, HashMap<TimeWindow, Integer>> averageReferenceInformationNoPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, Integer>> maximalAcceptablePerDeliveryAreaAndTw;

	private HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerSubDeliveryAreaAndTimeWindow;
	private HashMap<DeliveryArea, Double> daLowerWeights;
	private HashMap<DeliveryArea, DemandSegmentWeighting> daLowerSegmentWeightings;
	private HashMap<DeliveryArea, HashMap<TimeWindow, Double>> maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow;
	private HashMap<DeliveryArea, HashMap<TimeWindow, Double>> minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow;
	private HashMap<Integer, HashMap<Integer, Double>> maximumExpectedMultiplierPerDemandSegmentAndTimeWindow;
	private HashMap<Integer, HashMap<Integer, Double>> minimumExpectedMultiplierPerDemandSegmentAndTimeWindow;
	private HashMap<DemandSegment, HashMap<Alternative, Double>> demandMultiplierPerSegment;
	private int arrivalProcessId;
	private int stealingCounter = 0;
	private DemandSegmentWeighting demandSegmentWeighting;
	private DemandSegmentWeighting demandSegmentWeightingOriginal;
	private Double beta;
	private int bookingHorizonLength;
	private double expectedArrivalsOverall;
	private double arrivalProbability;
	private double maximumRevenueValue;
	private HashMap<Entity, Object> objectiveSpecificValues;
	private HashMap<Integer, Alternative> alternativesToTimeWindows;
	private HashMap<TimeWindow, Alternative> alternativesToTimeWindows2;
	private Alternative noPurchaseAlternative;
	private HashMap<Integer,Pair<Double, Pair<DemandSegment, DemandSegment>>> mapOriginalSegmentToSubSegment;
	private boolean duplicateSegments;

	// TODO: Add orienteering booleans to VFA?
	private static String[] paras = new String[] { "Constant_service_time", "samplePreferences",
			"includeDriveFromStartingPosition", "no_routing_candidates", "no_insertion_candidates",
			"dynamic_feasibility_check", "theft-based", "theft-based-advanced", "beta" , "duplicate_segments"};

	public DeterministicProgrammingBasedAcceptance(Region region, int bookingHorizonLength,
			VehicleAreaAssignmentSet vehicleAreaAssignmentSet, ArrayList<Routing> targetRoutingResults,
			OrderRequestSet orderRequestSet, DeliveryAreaSet deliveryAreaSet, Double expectedServiceTime,
			Double includeDriveFromStartingPosition, Double samplePreferences, Double numberRoutingCandidates,
			Double numberInsertionCandidates, Double dynamicFeasibilityCheck, Double theftBased,
			Double theftBasedAdvanced, HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors,
			DemandSegmentWeighting dsw, int arrivalProcessId, Double beta,
			double maximumRevenueValue, HashMap<Entity, Object> objectiveSpecificValues,  Double duplicateSegments) {

		this.initialise(region, bookingHorizonLength, deliveryAreaSet, neighbors, 
				 arrivalProcessId, beta, maximumRevenueValue,
				objectiveSpecificValues);

		
		this.targetRoutingResults = targetRoutingResults;
		this.orderRequestSet = orderRequestSet;
		this.vehicleAreaAssignmentSet = vehicleAreaAssignmentSet;
		this.expectedServiceTime = expectedServiceTime;
		this.includeDriveFromStartingPosition = includeDriveFromStartingPosition.intValue();
		this.numberRoutingCandidates = numberRoutingCandidates.intValue();
		this.numberInsertionCandidates = numberInsertionCandidates.intValue();
		this.timeWindowSet = orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getAlternativeSet()
				.getTimeWindowSet();
		this.alternativeSet = orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getAlternativeSet();

		if (dynamicFeasibilityCheck == 1.0) {
			this.dynamicFeasibilityCheck = true;
		} else {
			this.dynamicFeasibilityCheck = false;
		}

		if (theftBased == 1.0) {
			this.theftBased = true;
		} else {
			this.theftBased = false;
		}

		if (theftBasedAdvanced == 1.0) {
			this.theftBasedAdvanced = true;
		} else {
			this.theftBasedAdvanced = false;
		}
		
		if (samplePreferences == 1.0) {
			this.usepreferencesSampled = true;
		} else {
			this.usepreferencesSampled = false;
		}
		this.duplicateSegments=(duplicateSegments==1);
		this.demandSegmentWeightingOriginal=dsw;
	//Determine final demand segments and demand segment weighting
		
		if(this.duplicateSegments){
			this.mapOriginalSegmentToSubSegment= new  HashMap<Integer,Pair<Double, Pair<DemandSegment, DemandSegment>>> ();
			this.demandSegmentWeighting = new DemandSegmentWeighting();
			this.demandSegmentWeighting.setId(-1);
			DemandSegmentSet dss = ((DemandSegmentSet) this.demandSegmentWeightingOriginal.getSetEntity()).copyWithoutIdAndElements();
			dss.setId(-1);
			ArrayList<DemandSegment> newSegments = new ArrayList<DemandSegment>();
			ArrayList<DemandSegmentWeight> newWeights = new ArrayList<DemandSegmentWeight>();
			int initialId=-1;
			for(DemandSegmentWeight w : this.demandSegmentWeightingOriginal.getWeights()){
				DemandSegment ds = w.getDemandSegment();
				
				Double splitValue = null;
				try {
					splitValue=ProbabilityDistributionService.getXByCummulativeDistributionQuantile(ds.getBasketValueDistribution(), 0.5);
				} catch (ParameterUnknownException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				DemandSegment ds1= ds.copyWithoutId(-1);
				ds1.setId(initialId);
				ds1.setSetId(-1);
				ds1.setTempOriginalSegment(ds.getId());
				ds1.setSet(dss);
				DistributionParameterValue minPara = new DistributionParameterValue();
				minPara.setParameterTypeId(2);
				minPara.setValue(splitValue);
				ds1.getBasketValueDistribution().getParameterValues().add(minPara);
				newSegments.add(ds1);
				DemandSegmentWeight w1 = new DemandSegmentWeight();
				w1.setSetId(-1);
				w1.setDemandSegment(ds1);
				w1.setElementId(ds1.getId());
				w1.setWeight(w.getWeight()/2.0);
				w1.setId(initialId--);
				newWeights.add(w1);
				DemandSegment ds2= ds.copyWithoutId(-1);
				ds2.setId(initialId);
				ds2.setSet(dss);
				ds2.setSetId(-1);
				ds2.setTempOriginalSegment(ds.getId());
				DistributionParameterValue maxPara = new DistributionParameterValue();
				maxPara.setParameterTypeId(3);
				maxPara.setValue(splitValue);
				ds2.getBasketValueDistribution().getParameterValues().add(maxPara);
				newSegments.add(ds2);
				DemandSegmentWeight w2 = new DemandSegmentWeight();
				w2.setDemandSegment(ds2);
				w2.setWeight(w.getWeight()/2.0);
				w2.setId(initialId--);
				w2.setElementId(ds2.getId());
				w2.setSetId(-1);
				newWeights.add(w2);
				mapOriginalSegmentToSubSegment.put(ds.getId(), new Pair<Double, Pair<DemandSegment, DemandSegment>>(splitValue, new Pair<DemandSegment, DemandSegment>(ds2, ds1)));
				
			}
			dss.setElements(newSegments);
			this.demandSegmentWeighting.setWeights(newWeights);
			this.demandSegmentWeighting.setSetEntity(dss);
		}else{
			this.demandSegmentWeighting=this.demandSegmentWeightingOriginal;
		}
		
		this.daLowerWeights = new HashMap<DeliveryArea, Double>();
		this.daLowerSegmentWeightings = new HashMap<DeliveryArea, DemandSegmentWeighting>();
		LocationService.determineDeliveryAreaWeightAndDemandSegmentWeightingPerDeliveryAreaConsideringHierarchy(daLowerWeights,
				daLowerSegmentWeightings, deliveryAreaSet, demandSegmentWeighting);
		
	

	};

	public DeterministicProgrammingBasedAcceptance(Region region, int bookingHorizonLength,
			DeliveryAreaSet deliveryAreaSet, boolean samplePreferences, boolean theftBased, boolean theftBasedAdvanced,
			HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors, DemandSegmentWeighting dsw,
			HashMap<DeliveryArea, Double> daLowerWeights,
			HashMap<DeliveryArea, DemandSegmentWeighting> daLowerSegmentWeightings, HashMap<Integer,Pair<Double, Pair<DemandSegment, DemandSegment>>> mapOriginalSegmentToSubSegment , int arrivalProcessId, Double beta,
			double maximumRevenueValue, HashMap<Entity, Object> objectiveSpecificValues, AlternativeSet alternativeSet) {
	this.referenceInformationPrepared=true;
	this.dynamicFeasibilityCheck = false;
		this.theftBased = theftBased;
		this.theftBasedAdvanced = theftBasedAdvanced;
		this.usepreferencesSampled=samplePreferences;
		this.timeWindowSet = alternativeSet.getTimeWindowSet();
		this.alternativeSet = alternativeSet;
		this.daLowerWeights = daLowerWeights;
		this.demandSegmentWeighting=dsw;
		this.daLowerSegmentWeightings = daLowerSegmentWeightings;
		this.mapOriginalSegmentToSubSegment=mapOriginalSegmentToSubSegment;
		this.initialise(region, bookingHorizonLength, deliveryAreaSet, neighbors, 
				arrivalProcessId, beta, maximumRevenueValue,
				objectiveSpecificValues);

	};

	public void setOrderRequestSet(OrderRequestSet orderRequestSet) {
		this.orderRequestSet = orderRequestSet;
	}

	private void initialise(Region region, int bookingHorizonLength, DeliveryAreaSet deliveryAreaSet,
			HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors,
			 int arrivalProcessId, Double beta,
			double maximumRevenueValue, HashMap<Entity, Object> objectiveSpecificValues) {
		this.region = region;
		this.bookingHorizonLength = bookingHorizonLength;
		this.neighbors = neighbors;

		this.deliveryAreaSet = deliveryAreaSet;
		
		
		this.arrivalProcessId = arrivalProcessId;
		this.beta = beta;
		this.maximumRevenueValue = maximumRevenueValue;
		this.objectiveSpecificValues = objectiveSpecificValues;
		

	}

	public void start() {

		this.initialiseGlobal();

		// Sort order requests to arrive in time
		ArrayList<OrderRequest> relevantRequests = this.orderRequestSet.getElements();
		Collections.sort(relevantRequests, new OrderRequestArrivalTimeDescComparator());

		ArrayList<Order> orders = new ArrayList<Order>();

		HashMap<DeliveryArea, ArrayList<Order>> ordersPerDeliveryArea = new HashMap<DeliveryArea, ArrayList<Order>>();
		HashMap<DeliveryArea, ArrayList<Order>> acceptedOrdersPerDeliveryArea = new HashMap<DeliveryArea, ArrayList<Order>>();
		HashMap<DeliveryArea, HashMap<Integer, ArrayList<RouteElement>>> bestRoutingSoFarPerDeliveryArea = new HashMap<DeliveryArea, HashMap<Integer, ArrayList<RouteElement>>>();
		HashMap<DeliveryArea, Double> currentAcceptedTravelTimePerDeliveryArea = new HashMap<DeliveryArea, Double>();
		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			ordersPerDeliveryArea.put(area, new ArrayList<Order>());
			acceptedOrdersPerDeliveryArea.put(area, new ArrayList<Order>());
			bestRoutingSoFarPerDeliveryArea.put(area, new HashMap<Integer, ArrayList<RouteElement>>());
			currentAcceptedTravelTimePerDeliveryArea.put(area, null);
		}

		// Go through requests and update value function
		Order lastOrder = null;
		HashMap<TimeWindow, HashMap<Routing, DeliveryArea>> stealingAreaPerTimeWindowAndRouting = new HashMap<TimeWindow, HashMap<Routing, DeliveryArea>>();
		HashMap<TimeWindow, HashMap<Routing, ArrayList<DeliveryArea>>> advancedStealingAreasPerTimeWindowAndRouting = new HashMap<TimeWindow, HashMap<Routing, ArrayList<DeliveryArea>>>();

		for (OrderRequest request : relevantRequests) {

			// Determine respective delivery area
			DeliveryArea rArea = LocationService.assignCustomerToDeliveryAreaConsideringHierarchy(this.deliveryAreaSet,
					request.getCustomer());
			request.getCustomer().setTempDeliveryArea(rArea);
			DeliveryArea area = rArea.getDeliveryAreaOfSet();

			// Update assigned capacities and buffers to current t and TOPs
			DemandSegment currentSegment = null;
			if(lastOrder!=null) currentSegment=lastOrder.getOrderRequest().getCustomer().getOriginalDemandSegment();
			
			this.updateAssignedCapacitiesInformation(lastOrder, currentSegment, request.getArrivalTime(), this.arrivalProbability,
					this.theftBased, this.theftBasedAdvanced, this.assignedCapacitiesPerRouting.get(area),
					this.bufferPerRouting.get(area), stealingAreaPerTimeWindowAndRouting,
					advancedStealingAreasPerTimeWindowAndRouting, this.daLowerWeights, this.daLowerSegmentWeightings,
					this.demandMultiplierPerSegment, this.alternativesToTimeWindows2,
					this.alreadyAcceptedPerSubDeliveryAreaAndTimeWindow, this.openForAdvancedThefting.get(area), this.duplicateSegments, this.mapOriginalSegmentToSubSegment);

			

			ArrayList<HashMap<Integer, ArrayList<RouteElement>>> possibleRoutings = new ArrayList<HashMap<Integer, ArrayList<RouteElement>>>();
			HashMap<Integer, Pair<RouteElement, Double>> bestRoutingsValueAfterInsertion = new HashMap<Integer, Pair<RouteElement, Double>>();

			// Check aggregated orienteering based feasibility
			stealingAreaPerTimeWindowAndRouting = new HashMap<TimeWindow, HashMap<Routing, DeliveryArea>>();
			advancedStealingAreasPerTimeWindowAndRouting = new HashMap<TimeWindow, HashMap<Routing, ArrayList<DeliveryArea>>>();
			Set<TimeWindow> timeWindowsToOffer = new HashSet<TimeWindow>();
			currentSegment = request.getCustomer().getOriginalDemandSegment();
			
			DeterministicProgrammingBasedAcceptance.checkFeasibilityBasedOnAssignedCapacities(request, currentSegment, rArea,
					arrivalProcessId, assignedCapacitiesPerRouting.get(area), bufferPerRouting.get(area),
					timeWindowsToOffer, this.theftBased, this.theftBasedAdvanced,
					Math.max(this.assignedCapacitiesPerRouting.get(area).keySet().size(),
							bufferPerRouting.get(area).keySet().size()),
					this.alreadyAcceptedPerSubDeliveryAreaAndTimeWindow, neighbors, this.daLowerWeights,
					maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow, stealingAreaPerTimeWindowAndRouting,
					advancedStealingAreasPerTimeWindowAndRouting, this.openForAdvancedThefting.get(area), 
					this.duplicateSegments, this.mapOriginalSegmentToSubSegment);

			// Check feasible time windows and lowest insertion costs based
			// on dynamic routing
			HashMap<Integer, ArrayList<RouteElement>> bestRoutingSoFar=null;
			Double currentAcceptedTravelTime=null;
			if (this.dynamicFeasibilityCheck) {
				ArrayList<Order> relevantOrders = acceptedOrdersPerDeliveryArea.get(area);
				HashMap<Integer, VehicleAreaAssignment> relevantVaas = vasPerDeliveryAreaAndVehicleNo.get(area.getId());
				bestRoutingSoFar = bestRoutingSoFarPerDeliveryArea.get(area);
				 currentAcceptedTravelTime = currentAcceptedTravelTimePerDeliveryArea.get(area);
				ArrayList<TimeWindow> timeWindowsToOfferList = new ArrayList<TimeWindow>();
				timeWindowsToOfferList.addAll(timeWindowsToOffer);
				currentAcceptedTravelTime = DynamicRoutingHelperService
						.determineFeasibleTimeWindowsAndBestRoutingAfterInsertionBasedOnDynamicRoutingWithShiftWithoutWait(
								request, region, TIME_MULTIPLIER, this.timeWindowSet,
								(this.includeDriveFromStartingPosition == 1), this.expectedServiceTime,
								possibleRoutings, this.numberRoutingCandidates, this.numberInsertionCandidates,
								relevantVaas, relevantOrders, bestRoutingSoFar, currentAcceptedTravelTime,
								timeWindowsToOfferList, bestRoutingsValueAfterInsertion, numberOfThreads);
			} else {
				for (TimeWindow tw : timeWindowsToOffer) {
					// Insert dummy route elements with orienteering costs
					// and order
					RouteElement e = new RouteElement();
					Order order = new Order();
					order.setOrderRequestId(request.getId());
					order.setOrderRequest(request);
					order.setTimeWindowFinalId(tw.getId());
					e.setOrder(order);
					e.setTempAdditionalCostsValue(0.0);
					bestRoutingsValueAfterInsertion.put(tw.getId(), new Pair<RouteElement, Double>(e, 0.0));
				}
			}

			// Simulate customer decision
			Order order = new Order();
			order.setOrderRequest(request);
			order.setOrderRequestId(request.getId());
			order.setAccepted(false);

			if (bestRoutingsValueAfterInsertion.keySet().size() > 0) {

				CustomerDemandService.simulateCustomerDecision(order, bestRoutingsValueAfterInsertion.keySet(),
						orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getAlternativeSet(),
						alternativesToTimeWindows, this.usepreferencesSampled);

				if (order.getAccepted()) {
					int twId = order.getTimeWindowFinalId();
					int currentAccepted = this.alreadyAcceptedPerDeliveryArea.get(area.getId()).get(twId);
					this.alreadyAcceptedPerDeliveryArea.get(area.getId()).put(twId, ++currentAccepted);

					order.setTimeWindowFinalId(twId);
					order.setAccepted(true);

					// Choose best routing after insertion for the respective tw
					if (this.dynamicFeasibilityCheck) {
						RouteElement elementToInsert = bestRoutingsValueAfterInsertion.get(twId).getKey();

						int routingId = bestRoutingsValueAfterInsertion.get(twId).getKey().getTempRoutingId();
						DynamicRoutingHelperService.insertRouteElement(elementToInsert, possibleRoutings.get(routingId),
								vasPerDeliveryAreaAndVehicleNo.get(area.getId()), timeWindowSet, TIME_MULTIPLIER,
								(includeDriveFromStartingPosition == 1));
						bestRoutingSoFar = possibleRoutings.get(routingId);

						currentAcceptedTravelTime = bestRoutingsValueAfterInsertion.get(twId).getValue();

					}

				} else {

					order.setReasonRejection("Customer chose no-purchase option");
				}

			} else {

				order.setReasonRejection("No feasible, considered time windows");

			}

			if (this.dynamicFeasibilityCheck) {
				ordersPerDeliveryArea.get(area).add(order);
				// Update best and accepted travel time because it could change
				// also if no new order is accepted (better random solution)
				bestRoutingSoFarPerDeliveryArea.put(area, bestRoutingSoFar);
				currentAcceptedTravelTimePerDeliveryArea.put(area, currentAcceptedTravelTime);
				if (order.getAccepted()) {

					acceptedOrdersPerDeliveryArea.get(area).add(order);

				}
			}

			if (order.getAccepted()) {
				order.getOrderRequest().getCustomer().setTempDeliveryArea(rArea);
				lastOrder = order;
			} else {
				lastOrder = null;
			}
			orders.add(order);

		}

		this.orderSet = new OrderSet();
		System.out.println("number of stealings: " + this.stealingCounter);

		// Update buffers
		for (DeliveryArea area : this.assignedCapacitiesPerRouting.keySet()) {
			this.updateAssignedCapacitiesInformation(null, null, 0, this.arrivalProbability, this.theftBased,
					this.theftBasedAdvanced, this.assignedCapacitiesPerRouting.get(area),
					this.bufferPerRouting.get(area), stealingAreaPerTimeWindowAndRouting,
					advancedStealingAreasPerTimeWindowAndRouting, this.daLowerWeights, this.daLowerSegmentWeightings,
					this.demandMultiplierPerSegment, this.alternativesToTimeWindows2,
					this.alreadyAcceptedPerSubDeliveryAreaAndTimeWindow, this.openForAdvancedThefting.get(area), 
					this.duplicateSegments, this.mapOriginalSegmentToSubSegment);
		}
		ArrayList<ReferenceRouting> rrs = new ArrayList<ReferenceRouting>();
		for (DeliveryArea area : this.bufferPerRouting.keySet()) {

			for (Routing r : this.bufferPerRouting.get(area).keySet()) {

				int leftOver = 0;
				for (DeliveryArea subA : this.bufferPerRouting.get(area).get(r).keySet()) {
					for (TimeWindow tw : this.bufferPerRouting.get(area).get(r).get(subA).keySet()) {
						leftOver += this.bufferPerRouting.get(area).get(r).get(subA).get(tw);
					}
				}
				ReferenceRouting rr = new ReferenceRouting();
				rr.setDeliveryAreaId(area.getId());
				rr.setRoutingId(r.getId());
				rr.setRemainingCap(leftOver);
				rrs.add(rr);
			}
		}

		this.orderSet.setReferenceRoutingsPerDeliveryArea(rrs);
		this.orderSet.setOrderRequestSetId(this.orderRequestSet.getId());
		this.orderSet.setElements(orders);

	}

	private void initialiseGlobal() {

		
		
		// Map time windows to alternatives
		// TODO Consider that it only works with direct alternative-tw
		// assignments, not multiple ones
		this.alternativesToTimeWindows = new HashMap<Integer, Alternative>();
		this.alternativesToTimeWindows2 = new HashMap<TimeWindow, Alternative>();
		for (Alternative alt : this.alternativeSet.getElements()) {
			if (!alt.getNoPurchaseAlternative()) {
				alternativesToTimeWindows.put(alt.getTimeWindows().get(0).getId(), alt);
				alternativesToTimeWindows2.put(alt.getTimeWindows().get(0), alt);
			} else {
				this.noPurchaseAlternative = alt;
			}
		}

		// Maximum time frame and start time
		RoutingService.getPossibleDurationOfRouteByTimeWindowSet(this.timeWindowSet, TIME_MULTIPLIER);
		RoutingService.getDeliveryStartTimeByTimeWindowSet(this.timeWindowSet);
		if (!this.referenceInformationPrepared) {
			this.aggregatedReferenceInformationNo = DeterministicProgrammingBasedAcceptance
					.aggregateReferenceInformation(deliveryAreaSet, targetRoutingResults);
		}


		this.alreadyAcceptedPerDeliveryArea = new HashMap<Integer, HashMap<Integer, Integer>>();
		this.initialiseAlreadyAcceptedPerTimeWindow();
		if (this.theftBased) {
			this.alreadyAcceptedPerSubDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Integer>>();
			this.maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow = CustomerDemandService
					.determineMaximumExpectedMultiplierPerDeliveryAreaAndTimeWindow(daLowerWeights,
							daLowerSegmentWeightings, timeWindowSet);
			this.minimumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow = CustomerDemandService
					.determineMinimumExpectedMultiplierPerDeliveryAreaAndTimeWindow(daLowerWeights,
							daLowerSegmentWeightings, timeWindowSet);
		}
		this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow = CustomerDemandService
				.determineMaximumProbabilityPerDemandSegmentAndTimeWindow(this.demandSegmentWeighting, timeWindowSet);
		this.minimumExpectedMultiplierPerDemandSegmentAndTimeWindow = CustomerDemandService
				.determineMinimumProbabilityPerDemandSegmentAndTimeWindow(this.demandSegmentWeighting, timeWindowSet);
		this.demandMultiplierPerSegment = new HashMap<DemandSegment, HashMap<Alternative, Double>>();
		DemandSegmentSet dss = (DemandSegmentSet) this.demandSegmentWeighting.getSetEntity();
		for (Integer dsId : this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow.keySet()) {
			DemandSegment ds = dss.getDemandSegmentById(dsId);
			this.demandMultiplierPerSegment.put(ds, new HashMap<Alternative, Double>());
			double minimumNoPurchaseProb = 1.0;
			for (Integer twId : this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow.get(dsId).keySet()) {
				this.demandMultiplierPerSegment.get(ds).put(this.alternativesToTimeWindows.get(twId),
						this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow.get(dsId).get(twId) * this.beta
								+ this.minimumExpectedMultiplierPerDemandSegmentAndTimeWindow.get(dsId).get(twId)
										* (1.0 - this.beta));
				minimumNoPurchaseProb = minimumNoPurchaseProb
						- this.minimumExpectedMultiplierPerDemandSegmentAndTimeWindow.get(dsId).get(twId);
			}
			this.demandMultiplierPerSegment.get(ds).put(this.noPurchaseAlternative, minimumNoPurchaseProb);
		}
		if(dynamicFeasibilityCheck){
			this.prepareVehicleAssignmentsAndModelsForDeliveryAreas();
		}
		
		
		// Go through aggregated routings and determine capacity per demand
		// segment
		this.arrivalProbability = ArrivalProcessService.getMeanArrivalProbability(arrivalProcessId);
		this.expectedArrivalsOverall = ArrivalProcessService.getMeanArrivalProbability(arrivalProcessId)
				* this.bookingHorizonLength;

		if (!this.referenceInformationPrepared) {
			Pair<HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, HashMap<DemandSegment, HashMap<Integer, Double>>>>>>, HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>>> r = DeterministicProgrammingBasedAcceptance
					.determineCapacityAssignments(aggregatedReferenceInformationNo, daLowerWeights,
							daLowerSegmentWeightings, demandMultiplierPerSegment, expectedArrivalsOverall,
							maximumRevenueValue, objectiveSpecificValues, timeWindowSet, connection,
							noPurchaseAlternative, alternativesToTimeWindows2);

			assignedCapacitiesPerRouting = r.getKey();
			bufferPerRouting = r.getValue();
		}

		openForAdvancedThefting = new HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Double>>>>();
		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			openForAdvancedThefting.put(area,
					new HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Double>>>());
		}
		
		
		
	}

	public static Pair<HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, HashMap<DemandSegment, HashMap<Integer, Double>>>>>>, HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>>> determineCapacityAssignments(
			HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>> aggregatedReferenceInformationNo,
			HashMap<DeliveryArea, Double> daLowerWeights,
			HashMap<DeliveryArea, DemandSegmentWeighting> daLowerSegmentWeightings,
			HashMap<DemandSegment, HashMap<Alternative, Double>> demandMultiplierPerSegment,
			double expectedArrivalsOverall, double maximumRevenueValue, HashMap<Entity, Object> objectiveSpecificValues,
			TimeWindowSet timeWindowSet, RConnection connection, Alternative noPurchaseAlternative,
			HashMap<TimeWindow, Alternative> alternativesToTimeWindows) {

		HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, HashMap<DemandSegment, HashMap<Integer, Double>>>>>> assignedCapacitiesPerRouting = new HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, HashMap<DemandSegment, HashMap<Integer, Double>>>>>>();
		HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>> bufferPerRouting = new HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>>();

		try {
			connection = new RConnection();
			connection.eval("library(lpSolve)");
		} catch (RserveException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Per TOP and subarea: determine assignments by deterministic
		// programming
		for (DeliveryArea area : aggregatedReferenceInformationNo.keySet()) {
			assignedCapacitiesPerRouting.put(area,
					new HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, HashMap<DemandSegment, HashMap<Integer, Double>>>>>());
			bufferPerRouting.put(area, new HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>());
			for (Routing r : aggregatedReferenceInformationNo.get(area).keySet()) {
				assignedCapacitiesPerRouting.get(area).put(r,
						new HashMap<DeliveryArea, HashMap<TimeWindow, HashMap<DemandSegment, HashMap<Integer, Double>>>>());
				bufferPerRouting.get(area).put(r, new HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>());
				for (DeliveryArea subArea : aggregatedReferenceInformationNo.get(area).get(r).keySet()) {
					bufferPerRouting.get(area).get(r).put(subArea, new HashMap<TimeWindow, Integer>());
					HashMap<Integer, Integer> capacityPerTw = new HashMap<Integer, Integer>();
					for (TimeWindow tw : aggregatedReferenceInformationNo.get(area).get(r).get(subArea).keySet()) {
						capacityPerTw.put(tw.getId(),
								aggregatedReferenceInformationNo.get(area).get(r).get(subArea).get(tw));
					}

					try {
						HashMap<TimeWindow, HashMap<DemandSegment, HashMap<Integer, Double>>> assignedCapacitiesPerTimeWindowSegmentAndSubSegment = LinearProgrammingService
								.determineDeterministicLinearProgrammingSolutionPerDeliveryAreaWithR(
										daLowerSegmentWeightings.get(subArea), capacityPerTw,
										demandMultiplierPerSegment,
										(double) expectedArrivalsOverall * daLowerWeights.get(subArea),
										maximumRevenueValue, objectiveSpecificValues, timeWindowSet, connection, 0.5,
										alternativesToTimeWindows, noPurchaseAlternative, true, false)
								.getValue();
						assignedCapacitiesPerRouting.get(area).get(r).put(subArea,
								assignedCapacitiesPerTimeWindowSegmentAndSubSegment);

						// Check if whole capacity is used
						for (TimeWindow tw : assignedCapacitiesPerTimeWindowSegmentAndSubSegment.keySet()) {
							if (capacityPerTw != null && capacityPerTw.containsKey(tw.getId())) {
								int overallAssigned = 0;
								for (DemandSegment ds : assignedCapacitiesPerRouting.get(area).get(r).get(subArea)
										.get(tw).keySet()) {
									overallAssigned += assignedCapacitiesPerRouting.get(area).get(r).get(subArea)
											.get(tw).get(ds).get(0);
								}
								bufferPerRouting.get(area).get(r).get(subArea).put(tw,
										Math.max(capacityPerTw.get(tw.getId()) - overallAssigned, 0));
							}
						}

					} catch (RserveException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (REXPMismatchException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		
		connection.close();
		return new Pair<HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, HashMap<DemandSegment, HashMap<Integer, Double>>>>>>, HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>>>(
				assignedCapacitiesPerRouting, bufferPerRouting);
	}

	private void prepareVehicleAssignmentsAndModelsForDeliveryAreas() {

		// Initialise Hashmap
		this.vehicleAssignmentsPerDeliveryArea = new HashMap<Integer, ArrayList<VehicleAreaAssignment>>();
		this.vasPerDeliveryAreaAndVehicleNo = new HashMap<Integer, HashMap<Integer, VehicleAreaAssignment>>();
		this.overallCapacityPerDeliveryArea = new HashMap<Integer, Double>();
		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			this.vehicleAssignmentsPerDeliveryArea.put(area.getId(), new ArrayList<VehicleAreaAssignment>());
			this.vasPerDeliveryAreaAndVehicleNo.put(area.getId(), new HashMap<Integer, VehicleAreaAssignment>());
			this.overallCapacityPerDeliveryArea.put(area.getId(), 0.0);
		}

		for (VehicleAreaAssignment ass : this.vehicleAreaAssignmentSet.getElements()) {
			this.vehicleAssignmentsPerDeliveryArea.get(ass.getDeliveryAreaId()).add(ass);
			this.vasPerDeliveryAreaAndVehicleNo.get(ass.getDeliveryAreaId()).put(ass.getVehicleNo(), ass);
			double capacity = this.overallCapacityPerDeliveryArea.get(ass.getDeliveryAreaId());
			capacity += (ass.getEndTime() - ass.getStartTime()) * TIME_MULTIPLIER + (this.expectedServiceTime - 1);
			// TODO: Check if depot travel time should be included
			this.overallCapacityPerDeliveryArea.put(ass.getDeliveryAreaId(), capacity);

		}

	}

	
	/**
	 * Determine average distances or number of accepted customers for the
	 * orienteering results
	 */
	public static HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>> aggregateReferenceInformation(
			DeliveryAreaSet deliveryAreaSet, ArrayList<Routing> targetRoutingResults) {

		HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>> aggregatedReferenceInformationNo = new HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>>();

		// TA: consider that maximal acceptable costs is initialised too low

		for (DeliveryArea area : deliveryAreaSet.getElements()) {

			aggregatedReferenceInformationNo.put(area,
					new HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>());

		}

		for (Routing routing : targetRoutingResults) {
			HashMap<DeliveryArea, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> count = new HashMap<DeliveryArea, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>();

			for (Route r : routing.getRoutes()) {

				DeliveryArea area = LocationService.assignCustomerToDeliveryArea(deliveryAreaSet,
						r.getRouteElements().get(1).getOrder().getOrderRequest().getCustomer());
				if (!count.containsKey(area)) {
					count.put(area, new HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>());

				}
				for (int reId = 0; reId < r.getRouteElements().size(); reId++) {
					RouteElement re = r.getRouteElements().get(reId);
					double travelTimeFrom;
					if (reId < r.getRouteElements().size() - 1) {
						travelTimeFrom = r.getRouteElements().get(reId + 1).getTravelTime();
					} else {
						travelTimeFrom = LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(
								re.getOrder().getOrderRequest().getCustomer().getLat(),
								re.getOrder().getOrderRequest().getCustomer().getLon(),
								r.getVehicleAssignment().getEndingLocationLat(),
								r.getVehicleAssignment().getEndingLocationLon());
					}
					re.setTravelTimeFrom(travelTimeFrom);
					Customer cus = re.getOrder().getOrderRequest().getCustomer();
					DeliveryArea subArea = LocationService
							.assignCustomerToDeliveryAreaConsideringHierarchy(deliveryAreaSet, cus);
					if (!count.get(area).containsKey(subArea)) {
						count.get(area).put(subArea, new HashMap<TimeWindow, Integer>());

					}
					if (!count.get(area).get(subArea).containsKey(re.getOrder().getTimeWindowFinal())) {
						count.get(area).get(subArea).put(re.getOrder().getTimeWindowFinal(), 1);

					} else {
						count.get(area).get(subArea).put(re.getOrder().getTimeWindowFinal(),
								count.get(area).get(subArea).get(re.getOrder().getTimeWindowFinal()) + 1);

					}
				}
			}

			for (DeliveryArea a : aggregatedReferenceInformationNo.keySet()) {
				aggregatedReferenceInformationNo.get(a).put(routing, count.get(a));

			}

		}
		return aggregatedReferenceInformationNo;
	}

	private static void checkFeasibilityBasedOnAssignedCapacities(OrderRequest request, DemandSegment segmentOriginal, DeliveryArea subArea,
			int arrivalProcessId,
			HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, HashMap<DemandSegment, HashMap<Integer, Double>>>>> assignedCapacitiesPerRouting,
			HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> bufferPerRouting,
			Set<TimeWindow> feasibleTimeWindows, boolean thefting, boolean theftingAdvanced, int currentNumberOfTOPs,
			HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerSubDeliveryAreaAndTimeWindow,
			HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors, HashMap<DeliveryArea, Double> daLowerWeights,
			HashMap<DeliveryArea, HashMap<TimeWindow, Double>> maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow,
			HashMap<TimeWindow, HashMap<Routing, DeliveryArea>> stealingAreaPerTimeWindowAndRouting,
			HashMap<TimeWindow, HashMap<Routing, ArrayList<DeliveryArea>>> advancedStealingAreasPerTimeWindowAndRouting,
			HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Double>>> openForAdvancedThefting, boolean duplicateSegments, HashMap<Integer, Pair<Double, Pair<DemandSegment, DemandSegment>>> mapOriginalSegmentToSubsegment) {
		
		DemandSegment segment=segmentOriginal;
		DemandSegment segmentLower = null;
		if(duplicateSegments) {
			Pair<Double, Pair<DemandSegment, DemandSegment>> valSegs= mapOriginalSegmentToSubsegment.get(segment.getId());
			if(request.getBasketValue()>valSegs.getKey()){
				segment=valSegs.getValue().getValue();
				segmentLower=valSegs.getValue().getKey();
			}else{
				segment=valSegs.getValue().getKey();
			}
		}
		
		// Possible time windows for request
		ArrayList<TimeWindow> timeWindows = new ArrayList<TimeWindow>();
		ArrayList<ConsiderationSetAlternative> alternatives = segment
				.getConsiderationSet();
		for (ConsiderationSetAlternative alt : alternatives) {
			if (!alt.getAlternative().getNoPurchaseAlternative())
				timeWindows.addAll(alt.getAlternative().getTimeWindows());
		}

		ArrayList<TimeWindow> timeWindowsLowerSegment = new ArrayList<TimeWindow>(); 
		
		for (Routing r : assignedCapacitiesPerRouting.keySet()) {
			for (TimeWindow tw : timeWindows) {
				boolean capacityTaken=false;
				if (assignedCapacitiesPerRouting.get(r).containsKey(subArea)
						&& assignedCapacitiesPerRouting.get(r).get(subArea).containsKey(tw)
						&& assignedCapacitiesPerRouting.get(r).get(subArea).get(tw)
								.containsKey(segment)
						&& assignedCapacitiesPerRouting.get(r).get(subArea).get(tw)
								.get(segment).get(0) > 0) {
					feasibleTimeWindows.add(tw);
					capacityTaken=true;
				} else if (bufferPerRouting.get(r).containsKey(subArea)
						&& bufferPerRouting.get(r).get(subArea).containsKey(tw)
						&& bufferPerRouting.get(r).get(subArea).get(tw) > 0) {
					feasibleTimeWindows.add(tw);
					capacityTaken=true;
				} else if (thefting && alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.containsKey(subArea.getId())
						&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()).containsKey(tw.getId())
						&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()).get(tw.getId()) > 0) {

					// Find area with lowest demand-capacity ratio for stealing
					double lowestRatio = Double.MAX_VALUE;
					DeliveryArea areaWithLowestRatio = null;
					DeliveryArea areaWithBuffer = null;
					for (DeliveryArea neighbor : neighbors.get(subArea)) {

						if (bufferPerRouting.get(r).containsKey(neighbor)
								&& bufferPerRouting.get(r).get(neighbor).containsKey(tw)
								&& bufferPerRouting.get(r).get(neighbor).get(tw) > 0) {
							areaWithBuffer = neighbor;
							break;
						}

						if (assignedCapacitiesPerRouting.get(r).containsKey(neighbor)
								&& assignedCapacitiesPerRouting.get(r).get(neighbor).containsKey(tw)
								&& assignedCapacitiesPerRouting.get(r).get(neighbor).get(tw)
										.containsKey(segment)
								&& assignedCapacitiesPerRouting.get(r).get(neighbor).get(tw)
										.get(segment).get(0) > 0) {

							double arrivalsInArea = (request.getArrivalTime() - 1.0)
									* ArrivalProcessService.getMeanArrivalProbability(arrivalProcessId)
									* daLowerWeights.get(neighbor);

							double requestedForTimeWindow = arrivalsInArea
									* maximumExpectedMultiplierPerLowerDeliveryAreaAndTimeWindow.get(neighbor).get(tw);

							// Best result?
							double ratio = requestedForTimeWindow
									/ ((double) assignedCapacitiesPerRouting.get(r).get(neighbor).get(tw)
											.get(segment).get(0));
							if (ratio < lowestRatio) {
								areaWithLowestRatio = neighbor;
								lowestRatio = ratio;
							}
						}
					}

					// If there is an area to steal from, then we save it for
					// potential updates later
					if (!stealingAreaPerTimeWindowAndRouting.containsKey(tw)) {
						stealingAreaPerTimeWindowAndRouting.put(tw, new HashMap<Routing, DeliveryArea>());
					}

					if (areaWithBuffer != null) {
						stealingAreaPerTimeWindowAndRouting.get(tw).put(r, areaWithBuffer);
						feasibleTimeWindows.add(tw);
						capacityTaken=true;
					} else if (areaWithLowestRatio != null) {
						stealingAreaPerTimeWindowAndRouting.get(tw).put(r, areaWithLowestRatio);
						feasibleTimeWindows.add(tw);
						capacityTaken=true;
					}
				} else if (theftingAdvanced && currentNumberOfTOPs < 2) {

					// Look for two areas with buffers
					ArrayList<DeliveryArea> bestAreas = new ArrayList<DeliveryArea>();

					for (int neighborId = 0; neighborId < neighbors.get(subArea).size(); neighborId++) {

						DeliveryArea neighbor = neighbors.get(subArea).get(neighborId);

						if (bestAreas.size() > 0)
							break;

						if (bufferPerRouting.get(r).containsKey(neighbor)
								&& bufferPerRouting.get(r).get(neighbor).containsKey(tw)
								&& bufferPerRouting.get(r).get(neighbor).get(tw) > 1
								&& openForAdvancedThefting.containsKey(r)
								&& openForAdvancedThefting.get(r).containsKey(neighbor)
								&& openForAdvancedThefting.get(r).get(neighbor).containsKey(tw)
								&& openForAdvancedThefting.get(r).get(neighbor).get(tw) > 1) {

							bestAreas.clear();
							bestAreas.add(neighbor);
							break;

						}

						if ((neighborId < neighbors.get(subArea).size() - 1)
								&& bufferPerRouting.get(r).containsKey(neighbor)
								&& bufferPerRouting.get(r).get(neighbor).containsKey(tw)
								&& bufferPerRouting.get(r).get(neighbor).get(tw) > 0
								&& openForAdvancedThefting.containsKey(r)
								&& openForAdvancedThefting.get(r).containsKey(neighbor)
								&& openForAdvancedThefting.get(r).get(neighbor).containsKey(tw)
								&& openForAdvancedThefting.get(r).get(neighbor).get(tw) > 0) {

							for (int neighborId2 = neighborId + 1; neighborId2 < neighbors.get(subArea)
									.size(); neighborId2++) {
								DeliveryArea neighbor2 = neighbors.get(subArea).get(neighborId2);

								if (bufferPerRouting.get(r).containsKey(neighbor2)
										&& bufferPerRouting.get(r).get(neighbor2).containsKey(tw)
										&& bufferPerRouting.get(r).get(neighbor2).get(tw) > 0
										&& openForAdvancedThefting.containsKey(r)
										&& openForAdvancedThefting.get(r).containsKey(neighbor2)
										&& openForAdvancedThefting.get(r).get(neighbor2).containsKey(tw)
										&& openForAdvancedThefting.get(r).get(neighbor2).get(tw) > 0) {

									bestAreas.clear();
									bestAreas.add(neighbor);
									bestAreas.add(neighbor2);
									break;
								}
							}

						}

					}
					// If there is an area to steal from, then save areas for
					// later udpates
					if (!advancedStealingAreasPerTimeWindowAndRouting.containsKey(tw)) {
						advancedStealingAreasPerTimeWindowAndRouting.put(tw,
								new HashMap<Routing, ArrayList<DeliveryArea>>());
					}
					if (bestAreas.size() > 0) {
						advancedStealingAreasPerTimeWindowAndRouting.get(tw).put(r, bestAreas);
						feasibleTimeWindows.add(tw);
						capacityTaken=true;
					}
				}
				
				if (!capacityTaken && segmentLower!=null && assignedCapacitiesPerRouting.get(r).containsKey(subArea)
						&& assignedCapacitiesPerRouting.get(r).get(subArea).containsKey(tw)
						&& assignedCapacitiesPerRouting.get(r).get(subArea).get(tw)
								.containsKey(segmentLower)
						&& assignedCapacitiesPerRouting.get(r).get(subArea).get(tw)
								.get(segmentLower).get(0) > 0) {
					timeWindowsLowerSegment.add(tw);
			
				} 
			}
		}
		
		if(timeWindows.size()==0){
			timeWindows.addAll(timeWindowsLowerSegment);
		}
	}

	private void updateAssignedCapacitiesInformation(Order order, DemandSegment segmentOriginal, int t, double arrivalProbability, boolean thefting,
			boolean theftingAdvanced,
			HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, HashMap<DemandSegment, HashMap<Integer, Double>>>>> assignedCapacitiesPerRouting,
			HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> bufferPerRouting,
			HashMap<TimeWindow, HashMap<Routing, DeliveryArea>> stealingAreaPerTimeWindowAndRouting,
			HashMap<TimeWindow, HashMap<Routing, ArrayList<DeliveryArea>>> advancedStealingAreasPerTimeWindowAndRouting,
			HashMap<DeliveryArea, Double> daLowerWeights,
			HashMap<DeliveryArea, DemandSegmentWeighting> daLowerSegmentWeightings,
			HashMap<DemandSegment, HashMap<Alternative, Double>> demandMultiplierPerSegment,
			HashMap<TimeWindow, Alternative> alternativesToTimeWindows,
			HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerSubDeliveryAreaAndTimeWindow,
			HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Double>>> openForAdvancedThefting, boolean duplicateSegments, 
			HashMap<Integer, Pair<Double, Pair<DemandSegment, DemandSegment>>> mapOriginalSegmentToSubsegment) {

		DemandSegment segment = segmentOriginal;
		DemandSegment lowerSegment = null;
		if(duplicateSegments &&order!=null) {
			Pair<Double, Pair<DemandSegment, DemandSegment>> valSegs= this.mapOriginalSegmentToSubSegment.get(segment.getId());
			if(order.getOrderRequest().getBasketValue()>valSegs.getKey()){
				segment=valSegs.getValue().getValue();
				lowerSegment = valSegs.getValue().getKey();
			}else{
				segment=valSegs.getValue().getKey();
			}
		}
		
		DeliveryArea subArea = null;
		if (order != null) {
			subArea = order.getOrderRequest().getCustomer().getTempDeliveryArea();
		}
		ArrayList<Routing> toRemove = new ArrayList<Routing>();
		for (Routing r : assignedCapacitiesPerRouting.keySet()) {
			HashMap<DeliveryArea, HashMap<TimeWindow, HashMap<DemandSegment, HashMap<Integer, Double>>>> cap = assignedCapacitiesPerRouting
					.get(r);
			// If order is null, then there was no acceptance after last update
			// and the capacities do not have to be reduced
			boolean remove = false;
			if (order != null) {

				boolean capacityTaken = false;
				// First check if capacity is available for the fitting area and
				// segment
				if (cap.containsKey(subArea) && cap.get(subArea).containsKey(order.getTimeWindowFinal())
						&& cap.get(subArea).get(order.getTimeWindowFinal())
								.containsKey(segment)) {
					Double currentCap = cap.get(subArea).get(order.getTimeWindowFinal())
							.get(segment).get(0);
					if (currentCap > 0) {
						cap.get(subArea).get(order.getTimeWindowFinal())
								.get(segment)
								.put(0, currentCap - 1);
						capacityTaken = true;
					}
				}

				// Is there left-over capacity in the buffer?
				if (!capacityTaken && bufferPerRouting.containsKey(r) && bufferPerRouting.get(r).containsKey(subArea)
						&& bufferPerRouting.get(r).get(subArea).containsKey(order.getTimeWindowFinal())) {
					Integer currentBuffer = bufferPerRouting.get(r).get(subArea).get(order.getTimeWindowFinal());
					if (currentBuffer > 0) {
						bufferPerRouting.get(r).get(subArea).put(order.getTimeWindowFinal(),
								bufferPerRouting.get(r).get(subArea).get(order.getTimeWindowFinal()) - 1);
						capacityTaken = true;
					}

				}

				// Already accepted in this area? -> can steal
				// from neighbors
				if (!capacityTaken && thefting
						&& stealingAreaPerTimeWindowAndRouting.containsKey(order.getTimeWindowFinal())
						&& stealingAreaPerTimeWindowAndRouting.get(order.getTimeWindowFinal()).containsKey(r)
						&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.containsKey(subArea.getId())
						&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId())
								.containsKey(order.getTimeWindowFinal().getId())
						&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId())
								.get(order.getTimeWindowFinal().getId()) > 0) {

					// Can you steal from buffer? If not, steal from
					// segment-capacity
					int bufferValue = bufferPerRouting.get(r)
							.get(stealingAreaPerTimeWindowAndRouting.get(order.getTimeWindowFinal()).get(r))
							.get(order.getTimeWindowFinal());
					if (bufferValue > 0) {
						bufferPerRouting.get(r)
								.get(stealingAreaPerTimeWindowAndRouting.get(order.getTimeWindowFinal()).get(r))
								.put(order.getTimeWindowFinal(), bufferValue - 1);
					} else {
						cap.get(stealingAreaPerTimeWindowAndRouting
								.get(order
										.getTimeWindowFinal())
								.get(r)).get(order.getTimeWindowFinal())
								.get(segment)
								.put(0, cap
										.get(stealingAreaPerTimeWindowAndRouting.get(order.getTimeWindowFinal()).get(r))
										.get(order.getTimeWindowFinal())
										.get(segment).get(0)
										- 1);

					}

					capacityTaken = true;

				} else if (!capacityTaken && theftingAdvanced
						&& advancedStealingAreasPerTimeWindowAndRouting.containsKey(order.getTimeWindowFinal())
						&& advancedStealingAreasPerTimeWindowAndRouting.get(order.getTimeWindowFinal())
								.containsKey(r)) {

					if (advancedStealingAreasPerTimeWindowAndRouting.get(order.getTimeWindowFinal()).get(r)
							.size() > 1) {
						bufferPerRouting.get(r)
								.get(advancedStealingAreasPerTimeWindowAndRouting.get(order.getTimeWindowFinal()).get(r)
										.get(0))
								.put(order.getTimeWindowFinal(),
										bufferPerRouting.get(r)
												.get(advancedStealingAreasPerTimeWindowAndRouting
														.get(order.getTimeWindowFinal()).get(r).get(0))
												.get(order.getTimeWindowFinal()) - 1);
						bufferPerRouting.get(r)
								.get(advancedStealingAreasPerTimeWindowAndRouting.get(order.getTimeWindowFinal()).get(r)
										.get(1))
								.put(order.getTimeWindowFinal(),
										bufferPerRouting.get(r)
												.get(advancedStealingAreasPerTimeWindowAndRouting
														.get(order.getTimeWindowFinal()).get(r).get(1))
												.get(order.getTimeWindowFinal()) - 1);
					} else {
						bufferPerRouting.get(r)
								.get(advancedStealingAreasPerTimeWindowAndRouting.get(order.getTimeWindowFinal()).get(r)
										.get(0))
								.put(order.getTimeWindowFinal(),
										bufferPerRouting.get(r)
												.get(advancedStealingAreasPerTimeWindowAndRouting
														.get(order.getTimeWindowFinal()).get(r).get(0))
												.get(order.getTimeWindowFinal()) - 2);
					}
					capacityTaken = true;
				}
				
				if (!capacityTaken && lowerSegment!=null && cap.containsKey(subArea) && cap.get(subArea).containsKey(order.getTimeWindowFinal())
						&& cap.get(subArea).get(order.getTimeWindowFinal())
								.containsKey(lowerSegment)) {
					Double currentCap = cap.get(subArea).get(order.getTimeWindowFinal())
							.get(lowerSegment).get(0);
					if (currentCap > 0) {
						cap.get(subArea).get(order.getTimeWindowFinal())
								.get(lowerSegment)
								.put(0, currentCap - 1);
						capacityTaken = true;
					}
				}

				if (!capacityTaken) {

					toRemove.add(r);
					remove = true;
				}
			}

			if (!remove) {
				// Update of buffers
				for (DeliveryArea a : cap.keySet()) {
					for (TimeWindow tw : cap.get(a).keySet()) {
						double expectedArrivalsTw = 0;
						int overallCap = 0;
						for (DemandSegmentWeight dsw : daLowerSegmentWeightings.get(a).getWeights()) {

							if (demandMultiplierPerSegment.get(dsw.getDemandSegment())
									.containsKey(alternativesToTimeWindows.get(tw))) {
								double expectedArrivals = t * arrivalProbability * daLowerWeights.get(a)
										* dsw.getWeight() * demandMultiplierPerSegment.get(dsw.getDemandSegment())
												.get(alternativesToTimeWindows.get(tw));
								expectedArrivalsTw += expectedArrivals;
								if (cap.get(a).get(tw).containsKey(dsw.getDemandSegment())) {

									int expectedArrivalsInt = (int) Math.round(expectedArrivals);
									if (expectedArrivalsInt > expectedArrivals)
										expectedArrivalsInt--;
									int overhang = (int) (cap.get(a).get(tw).get(dsw.getDemandSegment()).get(0)
											- expectedArrivalsInt);
									if (overhang > 0) {

										if (!bufferPerRouting.containsKey(r)) {
											bufferPerRouting.put(r,
													new HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>());

										}
										if (!bufferPerRouting.get(r).containsKey(a)) {
											bufferPerRouting.get(r).put(a, new HashMap<TimeWindow, Integer>());

										}
										if (!bufferPerRouting.get(r).get(a).containsKey(tw)) {
											bufferPerRouting.get(r).get(a).put(tw, 0);

										}

										bufferPerRouting.get(r).get(a).put(tw,
												bufferPerRouting.get(r).get(a).get(tw) + overhang);
										cap.get(a).get(tw).get(dsw.getDemandSegment()).put(0,
												cap.get(a).get(tw).get(dsw.getDemandSegment()).get(0) - overhang);
										overallCap += cap.get(a).get(tw).get(dsw.getDemandSegment()).get(0);
									}
									;
								}
							}
						}

						if (bufferPerRouting.containsKey(r) && bufferPerRouting.get(r).containsKey(a)
								&& bufferPerRouting.get(r).get(a).containsKey(tw)) {
							overallCap += bufferPerRouting.get(r).get(a).get(tw);

						}

						if (!openForAdvancedThefting.containsKey(r)) {
							openForAdvancedThefting.put(r, new HashMap<DeliveryArea, HashMap<TimeWindow, Double>>());
						}

						if (!openForAdvancedThefting.get(r).containsKey(subArea)) {
							openForAdvancedThefting.get(r).put(a, new HashMap<TimeWindow, Double>());
						}
						if (expectedArrivalsTw < overallCap) {

							openForAdvancedThefting.get(r).get(a).put(tw,
									(double) Math.round(overallCap - expectedArrivalsTw));
						} else {
							openForAdvancedThefting.get(r).get(a).put(tw, 0.0);
						}
					}
				}
			}
		}

		for (Routing r : toRemove) {
			assignedCapacitiesPerRouting.remove(r);
			bufferPerRouting.remove(r);
		}

		if (this.theftBased && order != null) {
			if (!this.alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.containsKey(subArea.getId())) {
				this.alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.put(subArea.getId(),
						new HashMap<Integer, Integer>());
			}
			if (!this.alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId())
					.containsKey(order.getTimeWindowFinalId())) {
				this.alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId())
						.put(order.getTimeWindowFinalId(), 0);
			}
			this.alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()).put(order.getTimeWindowFinalId(),
					this.alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId())
							.get(order.getTimeWindowFinalId()) + 1);
		}
	}

	private void initialiseAlreadyAcceptedPerTimeWindow() {

		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			HashMap<Integer, Integer> acceptedPerTw = new HashMap<Integer, Integer>();
			for (TimeWindow tw : this.timeWindowSet.getElements()) {
				acceptedPerTw.put(tw.getId(), 0);
			}
			this.alreadyAcceptedPerDeliveryArea.put(area.getId(), acceptedPerTw);
		}
	}

	public static String[] getParameterSetting() {

		return paras;
	}

	public OrderSet getResult() {

		return this.orderSet;
	}

	public boolean isReferenceInformationPrepared() {
		return referenceInformationPrepared;
	}

	public void setReferenceInformationPrepared(boolean referenceInformationPrepared) {
		this.referenceInformationPrepared = referenceInformationPrepared;
	}

	public void setAggregatedReferenceInformationNo(
			HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>> aggregatedReferenceInformationNo) {
		this.aggregatedReferenceInformationNo = aggregatedReferenceInformationNo;
	}

	public void setAssignedCapacitiesPerRouting(
			HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, HashMap<DemandSegment, HashMap<Integer, Double>>>>>> assignedCapacitiesPerRouting) {
		this.assignedCapacitiesPerRouting = assignedCapacitiesPerRouting;
	}

	public void setBufferPerRouting(
			HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>> bufferPerRouting) {
		this.bufferPerRouting = bufferPerRouting;
	}
	

}
