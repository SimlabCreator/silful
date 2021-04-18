package logic.service.support;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.math3.util.Pair;

import data.entity.DeliveryArea;
import data.entity.TimeWindow;
import data.entity.ValueFunctionApproximationCoefficient;
import data.entity.ValueFunctionApproximationModel;
import logic.entity.MomentumHelper;
import logic.entity.ValueFunctionCoefficientType;
import logic.utility.MultiDimensionalArrayProducer;

public class ValueFunctionApproximationService {

	private static boolean useNegativeSigns = false;
	private static int LOGGING_FREQUENCE = 50;

	public static double evaluateStateForTabularValueFunctionApproximation(Object[] timeCoefficients,
			int[] currentState, int time, int orderHorizonLength) {
		double value = 0.0;

		if (time > 0) {
			value = MultiDimensionalArrayProducer.readDoubleArray(timeCoefficients, currentState);
		}

		return value;

	}

	/**
	 * Returns the value of the value function approximation for a linear model
	 * with already accepted per tw as input, end of order horizon at 0 and end
	 * of order horizon value of 0
	 * 
	 * @param alreadyAcceptedPerTimeWindow
	 * @param basicCoefficient
	 * @param variableCoefficients
	 * @return
	 */
	public static double evaluateStateForLinearValueFunctionApproximation_old(
			HashMap<Integer, Integer> alreadyAcceptedPerTimeWindow,
			HashMap<Integer, Double> acceptedInsertionCostsPerTimeWindow, HashMap<Integer, Double> coveredPerTimeWindow,
			int time, double basicCoefficient, ArrayList<ValueFunctionApproximationCoefficient> variableCoefficients,
			double timeCoefficient, int orderHorizonLength, int maximalAcceptablePerTimeWindow,
			Double timeCapacityInteractionCoefficient, double timeAndVehicleMultiplier, int numberOfTimeWindows,
			HashMap<Integer, Double> maximalCoveredPerTimeWindow) {

		double value = 0.0;

		// If time is after end of order horizon, the value is 0
		if (time > 0) {
			value += basicCoefficient;
			value += time / ((double) orderHorizonLength) * timeCoefficient;

			int overallAlreadyAccepted = 0;
			double overallAcceptedInsertionCosts = 0.0;
			double overallAcceptableInsertionCosts = 0.0;
			boolean insertionCostsNotNumber = false;
			for (ValueFunctionApproximationCoefficient c : variableCoefficients) {

				if (!c.isCosts() && !c.isCoverage()) {
					double alreadyAcceptedMultiplier = ((double) alreadyAcceptedPerTimeWindow.get(c.getTimeWindowId()))
							/ ((double) maximalAcceptablePerTimeWindow);
					if (c.isSquared())
						alreadyAcceptedMultiplier = alreadyAcceptedMultiplier * alreadyAcceptedMultiplier;
					if (useNegativeSigns) {
						value += (0.0 - 1.0) * c.getCoefficient() * alreadyAcceptedMultiplier;
					} else {
						value += c.getCoefficient() * alreadyAcceptedMultiplier;
					}

					if (!c.isSquared())
						overallAlreadyAccepted += alreadyAcceptedPerTimeWindow.get(c.getTimeWindowId());
				} else if (c.isCosts()) {
					insertionCostsNotNumber = true;
					double alreadyAcceptedMultiplier = acceptedInsertionCostsPerTimeWindow.get(c.getTimeWindowId())
							/ ((c.getTimeWindow().getEndTime() - c.getTimeWindow().getStartTime())
									* timeAndVehicleMultiplier);
					if (c.isSquared())
						alreadyAcceptedMultiplier = alreadyAcceptedMultiplier * alreadyAcceptedMultiplier;
					if (useNegativeSigns) {

						value += (0.0 - 1.0) * c.getCoefficient() * alreadyAcceptedMultiplier;
					} else {

						value += c.getCoefficient() * alreadyAcceptedMultiplier;
					}
					if (!c.isSquared()) {
						overallAcceptedInsertionCosts += acceptedInsertionCostsPerTimeWindow.get(c.getTimeWindowId());
						overallAcceptableInsertionCosts += ((c.getTimeWindow().getEndTime()
								- c.getTimeWindow().getStartTime()) * timeAndVehicleMultiplier);
					}

				} else if (c.isCoverage()) {
					value += c.getCoefficient() * coveredPerTimeWindow.get(c.getTimeWindowId())
							/ maximalCoveredPerTimeWindow.get(c.getTimeWindowId()) * ((double) time)
							/ ((double) orderHorizonLength);
				}
				// value += c.getCoefficient() * alreadyAcceptedMultiplier;
			}

			if (timeCapacityInteractionCoefficient != null && !insertionCostsNotNumber) {

				if (useNegativeSigns) {
					value += (0.0 - 1.0) * timeCapacityInteractionCoefficient * ((double) overallAlreadyAccepted)
							/ (((double) maximalAcceptablePerTimeWindow) * numberOfTimeWindows) * ((double) time)
							/ ((double) orderHorizonLength);
				} else {
					value += timeCapacityInteractionCoefficient * ((double) overallAlreadyAccepted)
							/ (((double) maximalAcceptablePerTimeWindow) * numberOfTimeWindows) * ((double) time)
							/ ((double) orderHorizonLength);
				}
				// value += timeCapacityInteractionCoefficient * ((double)
				// overallAlreadyAccepted)
				// / (((double) maximalAcceptablePerTimeWindow) *
				// variableCoefficients.size()) * ((double) time)
				// / ((double) orderHorizonLength);
			} else if (timeCapacityInteractionCoefficient != null) {
				if (useNegativeSigns) {
					value += (0.0 - 1.0) * timeCapacityInteractionCoefficient * overallAcceptedInsertionCosts
							/ overallAcceptableInsertionCosts * ((double) time) / ((double) orderHorizonLength);
				} else {
					value += timeCapacityInteractionCoefficient * overallAcceptedInsertionCosts
							/ overallAcceptableInsertionCosts * ((double) time) / ((double) orderHorizonLength);
				}
			}
		}

		return value;
	}

	public static double evaluateStateForLinearValueFunctionApproximation(int time,
			HashMap<Integer, Integer> alreadyAcceptedPerTimeWindow,
			HashMap<Integer, Double> acceptedInsertionCostsPerTimeWindow, HashMap<Integer, Double> coveredPerTimeWindow,
			Double remainingCapacity, Double acceptedCostOverall, Double areaPotential, double basicCoefficient, double timeCoefficient,
			ArrayList<ValueFunctionApproximationCoefficient> variableCoefficients,
			Double timeCapacityInteractionCoefficient, Double remainingCapacityCoefficient, Double acceptedCostOverallCoefficient,
			Double areaPotentialCoefficient, int orderHorizonLength, HashMap<TimeWindow, Integer> maxAcceptedPerTw,int overallAcceptable,
			HashMap<Integer, Double> maximalCoveredPerTimeWindow, Double maximumRemainingCapacity, Double maximumAcceptedCost,
			double maximumAreaPotential, double timeAndVehicleMultiplier, int numberOfTimeWindows) {

		double value = 0.0;

		// If time is after end of order horizon, the value is 0
		if (time > 0) {

			// Basic
			value += basicCoefficient;

			// Remaining time
			value += time / ((double) orderHorizonLength) * timeCoefficient;

			// Time window dependent coefficients
			int overallAlreadyAccepted = 0;
			double overallAcceptedInsertionCosts = 0.0;
			double overallAcceptableInsertionCosts = 0.0;
			boolean insertionCostsNotNumber = false;
			for (ValueFunctionApproximationCoefficient c : variableCoefficients) {

				if (!c.isCosts() && !c.isCoverage()) {
					double alreadyAcceptedMultiplier = ((double) alreadyAcceptedPerTimeWindow.get(c.getTimeWindowId()))
							/ ((double) maxAcceptedPerTw.get(c.getTimeWindow()));
					if (c.isSquared())
						alreadyAcceptedMultiplier = alreadyAcceptedMultiplier * alreadyAcceptedMultiplier;

					value += c.getCoefficient() * alreadyAcceptedMultiplier;

					if (!c.isSquared())
						overallAlreadyAccepted += alreadyAcceptedPerTimeWindow.get(c.getTimeWindowId());
				} else if (c.isCosts()) {
					insertionCostsNotNumber = true;
					double alreadyAcceptedMultiplier = acceptedInsertionCostsPerTimeWindow.get(c.getTimeWindowId())
							/ ((c.getTimeWindow().getEndTime() - c.getTimeWindow().getStartTime())
									* timeAndVehicleMultiplier);
					if (c.isSquared())
						alreadyAcceptedMultiplier = alreadyAcceptedMultiplier * alreadyAcceptedMultiplier;

					value += c.getCoefficient() * alreadyAcceptedMultiplier;

					if (!c.isSquared()) {
						overallAcceptedInsertionCosts += acceptedInsertionCostsPerTimeWindow.get(c.getTimeWindowId());
						overallAcceptableInsertionCosts += ((c.getTimeWindow().getEndTime()
								- c.getTimeWindow().getStartTime()) * timeAndVehicleMultiplier);
					}

				} else if (c.isCoverage()) {
					value += c.getCoefficient() * coveredPerTimeWindow.get(c.getTimeWindowId())
							/ maximalCoveredPerTimeWindow.get(c.getTimeWindowId()) * ((double) time)
							/ ((double) orderHorizonLength);
				}

			}
			

			// Time and capacity interaction
			if (timeCapacityInteractionCoefficient != null && !insertionCostsNotNumber) {
				value += timeCapacityInteractionCoefficient
						* (((double) overallAlreadyAccepted)
								/ ((double) overallAcceptable))
						* (((double) time) / ((double) orderHorizonLength));
			} else if (timeCapacityInteractionCoefficient != null) {
				value += timeCapacityInteractionCoefficient
						* (overallAcceptedInsertionCosts / overallAcceptableInsertionCosts)
						* (((double) time) / ((double) orderHorizonLength));

			}

			// Remaining capacity
			if (remainingCapacityCoefficient != null) {
				value += remainingCapacityCoefficient * remainingCapacity / maximumRemainingCapacity;
			}
			
			// Accepted cost overall
						if (acceptedCostOverallCoefficient != null) {
							value += acceptedCostOverallCoefficient * acceptedCostOverall / maximumAcceptedCost;
						}

			// Area potential
			if (areaPotentialCoefficient != null) {
				value += areaPotentialCoefficient * areaPotential / maximumAreaPotential * time
						/ ((double) orderHorizonLength);
			}
		}

		return value;
	}

	public static double evaluateStateForLinearValueFunctionApproximationWithOrienteering(int time,
			HashMap<Integer, Integer> alreadyAcceptedPerTimeWindow,
			HashMap<Integer, Integer> remainingCapacityPerTimeWindow, Double acceptedInsertionCosts,
			HashMap<Integer, Double> acceptedInsertionCostsPerTimeWindow, Double areaPotential,
			HashMap<Integer, Double> currentDemandCapacityRatio, double basicCoefficient, double timeCoefficient,
			ArrayList<ValueFunctionApproximationCoefficient> variableCoefficients, Double acceptedCostsCoefficient,
			Double areaPotentialCoefficient, Double timeCapacityInteractionCoefficient, int orderHorizonLength,
			HashMap<Integer, Integer> maximalAcceptablePerTimeWindow,
			HashMap<Integer, Integer> maximalRemainingCapacityPerTimeWindow, double maximumAcceptedCosts,
			double maximumAreaPotential, HashMap<Integer, Double> maximumDemandCapacityRatio,
			double timeAndVehicleMultiplier, int numberOfTimeWindows, boolean considerRemainingCap, double endValue) {

		double value = 0.0;

		// If time is after end of order horizon, the value is 0 or penalty
		if(time==0){
			value=endValue;
		}else if (time > 0) {

			// If we consider remaining capacity and it is 0, the value is 0
			int remainingCap = 0;
			if (considerRemainingCap) {
				for (Integer twId : remainingCapacityPerTimeWindow.keySet()) {
					remainingCap += remainingCapacityPerTimeWindow.get(twId);
				}
			}

			if ((considerRemainingCap && remainingCap > 0)
					|| remainingCapacityPerTimeWindow == null) {
				// Basic
				value += basicCoefficient;

				// Remaining time
				value += (double) time / ((double) orderHorizonLength) * timeCoefficient;

				// Time window dependent coefficients
				int overallAlreadyAccepted = 0;
				double overallAcceptedInsertionCosts = 0.0;
				int overallAcceptableNo = 0;
				double overallAcceptableInsertionCosts = 0.0;
				boolean insertionCostsNotNumber = false;
				for (ValueFunctionApproximationCoefficient c : variableCoefficients) {

					if (c.getType() == ValueFunctionCoefficientType.NUMBER) {
						double alreadyAcceptedMultiplier = ((double) alreadyAcceptedPerTimeWindow
								.get(c.getTimeWindowId()))
								/ ((double) maximalAcceptablePerTimeWindow.get(c.getTimeWindowId()));
						if (c.isSquared())
							alreadyAcceptedMultiplier = alreadyAcceptedMultiplier * alreadyAcceptedMultiplier;

						value += c.getCoefficient() * alreadyAcceptedMultiplier;
						if (!c.isSquared()) {
							overallAlreadyAccepted += alreadyAcceptedPerTimeWindow.get(c.getTimeWindowId());
							overallAcceptableNo += maximalAcceptablePerTimeWindow.get(c.getTimeWindowId());
						}

					} else if (c.getType() == ValueFunctionCoefficientType.REMAINING_CAPACITY) {
						double multiplier = ((double) remainingCapacityPerTimeWindow.get(c.getTimeWindowId()))
								/ ((double) maximalRemainingCapacityPerTimeWindow.get(c.getTimeWindowId()));
						value += c.getCoefficient() * multiplier;
					}else if (c.getType() == ValueFunctionCoefficientType.INTERACTION_REMAINING_CAPACITY_TIME) {
						double multiplier = ((double) remainingCapacityPerTimeWindow.get(c.getTimeWindowId()))
								/ ((double) maximalRemainingCapacityPerTimeWindow.get(c.getTimeWindowId()));
						multiplier=multiplier*((double) time) / ((double) orderHorizonLength);
						value += c.getCoefficient() * multiplier;
					}else if (c.getType() == ValueFunctionCoefficientType.RATIO) {
						double multiplier = ((double) alreadyAcceptedPerTimeWindow.get(c.getTimeWindowId()))
								/ ((double) maximalAcceptablePerTimeWindow.get(c.getTimeWindowId()));
						multiplier = multiplier * currentDemandCapacityRatio.get(c.getTimeWindowId())
								/ maximumDemandCapacityRatio.get(c.getTimeWindowId());
						value += c.getCoefficient() * multiplier;

					} else if (c.getType() == ValueFunctionCoefficientType.COST) {
						insertionCostsNotNumber = true;
						double alreadyAcceptedMultiplier = acceptedInsertionCostsPerTimeWindow.get(c.getTimeWindowId())
								/ ((c.getTimeWindow().getEndTime() - c.getTimeWindow().getStartTime())
										* timeAndVehicleMultiplier);
						if (c.isSquared())
							alreadyAcceptedMultiplier = alreadyAcceptedMultiplier * alreadyAcceptedMultiplier;

						value += c.getCoefficient() * alreadyAcceptedMultiplier;
						if (!c.isSquared()) {
							overallAcceptedInsertionCosts += acceptedInsertionCostsPerTimeWindow
									.get(c.getTimeWindowId());
							overallAcceptableInsertionCosts += ((c.getTimeWindow().getEndTime()
									- c.getTimeWindow().getStartTime()) * timeAndVehicleMultiplier);
						}
					}
				}

				// Accepted costs
				if (acceptedCostsCoefficient != null) {
					value += acceptedCostsCoefficient * acceptedInsertionCosts / maximumAcceptedCosts;
				}

				// Time and capacity interaction
				if (timeCapacityInteractionCoefficient != null && timeCapacityInteractionCoefficient!=0.0 &&!insertionCostsNotNumber) {
					value += timeCapacityInteractionCoefficient
							* (((double) overallAlreadyAccepted) / ((double) overallAcceptableNo))
							* (((double) (orderHorizonLength - time)) / ((double) orderHorizonLength));
				} else if (timeCapacityInteractionCoefficient != null && timeCapacityInteractionCoefficient!=0.0) {
					value += timeCapacityInteractionCoefficient
							* (overallAcceptedInsertionCosts / overallAcceptableInsertionCosts)
							* (((double) (orderHorizonLength - time)) / ((double) orderHorizonLength));

				}

				// Area potential
				if (areaPotentialCoefficient != null) {
					value += areaPotentialCoefficient * areaPotential / maximumAreaPotential * time
							/ ((double) orderHorizonLength);
				}

			}
		}else{
			System.out.println("Strange");
		}
		return value;
	}
	
	public static double evaluateStateForLinearValueFunctionApproximationWithOrienteeringForAreaSpecific(int time, double weightArea, 
			HashMap<Integer, Double> valueWeightPerTimeWindow,
			HashMap<Integer, Integer> alreadyAcceptedPerTimeWindow,
			HashMap<Integer, Integer> remainingCapacityPerTimeWindow, double basicCoefficient, double timeCoefficient,
			ArrayList<ValueFunctionApproximationCoefficient> variableCoefficients, int orderHorizonLength, double maximumWeightArea,
			HashMap<Integer, Double> maximumValueWeightPerTimeWindow,
			HashMap<Integer, Integer> maximalAcceptablePerTimeWindow,
			double timeAndVehicleMultiplier, int numberOfTimeWindows, boolean considerRemainingCap, double endValue) {

		double value = 0.0;

		// If time is after end of order horizon, the value is 0 or penalty
		if (time ==0){
			value=endValue;
		}else if(time > 0) {

			// If we consider remaining capacity and it is 0, the value is 0
			int remainingCap = 0;
			if (considerRemainingCap) {
				for (Integer twId : remainingCapacityPerTimeWindow.keySet()) {
					remainingCap += remainingCapacityPerTimeWindow.get(twId);
				}
			}

			if ((considerRemainingCap && remainingCap > 0)
					|| remainingCapacityPerTimeWindow == null) {
				// Basic
				value += basicCoefficient;

				// Remaining time
				value += (double) time / ((double) orderHorizonLength) * weightArea/maximumWeightArea*timeCoefficient;

				// Time window dependent coefficients
				int overallAlreadyAccepted = 0;
				double overallAcceptedInsertionCosts = 0.0;
				int overallAcceptableNo = 0;
				double overallAcceptableInsertionCosts = 0.0;
				boolean insertionCostsNotNumber = false;
				for (ValueFunctionApproximationCoefficient c : variableCoefficients) {
					double valueWeight = valueWeightPerTimeWindow.get(c.getTimeWindowId())/maximumValueWeightPerTimeWindow.get(c.getTimeWindowId());
					if (c.getType() == ValueFunctionCoefficientType.NUMBER) {
						double alreadyAcceptedMultiplier = ((double) alreadyAcceptedPerTimeWindow
								.get(c.getTimeWindowId()))
								/ ((double) maximalAcceptablePerTimeWindow.get(c.getTimeWindowId()));
						if (c.isSquared())
							alreadyAcceptedMultiplier = alreadyAcceptedMultiplier * alreadyAcceptedMultiplier;

						//Area specific
						alreadyAcceptedMultiplier = alreadyAcceptedMultiplier*valueWeight;
						value += c.getCoefficient() * alreadyAcceptedMultiplier;
						if (!c.isSquared()) {
							overallAlreadyAccepted += alreadyAcceptedPerTimeWindow.get(c.getTimeWindowId());
							overallAcceptableNo += maximalAcceptablePerTimeWindow.get(c.getTimeWindowId());
						}

					} else if (c.getType() == ValueFunctionCoefficientType.REMAINING_CAPACITY) {
						double multiplier = ((double) remainingCapacityPerTimeWindow.get(c.getTimeWindowId()))
								/ ((double) maximalAcceptablePerTimeWindow.get(c.getTimeWindowId()))*valueWeight;
						value += c.getCoefficient() * multiplier;
					}else if (c.getType() == ValueFunctionCoefficientType.INTERACTION_REMAINING_CAPACITY_TIME) {
						double multiplier = ((double) remainingCapacityPerTimeWindow.get(c.getTimeWindowId()))
								/ ((double) maximalAcceptablePerTimeWindow.get(c.getTimeWindowId()));
						multiplier=multiplier*((double) time) / ((double) orderHorizonLength)*valueWeight;
						value += c.getCoefficient() * multiplier;
					}
				}


			}
		}else{
			System.out.println("Strange");
		}
		return value;
	}

	public static double evaluateStateForLinearValueFunctionApproximationModelWithOrienteering(
			ValueFunctionApproximationModel model, int time, HashMap<Integer, Integer> alreadyAcceptedPerTimeWindow,
			HashMap<Integer, Integer> remainingCapacityPerTimeWindow,
			HashMap<Integer, Double> acceptedInsertionCostsPerTimeWindow, Double acceptedInsertionCosts,
			Double areaPotential, HashMap<Integer, Double> currentDemandCapacityRatio, int orderHorizonLength,
			HashMap<Integer, Integer> maximalAcceptablePerTimeWindow, HashMap<Integer, Integer> maximalRemainingCapacityPerTimeWindow, double maximumAcceptedCosts,
			double maximumAreaPotential, HashMap<Integer, Double> maximumDemandCapacityRatio,
			double timeAndVehicleMultiplier, int numberOfTimeWindows, boolean considerRemainingCap, double endValue) {

		return ValueFunctionApproximationService.evaluateStateForLinearValueFunctionApproximationWithOrienteering(time,
				alreadyAcceptedPerTimeWindow, remainingCapacityPerTimeWindow, acceptedInsertionCosts, acceptedInsertionCostsPerTimeWindow,
				areaPotential, currentDemandCapacityRatio, model.getBasicCoefficient(), model.getTimeCoefficient(),
				model.getCoefficients(), model.getAcceptedOverallCostCoefficient(), model.getAreaPotentialCoefficient(),
				model.getTimeCapacityInteractionCoefficient(), orderHorizonLength, maximalAcceptablePerTimeWindow,maximalRemainingCapacityPerTimeWindow,
				maximumAcceptedCosts, maximumAreaPotential, maximumDemandCapacityRatio, timeAndVehicleMultiplier,
				numberOfTimeWindows, considerRemainingCap, endValue);
	}
	
	public static double evaluateStateForLinearValueFunctionApproximationModelWithOrienteeringForAreaSpecific(
			ValueFunctionApproximationModel model,  int time, double weightArea,HashMap<Integer, Double> valueWeightPerTimeWindow, HashMap<Integer, Integer> alreadyAcceptedPerTimeWindow,
			HashMap<Integer, Integer> remainingCapacityPerTimeWindow,
			 int orderHorizonLength,double maximumWeightArea,HashMap<Integer, Double> maximumValueWeightPerTimeWindow,
			HashMap<Integer, Integer> maximalAcceptablePerTimeWindow,
			double timeAndVehicleMultiplier, int numberOfTimeWindows, boolean considerRemainingCap, double endValue) {

		return ValueFunctionApproximationService.evaluateStateForLinearValueFunctionApproximationWithOrienteeringForAreaSpecific(time,weightArea, valueWeightPerTimeWindow,
				alreadyAcceptedPerTimeWindow, remainingCapacityPerTimeWindow,  model.getBasicCoefficient(), model.getTimeCoefficient(),
				model.getCoefficients(),  orderHorizonLength, maximumWeightArea,maximumValueWeightPerTimeWindow, maximalAcceptablePerTimeWindow,
				 timeAndVehicleMultiplier,
				numberOfTimeWindows, considerRemainingCap, endValue);
	
	}

	public static double evaluateStateForLinearValueFunctionApproximationModel_old(
			ValueFunctionApproximationModel model, HashMap<Integer, Integer> alreadyAcceptedPerTimeWindow,
			HashMap<Integer, Double> acceptedInsertionCosts, HashMap<Integer, Double> coveredPerTimeWindow, int time,
			int orderHorizonLength, int maximalAcceptablePerTimeWindow, double timeMultiplier, int numberOfTimeWindows,
			HashMap<Integer, Double> maximalCoveredPerTimeWindow) {

		return ValueFunctionApproximationService.evaluateStateForLinearValueFunctionApproximation_old(
				alreadyAcceptedPerTimeWindow, acceptedInsertionCosts, coveredPerTimeWindow, time,
				model.getBasicCoefficient(), model.getCoefficients(), model.getTimeCoefficient(), orderHorizonLength,
				maximalAcceptablePerTimeWindow, model.getTimeCapacityInteractionCoefficient(), timeMultiplier,
				numberOfTimeWindows, maximalCoveredPerTimeWindow);
	}

	public static double evaluateStateForLinearValueFunctionApproximationModel(ValueFunctionApproximationModel model,
			int time, HashMap<Integer, Integer> alreadyAcceptedPerTimeWindow,
			HashMap<Integer, Double> acceptedInsertionCostsPerTimeWindow, HashMap<Integer, Double> coveredPerTimeWindow,
			Double remainingCapacity, Double acceptedCostOverall, Double areaPotential, int orderHorizonLength, HashMap<TimeWindow, Integer> maxAcceptedPerTw,int overallAcceptable,
			HashMap<Integer, Double> maximalCoveredPerTimeWindow, double maximumRemainingCapacity, double maximumAcceptedCost,
			double maximumAreaPotential, double timeAndVehicleMultiplier, int numberOfTimeWindows) {

		return ValueFunctionApproximationService.evaluateStateForLinearValueFunctionApproximation(time,
				alreadyAcceptedPerTimeWindow, acceptedInsertionCostsPerTimeWindow, coveredPerTimeWindow,
				remainingCapacity, acceptedCostOverall, areaPotential, model.getBasicCoefficient(), model.getTimeCoefficient(),
				model.getCoefficients(), model.getTimeCapacityInteractionCoefficient(),
				model.getRemainingCapacityCoefficient(), model.getAcceptedOverallCostCoefficient(),model.getAreaPotentialCoefficient(), orderHorizonLength,
				maxAcceptedPerTw, overallAcceptable, maximalCoveredPerTimeWindow, maximumRemainingCapacity,maximumAcceptedCost,
				maximumAreaPotential, timeAndVehicleMultiplier, numberOfTimeWindows);
	}

	public static void updateLinearValueFunctionApproximation(DeliveryArea area, int time,
			HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerTimeWindow,
			HashMap<Integer, HashMap<Integer, Double>> acceptedInsertionCosts,
			HashMap<Integer, HashMap<Integer, Double>> coveredValue, HashMap<Integer, Double> remainingCapacity,
			HashMap<Integer, Double> acceptedCostOverall,
			HashMap<Integer, Double> areaPotential, HashMap<Integer, Double> basicCoefficient,
			HashMap<Integer, Double> timeCoefficient,
			HashMap<Integer, ArrayList<ValueFunctionApproximationCoefficient>> variableCoefficients,
			HashMap<Integer, Double> timeCapacityInteractionCoefficients,
			HashMap<Integer, Double> remainingCapacityCoefficients, HashMap<Integer, Double> acceptedOverallCostCoefficients, HashMap<Integer, Double> areaPotentialCoefficients,
			double newValue, double stepSize, int orderHorizonLength, HashMap<TimeWindow, Integer> maxAcceptedPerTw,int overallAcceptable,
			HashMap<Integer, HashMap<Integer, Double>> maximalCoveredPerTimeWindow,
			HashMap<Integer, Double> maximumRemainingCapacity, HashMap<Integer, Double> maximumAcceptedOverallCost, HashMap<Integer, Double> maximumAreaPotential,
			HashMap<Integer, ArrayList<Double>> lossLog, HashMap<Integer, ArrayList<String>> weightLog,
			int requestSetId, double timeAndVehicleMultiplier, int numberOfTimeWindows, double momentumWeight,
			HashMap<Integer, MomentumHelper> oldMomentumPerDeliveryArea) {

		Double oldValue = evaluateStateForLinearValueFunctionApproximation(time,
				alreadyAcceptedPerTimeWindow.get(area.getId()), acceptedInsertionCosts.get(area.getId()),
				coveredValue.get(area.getId()), remainingCapacity.get(area.getId()), acceptedCostOverall.get(area.getId()), areaPotential.get(area.getId()),
				basicCoefficient.get(area.getId()), timeCoefficient.get(area.getId()),
				variableCoefficients.get(area.getId()), timeCapacityInteractionCoefficients.get(area.getId()),
				remainingCapacityCoefficients.get(area.getId()), acceptedOverallCostCoefficients.get(area.getId()), areaPotentialCoefficients.get(area.getId()),
				orderHorizonLength, maxAcceptedPerTw, overallAcceptable, maximalCoveredPerTimeWindow.get(area.getId()),
				maximumRemainingCapacity.get(area.getId()),maximumAcceptedOverallCost.get(area.getId()),
				maximumAreaPotential.get(area.getId()) * overallAcceptable,
				timeAndVehicleMultiplier, numberOfTimeWindows);

		MomentumHelper momentum = null;
		if (momentumWeight > 0) {
			if (oldMomentumPerDeliveryArea.containsKey(area.getId())) {
				momentum = oldMomentumPerDeliveryArea.get(area.getId());
			} else {
				momentum = new MomentumHelper(variableCoefficients.get(area.getId()));
				oldMomentumPerDeliveryArea.put(area.getId(), momentum);
			}
		}

		double loss = oldValue - newValue;
		if (time % LOGGING_FREQUENCE == 0)
			lossLog.get(requestSetId).add(loss);

		// Update basic
		double newBasic;
		if (momentum != null) {
			double newMomentum = loss + momentumWeight * momentum.getBasicCoefficientMomentum();
			newBasic = basicCoefficient.get(area.getId()) - stepSize * newMomentum;
			momentum.setBasicCoefficientMomentum(newMomentum);
		} else {
			newBasic = basicCoefficient.get(area.getId()) - stepSize * loss;
		}
		basicCoefficient.put(area.getId(), newBasic);
		String weight = "b:" + newBasic + ";";

		// Update time
		double newTime;
		if (momentum != null) {
			double newMomentum = loss * ((double) time) / ((double) orderHorizonLength)
					+ momentumWeight * momentum.getTimeCoefficientMomentum();
			newTime = timeCoefficient.get(area.getId()) - stepSize * newMomentum;
			momentum.setTimeCoefficientMomentum(newMomentum);
		} else {
			newTime = timeCoefficient.get(area.getId())
					- stepSize * loss * ((double) time) / ((double) orderHorizonLength);
		}
		timeCoefficient.put(area.getId(), newTime);
		weight += "t:" + newTime + ";";

		// Update time window dependent coefficients
		int overallAccepted = 0;
		double overallCosts = 0.0;
		double overallAcceptableCosts = 0.0;
		boolean costsNotNumber = false;
		for (ValueFunctionApproximationCoefficient c : variableCoefficients.get(area.getId())) {
			if (!c.isCosts() && !c.isCoverage()) {
				double alreadyAcceptedMultiplier = ((double) alreadyAcceptedPerTimeWindow.get(area.getId())
						.get(c.getTimeWindowId())) / ((double) maxAcceptedPerTw.get(c.getTimeWindow()));
				if (c.isSquared())
					alreadyAcceptedMultiplier = alreadyAcceptedMultiplier * alreadyAcceptedMultiplier;
				double newCoefficient;
				if (momentum != null) {
					double newMomentum = loss * alreadyAcceptedMultiplier
							+ momentumWeight * momentum.getCoefficientMomentums().get(c);
					newCoefficient = c.getCoefficient() - stepSize * newMomentum;
					momentum.getCoefficientMomentums().put(c, newMomentum);
				} else {
					newCoefficient = c.getCoefficient() - stepSize * loss * alreadyAcceptedMultiplier;
				}
				c.setCoefficient(newCoefficient);
				if (!c.isSquared() && !c.isDemandCapacityRatio())
					overallAccepted += alreadyAcceptedPerTimeWindow.get(area.getId()).get(c.getTimeWindowId());
				weight += "c_" + c.getTimeWindowId();
				if (c.isSquared())
					weight += "2";
				weight += ":" + newCoefficient + ";";
			} else if (c.isCosts()) {
				costsNotNumber = true;
				double acceptedInsertionMultiplier = ((double) acceptedInsertionCosts.get(area.getId())
						.get(c.getTimeWindowId()))
						/ ((c.getTimeWindow().getEndTime() - c.getTimeWindow().getStartTime())
								* timeAndVehicleMultiplier);
				if (c.isSquared())
					acceptedInsertionMultiplier = acceptedInsertionMultiplier * acceptedInsertionMultiplier;

				double newCoefficient;
				if (momentum != null) {
					double newMomentum = loss * acceptedInsertionMultiplier
							+ momentumWeight * momentum.getCoefficientMomentums().get(c);
					newCoefficient = c.getCoefficient() - stepSize * newMomentum;
					momentum.getCoefficientMomentums().put(c, newMomentum);
				} else {
					newCoefficient = c.getCoefficient() - stepSize * loss * acceptedInsertionMultiplier;
				}

				c.setCoefficient(newCoefficient);
				weight += "c_" + c.getTimeWindowId();
				if (c.isSquared())
					weight += "2";
				weight += "_c:" + newCoefficient + ";";
				if (!c.isSquared()) {
					overallCosts += acceptedInsertionCosts.get(area.getId()).get(c.getTimeWindowId());
					overallAcceptableCosts += (c.getTimeWindow().getEndTime() - c.getTimeWindow().getStartTime())
							* timeAndVehicleMultiplier;
				}
			} else if (c.isCoverage()) {
				double acceptedCoverage = ((double) coveredValue.get(area.getId()).get(c.getTimeWindowId()))
						/ maximalCoveredPerTimeWindow.get(area.getId()).get(c.getTimeWindowId());

				double newCoefficient;
				if (momentum != null) {
					double newMomentum = loss * acceptedCoverage * ((double) time) / ((double) orderHorizonLength)
							+ momentumWeight * momentum.getCoefficientMomentums().get(c);
					newCoefficient = c.getCoefficient() - stepSize * newMomentum;
					momentum.getCoefficientMomentums().put(c, newMomentum);
				} else {
					newCoefficient = c.getCoefficient()
							- stepSize * loss * acceptedCoverage * ((double) time) / ((double) orderHorizonLength);
				}

				c.setCoefficient(newCoefficient);
				weight += "c_" + c.getTimeWindow() + "_cov:" + newCoefficient + ";";
			}
		}

		// Update time capacity interaction
		if ((timeCapacityInteractionCoefficients.get(area.getId()) != null) && !costsNotNumber) {

			double newInteraction;
			if (momentum != null) {
				double newMomentum = loss * overallAccepted
						/ ((double) overallAcceptable) * ((double) time)
						/ ((double) orderHorizonLength)
						+ momentumWeight * momentum.getTimeCapacitiyInteractionMomentum();
				newInteraction = timeCapacityInteractionCoefficients.get(area.getId()) - stepSize * newMomentum;
				momentum.setTimeCapacitiyInteractionMomentum(newMomentum);
			} else {
				newInteraction = timeCapacityInteractionCoefficients.get(area.getId()) - stepSize * loss
						* overallAccepted / ((double) overallAcceptable)
						* ((double) time) / ((double) orderHorizonLength);
			}

			timeCapacityInteractionCoefficients.put(area.getId(), newInteraction);
			weight += "tc:" + newInteraction + ";";
		} else if (timeCapacityInteractionCoefficients.get(area.getId()) != null) {

			double newInteraction;
			if (momentum != null) {
				double newMomentum = loss * overallCosts / overallAcceptableCosts * ((double) time)
						/ ((double) orderHorizonLength)
						+ momentumWeight * momentum.getTimeCapacitiyInteractionMomentum();
				newInteraction = timeCapacityInteractionCoefficients.get(area.getId()) - stepSize * newMomentum;
				momentum.setTimeCapacitiyInteractionMomentum(newMomentum);
			} else {
				newInteraction = timeCapacityInteractionCoefficients.get(area.getId()) - stepSize * loss * overallCosts
						/ overallAcceptableCosts * ((double) time) / ((double) orderHorizonLength);
			}

			timeCapacityInteractionCoefficients.put(area.getId(), newInteraction);
			weight += "tc:" + newInteraction + ";";
		}

		// Update remaining capacity
		if (remainingCapacityCoefficients.get(area.getId()) != null) {

			double newRemainingCapacity;
			if (momentum != null) {
				double newMomentum = loss * remainingCapacity.get(area.getId())
						/ maximumRemainingCapacity.get(area.getId())
						+ momentumWeight * momentum.getRemainingCapacityMomentum();
				newRemainingCapacity = remainingCapacityCoefficients.get(area.getId()) - stepSize * newMomentum;
				momentum.setRemainingCapacityMomentum(newMomentum);
			} else {
				newRemainingCapacity = remainingCapacityCoefficients.get(area.getId()) - stepSize * loss
						* remainingCapacity.get(area.getId()) / maximumRemainingCapacity.get(area.getId());
			}

			remainingCapacityCoefficients.put(area.getId(), newRemainingCapacity);
			weight += "rc:" + newRemainingCapacity + ";";
		}

		// Update overall accepted cost
				if (acceptedOverallCostCoefficients.get(area.getId()) != null) {

					double newAccCost;
					if (momentum != null) {
						double newMomentum = loss * acceptedCostOverall.get(area.getId())
								/ maximumAcceptedOverallCost.get(area.getId())
								+ momentumWeight * momentum.getAcceptedCostMomentum();
						newAccCost = acceptedOverallCostCoefficients.get(area.getId()) - stepSize * newMomentum;
						momentum.setAcceptedCostMomentum(newMomentum);
					} else {
						newAccCost = acceptedOverallCostCoefficients.get(area.getId()) - stepSize * loss
								* acceptedCostOverall.get(area.getId()) / maximumAcceptedOverallCost.get(area.getId());
					}

					acceptedOverallCostCoefficients.put(area.getId(), newAccCost);
					weight += "oc:" + newAccCost + ";";
				}
				
		// Update area potential
		if (areaPotentialCoefficients.get(area.getId()) != null) {

			double newAreaPotential;
			if (momentum != null) {
				double newMomentum = loss * areaPotential.get(area.getId())
						/ (maximumAreaPotential.get(area.getId()) *overallAcceptable)
						* time / ((double) orderHorizonLength) + momentumWeight * momentum.getAreaPotentialMomentum();
				newAreaPotential = areaPotentialCoefficients.get(area.getId()) - stepSize * newMomentum;
				momentum.setAreaPotentialMomentum(newMomentum);
			} else {
				newAreaPotential = areaPotentialCoefficients.get(area.getId()) - stepSize * loss
						* areaPotential.get(area.getId()) / (maximumAreaPotential.get(area.getId())
								* (double) overallAcceptable)
						* time / ((double) orderHorizonLength);
			}

			areaPotentialCoefficients.put(area.getId(), newAreaPotential);
			weight += "ap:" + newAreaPotential + ";";
		}

		if (time % LOGGING_FREQUENCE == 0)
			weightLog.get(requestSetId).add(weight);

	}

	public static void updateLinearValueFunctionApproximationWithOrienteering(DeliveryArea area, int time,
			HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerTimeWindow,
			HashMap<Integer, HashMap<Integer, Integer>> remainingCapacityPerTimeWindow,
			HashMap<Integer, Double> acceptedInsertionCosts,
			HashMap<Integer, HashMap<Integer, Double>> acceptedInsertionCostsPerTw,
			HashMap<Integer, Double> areaPotential, HashMap<Integer, Double> currentDemandCapacityRatio,
			HashMap<Integer, Double> basicCoefficient, HashMap<Integer, Double> timeCoefficient,
			HashMap<Integer, ArrayList<ValueFunctionApproximationCoefficient>> variableCoefficients,
			HashMap<Integer, Double> acceptedCostsCoefficient, HashMap<Integer, Double> areaPotentialCoefficients,
			HashMap<Integer, Double> timeCapacityInteractionCoefficients, double newValue, double stepSize,
			int orderHorizonLength, HashMap<Integer, HashMap<Integer, Integer>> maximalAcceptablePerTimeWindow,HashMap<Integer, HashMap<Integer, Integer>> maximalRemainingCapacityPerTimeWindow,
			HashMap<Integer, Double> maximumAcceptedCosts, HashMap<Integer, Double> maximumAreaPotential,
			HashMap<Integer, Double> maximumDemandCapacityRatio, HashMap<Integer, ArrayList<Double>> lossLog,
			HashMap<Integer, ArrayList<String>> weightLog, int requestSetId, double timeAndVehicleMultiplier,
			int numberOfTimeWindows, double momentumWeight,
			HashMap<Integer, MomentumHelper> oldMomentumPerDeliveryArea, boolean considerRemainingCap, double endValue) {

		double maxAreaPotential = 0;
		for (Integer twId : maximalAcceptablePerTimeWindow.get(area.getId()).keySet()) {
			maxAreaPotential += maximalAcceptablePerTimeWindow.get(area.getId()).get(twId);
		}
		maxAreaPotential = maxAreaPotential * maximumAreaPotential.get(area.getId());

		Double oldValue = evaluateStateForLinearValueFunctionApproximationWithOrienteering(time,
				alreadyAcceptedPerTimeWindow.get(area.getId()), remainingCapacityPerTimeWindow.get(area.getId()), acceptedInsertionCosts.get(area.getId()),
				acceptedInsertionCostsPerTw.get(area.getId()), areaPotential.get(area.getId()),
				currentDemandCapacityRatio, basicCoefficient.get(area.getId()), timeCoefficient.get(area.getId()),
				variableCoefficients.get(area.getId()), acceptedCostsCoefficient.get(area.getId()),
				areaPotentialCoefficients.get(area.getId()), timeCapacityInteractionCoefficients.get(area.getId()),
				orderHorizonLength, maximalAcceptablePerTimeWindow.get(area.getId()),maximalRemainingCapacityPerTimeWindow.get(area.getId()),
				maximumAcceptedCosts.get(area.getId()),

				maxAreaPotential, maximumDemandCapacityRatio, timeAndVehicleMultiplier, numberOfTimeWindows, considerRemainingCap, endValue);

		MomentumHelper momentum = null;
		if (momentumWeight > 0) {
			if (oldMomentumPerDeliveryArea.containsKey(area.getId())) {
				momentum = oldMomentumPerDeliveryArea.get(area.getId());
			} else {
				momentum = new MomentumHelper(variableCoefficients.get(area.getId()));
				oldMomentumPerDeliveryArea.put(area.getId(), momentum);
			}
		}

		double loss = oldValue - newValue;
		if (time % LOGGING_FREQUENCE == 0)
			lossLog.get(requestSetId).add(loss);

		// Update basic
		double newBasic;
		if (momentum != null) {
			double newMomentum = loss + momentumWeight * momentum.getBasicCoefficientMomentum();
			newBasic = basicCoefficient.get(area.getId()) - stepSize * newMomentum;
			momentum.setBasicCoefficientMomentum(newMomentum);
		} else {
			newBasic = basicCoefficient.get(area.getId()) - stepSize * loss;
		}
		basicCoefficient.put(area.getId(), newBasic);
		String weight = "b:" + newBasic + ";";

		// Update time
		double newTime;
		if (momentum != null) {
			double newMomentum = loss * ((double) time) / ((double) orderHorizonLength)
					+ momentumWeight * momentum.getTimeCoefficientMomentum();
			newTime = timeCoefficient.get(area.getId()) - stepSize * newMomentum;
			momentum.setTimeCoefficientMomentum(newMomentum);
		} else {
			newTime = timeCoefficient.get(area.getId())
					- stepSize * loss * ((double) time) / ((double) orderHorizonLength);
		}
		timeCoefficient.put(area.getId(), newTime);
		weight += "t:" + newTime + ";";

		// Update time window dependent coefficients
		int overallAccepted = 0;
		int overallAcceptableNo = 0;
		double overallCosts = 0.0;
		double overallAcceptableCosts = 0.0;
		boolean costsNotNumber = false;
		for (ValueFunctionApproximationCoefficient c : variableCoefficients.get(area.getId())) {

			if (c.getType() == ValueFunctionCoefficientType.NUMBER) {
				double alreadyAcceptedMultiplier = ((double) alreadyAcceptedPerTimeWindow.get(area.getId())
						.get(c.getTimeWindowId()))
						/ ((double) maximalAcceptablePerTimeWindow.get(area.getId()).get(c.getTimeWindowId()));
				if (c.isSquared())
					alreadyAcceptedMultiplier = alreadyAcceptedMultiplier * alreadyAcceptedMultiplier;

				double newCoefficient;
				if (momentum != null) {
					double newMomentum = loss * alreadyAcceptedMultiplier
							+ momentumWeight * momentum.getCoefficientMomentums().get(c);
					newCoefficient = c.getCoefficient() - stepSize * newMomentum;
					momentum.getCoefficientMomentums().put(c, newMomentum);
				} else {
					newCoefficient = c.getCoefficient() - stepSize * loss * alreadyAcceptedMultiplier;
				}
				// if(newCoefficient < c.getCoefficient() ){
				// System.out.println("");
				// }
				c.setCoefficient(newCoefficient);
				weight += "c_" + c.getTimeWindowId();
				if (c.isSquared())
					weight += "2";

				weight += ":" + newCoefficient + ";";

				if (!c.isSquared()) {
					overallAccepted += alreadyAcceptedPerTimeWindow.get(area.getId()).get(c.getTimeWindowId());
					overallAcceptableNo += maximalAcceptablePerTimeWindow.get(area.getId()).get(c.getTimeWindowId());
				}

			} else if (c.getType() == ValueFunctionCoefficientType.REMAINING_CAPACITY) {
				double alreadyAcceptedMultiplier = ((double) remainingCapacityPerTimeWindow.get(area.getId())
						.get(c.getTimeWindowId()))
						/ ((double) maximalRemainingCapacityPerTimeWindow.get(area.getId()).get(c.getTimeWindowId()));
				if (c.isSquared())
					alreadyAcceptedMultiplier = alreadyAcceptedMultiplier * alreadyAcceptedMultiplier;

				double newCoefficient;
				if (momentum != null) {
					double newMomentum = loss * alreadyAcceptedMultiplier
							+ momentumWeight * momentum.getCoefficientMomentums().get(c);
					newCoefficient = c.getCoefficient() - stepSize * newMomentum;
					momentum.getCoefficientMomentums().put(c, newMomentum);
				} else {
					newCoefficient = c.getCoefficient() - stepSize * loss * alreadyAcceptedMultiplier;
				}
				// if(newCoefficient < c.getCoefficient() ){
				// System.out.println("");
				// }
				c.setCoefficient(newCoefficient);
				weight += "c_r" + c.getTimeWindowId();
				if (c.isSquared())
					weight += "2";

				weight += ":" + newCoefficient + ";";

			}else if (c.getType() == ValueFunctionCoefficientType.INTERACTION_REMAINING_CAPACITY_TIME) {
				double alreadyAcceptedMultiplier = ((double) remainingCapacityPerTimeWindow.get(area.getId())
						.get(c.getTimeWindowId()))
						/ ((double) maximalRemainingCapacityPerTimeWindow.get(area.getId()).get(c.getTimeWindowId()))*((double) time) / ((double) orderHorizonLength);

				double newCoefficient;
				if (momentum != null) {
					double newMomentum = loss * alreadyAcceptedMultiplier
							+ momentumWeight * momentum.getCoefficientMomentums().get(c);
					newCoefficient = c.getCoefficient() - stepSize * newMomentum;
					momentum.getCoefficientMomentums().put(c, newMomentum);
				} else {
					newCoefficient = c.getCoefficient() - stepSize * loss * alreadyAcceptedMultiplier;
				}
				// if(newCoefficient < c.getCoefficient() ){
				// System.out.println("");
				// }
				c.setCoefficient(newCoefficient);
				weight += "c_rt" + c.getTimeWindowId();
				weight += ":" + newCoefficient + ";";

			}else if (c.getType() == ValueFunctionCoefficientType.RATIO) {
				double alreadyAcceptedMultiplier = ((double) alreadyAcceptedPerTimeWindow.get(area.getId())
						.get(c.getTimeWindowId()))
						/ ((double) maximalAcceptablePerTimeWindow.get(area.getId()).get(c.getTimeWindowId()));

				alreadyAcceptedMultiplier = alreadyAcceptedMultiplier
						* currentDemandCapacityRatio.get(c.getTimeWindowId())
						/ maximumDemandCapacityRatio.get(c.getTimeWindowId());

				double newCoefficient;
				if (momentum != null) {
					double newMomentum = loss * alreadyAcceptedMultiplier
							+ momentumWeight * momentum.getCoefficientMomentums().get(c);
					newCoefficient = c.getCoefficient() - stepSize * newMomentum;
					momentum.getCoefficientMomentums().put(c, newMomentum);
				} else {
					newCoefficient = c.getCoefficient() - stepSize * loss * alreadyAcceptedMultiplier;
				}
				// if(newCoefficient < c.getCoefficient() ){
				// System.out.println("");
				// }
				c.setCoefficient(newCoefficient);
				weight += "c_dc" + c.getTimeWindowId();

			} else if (c.getType() == ValueFunctionCoefficientType.COST) {
				costsNotNumber = true;
				double acceptedInsertionMultiplier = ((double) acceptedInsertionCostsPerTw.get(area.getId())
						.get(c.getTimeWindowId()))
						/ ((c.getTimeWindow().getEndTime() - c.getTimeWindow().getStartTime())
								* timeAndVehicleMultiplier);
				if (c.isSquared())
					acceptedInsertionMultiplier = acceptedInsertionMultiplier * acceptedInsertionMultiplier;

				double newCoefficient;
				if (momentum != null) {
					double newMomentum = loss * acceptedInsertionMultiplier
							+ momentumWeight * momentum.getCoefficientMomentums().get(c);
					newCoefficient = c.getCoefficient() - stepSize * newMomentum;
					momentum.getCoefficientMomentums().put(c, newMomentum);
				} else {
					newCoefficient = c.getCoefficient() - stepSize * loss * acceptedInsertionMultiplier;
				}

				c.setCoefficient(newCoefficient);
				weight += "c_" + c.getTimeWindowId();
				if (c.isSquared())
					weight += "2";
				weight += "_c:" + newCoefficient + ";";
				if (!c.isSquared()) {
					overallCosts += acceptedInsertionCostsPerTw.get(area.getId()).get(c.getTimeWindowId());
					overallAcceptableCosts += (c.getTimeWindow().getEndTime() - c.getTimeWindow().getStartTime())
							* timeAndVehicleMultiplier;
				}

			}
		}

		// Update overall costs
		if (acceptedCostsCoefficient.get(area.getId()) != null) {

			double newCosts;
			if (momentum != null) {
				double newMomentum = loss * acceptedInsertionCosts.get(area.getId())
						/ maximumAcceptedCosts.get(area.getId()) + momentumWeight * momentum.getAcceptedCostMomentum();
				newCosts = acceptedCostsCoefficient.get(area.getId()) - stepSize * newMomentum;
				momentum.setAcceptedCostMomentum(newMomentum);
			} else {
				newCosts = acceptedCostsCoefficient.get(area.getId()) - stepSize * loss
						* acceptedInsertionCosts.get(area.getId()) / maximumAcceptedCosts.get(area.getId());
			}

			acceptedCostsCoefficient.put(area.getId(), newCosts);
			weight += "c:" + newCosts + ";";
		}

		// Update time capacity interaction
		if ((timeCapacityInteractionCoefficients.get(area.getId()) != null) && !costsNotNumber) {

			double newInteraction;
			if (momentum != null) {
				double newMomentum = loss * overallAccepted / ((double) overallAcceptableNo)
						* ((double) (orderHorizonLength - time)) / ((double) orderHorizonLength)
						+ momentumWeight * momentum.getTimeCapacitiyInteractionMomentum();
				newInteraction = timeCapacityInteractionCoefficients.get(area.getId()) - stepSize * newMomentum;
				momentum.setTimeCapacitiyInteractionMomentum(newMomentum);
			} else {
				newInteraction = timeCapacityInteractionCoefficients.get(area.getId())
						- stepSize * loss * overallAccepted / ((double) overallAcceptableNo)
								* ((double) (orderHorizonLength - time)) / ((double) orderHorizonLength);
			}

			timeCapacityInteractionCoefficients.put(area.getId(), newInteraction);
			weight += "tc:" + newInteraction + ";";
		} else if (timeCapacityInteractionCoefficients.get(area.getId()) != null) {

			double newInteraction;
			if (momentum != null) {
				double newMomentum = loss * overallCosts / overallAcceptableCosts
						* ((double) (orderHorizonLength - time)) / ((double) orderHorizonLength)
						+ momentumWeight * momentum.getTimeCapacitiyInteractionMomentum();
				newInteraction = timeCapacityInteractionCoefficients.get(area.getId()) - stepSize * newMomentum;
				momentum.setTimeCapacitiyInteractionMomentum(newMomentum);
			} else {
				newInteraction = timeCapacityInteractionCoefficients.get(area.getId())
						- stepSize * loss * overallCosts / overallAcceptableCosts
								* ((double) (orderHorizonLength - time)) / ((double) orderHorizonLength);
			}

			timeCapacityInteractionCoefficients.put(area.getId(), newInteraction);
			weight += "tc:" + newInteraction + ";";
		}

		// Update area potential
		if (areaPotentialCoefficients.get(area.getId()) != null) {

			double newAreaPotential;
			if (momentum != null) {
				double newMomentum = loss * areaPotential.get(area.getId()) / (maxAreaPotential) * time
						/ ((double) orderHorizonLength) + momentumWeight * momentum.getAreaPotentialMomentum();
				newAreaPotential = areaPotentialCoefficients.get(area.getId()) - stepSize * newMomentum;
				momentum.setAreaPotentialMomentum(newMomentum);
			} else {
				newAreaPotential = areaPotentialCoefficients.get(area.getId()) - stepSize * loss
						* areaPotential.get(area.getId()) / (maxAreaPotential) * time / ((double) orderHorizonLength);
			}

			areaPotentialCoefficients.put(area.getId(), newAreaPotential);
			weight += "ap:" + newAreaPotential + ";";
		}

		if (time % LOGGING_FREQUENCE == 0)
			weightLog.get(requestSetId).add(weight);

	}
	
	
	public static void updateLinearValueFunctionApproximationWithOrienteeringAreaSpecific(DeliveryArea area, DeliveryArea subArea,int time, double weightArea,HashMap<Integer, Double> valueWeightPerTimeWindow,
			HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerTimeWindow,
			HashMap<Integer, HashMap<Integer, Integer>> remainingCapacityPerTimeWindow,
			HashMap<Integer, Double> basicCoefficient, HashMap<Integer, Double> timeCoefficient,
			HashMap<Integer, ArrayList<ValueFunctionApproximationCoefficient>> variableCoefficients,
			double newValue, double stepSize,
			int orderHorizonLength, double maximumWeightArea, HashMap<Integer, Double> maximumValueWeightPerTimeWindow, HashMap<Integer, Integer> maximalAcceptablePerTimeWindow,
			HashMap<Integer, ArrayList<Double>> lossLog,
			HashMap<Integer, ArrayList<String>> weightLog, int requestSetId, double timeAndVehicleMultiplier,
			int numberOfTimeWindows, double momentumWeight,
			HashMap<Integer, MomentumHelper> oldMomentumPerDeliveryArea, boolean considerRemainingCap, double endValue) {

	

		Double oldValue = evaluateStateForLinearValueFunctionApproximationWithOrienteeringForAreaSpecific(time,weightArea,valueWeightPerTimeWindow,
				alreadyAcceptedPerTimeWindow.get(subArea.getId()), remainingCapacityPerTimeWindow.get(subArea.getId()), 
				 basicCoefficient.get(area.getId()), timeCoefficient.get(area.getId()),
				variableCoefficients.get(area.getId()), 
				orderHorizonLength, maximumWeightArea, maximumValueWeightPerTimeWindow, maximalAcceptablePerTimeWindow, timeAndVehicleMultiplier, numberOfTimeWindows, considerRemainingCap, endValue);
		

		MomentumHelper momentum = null;
		if (momentumWeight > 0) {
			if (oldMomentumPerDeliveryArea.containsKey(area.getId())) {
				momentum = oldMomentumPerDeliveryArea.get(area.getId());
			} else {
				momentum = new MomentumHelper(variableCoefficients.get(area.getId()));
				oldMomentumPerDeliveryArea.put(area.getId(), momentum);
			}
		}

		double loss = oldValue - newValue;
		if (time % LOGGING_FREQUENCE == 0)
			lossLog.get(requestSetId).add(loss);

		// Update basic
		double newBasic;
		if (momentum != null) {
			double newMomentum = loss + momentumWeight * momentum.getBasicCoefficientMomentum();
			newBasic = basicCoefficient.get(area.getId()) - stepSize * newMomentum;
			momentum.setBasicCoefficientMomentum(newMomentum);
		} else {
			newBasic = basicCoefficient.get(area.getId()) - stepSize * loss;
		}
		basicCoefficient.put(area.getId(), newBasic);
		String weight = "b:" + newBasic + ";";

		// Update time
		double newTime;
		if (momentum != null) {
			double newMomentum = loss * ((double) time) / ((double) orderHorizonLength)*weightArea/maximumWeightArea
					+ momentumWeight * momentum.getTimeCoefficientMomentum();
			newTime = timeCoefficient.get(area.getId()) - stepSize * newMomentum;
			momentum.setTimeCoefficientMomentum(newMomentum);
		} else {
			newTime = timeCoefficient.get(area.getId())
					- stepSize * loss * ((double) time) / ((double) orderHorizonLength);
		}
		timeCoefficient.put(area.getId(), newTime);
		weight += "t:" + newTime + ";";

		// Update time window dependent coefficients
		
		for (ValueFunctionApproximationCoefficient c : variableCoefficients.get(area.getId())) {

			double areaValueMul= valueWeightPerTimeWindow.get(c.getTimeWindowId())/maximumValueWeightPerTimeWindow.get(c.getTimeWindowId());
			
			if (c.getType() == ValueFunctionCoefficientType.NUMBER) {
				double alreadyAcceptedMultiplier = ((double) alreadyAcceptedPerTimeWindow.get(area.getId())
						.get(c.getTimeWindowId()))
						/ ((double) maximalAcceptablePerTimeWindow.get(c.getTimeWindowId()))*areaValueMul;
				if (c.isSquared())
					alreadyAcceptedMultiplier = alreadyAcceptedMultiplier * alreadyAcceptedMultiplier;

				double newCoefficient;
				if (momentum != null) {
					double newMomentum = loss * alreadyAcceptedMultiplier
							+ momentumWeight * momentum.getCoefficientMomentums().get(c);
					newCoefficient = c.getCoefficient() - stepSize * newMomentum;
					momentum.getCoefficientMomentums().put(c, newMomentum);
				} else {
					newCoefficient = c.getCoefficient() - stepSize * loss * alreadyAcceptedMultiplier;
				}
				// if(newCoefficient < c.getCoefficient() ){
				// System.out.println("");
				// }
				c.setCoefficient(newCoefficient);
				weight += "c_" + c.getTimeWindowId();
				if (c.isSquared())
					weight += "2";

				weight += ":" + newCoefficient + ";";

				
			} else if (c.getType() == ValueFunctionCoefficientType.REMAINING_CAPACITY) {
				double alreadyAcceptedMultiplier = ((double) remainingCapacityPerTimeWindow.get(area.getId())
						.get(c.getTimeWindowId()))
						/ ((double) maximalAcceptablePerTimeWindow.get(c.getTimeWindowId()))*areaValueMul;
				if (c.isSquared())
					alreadyAcceptedMultiplier = alreadyAcceptedMultiplier * alreadyAcceptedMultiplier;

				double newCoefficient;
				if (momentum != null) {
					double newMomentum = loss * alreadyAcceptedMultiplier
							+ momentumWeight * momentum.getCoefficientMomentums().get(c);
					newCoefficient = c.getCoefficient() - stepSize * newMomentum;
					momentum.getCoefficientMomentums().put(c, newMomentum);
				} else {
					newCoefficient = c.getCoefficient() - stepSize * loss * alreadyAcceptedMultiplier;
				}
				// if(newCoefficient < c.getCoefficient() ){
				// System.out.println("");
				// }
				c.setCoefficient(newCoefficient);
				weight += "c_r" + c.getTimeWindowId();
				if (c.isSquared())
					weight += "2";

				weight += ":" + newCoefficient + ";";

			}else if (c.getType() == ValueFunctionCoefficientType.INTERACTION_REMAINING_CAPACITY_TIME) {
				double alreadyAcceptedMultiplier = ((double) remainingCapacityPerTimeWindow.get(area.getId())
						.get(c.getTimeWindowId()))
						/ ((double) maximalAcceptablePerTimeWindow.get(c.getTimeWindowId()))*((double) time) / ((double) orderHorizonLength)*areaValueMul;

				double newCoefficient;
				if (momentum != null) {
					double newMomentum = loss * alreadyAcceptedMultiplier
							+ momentumWeight * momentum.getCoefficientMomentums().get(c);
					newCoefficient = c.getCoefficient() - stepSize * newMomentum;
					momentum.getCoefficientMomentums().put(c, newMomentum);
				} else {
					newCoefficient = c.getCoefficient() - stepSize * loss * alreadyAcceptedMultiplier;
				}
				// if(newCoefficient < c.getCoefficient() ){
				// System.out.println("");
				// }
				c.setCoefficient(newCoefficient);
				weight += "c_rt" + c.getTimeWindowId();
				weight += ":" + newCoefficient + ";";

			}
		}

		if (time % LOGGING_FREQUENCE == 0)
			weightLog.get(requestSetId).add(weight);

	}

	public static void updateLinearValueFunctionApproximation_old(DeliveryArea area,
			HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerTimeWindow,
			HashMap<Integer, HashMap<Integer, Double>> acceptedInsertionCosts,
			HashMap<Integer, HashMap<Integer, Double>> coveredValue, int time,
			HashMap<Integer, Double> basicCoefficient,
			HashMap<Integer, ArrayList<ValueFunctionApproximationCoefficient>> variableCoefficients,
			HashMap<Integer, Double> timeCoefficient, double newValue, double stepSize, int orderHorizonLength,
			int maximalAcceptablePerTimeWindow, HashMap<Integer, ArrayList<Double>> lossLog,
			HashMap<Integer, ArrayList<String>> weightLog, int requestSetId,
			HashMap<Integer, Double> timeCapacityInteractionCoefficients, double timeAndVehicleMultiplier,
			int numberOfTimeWindows, HashMap<Integer, HashMap<Integer, Double>> maximalCoveredPerTimeWindow) {

		Double oldValue = evaluateStateForLinearValueFunctionApproximation_old(
				alreadyAcceptedPerTimeWindow.get(area.getId()), acceptedInsertionCosts.get(area.getId()),
				coveredValue.get(area.getId()), time, basicCoefficient.get(area.getId()),
				variableCoefficients.get(area.getId()), timeCoefficient.get(area.getId()), orderHorizonLength,
				maximalAcceptablePerTimeWindow, timeCapacityInteractionCoefficients.get(area.getId()),
				timeAndVehicleMultiplier, numberOfTimeWindows, maximalCoveredPerTimeWindow.get(area.getId()));

		double loss = oldValue - newValue;
		if (loss > 10)
			System.out.println("interesting");
		if (time % 100 == 0)
			lossLog.get(requestSetId).add(loss);
		Double newBasic = basicCoefficient.get(area.getId()) - stepSize * loss;
		basicCoefficient.put(area.getId(), newBasic);
		String weight = "b:" + newBasic + ";";
		double newTime = timeCoefficient.get(area.getId())
				- stepSize * loss * ((double) time) / ((double) orderHorizonLength);
		timeCoefficient.put(area.getId(), newTime);
		weight += "t:" + newTime + ";";
		int overallAccepted = 0;
		double overallCosts = 0.0;
		double overallAcceptableCosts = 0.0;
		boolean costsNotNumber = false;
		for (ValueFunctionApproximationCoefficient c : variableCoefficients.get(area.getId())) {

			if (!c.isCosts() && !c.isCoverage()) {
				double alreadyAcceptedMultiplier = ((double) alreadyAcceptedPerTimeWindow.get(area.getId())
						.get(c.getTimeWindowId())) / ((double) maximalAcceptablePerTimeWindow);
				if (c.isSquared())
					alreadyAcceptedMultiplier = alreadyAcceptedMultiplier * alreadyAcceptedMultiplier;
				double newCoefficient = c.getCoefficient() - stepSize * loss * alreadyAcceptedMultiplier;
				c.setCoefficient(newCoefficient);
				if (!c.isSquared())
					overallAccepted += alreadyAcceptedPerTimeWindow.get(area.getId()).get(c.getTimeWindowId());
				weight += "c_" + c.getTimeWindowId();
				if (c.isSquared())
					weight += "2";
				weight += ":" + newCoefficient + ";";
			} else if (c.isCosts()) {
				costsNotNumber = true;
				double acceptedInsertionMultiplier = ((double) acceptedInsertionCosts.get(area.getId())
						.get(c.getTimeWindowId()))
						/ ((c.getTimeWindow().getEndTime() - c.getTimeWindow().getStartTime())
								* timeAndVehicleMultiplier);
				if (c.isSquared())
					acceptedInsertionMultiplier = acceptedInsertionMultiplier * acceptedInsertionMultiplier;
				double newCoefficient = c.getCoefficient() - stepSize * loss * acceptedInsertionMultiplier;
				c.setCoefficient(newCoefficient);
				weight += "c_" + c.getTimeWindowId();
				if (c.isSquared())
					weight += "2";
				weight += "_c:" + newCoefficient + ";";
				if (!c.isSquared()) {
					overallCosts += acceptedInsertionCosts.get(area.getId()).get(c.getTimeWindowId());
					overallAcceptableCosts += (c.getTimeWindow().getEndTime() - c.getTimeWindow().getStartTime())
							* timeAndVehicleMultiplier;
				}
			} else if (c.isCoverage()) {
				double acceptedCoverage = ((double) coveredValue.get(area.getId()).get(c.getTimeWindowId()))
						/ maximalCoveredPerTimeWindow.get(area.getId()).get(c.getTimeWindowId());
				double newCoefficient = c.getCoefficient()
						- stepSize * loss * acceptedCoverage * ((double) time) / ((double) orderHorizonLength);
				if (time < 20)
					System.out.println("coverage update:"
							+ stepSize * loss * acceptedCoverage * ((double) time) / ((double) orderHorizonLength));
				c.setCoefficient(newCoefficient);
				weight += "c_" + c.getTimeWindow() + "_cov:" + newCoefficient + ";";
			}
		}

		if ((timeCapacityInteractionCoefficients.get(area.getId()) != null) && !costsNotNumber) {
			double newInteraction = timeCapacityInteractionCoefficients.get(area.getId()) - stepSize * loss
					* overallAccepted / ((double) maximalAcceptablePerTimeWindow * (double) numberOfTimeWindows)
					* ((double) time) / ((double) orderHorizonLength);
			timeCapacityInteractionCoefficients.put(area.getId(), newInteraction);
			weight += "tc:" + newInteraction + ";";
		} else if (timeCapacityInteractionCoefficients.get(area.getId()) != null) {
			double newInteraction = timeCapacityInteractionCoefficients.get(area.getId()) - stepSize * loss
					* overallCosts / overallAcceptableCosts * ((double) time) / ((double) orderHorizonLength);
			timeCapacityInteractionCoefficients.put(area.getId(), newInteraction);
			weight += "tc:" + newInteraction + ";";
		}

		if (time % 100 == 0)
			weightLog.get(requestSetId).add(weight);

	}

	public static void updateTabularTimeDependentValueFunctionApproximation(Object[] timeCoefficients,
			int[] currentState, int time, int orderHorizonLength, double newValue,
			HashMap<Integer, ArrayList<Double>> lossLog, int requestSetId, double stepSize) {
		Double oldValue = evaluateStateForTabularValueFunctionApproximation(timeCoefficients, currentState, time,
				orderHorizonLength);

		double loss = oldValue - newValue;
		if (time % 100 == 0)
			lossLog.get(requestSetId).add(loss);
		Pair<Double, Double> values = MultiDimensionalArrayProducer.readDoublePairArray(timeCoefficients, currentState);
		Double newBasic = values.getKey() - stepSize * loss;
		double newTime = values.getValue() - stepSize * loss * ((double) time) / ((double) orderHorizonLength);
		MultiDimensionalArrayProducer.writeToDoublePairArray(new Pair<Double, Double>(newBasic, newTime),
				timeCoefficients, currentState);
	}

	public static void updateTabularValueFunctionApproximation(Object[] coefficients, int[] currentState, int time,
			int orderHorizonLength, double newValue, HashMap<Integer, ArrayList<Double>> lossLog, int requestSetId,
			double stepSize) {
		Double oldValue = evaluateStateForTabularValueFunctionApproximation(coefficients, currentState, time,
				orderHorizonLength);

		double loss = oldValue - newValue;
		if (time % 100 == 0)
			lossLog.get(requestSetId).add(loss);
		Double value = oldValue - stepSize * loss;
		MultiDimensionalArrayProducer.writeToDoubleArray(value, coefficients, currentState);
	}
}
