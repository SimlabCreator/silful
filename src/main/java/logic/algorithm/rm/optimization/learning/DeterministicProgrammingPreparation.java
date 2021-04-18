package logic.algorithm.rm.optimization.learning;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.math3.util.Pair;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import data.entity.Alternative;
import data.entity.AlternativeSet;
import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.DemandSegment;
import data.entity.DemandSegmentSet;
import data.entity.DemandSegmentWeight;
import data.entity.DemandSegmentWeighting;
import data.entity.DistributionParameterValue;
import data.entity.Entity;
import data.entity.GeneralAtomicOutputValue;
import data.entity.Order;
import data.entity.OrderRequestSet;
import data.entity.Region;
import data.entity.Routing;
import data.entity.TimeWindow;
import data.entity.TimeWindowSet;
import logic.algorithm.AtomicOutputAlgorithm;
import logic.service.support.ArrivalProcessService;
import logic.service.support.CustomerDemandService;
import logic.service.support.LocationService;
import logic.service.support.ProbabilityDistributionService;
import logic.utility.exceptions.ParameterUnknownException;

/**
 * 
 * @author M. Lang
 *
 */
public class DeterministicProgrammingPreparation implements AtomicOutputAlgorithm {

	private static String[] paras = new String[] { "Beta_lower_bound", "Beta_upper_bound", "Beta_stepsize",
			"samplePreferences", "theft-based", "theft-based-advanced", "duplicate_segments" };

	private Region region;
	private ArrayList<OrderRequestSet> orderRequestSetsForLearning;
	private TimeWindowSet timeWindowSet;
	private AlternativeSet alternativeSet;
	private double maximumRevenueValue;
	private HashMap<Entity, Object> objectiveSpecificValues;
	private ArrayList<Routing> routingsForLearning;
	private double betaFinal;
	private double betaLowerBound;
	private double betaUpperBound;
	private double betaStepSize;
	private HashMap<Integer, Alternative> alternativesToTimeWindows;
	private HashMap<TimeWindow, Alternative> alternativesToTimeWindows2;
	private RConnection connection;
	private double expectedArrivalsOverall;
	private Alternative noPurchaseAlternative;
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
	private HashMap<Integer,Pair<Double, Pair<DemandSegment, DemandSegment>>> mapOriginalSegmentToSubSegment;
	private DemandSegmentWeighting demandSegmentWeighting;
	private DemandSegmentWeighting demandSegmentWeightingOriginal;
	private int periodLength;
	private DeliveryAreaSet deliveryAreaSet;
	private boolean theftBased;
	private boolean theftBasedAdvanced;
	private HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors;
	private HashMap<Integer, HashMap<Integer, Double>> maximumExpectedMultiplierPerDemandSegmentAndTimeWindow;
	private HashMap<Integer, HashMap<Integer, Double>> minimumExpectedMultiplierPerDemandSegmentAndTimeWindow;
	private HashMap<DemandSegment, HashMap<Alternative, Double>> demandMultiplierPerSegment;
	private GeneralAtomicOutputValue result;
	private boolean duplicateSegments;

	public DeterministicProgrammingPreparation(Region region, int bookingHorizonLength, DeliveryAreaSet deliveryAreaSet,
			DemandSegmentWeighting dsw, ArrayList<OrderRequestSet> orderRequestSetsForLearning,
			HashMap<Entity, Object> objectiveSpecificValues, Double maximumRevenueValue,
			ArrayList<Routing> routingsForLearning, double betaLowerBound, double betaUpperBound, double betaStepSize,
			int arrivalProcessId, Double samplePreferences, Double theftBased, Double theftBasedAdvanced,
			HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors, Double duplicateSegments) {

		this.region = region;
		this.neighbors = neighbors;
		this.orderRequestSetsForLearning = orderRequestSetsForLearning;
		this.timeWindowSet = this.orderRequestSetsForLearning.get(0).getCustomerSet().getOriginalDemandSegmentSet()
				.getAlternativeSet().getTimeWindowSet();
		this.alternativeSet = orderRequestSetsForLearning.get(0).getCustomerSet().getOriginalDemandSegmentSet()
				.getAlternativeSet();
		this.maximumRevenueValue = maximumRevenueValue;
		this.objectiveSpecificValues = objectiveSpecificValues;
		this.routingsForLearning = routingsForLearning;
		this.betaLowerBound = betaLowerBound;
		this.betaUpperBound = betaUpperBound;
		this.betaStepSize = betaStepSize;
		this.duplicateSegments = (duplicateSegments == 1.0);
		this.arrivalProcessId = arrivalProcessId;
		this.demandSegmentWeightingOriginal = dsw;
		this.deliveryAreaSet = deliveryAreaSet;

		this.usepreferencesSampled = (samplePreferences == 1.0);

		this.theftBased = (theftBased == 1.0);

		this.theftBasedAdvanced = (theftBasedAdvanced == 1.0);

		this.periodLength = bookingHorizonLength;
	};

	public void start() {

		this.expectedArrivalsOverall = ArrivalProcessService.getMeanArrivalProbability(arrivalProcessId)
				* this.periodLength;
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
				w1.setWeight(w.getWeight()/2.0);
				w1.setId(initialId--);
				w1.setElementId(ds1.getId());
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
				w2.setSetId(-1);
				w2.setElementId(ds2.getId());
				newWeights.add(w2);
				mapOriginalSegmentToSubSegment.put(ds.getId(), new Pair<Double, Pair<DemandSegment, DemandSegment>>(splitValue, new Pair<DemandSegment, DemandSegment>(ds2, ds1)));
				
			}
			dss.setElements(newSegments);
			this.demandSegmentWeighting.setWeights(newWeights);
			this.demandSegmentWeighting.setSetEntity(dss);
		}else{
			this.demandSegmentWeighting=this.demandSegmentWeightingOriginal;
		}
		
		this.daWeights = new HashMap<DeliveryArea, Double>();
		this.daSegmentWeightings = new HashMap<DeliveryArea, DemandSegmentWeighting>();
		LocationService.determineDeliveryAreaWeightAndDemandSegmentWeightingPerDeliveryAreaConsideringHierarchy(daWeights,
				daSegmentWeightings, deliveryAreaSet, demandSegmentWeighting);
		
		this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow = CustomerDemandService
				.determineMaximumProbabilityPerDemandSegmentAndTimeWindow(this.demandSegmentWeighting, timeWindowSet);
		this.minimumExpectedMultiplierPerDemandSegmentAndTimeWindow = CustomerDemandService
				.determineMinimumProbabilityPerDemandSegmentAndTimeWindow(this.demandSegmentWeighting, timeWindowSet);

		// Find best beta
		Double bestBeta = null;
		

		double bestValue = -1 * Double.MAX_VALUE;
		for (double beta = this.betaLowerBound; beta <= this.betaUpperBound; beta += this.betaStepSize) {
			double currentValue = this.determineBetaValueBySimulation(beta);

			if (currentValue > bestValue) {
				bestBeta = beta;
				bestValue = currentValue;
			}
		}

		result = new GeneralAtomicOutputValue();
		result.setParameterTypeId(80);
		result.setValue(bestBeta);

	}

	private double evaluate(ArrayList<Order> orders) {

		double value = 0;
		int accOrders = 0;
		for (Order o : orders) {
			if (o.getAccepted()) {
				value += CustomerDemandService.calculateExpectedValue(maximumRevenueValue, objectiveSpecificValues,
						o.getOrderRequest().getCustomer().getOriginalDemandSegment());
				accOrders++;
			}
		}
		System.out.println("Number of accepted orders: " + accOrders);
		return value;
	}

	private void initialiseDemandMultipliers(double currentBeta) {

		this.demandMultiplierPerSegment = new HashMap<DemandSegment, HashMap<Alternative, Double>>();
		DemandSegmentSet dss = (DemandSegmentSet) this.demandSegmentWeighting.getSetEntity();
		for (Integer dsId : this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow.keySet()) {
			DemandSegment ds = dss.getDemandSegmentById(dsId);
			this.demandMultiplierPerSegment.put(ds, new HashMap<Alternative, Double>());
			double minimumNoPurchaseProb = 1.0;
			for (Integer twId : this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow.get(dsId).keySet()) {
				this.demandMultiplierPerSegment.get(ds).put(this.alternativesToTimeWindows.get(twId),
						this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow.get(dsId).get(twId) * currentBeta
								+ this.minimumExpectedMultiplierPerDemandSegmentAndTimeWindow.get(dsId).get(twId)
										* (1.0 - currentBeta));
				minimumNoPurchaseProb = minimumNoPurchaseProb
						- this.minimumExpectedMultiplierPerDemandSegmentAndTimeWindow.get(dsId).get(twId);
			}
			this.demandMultiplierPerSegment.get(ds).put(this.noPurchaseAlternative, minimumNoPurchaseProb);
		}
	}

	private double determineBetaValueBySimulation(double beta) {

		// Initialise lower and upper bound demand multipliers
		this.initialiseDemandMultipliers(beta);

		// Initialise reference information
		HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>> referenceInformation = DeterministicProgrammingBasedAcceptance
				.aggregateReferenceInformation(deliveryAreaSet, this.routingsForLearning);

		// Build capacity per segment/time window combination
		Pair<HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, HashMap<DemandSegment, HashMap<Integer, Double>>>>>>, HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>>> r = DeterministicProgrammingBasedAcceptance
				.determineCapacityAssignments(referenceInformation, this.daWeights, this.daSegmentWeightings,
						this.demandMultiplierPerSegment, this.expectedArrivalsOverall, this.maximumRevenueValue,
						objectiveSpecificValues, timeWindowSet, connection, noPurchaseAlternative,
						alternativesToTimeWindows2);
		Pair<HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, HashMap<DemandSegment, HashMap<Integer, Double>>>>>>, HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>>> rCopy = this
				.copyAssignedCapacities(r.getKey(), r.getValue());

		DeterministicProgrammingBasedAcceptance accAlgo = new DeterministicProgrammingBasedAcceptance(region,
				this.periodLength, this.deliveryAreaSet, this.usepreferencesSampled, this.theftBased,
				this.theftBasedAdvanced, this.neighbors, this.demandSegmentWeighting, this.daWeights,
				this.daSegmentWeightings, this.mapOriginalSegmentToSubSegment, this.arrivalProcessId, beta, this.maximumRevenueValue,
				this.objectiveSpecificValues, this.alternativeSet);
		double avgValue = 0;
		for (OrderRequestSet rs : this.orderRequestSetsForLearning) {
			accAlgo.setOrderRequestSet(rs);
			accAlgo.setAssignedCapacitiesPerRouting(r.getKey());
			accAlgo.setBufferPerRouting(r.getValue());
			accAlgo.start();
			ArrayList<Order> orders = accAlgo.getResult().getElements();

			double value = this.evaluate(orders);
			avgValue += value;

			r = this.copyAssignedCapacities(rCopy.getKey(), rCopy.getValue());
		}
		avgValue = avgValue / this.orderRequestSetsForLearning.size();
		System.out.println("Beta: " + beta + "; Value: " + avgValue);
		return avgValue;

	}

	private Pair<HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, HashMap<DemandSegment, HashMap<Integer, Double>>>>>>, HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>>> copyAssignedCapacities(
			HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, HashMap<DemandSegment, HashMap<Integer, Double>>>>>> assignedCapacities,
			HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>> buffer) {
		HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, HashMap<DemandSegment, HashMap<Integer, Double>>>>>> copy = new HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, HashMap<DemandSegment, HashMap<Integer, Double>>>>>>();
		HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>> copyBuffer = new HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>>();

		double overallCap = 0;
		for (DeliveryArea area : assignedCapacities.keySet()) {
			copy.put(area,
					new HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, HashMap<DemandSegment, HashMap<Integer, Double>>>>>());
			copyBuffer.put(area, new HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>());
			for (Routing r : assignedCapacities.get(area).keySet()) {
				copy.get(area).put(r,
						new HashMap<DeliveryArea, HashMap<TimeWindow, HashMap<DemandSegment, HashMap<Integer, Double>>>>());
				copyBuffer.get(area).put(r, new HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>());
				for (DeliveryArea subArea : assignedCapacities.get(area).get(r).keySet()) {
					copy.get(area).get(r).put(subArea,
							new HashMap<TimeWindow, HashMap<DemandSegment, HashMap<Integer, Double>>>());
					copyBuffer.get(area).get(r).put(subArea, new HashMap<TimeWindow, Integer>());
					for (TimeWindow tw : assignedCapacities.get(area).get(r).get(subArea).keySet()) {
						copy.get(area).get(r).get(subArea).put(tw,
								new HashMap<DemandSegment, HashMap<Integer, Double>>());
						if (buffer.containsKey(area) && buffer.get(area).containsKey(r)
								&& buffer.get(area).get(r).containsKey(subArea)
								&& buffer.get(area).get(r).get(subArea).containsKey(tw)) {
							copyBuffer.get(area).get(r).get(subArea).put(tw,
									buffer.get(area).get(r).get(subArea).get(tw));
							overallCap += buffer.get(area).get(r).get(subArea).get(tw);
						}
						for (DemandSegment ds : assignedCapacities.get(area).get(r).get(subArea).get(tw).keySet()) {
							copy.get(area).get(r).get(subArea).get(tw).put(ds, new HashMap<Integer, Double>());
							copy.get(area).get(r).get(subArea).get(tw).get(ds).put(0,
									assignedCapacities.get(area).get(r).get(subArea).get(tw).get(ds).get(0));
							overallCap += assignedCapacities.get(area).get(r).get(subArea).get(tw).get(ds).get(0);
						}
					}
				}
			}
		}

		return new Pair<HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, HashMap<DemandSegment, HashMap<Integer, Double>>>>>>, HashMap<DeliveryArea, HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>>>>(
				copy, copyBuffer);
	}

	public static String[] getParameterSetting() {

		return paras;
	}

	public GeneralAtomicOutputValue getResult() {

		return result;
	}

}
