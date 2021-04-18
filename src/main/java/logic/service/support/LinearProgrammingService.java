package logic.service.support;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.apache.commons.math3.util.Pair;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import data.entity.Alternative;
import data.entity.AlternativeOffer;
import data.entity.Capacity;
import data.entity.ConsiderationSetAlternative;
import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.DemandSegment;
import data.entity.DemandSegmentSet;
import data.entity.DemandSegmentWeight;
import data.entity.DemandSegmentWeighting;
import data.entity.Entity;
import data.entity.Order;
import data.entity.OrderRequest;
import data.entity.OrderRequestSet;
import data.entity.Region;
import data.entity.RouteElement;
import data.entity.TimeWindow;
import data.entity.TimeWindowSet;
import data.entity.VehicleAreaAssignment;
import logic.entity.AssortmentAlgorithm;
import logic.utility.comparator.OrderRequestArrivalTimeDescComparator;
import logic.utility.exceptions.ParameterUnknownException;


public class LinearProgrammingService {
	private static double DISCOUNT_FACTOR = 1.0;

	public static ArrayList<Order> simulateOrderHorizonForDeterministicLinearProgrammingWithStaticCapacityAssignment(
			double beta, HashMap<Integer, ArrayList<Capacity>> capacitiesPerDeliveryAreaToCopy,
			OrderRequestSet orderRequestSet, DeliveryAreaSet daSet, Region region, TimeWindowSet timeWindowSet,
			HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightings, HashMap<DeliveryArea, Double> daWeights,
			int arrivalProcessId,
			HashMap<DemandSegment, HashMap<TimeWindow, Double>> lowerBoundDemandMultiplierPerSegment,
			HashMap<DemandSegment, HashMap<TimeWindow, Double>> upperBoundDemandMultiplierPerSegment,
			double maximumRevenueValue, HashMap<Entity, Object> objectiveSpecificValues, boolean usepreferencesSampled,
			int periodLength, double betaRange, AssortmentAlgorithm algo, boolean possiblyLargeOfferSet,
			boolean actualValue, double riskRange) {

		// Go through requests
		ArrayList<OrderRequest> requests = orderRequestSet.getElements();
		Collections.sort(requests, new OrderRequestArrivalTimeDescComparator());
		ArrayList<Order> orders = new ArrayList<Order>();

		// Prepare capacities
		HashMap<Integer, HashMap<Integer, Capacity>> capacitiesPerDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Capacity>>();
		HashMap<Integer, HashMap<Integer, HashMap<Integer, Capacity>>> capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindowFinal = new HashMap<Integer, HashMap<Integer, HashMap<Integer, Capacity>>>();
		HashMap<Integer, HashMap<Integer, HashMap<Integer, Capacity>>> capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow = new HashMap<Integer, HashMap<Integer, HashMap<Integer, Capacity>>>();

		double[] capacityRange = LinearProgrammingService.prepareCapacities(capacitiesPerDeliveryAreaToCopy,
				orderRequestSet, capacitiesPerDeliveryAreaAndTimeWindow,
				capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindowFinal,
				capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow);

		HashMap<Integer, Integer> areaArrivalCounter = new HashMap<Integer, Integer>();

		for (DeliveryArea area : daSet.getElements()) {
			if (area.getSubsetId() != null) {
				for (DeliveryArea sArea : area.getSubset().getElements()) {
					areaArrivalCounter.put(sArea.getId(), 0);
				}
			} else {
				areaArrivalCounter.put(area.getId(), 0);
			}

		}

		for (OrderRequest request : requests) {

			// Determine respective delivery area
			DeliveryArea rArea = LocationService.assignCustomerToDeliveryAreaConsideringHierarchy(daSet,
					request.getCustomer());
			areaArrivalCounter.put(rArea.getId(), areaArrivalCounter.get(rArea.getId()) + 1);
			// Determine feasible time windows (that are in consideration set)
			// based on routes of delivery area set

			ArrayList<TimeWindow> consideredAndFeasibleTimeWindows = new ArrayList<TimeWindow>();
			HashMap<TimeWindow, Alternative> alternativesToTimeWindows = new HashMap<TimeWindow, Alternative>();
			for (ConsiderationSetAlternative alt : request.getCustomer().getOriginalDemandSegment()
					.getConsiderationSet()) {
				if (!alt.getAlternative().getNoPurchaseAlternative()) {

					if (capacitiesPerDeliveryAreaAndTimeWindow.get(rArea.getId())
							.get(alt.getAlternative().getTimeWindows().get(0).getId()).getCapacityNumber() > 0) {
						consideredAndFeasibleTimeWindows.add(alt.getAlternative().getTimeWindows().get(0));
					}

					alternativesToTimeWindows.put(alt.getAlternative().getTimeWindows().get(0), alt.getAlternative());
				}

			}

			// Determine future value with same capacities
			double expectedArrivals = ArrivalProcessService.getMeanArrivalProbability(arrivalProcessId)
					* (request.getArrivalTime() - 1) * daWeights.get(rArea) * (1.0 - riskRange);

			if (request.getArrivalTime() < 50)
				System.out.println("Check");
			OrderRequest currentRequest = null;
			if (actualValue)
				currentRequest = request;

			double futureValueSameCapacities = LinearProgrammingService
					.determineDeterministicLinearProgrammingSolutionPerDeliveryArea(daSegmentWeightings.get(rArea),
							capacitiesPerDeliveryAreaAndTimeWindow.get(rArea.getId()),
							capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindowFinal.get(rArea.getId()),
							capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow.get(rArea.getId()),
							lowerBoundDemandMultiplierPerSegment, upperBoundDemandMultiplierPerSegment, beta,
							expectedArrivals, maximumRevenueValue, objectiveSpecificValues, timeWindowSet,
							(request.getArrivalTime() - 1), periodLength, capacityRange[1], capacityRange[0], betaRange,
							currentRequest);

			// For all time windows that are feasible,
			// determine future value without the respective resource
			HashMap<TimeWindow, Double> futureValueLessCapacities = new HashMap<TimeWindow, Double>();
			for (TimeWindow tw : consideredAndFeasibleTimeWindows) {
				capacitiesPerDeliveryAreaAndTimeWindow.get(rArea.getId()).get(tw.getId()).setCapacityNumber(
						capacitiesPerDeliveryAreaAndTimeWindow.get(rArea.getId()).get(tw.getId()).getCapacityNumber()
								- 1);
				for (DemandSegment ds : orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getElements()) {
					for (Integer twId : capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow
							.get(rArea.getId()).get(ds.getId()).keySet()) {
						if (twId != tw.getId()) {
							int oldNumber = capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow
									.get(rArea.getId()).get(ds.getId()).get(twId).getCapacityNumber();
							capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow.get(rArea.getId())
									.get(ds.getId()).get(twId).setCapacityNumber(oldNumber - 1);
						}
					}
				}
				double futureValueLess = LinearProgrammingService
						.determineDeterministicLinearProgrammingSolutionPerDeliveryArea(daSegmentWeightings.get(rArea),
								capacitiesPerDeliveryAreaAndTimeWindow.get(rArea.getId()),
								capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindowFinal
										.get(rArea.getId()),
								capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow.get(rArea.getId()),
								lowerBoundDemandMultiplierPerSegment, upperBoundDemandMultiplierPerSegment, beta,
								expectedArrivals, maximumRevenueValue, objectiveSpecificValues, timeWindowSet,
								(request.getArrivalTime() - 1), periodLength, capacityRange[1], capacityRange[0],
								betaRange, currentRequest);
				capacitiesPerDeliveryAreaAndTimeWindow.get(rArea.getId()).get(tw.getId()).setCapacityNumber(
						capacitiesPerDeliveryAreaAndTimeWindow.get(rArea.getId()).get(tw.getId()).getCapacityNumber()
								+ 1);
				for (DemandSegment ds : orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getElements()) {
					for (Integer twId : capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow
							.get(rArea.getId()).get(ds.getId()).keySet()) {
						if (twId != tw.getId()) {
							int oldNumber = capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow
									.get(rArea.getId()).get(ds.getId()).get(twId).getCapacityNumber();
							capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow.get(rArea.getId())
									.get(ds.getId()).get(twId).setCapacityNumber(oldNumber + 1);
						}
					}
				}
				futureValueLessCapacities.put(tw, futureValueLess);
			}

			Pair<ArrayList<AlternativeOffer>, Double> bestOffer = AssortmentProblemService.determineBestOfferSet(
					request, futureValueLessCapacities, futureValueSameCapacities, maximumRevenueValue,
					objectiveSpecificValues, algo, alternativesToTimeWindows, possiblyLargeOfferSet, actualValue, false,
					null, null, DISCOUNT_FACTOR);
			ArrayList<AlternativeOffer> bestOfferedAlternatives = bestOffer.getKey();
			// Simulate customer decision and insert into route
			Order order = new Order();
			order.setOrderRequest(request);
			order.setOrderRequestId(request.getId());
			order.setAccepted(false);

			if (request.getArrivalTime() < 100 && request.getBasketValue() > 55
					&& bestOffer.getKey().size() < futureValueLessCapacities.keySet().size()) {
				System.out.println("Interesting");
			}
			// TODO Check: bestofferedAlternativesPlausibility
			if (bestOfferedAlternatives.size() > 0) { // If windows are offered
				// Sample selection from customer
				AlternativeOffer selectedAlt;
				if (usepreferencesSampled) {
					selectedAlt = CustomerDemandService.selectCustomerDemandBasedOnSampledPreferences(
							bestOfferedAlternatives, order, orderRequestSet.getCustomerSet()
									.getOriginalDemandSegmentSet().getAlternativeSet().getNoPurchaseAlternative());

				} else {
					selectedAlt = CustomerDemandService.sampleCustomerDemand(bestOfferedAlternatives, order);
				}

				if (selectedAlt != null) {
					if (!selectedAlt.getAlternative().getNoPurchaseAlternative()) {
						order.setTimeWindowFinal(selectedAlt.getAlternative().getTimeWindows().get(0));
						order.setTimeWindowFinalId(selectedAlt.getAlternative().getTimeWindows().get(0).getId());
						order.setAccepted(true);

						capacitiesPerDeliveryAreaAndTimeWindow.get(rArea.getId()).get(order.getTimeWindowFinalId())
								.setCapacityNumber(capacitiesPerDeliveryAreaAndTimeWindow.get(rArea.getId())
										.get(order.getTimeWindowFinalId()).getCapacityNumber() - 1);

						for (DemandSegment ds : orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet()
								.getElements()) {
							for (Integer twId : capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow
									.get(rArea.getId()).get(ds.getId()).keySet()) {
								if (twId != order.getTimeWindowFinalId()) {
									int oldNumber = capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow
											.get(rArea.getId()).get(ds.getId()).get(twId).getCapacityNumber();
									capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow
											.get(rArea.getId()).get(ds.getId()).get(twId)
											.setCapacityNumber(oldNumber - 1);
								}
							}
						}
					} else {
						order.setReasonRejection("No-purchase alternative selected");
					}

				} else {

					order.setReasonRejection("No-purchase alternative selected");

				}

			} else {

				order.setReasonRejection("No alternative offered");
			}

			// Add order to order list
			orders.add(order);
		}
		// TODO Check: Routes and left over capacities
		int sum = 0;
		for (Integer daId : capacitiesPerDeliveryAreaAndTimeWindow.keySet()) {
			for (Integer twId : capacitiesPerDeliveryAreaAndTimeWindow.get(daId).keySet()) {
				sum += capacitiesPerDeliveryAreaAndTimeWindow.get(daId).get(twId).getCapacityNumber();
				if (capacitiesPerDeliveryAreaAndTimeWindow.get(daId).get(twId).getCapacityNumber() > 1) {
					System.out.println("Strange");
				}
			}
		}
		System.out.println("Left over capacities: " + sum);
		return orders;
	}

	public static ArrayList<Order> simulateOrderHorizonForDeterministicLinearProgrammingWithStaticCapacityAssignmentAndDynamicFeasibilityCheck(
			double beta, HashMap<Integer, ArrayList<Capacity>> capacitiesPerDeliveryAreaToCopy,
			OrderRequestSet orderRequestSet, DeliveryAreaSet daSet,
			HashMap<Integer, ArrayList<VehicleAreaAssignment>> vehicleAssignmentsPerDeliveryArea,
			HashMap<Integer, HashMap<Integer, VehicleAreaAssignment>> vehicleAssignmentPerDeliveryAreaAndVehicleNo,
			Region region, double timeMultiplier, double expectedServiceTime, TimeWindowSet timeWindowSet,
			HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightings, HashMap<DeliveryArea, Double> daWeights,
			int arrivalProcessId,
			HashMap<DemandSegment, HashMap<TimeWindow, Double>> lowerBoundDemandMultiplierPerSegment,
			HashMap<DemandSegment, HashMap<TimeWindow, Double>> upperBoundDemandMultiplierPerSegment,
			double maximumRevenueValue, HashMap<Entity, Object> objectiveSpecificValues,
			int includeDriveFromStartingPosition, boolean usepreferencesSampled, int periodLength, double betaRange,
			AssortmentAlgorithm algo, boolean possiblyLargeOfferSet, boolean actualValue, double riskRange) {

		// Initialise routes (per delivery area set, to cover delivery area
		// hierarchies)
		HashMap<Integer, HashMap<Integer, ArrayList<RouteElement>>> routesPerDeliveryAreaSet = new HashMap<Integer, HashMap<Integer, ArrayList<RouteElement>>>();
		for (DeliveryArea area : daSet.getElements()) {
			if (area.getSubsetId() != 0) {
				routesPerDeliveryAreaSet.put(area.getSubsetId(),
						DynamicRoutingHelperService.initialiseRoutes(area, vehicleAssignmentsPerDeliveryArea,
								timeWindowSet, timeMultiplier, region, (includeDriveFromStartingPosition == 1)));
			} else {
				routesPerDeliveryAreaSet.put(area.getSetId(),
						DynamicRoutingHelperService.initialiseRoutes(area, vehicleAssignmentsPerDeliveryArea,
								timeWindowSet, timeMultiplier, region, (includeDriveFromStartingPosition == 1)));
			}

		}

		// Go through requests
		ArrayList<OrderRequest> requests = orderRequestSet.getElements();
		Collections.sort(requests, new OrderRequestArrivalTimeDescComparator());
		ArrayList<Order> orders = new ArrayList<Order>();

		// Prepare capacities
		HashMap<Integer, HashMap<Integer, Capacity>> capacitiesPerDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Capacity>>();
		HashMap<Integer, HashMap<Integer, HashMap<Integer, Capacity>>> capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindowFinal = new HashMap<Integer, HashMap<Integer, HashMap<Integer, Capacity>>>();
		HashMap<Integer, HashMap<Integer, HashMap<Integer, Capacity>>> capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow = new HashMap<Integer, HashMap<Integer, HashMap<Integer, Capacity>>>();

		double[] capacityRange = LinearProgrammingService.prepareCapacities(capacitiesPerDeliveryAreaToCopy,
				orderRequestSet, capacitiesPerDeliveryAreaAndTimeWindow,
				capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindowFinal,
				capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow);

		HashMap<Integer, Integer> notOfferedAnythingBecauseNoFeasible53PerDA = new HashMap<Integer, Integer>();
		for (Integer area : capacitiesPerDeliveryAreaAndTimeWindow.keySet()) {
			notOfferedAnythingBecauseNoFeasible53PerDA.put(area, 0);
		}
		for (OrderRequest request : requests) {

			// Determine respective delivery area
			DeliveryArea rArea = LocationService.assignCustomerToDeliveryAreaConsideringHierarchy(daSet,
					request.getCustomer());

			// Determine feasible time windows (that are in consideration set)
			// based on routes of delivery area set

			HashMap<Integer, RouteElement> feasibleTimeWindows = DynamicRoutingHelperService
					.getFeasibleTimeWindowsBasedOnCheapestInsertionHeuristic(request,
							routesPerDeliveryAreaSet.get(rArea.getSetId()),
							vehicleAssignmentPerDeliveryAreaAndVehicleNo.get(rArea.getDeliveryAreaOfSet().getId()),
							region, timeMultiplier, expectedServiceTime, timeWindowSet,
							(includeDriveFromStartingPosition == 1));
			ArrayList<TimeWindow> consideredAndFeasibleTimeWindows = new ArrayList<TimeWindow>();
			HashMap<TimeWindow, Alternative> alternativesToTimeWindows = new HashMap<TimeWindow, Alternative>();
			for (ConsiderationSetAlternative alt : request.getCustomer().getOriginalDemandSegment()
					.getConsiderationSet()) {
				if (!alt.getAlternative().getNoPurchaseAlternative()) {
					if (feasibleTimeWindows.containsKey(alt.getAlternative().getTimeWindows().get(0).getId())) {
						if (capacitiesPerDeliveryAreaAndTimeWindow.get(rArea.getId())
								.get(alt.getAlternative().getTimeWindows().get(0).getId()).getCapacityNumber() > 0) {
							consideredAndFeasibleTimeWindows.add(alt.getAlternative().getTimeWindows().get(0));
						}
					}
					alternativesToTimeWindows.put(alt.getAlternative().getTimeWindows().get(0), alt.getAlternative());
				}

			}

			// Determine future value with same capacities
			double expectedArrivals = ArrivalProcessService.getMeanArrivalProbability(arrivalProcessId)
					* (request.getArrivalTime() - 1) * daWeights.get(rArea) * (1.0 - riskRange);

			OrderRequest currentRequest = null;
			if (actualValue)
				currentRequest = request;

			double futureValueSameCapacities = LinearProgrammingService
					.determineDeterministicLinearProgrammingSolutionPerDeliveryArea(daSegmentWeightings.get(rArea),
							capacitiesPerDeliveryAreaAndTimeWindow.get(rArea.getId()),
							capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindowFinal.get(rArea.getId()),
							capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow.get(rArea.getId()),
							lowerBoundDemandMultiplierPerSegment, upperBoundDemandMultiplierPerSegment, beta,
							expectedArrivals, maximumRevenueValue, objectiveSpecificValues, timeWindowSet,
							(request.getArrivalTime() - 1), periodLength, capacityRange[1], capacityRange[0], betaRange,
							currentRequest);

			// For all time windows that are feasible,
			// determine future value without the respective resource
			HashMap<TimeWindow, Double> futureValueLessCapacities = new HashMap<TimeWindow, Double>();
			for (TimeWindow tw : consideredAndFeasibleTimeWindows) {
				capacitiesPerDeliveryAreaAndTimeWindow.get(rArea.getId()).get(tw.getId()).setCapacityNumber(
						capacitiesPerDeliveryAreaAndTimeWindow.get(rArea.getId()).get(tw.getId()).getCapacityNumber()
								- 1);
				for (DemandSegment ds : orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getElements()) {
					for (Integer twId : capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow
							.get(rArea.getId()).get(ds.getId()).keySet()) {
						if (twId != tw.getId()) {
							int oldNumber = capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow
									.get(rArea.getId()).get(ds.getId()).get(twId).getCapacityNumber();
							capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow.get(rArea.getId())
									.get(ds.getId()).get(twId).setCapacityNumber(oldNumber - 1);
						}
					}
				}
				double futureValueLess = LinearProgrammingService
						.determineDeterministicLinearProgrammingSolutionPerDeliveryArea(daSegmentWeightings.get(rArea),
								capacitiesPerDeliveryAreaAndTimeWindow.get(rArea.getId()),
								capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindowFinal
										.get(rArea.getId()),
								capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow.get(rArea.getId()),
								lowerBoundDemandMultiplierPerSegment, upperBoundDemandMultiplierPerSegment, beta,
								expectedArrivals, maximumRevenueValue, objectiveSpecificValues, timeWindowSet,
								(request.getArrivalTime() - 1), periodLength, capacityRange[1], capacityRange[0],
								betaRange, currentRequest);
				capacitiesPerDeliveryAreaAndTimeWindow.get(rArea.getId()).get(tw.getId()).setCapacityNumber(
						capacitiesPerDeliveryAreaAndTimeWindow.get(rArea.getId()).get(tw.getId()).getCapacityNumber()
								+ 1);
				for (DemandSegment ds : orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getElements()) {
					for (Integer twId : capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow
							.get(rArea.getId()).get(ds.getId()).keySet()) {
						if (twId != tw.getId()) {
							int oldNumber = capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow
									.get(rArea.getId()).get(ds.getId()).get(twId).getCapacityNumber();
							capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow.get(rArea.getId())
									.get(ds.getId()).get(twId).setCapacityNumber(oldNumber + 1);
						}
					}
				}
				futureValueLessCapacities.put(tw, futureValueLess);
			}

			Pair<ArrayList<AlternativeOffer>, Double> bestOffer = AssortmentProblemService.determineBestOfferSet(
					request, futureValueLessCapacities, futureValueSameCapacities, maximumRevenueValue,
					objectiveSpecificValues, algo, alternativesToTimeWindows, possiblyLargeOfferSet, actualValue, false,
					null, null, DISCOUNT_FACTOR);
			ArrayList<AlternativeOffer> bestOfferedAlternatives = bestOffer.getKey();
			// Simulate customer decision and insert into route
			Order order = new Order();
			order.setOrderRequest(request);
			order.setOrderRequestId(request.getId());
			order.setAccepted(false);

			// TODO Check: bestofferedAlternativesPlausibility
			if (bestOfferedAlternatives.size() > 0) { // If windows are offered
				// Sample selection from customer
				AlternativeOffer selectedAlt;
				if (usepreferencesSampled) {
					selectedAlt = CustomerDemandService.selectCustomerDemandBasedOnSampledPreferences(
							bestOfferedAlternatives, order, orderRequestSet.getCustomerSet()
									.getOriginalDemandSegmentSet().getAlternativeSet().getNoPurchaseAlternative());

				} else {
					selectedAlt = CustomerDemandService.sampleCustomerDemand(bestOfferedAlternatives, order);
				}

				if (selectedAlt != null) {
					if (!selectedAlt.getAlternative().getNoPurchaseAlternative()) {
						order.setTimeWindowFinal(selectedAlt.getAlternative().getTimeWindows().get(0));
						order.setTimeWindowFinalId(selectedAlt.getAlternative().getTimeWindows().get(0).getId());
						order.setAccepted(true);
						DynamicRoutingHelperService.insertRouteElement(
								feasibleTimeWindows.get(order.getTimeWindowFinalId()),
								routesPerDeliveryAreaSet.get(rArea.getSetId()),
								vehicleAssignmentPerDeliveryAreaAndVehicleNo.get(rArea.getDeliveryAreaOfSet().getId()),
								timeWindowSet, timeMultiplier, (includeDriveFromStartingPosition == 1));

						capacitiesPerDeliveryAreaAndTimeWindow.get(rArea.getId()).get(order.getTimeWindowFinalId())
								.setCapacityNumber(capacitiesPerDeliveryAreaAndTimeWindow.get(rArea.getId())
										.get(order.getTimeWindowFinalId()).getCapacityNumber() - 1);

						for (DemandSegment ds : orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet()
								.getElements()) {
							for (Integer twId : capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow
									.get(rArea.getId()).get(ds.getId()).keySet()) {
								if (twId != order.getTimeWindowFinalId()) {
									int oldNumber = capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow
											.get(rArea.getId()).get(ds.getId()).get(twId).getCapacityNumber();
									capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow
											.get(rArea.getId()).get(ds.getId()).get(twId)
											.setCapacityNumber(oldNumber - 1);
								}
							}
						}

					} else {
						if (bestOfferedAlternatives.size() > 3)
							System.out.println("bad luck");
						order.setReasonRejection("No-purchase alternative selected");
					}

				} else {

					order.setReasonRejection("No-purchase alternative selected");

				}

			} else {
				for (Integer id : feasibleTimeWindows.keySet()) {
					if (request.getArrivalTime() < 15 && request.getCustomer().getOriginalDemandSegmentId() == 53
							&& feasibleTimeWindows.size() > 0 && capacitiesPerDeliveryAreaAndTimeWindow
									.get(rArea.getId()).get(id).getCapacityNumber() > 0)
						System.out.println("Why????");
				}

				order.setReasonRejection("No alternative offered");
			}

			// Add order to order list
			orders.add(order);
		}
		// TODO Check: Routes and left over capacities
		int sum = 0;
		for (Integer daId : capacitiesPerDeliveryAreaAndTimeWindow.keySet()) {
			for (Integer twId : capacitiesPerDeliveryAreaAndTimeWindow.get(daId).keySet()) {
				sum += capacitiesPerDeliveryAreaAndTimeWindow.get(daId).get(twId).getCapacityNumber();
			}
		}
		System.out.println("Left over capacities: " + sum);
		return orders;
	}

	private static double determineDeterministicLinearProgrammingSolutionPerDeliveryArea(
			DemandSegmentWeighting demandSegmentWeighting, HashMap<Integer, Capacity> capacitiesPerTimeWindow,
			HashMap<Integer, HashMap<Integer, Capacity>> capacitiesOtherTimeWindowsPerDemandSegmentAndTimeWindowFinal,
			HashMap<Integer, HashMap<Integer, Capacity>> capacitiesOtherTimeWindowsPerDemandSegmentAndTimeWindow,
			HashMap<DemandSegment, HashMap<TimeWindow, Double>> lowerBoundDemandMultiplierPerSegment,
			HashMap<DemandSegment, HashMap<TimeWindow, Double>> upperBoundDemandMultiplierPerSegment, double beta,
			double expectedArrivals, double maximumRevenueValue, HashMap<Entity, Object> objectiveSpecificValues,
			TimeWindowSet timeWindowSet, int leftOverTime, int periodLength, double maximumOtherCapacityNumber,
			double minimumOtherCapacityNumber, double betaRange, OrderRequest request) {

//		Double separatingValue = null;
//		if (request != null) {
//			separatingValue = CustomerDemandService.calculateOrderValue(request, maximumRevenueValue,
//					objectiveSpecificValues);
//		}
//		PrintStream originalStream = System.out;
//
//		PrintStream dummyStream = new PrintStream(new OutputStream() {
//			public void write(int b) {
//				// NO-OP
//			}
//		});
//
//		System.setOut(dummyStream);
//
//		int sumCapacity = 0;
//		for (Integer capId : capacitiesPerTimeWindow.keySet()) {
//			sumCapacity += capacitiesPerTimeWindow.get(capId).getCapacityNumber();
//		}
//		System.out.println("SumCapacity: " + sumCapacity);
//		if (expectedArrivals < sumCapacity)
//			System.out.println("Too much capacity - for real (not considering no-purchase!)");
//
//		ArrayList<DemandSegment> segments = ((DemandSegmentSet) demandSegmentWeighting.getSetEntity()).getElements();
//		LPWizard lpw = new LPWizard();
//		lpw.setMinProblem(false);
//
//		// Set objective function
//		String variableName = "x_";
//		HashMap<DemandSegment, Double> expectedValues = new HashMap<DemandSegment, Double>();
//		for (DemandSegment segment : segments) {
//
//			double value = CustomerDemandService.calculateExpectedValue(maximumRevenueValue, objectiveSpecificValues,
//					segment);
//			expectedValues.put(segment, value);
//
//			for (TimeWindow tw : timeWindowSet.getElements()) {
//
//				String currentVariableName = variableName + tw.getId() + "_" + segment.getId();
//
//				lpw = lpw.plus(currentVariableName, value);
//
//				double lowerValue = separatingValue;
//				if (separatingValue < expectedValues.get(segment)) {
//					try {
//						lowerValue = (separatingValue + CustomerDemandService.calculateExpectedValue(
//								ProbabilityDistributionService.getXByCummulativeDistributionQuantile(
//										segment.getBasketValueDistribution(), 0.1),
//								maximumRevenueValue, objectiveSpecificValues, segment)) / 2.0;
//					} catch (ParameterUnknownException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}
//				if (separatingValue != null) {
//					currentVariableName = currentVariableName + "_below";
//
//					lpw = lpw.plus(currentVariableName, lowerValue);
//				}
//			}
//		}
//
//		String evS = "";
//		for (Double v : expectedValues.values()) {
//			evS += v + "; ";
//		}
//
//		System.out.println(evS);
//		// Set constraints
//		// 1. Cannot allocate more per time window than available
//		// lpw.addConstraint("c1",4,">=").plus("x1",1).plus("x2",1).plus("x3",1);
//		String constraintName = "c_tw_";
//		for (Capacity cap : capacitiesPerTimeWindow.values()) {
//			String currentConstraintName = constraintName + cap.getTimeWindowId();
//
//			String logConstraint = currentConstraintName;
//			LPWizardConstraint constraint = lpw.addConstraint(currentConstraintName, cap.getCapacityNumber(), ">=");
//			logConstraint += cap.getCapacityNumber() + ">=";
//			for (DemandSegment segment : segments) {
//				constraint.plus("x_" + cap.getTimeWindowId() + "_" + segment.getId(), 1);
//				logConstraint += " " + "x_" + cap.getTimeWindowId() + "_" + segment.getId();
//				if (separatingValue != null) {
//					constraint.plus("x_" + cap.getTimeWindowId() + "_" + segment.getId() + "_below", 1);
//				}
//			}
//			System.out.println(logConstraint);
//		}
//
//		// 2. Per segment: Cannot allocate more than requested per time window
//		// AND: all variables must not be below 0
//		constraintName = "c_s_tw_";
//		String constraintNamePositive = "c_positive_";
//		for (DemandSegmentWeight segmentW : demandSegmentWeighting.getWeights()) {
//			for (Capacity cap : capacitiesPerTimeWindow.values()) {
//
//				double demandDouble;
//				if (!lowerBoundDemandMultiplierPerSegment.get(segmentW.getDemandSegment())
//						.containsKey(cap.getTimeWindow())) {
//					demandDouble = 0.0;
//
//				} else {
//					double betaMultiplier = beta
//							+ betaRange * ((((double) capacitiesOtherTimeWindowsPerDemandSegmentAndTimeWindowFinal
//									.get(segmentW.getElementId()).get(cap.getTimeWindowId()).getCapacityNumber())
//									- ((double) maximumOtherCapacityNumber + (double) minimumOtherCapacityNumber) / 2.0)
//									/ ((double) maximumOtherCapacityNumber));
//
//					// Weight with current capacity situation
//					betaMultiplier = betaMultiplier
//							* ((double) capacitiesOtherTimeWindowsPerDemandSegmentAndTimeWindow
//									.get(segmentW.getElementId()).get(cap.getTimeWindowId()).getCapacityNumber())
//							/ ((double) capacitiesOtherTimeWindowsPerDemandSegmentAndTimeWindowFinal
//									.get(segmentW.getElementId()).get(cap.getTimeWindowId()).getCapacityNumber());
//					betaMultiplier = Math.max(betaMultiplier, 0.0);
//					betaMultiplier = Math.min(betaMultiplier, 1.0);
//					demandDouble = ((double) expectedArrivals) * segmentW.getWeight()
//							* (betaMultiplier
//									* lowerBoundDemandMultiplierPerSegment.get(segmentW.getDemandSegment())
//											.get(cap.getTimeWindow())
//									+ (1.0 - betaMultiplier) * upperBoundDemandMultiplierPerSegment
//											.get(segmentW.getDemandSegment()).get(cap.getTimeWindow()));
//
//				}
//
//				if (separatingValue != null) {
//					double demandBelow = 0.0;
//					try {
//
//						double quota = ProbabilityDistributionService.getQuantileByCummulativeDistribution(
//								segmentW.getDemandSegment().getBasketValueDistribution(), request.getBasketValue());
//						double value = CustomerDemandService.calculateExpectedValue(maximumRevenueValue,
//								objectiveSpecificValues, segmentW.getDemandSegment());
//						if (separatingValue < value) {
//							demandBelow = demandDouble * quota;
//						} else {
//							demandBelow = demandDouble * (1.0 - quota);
//						}
//
//					} catch (ParameterUnknownException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//
//					long demandBelowLong = Math.round(demandBelow);
//
//					String currentConstraintName = constraintName + segmentW.getElementId() + cap.getTimeWindowId()
//							+ "_below";
//					LPWizardConstraint constraint = lpw.addConstraint(currentConstraintName, demandBelowLong, ">=");
//					constraint.plus("x_" + cap.getTimeWindowId() + "_" + segmentW.getElementId() + "_below", 1);
//
//					String currentConstraintPositive = constraintNamePositive + segmentW.getElementId()
//							+ cap.getTimeWindowId() + "_below";
//					LPWizardConstraint constraintPositive = lpw.addConstraint(currentConstraintPositive, 0, "<=");
//					constraintPositive.plus("x_" + cap.getTimeWindowId() + "_" + segmentW.getElementId() + "_below", 1);
//					demandDouble = demandDouble - demandBelowLong;
//				}
//
//				long demandNo = Math.round(demandDouble);
//
//				// Round down
//				if (demandNo > demandDouble)
//					demandNo = demandNo - 1;
//
//				String currentConstraintName = constraintName + segmentW.getElementId() + cap.getTimeWindowId();
//				LPWizardConstraint constraint = lpw.addConstraint(currentConstraintName, demandNo, ">=");
//				constraint.plus("x_" + cap.getTimeWindowId() + "_" + segmentW.getElementId(), 1);
//
//				String currentConstraintPositive = constraintNamePositive + segmentW.getElementId()
//						+ cap.getTimeWindowId();
//				LPWizardConstraint constraintPositive = lpw.addConstraint(currentConstraintPositive, 0, "<=");
//				constraintPositive.plus("x_" + cap.getTimeWindowId() + "_" + segmentW.getElementId(), 1);
//			}
//		}
//
//		// 3. Per segment: Cannot allocate more than requested over all time
//		// windows
//		constraintName = "c_s_";
//		int sumRequestedSegments = 0;
//		for (DemandSegmentWeight segmentW : demandSegmentWeighting.getWeights()) {
//			String currentConstraintName = constraintName + segmentW.getElementId();
//			// Demand are all arrivals for that segment - minus the lower bound
//			// of
//			// the no purchase probability
//			double demandDouble = ((double) expectedArrivals) * segmentW.getWeight()
//					* (1.0 - lowerBoundDemandMultiplierPerSegment.get(segmentW.getDemandSegment()).get(null));
//
//			if (separatingValue != null) {
//				double demandBelow = 0.0;
//
//				try {
//					double quota = ProbabilityDistributionService.getQuantileByCummulativeDistribution(
//							segmentW.getDemandSegment().getBasketValueDistribution(), request.getBasketValue());
//					double value = CustomerDemandService.calculateExpectedValue(maximumRevenueValue,
//							objectiveSpecificValues, segmentW.getDemandSegment());
//					if (separatingValue < value) {
//						demandBelow = demandDouble * quota;
//					} else {
//						demandBelow = demandDouble * (1.0 - quota);
//					}
//
//				} catch (ParameterUnknownException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//
//				long demandBelowLong = Math.round(demandBelow);
//				if (demandBelowLong > demandDouble)
//					demandBelowLong = demandBelowLong - 1;
//
//				LPWizardConstraint constraint = lpw.addConstraint(currentConstraintName, demandBelowLong, ">=");
//
//				for (Capacity cap : capacitiesPerTimeWindow.values()) {
//
//					constraint.plus("x_" + cap.getTimeWindowId() + "_" + segmentW.getElementId() + "_below", 1);
//
//				}
//
//				demandDouble = demandDouble - demandBelowLong;
//			}
//
//			long demandNo = Math.round(demandDouble);
//			if (demandNo > demandDouble)
//				demandNo = demandNo - 1;
//			sumRequestedSegments += demandNo;
//			String logConstraint = currentConstraintName + " " + demandNo;
//			LPWizardConstraint constraint = lpw.addConstraint(currentConstraintName, demandNo, ">=");
//			logConstraint += " " + demandNo + ">=";
//			for (Capacity cap : capacitiesPerTimeWindow.values()) {
//
//				constraint.plus("x_" + cap.getTimeWindowId() + "_" + segmentW.getElementId(), 1);
//				logConstraint += " " + "x_" + cap.getTimeWindowId() + "_" + segmentW.getElementId();
//			}
//			// System.out.println(logConstraint);
//		}
//
//		LPSolution sol = lpw.solve();
//
		double result = 0.0;

//		result= sol.getObjectiveValue();
//
//		int toSell = 0;
//		double objV = 0.0;
//		for (DemandSegmentWeight segmentW : demandSegmentWeighting.getWeights()) {
//			for (Capacity cap : capacitiesPerTimeWindow.values()) {
//				toSell += sol.getInteger("x_" + cap.getTimeWindowId() + "_" + segmentW.getElementId());
//				toSell += sol.getInteger("x_" + cap.getTimeWindowId() + "_" + segmentW.getElementId() + "_below");
//				objV += sol.getInteger("x_" + cap.getTimeWindowId() + "_" + segmentW.getElementId())
//						* expectedValues.get(segmentW.getDemandSegment());
//				objV += sol.getInteger("x_" + cap.getTimeWindowId() + "_" + segmentW.getElementId() + "_below")
//						* separatingValue;
//			}
//		}
//
//		System.setOut(originalStream);
//		if (Math.round(expectedArrivals * (1 - 0.111)) > sumRequestedSegments + 1)
//			System.out.println("Aggregation problem: expected arrivals is " + expectedArrivals
//					+ " and requested over segments is " + sumRequestedSegments);
//
//		if (toSell < sumCapacity - 1) {
//			System.out.println("Too much capacity - optimization");
//			for (DemandSegmentWeight segmentW : demandSegmentWeighting.getWeights()) {
//				for (Capacity cap : capacitiesPerTimeWindow.values()) {
//
//					long demandNo;
//					if (!lowerBoundDemandMultiplierPerSegment.get(segmentW.getDemandSegment())
//							.containsKey(cap.getTimeWindow())) {
//						demandNo = 0;
//						if (segmentW.getDemandSegment().getId() == 53)
//							System.out.println("Something strange");
//					} else {
//						double betaMultiplier = beta
//								+ betaRange * ((((double) capacitiesOtherTimeWindowsPerDemandSegmentAndTimeWindow
//										.get(segmentW.getElementId()).get(cap.getTimeWindowId()).getCapacityNumber())
//										- (maximumOtherCapacityNumber + minimumOtherCapacityNumber) / 2)
//										/ maximumOtherCapacityNumber);
//						// *2*capacitiesOtherTimeWindowsPerDemandSegmentAndTimeWindow.get(segmentW.getElementId()).get(cap.getTimeWindowId()).getCapacityNumber()/maximumOtherCapacityNumber*2*(periodLength-leftOverTime)/((double)periodLength);
//						betaMultiplier = Math.max(betaMultiplier, 0);
//						betaMultiplier = Math.min(betaMultiplier, 1);
//						demandNo = Math.round(((double) expectedArrivals) * segmentW.getWeight()
//								* (betaMultiplier
//										* lowerBoundDemandMultiplierPerSegment.get(segmentW.getDemandSegment())
//												.get(cap.getTimeWindow())
//										+ (1.0 - betaMultiplier) * upperBoundDemandMultiplierPerSegment
//												.get(segmentW.getDemandSegment()).get(cap.getTimeWindow())));
//
//					}
//				}
//			}
//		}

		return result;

	}

	public static Pair<Double, HashMap<TimeWindow, HashMap<DemandSegment, HashMap<Integer, Double>>>> determineDeterministicLinearProgrammingSolutionPerDeliveryAreaWithR(
			DemandSegmentWeighting demandSegmentWeighting, HashMap<Integer, Integer> capacitiesPerTimeWindow,
			HashMap<DemandSegment, HashMap<Alternative, Double>> demandMultiplierPerSegment, double expectedArrivals,
			double maximumRevenueValue, HashMap<Entity, Object> objectiveSpecificValues, TimeWindowSet timeWindowSet, RConnection connection, double expectedRevenueQuantile,
			HashMap<TimeWindow, Alternative> alternativesToTimeWindows, Alternative noPurchaseAlternative,
			boolean integerProgram, boolean splitSegmentsBasedOnMean) throws RserveException, REXPMismatchException {

		// Prepare connection
		if (connection == null) {

			connection = new RConnection();
			connection.eval("library(lpSolve)");

		}
		;

		double value = 0.0;
		// Prepare time window pointer
		HashMap<Integer, Integer> timeWindowPointer = new HashMap<Integer, Integer>();
		int index = 0;
		int numberOfTimeWindows = timeWindowSet.getElements().size();
		for (TimeWindow tw : timeWindowSet.getElements()) {
			timeWindowPointer.put(tw.getId(), index);
			if (splitSegmentsBasedOnMean) {
				index = index + demandSegmentWeighting.getWeights().size() * 2;
			} else {
				index = index + demandSegmentWeighting.getWeights().size();
			}

		}

		// Prepare segment index
		HashMap<Integer, Integer> demandSegmentPointer = new HashMap<Integer, Integer>();
		index = 0;
		int numberOfSegments;
		if (splitSegmentsBasedOnMean) {
			numberOfSegments = demandSegmentWeighting.getWeights().size() * 2;
		} else {
			numberOfSegments = demandSegmentWeighting.getWeights().size();
		}
		for (DemandSegmentWeight ds : demandSegmentWeighting.getWeights()) {
			demandSegmentPointer.put(ds.getDemandSegment().getId(), index);
			index++;
		}

		ArrayList<Double> objectiveFunctionCoefficients = new ArrayList<Double>();
		// ArrayList<String> constraints = new ArrayList<String>();
		connection.eval("constraintMatrix=c()");
		connection.eval("constraintOperators=c()");
		connection.eval("constraintValues=c()");
		for (TimeWindow tw : timeWindowSet.getElements()) {

			// Objective function coefficients
			for (DemandSegmentWeight ds : demandSegmentWeighting.getWeights()) {
				if (splitSegmentsBasedOnMean) {
					objectiveFunctionCoefficients.add(CustomerDemandService.calculateQuantileValue(maximumRevenueValue,
							objectiveSpecificValues, ds.getDemandSegment(), 0.5));
				}
				objectiveFunctionCoefficients.add(CustomerDemandService.calculateQuantileValue(maximumRevenueValue,
						objectiveSpecificValues, ds.getDemandSegment(), expectedRevenueQuantile));

			}

			// Constraints 1: Can not assign more than available
			ArrayList<Double> timeWindowConstraints = new ArrayList<Double>();
			for (int i = 0; i < timeWindowPointer.get(tw.getId()); i++) {
				timeWindowConstraints.add(0.0);
			}

			for (int i = timeWindowPointer.get(tw.getId()); i < timeWindowPointer.get(tw.getId())
					+ numberOfSegments; i++) {
				timeWindowConstraints.add(1.0);
			}

			for (int i = timeWindowPointer.get(tw.getId()) + numberOfSegments; i < numberOfTimeWindows
					* numberOfSegments; i++) {
				timeWindowConstraints.add(0.0);
			}

			int currentCapacity = 0;
			if (capacitiesPerTimeWindow != null && capacitiesPerTimeWindow.get(tw.getId()) != null) {
				currentCapacity = capacitiesPerTimeWindow.get(tw.getId());
			}
			connection.eval("constraintMatrix=rbind(constraintMatrix,"
					+ LinearProgrammingService.toRVectorStringDouble(timeWindowConstraints) + ")");
			connection.eval("constraintOperators=c(constraintOperators, \"<=\")");
			connection.eval("constraintValues=c(constraintValues," + currentCapacity + ")");

		}

		String objectiveFunctionString = LinearProgrammingService.toRVectorStringDouble(objectiveFunctionCoefficients);
		connection.eval("objectiveFunctionValues=" + objectiveFunctionString);

		// Constraints 2: Segment-related constraints

		for (DemandSegmentWeight dsw : demandSegmentWeighting.getWeights()) {

			// Per segment, the number of assigned capacities can not be higher
			// than the number of expected arrivals

			int numberOfSubSegments = 1;
			if (splitSegmentsBasedOnMean) {
				numberOfSubSegments = 2;
			}

			for (int subS = 0; subS < numberOfSubSegments; subS++) {
				int currentDemandSegmentIndex = demandSegmentPointer.get(dsw.getElementId()) + subS;
				HashMap<Integer, Double> segmentConstraints = new HashMap<Integer, Double>();

				for (TimeWindow timeWindow : timeWindowSet.getElements()) {

					// Collect for current time window to restrict sum over all
					// time windows for that segment

					for (int a = timeWindowPointer.get(timeWindow.getId()); a < timeWindowPointer
							.get(timeWindow.getId()) + currentDemandSegmentIndex; a++) {
						segmentConstraints.put(a, 0.0);
					}

					segmentConstraints.put(timeWindowPointer.get(timeWindow.getId()) + currentDemandSegmentIndex, 1.0);

					for (int a = timeWindowPointer.get(timeWindow.getId()) + currentDemandSegmentIndex
							+ 1; a < timeWindowPointer.get(timeWindow.getId()) + numberOfSegments; a++) {
						segmentConstraints.put(a, 0.0);
					}

					// Cannot assign more than requested per segment and time
					// window
					ArrayList<Double> twSegConstraints = new ArrayList<Double>();
					for (int i = 0; i < timeWindowPointer.get(timeWindow.getId()) + currentDemandSegmentIndex; i++) {
						twSegConstraints.add(0.0);
					}
					twSegConstraints.add(1.0);
					for (int i = timeWindowPointer.get(timeWindow.getId()) + currentDemandSegmentIndex
							+ 1; i < numberOfTimeWindows * numberOfSegments; i++) {
						twSegConstraints.add(0.0);
					}

					connection.eval("constraintMatrix=rbind(constraintMatrix,"
							+ LinearProgrammingService.toRVectorStringDouble(twSegConstraints) + ")");
					connection.eval("constraintOperators=c(constraintOperators, \"<=\")");
					double multiplier = 0.0;
					if (demandMultiplierPerSegment.get(dsw.getDemandSegment())
							.containsKey(alternativesToTimeWindows.get(timeWindow))) {
						multiplier = demandMultiplierPerSegment.get(dsw.getDemandSegment())
								.get(alternativesToTimeWindows.get(timeWindow));
					}
					double expectedArrivalsWeighted = (expectedArrivals * dsw.getWeight() * multiplier);
					long intExpectedValue;
					if (splitSegmentsBasedOnMean) {
						intExpectedValue = Math.round(expectedArrivalsWeighted * 0.5);
						if (subS == 1 && intExpectedValue * 2.0 > expectedArrivalsWeighted)
							intExpectedValue--;
					} else {
						intExpectedValue = Math.round(expectedArrivalsWeighted);
						if (intExpectedValue > expectedArrivalsWeighted)
							intExpectedValue--;
					}

					connection.eval("constraintValues=c(constraintValues," + intExpectedValue + ")");

					// Also: Values need to be positive
					connection.eval("constraintMatrix=rbind(constraintMatrix,"
							+ LinearProgrammingService.toRVectorStringDouble(twSegConstraints) + ")");
					connection.eval("constraintOperators=c(constraintOperators, \">=\")");
					connection.eval("constraintValues=c(constraintValues,0)");
				}

				ArrayList<Double> sortedSegmentConstraints = new ArrayList<Double>();
				for (int i = 0; i < numberOfTimeWindows * numberOfSegments; i++) {
					sortedSegmentConstraints.add(segmentConstraints.get(i));

				}
				connection.eval("constraintMatrix=rbind(constraintMatrix,"
						+ LinearProgrammingService.toRVectorStringDouble(sortedSegmentConstraints) + ")");
				connection.eval("constraintOperators=c(constraintOperators, \"<=\")");
				long intExpectedValue;

				if (splitSegmentsBasedOnMean) {
					intExpectedValue = Math.round((expectedArrivals * dsw.getWeight()
							* (1.0 - demandMultiplierPerSegment.get(dsw.getDemandSegment()).get(noPurchaseAlternative)))
							* 0.5);
					if (subS == 1 && intExpectedValue * 2.0 > (expectedArrivals * dsw.getWeight() * (1.0
							- demandMultiplierPerSegment.get(dsw.getDemandSegment()).get(noPurchaseAlternative))))
						intExpectedValue--;
				} else {
					intExpectedValue = Math.round((expectedArrivals * dsw.getWeight() * (1.0
							- demandMultiplierPerSegment.get(dsw.getDemandSegment()).get(noPurchaseAlternative))));
					if (intExpectedValue > (expectedArrivals * dsw.getWeight() * (1.0
							- demandMultiplierPerSegment.get(dsw.getDemandSegment()).get(noPurchaseAlternative))))
						intExpectedValue--;
				}

				connection.eval("constraintValues=c(constraintValues," + intExpectedValue + ")");
			}

		}

		if (integerProgram) {
			connection.voidEval(
					"solution <- lp(direction=\"max\", objective.in=objectiveFunctionValues, const.mat=constraintMatrix,const.dir=constraintOperators,const.rhs=constraintValues, all.int=TRUE)");
		} else {
			connection.voidEval(
					"solution <- lp(direction=\"max\", objective.in=objectiveFunctionValues, const.mat=constraintMatrix,const.dir=constraintOperators,const.rhs=constraintValues, all.int=FALSE)");
		}
		value = connection.eval("solution$objval").asDouble();
		double[] solution = connection.eval("solution$solution").asDoubles();

		HashMap<TimeWindow, HashMap<DemandSegment, HashMap<Integer, Double>>> assignedCapacityPerTimeWindowAndSegment = new HashMap<TimeWindow, HashMap<DemandSegment, HashMap<Integer, Double>>>();
		int numberOfSubSegments = 1;
		if (splitSegmentsBasedOnMean) {
			numberOfSubSegments = 2;
		}
		for (TimeWindow tw : timeWindowSet.getElements()) {
			assignedCapacityPerTimeWindowAndSegment.put(tw, new HashMap<DemandSegment, HashMap<Integer, Double>>());
			for (DemandSegmentWeight dsw : demandSegmentWeighting.getWeights()) {
				assignedCapacityPerTimeWindowAndSegment.get(tw).put(dsw.getDemandSegment(), new HashMap<Integer, Double>());
				for (int subS = 0; subS < numberOfSubSegments; subS++) {
					int currentDemandSegmentIndex = demandSegmentPointer.get(dsw.getElementId()) + subS;
					int segmentPrio = 0;
					if((numberOfSubSegments>1 && expectedRevenueQuantile>0.5 &&subS==0) || (numberOfSubSegments>1 && expectedRevenueQuantile<=0.5 &&subS==1)){
						segmentPrio=1;
					}
						
					assignedCapacityPerTimeWindowAndSegment.get(tw).get(dsw.getDemandSegment()).put(segmentPrio,
							solution[timeWindowPointer.get(tw.getId()) + currentDemandSegmentIndex]);
				}
			}

		}

		return new Pair<Double,HashMap<TimeWindow, HashMap<DemandSegment, HashMap<Integer, Double>>>>(value, assignedCapacityPerTimeWindowAndSegment);

	}

	private static String toRVectorStringDouble(ArrayList<Double> valueList) {

		StringBuilder buffer = new StringBuilder();
		buffer.append("c(");
		for (int i = 0; i < valueList.size() - 1; i++) {
			buffer.append(valueList.get(i) + ",");
		}

		buffer.append(valueList.get(valueList.size() - 1) + ")");

		return buffer.toString();
	}

	private static double[] prepareCapacities(HashMap<Integer, ArrayList<Capacity>> capacitiesPerDeliveryAreaToCopy,
			OrderRequestSet orderRequestSet,
			HashMap<Integer, HashMap<Integer, Capacity>> capacitiesPerDeliveryAreaAndTimeWindow,
			HashMap<Integer, HashMap<Integer, HashMap<Integer, Capacity>>> capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindowFinal,
			HashMap<Integer, HashMap<Integer, HashMap<Integer, Capacity>>> capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow) {

		for (Integer daId : capacitiesPerDeliveryAreaToCopy.keySet()) {
			HashMap<Integer, Capacity> capacities = new HashMap<Integer, Capacity>();
			HashMap<Integer, HashMap<Integer, Capacity>> capacitiesSegments = new HashMap<Integer, HashMap<Integer, Capacity>>();
			for (Capacity c : capacitiesPerDeliveryAreaToCopy.get(daId)) {
				Capacity cCopy = c.copy();
				capacities.put(c.getTimeWindowId(), cCopy);
				for (DemandSegment ds : orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getElements()) {
					if (capacitiesSegments.containsKey(ds.getId())) {

						for (ConsiderationSetAlternative csAlt : ds.getConsiderationSet()) {
							if (!csAlt.getAlternative().getNoPurchaseAlternative()) {
								TimeWindow tw = csAlt.getAlternative().getTimeWindows().get(0);
								if (tw.getId() != c.getTimeWindowId()) {
									if (capacitiesSegments.get(ds.getId()).containsKey(tw.getId())) {
										capacitiesSegments.get(ds.getId()).get(tw.getId()).setCapacityNumber(
												capacitiesSegments.get(ds.getId()).get(tw.getId()).getCapacityNumber()
														+ c.getCapacityNumber());
									} else {
										Capacity cap = new Capacity();
										cap.setTimeWindowId(c.getTimeWindowId());
										cap.setDeliveryAreaId(c.getDeliveryAreaId());
										cap.setCapacityNumber(c.getCapacityNumber());
										capacitiesSegments.get(ds.getId()).put(tw.getId(), cap);
									}

								}
							}
						}

					} else {
						HashMap<Integer, Capacity> capDS = new HashMap<Integer, Capacity>();

						for (ConsiderationSetAlternative csAlt : ds.getConsiderationSet()) {
							if (!csAlt.getAlternative().getNoPurchaseAlternative()) {
								TimeWindow tw = csAlt.getAlternative().getTimeWindows().get(0);
								if (tw.getId() != c.getTimeWindowId()) {
									Capacity cap = new Capacity();
									cap.setTimeWindowId(c.getTimeWindowId());
									cap.setDeliveryAreaId(c.getDeliveryAreaId());
									cap.setCapacityNumber(c.getCapacityNumber());
									capDS.put(tw.getId(), cap);
								}
							}
						}
						capacitiesSegments.put(ds.getId(), capDS);

					}
				}
			}
			capacitiesPerDeliveryAreaAndTimeWindow.put(daId, capacities);
			capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow.put(daId, capacitiesSegments);
			capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindowFinal.put(daId, capacitiesSegments);
		}

		double maximumOtherCapacityNumber = 0.0;
		double minimumOtherCapacityNumber = Double.MAX_VALUE;

		for (Integer daId : capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow.keySet()) {
			for (Integer segId : capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow.get(daId)
					.keySet()) {
				for (Integer twId : capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow.get(daId)
						.get(segId).keySet()) {
					if (capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow.get(daId).get(segId)
							.get(twId).getCapacityNumber() > maximumOtherCapacityNumber)
						maximumOtherCapacityNumber = capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow
								.get(daId).get(segId).get(twId).getCapacityNumber();
					if (capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow.get(daId).get(segId)
							.get(twId).getCapacityNumber() < minimumOtherCapacityNumber)
						minimumOtherCapacityNumber = capacitiesOtherTimeWindowsPerDeliveryAreaDemandSegmentAndTimeWindow
								.get(daId).get(segId).get(twId).getCapacityNumber();
				}
			}
		}

		return new double[] { minimumOtherCapacityNumber, maximumOtherCapacityNumber };
	}

}
