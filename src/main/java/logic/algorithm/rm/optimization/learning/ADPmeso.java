package logic.algorithm.rm.optimization.learning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math3.util.Pair;
import org.rosuda.JRI.Rengine;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import data.entity.Alternative;
import data.entity.AlternativeOffer;
import data.entity.AlternativeSet;
import data.entity.ConsiderationSetAlternative;
import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.DemandSegmentWeighting;
import data.entity.Entity;
import data.entity.Order;
import data.entity.OrderRequest;
import data.entity.OrderRequestSet;
import data.entity.Region;
import data.entity.Route;
import data.entity.RouteElement;
import data.entity.Routing;
import data.entity.TimeWindow;
import data.entity.ValueFunctionApproximationCoefficient;
import data.entity.ValueFunctionApproximationModel;
import data.entity.ValueFunctionApproximationModelSet;
import data.entity.VehicleAreaAssignment;
import data.entity.VehicleAreaAssignmentSet;
import logic.algorithm.rm.optimization.control.AggregateReferenceInformationAlgorithm;
import logic.algorithm.rm.optimization.control.ValueFunctionApproximationAlgorithm;
import logic.entity.AssortmentAlgorithm;
import logic.entity.MomentumHelper;
import logic.entity.NonParametricValueFunctionAddon;
import logic.entity.ValueFunctionCoefficientType;
import logic.service.support.AssortmentProblemService;
import logic.service.support.CustomerDemandService;
import logic.service.support.DynamicRoutingHelperService;
import logic.service.support.LearningService;
import logic.service.support.LocationService;
import logic.service.support.RoutingService;
import logic.utility.SettingsProvider;
import logic.utility.comparator.OrderArrivalTimeAscComparator;
import logic.utility.comparator.RouteElementArrivalTimeDescComparator;
import smile.regression.OLS;

/*
 * Combination of approximate dynamic programming models
 */
public class ADPmeso extends AggregateReferenceInformationAlgorithm implements ValueFunctionApproximationAlgorithm {

	private ValueFunctionApproximationModelSet result;
	private static boolean learningTypeBatch = true;
	private static boolean tdUpdate = false;
	private static int numberOfThreads = 3;
	private static double E_GREEDY_VALUE = 0.7;
	private static boolean possiblyLargeOfferSet = true;
	private static AssortmentAlgorithm algo = AssortmentAlgorithm.REVENUE_ORDERED_SET;
	private static int numberOfReleventObservationsForLearning = 1000;

	private VehicleAreaAssignmentSet vehicleAreaAssignmentSet;
	private AlternativeSet alternativeSet;
	private ArrayList<OrderRequestSet> orderRequestSetsForLearning;
	private HashMap<Integer, HashMap<Integer, HashMap<Integer, OrderRequest>>> orderRequestsPerDeliveryArea;
	private HashMap<Integer, ArrayList<VehicleAreaAssignment>> vehicleAssignmentsPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, VehicleAreaAssignment>> vasPerDeliveryAreaSetAndVehicleNo;
	private HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors;

	private DemandSegmentWeighting demandSegmentWeighting;

	private HashMap<Integer, Double> overallCapacityPerDeliveryArea;

	private HashMap<Integer, Double> remainingCapacityPerDeliveryArea;
	private HashMap<Integer, Double> acceptedCostPerDeliveryArea;
	private HashMap<Integer, Double> acceptedAmountPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerDeliveryArea;
	private HashMap<Integer, HashMap<Integer, Double>> distancePerDeliveryAreaAndRouting;
	private HashMap<Integer, HashMap<Integer, HashMap<Integer, Double>>> distancePerDeliveryAreaAndTwAndRouting;
	private HashMap<DeliveryArea, ArrayList<Pair<ArrayList<double[]>, Double[]>>> observationsPerDeliveryArea;
	private HashMap<DeliveryArea, Double[][]> lookupTableSumPerDeliveryArea;
	private HashMap<DeliveryArea, Integer[][]> lookupTableCountPerDeliveryArea;
	private HashMap<DeliveryArea, Double[]> lookupArraySumPerDeliveryArea;// If
																			// only
																			// time
	private HashMap<DeliveryArea, Integer[]> lookupArrayCountPerDeliveryArea;// If
																				// only
																				// time
	private HashMap<DeliveryArea, double[]> linearFunctionCoefficientsPerDeliveryArea;
	private HashMap<DeliveryArea, Double> linearFunctionInterceptPerDeliveryArea;
	private HashMap<DeliveryArea, smile.regression.RandomForest> randomForestPerDeliveryArea;

	private int includeDriveFromStartingPosition;
	private int orderHorizonLength;
	private boolean usepreferencesSampled;
	private boolean useActualBasketValue;
	private boolean considerInsertionCosts;
	private boolean mixedEcAndIc;
	private int indexInsertionCosts;
	private boolean considerOverallRemainingCapacity;
	private int indexRemainingBudget;
	private boolean considerOverallAcceptedTimeInteraction;
	private int indexOverallAcceptedTimeInteraction;
	private int indexOverallAccepted;
	private double mesoWeightLf;
	private int explorationStrategy;
	private HashMap<TimeWindow, Alternative> alternativesToTimeWindows;
	private HashMap<Integer, Integer> timeWindowInputMapper;

	private int numberOfGRASPSolutions;
	private int numberPotentialInsertionCandidates;

	private int distanceType;
	private boolean distanceMeasurePerTw;
	private int maximumDistanceMeasureIncrease;
	private int switchDistanceOffPoint;
	private double[] maxValuePerAttribute;
	private int numberOfAttributes;
	private HashMap<Integer, HashMap<Integer, Double>> maximumExpectedMultiplierPerDemandSegmentAndTimeWindow;
	private double learningRate;
	private double annealingTemperature;
	private double momentumWeight;
	private HashMap<Integer, MomentumHelper> oldMomentumPerDeliveryArea;
	private double noRepetitionsSample;

	private static String[] paras = new String[] { "Constant_service_time", "actualBasketValue", "samplePreferences",
			"includeDriveFromStartingPosition", "consider_overall_remaining_capacity",
			"consider_overall_accepted_insertion_costs", "time_cap_interaction",
			"exploration_(0:on-policy,1:wheel,2:e-greedy)", "no_routing_candidates", "no_insertion_candidates",
			"distance_type", "distance_measure_per_tw", "maximum_distance_measure_increase",
			"switch_distance_off_point", "meso_weight_lf", "stepsize_adp_learning",
			"annealing_temperature_(Negative:no_annealing)", "momentum_weight", "no_repetitions_sample" };

	public ADPmeso(Region region, VehicleAreaAssignmentSet vehicleAreaAssignmentSet,
			ArrayList<OrderRequestSet> orderRequestSetsForLearning, ArrayList<Routing> previousRoutingResults,
			DeliveryAreaSet deliveryAreaSet, DemandSegmentWeighting demandSegmentWeighting, Double expectedServiceTime,
			HashMap<Entity, Object> objectiveSpecificValues, Double maximumRevenueValue,
			Double includeDriveFromStartingPosition, int orderHorizonLength, Double samplePreferences,
			Double actualBasketValue, Double considerOverallRemainingCapacity, Double considerOverallAcceptedCost,
			Double considerOverallAcceptedTimeInteraction, Double explorationStrategy,
			Double numberPotentialInsertionCandidates, Double numberRoutingCandidates,
			HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors, Double distanceType, Double distanceMeasurePerTw,
			Double maximumDistanceMeasureIncrease, Double switchDistanceOffPoint, Double mesoWeight,
			Double learningRate, Double annealingTemperature, Double momentumWeight, Double noRepetitionsSample) {
		this.maximumDistanceMeasureIncrease = maximumDistanceMeasureIncrease.intValue();
		this.distanceMeasurePerTw = (distanceMeasurePerTw == 1.0);
		this.switchDistanceOffPoint = switchDistanceOffPoint.intValue();
		AggregateReferenceInformationAlgorithm.setRegion(region);
		this.vehicleAreaAssignmentSet = vehicleAreaAssignmentSet;
		this.orderRequestSetsForLearning = orderRequestSetsForLearning;
		AggregateReferenceInformationAlgorithm.setDeliveryAreaSet(deliveryAreaSet);

		AggregateReferenceInformationAlgorithm.setTimeWindowSet(this.orderRequestSetsForLearning.get(0).getCustomerSet()
				.getOriginalDemandSegmentSet().getAlternativeSet().getTimeWindowSet());

		this.alternativeSet = this.orderRequestSetsForLearning.get(0).getCustomerSet().getOriginalDemandSegmentSet()
				.getAlternativeSet();
		this.mesoWeightLf = mesoWeight;
		AggregateReferenceInformationAlgorithm.setExpectedServiceTime(expectedServiceTime);
		AggregateReferenceInformationAlgorithm.setMaximumRevenueValue(maximumRevenueValue);
		AggregateReferenceInformationAlgorithm.setObjectiveSpecificValues(objectiveSpecificValues);
		this.includeDriveFromStartingPosition = includeDriveFromStartingPosition.intValue();
		this.orderHorizonLength = orderHorizonLength;
		this.explorationStrategy = explorationStrategy.intValue();
		this.numberPotentialInsertionCandidates = numberPotentialInsertionCandidates.intValue();
		this.numberOfGRASPSolutions = numberRoutingCandidates.intValue();
		AggregateReferenceInformationAlgorithm.setPreviousRoutingResults(previousRoutingResults);
		this.neighbors = neighbors;
		this.distanceType = distanceType.intValue();
		this.usepreferencesSampled = (samplePreferences == 1.0);
		this.useActualBasketValue = (actualBasketValue == 1.0);
		this.considerOverallRemainingCapacity = (considerOverallRemainingCapacity == 1.0);
		if (considerOverallAcceptedCost > 0) {
			this.considerInsertionCosts = true;
			if (considerOverallAcceptedCost > 1) {
				this.mixedEcAndIc = true;
			}
		} else {
			this.considerInsertionCosts = false;
		}

		this.considerOverallAcceptedTimeInteraction = (considerOverallAcceptedTimeInteraction == 1.0);
		this.demandSegmentWeighting = demandSegmentWeighting;
		this.learningRate = learningRate;
		this.annealingTemperature = annealingTemperature;
		this.momentumWeight = momentumWeight;
		this.noRepetitionsSample = noRepetitionsSample;
	};


	public void start() {

		this.initGlobal();

		// Solve problem per delivery area
		ArrayList<ValueFunctionApproximationModel> models = new ArrayList<ValueFunctionApproximationModel>();
		for (DeliveryArea area : AggregateReferenceInformationAlgorithm.getDeliveryAreaSet().getElements()) {

			this.applyADPForDeliveryArea(area);

			ValueFunctionApproximationModel model = new ValueFunctionApproximationModel();
			model.setDeliveryAreaId(area.getId());
			model.setBasicCoefficient(this.linearFunctionInterceptPerDeliveryArea.get(area));
			model.setTimeCoefficient(this.linearFunctionCoefficientsPerDeliveryArea.get(area)[0]);
			if (this.considerOverallRemainingCapacity)
				model.setRemainingCapacityCoefficient(
						this.linearFunctionCoefficientsPerDeliveryArea.get(area)[this.indexRemainingBudget]);
			if (this.considerOverallAcceptedTimeInteraction)
				model.setTimeCapacityInteractionCoefficient(this.linearFunctionCoefficientsPerDeliveryArea
						.get(area)[this.indexOverallAcceptedTimeInteraction]);
			if (this.considerInsertionCosts)
				model.setAcceptedOverallCostCoefficient(
						this.linearFunctionCoefficientsPerDeliveryArea.get(area)[this.indexInsertionCosts]);
			if(this.mixedEcAndIc){
				model.setAcceptedOverallCostType(2);
			}else if(this.considerInsertionCosts){
				model.setAcceptedOverallCostType(1);
			}
				
			ArrayList<ValueFunctionApproximationCoefficient> coefficients = new ArrayList<ValueFunctionApproximationCoefficient>();
			for (TimeWindow tw : AggregateReferenceInformationAlgorithm.getTimeWindowSet().getElements()) {
				ValueFunctionApproximationCoefficient c = new ValueFunctionApproximationCoefficient();
				c.setCoefficient(this.linearFunctionCoefficientsPerDeliveryArea.get(area)[this.timeWindowInputMapper
						.get(tw.getId())]);
				c.setDeliveryAreaId(area.getId());
				c.setTimeWindowId(tw.getId());
				c.setType(ValueFunctionCoefficientType.NUMBER);
				coefficients.add(c);
			}
			model.setCoefficients(coefficients);

			// Build lookup table with averages
			ObjectMapper mapper = new ObjectMapper();
			mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
			String addonString = null;
			if (this.considerInsertionCosts || this.considerOverallRemainingCapacity
					|| this.considerOverallAcceptedTimeInteraction) {
				Double[][] lookupTable = new Double[this.lookupTableSumPerDeliveryArea
						.get(area).length][this.lookupTableSumPerDeliveryArea.get(area)[0].length];
				for (int i = 0; i < this.lookupTableSumPerDeliveryArea.get(area).length; i++) {
					for (int j = 0; j < this.lookupTableSumPerDeliveryArea.get(area)[0].length; j++) {
						if (this.lookupTableSumPerDeliveryArea.get(area)[i][j] != null) {
							lookupTable[i][j] = this.lookupTableSumPerDeliveryArea.get(area)[i][j]
									/ this.lookupTableCountPerDeliveryArea.get(area)[i][j];
						}

					}
				}

				NonParametricValueFunctionAddon addon = new NonParametricValueFunctionAddon(lookupTable, null,
						// this.randomForestPerDeliveryArea.get(area),
						this.mesoWeightLf, this.maxValuePerAttribute);
				try {
					addonString = mapper.writeValueAsString(addon);
				} catch (JsonProcessingException e) {

					e.printStackTrace();
				}
			} else {
				Double[] lookupTable = new Double[this.lookupArraySumPerDeliveryArea.get(area).length];
				for (int i = 0; i < this.lookupArraySumPerDeliveryArea.get(area).length; i++) {
					lookupTable[i] = this.lookupArraySumPerDeliveryArea.get(area)[i]
							/ this.lookupArrayCountPerDeliveryArea.get(area)[i];
				}

				NonParametricValueFunctionAddon addon = new NonParametricValueFunctionAddon(null, lookupTable,
						// this.randomForestPerDeliveryArea.get(area),
						this.mesoWeightLf, this.maxValuePerAttribute);

				try {
					addonString = mapper.writeValueAsString(addon);
				} catch (JsonProcessingException e) {

					e.printStackTrace();
				}
			}

			model.setComplexModelJSON(addonString);

			models.add(model);

		}

		// Prepare final model set
		ValueFunctionApproximationModelSet set = new ValueFunctionApproximationModelSet();
		set.setDeliveryAreaSetId(AggregateReferenceInformationAlgorithm.getDeliveryAreaSet().getId());
		set.setTimeWindowSetId(AggregateReferenceInformationAlgorithm.getTimeWindowSet().getId());
		set.setTypeId(1); // For linear model
		set.setIsCommitted(true);
		set.setIsNumber(true);
		set.setElements(models);

		this.result = set;

	}

	private void applyADPForDeliveryArea(DeliveryArea area) {

		// Per order request set, train the value function
		int currentIteration = 0;
		int numberOfRunThroughs = 0;
		while (numberOfRunThroughs <= this.noRepetitionsSample) {
			int currentIterationInner = 0;
			for (Integer requestSetId : this.orderRequestsPerDeliveryArea.get(area.getId()).keySet()) {
				// Stop early if only part of the set should be run
				if (currentIterationInner > (this.noRepetitionsSample - numberOfRunThroughs)
						* this.orderRequestsPerDeliveryArea.get(area.getId()).size())
					break;

				System.out.println("Iteration: " + currentIteration);
				Pair<ArrayList<double[]>, Double[]> observations = this.simulateArrivalProcess(area, requestSetId,
						currentIteration,
						(double) this.orderRequestsPerDeliveryArea.get(area.getId()).size() * noRepetitionsSample);
				// this.observationsPerDeliveryArea.get(area).add(observations);
				if (!tdUpdate) {
					this.updateLookupTable(area, observations.getKey(), observations.getValue());
					double currentLearningRate = this.learningRate;
					if (this.annealingTemperature > 0) {
						currentLearningRate = this.learningRate
								/ (1.0 + (double) currentIteration / this.annealingTemperature);
					}
					this.updateLinearFunctionWithGradientDescent(area, observations.getKey(), observations.getValue(),
							learningTypeBatch, currentLearningRate);
				}
				// if (currentIteration % 100 == 0)
				// this.updateLinearFunction(area, false);
				currentIteration++;
				currentIterationInner++;

			}
			numberOfRunThroughs++;
		}

	}

	/**
	 * Perform stochastic gradient descent with mini batch (= one simulation
	 * run) and Monte Carlo update
	 * 
	 * @param area
	 * @param observations
	 * @param values
	 * @param learningRate
	 */
	private void updateLinearFunctionWithGradientDescent(DeliveryArea area, ArrayList<double[]> observations,
			Double[] values, boolean batch, double learningRate) {

		MomentumHelper momentum = null;
		if (momentumWeight > 0) {
			momentum = oldMomentumPerDeliveryArea.get(area.getId());

		}

		// Determine all losses and update coefficients
		Double[] losses = new Double[values.length];

		double interceptGradient = 0.0;

		double timeGradient = 0.0;

		double icGradient = 0.0;

		HashMap<Integer, Double> timeWindowGradients = new HashMap<Integer, Double>();
		for (Integer tw : timeWindowInputMapper.keySet()) {
			timeWindowGradients.put(tw, 0.0);
		}

		double rbGradient = 0.0;

		double timeCapGradient = 0.0;

		for (int i = 0; i < values.length; i++) {

			double valueLf = this.linearFunctionInterceptPerDeliveryArea.get(area)
					+ this.linearFunctionCoefficientsPerDeliveryArea.get(area)[0] * observations.get(i)[0];
			if (indexRemainingBudget > 0) {
				valueLf += this.linearFunctionCoefficientsPerDeliveryArea.get(area)[indexRemainingBudget]
						* observations.get(i)[indexRemainingBudget];
			}

			if (indexInsertionCosts > 0) {
				valueLf += this.linearFunctionCoefficientsPerDeliveryArea.get(area)[indexInsertionCosts]
						* observations.get(i)[indexInsertionCosts];
			}

			if (this.indexOverallAcceptedTimeInteraction > 0) {
				valueLf += this.linearFunctionCoefficientsPerDeliveryArea.get(area)[indexOverallAcceptedTimeInteraction]
						* observations.get(i)[indexOverallAcceptedTimeInteraction];
			}

			for (Integer tw : timeWindowInputMapper.keySet()) {
				valueLf += this.linearFunctionCoefficientsPerDeliveryArea.get(area)[timeWindowInputMapper.get(tw)]
						* observations.get(i)[timeWindowInputMapper.get(tw)];
			}
			losses[i] = valueLf - values[i];
			if (batch) {
				// intercept = intercept - learningRate * losses[i] * 1.0;
				interceptGradient += losses[i] * 1.0;
				timeGradient += losses[i] * observations.get(i)[0];
				if (indexInsertionCosts > 0) {

					icGradient += losses[i] * observations.get(i)[indexInsertionCosts];
				}

				if (indexRemainingBudget > 0) {

					rbGradient += losses[i] * observations.get(i)[indexRemainingBudget];
				}

				if (indexOverallAcceptedTimeInteraction > 0) {

					timeCapGradient += losses[i] * observations.get(i)[indexOverallAcceptedTimeInteraction];
				}

				for (Integer tw : timeWindowInputMapper.keySet()) {

					timeWindowGradients.put(tw, timeWindowGradients.get(tw)
							+ losses[i] * observations.get(i)[timeWindowInputMapper.get(tw)]);
				}
			} else {
				this.updateLinearFunctionWithGradientDescentIndividualUpdate(area, losses[i], observations.get(i),
						learningRate);
			}

		}

		if (batch) {
			if (this.momentumWeight > 0) {
				double newMomentum = (1.0 - this.momentumWeight) * interceptGradient
						+ this.momentumWeight * momentum.getBasicCoefficientMomentum();
				this.linearFunctionInterceptPerDeliveryArea.put(area,
						this.linearFunctionInterceptPerDeliveryArea.get(area) - learningRate * newMomentum);
				this.oldMomentumPerDeliveryArea.get(area.getId()).setBasicCoefficientMomentum(newMomentum);
			} else {
				this.linearFunctionInterceptPerDeliveryArea.put(area,
						this.linearFunctionInterceptPerDeliveryArea.get(area) - learningRate * interceptGradient);
			}

			if (this.momentumWeight > 0) {
				double newMomentum = (1.0 - this.momentumWeight) * timeGradient
						+ this.momentumWeight * momentum.getMomentumForAttribute(0);
				// System.out.println("Current time coefficient:
				// "+this.linearFunctionCoefficientsPerDeliveryArea
				// .get(area)[0]+"; Current gradient:"+timeGradient+"; current
				// momentum:"+newMomentum+"; current alpha:"+learningRate);
				this.linearFunctionCoefficientsPerDeliveryArea
						.get(area)[0] = this.linearFunctionCoefficientsPerDeliveryArea.get(area)[0]
								- learningRate * newMomentum;
				this.oldMomentumPerDeliveryArea.get(area.getId()).setMomentumForAttribute(0, newMomentum);

			} else {
				this.linearFunctionCoefficientsPerDeliveryArea.get(
						area)[0] = linearFunctionCoefficientsPerDeliveryArea.get(area)[0] - learningRate * timeGradient;
			}

			if (indexRemainingBudget > 0) {

				if (this.momentumWeight > 0) {
					double newMomentum = (1.0 - this.momentumWeight) * rbGradient
							+ this.momentumWeight * momentum.getMomentumForAttribute(indexRemainingBudget);
					this.linearFunctionCoefficientsPerDeliveryArea
							.get(area)[indexRemainingBudget] = this.linearFunctionCoefficientsPerDeliveryArea
									.get(area)[indexRemainingBudget] - learningRate * newMomentum;
					this.oldMomentumPerDeliveryArea.get(area.getId()).setMomentumForAttribute(indexRemainingBudget,
							newMomentum);
				} else {
					this.linearFunctionCoefficientsPerDeliveryArea
							.get(area)[indexRemainingBudget] = this.linearFunctionCoefficientsPerDeliveryArea
									.get(area)[indexRemainingBudget] - learningRate * rbGradient;
				}

			}

			if (indexInsertionCosts > 0) {
				if (this.momentumWeight > 0) {
					double newMomentum = (1.0 - this.momentumWeight) * icGradient
							+ this.momentumWeight * momentum.getMomentumForAttribute(indexInsertionCosts);
					this.linearFunctionCoefficientsPerDeliveryArea
							.get(area)[indexInsertionCosts] = this.linearFunctionCoefficientsPerDeliveryArea
									.get(area)[indexInsertionCosts] - learningRate * newMomentum;
//					System.out.println("Current ic coefficient: "
//							+ this.linearFunctionCoefficientsPerDeliveryArea.get(area)[indexInsertionCosts]
//							+ "; Current gradient:" + icGradient + "; current momentum:" + newMomentum
//							+ "; current alpha:" + learningRate);

					this.oldMomentumPerDeliveryArea.get(area.getId()).setMomentumForAttribute(indexInsertionCosts,
							newMomentum);
				} else {
					this.linearFunctionCoefficientsPerDeliveryArea
							.get(area)[indexInsertionCosts] = this.linearFunctionCoefficientsPerDeliveryArea
									.get(area)[indexInsertionCosts] - learningRate * icGradient;
				}
			}

			if (indexOverallAcceptedTimeInteraction > 0) {
				if (this.momentumWeight > 0) {
					double newMomentum = (1.0 - this.momentumWeight) * timeCapGradient + this.momentumWeight
							* momentum.getMomentumForAttribute(indexOverallAcceptedTimeInteraction);
					this.linearFunctionCoefficientsPerDeliveryArea
							.get(area)[indexOverallAcceptedTimeInteraction] = this.linearFunctionCoefficientsPerDeliveryArea
									.get(area)[indexOverallAcceptedTimeInteraction] - learningRate * newMomentum;
					this.oldMomentumPerDeliveryArea.get(area.getId())
							.setMomentumForAttribute(indexOverallAcceptedTimeInteraction, newMomentum);
				} else {
					this.linearFunctionCoefficientsPerDeliveryArea
							.get(area)[indexOverallAcceptedTimeInteraction] = this.linearFunctionCoefficientsPerDeliveryArea
									.get(area)[indexOverallAcceptedTimeInteraction] - learningRate * timeCapGradient;
				}

			}

			for (Integer tw : timeWindowInputMapper.keySet()) {
				if (this.momentumWeight > 0) {
					double newMomentum = (1.0 - this.momentumWeight) * timeWindowGradients.get(tw)
							+ this.momentumWeight * momentum.getMomentumForAttribute(timeWindowInputMapper.get(tw));
					this.linearFunctionCoefficientsPerDeliveryArea.get(area)[timeWindowInputMapper
							.get(tw)] = this.linearFunctionCoefficientsPerDeliveryArea.get(area)[timeWindowInputMapper
									.get(tw)] - learningRate * newMomentum;
					

					this.oldMomentumPerDeliveryArea.get(area.getId())
							.setMomentumForAttribute(timeWindowInputMapper.get(tw), newMomentum);
				} else {
					this.linearFunctionCoefficientsPerDeliveryArea.get(area)[timeWindowInputMapper
							.get(tw)] = this.linearFunctionCoefficientsPerDeliveryArea.get(area)[timeWindowInputMapper
									.get(tw)] - learningRate * timeWindowGradients.get(tw);
				}
			}
			
			
//				 System.out.println("gradient cost:"+icGradient+" coefficient :"+this.linearFunctionCoefficientsPerDeliveryArea
//							.get(area)[indexInsertionCosts]+"tw 101 coeff:" +
//				 this.linearFunctionCoefficientsPerDeliveryArea
//				 .get(area)[timeWindowInputMapper.get(101)] + "grad:"+timeWindowGradients.get(101)+
//				 "tw 102 coeff:" +
//				 this.linearFunctionCoefficientsPerDeliveryArea
//				 .get(area)[timeWindowInputMapper.get(102)]+ "grad:"+timeWindowGradients.get(102)+
//				 "tw 103 coeff:" +
//				 this.linearFunctionCoefficientsPerDeliveryArea
//				 .get(area)[timeWindowInputMapper.get(103)]+ "grad:"+timeWindowGradients.get(103));
//				 ;
				
				 
		}

	}

	private void updateLinearFunctionWithGradientDescentIndividualUpdate(DeliveryArea area, double loss, double[] input,
			double learningRate) {

		MomentumHelper momentum = null;
		if (momentumWeight > 0) {
			momentum = oldMomentumPerDeliveryArea.get(area.getId());

		}
		if (this.momentumWeight > 0) {
			double newMomentum = (1.0 - this.momentumWeight) * loss * 1.0
					+ this.momentumWeight * momentum.getBasicCoefficientMomentum();
			double intercept = this.linearFunctionInterceptPerDeliveryArea.get(area) - learningRate * newMomentum;
			this.linearFunctionInterceptPerDeliveryArea.put(area, intercept);
			momentum.setBasicCoefficientMomentum(newMomentum);
		} else {
			double intercept = this.linearFunctionInterceptPerDeliveryArea.get(area) - learningRate * loss * 1.0;
			this.linearFunctionInterceptPerDeliveryArea.put(area, intercept);
		}

		if (this.momentumWeight > 0) {
			double newMomentum = (1.0 - this.momentumWeight) * loss * input[0]
					+ this.momentumWeight * momentum.getMomentumForAttribute(0);
			double timeCoef = this.linearFunctionCoefficientsPerDeliveryArea.get(area)[0] - learningRate * newMomentum;
			this.linearFunctionCoefficientsPerDeliveryArea.get(area)[0] = timeCoef;
			momentum.setMomentumForAttribute(0, newMomentum);
		} else {
			double timeCoef = this.linearFunctionCoefficientsPerDeliveryArea.get(area)[0]
					- learningRate * loss * input[0];
			this.linearFunctionCoefficientsPerDeliveryArea.get(area)[0] = timeCoef;
		}

		if (indexInsertionCosts > 0) {

			if (this.momentumWeight > 0) {
				double newMomentum = (1.0 - this.momentumWeight) * loss * input[indexInsertionCosts]
						+ this.momentumWeight * momentum.getMomentumForAttribute(indexInsertionCosts);
				double insertionCostsCoef = this.linearFunctionCoefficientsPerDeliveryArea
						.get(area)[indexInsertionCosts] - learningRate * newMomentum;
				this.linearFunctionCoefficientsPerDeliveryArea.get(area)[indexInsertionCosts] = insertionCostsCoef;
				momentum.setMomentumForAttribute(indexInsertionCosts, newMomentum);
			} else {
				double insertionCostsCoef = this.linearFunctionCoefficientsPerDeliveryArea
						.get(area)[indexInsertionCosts] - learningRate * loss * input[indexInsertionCosts];
				this.linearFunctionCoefficientsPerDeliveryArea.get(area)[indexInsertionCosts] = insertionCostsCoef;
			}

		}

		if (indexRemainingBudget > 0) {

			if (this.momentumWeight > 0) {
				double newMomentum = (1.0 - this.momentumWeight) * loss * input[indexRemainingBudget]
						+ this.momentumWeight * momentum.getMomentumForAttribute(indexRemainingBudget);
				double remainingBudgetCoef = this.linearFunctionCoefficientsPerDeliveryArea
						.get(area)[indexRemainingBudget] - learningRate * newMomentum;
				this.linearFunctionCoefficientsPerDeliveryArea.get(area)[indexRemainingBudget] = remainingBudgetCoef;
				momentum.setMomentumForAttribute(indexRemainingBudget, newMomentum);
			} else {
				double remainingBudgetCoef = this.linearFunctionCoefficientsPerDeliveryArea
						.get(area)[indexRemainingBudget] - learningRate * loss * input[indexRemainingBudget];
				this.linearFunctionCoefficientsPerDeliveryArea.get(area)[indexRemainingBudget] = remainingBudgetCoef;
			}

		}

		if (indexOverallAcceptedTimeInteraction > 0) {

			if (this.momentumWeight > 0) {
				double newMomentum = (1.0 - this.momentumWeight) * loss * input[indexOverallAcceptedTimeInteraction]
						+ this.momentumWeight * momentum.getMomentumForAttribute(indexOverallAcceptedTimeInteraction);
				double timeCapCoef = this.linearFunctionCoefficientsPerDeliveryArea
						.get(area)[indexOverallAcceptedTimeInteraction] - learningRate * newMomentum;
				this.linearFunctionCoefficientsPerDeliveryArea
						.get(area)[indexOverallAcceptedTimeInteraction] = timeCapCoef;
				momentum.setMomentumForAttribute(indexOverallAcceptedTimeInteraction, newMomentum);
			} else {
				double timeCapCoef = this.linearFunctionCoefficientsPerDeliveryArea
						.get(area)[indexOverallAcceptedTimeInteraction]
						- learningRate * loss * input[indexOverallAcceptedTimeInteraction];
				this.linearFunctionCoefficientsPerDeliveryArea
						.get(area)[indexOverallAcceptedTimeInteraction] = timeCapCoef;
			}

		}

		for (Integer tw : timeWindowInputMapper.keySet()) {

			if (this.momentumWeight > 0) {
				double newMomentum = (1.0 - this.momentumWeight) * loss * input[timeWindowInputMapper.get(tw)]
						+ this.momentumWeight * momentum.getMomentumForAttribute(timeWindowInputMapper.get(tw));
				double coeffTw = this.linearFunctionCoefficientsPerDeliveryArea.get(area)[timeWindowInputMapper.get(tw)]
						- learningRate * newMomentum;
				this.linearFunctionCoefficientsPerDeliveryArea.get(area)[timeWindowInputMapper.get(tw)] = coeffTw;
				momentum.setMomentumForAttribute(timeWindowInputMapper.get(tw), newMomentum);
			} else {
				double coeffTw = this.linearFunctionCoefficientsPerDeliveryArea.get(area)[timeWindowInputMapper.get(tw)]
						- learningRate * loss * input[timeWindowInputMapper.get(tw)];
				this.linearFunctionCoefficientsPerDeliveryArea.get(area)[timeWindowInputMapper.get(tw)] = coeffTw;
			}

		}
	}

	private Pair<ArrayList<double[]>, Double[]> simulateArrivalProcess(DeliveryArea area, int requestSetId,
			int currentIteration, double numberOfIterations) {

		ArrayList<double[]> observations = new ArrayList<double[]>();

		// Initialise routes for dynamic feasibility check
		HashMap<Integer, ArrayList<RouteElement>> routes = DynamicRoutingHelperService.initialiseRoutes(area,
				this.vehicleAssignmentsPerDeliveryArea, AggregateReferenceInformationAlgorithm.getTimeWindowSet(),
				AggregateReferenceInformationAlgorithm.getTIME_MULTIPLIER(),
				AggregateReferenceInformationAlgorithm.getRegion(), (includeDriveFromStartingPosition == 1));

		this.initialiseAlreadyAcceptedPerTimeWindow(area);
		if (this.considerInsertionCosts)
			this.initialiseAcceptedCostOverall(area);
		if (this.considerOverallRemainingCapacity)
			this.initialiseRemainingCapacity(area);
		if (this.considerOverallAcceptedTimeInteraction)
			this.initialiseOverallAcceptedAmount(area);
		Double currentAcceptedTravelTime = null;
		HashMap<Integer, ArrayList<RouteElement>> bestRoutingSoFar = null;
		ArrayList<Order> acceptedOrders = new ArrayList<Order>();

		HashMap<Integer, Double> distancePerRouting = new HashMap<Integer, Double>();
		HashMap<Integer, HashMap<Integer, Double>> distancePerTwAndRouting = new HashMap<Integer, HashMap<Integer, Double>>();
		if (distanceType != 0) {
			for (Routing r : AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNo.get(area)
					.keySet()) {
				distancePerRouting.put(r.getId(), 0.0);

				for (TimeWindow tw : AggregateReferenceInformationAlgorithm.getTimeWindowSet().getElements()) {
					if (!distancePerTwAndRouting.containsKey(tw.getId())) {
						distancePerTwAndRouting.put(tw.getId(), new HashMap<Integer, Double>());
					}

					distancePerTwAndRouting.get(tw.getId()).put(r.getId(), 0.0);
				}
			}
			this.distancePerDeliveryAreaAndRouting.put(area.getId(), distancePerRouting);
			this.distancePerDeliveryAreaAndTwAndRouting.put(area.getId(), distancePerTwAndRouting);
		}

		HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerSubDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Integer>>();
		Double currentDistanceMeasure = 0.0;
		HashMap<Integer, Double> currentDistanceMeasurePerTw = null;
		if (this.distanceMeasurePerTw) {
			currentDistanceMeasurePerTw = new HashMap<Integer, Double>();
			for (TimeWindow tw : AggregateReferenceInformationAlgorithm.getTimeWindowSet().getElements()) {
				currentDistanceMeasurePerTw.put(tw.getId(), 0.0);
			}
		}

		double[] newObservation = new double[this.numberOfAttributes];
		if (!tdUpdate) {
			newObservation[0] = 1.0;
			for (Integer tw : this.timeWindowInputMapper.keySet()) {
				newObservation[this.timeWindowInputMapper.get(tw)] = 0.0;
			}
			if (this.considerInsertionCosts) {
				newObservation[this.indexInsertionCosts] = 0.0;
			}

			if (this.considerOverallRemainingCapacity) {
				newObservation[this.indexRemainingBudget] = 1.0;
			}

			if (this.considerOverallAcceptedTimeInteraction) {
				newObservation[this.indexOverallAcceptedTimeInteraction] = 0.0;
				newObservation[this.indexOverallAccepted] = 0.0;

			}

			observations.add(newObservation);
		}
		// Go through requests and update value function
		int disT = this.distanceType;
		if(this.distanceType ==4){
			disT = 2;
			this.maximumDistanceMeasureIncrease=0;
		}
		for (int t = this.orderHorizonLength; t > 0; t--) {

			if (t <= switchDistanceOffPoint) {
				disT = 0;
			}
			
			if(this.distanceType ==4 && t<= this.orderHorizonLength*1.0/3.0){
				disT=3;
				this.maximumDistanceMeasureIncrease=3;
			}
			
			OrderRequest request;
			if (this.orderRequestsPerDeliveryArea.get(area.getId()).get(requestSetId).containsKey(t)) {
				request = this.orderRequestsPerDeliveryArea.get(area.getId()).get(requestSetId).get(t);
			} else {
				request = null;
			}

			// Check feasible time windows
			ArrayList<HashMap<Integer, ArrayList<RouteElement>>> possibleRoutings = new ArrayList<HashMap<Integer, ArrayList<RouteElement>>>();
			HashMap<Integer, Pair<RouteElement, Double>> bestRoutingsValueAfterInsertion = new HashMap<Integer, Pair<RouteElement, Double>>();
			DeliveryArea subArea = null;
			if (request != null) {
				ArrayList<TimeWindow> consideredTimeWindows = new ArrayList<TimeWindow>();
				for (ConsiderationSetAlternative alt : request.getCustomer().getOriginalDemandSegment()
						.getConsiderationSet()) {
					if (!alt.getAlternative().getNoPurchaseAlternative()) {
						consideredTimeWindows.add(alt.getAlternative().getTimeWindows().get(0));
					}

				}
				currentAcceptedTravelTime = DynamicRoutingHelperService
						.determineFeasibleTimeWindowsAndBestRoutingAfterInsertionBasedOnDynamicRoutingWithShiftWithoutWait(
								request, AggregateReferenceInformationAlgorithm.getRegion(),
								AggregateReferenceInformationAlgorithm.getTIME_MULTIPLIER(),
								AggregateReferenceInformationAlgorithm.getTimeWindowSet(),
								(this.includeDriveFromStartingPosition == 1), this.getExpectedServiceTime(),
								possibleRoutings, this.numberOfGRASPSolutions, this.numberPotentialInsertionCandidates,
								vasPerDeliveryAreaSetAndVehicleNo.get(area.getId()), acceptedOrders, bestRoutingSoFar,
								currentAcceptedTravelTime, consideredTimeWindows, bestRoutingsValueAfterInsertion,
								numberOfThreads);
				if (this.considerOverallRemainingCapacity) {
					this.remainingCapacityPerDeliveryArea.put(area.getId(),
							this.overallCapacityPerDeliveryArea.get(area.getId()) - currentAcceptedTravelTime
									- this.getExpectedServiceTime() * acceptedOrders.size());
				}
				subArea = LocationService.assignCustomerToDeliveryAreaConsideringHierarchy(getDeliveryAreaSet(),
						request.getCustomer());
				subArea.setSetId(area.getSubsetId());
			}

			Pair<RouteElement, Double> result = this.chooseOfferSetAndSimulateDecision(t, area, request,
					bestRoutingsValueAfterInsertion, routes, alternativesToTimeWindows, currentIteration,
					numberOfIterations, currentDistanceMeasure, currentDistanceMeasurePerTw,
					alreadyAcceptedPerSubDeliveryAreaAndTimeWindow, subArea, disT);

			RouteElement newElement = result.getKey();

			// If customer chose a time window, he needs to be added to the
			// current route
			if (newElement != null) {

				int routingId = bestRoutingsValueAfterInsertion.get(newElement.getOrder().getTimeWindowFinalId())
						.getKey().getTempRoutingId();
				DynamicRoutingHelperService.insertRouteElement(newElement, possibleRoutings.get(routingId),
						vasPerDeliveryAreaSetAndVehicleNo.get(area.getId()), getTimeWindowSet(),
						AggregateReferenceInformationAlgorithm.getTIME_MULTIPLIER(),
						(includeDriveFromStartingPosition == 1));
				bestRoutingSoFar = possibleRoutings.get(routingId);
				currentAcceptedTravelTime = bestRoutingsValueAfterInsertion
						.get(newElement.getOrder().getTimeWindowFinalId()).getValue();
				acceptedOrders.add(newElement.getOrder());
				if (this.distanceType != 0) {
					if (!alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.containsKey(subArea.getId())) {
						alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.put(subArea.getId(),
								new HashMap<Integer, Integer>());
					}
					if (!alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId())
							.containsKey(newElement.getOrder().getTimeWindowFinalId())) {
						alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId())
								.put(newElement.getOrder().getTimeWindowFinalId(), 0);
					}
					alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId()).put(
							newElement.getOrder().getTimeWindowFinalId(), alreadyAcceptedPerSubDeliveryAreaAndTimeWindow
									.get(subArea.getId()).get(newElement.getOrder().getTimeWindowFinalId()) + 1);
					currentDistanceMeasure = result.getValue();
				}

			}
			if (!tdUpdate) {
				newObservation = new double[this.numberOfAttributes];
				newObservation[0] = ((double) (t - 1)) / this.maxValuePerAttribute[0];
				for (Integer tw : this.timeWindowInputMapper.keySet()) {
					newObservation[this.timeWindowInputMapper
							.get(tw)] = (double) this.alreadyAcceptedPerDeliveryArea.get(area.getId()).get(tw)
									/ this.maxValuePerAttribute[this.timeWindowInputMapper.get(tw)];
				}
				if (this.considerInsertionCosts) {
					newObservation[this.indexInsertionCosts] = this.acceptedCostPerDeliveryArea.get(area.getId())
							/ this.maxValuePerAttribute[this.indexInsertionCosts];
				}

				if (this.considerOverallRemainingCapacity) {
					newObservation[this.indexRemainingBudget] = this.remainingCapacityPerDeliveryArea.get(area.getId())
							/ this.maxValuePerAttribute[this.indexRemainingBudget];
				}

				if (this.considerOverallAcceptedTimeInteraction) {
					newObservation[this.indexOverallAcceptedTimeInteraction] = this.acceptedAmountPerDeliveryArea
							.get(area.getId()) * (this.orderHorizonLength - (double) (t - 1))
							/ this.maxValuePerAttribute[this.indexOverallAcceptedTimeInteraction];
					newObservation[this.indexOverallAccepted] = this.acceptedAmountPerDeliveryArea.get(area.getId())
							/ this.maxValuePerAttribute[this.indexOverallAccepted];

				}

				observations.add(newObservation);
			}
		}
		Double[] outputValues = null;
		if (!tdUpdate) {
			outputValues = this.prepareOutputValues(acceptedOrders, false);
		}
		return new Pair<ArrayList<double[]>, Double[]>(observations, outputValues);
	}

	private Pair<RouteElement, Double> chooseOfferSetAndSimulateDecision(int t, DeliveryArea area, OrderRequest request,
			HashMap<Integer, Pair<RouteElement, Double>> bestRoutingsValueAfterInsertion,
			HashMap<Integer, ArrayList<RouteElement>> routes,
			HashMap<TimeWindow, Alternative> alternativesToTimeWindows, int currentIteration, double numberOfIterations,
			double currentDistanceMeasure, HashMap<Integer, Double> currentDistanceMeasurePerTw,
			HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerSubDeliveryAreaAndTimeWindow,
			DeliveryArea subArea, int disT) {

		// Calculate value of same state in next time step
		// If at end of order horizon, the value is 0
		int numberOfRelevantAttributes = numberOfAttributes;
		if (this.indexOverallAcceptedTimeInteraction > 0)
			numberOfRelevantAttributes--;
		double[] input = new double[numberOfRelevantAttributes];
		double noAssignmentValue = ADPmeso.determineValue(this.linearFunctionCoefficientsPerDeliveryArea.get(area),
				this.linearFunctionInterceptPerDeliveryArea.get(area), this.lookupTableSumPerDeliveryArea.get(area),
				this.lookupTableCountPerDeliveryArea.get(area), this.lookupArraySumPerDeliveryArea.get(area),
				this.lookupArrayCountPerDeliveryArea.get(area), randomForestPerDeliveryArea.get(area), t - 1,
				this.alreadyAcceptedPerDeliveryArea.get(area.getId()),
				this.remainingCapacityPerDeliveryArea.get(area.getId()),
				this.acceptedCostPerDeliveryArea.get(area.getId()),
				this.acceptedAmountPerDeliveryArea.get(area.getId()), maxValuePerAttribute, this.timeWindowInputMapper,
				this.indexInsertionCosts, this.indexRemainingBudget, this.indexOverallAcceptedTimeInteraction,
				this.indexOverallAccepted, this.mesoWeightLf, this.numberOfAttributes, input) * maximumValueAcceptable;

		double maxValue = 0.0;
		ArrayList<AlternativeOffer> selectedOfferedAlternatives = new ArrayList<AlternativeOffer>();
		// ArrayList<Pair<ArrayList<AlternativeOffer>, Double>> offerSetValues =
		// new ArrayList<Pair<ArrayList<AlternativeOffer>, Double>>();

		Set<TimeWindow> timeWindowFeasible = new HashSet<TimeWindow>();
		ArrayList<TimeWindow> timeWindows = new ArrayList<TimeWindow>();
		HashMap<Integer, HashMap<Integer, Double>> newDistancePerTimeWindowAndRouting = new HashMap<Integer, HashMap<Integer, Double>>();
		HashMap<Integer, Routing> routingSmallestDistancePerTimeWindow = new HashMap<Integer, Routing>();
		HashMap<Integer, Double> distanceMeasurePerTimeWindow = null;
		if (request != null) {

			for (Integer twId : bestRoutingsValueAfterInsertion.keySet()) {
				TimeWindow tw = AggregateReferenceInformationAlgorithm.getTimeWindowSet().getTimeWindowById(twId);
				// Do not add time window, if accepted cost relevant and no
				// offer in STPs
				if (!this.considerInsertionCosts || (this.considerInsertionCosts
						&& AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts
								.containsKey(subArea)
						&& AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts.get(subArea)
								.containsKey(tw)) || (this.considerInsertionCosts && this.mixedEcAndIc && (bestRoutingsValueAfterInsertion.get(twId).getKey().getWaitingTime()
								+ bestRoutingsValueAfterInsertion.get(twId).getKey()
										.getTempBufferToNext())< averageAdditionalCostPerOrder * 2.0)) {
					timeWindows.add(tw);
				}

			}

			// Determine distance value
			if (disT != 0) {
				distanceMeasurePerTimeWindow = ADPWithSoftOrienteeringANN
						.calculateResultingDistancePerTimeWindowAndRouting(subArea, neighbors.get(subArea),
								neighborsTw, timeWindows,
								AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationNo.get(area),
								AggregateReferenceInformationAlgorithm.countAcceptableCombinationOverReferences
										.get(area),
								alreadyAcceptedPerSubDeliveryAreaAndTimeWindow,
								this.distancePerDeliveryAreaAndRouting.get(area.getId()),
								distancePerDeliveryAreaAndTwAndRouting.get(area.getId()),
								newDistancePerTimeWindowAndRouting, routingSmallestDistancePerTimeWindow, disT,
								this.distanceMeasurePerTw);
			}

			// For all feasible, assume you accept -> get value
			HashMap<TimeWindow, Double> twValue = new HashMap<TimeWindow, Double>();
			for (TimeWindow tw : timeWindows) {
				int twId = tw.getId();
				if (this.distanceMeasurePerTw)
					currentDistanceMeasure = currentDistanceMeasurePerTw.get(tw.getId());
				if (disT == 0 || distanceMeasurePerTimeWindow.get(tw.getId())
						- currentDistanceMeasure < this.maximumDistanceMeasureIncrease + 1) {
					timeWindowFeasible.add(tw);

					int currentAccepted = this.alreadyAcceptedPerDeliveryArea.get(area.getId()).get(twId);
					this.alreadyAcceptedPerDeliveryArea.get(area.getId()).put(twId, ++currentAccepted);

					double currentOverallRemainingCapacity = 0.0;
					if (this.considerOverallRemainingCapacity) {
						currentOverallRemainingCapacity = this.remainingCapacityPerDeliveryArea.get(area.getId());
						double newRemainingCapacity = currentOverallRemainingCapacity
								- bestRoutingsValueAfterInsertion.get(twId).getKey().getTempShiftWithoutWait();
						if (newRemainingCapacity < 0) {
							System.out.println("Remaining capacity is negative");
						}
						this.remainingCapacityPerDeliveryArea.put(area.getId(), newRemainingCapacity);
					}

					double currentOverallAcceptedAmount = 0.0;
					if (this.considerOverallAcceptedTimeInteraction) {
						currentOverallAcceptedAmount = this.acceptedAmountPerDeliveryArea.get(area.getId());
						double newOverallAcceptedAmount = currentOverallAcceptedAmount + 1;
						this.acceptedAmountPerDeliveryArea.put(area.getId(), newOverallAcceptedAmount);
					}

					double currentOverallAcceptedCost = 0.0;
					if (this.considerInsertionCosts) {
						currentOverallAcceptedCost = this.acceptedCostPerDeliveryArea.get(area.getId());
						double newAcceptedCostOverall = currentOverallAcceptedCost;
						
						//For pure EC, take expected cost
						if(!this.mixedEcAndIc){
							newAcceptedCostOverall = newAcceptedCostOverall
									+ AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts
											.get(subArea).get(tw);
						}else{
							if((bestRoutingsValueAfterInsertion.get(twId).getKey().getWaitingTime()
								+ bestRoutingsValueAfterInsertion.get(twId).getKey()
										.getTempBufferToNext())<averageAdditionalCostPerOrder){
								newAcceptedCostOverall = newAcceptedCostOverall
										+ bestRoutingsValueAfterInsertion.get(twId).getKey().getTempShiftWithoutWait();
							}else if((bestRoutingsValueAfterInsertion.get(twId).getKey().getWaitingTime()
								+ bestRoutingsValueAfterInsertion.get(twId).getKey()
										.getTempBufferToNext())<averageAdditionalCostPerOrder*2.0){
								if(AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts
										.containsKey(subArea)
										&& AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts.get(subArea)
										.containsKey(tw)){
									newAcceptedCostOverall = newAcceptedCostOverall
											+ (bestRoutingsValueAfterInsertion.get(twId).getKey().getTempShiftWithoutWait()+
													AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts
													.get(subArea).get(tw))/2.0;
								}else{
									newAcceptedCostOverall = newAcceptedCostOverall
											+ bestRoutingsValueAfterInsertion.get(twId).getKey().getTempShiftWithoutWait();
								}
							}else{
								newAcceptedCostOverall = newAcceptedCostOverall
										+ AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts
												.get(subArea).get(tw);
							}
						}
						
						this.acceptedCostPerDeliveryArea.put(area.getId(), newAcceptedCostOverall);
					}

					double assignmentValue = ADPmeso.determineValue(
							this.linearFunctionCoefficientsPerDeliveryArea.get(area),
							this.linearFunctionInterceptPerDeliveryArea.get(area),
							this.lookupTableSumPerDeliveryArea.get(area),
							this.lookupTableCountPerDeliveryArea.get(area),
							this.lookupArraySumPerDeliveryArea.get(area),
							this.lookupArrayCountPerDeliveryArea.get(area), randomForestPerDeliveryArea.get(area),
							request.getArrivalTime() - 1, this.alreadyAcceptedPerDeliveryArea.get(area.getId()),
							this.remainingCapacityPerDeliveryArea.get(area.getId()),
							this.acceptedCostPerDeliveryArea.get(area.getId()),
							this.acceptedAmountPerDeliveryArea.get(area.getId()), maxValuePerAttribute,
							this.timeWindowInputMapper, this.indexInsertionCosts, this.indexRemainingBudget,
							this.indexOverallAcceptedTimeInteraction, this.indexOverallAccepted, this.mesoWeightLf,
							this.numberOfAttributes, input) * maximumValueAcceptable;

					twValue.put(tw, assignmentValue);

					this.alreadyAcceptedPerDeliveryArea.get(area.getId()).put(twId, --currentAccepted);

					if (this.considerOverallRemainingCapacity) {
						this.remainingCapacityPerDeliveryArea.put(area.getId(), currentOverallRemainingCapacity);
					}

					if (this.considerOverallAcceptedTimeInteraction) {
						this.acceptedAmountPerDeliveryArea.put(area.getId(), currentOverallAcceptedAmount);
						;
					}

					if (this.considerInsertionCosts) {
						this.acceptedCostPerDeliveryArea.put(area.getId(), currentOverallAcceptedCost);
					}

				}

			}
			// Find best subset from the time windows with value add

			Pair<ArrayList<AlternativeOffer>, Double> bestOffer = AssortmentProblemService.determineBestOfferSet(
					request, twValue, noAssignmentValue, AggregateReferenceInformationAlgorithm.getMaximumRevenueValue(),
					AggregateReferenceInformationAlgorithm.getObjectiveSpecificValues(), algo, alternativesToTimeWindows, possiblyLargeOfferSet,
					this.useActualBasketValue, false, null, null, 1.0);
			maxValue = bestOffer.getValue();
			selectedOfferedAlternatives = bestOffer.getKey();

		}

		// Update with TD update
		if (tdUpdate) {

			double currentStepSize = this.learningRate;
			if (this.annealingTemperature > 0) {
				currentStepSize = this.learningRate / (1.0 + (double) currentIteration / this.annealingTemperature);
			}

			double newValue = (noAssignmentValue + maxValue) / maximumValueAcceptable;
			double loss = ADPmeso.determineValue(this.linearFunctionCoefficientsPerDeliveryArea.get(area),
					this.linearFunctionInterceptPerDeliveryArea.get(area), this.lookupTableSumPerDeliveryArea.get(area),
					this.lookupTableCountPerDeliveryArea.get(area), this.lookupArraySumPerDeliveryArea.get(area),
					this.lookupArrayCountPerDeliveryArea.get(area), randomForestPerDeliveryArea.get(area), t,
					this.alreadyAcceptedPerDeliveryArea.get(area.getId()),
					this.remainingCapacityPerDeliveryArea.get(area.getId()),
					this.acceptedCostPerDeliveryArea.get(area.getId()),
					this.acceptedAmountPerDeliveryArea.get(area.getId()), maxValuePerAttribute,
					this.timeWindowInputMapper, this.indexInsertionCosts, this.indexRemainingBudget,
					this.indexOverallAcceptedTimeInteraction, this.indexOverallAccepted, this.mesoWeightLf,
					this.numberOfAttributes, input) - newValue;
			// System.out.println("Loss:"+loss);
			this.updateLinearFunctionWithGradientDescentIndividualUpdate(area, loss, input, currentStepSize);
		}

		// Simulate customer decision
		if (request != null) {
			/// Choose offer set
			if (bestRoutingsValueAfterInsertion.keySet().size() > 0) {
				if (this.explorationStrategy == 2) {

					// E-Greedy
					LearningService.chooseOfferSetBasedOnEGreedyStrategy(timeWindowFeasible, E_GREEDY_VALUE, 1.0,
							currentIteration, (int) numberOfIterations, 1.0 / 3.0, 2.0 / 3.0,
							selectedOfferedAlternatives, alternativesToTimeWindows);

				}
			}

			/// If windows are offered, let customer choose
			if (selectedOfferedAlternatives.size() > 0) {
				// Sample selection from customer
				Order order = new Order();
				order.setOrderRequest(request);
				order.setOrderRequestId(request.getId());

				AlternativeOffer selectedAlt;
				if (usepreferencesSampled) {
					selectedAlt = CustomerDemandService.selectCustomerDemandBasedOnSampledPreferences(
							selectedOfferedAlternatives, order, this.alternativeSet.getNoPurchaseAlternative());

				} else {
					selectedAlt = CustomerDemandService.sampleCustomerDemand(selectedOfferedAlternatives, order);
				}

				if (selectedAlt != null) {
					order.setAccepted(true);
					int twId = selectedAlt.getAlternative().getTimeWindows().get(0).getId();
					order.setTimeWindowFinalId(twId);
					int currentAccepted = this.alreadyAcceptedPerDeliveryArea.get(area.getId()).get(twId);
					this.alreadyAcceptedPerDeliveryArea.get(area.getId()).put(twId, ++currentAccepted);

					if (this.considerOverallRemainingCapacity) {
						double currentOverallRemainingCapacity = this.remainingCapacityPerDeliveryArea
								.get(area.getId());
						double newRemainingCapacity = currentOverallRemainingCapacity
								- bestRoutingsValueAfterInsertion.get(twId).getKey().getTempShiftWithoutWait();
						this.remainingCapacityPerDeliveryArea.put(area.getId(), newRemainingCapacity);
						if (newRemainingCapacity < 0) {
							System.out.println("Remaining capacity is negative");
						}
					}

					double currentOverallAcceptedAmount = 0.0;
					if (this.considerOverallAcceptedTimeInteraction) {
						currentOverallAcceptedAmount = this.acceptedAmountPerDeliveryArea.get(area.getId());
						double newOverallAcceptedAmount = currentOverallAcceptedAmount + 1;
						this.acceptedAmountPerDeliveryArea.put(area.getId(), newOverallAcceptedAmount);
					}

					if (this.considerInsertionCosts) {
						double currentOverallAcceptedCost = this.acceptedCostPerDeliveryArea.get(area.getId());
						double newAcceptedCostOverall = currentOverallAcceptedCost;
						//For pure EC, take expected cost
						if(!this.mixedEcAndIc){
							newAcceptedCostOverall = newAcceptedCostOverall
									+ AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts
											.get(subArea).get(selectedAlt.getAlternative().getTimeWindows().get(0));
						}else{
							if((bestRoutingsValueAfterInsertion.get(twId).getKey().getWaitingTime()
								+ bestRoutingsValueAfterInsertion.get(twId).getKey()
										.getTempBufferToNext())<averageAdditionalCostPerOrder){
								newAcceptedCostOverall = newAcceptedCostOverall
										+ bestRoutingsValueAfterInsertion.get(twId).getKey().getTempShiftWithoutWait();
							}else if((bestRoutingsValueAfterInsertion.get(twId).getKey().getWaitingTime()
								+ bestRoutingsValueAfterInsertion.get(twId).getKey()
										.getTempBufferToNext())<averageAdditionalCostPerOrder*2.0){
								if(AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts
										.containsKey(subArea)
										&& AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts.get(subArea)
										.containsKey(selectedAlt.getAlternative().getTimeWindows().get(0))){
									newAcceptedCostOverall = newAcceptedCostOverall
											+ (bestRoutingsValueAfterInsertion.get(twId).getKey().getTempShiftWithoutWait()+
													AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts
													.get(subArea).get(selectedAlt.getAlternative().getTimeWindows().get(0)))/2.0;
								}else{
									newAcceptedCostOverall = newAcceptedCostOverall
											+ bestRoutingsValueAfterInsertion.get(twId).getKey().getTempShiftWithoutWait();
								}
							}else{
								newAcceptedCostOverall = newAcceptedCostOverall
										+ AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts
												.get(subArea).get(selectedAlt.getAlternative().getTimeWindows().get(0));
							}
						}

						this.acceptedCostPerDeliveryArea.put(area.getId(), newAcceptedCostOverall);
					}

					// Update distances
					if (disT != 0) {
						this.distancePerDeliveryAreaAndRouting.put(area.getId(),
								newDistancePerTimeWindowAndRouting.get(order.getTimeWindowFinalId()));
						this.distancePerDeliveryAreaAndTwAndRouting.get(area.getId()).put(order.getTimeWindowFinalId(),
								newDistancePerTimeWindowAndRouting.get(order.getTimeWindowFinalId()));
						currentDistanceMeasure = distanceMeasurePerTimeWindow.get(order.getTimeWindowFinalId());
						if (this.distanceMeasurePerTw)
							currentDistanceMeasurePerTw.put(order.getTimeWindowFinalId(),
									distanceMeasurePerTimeWindow.get(order.getTimeWindowFinalId()));
					}

					bestRoutingsValueAfterInsertion.get(twId).getKey().setOrder(order);
					return new Pair<RouteElement, Double>(bestRoutingsValueAfterInsertion.get(twId).getKey(),
							currentDistanceMeasure);
				}

			}
		}
		return new Pair<RouteElement, Double>(null, currentDistanceMeasure);

	}

	public static double determineValue(double[] linearFunctionCoefficients, Double linearFunctionIntercept,
			Double[][] lookupTableSum, Integer[][] lookupTableCount, Double[] lookupArraySum,
			Integer[] lookupArrayCount, smile.regression.RandomForest randomForest, int t,
			HashMap<Integer, Integer> alreadyAccepted, Double remainingCapacity, Double acceptedCost,
			Double acceptedAmount, double[] maxValuePerAttribute, HashMap<Integer, Integer> timeWindowInputMapper,
			int indexInsertionCosts, int indexRemainingBudget, int indexAmountTimeInteraction, int indexAmount,
			double mesoWeightLf, int numberOfAttributes, double[] input) {

		double value = 0.0;
		// int numberOfRelevantAttributes = numberOfAttributes;
		// if (indexAmountTimeInteraction > 0)
		// numberOfRelevantAttributes--;

		input[0] = t / maxValuePerAttribute[0];
		double amountOverall = 0.0;
		double valueLf = linearFunctionIntercept + linearFunctionCoefficients[0] * input[0];
		if (indexRemainingBudget > 0) {
			input[indexRemainingBudget] = remainingCapacity / maxValuePerAttribute[indexRemainingBudget];
			valueLf += linearFunctionCoefficients[indexRemainingBudget] * input[indexRemainingBudget];
		}

		if (indexInsertionCosts > 0) {
			input[indexInsertionCosts] = acceptedCost / maxValuePerAttribute[indexInsertionCosts];
			valueLf += linearFunctionCoefficients[indexInsertionCosts] * input[indexInsertionCosts];
		}

		if (indexAmountTimeInteraction > 0) {
			input[indexAmountTimeInteraction] = acceptedAmount * (maxValuePerAttribute[0] - t)
					/ maxValuePerAttribute[indexAmountTimeInteraction];
			amountOverall = acceptedAmount / maxValuePerAttribute[indexAmount];
			valueLf += linearFunctionCoefficients[indexAmountTimeInteraction] * input[indexAmountTimeInteraction];

		}

		for (Integer tw : timeWindowInputMapper.keySet()) {
			input[timeWindowInputMapper.get(tw)] = alreadyAccepted.get(tw)
					/ maxValuePerAttribute[timeWindowInputMapper.get(tw)];
			valueLf += linearFunctionCoefficients[timeWindowInputMapper.get(tw)] * input[timeWindowInputMapper.get(tw)];
		}

		value = valueLf * mesoWeightLf;

		// Interesting: random forest value

		// double valueRF = randomForest.predict(input);

		double valueLT;

		int timeIndex = (int) Math.floor(input[0] / (1.0 / 100.0));
		if (timeIndex == 100)
			timeIndex = 100 - 1;
		if (indexInsertionCosts > 0 || indexRemainingBudget > 0 || indexAmountTimeInteraction > 0) {
			int otherIndex;
			if (indexInsertionCosts > 0) {
				if (input[indexInsertionCosts] > 1.0)
					input[indexInsertionCosts] = 1.0;
				if (input[indexInsertionCosts] < 0.0)
					input[indexInsertionCosts] = 0.0;
				otherIndex = (int) Math.floor(input[indexInsertionCosts] / (1.0 / 100.0));
			} else if (indexRemainingBudget > 0) {
				if (input[indexRemainingBudget] > 1.0)
					input[indexRemainingBudget] = 1.0;
				if (input[indexRemainingBudget] < 0.0)
					input[indexRemainingBudget] = 0.0;
				otherIndex = (int) Math.floor(input[indexRemainingBudget] / (1.0 / 100.0));
			} else {
				if (amountOverall > 1.0)
					amountOverall = 1.0;
				if (amountOverall < 0.0)
					amountOverall = 0.0;
				otherIndex = (int) Math.floor(amountOverall / (1.0 / 100.0));
			}
			if (otherIndex == 100)
				otherIndex = 100 - 1;
			if (lookupTableCount[timeIndex][otherIndex] != null) {
				valueLT = lookupTableSum[timeIndex][otherIndex] / lookupTableCount[timeIndex][otherIndex];
				value += valueLT * (1.0 - mesoWeightLf);
			} else {
				value = valueLf;
			}
		} else {
			// if (lookupArrayCount[timeIndex] != null) {
			// valueLT = lookupArraySum[timeIndex] /
			// lookupArrayCount[timeIndex];
			// value += valueLT * (1.0 - mesoWeightLf);
			// } else {
			value = valueLf;
			// }
		}

		if (t == 0)
			return 0;
		return value;
	}

	private void initGlobal() {
		// Maximum time frame and start time
		RoutingService.getPossibleDurationOfRouteByTimeWindowSet(
				AggregateReferenceInformationAlgorithm.getTimeWindowSet(),
				AggregateReferenceInformationAlgorithm.getTIME_MULTIPLIER());
		RoutingService.getDeliveryStartTimeByTimeWindowSet(AggregateReferenceInformationAlgorithm.getTimeWindowSet());
		DynamicRoutingHelperService.distanceMultiplierAsTheCrowFlies = AggregateReferenceInformationAlgorithm
				.getDistanceMultiplierAsTheCrowFlies();
		
		// Map time windows to alternatives
		this.alternativesToTimeWindows = new HashMap<TimeWindow, Alternative>();
		for (Alternative alt : this.alternativeSet.getElements()) {
			if (!alt.getNoPurchaseAlternative()) {
				alternativesToTimeWindows.put(alt.getTimeWindows().get(0), alt);
			}
		}

		// Separate into delivery areas for parallel computing
		this.orderRequestsPerDeliveryArea = CustomerDemandService.prepareOrderRequestsForDeliveryAreas(
				this.orderRequestSetsForLearning, AggregateReferenceInformationAlgorithm.getDeliveryAreaSet());
		this.vehicleAssignmentsPerDeliveryArea = new HashMap<Integer, ArrayList<VehicleAreaAssignment>>();
		this.vasPerDeliveryAreaSetAndVehicleNo = new HashMap<Integer, HashMap<Integer, VehicleAreaAssignment>>();
		this.overallCapacityPerDeliveryArea = new HashMap<Integer, Double>();
		DynamicRoutingHelperService.prepareVehicleAssignmentsForDeliveryAreas(vehicleAreaAssignmentSet,
				vehicleAssignmentsPerDeliveryArea, vasPerDeliveryAreaSetAndVehicleNo, overallCapacityPerDeliveryArea,
				getDeliveryAreaSet(), getTimeWindowSet(), AggregateReferenceInformationAlgorithm.getTIME_MULTIPLIER(),
				this.getExpectedServiceTime());

		// Init buffers overall
		this.alreadyAcceptedPerDeliveryArea = new HashMap<Integer, HashMap<Integer, Integer>>();
		this.remainingCapacityPerDeliveryArea = new HashMap<Integer, Double>();
		this.acceptedCostPerDeliveryArea = new HashMap<Integer, Double>();
		this.acceptedAmountPerDeliveryArea = new HashMap<Integer, Double>();
		this.distancePerDeliveryAreaAndRouting = new HashMap<Integer, HashMap<Integer, Double>>();
		this.distancePerDeliveryAreaAndTwAndRouting = new HashMap<Integer, HashMap<Integer, HashMap<Integer, Double>>>();
		this.observationsPerDeliveryArea = new HashMap<DeliveryArea, ArrayList<Pair<ArrayList<double[]>, Double[]>>>();
		this.linearFunctionCoefficientsPerDeliveryArea = new HashMap<DeliveryArea, double[]>();
		this.linearFunctionInterceptPerDeliveryArea = new HashMap<DeliveryArea, Double>();
		this.randomForestPerDeliveryArea = new HashMap<DeliveryArea, smile.regression.RandomForest>();
		this.lookupTableSumPerDeliveryArea = new HashMap<DeliveryArea, Double[][]>();
		this.lookupTableCountPerDeliveryArea = new HashMap<DeliveryArea, Integer[][]>();
		this.lookupArraySumPerDeliveryArea = new HashMap<DeliveryArea, Double[]>();
		this.lookupArrayCountPerDeliveryArea = new HashMap<DeliveryArea, Integer[]>();
		this.oldMomentumPerDeliveryArea = new HashMap<Integer, MomentumHelper>();
		this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow = CustomerDemandService
				.determineMaximumProbabilityPerDemandSegmentAndTimeWindow(this.demandSegmentWeighting,
						getTimeWindowSet());
		this.aggregateReferenceInformation(false, false);

		// Init models and lookup table
		this.initModels();

		MomentumHelper momentum = null;
		if (momentumWeight > 0) {
			for (DeliveryArea area : AggregateReferenceInformationAlgorithm.getDeliveryAreaSet().getElements()) {

				momentum = new MomentumHelper(true, this.linearFunctionCoefficientsPerDeliveryArea.get(area).length);
				oldMomentumPerDeliveryArea.put(area.getId(), momentum);
			}
		}
	}

	private void initModels() {

		for (DeliveryArea area : AggregateReferenceInformationAlgorithm.getDeliveryAreaSet().getElements()) {
			observationsPerDeliveryArea.put(area, new ArrayList<Pair<ArrayList<double[]>, Double[]>>());
			if (this.considerInsertionCosts || this.considerOverallRemainingCapacity
					|| this.considerOverallAcceptedTimeInteraction) {
				this.lookupTableSumPerDeliveryArea.put(area, new Double[100][100]);
				this.lookupTableCountPerDeliveryArea.put(area, new Integer[100][100]);
			} else {
				this.lookupArraySumPerDeliveryArea.put(area, new Double[100]);
				this.lookupArrayCountPerDeliveryArea.put(area, new Integer[100]);
			}

			// Always: number per time window, time
			numberOfAttributes = AggregateReferenceInformationAlgorithm.getTimeWindowSet().getElements().size() + 1;
			if (this.considerOverallRemainingCapacity)
				numberOfAttributes++;
			if (this.considerInsertionCosts) {
				numberOfAttributes++;
			}
			if (this.considerOverallAcceptedTimeInteraction) {
				numberOfAttributes += 2;
			}

			//
			this.maxValuePerAttribute = new double[numberOfAttributes];

			int indexRemainingTime = 0;
			maxValuePerAttribute[indexRemainingTime] = this.orderHorizonLength;

			this.timeWindowInputMapper = new HashMap<Integer, Integer>();
			int currentIndex = indexRemainingTime;
			for (TimeWindow tw : AggregateReferenceInformationAlgorithm.getTimeWindowSet().getElements()) {
				timeWindowInputMapper.put(tw.getId(), ++currentIndex);
				maxValuePerAttribute[currentIndex] = AggregateReferenceInformationAlgorithm.maxAcceptedPerTw.get(tw);
			}

			this.indexRemainingBudget = -1;
			if (this.considerOverallRemainingCapacity) {
				indexRemainingBudget = ++currentIndex;
				maxValuePerAttribute[indexRemainingBudget] = this.overallCapacityPerDeliveryArea.get(area.getId());
			}

			this.indexInsertionCosts = -1;
			if (this.considerInsertionCosts) {
				indexInsertionCosts = ++currentIndex;
				maxValuePerAttribute[indexInsertionCosts] = this.overallCapacityPerDeliveryArea.get(area.getId());
			}
			this.indexOverallAcceptedTimeInteraction = -1;
			this.indexOverallAccepted = -1;
			if (this.considerOverallAcceptedTimeInteraction) {
				indexOverallAcceptedTimeInteraction = ++currentIndex;
				indexOverallAccepted = ++currentIndex;
				maxValuePerAttribute[indexOverallAcceptedTimeInteraction] = this.orderHorizonLength
						* AggregateReferenceInformationAlgorithm.maximumAcceptableOverTw;
				maxValuePerAttribute[indexOverallAccepted] = AggregateReferenceInformationAlgorithm.maximumAcceptableOverTw;
			}

			HashMap<Routing, ArrayList<double[]>> observationsPerRouting = new HashMap<Routing, ArrayList<double[]>>();
			HashMap<Routing, ArrayList<Order>> ordersPerRouting = new HashMap<Routing, ArrayList<Order>>();

			for (Routing r :getPreviousRoutingResults()) {
				ArrayList<double[]> observations = new ArrayList<double[]>();
				ArrayList<Order> orders = new ArrayList<Order>();
				ArrayList<RouteElement> res = new ArrayList<RouteElement>();

				for (Route rou : r.getRoutes()) {
					res.addAll(rou.getRouteElements());
				}

				Collections.sort(res, new RouteElementArrivalTimeDescComparator());

				// One observation for overall order horizon
				// Add one observation for the overall order horizon
				double[] observationFirst = new double[numberOfAttributes];

				if (this.considerOverallRemainingCapacity) {

					observationFirst[indexRemainingBudget] = 1.0;

				}

				if (this.considerInsertionCosts) {

					observationFirst[indexInsertionCosts] = 0.0;

				}

				if (this.considerOverallAcceptedTimeInteraction) {

					observationFirst[this.indexOverallAcceptedTimeInteraction] = 0.0;
					observationFirst[this.indexOverallAccepted] = 0.0;

				}

				observationFirst[indexRemainingTime] = this.orderHorizonLength
						/ maxValuePerAttribute[indexRemainingTime];

				for (TimeWindow tw : AggregateReferenceInformationAlgorithm.getTimeWindowSet().getElements()) {

					observationFirst[timeWindowInputMapper.get(tw.getId())] = 0.0;

				}

				observations.add(observationFirst);

				int last = this.orderHorizonLength;

				for (RouteElement re : res) {
					orders.add(re.getOrder());

					for (int t = last - 1; t >= re.getOrder().getOrderRequest().getArrivalTime(); t--) {
						double[] observation = new double[numberOfAttributes];

						observation[indexRemainingTime] = (double) t / maxValuePerAttribute[indexRemainingTime];

						for (TimeWindow tw : AggregateReferenceInformationAlgorithm.getTimeWindowSet().getElements()) {
							if (t == this.orderHorizonLength - 1) {
								observation[timeWindowInputMapper.get(tw.getId())] = 0;
							} else {
								observation[timeWindowInputMapper.get(tw.getId())] = observations
										.get(observations.size() - 1)[timeWindowInputMapper.get(tw.getId())];
							}
							;
						}

						if (this.considerOverallRemainingCapacity) {
							if (t == this.orderHorizonLength - 1) {
								observation[indexRemainingBudget] = 1.0;
							} else {
								observation[indexRemainingBudget] = observations
										.get(observations.size() - 1)[indexRemainingBudget];
							}
						}

						if (this.considerOverallAcceptedTimeInteraction) {
							if (t == this.orderHorizonLength - 1) {
								observation[indexOverallAcceptedTimeInteraction] = 0.0;
								observation[indexOverallAccepted] = 0.0;

							} else {
								observation[indexOverallAcceptedTimeInteraction] = observations
										.get(observations.size() - 1)[indexOverallAcceptedTimeInteraction];
								observation[indexOverallAccepted] = observations
										.get(observations.size() - 1)[indexOverallAccepted];
							}

						}

						if (this.considerInsertionCosts) {
							if (t == this.orderHorizonLength - 1) {
								observation[indexInsertionCosts] = 0.0;
							} else {
								observation[indexInsertionCosts] = observations
										.get(observations.size() - 1)[indexInsertionCosts];
							}
						}

						observations.add(observation);
					}

					double[] observation = new double[numberOfAttributes];

					observation[indexRemainingTime] = (re.getOrder().getOrderRequest().getArrivalTime() - 1.0)
							/ maxValuePerAttribute[indexRemainingTime];

					for (TimeWindow tw : AggregateReferenceInformationAlgorithm.getTimeWindowSet().getElements()) {

						double lastValue;
						if (re.getOrder().getOrderRequest().getArrivalTime() == this.orderHorizonLength) {
							lastValue = 0;
						} else {
							lastValue = observations.get(observations.size() - 1)[timeWindowInputMapper
									.get(tw.getId())];
						}

						if (re.getOrder().getTimeWindowFinalId() == tw.getId())
							lastValue = lastValue
									+ 1.0 / this.maxValuePerAttribute[timeWindowInputMapper.get(tw.getId())];
						observation[timeWindowInputMapper.get(tw.getId())] = lastValue;
					}

					if (this.considerOverallRemainingCapacity) {
						double lastValue;
						if (re.getOrder().getOrderRequest().getArrivalTime() == this.orderHorizonLength) {
							lastValue = 1.0;
						} else {
							lastValue = observations.get(observations.size() - 1)[indexRemainingBudget];
						}
						observation[indexRemainingBudget] = lastValue
								- re.getTempAdditionalCostsValue() / maxValuePerAttribute[indexRemainingBudget];
					}

					if (this.considerOverallAcceptedTimeInteraction) {
						observation[indexOverallAcceptedTimeInteraction] = (orders.size()
								* (this.orderHorizonLength - (re.getOrder().getOrderRequest().getArrivalTime() - 1.0)))
								/ maxValuePerAttribute[indexOverallAcceptedTimeInteraction];
						observation[indexOverallAccepted] = (orders.size())
								/ maxValuePerAttribute[indexOverallAccepted];

					}

					if (this.considerInsertionCosts) {
						double lastValue;
						if (re.getOrder().getOrderRequest().getArrivalTime() == this.orderHorizonLength) {
							lastValue = 0.0;
						} else {
							lastValue = observations.get(observations.size() - 1)[indexInsertionCosts];
						}
						observation[indexInsertionCosts] = lastValue
								+ re.getTempAdditionalCostsValue() / maxValuePerAttribute[indexInsertionCosts];
			//			AggregateReferenceInformationAlgorithm.aggregatedReferenceInformationCosts.get(re.getOrder().getOrderRequest().getCustomer().getTempDeliveryArea()).get(re.getOrder().getTimeWindowFinal());
					}

					observations.add(observation);

					last = re.getOrder().getOrderRequest().getArrivalTime() - 1;
				}

				if (last > 0) {
					for (int t = last - 1; t >= 0; t--) {
						double[] observation = new double[numberOfAttributes];

						observation[indexRemainingTime] = (double) t / maxValuePerAttribute[indexRemainingTime];

						for (TimeWindow tw : AggregateReferenceInformationAlgorithm.getTimeWindowSet().getElements()) {

							observation[timeWindowInputMapper.get(tw.getId())] = observations
									.get(observations.size() - 1)[timeWindowInputMapper.get(tw.getId())];

						}

						if (this.considerOverallRemainingCapacity) {

							observation[indexRemainingBudget] = observations
									.get(observations.size() - 1)[indexRemainingBudget];

						}
						if (this.considerInsertionCosts) {

							observation[indexInsertionCosts] = observations
									.get(observations.size() - 1)[indexInsertionCosts];

						}

						if (this.considerOverallAcceptedTimeInteraction) {
							observation[this.indexOverallAcceptedTimeInteraction] = observations
									.get(observations.size() - 1)[indexOverallAcceptedTimeInteraction];
							observation[this.indexOverallAccepted] = observations
									.get(observations.size() - 1)[indexOverallAccepted];

						}

						observations.add(observation);
					}
				}

				observationsPerRouting.put(r, observations);
				ordersPerRouting.put(r, orders);

			}
			// Prepare output values and add to observations list
			for (Routing r : observationsPerRouting.keySet()) {
				Pair<ArrayList<double[]>, Double[]> ob = new Pair<ArrayList<double[]>, Double[]>(
						observationsPerRouting.get(r), prepareOutputValues(ordersPerRouting.get(r), true));
				this.observationsPerDeliveryArea.get(area).add(ob);
				// Update lookup table
				this.updateLookupTable(area, ob.getKey(), ob.getValue());
			}

			// Update linear function
			this.updateLinearFunction(area, true);
		}

	}

	private void updateLinearFunction(DeliveryArea area, boolean init) {

		while (this.observationsPerDeliveryArea.get(area).size() > numberOfReleventObservationsForLearning) {
			this.observationsPerDeliveryArea.get(area).remove(0);
		}

		if (false) {
			// Build input data
			int numberOfRelevantAttributes = this.maxValuePerAttribute.length;
			if (this.considerOverallAcceptedTimeInteraction) {
				numberOfRelevantAttributes--;
			}

			if (this.considerInsertionCosts || this.considerOverallRemainingCapacity) {
				numberOfRelevantAttributes--;
			}
			double[][] x = new double[this.observationsPerDeliveryArea.get(area).size()
					* (this.orderHorizonLength + 1)][numberOfRelevantAttributes];
			double[] y = new double[this.observationsPerDeliveryArea.get(area).size() * (this.orderHorizonLength + 1)];
			int currentIndex = 0;
			int twForNorm = this.timeWindowInputMapper.keySet().iterator().next();
			for (Pair<ArrayList<double[]>, Double[]> r : this.observationsPerDeliveryArea.get(area)) {
				int currentIndexInside = 0;
				for (double[] obs : r.getKey()) {
					x[currentIndex][0] = obs[0];

					// Time windows: use first as reference in case of overall
					// capacity -> normalisation

					for (Integer tw : this.timeWindowInputMapper.keySet()) {

						if (!(this.considerInsertionCosts || this.considerOverallRemainingCapacity)) {
							x[currentIndex][this.timeWindowInputMapper.get(tw)] = obs[this.timeWindowInputMapper
									.get(tw)];
						} else if (this.timeWindowInputMapper.get(tw) > this.timeWindowInputMapper.get(twForNorm)) {
							x[currentIndex][this.timeWindowInputMapper.get(tw) - 1] = obs[this.timeWindowInputMapper
									.get(tw)];
						} else if (tw != twForNorm) {
							x[currentIndex][this.timeWindowInputMapper.get(tw)] = obs[this.timeWindowInputMapper
									.get(tw)];
						}
					}
					if (this.considerInsertionCosts) {
						x[currentIndex][this.indexInsertionCosts - 1] = obs[this.indexInsertionCosts];
					}
					if (this.considerOverallRemainingCapacity) {
						x[currentIndex][this.indexRemainingBudget - 1] = obs[this.indexRemainingBudget];
					}

					if (this.considerOverallAcceptedTimeInteraction) {
						if (!(this.considerInsertionCosts || this.considerOverallRemainingCapacity)) {
							x[currentIndex][this.indexOverallAcceptedTimeInteraction] = obs[this.indexOverallAcceptedTimeInteraction];
						} else {
							x[currentIndex][this.indexOverallAcceptedTimeInteraction
									- 1] = obs[this.indexOverallAcceptedTimeInteraction];
						}
					}
					y[currentIndex] = r.getValue()[currentIndexInside++];
					currentIndex++;
				}

			}

			if (init) {
				OLS olsR = new OLS(x, y);
				if (this.considerInsertionCosts || this.considerOverallRemainingCapacity) {
					numberOfRelevantAttributes++;
					double[] coeff = new double[numberOfRelevantAttributes];
					for (int i = 0; i < olsR.coefficients().length; i++) {
						if (i < this.timeWindowInputMapper.get(twForNorm)) {
							coeff[i] = olsR.coefficients()[i];
						} else if (i == this.timeWindowInputMapper.get(twForNorm)) {
							coeff[i] = 0;
							coeff[i + 1] = olsR.coefficients()[i];
						} else {
							coeff[i + 1] = olsR.coefficients()[i];
						}
					}
					this.linearFunctionCoefficientsPerDeliveryArea.put(area, coeff);
				} else {
					this.linearFunctionCoefficientsPerDeliveryArea.put(area, olsR.coefficients());
				}

				this.linearFunctionInterceptPerDeliveryArea.put(area, olsR.intercept());
			}

			smile.regression.RandomForest rfR = new smile.regression.RandomForest(x, y, 10);
			this.randomForestPerDeliveryArea.put(area, rfR);
		} else {

			Rengine re = SettingsProvider.getRe();

			int setId = 1;
			ArrayList<Integer> setIds = new ArrayList<Integer>();
			ArrayList<Double> times = new ArrayList<Double>();
			ArrayList<Double> costs = new ArrayList<Double>();
			ArrayList<Double> budgets = new ArrayList<Double>();
			ArrayList<Double> timeInteractions = new ArrayList<Double>();
			ArrayList<Double> values = new ArrayList<Double>();
			HashMap<Integer, ArrayList<Double>> valuesPerTw = new HashMap<Integer, ArrayList<Double>>();
			for (Pair<ArrayList<double[]>, Double[]> r : this.observationsPerDeliveryArea.get(area)) {
				int currentIndexInside = 0;
				for (double[] obs : r.getKey()) {
					setIds.add(setId);
					times.add(obs[0]);
					if (this.considerInsertionCosts) {
						costs.add(obs[this.indexInsertionCosts]);
					}
					if (this.considerOverallRemainingCapacity) {
						budgets.add(obs[this.indexRemainingBudget]);
					}
					if (this.considerOverallAcceptedTimeInteraction) {
						timeInteractions.add(obs[this.indexOverallAcceptedTimeInteraction]);
					}
					for (Integer tw : this.timeWindowInputMapper.keySet()) {
						if (!valuesPerTw.containsKey(tw))
							valuesPerTw.put(tw, new ArrayList<Double>());
						valuesPerTw.get(tw).add(obs[this.timeWindowInputMapper.get(tw)]);
					}
					values.add(r.getValue()[currentIndexInside++]);
				}
				setId++;
			}

			String setIdsString = this.toRVectorStringInteger(setIds);
			re.eval("setIds=" + setIdsString);
			String timesString = this.toRVectorStringDouble(times);
			re.eval("times=" + timesString);
			re.eval("inputData = cbind(setIds, times)");

			if (considerInsertionCosts) {
				String costsString = this.toRVectorStringDouble(costs);
				re.eval("costs=" + costsString);
				re.eval("inputData = cbind(inputData, costs)");
			}

			if (this.considerOverallRemainingCapacity) {
				String budgetString = this.toRVectorStringDouble(budgets);
				re.eval("budgets=" + budgetString);
				re.eval("inputData = cbind(inputData, budgets)");
			}

			if (this.considerOverallAcceptedTimeInteraction) {
				String tcInteractionString = this.toRVectorStringDouble(timeInteractions);
				re.eval("tcInteraction=" + tcInteractionString);
				re.eval("inputData = cbind(inputData, tcInteraction)");
			}

			for (Integer tw : this.timeWindowInputMapper.keySet()) {
				String twNameString = "tw" + tw;
				String twDataString = this.toRVectorStringDouble(valuesPerTw.get(tw));
				re.eval(twNameString + "=" + twDataString);
				re.eval("inputData =cbind(inputData," + twNameString + ")");
			}

			String valuesString = this.toRVectorStringDouble(values);
			re.eval("futureValues=" + valuesString);
			re.eval("inputData = cbind(inputData,futureValues)");

			// re.eval("library(nlme)");
			re.eval("library(rms)");
			re.eval("inputData=data.frame(inputData)");

			String toEvalutePlm = "result = Gls(futureValues~times";
			if (considerInsertionCosts)
				toEvalutePlm = toEvalutePlm + "+costs";
			if (this.considerOverallRemainingCapacity)
				toEvalutePlm = toEvalutePlm + "+budgets";
			if (considerOverallAcceptedTimeInteraction)
				toEvalutePlm = toEvalutePlm + "+tcInteraction";
			for (Integer tw : this.timeWindowInputMapper.keySet()) {
				toEvalutePlm = toEvalutePlm + "+tw" + tw;
			}
			// Method can also be ML
			re.eval(toEvalutePlm + ", data = inputData, method =\"REML\")");

			this.linearFunctionInterceptPerDeliveryArea.put(area, re.eval("result$coefficients[1]").asDouble());

			int numberOfRelevantAttributes = this.maxValuePerAttribute.length;
			if (this.considerOverallAcceptedTimeInteraction) {
				numberOfRelevantAttributes--;
			}
			double[] coeff = new double[numberOfRelevantAttributes];
			coeff[0] = re.eval("result$coefficients[\"times\"]").asDouble();
			if (considerInsertionCosts) {
				coeff[this.indexInsertionCosts] = re.eval("result$coefficients[\"costs\"]").asDouble();
			}

			if (this.considerOverallRemainingCapacity) {
				coeff[this.indexRemainingBudget] = re.eval("result$coefficients[\"budgets\"]").asDouble();
			}

			if (this.considerOverallAcceptedTimeInteraction) {
				coeff[this.indexOverallAcceptedTimeInteraction] = re.eval("result$coefficients[\"tcInteraction\"]")
						.asDouble();
			}

			for (Integer tw : this.timeWindowInputMapper.keySet()) {
				coeff[this.timeWindowInputMapper.get(tw)] = re.eval("result$coefficients[\"tw" + tw + "\"]").asDouble();
			}

			re.end();
			this.linearFunctionCoefficientsPerDeliveryArea.put(area, coeff);

		}

	}

	private void updateLookupTable(DeliveryArea area, ArrayList<double[]> observations, Double[] values) {

		for (int i = 0; i < observations.size(); i++) {

			int timeIndex = (int) Math
					.floor((double) Math.round(observations.get(i)[0] / (1.0 / 100.0) * 10000d) / 10000d);
			if (timeIndex == 100)
				timeIndex = 100 - 1;

			if (this.considerInsertionCosts || this.considerOverallRemainingCapacity
					|| this.considerOverallAcceptedTimeInteraction) {
				int otherIndex;
				if (this.considerInsertionCosts) {
					if (observations.get(i)[this.indexInsertionCosts] > 1.0)
						observations.get(i)[this.indexInsertionCosts] = 1.0;
					if (observations.get(i)[this.indexInsertionCosts] < 0.0)
						observations.get(i)[this.indexInsertionCosts] = 0.0;
					otherIndex = (int) Math.floor(observations.get(i)[this.indexInsertionCosts] / (1.0 / 100.0));
				} else if (this.considerOverallRemainingCapacity) {
					if (observations.get(i)[this.indexRemainingBudget] > 1.0)
						observations.get(i)[this.indexRemainingBudget] = 1.0;
					if (observations.get(i)[this.indexRemainingBudget] < 0.0)
						observations.get(i)[this.indexRemainingBudget] = 0.0;
					otherIndex = (int) Math.floor(observations.get(i)[this.indexRemainingBudget] / (1.0 / 100.0));
				} else {
					if (observations.get(i)[indexOverallAccepted] > 1.0)
						observations.get(i)[indexOverallAccepted] = 1.0;
					if (observations.get(i)[indexOverallAccepted] < 0.0)
						observations.get(i)[indexOverallAccepted] = 0.0;
					otherIndex = (int) Math.floor(observations.get(i)[indexOverallAccepted] / (1.0 / 100.0));
				}
				if (otherIndex == 100)
					otherIndex = 100 - 1;
				if (this.lookupTableCountPerDeliveryArea.get(area)[timeIndex][otherIndex] != null) {
					this.lookupTableCountPerDeliveryArea
							.get(area)[timeIndex][otherIndex] = this.lookupTableCountPerDeliveryArea
									.get(area)[timeIndex][otherIndex] + 1;
					this.lookupTableSumPerDeliveryArea
							.get(area)[timeIndex][otherIndex] = this.lookupTableSumPerDeliveryArea
									.get(area)[timeIndex][otherIndex] + values[i];
				} else {
					this.lookupTableCountPerDeliveryArea.get(area)[timeIndex][otherIndex] = 1;
					this.lookupTableSumPerDeliveryArea.get(area)[timeIndex][otherIndex] = values[i];
				}
			} else {
				if (this.lookupArrayCountPerDeliveryArea.get(area)[timeIndex] != null) {
					this.lookupArrayCountPerDeliveryArea
							.get(area)[timeIndex] = this.lookupArrayCountPerDeliveryArea.get(area)[timeIndex] + 1;
					this.lookupArraySumPerDeliveryArea
							.get(area)[timeIndex] = this.lookupArraySumPerDeliveryArea.get(area)[timeIndex] + values[i];
				} else {
					this.lookupArrayCountPerDeliveryArea.get(area)[timeIndex] = 1;
					this.lookupArraySumPerDeliveryArea.get(area)[timeIndex] = values[i];
				}
			}

		}

	}

	private Double[] prepareOutputValues(ArrayList<Order> orders, boolean init) {
		Double[] outputs = new Double[orderHorizonLength + 1];

		// Determine backward details
		Collections.sort(orders, new OrderArrivalTimeAscComparator());

		double value = 0.0;
		double additionalInitValue = 0.0;

		int currentT = 0;
		for (int a = currentT; a < orders.get(0).getOrderRequest().getArrivalTime(); a++) {
			outputs[orderHorizonLength - a] = value;
		}
		currentT = orders.get(0).getOrderRequest().getArrivalTime();

		int targetT = this.orderHorizonLength;

		for (int oId = 0; oId < orders.size(); oId++) {
			Order order = orders.get(oId);
			if (oId < orders.size() - 1) {
				targetT = orders.get(oId + 1).getOrderRequest().getArrivalTime() - 1;
			} else {
				targetT = this.orderHorizonLength;
			}

			double addValue;
			if (this.useActualBasketValue && !init) {
				addValue = CustomerDemandService.calculateOrderValue(order.getOrderRequest(),
						AggregateReferenceInformationAlgorithm.getMaximumRevenueValue(),
						AggregateReferenceInformationAlgorithm.getObjectiveSpecificValues());
			} else {
				addValue = CustomerDemandService.calculateMedianValue(
						AggregateReferenceInformationAlgorithm.getMaximumRevenueValue(),
						AggregateReferenceInformationAlgorithm.getObjectiveSpecificValues(),
						order.getOrderRequest().getCustomer().getOriginalDemandSegment());
			}

			if (init) {
				// if(tdUpdate){
				if (this.considerOverallAcceptedTimeInteraction) {
					int currentRelevantPow = (int) (orderHorizonLength / 2.0 - currentT);
					double additionalValueInteraction = Math.min(1.0,
							addValue * this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow
									.get(order.getOrderRequest().getCustomer().getOriginalDemandSegmentId())
									.get(order.getTimeWindowFinalId())
									* Math.pow((1.0 - 1.0 / (this.orderHorizonLength*10.0)), currentRelevantPow));
					value = value + additionalValueInteraction;
					additionalInitValue = additionalInitValue + (addValue//*this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow
							//.get(order.getOrderRequest().getCustomer().getOriginalDemandSegmentId())
							//.get(order.getTimeWindowFinalId()) 
							- additionalValueInteraction)
							/ AggregateReferenceInformationAlgorithm.maximumValueAcceptable;

				} else {
					value = value + addValue * this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow
							.get(order.getOrderRequest().getCustomer().getOriginalDemandSegmentId())
							.get(order.getTimeWindowFinalId());
					additionalInitValue = additionalInitValue
							+ (addValue - addValue * this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow
									.get(order.getOrderRequest().getCustomer().getOriginalDemandSegmentId())
									.get(order.getTimeWindowFinalId()))
									/ AggregateReferenceInformationAlgorithm.maximumValueAcceptable;
				}

				// }else{
				// value = value + (addValue *
				// this.maximumExpectedMultiplierPerDemandSegmentAndTimeWindow
				// .get(order.getOrderRequest().getCustomer().getOriginalDemandSegmentId())
				// .get(order.getTimeWindowFinalId()) + addValue) / 2.0;
				// }

			} else {
				value = value + addValue;
			}

			while (currentT <= targetT) {


				// AggregateReferenceInformationAlgorithm.maximumValueAcceptable;
				// } else {
				outputs[orderHorizonLength - currentT] = value
						/ AggregateReferenceInformationAlgorithm.maximumValueAcceptable;
				// }

				// * Math.pow(this.discountingFactorProbability,
				// this.orderHorizonLength - currentT));
				currentT++; // at end, currentT equals arrival time of next
							// (targetT + 1)
			}

		}

		// Fill up till end of order horizon
		for (int t = currentT; t <= targetT; t++) {

			outputs[orderHorizonLength - currentT] = value
					/ AggregateReferenceInformationAlgorithm.maximumValueAcceptable;


		}

		if (init) {
			for (int t = 0; t < orderHorizonLength + 1; t++) {
				outputs[orderHorizonLength - t] = outputs[orderHorizonLength - t]
						+ additionalInitValue / ((double) orderHorizonLength) * ((double) t);
			}
		}
		return outputs;
	}

	private void initialiseRemainingCapacity(DeliveryArea area) {
		this.remainingCapacityPerDeliveryArea.put(area.getId(), this.overallCapacityPerDeliveryArea.get(area.getId()));
	}

	private void initialiseOverallAcceptedAmount(DeliveryArea area) {
		this.acceptedAmountPerDeliveryArea.put(area.getId(), 0.0);
	}

	private void initialiseAcceptedCostOverall(DeliveryArea area) {
		this.acceptedCostPerDeliveryArea.put(area.getId(), 0.0);
	}

	private void initialiseAlreadyAcceptedPerTimeWindow(DeliveryArea area) {
		HashMap<Integer, Integer> acceptedPerTw = new HashMap<Integer, Integer>();
		for (TimeWindow tw : AggregateReferenceInformationAlgorithm.getTimeWindowSet().getElements()) {
			acceptedPerTw.put(tw.getId(), 0);
		}
		this.alreadyAcceptedPerDeliveryArea.put(area.getId(), acceptedPerTw);
	}


	public ValueFunctionApproximationModelSet getResult() {

		return result;
	}

	public static String[] getParameterSetting() {
		return paras;
	}

	private String toRVectorStringDouble(ArrayList<Double> valueList) {

		StringBuilder buffer = new StringBuilder();
		buffer.append("c(");
		for (int i = 0; i < valueList.size() - 1; i++) {
			buffer.append(valueList.get(i) + ",");
		}

		buffer.append(valueList.get(valueList.size() - 1) + ")");

		return buffer.toString();
	}

	private String toRVectorStringInteger(ArrayList<Integer> valueList) {

		StringBuilder buffer = new StringBuilder();
		buffer.append("c(");
		for (int i = 0; i < valueList.size() - 1; i++) {
			buffer.append(valueList.get(i) + ",");
		}

		buffer.append(valueList.get(valueList.size() - 1) + ")");

		return buffer.toString();
	}

}
