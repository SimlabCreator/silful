package logic.algorithm.rm.optimization.learning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.math3.util.Pair;

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
import data.entity.Region;
import data.entity.Routing;
import data.entity.TimeWindow;
import data.entity.TimeWindowSet;
import logic.algorithm.vr.capacity.CapacityAggregation;
import logic.algorithm.vr.capacity.CapacityAlgorithm;
import logic.entity.AssortmentAlgorithm;
import logic.service.support.CustomerDemandService;
import logic.service.support.LinearProgrammingService;
import logic.utility.comparator.PairDoubleValueAscComparator;
import logic.utility.comparator.PairIntegerValueAscComparator;

/**
 * ParallelFlightsPreparation
 * 
 * @author M. Lang
 *
 */
public class ParallelFlightsPreparation implements CapacityAlgorithm {

	private static double BETA_RANGE = 0.1;
	private static boolean possiblyLargeOfferSet=true;
	private static double RISK_RANGE =0.1;
	private static  AssortmentAlgorithm algo =  AssortmentAlgorithm.REVENUE_ORDERED_SET;//TODO: As input parameter!

	private static String[] paras = new String[] { "No_routing_candidates", "Beta_lower_bound",
			"Beta_upper_bound", "Beta_stepsize", "samplePreferences",
			"averageCapacitiesAsOption", "actualBasketValue"};
	
	private CapacitySet averageCapacitySet;
	private Region region;
	private ArrayList<OrderRequestSet> orderRequestSetsForLearning;
	private TimeWindowSet timeWindowSet;
	private double maximumRevenueValue;
	private HashMap<Entity, Object> objectiveSpecificValues;
	private ArrayList<Routing> routingsForLearning;
	private ArrayList<CapacitySet> potentialCapacities;
	private CapacitySet result;
	private HashMap<CapacitySet, Pair<Double, Double>> betaAndValueForCapacitySet;
	private HashMap<DemandSegment, HashMap<TimeWindow, Double>> lowerBoundDemandMultiplierPerSegment;
	private HashMap<DemandSegment, HashMap<TimeWindow, Double>> upperBoundDemandMultiplierPerSegment;
	private double betaLowerBound;
	private double betaUpperBound;
	private double betaStepSize;
	private int numberOfPotentialRoutings;
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
	private boolean averageCapacitiesAsOption;
	private int periodLength;
	private DeliveryAreaSet deliveryAreaSet;
	private boolean actualValue;

	public ParallelFlightsPreparation(Region region, DeliveryAreaSet deliveryAreaSet,
			DemandSegmentSet demandSegmentSet, ArrayList<OrderRequestSet> orderRequestSetsForLearning,
			CapacitySet capacitySet,  HashMap<Entity, Object> objectiveSpecificValues,
			Double maximumRevenueValue, ArrayList<Routing> routingsForLearning, double betaLowerBound,
			double betaUpperBound, double betaStepSize, double numberOfPotentialRoutings, int arrivalProcessId,
			HashMap<DeliveryArea, Double> daWeights, HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightings, Double samplePreferences, Double averageCapacitiesAsOption,
			int periodLength, Double actualValue) {
		this.region = region;
		this.orderRequestSetsForLearning = orderRequestSetsForLearning;
		this.averageCapacitySet = capacitySet;
		this.timeWindowSet = this.orderRequestSetsForLearning.get(0).getCustomerSet().getOriginalDemandSegmentSet()
				.getAlternativeSet().getTimeWindowSet();
		this.maximumRevenueValue = maximumRevenueValue;
		this.objectiveSpecificValues = objectiveSpecificValues;
		this.routingsForLearning = routingsForLearning;
		this.betaLowerBound = betaLowerBound;
		this.betaUpperBound = betaUpperBound;
		this.betaStepSize = betaStepSize;
		this.numberOfPotentialRoutings = (int) numberOfPotentialRoutings;
		this.daSegmentWeightings = daSegmentWeightings;
		this.daWeights = daWeights;
		this.arrivalProcessId = arrivalProcessId;
		this.demandSegmentSet = demandSegmentSet;
		this.deliveryAreaSet=deliveryAreaSet;
		
		if (samplePreferences == 1.0) {
			this.usepreferencesSampled = true;
		} else {
			this.usepreferencesSampled = false;
		}
	
		if (averageCapacitiesAsOption == 1.0) {
			this.averageCapacitiesAsOption = true;
		} else {
			this.averageCapacitiesAsOption = false;
		}
		
		if(actualValue==1.0){
			this.actualValue=true;
		}else{
			this.actualValue=false;
		}
		
		this.periodLength = periodLength;
	};

	public void start() {

		this.betaAndValueForCapacitySet = new HashMap<CapacitySet, Pair<Double, Double>>();
		// Find routings that are closest to capacities and define respective
		// capacities
		this.findPotentialCapacities();

		// Initialise lower and upper bound demand multipliers
		this.initialiseDemandMultipliers();

		// Find best beta per routing
		for (CapacitySet capacities : potentialCapacities)
			this.findBestBetaForCapacities(capacities);

		// Select capacity set with best results and respective beta
		CapacitySet bestSet = null;
		double bestBeta = 0.0;
		double bestValue = 0.0;
		for (CapacitySet set : this.betaAndValueForCapacitySet.keySet()) {
			if (this.betaAndValueForCapacitySet.get(set).getValue() > bestValue) {
				bestSet = set;
				bestBeta = this.betaAndValueForCapacitySet.get(set).getKey();
				bestValue = this.betaAndValueForCapacitySet.get(set).getValue();
			}
		}
		bestSet.setWeight(bestBeta);
		this.result = bestSet;

	}

	private void findPotentialCapacities() {

		// Calculate difference values between average capacities and capacities
		// for the routings
		HashMap<CapacitySet, Integer> differenceValuesOverall = new HashMap<CapacitySet, Integer>();
		HashMap<CapacitySet, Integer> differenceValuesMax = new HashMap<CapacitySet, Integer>();

		for (Routing r : this.routingsForLearning) {

			// Identify capacities
			CapacityAggregation algo = new CapacityAggregation(r, this.averageCapacitySet.getDeliveryAreaSet());
			algo.start();
			CapacitySet capacitySet = algo.getResult();
			capacitySet.setRoutingId(r.getId());
			// Calculate overall and max difference
			int overallDifference = 0;
			int maxDifference = 0;
			
			HashMap<Integer,Integer> capPerDa = new HashMap<Integer,Integer> ();
			for(DeliveryArea area: capacitySet.getDeliveryAreaSet().getElements()){
				if(area.getSubsetId()!=null){
					for(DeliveryArea sArea: area.getSubset().getElements()){
						capPerDa.put(sArea.getId(), 0);
					}
				}else{
					capPerDa.put(area.getId(), 0);
				}
				
				
			}
			for (Capacity capA : this.averageCapacitySet.getElements()) {

				for (Capacity cap : capacitySet.getElements()) {
					if (cap.getDeliveryAreaId().equals(capA.getDeliveryAreaId())
							&& cap.getTimeWindowId().equals(capA.getTimeWindowId())) {
						int currentDifference = Math.abs(capA.getCapacityNumber() - cap.getCapacityNumber());
						if(currentDifference>4)
							System.out.println("Check");
						capPerDa.put(cap.getDeliveryAreaId(), capPerDa.get(cap.getDeliveryAreaId())+cap.getCapacityNumber());
						overallDifference += currentDifference;
						if (currentDifference > maxDifference) {
							maxDifference = currentDifference;

						}

					}
				}
			}

			for(Integer areaId: capPerDa.keySet()){
				if(capPerDa.get(areaId)>13){
					System.out.println("Strange");
				}
			}
			differenceValuesOverall.put(capacitySet, overallDifference);
			differenceValuesMax.put(capacitySet, maxDifference);
		}

		// Find maximum values for normalisation (normalisation not based on
		// variance because metric with larger variance should dominate)
		@SuppressWarnings("rawtypes")
		List differenceValuesOverallList = new ArrayList(differenceValuesOverall.values());
		Collections.sort(differenceValuesOverallList);
		@SuppressWarnings("rawtypes")
		List differenceValuesMaxList = new ArrayList(differenceValuesMax.values());
		Collections.sort(differenceValuesMaxList);

		int maxDifference = (Integer) differenceValuesMaxList.get(differenceValuesMaxList.size() - 1);
		int maxOverallDifference = (Integer) differenceValuesOverallList.get(differenceValuesOverallList.size() - 1);

		ArrayList<Pair<CapacitySet, Double>> values = new ArrayList<Pair<CapacitySet, Double>>();
		ArrayList<Pair<CapacitySet, Integer>> maxValues = new ArrayList<Pair<CapacitySet, Integer>>();
		ArrayList<Pair<CapacitySet, Integer>> avgValues = new ArrayList<Pair<CapacitySet, Integer>>();

		for (CapacitySet cs : differenceValuesOverall.keySet()) {
			values.add(new Pair<CapacitySet, Double>(cs, ((double)differenceValuesOverall.get(cs) / (double) maxOverallDifference)
					+  ((double)differenceValuesMax.get(cs) /  (double)maxDifference)));
			maxValues.add(new Pair<CapacitySet, Integer>(cs, differenceValuesMax.get(cs) ));
			avgValues.add(new Pair<CapacitySet, Integer>(cs, differenceValuesOverall.get(cs)));
		}

		Collections.sort(values, new PairDoubleValueAscComparator());
	//	Collections.sort(maxValues, new PairIntegerValueAscComparator());
	//	Collections.sort(avgValues, new PairIntegerValueAscComparator());
		

		int numberRou = 0;
		this.potentialCapacities = new ArrayList<CapacitySet>();
		while (numberRou < this.numberOfPotentialRoutings && values.size() > numberRou) {
			this.potentialCapacities.add(values.get(numberRou).getKey());
			numberRou++;
		}

		if (this.averageCapacitiesAsOption) {
			this.potentialCapacities.add(this.averageCapacitySet);
		}

	}

	private void initialiseDemandMultipliers() {

		// Initialise hashmaps
		lowerBoundDemandMultiplierPerSegment = new HashMap<DemandSegment, HashMap<TimeWindow, Double>>();
		upperBoundDemandMultiplierPerSegment = new HashMap<DemandSegment, HashMap<TimeWindow, Double>>();

		// Get upper and lower bound probabilities per segment
		for (DemandSegment s : this.demandSegmentSet.getElements()) {
			HashMap<TimeWindow, Double> lowerPerTimeWindow = new HashMap<TimeWindow, Double>();
			HashMap<TimeWindow, Double> upperPerTimeWindow = new HashMap<TimeWindow, Double>();

			// Per time window that is in consideration set
			for (ConsiderationSetAlternative ca : s.getConsiderationSet()) {
				// Upper Bound
				AlternativeOffer offer = new AlternativeOffer();
				offer.setAlternative(ca.getAlternative());
				offer.setAlternativeId(ca.getAlternativeId());
				ArrayList<AlternativeOffer> offeredAlternatives = new ArrayList<AlternativeOffer>();
				offeredAlternatives.add(offer);
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


	private void findBestBetaForCapacities(CapacitySet capacitySet) {

		// Go through different betas
		double bestBeta = 0.0;
		double bestValue = 0.0;
		for (double beta = this.betaLowerBound; beta <= this.betaUpperBound; beta += this.betaStepSize) {

			// Simulate order horizon
			double expectedOverallValue = this.calculateExpectedOverallValueBySimulation(beta, capacitySet);

			// Update best beta
			if (bestValue < expectedOverallValue) {
				bestBeta = beta;
				bestValue = expectedOverallValue;
			}
		}

		Pair<Double, Double> betaAndValue = new Pair<Double, Double>(bestBeta, bestValue);
		this.betaAndValueForCapacitySet.put(capacitySet, betaAndValue);

	}

	private double calculateExpectedOverallValueBySimulation(double beta, CapacitySet capacitySet) {

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

		double expectedOverallValue = 0.0;

		// Go through order request sets and obtain overall values
		for (OrderRequestSet set : this.orderRequestSetsForLearning) {
			
			ArrayList<Order> acceptedOrders = LinearProgrammingService
					.simulateOrderHorizonForDeterministicLinearProgrammingWithStaticCapacityAssignment(
							beta, capacitiesPerDeliveryArea, set, this.deliveryAreaSet, region,
							timeWindowSet, daSegmentWeightings, daWeights,
							arrivalProcessId, lowerBoundDemandMultiplierPerSegment,
							upperBoundDemandMultiplierPerSegment, maximumRevenueValue, objectiveSpecificValues,
							 usepreferencesSampled, periodLength, BETA_RANGE, algo, possiblyLargeOfferSet, actualValue, RISK_RANGE);

			double overallValue = 0.0;

			for (Order o : acceptedOrders) {

				if (o.getAccepted()) {
					
					overallValue += CustomerDemandService.calculateOrderValue(o.getOrderRequest(), maximumRevenueValue,
							objectiveSpecificValues);
			
				}
			}

			expectedOverallValue += overallValue;
		}

		return expectedOverallValue / this.orderRequestSetsForLearning.size();

	}

	public static String[] getParameterSetting() {

		return paras;
	}

	public CapacitySet getResult() {

		return result;
	}

}
