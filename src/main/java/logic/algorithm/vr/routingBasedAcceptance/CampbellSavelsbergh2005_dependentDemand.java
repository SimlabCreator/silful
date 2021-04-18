package logic.algorithm.vr.routingBasedAcceptance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

import data.entity.Alternative;
import data.entity.AlternativeOffer;
import data.entity.AlternativeSet;
import data.entity.ConsiderationSetAlternative;
import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.DemandSegment;
import data.entity.DemandSegmentSet;
import data.entity.DemandSegmentWeighting;
import data.entity.Order;
import data.entity.OrderRequest;
import data.entity.OrderRequestSet;
import data.entity.OrderSet;
import data.entity.ProbabilityDistribution;
import data.entity.Region;
import data.entity.Route;
import data.entity.RouteElement;
import data.entity.Routing;
import data.entity.TimeWindow;
import data.entity.TimeWindowSet;
import data.entity.VehicleAreaAssignment;
import data.entity.VehicleAreaAssignmentSet;
import logic.algorithm.vr.RoutingAlgorithm;
import logic.process.support.DataGenerationOrderRequestsII;
import logic.service.support.CustomerDemandService;
import logic.service.support.DynamicRoutingHelperService;
import logic.service.support.LocationService;
import logic.service.support.ProbabilityDistributionService;
import logic.utility.comparator.ObjectValuePairDoubleValueDescComparator;
import logic.utility.comparator.OrderRequestArrivalTimeDescComparator;
import logic.utility.comparator.RouteElementProfitDescComparator;
import logic.utility.exceptions.ParameterUnknownException;

/*
* Based on Campbell Savelsbergh 2005 but with dependent demand
* Campbell, A. M., & Savelsbergh, M. W. (2005). Decision support for consumer direct grocery initiatives. Transportation Science, 39(3), 313-327.
 */
public class CampbellSavelsbergh2005_dependentDemand implements RoutingAlgorithm {

	private static double TIME_MULTIPLIER = 60.0;
	private OrderRequestSet orderRequestSet;
	private DeliveryAreaSet deliveryAreaSet;
	private Region region;
	private TimeWindowSet timeWindowSet;
	private DemandSegmentWeighting demandSegmentWeighting;
	private HashMap<DeliveryArea, Double> daWeights;
	private HashMap<DeliveryArea, HashMap<DemandSegment, Double>> daSegmentWeightings;
	private HashMap<DemandSegment, Double> noPurchaseProbPerDemandSegment;
	private HashMap<Integer, HashMap<Integer, Double>> probabilityThatAboveNoPurchasePerDemandSegmentAndTimeWindow;
	private int includeDriveFromStartingPosition;
	private int numberOfGRASPSolutions;
	private int numberPotentialInsertionCandidates;
	private int usePreferencesSampled;
	private int considerReg;
	private double expectedServiceTime;
	private double costMultiplier;
	private VehicleAreaAssignmentSet vehicleAreaAssignmentSet;
	private Routing finalRouting;
	private boolean deliveryAreaHierarchy = false;
	private ProbabilityDistribution arrDistribution;
	private HashMap<DeliveryArea, ArrayList<OrderRequest>> pseudoRequestsREGPerDeliveryArea;
	private int numberAccepted=0;

	private static String[] paras = new String[] { "includeDriveFromStartingPosition", "no_routing_candidates",
			"no_insertion_candidates", "Constant_service_time", "cost_multiplier", "samplePreferences",
			"consider_REG" };

	public CampbellSavelsbergh2005_dependentDemand(ProbabilityDistribution arrDistribution,
			OrderRequestSet orderRequestSet, DemandSegmentWeighting demandSegmentWeighting,
			DeliveryAreaSet deliveryAreaSet, VehicleAreaAssignmentSet vehicleAreaAssignmentSet,
			Double includeDriveFromStartingPosition, Double numberRoutingCandidates,
			Double numberPotentialInsertionCandidates, Double constantServiceTime, Double preferencesSampled,
			Double considerReg, Double costMultiplier) {

		this.orderRequestSet = orderRequestSet;
		this.demandSegmentWeighting = demandSegmentWeighting;
		this.timeWindowSet = orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getAlternativeSet()
				.getTimeWindowSet();
		this.deliveryAreaSet = deliveryAreaSet;
		this.region = this.deliveryAreaSet.getRegion();
		this.vehicleAreaAssignmentSet = vehicleAreaAssignmentSet;
		this.numberPotentialInsertionCandidates = numberPotentialInsertionCandidates.intValue();
		this.includeDriveFromStartingPosition = includeDriveFromStartingPosition.intValue();
		this.numberOfGRASPSolutions = numberRoutingCandidates.intValue();
		this.expectedServiceTime = constantServiceTime;
		this.usePreferencesSampled = preferencesSampled.intValue();
		this.considerReg = considerReg.intValue();
		this.arrDistribution = arrDistribution;
		this.costMultiplier = costMultiplier;
	}

	public void start() {

		// Initialise alternatives to time window mapping
		HashMap<Integer, Alternative> alternativesForTimeWindows = new HashMap<Integer, Alternative>();
		for (Alternative alt : this.orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getAlternativeSet()
				.getElements()) {
			if (!alt.getNoPurchaseAlternative()) {

				alternativesForTimeWindows.put(alt.getTimeWindows().get(0).getId(), alt);
			}

		}

		// Determine arrival probability weights and segment weights per da
		OrderRequestSet pseudoRequests = new OrderRequestSet();
		if (this.considerReg == 1) {

			// Create potential future requests
			DataGenerationOrderRequestsII generationProcess = new DataGenerationOrderRequestsII();

			try {
				pseudoRequests = (generationProcess.generateData(this.orderRequestSet.getBookingHorizon(),
						arrDistribution, this.demandSegmentWeighting, null, this.usePreferencesSampled, 1, false))
								.getValue();
			} catch (ParameterUnknownException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			this.pseudoRequestsREGPerDeliveryArea = new HashMap<DeliveryArea, ArrayList<OrderRequest>>();
			for (OrderRequest r : pseudoRequests.getElements()) {
				try {
					r.setBasketValue(ProbabilityDistributionService.getMeanByProbabilityDistribution(
							r.getCustomer().getOriginalDemandSegment().getBasketValueDistribution()));
				} catch (ParameterUnknownException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				DeliveryArea area = LocationService.assignCustomerToDeliveryArea(deliveryAreaSet, r.getCustomer());
				if (this.pseudoRequestsREGPerDeliveryArea.containsKey(area)) {
					this.pseudoRequestsREGPerDeliveryArea.get(area).add(r);
				} else {
					ArrayList<OrderRequest> requests = new ArrayList<OrderRequest>();
					requests.add(r);
					this.pseudoRequestsREGPerDeliveryArea.put(area, requests);
				}
			}

			this.daWeights = new HashMap<DeliveryArea, Double>();
			this.daSegmentWeightings = new HashMap<DeliveryArea, HashMap<DemandSegment, Double>>();
			LocationService.determineDeliveryAreaWeightAndDemandSegmentWeightsPerDeliveryAreaAndDemandSegmentConsideringHierarchy(daWeights,
					daSegmentWeightings, deliveryAreaSet, demandSegmentWeighting);

			this.noPurchaseProbPerDemandSegment = new HashMap<DemandSegment, Double>();
			this.probabilityThatAboveNoPurchasePerDemandSegmentAndTimeWindow = new HashMap<Integer, HashMap<Integer, Double>>();
			for (DemandSegment s : ((DemandSegmentSet) this.demandSegmentWeighting.getSetEntity()).getElements()) {

				// No-purchase probability
				ArrayList<AlternativeOffer> offeredAlternatives = new ArrayList<AlternativeOffer>();
				AlternativeOffer noPurchaseAlternative = null;
				for (ConsiderationSetAlternative a : s.getConsiderationSet()) {

					AlternativeOffer offer = new AlternativeOffer();
					offer.setAlternative(a.getAlternative());
					offer.setAlternativeId(a.getAlternativeId());
					offeredAlternatives.add(offer);
					if (a.getAlternative().getNoPurchaseAlternative()) {
						noPurchaseAlternative = offer;
					}

				}
				HashMap<AlternativeOffer, Double> probs = CustomerDemandService
						.getProbabilitiesForModel(offeredAlternatives, s);
				this.noPurchaseProbPerDemandSegment.put(s, probs.get(noPurchaseAlternative));

				// Probability that tw utility is above no purchase probability
				HashMap<Integer, Double> probabilityThatAboveNoPurchasePerTimeWindow = new HashMap<Integer, Double>();
				for (ConsiderationSetAlternative a : s.getConsiderationSet()) {
					if (!a.getAlternative().getNoPurchaseAlternative()) {
						offeredAlternatives = new ArrayList<AlternativeOffer>();
						AlternativeOffer offer = new AlternativeOffer();
						offer.setAlternative(a.getAlternative());
						offer.setAlternativeId(a.getAlternative().getId());
						offeredAlternatives.add(offer);
						if (noPurchaseAlternative != null) {

							offeredAlternatives.add(noPurchaseAlternative);
						}
						probs = CustomerDemandService.getProbabilitiesForModel(offeredAlternatives, s);
						probabilityThatAboveNoPurchasePerTimeWindow
								.put(a.getAlternative().getTimeWindows().get(0).getId(), probs.get(offer));
					}
				}
				this.probabilityThatAboveNoPurchasePerDemandSegmentAndTimeWindow.put(s.getId(),
						probabilityThatAboveNoPurchasePerTimeWindow);
			}

		}

		// Group vehicle area assignments according to delivery areas
		HashMap<DeliveryArea, HashMap<Integer, VehicleAreaAssignment>> vaaPerDeliveryArea = new HashMap<DeliveryArea, HashMap<Integer, VehicleAreaAssignment>>();
		for (VehicleAreaAssignment ass : this.vehicleAreaAssignmentSet.getElements()) {
			if (!vaaPerDeliveryArea.containsKey(ass.getDeliveryArea())) {
				vaaPerDeliveryArea.put(ass.getDeliveryArea(), new HashMap<Integer, VehicleAreaAssignment>());
			}

			vaaPerDeliveryArea.get(ass.getDeliveryArea()).put(ass.getVehicleNo(), ass);
		}

		// Sort order requests (descending arrival time)
		ArrayList<OrderRequest> orderRequests = this.orderRequestSet.getElements();
		Collections.sort(orderRequests, new OrderRequestArrivalTimeDescComparator());

		// Initialise order buffers and last routing buffers per delivery area
		HashMap<DeliveryArea, ArrayList<Order>> ordersPerDeliveryArea = new HashMap<DeliveryArea, ArrayList<Order>>();
		HashMap<DeliveryArea, ArrayList<Order>> acceptedOrdersPerDeliveryArea = new HashMap<DeliveryArea, ArrayList<Order>>();
		HashMap<DeliveryArea, HashMap<Integer, ArrayList<RouteElement>>> lastRoutingPerDeliveryArea = new HashMap<DeliveryArea, HashMap<Integer, ArrayList<RouteElement>>>();

		for (DeliveryArea area : this.deliveryAreaSet.getElements()) {
			ordersPerDeliveryArea.put(area, new ArrayList<Order>());
			acceptedOrdersPerDeliveryArea.put(area, new ArrayList<Order>());
			lastRoutingPerDeliveryArea.put(area, new HashMap<Integer, ArrayList<RouteElement>>());
			if (area.getSubsetId() != null)
				deliveryAreaHierarchy = true;
		}

		// Go through requests
		for (OrderRequest request : orderRequests) {
			DeliveryArea area = LocationService.assignCustomerToDeliveryAreaConsideringHierarchy(deliveryAreaSet,
					request.getCustomer());

			Order order = new Order();
			order.setOrderRequest(request);
			order.setOrderRequestId(request.getId());
			order.setAccepted(false);

			ArrayList<Order> relevantOrders;
			HashMap<Integer, VehicleAreaAssignment> relevantVaas;
			ArrayList<OrderRequest> relevantOrderRequests = new ArrayList<OrderRequest>();
			if (deliveryAreaHierarchy) {
				relevantOrders = acceptedOrdersPerDeliveryArea.get(area.getDeliveryAreaOfSet());
				relevantVaas = vaaPerDeliveryArea.get(area.getDeliveryAreaOfSet());
				if (this.considerReg == 1)
					relevantOrderRequests = this.pseudoRequestsREGPerDeliveryArea.get(area.getDeliveryAreaOfSet());
			} else {
				relevantOrders = acceptedOrdersPerDeliveryArea.get(area);
				relevantVaas = vaaPerDeliveryArea.get(area);
				if (this.considerReg == 1)
					relevantOrderRequests = this.pseudoRequestsREGPerDeliveryArea.get(area);
			}
			ArrayList<HashMap<Integer, ArrayList<RouteElement>>> possibleRoutings = this.determinePossibleRoutings(
					relevantOrders, relevantVaas, timeWindowSet, this.numberOfGRASPSolutions,
					this.numberPotentialInsertionCandidates, TIME_MULTIPLIER, region, includeDriveFromStartingPosition,
					expectedServiceTime, costMultiplier);

			ArrayList<HashMap<Integer, ArrayList<RouteElement>>> potentialRoutings = new ArrayList<HashMap<Integer, ArrayList<RouteElement>>>();

			for (HashMap<Integer, ArrayList<RouteElement>> routes : possibleRoutings) {
				int included = 0;
				for (Integer routeId : routes.keySet()) {
					included += routes.get(routeId).size() - 2;
				}
				if (included == relevantOrders.size())
					potentialRoutings.add(routes);
			}

			if (potentialRoutings.size() < 1) {
				potentialRoutings.add(lastRoutingPerDeliveryArea.get(area.getDeliveryAreaOfSet()));
			}

			HashMap<Integer, Pair<HashMap<Integer, ArrayList<RouteElement>>, RouteElement>> feasibleTimeWindows = this
					.determineFeasibleTimeWindows(potentialRoutings, relevantVaas, relevantOrders,
							relevantOrderRequests, request, area, alternativesForTimeWindows,
							((DemandSegmentSet) this.demandSegmentWeighting.getSetEntity()).getAlternativeSet());

			CustomerDemandService.simulateCustomerDecision(order, feasibleTimeWindows.keySet(), orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getAlternativeSet(), alternativesForTimeWindows, (this.usePreferencesSampled==1));
			if (order.getAccepted()) {
				this.numberAccepted++;
				if (this.considerReg == 0) {
					RouteElement e = feasibleTimeWindows.get(order.getTimeWindowFinalId()).getValue();
					e.setOrder(order);
					DynamicRoutingHelperService.insertRouteElement(
							feasibleTimeWindows.get(order.getTimeWindowFinalId()).getValue(),
							feasibleTimeWindows.get(order.getTimeWindowFinalId()).getKey(), relevantVaas, timeWindowSet,
							TIME_MULTIPLIER, (includeDriveFromStartingPosition==1));
				} else {
					RouteElement element = DynamicRoutingHelperService.getCheapestInsertionElementByOrder(order,
							feasibleTimeWindows.get(order.getTimeWindowFinalId()).getKey(), relevantVaas, region,
							TIME_MULTIPLIER, expectedServiceTime, timeWindowSet,  (includeDriveFromStartingPosition==1), false);
					DynamicRoutingHelperService.insertRouteElement(element,
							feasibleTimeWindows.get(order.getTimeWindowFinalId()).getKey(), relevantVaas, timeWindowSet,
							TIME_MULTIPLIER, (includeDriveFromStartingPosition==1));

				}
			}

			if (this.deliveryAreaHierarchy) {
				ordersPerDeliveryArea.get(area.getDeliveryAreaOfSet()).add(order);

				if (order.getAccepted()) {
					lastRoutingPerDeliveryArea.put(area.getDeliveryAreaOfSet(),
							feasibleTimeWindows.get(order.getTimeWindowFinalId()).getKey());
					acceptedOrdersPerDeliveryArea.get(area.getDeliveryAreaOfSet()).add(order);
				}

			} else {
				ordersPerDeliveryArea.get(area).add(order);
				if (order.getAccepted()) {
					lastRoutingPerDeliveryArea.put(area,
							feasibleTimeWindows.get(order.getTimeWindowFinalId()).getKey());
					acceptedOrdersPerDeliveryArea.get(area).add(order);
				}
			}
		}

		ArrayList<Route> routes = new ArrayList<Route>();
		ArrayList<Order> orders = new ArrayList<Order>();
		for (DeliveryArea area : ordersPerDeliveryArea.keySet()) {
			orders.addAll(ordersPerDeliveryArea.get(area));

			ArrayList<HashMap<Integer, ArrayList<RouteElement>>> possibleRoutings = this.determinePossibleRoutings(
					acceptedOrdersPerDeliveryArea.get(area), vaaPerDeliveryArea.get(area), timeWindowSet,
					this.numberOfGRASPSolutions, this.numberPotentialInsertionCandidates, TIME_MULTIPLIER, region,
					includeDriveFromStartingPosition, expectedServiceTime, costMultiplier);

			possibleRoutings.add(lastRoutingPerDeliveryArea.get(area));
			HashMap<Integer, ArrayList<RouteElement>> bestRouting = this.determineBestRouting(possibleRoutings,
					acceptedOrdersPerDeliveryArea.get(area));

			for (Integer routeId : bestRouting.keySet()) {
				Route route = new Route();
				ArrayList<RouteElement> elements = bestRouting.get(routeId);
				// Delete dummy elements
				elements.remove(0);
				elements.remove(elements.size() - 1);

				for (RouteElement e : elements) {
					e.setTimeWindowId(e.getOrder().getTimeWindowFinalId());
					e.setTravelTime(e.getTravelTimeTo());
				}
				route.setRouteElements(elements);
				route.setVehicleAreaAssignmentId(vaaPerDeliveryArea.get(area).get(routeId).getId());
				routes.add(route);
			}
		}

		this.finalRouting = new Routing();
		this.finalRouting.setRoutes(routes);
		this.finalRouting.setPossiblyFinalRouting(true);
		OrderSet orderSet = new OrderSet();
		orderSet.setElements(orders);
		orderSet.setOrderRequestSetId(this.orderRequestSet.getId());
		this.finalRouting.setOrderSet(orderSet);

	}

	private HashMap<Integer, Pair<HashMap<Integer, ArrayList<RouteElement>>, RouteElement>> determineFeasibleTimeWindows(
			ArrayList<HashMap<Integer, ArrayList<RouteElement>>> potentialRoutings,
			HashMap<Integer, VehicleAreaAssignment> relevantVaas, ArrayList<Order> relevantOrders,
			ArrayList<OrderRequest> relevantOrderRequests, OrderRequest request, DeliveryArea requestArea,
			HashMap<Integer, Alternative> alternativesForTimeWindows, AlternativeSet altSet) {

		HashMap<Integer, Pair<HashMap<Integer, ArrayList<RouteElement>>, RouteElement>> feasibleTws;

		if (this.considerReg == 0) {

			feasibleTws = this.determineFeasibleTwsForDYN(potentialRoutings, relevantVaas, relevantOrders, request);

		} else {
			feasibleTws = this.determineFeasibleTwsForREG(potentialRoutings, relevantVaas, relevantOrders,
					relevantOrderRequests, request, requestArea, alternativesForTimeWindows, altSet);
		}

		return feasibleTws;
	}

	private HashMap<Integer, Pair<HashMap<Integer, ArrayList<RouteElement>>, RouteElement>> determineFeasibleTwsForREG(
			ArrayList<HashMap<Integer, ArrayList<RouteElement>>> potentialRoutings,
			HashMap<Integer, VehicleAreaAssignment> relevantVaas, ArrayList<Order> relevantOrders,
			ArrayList<OrderRequest> relevantPseudoRequests, OrderRequest request, DeliveryArea requestArea,
			HashMap<Integer, Alternative> alternativesForTimeWindows, AlternativeSet altSet) {
		HashMap<Integer, Pair<HashMap<Integer, ArrayList<RouteElement>>, RouteElement>> feasibleTws = new HashMap<Integer, Pair<HashMap<Integer, ArrayList<RouteElement>>, RouteElement>>();

		// Determine highest profit routing
		HashMap<Integer, ArrayList<RouteElement>> bestRoutingBuffer = this.determineBestRouting(potentialRoutings,
				relevantOrders);
		// HashMap<Integer, ArrayList<RouteElement>> bestRouting =
		// this.copyRouting(bestRoutingBuffer);

		// Determine still relevant pseudo requests
		ArrayList<OrderRequest> requestsToInsert = new ArrayList<OrderRequest>();
		for (int i = 0; i < relevantPseudoRequests.size(); i++) {
			if (relevantPseudoRequests.get(i).getArrivalTime() < request.getArrivalTime()) {
				requestsToInsert.add(relevantPseudoRequests.get(i));
				DeliveryArea subArea;
				if (relevantPseudoRequests.get(i).getCustomer().getTempDeliveryArea() == null) {
					subArea = LocationService.assignCustomerToDeliveryAreaConsideringHierarchy(deliveryAreaSet,
							relevantPseudoRequests.get(i).getCustomer());
					relevantPseudoRequests.get(i).getCustomer().setTempDeliveryArea(subArea);
				} else {
					subArea = relevantPseudoRequests.get(i).getCustomer().getTempDeliveryArea();
				}

			} else {
				relevantPseudoRequests.remove(i--);
			}
		}
		request.getCustomer().setTempDeliveryArea(requestArea);
		// Insert requests - choose highest profit time window option per
		// request

		HashMap<Integer, Double> altPref = request.getAlternativePreferences();

		// Backup
		ArrayList<OrderRequest> requestsToInsertCopy = new ArrayList<OrderRequest>();
		requestsToInsertCopy.addAll(requestsToInsert);
		for (Integer altId : altPref.keySet()) {
			if (altId != altSet.getNoPurchaseAlternative().getId()) {

				HashMap<Integer, ArrayList<RouteElement>> bestRouting = this.copyRouting(bestRoutingBuffer);

				HashMap<Integer, Double> altPrefNew = new HashMap<Integer, Double>();
				for (Integer id : altPref.keySet()) {
					if (id.intValue() == altId.intValue()) {
						altPrefNew.put(altId, altPref.get(altSet.getNoPurchaseAlternative().getId()) + 1);
					} else if (altSet.getNoPurchaseAlternative().getId() == id.intValue()) {
						altPrefNew.put(id, altPref.get(altSet.getNoPurchaseAlternative().getId()));
					} else {
						altPrefNew.put(id, altPref.get(altSet.getNoPurchaseAlternative().getId()) - 1);
					}

				}

				request.setAlternativePreferences(altPrefNew);
				requestsToInsert.add(request);

				RouteElement requestIncluded = null;
				while (requestsToInsert.size() > 0) {

					ArrayList<Pair<RouteElement, Double>> valueForInsertionOptions = new ArrayList<Pair<RouteElement, Double>>();
					for (OrderRequest r : requestsToInsert) {

						// Feasible time windows
						HashMap<Integer, RouteElement> feasibleTWR = DynamicRoutingHelperService
								.getFeasibleTimeWindowsBasedOnCheapestInsertionHeuristic(r, bestRouting, relevantVaas,
										region, TIME_MULTIPLIER, expectedServiceTime, timeWindowSet,
										 (includeDriveFromStartingPosition==1));

						// Determine best option for r
						double bestOptionValue = -1 * Double.MAX_VALUE;
						RouteElement bestOption = null;

						for (Integer twId : feasibleTWR.keySet()) {

							if (r.getAlternativePreferences().get(alternativesForTimeWindows.get(twId).getId()) >= r
									.getAlternativePreferences().get(altSet.getNoPurchaseAlternative().getId())) {
								double rValue = 0.0;
								if (request.getId() != r.getId()) {
									rValue = r.getBasketValue()
											* this.probabilityThatAboveNoPurchasePerDemandSegmentAndTimeWindow
													.get(r.getCustomer().getOriginalDemandSegmentId()).get(twId)
											* this.daWeights.get(r.getCustomer().getTempDeliveryArea())
											* this.daSegmentWeightings.get(r.getCustomer().getTempDeliveryArea())
													.get(r.getCustomer().getOriginalDemandSegment())
											- (feasibleTWR.get(twId).getTempShiftWithoutWait()
													- feasibleTWR.get(twId).getServiceTime()) * this.costMultiplier;
								} else {
									rValue = r.getBasketValue()* this.probabilityThatAboveNoPurchasePerDemandSegmentAndTimeWindow
											.get(r.getCustomer().getOriginalDemandSegmentId()).get(twId) - (feasibleTWR.get(twId).getTempShiftWithoutWait()
											- feasibleTWR.get(twId).getServiceTime()) * this.costMultiplier;
								}
								if (rValue > bestOptionValue) {
									bestOptionValue = rValue;
									bestOption = feasibleTWR.get(twId);

								}

							}

						}
						if (bestOption != null) {
							double expectedValue = 0.0;
							for (DemandSegment s : this.daSegmentWeightings.get(r.getCustomer().getTempDeliveryArea())
									.keySet()) {
								try {
									expectedValue += this.daSegmentWeightings.get(r.getCustomer().getTempDeliveryArea())
											.get(s)
											* ProbabilityDistributionService
													.getMeanByProbabilityDistribution(s.getBasketValueDistribution());
								} catch (ParameterUnknownException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							try {
								bestOptionValue = bestOptionValue + (request.getArrivalTime() - 1.0)
										* ProbabilityDistributionService
												.getMeanByProbabilityDistribution(arrDistribution)
										* this.daWeights.get(r.getCustomer().getTempDeliveryArea()) * expectedValue;
							} catch (ParameterUnknownException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

							if (r.getId() == request.getId()
									&& request.getCustomer().getOriginalDemandSegmentId() == 56) {
								System.out.println("Value is");
							}
							valueForInsertionOptions.add(new Pair<RouteElement, Double>(bestOption, bestOptionValue));
						}
					}

					if (valueForInsertionOptions.size() < 1)
						break;

					Collections.sort(valueForInsertionOptions, new ObjectValuePairDoubleValueDescComparator());

					DynamicRoutingHelperService.insertRouteElement(valueForInsertionOptions.get(0).getKey(),
							bestRouting, relevantVaas, timeWindowSet, TIME_MULTIPLIER,
							(includeDriveFromStartingPosition==1));
					if (valueForInsertionOptions.get(0).getKey().getOrder().getOrderRequest().getId() == request
							.getId())
						requestIncluded = valueForInsertionOptions.get(0).getKey();
					System.out.println("Before:"+requestsToInsert.size());
					requestsToInsert.remove(valueForInsertionOptions.get(0).getKey().getOrder().getOrderRequest());
					System.out.println("After:"+requestsToInsert.size());
				}

				// Current request inside?

				if (requestIncluded != null) {
					/// Check profitability
					if (request.getBasketValue()
							- (requestIncluded.getTempShiftWithoutWait() - requestIncluded.getServiceTime())
									* this.costMultiplier > 0) {

						bestRouting = this.copyRouting(bestRoutingBuffer);
						/// Add to feasible time windows
						feasibleTws.put(requestIncluded.getOrder().getTimeWindowFinalId(),
								new Pair<HashMap<Integer, ArrayList<RouteElement>>, RouteElement>(bestRouting, null));
					}
				}

				requestsToInsert = new ArrayList<OrderRequest>();
				requestsToInsert.addAll(requestsToInsertCopy);
			}
		}

		request.setAlternativePreferences(altPref);

		return feasibleTws;
	}

	private HashMap<Integer, Pair<HashMap<Integer, ArrayList<RouteElement>>, RouteElement>> determineFeasibleTwsForDYN(
			ArrayList<HashMap<Integer, ArrayList<RouteElement>>> potentialRoutings,
			HashMap<Integer, VehicleAreaAssignment> relevantVaas, ArrayList<Order> acceptedRelevantOrders,
			OrderRequest request) {
		HashMap<Integer, Pair<HashMap<Integer, ArrayList<RouteElement>>, RouteElement>> feasibleTws = new HashMap<Integer, Pair<HashMap<Integer, ArrayList<RouteElement>>, RouteElement>>();
		HashMap<Integer, Double> bestValuePerTw = new HashMap<Integer, Double>();

		for (int routingId = 0; routingId < potentialRoutings.size(); routingId++) {

			Pair<Integer, Double> value = this.evaluateRouting(potentialRoutings.get(routingId),
					acceptedRelevantOrders);

			HashMap<Integer, RouteElement> feasibleTwsPerRouting = DynamicRoutingHelperService
					.getFeasibleTimeWindowsBasedOnCheapestInsertionHeuristic(request, potentialRoutings.get(routingId),
							relevantVaas, region, TIME_MULTIPLIER, expectedServiceTime, timeWindowSet,
							 (includeDriveFromStartingPosition==1));

			for (Integer twId : feasibleTwsPerRouting.keySet()) {

				// Only consider option if it provides profit
				if ((feasibleTwsPerRouting.get(twId).getTempShift() - feasibleTwsPerRouting.get(twId).getServiceTime())
						* this.costMultiplier < request.getBasketValue()) {

					if (!feasibleTws.containsKey(twId)) {
						feasibleTws.put(twId, new Pair<HashMap<Integer, ArrayList<RouteElement>>, RouteElement>(
								potentialRoutings.get(routingId), feasibleTwsPerRouting.get(twId)));
						bestValuePerTw.put(twId, value.getValue());
					} else {

						// Update if better than earlier option
						double currentValue = value.getValue() + request.getBasketValue()
								- feasibleTwsPerRouting.get(twId).getTempShift() * this.costMultiplier;
						if (bestValuePerTw.get(twId) < currentValue) {
							bestValuePerTw.put(twId, currentValue);
							feasibleTws.put(twId, new Pair<HashMap<Integer, ArrayList<RouteElement>>, RouteElement>(
									potentialRoutings.get(routingId), feasibleTwsPerRouting.get(twId)));
						}
					}
				}

			}
		}

		return feasibleTws;
	}

	

	private Pair<Integer, Double> evaluateRouting(HashMap<Integer, ArrayList<RouteElement>> routing,
			ArrayList<Order> acceptedOrders) {
		double value = 0.0;
		int numberInfeasible;

		numberInfeasible = acceptedOrders.size();

		for (Integer routeId : routing.keySet()) {
			numberInfeasible -= routing.get(routeId).size() - 2;
			for (RouteElement e : routing.get(routeId)) {
				value += e.getOrder().getOrderRequest().getBasketValue() - e.getTravelTimeFrom() * this.costMultiplier;
			}
		}

		return new Pair<Integer, Double>(numberInfeasible, value);
	}

	private HashMap<Integer, ArrayList<RouteElement>> determineBestRouting(
			ArrayList<HashMap<Integer, ArrayList<RouteElement>>> possibleRoutings, ArrayList<Order> acceptedOrders) {
		double bestValue = -1 * Double.MAX_VALUE;
		int bestInfeasible = Integer.MAX_VALUE;
		HashMap<Integer, ArrayList<RouteElement>> bestRouting = new HashMap<Integer, ArrayList<RouteElement>>();
		for (HashMap<Integer, ArrayList<RouteElement>> routing : possibleRoutings) {
			Pair<Integer, Double> routingValue = this.evaluateRouting(routing, acceptedOrders);

			// Feasibility is most important
			if (bestInfeasible > routingValue.getKey()) {
				bestInfeasible = routingValue.getKey();
				bestValue = routingValue.getValue();
				bestRouting = routing;
			} else if ((bestInfeasible == routingValue.getKey()) && (routingValue.getValue() > bestValue)) {
				bestRouting = routing;
				bestValue = routingValue.getValue();
			}
		}

		return bestRouting;
	}

	/**
	 * Determines possible routings for already accepted. Can return infeasible
	 * routings.
	 * 
	 * @param acceptedOrders
	 * @param vaasPerVehicleNo
	 * @param timeWindowSet
	 * @param numberOfRoutingCandidates
	 * @param numberOfInsertionCandidates
	 * @param timeMultiplier
	 * @param region
	 * @param includeDriveFromStartingPosition
	 * @param expectedServiceTime
	 * @param costMultiplier
	 * @return
	 */
	private ArrayList<HashMap<Integer, ArrayList<RouteElement>>> determinePossibleRoutings(
			ArrayList<Order> acceptedOrders, HashMap<Integer, VehicleAreaAssignment> vaasPerVehicleNo,
			TimeWindowSet timeWindowSet, int numberOfRoutingCandidates, int numberOfInsertionCandidates,
			double timeMultiplier, Region region, int includeDriveFromStartingPosition, double expectedServiceTime,
			double costMultiplier) {

		ArrayList<HashMap<Integer, ArrayList<RouteElement>>> routings = new ArrayList<HashMap<Integer, ArrayList<RouteElement>>>();

		for (int routingId = 0; routingId < numberOfRoutingCandidates; routingId++) {

			ArrayList<Order> ordersToInsert = new ArrayList<Order>();
			for (Order order : acceptedOrders) {
				ordersToInsert.add(order);
			}

			HashMap<Integer, ArrayList<RouteElement>> routes = DynamicRoutingHelperService.initialiseRoutes(
					vaasPerVehicleNo.values(), timeWindowSet, timeMultiplier, region, (includeDriveFromStartingPosition==1));

			while (ordersToInsert.size() > 0) {

				ArrayList<RouteElement> insertionOptions = new ArrayList<RouteElement>();

				// Determine feasible insertions
				for (Order order : ordersToInsert) {
					RouteElement insertionOption = DynamicRoutingHelperService.getCheapestInsertionElementByOrder(order,
							routes, vaasPerVehicleNo, region, timeMultiplier, expectedServiceTime, timeWindowSet,
							 (includeDriveFromStartingPosition==1), false);

					if (insertionOption != null) {
						insertionOptions.add(insertionOption);
					}
				}

				if (insertionOptions.size() < 1)
					break;

				// Sort regarding value to find maximum and minimum value
				Collections.sort(insertionOptions, new RouteElementProfitDescComparator(costMultiplier));

				int chosenIndex = 0;

				int borderElement = Math.min(numberOfInsertionCandidates, insertionOptions.size());

				// Choose a random number between 0 and the borderElement
				Random randomGenerator = new Random();
				chosenIndex = randomGenerator.nextInt(borderElement);

				// Insert the respective Element in the route
				RouteElement toInsert = insertionOptions.get(chosenIndex);
				DynamicRoutingHelperService.insertRouteElement(toInsert, routes, vaasPerVehicleNo, timeWindowSet,
						timeMultiplier, (includeDriveFromStartingPosition==1));
				// Delete the respective order from the unassigned orders
				ordersToInsert.remove(insertionOptions.get(chosenIndex).getOrder());

			}

			routings.add(routes);

		}

		return routings;

	}

	private HashMap<Integer, ArrayList<RouteElement>> copyRouting(HashMap<Integer, ArrayList<RouteElement>> toCopy) {
		HashMap<Integer, ArrayList<RouteElement>> newRouting = new HashMap<Integer, ArrayList<RouteElement>>();
		for (Integer routeId : toCopy.keySet()) {
			ArrayList<RouteElement> elements = toCopy.get(routeId);
			ArrayList<RouteElement> newElements = new ArrayList<RouteElement>();
			for (RouteElement e : elements) {
				RouteElement eCopy = e.copyElement();
				eCopy.setOrder(e.getOrder());
				newElements.add(eCopy);
			}
			newRouting.put(routeId, newElements);
		}

		return newRouting;
	}

	public Routing getResult() {
		return this.finalRouting;
	}

	public static String[] getParameterSetting() {

		return paras;
	}

}
