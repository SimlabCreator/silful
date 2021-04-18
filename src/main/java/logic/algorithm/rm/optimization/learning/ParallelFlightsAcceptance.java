package logic.algorithm.rm.optimization.learning;

import java.util.ArrayList;
import java.util.HashMap;

import data.entity.Alternative;
import data.entity.AlternativeOffer;
import data.entity.Capacity;
import data.entity.CapacitySet;
import data.entity.ConsiderationSetAlternative;
import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.DemandSegment;
import data.entity.DemandSegmentSet;
import data.entity.DemandSegmentWeighting;
import data.entity.Entity;
import data.entity.Order;
import data.entity.OrderRequestSet;
import data.entity.OrderSet;
import data.entity.Region;
import data.entity.TimeWindow;
import data.entity.TimeWindowSet;
import logic.algorithm.rm.optimization.acceptance.AcceptanceAlgorithm;
import logic.entity.AssortmentAlgorithm;
import logic.service.support.CustomerDemandService;
import logic.service.support.LinearProgrammingService;

/**
 * ParallelFlightsAcceptance
 * @author M. Lang
 *
 */
public class ParallelFlightsAcceptance implements AcceptanceAlgorithm {

	private static double BETA_RANGE = 0.1;
	private static double RISK_RANGE =0.1;
	private static boolean possiblyLargeOfferSet=true;
	private static AssortmentAlgorithm algo = AssortmentAlgorithm.REVENUE_ORDERED_SET;// TODO:
																						// As
																						// input
																						// parameter!

	private static String[] paras = new String[] { "samplePreferences" ,"actualBasketValue"};
	private CapacitySet capacitySet;
	private double beta;
	private Region region;
	private OrderRequestSet orderRequestSet;
	private TimeWindowSet timeWindowSet;
	private double maximumRevenueValue;
	private HashMap<Entity, Object> objectiveSpecificValues;
	private OrderSet result;
	private HashMap<DemandSegment, HashMap<TimeWindow, Double>> lowerBoundDemandMultiplierPerSegment;
	private HashMap<DemandSegment, HashMap<TimeWindow, Double>> upperBoundDemandMultiplierPerSegment;

	private HashMap<DeliveryArea, Double> daWeights; // Should be for lowest da
														// level
	private HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightings; // Should
																				// be
																				// for
																				// lowest
																				// da
																				// level
	private int arrivalProcessId;
	private boolean usepreferencesSampled;
	private DemandSegmentSet demandSegmentSet;
	private int periodLength;
	private DeliveryAreaSet deliveryAreaSet;
	private boolean actualValue;

	public ParallelFlightsAcceptance(Region region, DeliveryAreaSet deliveryAreaSet, DemandSegmentSet demandSegmentSet,
			OrderRequestSet orderRequestSet, CapacitySet capacitySet, HashMap<Entity, Object> objectiveSpecificValues,
			Double maximumRevenueValue, int arrivalProcessId, HashMap<DeliveryArea, Double> daWeights,
			HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightings, Double samplePreferences,
			int periodLength, Double actualValue) {
		this.region = region;
		this.deliveryAreaSet = deliveryAreaSet;
		this.orderRequestSet = orderRequestSet;
		this.capacitySet = capacitySet;
		this.timeWindowSet = this.orderRequestSet.getCustomerSet().getOriginalDemandSegmentSet().getAlternativeSet()
				.getTimeWindowSet();

		this.maximumRevenueValue = maximumRevenueValue;
		this.objectiveSpecificValues = objectiveSpecificValues;
		this.daSegmentWeightings = daSegmentWeightings;
		this.daWeights = daWeights;
		this.arrivalProcessId = arrivalProcessId;
		this.demandSegmentSet = demandSegmentSet;

		if (samplePreferences == 1.0) {
			this.usepreferencesSampled = true;
		} else {
			this.usepreferencesSampled = false;
		}
		
		if(actualValue==1.0){
			this.actualValue=true;
		}else{
			this.actualValue=false;
		}
		this.periodLength = periodLength;
	};

	public void start() {

		// Initialise lower and upper bound demand multipliers
		this.initialiseDemandMultipliers();

		// Group capacities by delivery area
		HashMap<Integer, ArrayList<Capacity>> capacitiesPerDeliveryArea = new HashMap<Integer, ArrayList<Capacity>>();
		for (Capacity cap : capacitySet.getElements()) {
			if (!capacitiesPerDeliveryArea.containsKey(cap.getDeliveryAreaId())) {
				ArrayList<Capacity> capacities = new ArrayList<Capacity>();
				capacities.add(cap);
				capacitiesPerDeliveryArea.put(cap.getDeliveryAreaId(), capacities);
			} else {
				capacitiesPerDeliveryArea.get(cap.getDeliveryAreaId()).add(cap);
			}
		}

		this.beta = capacitySet.getWeight();

		// Simulate order horizon
		ArrayList<Order> acceptedOrders = LinearProgrammingService
				.simulateOrderHorizonForDeterministicLinearProgrammingWithStaticCapacityAssignment(beta,
						capacitiesPerDeliveryArea, orderRequestSet, deliveryAreaSet,region,
						timeWindowSet, daSegmentWeightings, daWeights, arrivalProcessId,
						lowerBoundDemandMultiplierPerSegment, upperBoundDemandMultiplierPerSegment, maximumRevenueValue,
						objectiveSpecificValues, usepreferencesSampled, periodLength,
						BETA_RANGE, algo, possiblyLargeOfferSet, actualValue, RISK_RANGE);

		OrderSet orderSet = new OrderSet();
		orderSet.setOrderRequestSet(orderRequestSet);
		orderSet.setOrderRequestSetId(orderRequestSet.getId());
		orderSet.setElements(acceptedOrders);
		this.result = orderSet;

	}

	private void initialiseDemandMultipliers() {

		// Initialise hashmaps
		lowerBoundDemandMultiplierPerSegment = new HashMap<DemandSegment, HashMap<TimeWindow, Double>>();
		upperBoundDemandMultiplierPerSegment = new HashMap<DemandSegment, HashMap<TimeWindow, Double>>();

		// Get upper and lower bound probabilities per segment
		for (DemandSegment s : this.demandSegmentSet.getElements()) {
			HashMap<TimeWindow, Double> lowerPerTimeWindow = new HashMap<TimeWindow, Double>();
			HashMap<TimeWindow, Double> upperPerTimeWindow = new HashMap<TimeWindow, Double>();

			
			AlternativeOffer noPurchaseOffer = null;
			for (ConsiderationSetAlternative ca : s.getConsiderationSet()) {
				if(ca.getAlternative().getNoPurchaseAlternative()){
					noPurchaseOffer = new AlternativeOffer();
					noPurchaseOffer.setAlternative(ca.getAlternative());
					noPurchaseOffer.setAlternativeId(ca.getAlternativeId());
				}
			}
			
			// Per time window that is in consideration set
			for (ConsiderationSetAlternative ca : s.getConsiderationSet()) {
				// Upper Bound
				AlternativeOffer offer = new AlternativeOffer();
				offer.setAlternative(ca.getAlternative());
				offer.setAlternativeId(ca.getAlternativeId());
				ArrayList<AlternativeOffer> offeredAlternatives = new ArrayList<AlternativeOffer>();
				offeredAlternatives.add(offer);
				if(noPurchaseOffer!=null){
					offeredAlternatives.add(noPurchaseOffer);
				}
				HashMap<AlternativeOffer, Double> probabilities = CustomerDemandService.getProbabilitiesForModel(
						this.demandSegmentSet.getDemandModelType().getName(), offeredAlternatives, s);
				// TODO: Only works with 1:1 relationships between alternatives
				// and time windows
				if (!ca.getAlternative().getNoPurchaseAlternative()) {
					upperPerTimeWindow.put(ca.getAlternative().getTimeWindows().get(0), probabilities.get(offer));
				} else {
					upperPerTimeWindow.put(null, probabilities.get(offer));
				}

				// Lower Bound
				offeredAlternatives = new ArrayList<AlternativeOffer>();
				AlternativeOffer offerRelevant = new AlternativeOffer();
				boolean containsNoPurchaseOffer = false;
				for (ConsiderationSetAlternative ca2 : s.getConsiderationSet()) {
					AlternativeOffer offera = new AlternativeOffer();
					offera.setAlternative(ca2.getAlternative());
					offera.setAlternativeId(ca2.getAlternativeId());
					if (ca2.getAlternative().getNoPurchaseAlternative())
						containsNoPurchaseOffer = true;
					if (ca2.getAlternativeId()==ca.getAlternativeId())
						offerRelevant = offera;
					offeredAlternatives.add(offera);
				}

				probabilities = CustomerDemandService.getProbabilitiesForModel(
						this.demandSegmentSet.getDemandModelType().getName(), offeredAlternatives, s);
				// TODO: Only works with 1:1 relationships between alternatives
				// and time windows
				if (!ca.getAlternative().getNoPurchaseAlternative()) {
					lowerPerTimeWindow.put(ca.getAlternative().getTimeWindows().get(0),
							probabilities.get(offerRelevant));
				} else {
					lowerPerTimeWindow.put(null, probabilities.get(offer));
				}

				// If no purchase alternative is not in consideration set
				if (!containsNoPurchaseOffer)
					lowerPerTimeWindow.put(null, probabilities.get(null));

			}
			this.lowerBoundDemandMultiplierPerSegment.put(s, lowerPerTimeWindow);
			this.upperBoundDemandMultiplierPerSegment.put(s, upperPerTimeWindow);

		}
	}

	public static String[] getParameterSetting() {

		return paras;
	}

	public OrderSet getResult() {

		return result;
	}

}
