package logic.utility;

import java.util.ArrayList;
import java.util.HashMap;

import data.entity.AlternativeSet;
import data.entity.CapacitySet;
import data.entity.ControlSet;
import data.entity.DeliveryAreaSet;
import data.entity.DemandSegmentForecastSet;
import data.entity.DemandSegmentSet;
import data.entity.DemandSegmentWeighting;
import data.entity.DynamicProgrammingTree;
import data.entity.Experiment;
import data.entity.Node;
import data.entity.NodeDistance;
import data.entity.OrderRequestSet;
import data.entity.OrderSet;
import data.entity.ProbabilityDistribution;
import data.entity.Routing;
import data.entity.RoutingAssignment;
import data.entity.SetEntity;
import data.entity.TimeWindowSet;
import data.entity.TravelTimeSet;
import data.entity.ValueBucketForecastSet;
import data.entity.ValueBucketSet;
import data.entity.ValueFunctionApproximationModelSet;
import data.entity.Vehicle;
import data.entity.VehicleAreaAssignmentSet;
import data.utility.DataServiceProvider;
import logic.service.support.ParameterService;

/**
 * Helper that loads needed input settings
 * 
 * @author M. Lang
 *
 */
public class InputPreparator {

	public static ArrayList<OrderSet> getHistoricalOrders(int periodNumber) {

		ArrayList<OrderSet> historicalOrders = new ArrayList<OrderSet>();
		int currentPeriod = 0;
		while (currentPeriod <= periodNumber) {
			if (currentPeriod == 0) {
				historicalOrders.add((OrderSet) DataServiceProvider.getOrderDataServiceImplInstance()
						.getSetById(SettingsProvider.getPeriodSetting().getHistoricalOrderSetId()));
			} else {// If it is not the first period, the respective orderset of
					// the last period is the historical order set
				if (periodNumber == 1) {
					historicalOrders.add((OrderSet) DataServiceProvider.getOrderDataServiceImplInstance()
							.getSetById(SettingsProvider.getPeriodSetting().getOrderSetId()));
				} else {
					historicalOrders.add((OrderSet) DataServiceProvider.getOrderDataServiceImplInstance()
							.getSetById(SettingsProvider.getPeriodSettingFollower(currentPeriod - 1).getOrderSetId()));
				}
			}

			currentPeriod++;
		}

		return historicalOrders;
	}

	public static ValueBucketForecastSet getHistoricalDemandForecastSet(int periodNumber) {
		ValueBucketForecastSet historicalDemandForecastSet;
		if (periodNumber == 0) { // First period: explicit historical demand
									// forecast set
			if (SettingsProvider.getPeriodSetting().getHistoricalDemandForecastValueBucketsSetId() == null
					|| SettingsProvider.getPeriodSetting().getHistoricalDemandForecastValueBucketsSetId() == 0) {
				historicalDemandForecastSet = null;
			} else {
				historicalDemandForecastSet = (ValueBucketForecastSet) DataServiceProvider
						.getValueBucketForecastDataServiceImplInstance()
						.getSetById(SettingsProvider.getPeriodSetting().getHistoricalDemandForecastValueBucketsSetId());
			}
		} else {// If it is not the first period, the respective forecastset of
				// the last period is the historical forecast set

			if (periodNumber == 1) {
				historicalDemandForecastSet = (ValueBucketForecastSet) DataServiceProvider
						.getValueBucketForecastDataServiceImplInstance()
						.getSetById(SettingsProvider.getPeriodSetting().getValueBucketForecastSetId());
			} else {
				historicalDemandForecastSet = (ValueBucketForecastSet) DataServiceProvider
						.getValueBucketForecastDataServiceImplInstance().getSetById(SettingsProvider
								.getPeriodSettingFollower(periodNumber - 1).getValueBucketForecastSetId());
			}

		}

		return historicalDemandForecastSet;
	}

	
	public static ProbabilityDistribution getArrivalProbabilityDistribution(int periodNumber){
		ProbabilityDistribution arrDistribution; 
		
		if (periodNumber == 0) {
			if (SettingsProvider.getPeriodSetting().getArrivalProbabilityDistributionId() == null
					|| SettingsProvider.getPeriodSetting().getArrivalProbabilityDistributionId() == 0) {
				arrDistribution = null;
			} else {
				arrDistribution =(ProbabilityDistribution) DataServiceProvider
						.getProbabilityDistributionDataServiceImplInstance()
						.getById(SettingsProvider.getPeriodSetting().getArrivalProbabilityDistributionId());
			}
		} else {
			if (SettingsProvider.getPeriodSettingFollower(periodNumber).getArrivalProbabilityDistributionId() == null
					|| SettingsProvider.getPeriodSetting().getArrivalProbabilityDistributionId() == 0) {
				arrDistribution = null;
			} else {
				arrDistribution = (ProbabilityDistribution) DataServiceProvider
						.getProbabilityDistributionDataServiceImplInstance()
						.getById(SettingsProvider.getPeriodSettingFollower(periodNumber).getArrivalProbabilityDistributionId());
			}
		}
		
		return arrDistribution;
	}
	/**
	 * Provides value bucket set defined in the period settings. Null if no
	 * setting was defined for the respective period.
	 * 
	 * @param periodNumber
	 * @return
	 */
	public static ValueBucketSet getValueBucketSet(int periodNumber) {
		ValueBucketSet valueBucketSet;
		if (periodNumber == 0) {
			if (SettingsProvider.getPeriodSetting().getValueBucketSetId() == null
					|| SettingsProvider.getPeriodSetting().getValueBucketSetId() == 0) {
				valueBucketSet = null;
			} else {
				valueBucketSet = (ValueBucketSet) DataServiceProvider.getValueBucketDataServiceImplInstance()
						.getSetById(SettingsProvider.getPeriodSetting().getValueBucketSetId());
			}
		} else {
			if (SettingsProvider.getPeriodSettingFollower(periodNumber).getValueBucketSetId() == null
					|| SettingsProvider.getPeriodSetting().getValueBucketSetId() == 0) {
				valueBucketSet = null;
			} else {
				valueBucketSet = (ValueBucketSet) DataServiceProvider.getValueBucketDataServiceImplInstance()
						.getSetById(SettingsProvider.getPeriodSettingFollower(periodNumber).getValueBucketSetId());
			}
		}
		return valueBucketSet;
	}

	/**
	 * Provides value function model set defined in the period settings. Null if
	 * no setting was defined for the respective period.
	 * 
	 * @param periodNumber
	 * @return
	 */
	public static ValueFunctionApproximationModelSet getValueFunctionApproximationModelSet(int periodNumber) {
		ValueFunctionApproximationModelSet valueFunctionApproximationModelSet;
		if (periodNumber == 0) {
			if (SettingsProvider.getPeriodSetting().getValueFunctionModelSetId() == null
					|| SettingsProvider.getPeriodSetting().getValueFunctionModelSetId() == 0) {
				valueFunctionApproximationModelSet = null;
			} else {
				valueFunctionApproximationModelSet = (ValueFunctionApproximationModelSet) DataServiceProvider
						.getValueFunctionApproximationDataServiceImplInstance()
						.getSetById(SettingsProvider.getPeriodSetting().getValueFunctionModelSetId());
			}
		} else {
			if (SettingsProvider.getPeriodSettingFollower(periodNumber).getValueFunctionModelSetId() == null
					|| SettingsProvider.getPeriodSetting().getValueFunctionModelSetId() == 0) {
				valueFunctionApproximationModelSet = null;
			} else {
				valueFunctionApproximationModelSet = (ValueFunctionApproximationModelSet) DataServiceProvider
						.getValueFunctionApproximationDataServiceImplInstance().getSetById(
								SettingsProvider.getPeriodSettingFollower(periodNumber).getValueFunctionModelSetId());
			}
		}
		return valueFunctionApproximationModelSet;
	}

	/**
	 * Provides vehicle area assignment set defined in the period settings. Null
	 * if no setting was defined for the respective period.
	 * 
	 * @param periodNumber
	 * @return
	 */
	public static VehicleAreaAssignmentSet getVehicleAreaAssignmentSet(int periodNumber) {
		VehicleAreaAssignmentSet vehicleAreaAssignmentSet;
		if (periodNumber == 0) {
			if (SettingsProvider.getPeriodSetting().getVehicleAssignmentSetId() == null
					|| SettingsProvider.getPeriodSetting().getVehicleAssignmentSetId() == 0) {
				vehicleAreaAssignmentSet = null;
			} else {
				vehicleAreaAssignmentSet = (VehicleAreaAssignmentSet) DataServiceProvider
						.getVehicleAssignmentDataServiceImplInstance()
						.getSetById(SettingsProvider.getPeriodSetting().getVehicleAssignmentSetId());
			}
		} else {
			if (SettingsProvider.getPeriodSettingFollower(periodNumber).getVehicleAssignmentSetId() == null
					|| SettingsProvider.getPeriodSetting().getVehicleAssignmentSetId() == 0) {
				vehicleAreaAssignmentSet = null;
			} else {
				vehicleAreaAssignmentSet = (VehicleAreaAssignmentSet) DataServiceProvider
						.getVehicleAssignmentDataServiceImplInstance().getSetById(
								SettingsProvider.getPeriodSettingFollower(periodNumber).getVehicleAssignmentSetId());
			}
		}
		return vehicleAreaAssignmentSet;
	}

	/**
	 * Provides learning order request list defined in the period settings. Null
	 * if no setting was defined for the respective period.
	 * 
	 * @param periodNumber
	 * @return
	 */
	public static ArrayList<OrderRequestSet> getLearningOrderRequestSets(int periodNumber) {
		ArrayList<OrderRequestSet> orderRequestSetsForLearning;
		if (periodNumber == 0) {
			if (SettingsProvider.getPeriodSetting().getLearningOutputRequestsExperimentId() == null
					|| SettingsProvider.getPeriodSetting().getLearningOutputRequestsExperimentId() == 0) {
				orderRequestSetsForLearning = null;
			} else {
//				ArrayList<? extends SetEntity> test = DataServiceProvider.getOrderRequestDataServiceImplInstance()
//						.getAllOrderRequestSetsByExperimentId(
//								SettingsProvider.getPeriodSetting().getLearningOutputRequestsExperimentId());
				orderRequestSetsForLearning = (ArrayList<OrderRequestSet>) DataServiceProvider.getOrderRequestDataServiceImplInstance()
						.getAllOrderRequestSetsByExperimentId(
								SettingsProvider.getPeriodSetting().getLearningOutputRequestsExperimentId());
			}
		} else {
			if (SettingsProvider.getPeriodSettingFollower(periodNumber).getLearningOutputRequestsExperimentId() == null
					|| SettingsProvider.getPeriodSetting().getLearningOutputRequestsExperimentId() == 0) {
				orderRequestSetsForLearning = null;
			} else {
				orderRequestSetsForLearning = (ArrayList<OrderRequestSet>) DataServiceProvider.getOrderRequestDataServiceImplInstance()
						.getAllOrderRequestSetsByExperimentId(SettingsProvider.getPeriodSettingFollower(periodNumber)
								.getLearningOutputRequestsExperimentId());
			}
		}
		return orderRequestSetsForLearning;
	}
	/**
	 * Provides learning order set list defined in the period settings. Null if no
	 * setting was defined for the respective period.
	 * 
	 * @param periodNumber
	 * @return
	 */
	public static HashMap<Integer, ArrayList<OrderSet>> getLearningOrderSets(int periodNumber) {

		HashMap<Integer, ArrayList<OrderSet>> orderSetsForLearning = new HashMap<Integer, ArrayList<OrderSet>>();
		if (periodNumber == 0) {
			if (SettingsProvider.getPeriodSetting().getLearningOutputOrderSetsExperimentIds() == null) {
				orderSetsForLearning = null;
			} else {

				for (Integer id : SettingsProvider.getPeriodSetting().getLearningOutputOrderSetsExperimentIds()){
					Experiment e = DataServiceProvider.getSettingsDataServiceImplInstance().getExperimentById(id);
					if(e.getCopyExperimentId()!=null && e.getCopyExperimentId()!=0){
						if(!orderSetsForLearning.containsKey(e.getCopyExperimentId())){
							orderSetsForLearning.put(e.getCopyExperimentId(),new ArrayList<OrderSet>());
						}
						orderSetsForLearning.get(e.getCopyExperimentId()).addAll(DataServiceProvider.getOrderDataServiceImplInstance().getAllOrderSetsByExperimentId(id));
						
					}else{
						
						if(!orderSetsForLearning.containsKey(id)){
							orderSetsForLearning.put(id,new ArrayList<OrderSet>());
						}
						orderSetsForLearning.get(id).addAll(DataServiceProvider.getOrderDataServiceImplInstance().getAllOrderSetsByExperimentId(id));
					}
					
				}
					
			}
		} else {
			if (SettingsProvider.getPeriodSettingFollower(periodNumber)
					.getLearningOutputFinalRoutingsExperimentIds() == null) {
				orderSetsForLearning = null;
			} else {
				for (Integer id : SettingsProvider.getPeriodSettingFollower(periodNumber)
						.getLearningOutputFinalRoutingsExperimentIds()){
					Experiment e = DataServiceProvider.getSettingsDataServiceImplInstance().getExperimentById(id);
					if(e.getCopyExperimentId()!=null && e.getCopyExperimentId()!=0){
						if(!orderSetsForLearning.containsKey(e.getCopyExperimentId())){
							orderSetsForLearning.put(e.getCopyExperimentId(),new ArrayList<OrderSet>());
						}
						orderSetsForLearning.get(e.getCopyExperimentId()).addAll(DataServiceProvider.getOrderDataServiceImplInstance().getAllOrderSetsByExperimentId(id));
								
					}else{
						
						if(!orderSetsForLearning.containsKey(id)){
							orderSetsForLearning.put(id,new ArrayList<OrderSet>());
						}
						orderSetsForLearning.get(id).addAll(DataServiceProvider.getOrderDataServiceImplInstance().getAllOrderSetsByExperimentId(id));
					}
				}
					
			}
		}
		return orderSetsForLearning;
	}

	/**
	 * Provides learning routing list defined in the period settings. Null if no
	 * setting was defined for the respective period.
	 * 
	 * @param periodNumber
	 * @return
	 */
	public static HashMap<Integer, ArrayList<Routing>> getLearningFinalRoutings(int periodNumber) {

		HashMap<Integer, ArrayList<Routing>> routingsForLearning = new HashMap<Integer, ArrayList<Routing>>();
		if (periodNumber == 0) {
			if (SettingsProvider.getPeriodSetting().getLearningOutputFinalRoutingsExperimentIds() == null) {
				routingsForLearning = null;
			} else {

				for (Integer id : SettingsProvider.getPeriodSetting().getLearningOutputFinalRoutingsExperimentIds()){
					Experiment e = DataServiceProvider.getSettingsDataServiceImplInstance().getExperimentById(id);
					if(e.getCopyExperimentId()!=null && e.getCopyExperimentId()!=0){
						if(!routingsForLearning.containsKey(e.getCopyExperimentId())){
							routingsForLearning.put(e.getCopyExperimentId(),new ArrayList<Routing>());
						}
						routingsForLearning.get(e.getCopyExperimentId()).addAll(DataServiceProvider.getRoutingDataServiceImplInstance().getAllRoutingsByExperimentId(id));
								
					}else{
						
						if(!routingsForLearning.containsKey(id)){
							routingsForLearning.put(id,new ArrayList<Routing>());
						}
						routingsForLearning.get(id).addAll(DataServiceProvider.getRoutingDataServiceImplInstance().getAllRoutingsByExperimentId(id));
					}
					
				}
					
			}
		} else {
			if (SettingsProvider.getPeriodSettingFollower(periodNumber)
					.getLearningOutputFinalRoutingsExperimentIds() == null) {
				routingsForLearning = null;
			} else {
				for (Integer id : SettingsProvider.getPeriodSettingFollower(periodNumber)
						.getLearningOutputFinalRoutingsExperimentIds()){
					Experiment e = DataServiceProvider.getSettingsDataServiceImplInstance().getExperimentById(id);
					if(e.getCopyExperimentId()!=null && e.getCopyExperimentId()!=0){
						if(!routingsForLearning.containsKey(e.getCopyExperimentId())){
							routingsForLearning.put(e.getCopyExperimentId(),new ArrayList<Routing>());
						}
						routingsForLearning.get(e.getCopyExperimentId()).addAll(DataServiceProvider.getRoutingDataServiceImplInstance().getAllRoutingsByExperimentId(id));
								
					}else{
						
						if(!routingsForLearning.containsKey(id)){
							routingsForLearning.put(id,new ArrayList<Routing>());
						}
						routingsForLearning.get(id).addAll(DataServiceProvider.getRoutingDataServiceImplInstance().getAllRoutingsByExperimentId(id));
					}
				}
					
			}
		}
		return routingsForLearning;
	}
	
	/**
	 * Provides benchmarking routing list defined in the period settings. Null if no
	 * setting was defined for the respective period.
	 * 
	 * @param periodNumber
	 * @return
	 */
	public static HashMap<Integer, ArrayList<Routing>> getBenchmarkingFinalRoutings(int periodNumber) {

		HashMap<Integer, ArrayList<Routing>> routingsForBenchmarking = new HashMap<Integer, ArrayList<Routing>>();
		if (periodNumber == 0) {
			if (SettingsProvider.getPeriodSetting().getBenchmarkingOutputFinalRoutingsExperimentIds() == null) {
				routingsForBenchmarking = null;
			} else {

				for (Integer id : SettingsProvider.getPeriodSetting().getBenchmarkingOutputFinalRoutingsExperimentIds()){
					
					
					Experiment e = DataServiceProvider.getSettingsDataServiceImplInstance().getExperimentById(id);
					if(e.getCopyExperimentId()!=null && e.getCopyExperimentId()!=0){
						if(!routingsForBenchmarking.containsKey(e.getCopyExperimentId())){
							routingsForBenchmarking.put(e.getCopyExperimentId(),new ArrayList<Routing>());
						}
						routingsForBenchmarking.get(e.getCopyExperimentId()).addAll(DataServiceProvider.getRoutingDataServiceImplInstance().getAllRoutingsByExperimentId(id));
								
					}else{
						
						if(!routingsForBenchmarking.containsKey(id)){
							routingsForBenchmarking.put(id,new ArrayList<Routing>());
						}
						routingsForBenchmarking.get(id).addAll(DataServiceProvider.getRoutingDataServiceImplInstance().getAllRoutingsByExperimentId(id));
					}
				}

			}
		} else {
			if (SettingsProvider.getPeriodSettingFollower(periodNumber)
					.getBenchmarkingOutputFinalRoutingsExperimentIds() == null) {
				routingsForBenchmarking = null;
			} else {
				for (Integer id : SettingsProvider.getPeriodSettingFollower(periodNumber)
						.getBenchmarkingOutputFinalRoutingsExperimentIds()){
					Experiment e = DataServiceProvider.getSettingsDataServiceImplInstance().getExperimentById(id);
					if(e.getCopyExperimentId()!=null && e.getCopyExperimentId()!=0){
						if(!routingsForBenchmarking.containsKey(e.getCopyExperimentId())){
							routingsForBenchmarking.put(e.getCopyExperimentId(),new ArrayList<Routing>());
						}
						routingsForBenchmarking.get(e.getCopyExperimentId()).addAll(DataServiceProvider.getRoutingDataServiceImplInstance().getAllRoutingsByExperimentId(id));
								
					}else{
						
						if(!routingsForBenchmarking.containsKey(id)){
							routingsForBenchmarking.put(id,new ArrayList<Routing>());
						}
						routingsForBenchmarking.get(id).addAll(DataServiceProvider.getRoutingDataServiceImplInstance().getAllRoutingsByExperimentId(id));
					}
				}
				
			}
		}
		return routingsForBenchmarking;
	}
	
	/**
	 * Provides benchmarking order set list defined in the period settings. Null if no
	 * setting was defined for the respective period.
	 * 
	 * @param periodNumber
	 * @return
	 */
	public static HashMap<Integer, ArrayList<OrderSet>> getBenchmarkingOrderSets(int periodNumber) {

		HashMap<Integer, ArrayList<OrderSet>> orderSetsForBenchmarking = new HashMap<Integer, ArrayList<OrderSet>>();
		if (periodNumber == 0) {
			if (SettingsProvider.getPeriodSetting().getBenchmarkingOutputOrderSetsExperimentIds() == null) {
				orderSetsForBenchmarking = null;
			} else {

				for (Integer id : SettingsProvider.getPeriodSetting().getBenchmarkingOutputOrderSetsExperimentIds()){
					
					
					Experiment e = DataServiceProvider.getSettingsDataServiceImplInstance().getExperimentById(id);
					if(e.getCopyExperimentId()!=null && e.getCopyExperimentId()!=0){
						if(!orderSetsForBenchmarking.containsKey(e.getCopyExperimentId())){
							orderSetsForBenchmarking.put(e.getCopyExperimentId(),new ArrayList<OrderSet>());
						}
						orderSetsForBenchmarking.get(e.getCopyExperimentId()).addAll(DataServiceProvider.getOrderDataServiceImplInstance().getAllOrderSetsByExperimentId(id));
								
					}else{
						
						if(!orderSetsForBenchmarking.containsKey(id)){
							orderSetsForBenchmarking.put(id,new ArrayList<OrderSet>());
						}
						orderSetsForBenchmarking.get(id).addAll(DataServiceProvider.getOrderDataServiceImplInstance().getAllOrderSetsByExperimentId(id));
					}
				}

			}
		} else {
			if (SettingsProvider.getPeriodSettingFollower(periodNumber)
					.getBenchmarkingOutputOrderSetsExperimentIds() == null) {
				orderSetsForBenchmarking = null;
			} else {
				for (Integer id : SettingsProvider.getPeriodSettingFollower(periodNumber)
						.getBenchmarkingOutputOrderSetsExperimentIds()){
					Experiment e = DataServiceProvider.getSettingsDataServiceImplInstance().getExperimentById(id);
					if(e.getCopyExperimentId()!=null && e.getCopyExperimentId()!=0){
						if(!orderSetsForBenchmarking.containsKey(e.getCopyExperimentId())){
							orderSetsForBenchmarking.put(e.getCopyExperimentId(),new ArrayList<OrderSet>());
						}
						orderSetsForBenchmarking.get(e.getCopyExperimentId()).addAll(DataServiceProvider.getOrderDataServiceImplInstance().getAllOrderSetsByExperimentId(id));
								
					}else{
						
						if(!orderSetsForBenchmarking.containsKey(id)){
							orderSetsForBenchmarking.put(id,new ArrayList<OrderSet>());
						}
						orderSetsForBenchmarking.get(id).addAll(DataServiceProvider.getOrderDataServiceImplInstance().getAllOrderSetsByExperimentId(id));
					}
				}
				
			}
		}
		return orderSetsForBenchmarking;
	}

	/**
	 * Provides capacity set defined in the period settings. Null if no setting
	 * was defined for the respective period.
	 * 
	 * @param periodNumber
	 * @return
	 */
	public static CapacitySet getCapacitySet(int periodNumber) {
		CapacitySet capacitySet;
		if (periodNumber == 0) {
			if (SettingsProvider.getPeriodSetting().getCapacitySetId() == null
					|| SettingsProvider.getPeriodSetting().getCapacitySetId() == 0) {
				capacitySet = null;
			} else {
				capacitySet = (CapacitySet) DataServiceProvider.getCapacityDataServiceImplInstance()
						.getSetById(SettingsProvider.getPeriodSetting().getCapacitySetId());
			}

		} else {

			if (SettingsProvider.getPeriodSettingFollower(periodNumber).getCapacitySetId() == null
					|| SettingsProvider.getPeriodSetting().getCapacitySetId() == 0) {
				capacitySet = null;
			} else {
				capacitySet = (CapacitySet) DataServiceProvider.getCapacityDataServiceImplInstance()
						.getSetById(SettingsProvider.getPeriodSettingFollower(periodNumber).getCapacitySetId());
			}

		}
		return capacitySet;
	}

	/**
	 * Provides order set defined in the period settings. Null if no setting
	 * was defined for the respective period.
	 * 
	 * @param periodNumber
	 * @return
	 */
	public static OrderSet getOrderSet(int periodNumber) {
		OrderSet orderSet;
		if (periodNumber == 0) {
			if (SettingsProvider.getPeriodSetting().getOrderSetId() == null
					|| SettingsProvider.getPeriodSetting().getOrderSetId() == 0) {
				orderSet = null;
			} else {
				orderSet = (OrderSet) DataServiceProvider.getOrderDataServiceImplInstance()
						.getSetById(SettingsProvider.getPeriodSetting().getOrderSetId());
			}

		} else {

			if (SettingsProvider.getPeriodSettingFollower(periodNumber).getOrderSetId() == null
					|| SettingsProvider.getPeriodSetting().getOrderSetId() == 0) {
				orderSet = null;
			} else {
				orderSet = (OrderSet) DataServiceProvider.getOrderDataServiceImplInstance()
						.getSetById(SettingsProvider.getPeriodSettingFollower(periodNumber).getOrderSetId());
			}

		}
		return orderSet;
	}
	
	/**
	 * Provides control set defined in the period settings. Null if no setting
	 * was defined for the respective period.
	 * 
	 * @param periodNumber
	 * @return
	 */
	public static ControlSet getControlSet(int periodNumber) {
		ControlSet controlSet;
		if (periodNumber == 0) {
			if (SettingsProvider.getPeriodSetting().getControlSetId() == null
					|| SettingsProvider.getPeriodSetting().getControlSetId() == 0) {
				controlSet = null;
			} else {
				controlSet = (ControlSet) DataServiceProvider.getControlDataServiceImplInstance()
						.getSetById(SettingsProvider.getPeriodSetting().getControlSetId());
			}

		} else {

			if (SettingsProvider.getPeriodSettingFollower(periodNumber).getControlSetId() == null
					|| SettingsProvider.getPeriodSetting().getControlSetId() == 0) {
				controlSet = null;
			} else {
				controlSet = (ControlSet) DataServiceProvider.getControlDataServiceImplInstance()
						.getSetById(SettingsProvider.getPeriodSettingFollower(periodNumber).getControlSetId());
			}

		}
		return controlSet;
	}

	/**
	 * Provides dynamic programming tree defined in the period settings. Null if
	 * no setting was defined for the respective period.
	 * 
	 * @param periodNumber
	 * @return
	 */
	public static DynamicProgrammingTree getDynamicProgrammingTreeSet(int periodNumber) {
		DynamicProgrammingTree dpt;
		if (periodNumber == 0) {
			if (SettingsProvider.getPeriodSetting().getDynamicProgrammingTreeId() == null
					|| SettingsProvider.getPeriodSetting().getDynamicProgrammingTreeId() == 0) {
				dpt = null;
			} else {
				dpt = DataServiceProvider.getControlDataServiceImplInstance().getDynamicProgrammingTreeById(
						SettingsProvider.getPeriodSetting().getDynamicProgrammingTreeId());
			}

		} else {

			if (SettingsProvider.getPeriodSettingFollower(periodNumber).getDynamicProgrammingTreeId() == null
					|| SettingsProvider.getPeriodSetting().getDynamicProgrammingTreeId() == 0) {
				dpt = null;
			} else {
				dpt = DataServiceProvider.getControlDataServiceImplInstance().getDynamicProgrammingTreeById(
						SettingsProvider.getPeriodSettingFollower(periodNumber).getDynamicProgrammingTreeId());
			}

		}
		return dpt;
	}

	/**
	 * Provides DemandSegmentWeighting defined in the period settings. Null if
	 * no setting was defined for the respective period.
	 * 
	 * @param periodNumber
	 * @return
	 */
	public static DemandSegmentWeighting getDemandSegmentWeighting(int periodNumber) {
		DemandSegmentWeighting demandSegmentWeighting;
		if (periodNumber == 0) {
			if (SettingsProvider.getPeriodSetting().getDemandSegmentWeightingId() == null
					|| SettingsProvider.getPeriodSetting().getDemandSegmentWeightingId() == 0) {
				demandSegmentWeighting = null;
			} else {
				demandSegmentWeighting = (DemandSegmentWeighting) DataServiceProvider
						.getDemandSegmentDataServiceImplInstance()
						.getWeightingById(SettingsProvider.getPeriodSetting().getDemandSegmentWeightingId());
			}

		} else {

			if (SettingsProvider.getPeriodSettingFollower(periodNumber).getDemandSegmentWeightingId() == null
					|| SettingsProvider.getPeriodSetting().getDemandSegmentWeightingId() == 0) {
				demandSegmentWeighting = null;
			} else {
				demandSegmentWeighting = (DemandSegmentWeighting) DataServiceProvider
						.getDemandSegmentDataServiceImplInstance().getWeightingById(
								SettingsProvider.getPeriodSettingFollower(periodNumber).getDemandSegmentWeightingId());
			}

		}
		return demandSegmentWeighting;
	}

	/**
	 * Provides DemandSegmentSet defined in the period settings. Null if no
	 * setting was defined for the respective period.
	 * 
	 * @param periodNumber
	 * @return
	 */
	public static DemandSegmentSet getDemandSegmentSet(int periodNumber) {
		DemandSegmentSet demandSegmentSet;
		if (periodNumber == 0) {
			if (SettingsProvider.getPeriodSetting().getDemandSegmentSetId() == null
					|| SettingsProvider.getPeriodSetting().getDemandSegmentSetId() == 0) {
				demandSegmentSet = null;
			} else {
				demandSegmentSet = (DemandSegmentSet) DataServiceProvider.getDemandSegmentDataServiceImplInstance()
						.getSetById(SettingsProvider.getPeriodSetting().getDemandSegmentSetId());
			}

		} else {

			if (SettingsProvider.getPeriodSettingFollower(periodNumber).getDemandSegmentSetId() == null
					|| SettingsProvider.getPeriodSetting().getDemandSegmentSetId() == 0) {
				demandSegmentSet = null;
			} else {
				demandSegmentSet = (DemandSegmentSet) DataServiceProvider.getDemandSegmentDataServiceImplInstance()
						.getSetById(SettingsProvider.getPeriodSettingFollower(periodNumber).getDemandSegmentSetId());
			}

		}
		return demandSegmentSet;
	}

	/**
	 * Provides value bucket forecast set defined in the period settings. Null
	 * if no setting was defined for the respective period.
	 * 
	 * @param periodNumber
	 * @return
	 */
	public static ValueBucketForecastSet getValueBucketForecastSet(int periodNumber) {
		ValueBucketForecastSet demandForecastSet;
		if (periodNumber == 0) {
			if (SettingsProvider.getPeriodSetting().getValueBucketForecastSetId() == null
					|| SettingsProvider.getPeriodSetting().getValueBucketForecastSetId() == 0) {
				demandForecastSet = null;
			} else {
				demandForecastSet = (ValueBucketForecastSet) DataServiceProvider
						.getValueBucketForecastDataServiceImplInstance()
						.getSetById(SettingsProvider.getPeriodSetting().getValueBucketForecastSetId());
			}

		} else {

			if (SettingsProvider.getPeriodSettingFollower(periodNumber).getValueBucketForecastSetId() == null
					|| SettingsProvider.getPeriodSetting().getValueBucketForecastSetId() == 0) {
				demandForecastSet = null;
			} else {
				demandForecastSet = (ValueBucketForecastSet) DataServiceProvider
						.getValueBucketForecastDataServiceImplInstance().getSetById(
								SettingsProvider.getPeriodSettingFollower(periodNumber).getValueBucketForecastSetId());
			}

		}
		return demandForecastSet;
	}

	/**
	 * Provides demand segment forecast set defined in the period settings. Null
	 * if no setting was defined for the respective period.
	 * 
	 * @param periodNumber
	 * @return
	 */
	public static DemandSegmentForecastSet getDemandSegmentForecastSet(int periodNumber) {
		DemandSegmentForecastSet demandForecastSet;
		if (periodNumber == 0) {
			if (SettingsProvider.getPeriodSetting().getDemandSegmentForecastSetId() == null
					|| SettingsProvider.getPeriodSetting().getDemandSegmentForecastSetId() == 0) {
				demandForecastSet = null;
			} else {
				demandForecastSet = (DemandSegmentForecastSet) DataServiceProvider
						.getDemandSegmentForecastDataServiceImplInstance()
						.getSetById(SettingsProvider.getPeriodSetting().getDemandSegmentForecastSetId());
			}

		} else {

			if (SettingsProvider.getPeriodSettingFollower(periodNumber).getDemandSegmentForecastSetId() == null
					|| SettingsProvider.getPeriodSetting().getDemandSegmentForecastSetId() == 0) {
				demandForecastSet = null;
			} else {
				demandForecastSet = (DemandSegmentForecastSet) DataServiceProvider
						.getDemandSegmentForecastDataServiceImplInstance().getSetById(SettingsProvider
								.getPeriodSettingFollower(periodNumber).getDemandSegmentForecastSetId());
			}

		}
		return demandForecastSet;
	}

	/**
	 * Provides travel time set defined in the period settings. Null if no
	 * setting was defined for the respective period.
	 * 
	 * @param periodNumber
	 * @return
	 */
	public static TravelTimeSet getTravelTimeSet(int periodNumber) {
		TravelTimeSet travelTimeSet;
		if (periodNumber == 0) {
			if (SettingsProvider.getPeriodSetting().getTravelTimeSetId() == null
					|| SettingsProvider.getPeriodSetting().getTravelTimeSetId() == 0) {
				travelTimeSet = null;
			} else {
				travelTimeSet = (TravelTimeSet) DataServiceProvider.getTravelTimeDataServiceImplInstance()
						.getSetById(SettingsProvider.getPeriodSetting().getTravelTimeSetId());
			}

		} else {

			if (SettingsProvider.getPeriodSettingFollower(periodNumber).getTravelTimeSetId() == null
					|| SettingsProvider.getPeriodSetting().getTravelTimeSetId() == 0) {
				travelTimeSet = null;
			} else {
				travelTimeSet = (TravelTimeSet) DataServiceProvider.getTravelTimeDataServiceImplInstance()
						.getSetById(SettingsProvider.getPeriodSettingFollower(periodNumber).getTravelTimeSetId());
			}

		}
		return travelTimeSet;
	}

	/**
	 * Provides vehicles defined in the period settings. Null if no setting was
	 * defined for the respective period.
	 * 
	 * @param periodNumber
	 * @return
	 */
	public static ArrayList<Vehicle> getVehicles(int periodNumber) {
		ArrayList<Vehicle> vehicles;
		if (periodNumber == 0) {
			vehicles = SettingsProvider.getPeriodSetting().getVehicles();
		} else {

			vehicles = SettingsProvider.getPeriodSettingFollower(periodNumber).getVehicles();

		}
		return vehicles;
	}

	/**
	 * Provides order request set defined in the period settings. Null if no
	 * setting was defined for the respective period.
	 * 
	 * @param periodNumber
	 * @return
	 */
	public static OrderRequestSet getOrderRequestSet(int periodNumber) {
		OrderRequestSet orderRequestSet;
		if (periodNumber == 0) {
			if (SettingsProvider.getPeriodSetting().getOrderRequestSetId() == null
					|| SettingsProvider.getPeriodSetting().getOrderRequestSetId() == 0) {
				orderRequestSet = null;
			} else {
				orderRequestSet = (OrderRequestSet) DataServiceProvider.getOrderRequestDataServiceImplInstance()
						.getSetById(SettingsProvider.getPeriodSetting().getOrderRequestSetId());
			}

		} else {

			if (SettingsProvider.getPeriodSettingFollower(periodNumber).getOrderRequestSetId() == null
					|| SettingsProvider.getPeriodSetting().getOrderRequestSetId() == 0) {
				orderRequestSet = null;
			} else {
				orderRequestSet = (OrderRequestSet) DataServiceProvider.getOrderRequestDataServiceImplInstance()
						.getSetById(SettingsProvider.getPeriodSettingFollower(periodNumber).getOrderRequestSetId());
			}

		}
		return orderRequestSet;
	}

	/**
	 * Provides delivery area set which is constant over periods and assigned
	 * beforehand TODO: Reconsider if reasonable. Adapt once delivery areas are
	 * defined dynamically.
	 * 
	 * @param periodNumber
	 * @return
	 */
	public static DeliveryAreaSet getDeliveryAreaSet(int periodNumber) {
		DeliveryAreaSet deliveryAreaSet = (DeliveryAreaSet) DataServiceProvider.getDeliveryAreaDataServiceImplInstance()
				.getSetById(SettingsProvider.getPeriodSetting().getDeliveryAreaSetId());

		return deliveryAreaSet;
	}

	/**
	 * Provides alternative set which is constant over periods and assigned
	 * beforehand TODO: Reconsider if reasonable. Adapt once alternatives are
	 * defined dynamically.
	 * 
	 * @param periodNumber
	 * @return
	 */
	public static AlternativeSet getAlternativeSet(int periodNumber) {
		AlternativeSet alternativeSet = (AlternativeSet) DataServiceProvider.getAlternativeDataServiceImplInstance()
				.getSetById(SettingsProvider.getPeriodSetting().getAlternativeSetId());

		return alternativeSet;
	}

	/**
	 * Provides time window set which is constant over periods and assigned
	 * beforehand TODO: Reconsider if reasonable. Adapt once alternatives are
	 * defined dynamically.
	 * 
	 * @param periodNumber
	 * @return
	 */
	public static TimeWindowSet getTimeWindowSet(int periodNumber) {
		TimeWindowSet timeWindowSet = (TimeWindowSet) DataServiceProvider.getTimeWindowDataServiceImplInstance()
				.getSetById(SettingsProvider.getPeriodSetting().getTimeWindowSetId());

		return timeWindowSet;
	}

	/**
	 * Provides parameter value which is constant over periods and assigned
	 * beforehand
	 * 
	 * @param periodNumber
	 * @return
	 */
	public static Double getParameterValue(int periodNumber, String parameterName) {
		return ParameterService.getRespectiveParameterValue(parameterName,
				SettingsProvider.getPeriodSetting().getParameterValues());
	}

	/**
	 * Provides routing for the respective period and t. If t=-1, it is an
	 * initial routing. If t=-2, it is a final routing.
	 * 
	 * @param periodNumber
	 * @param t
	 * @return
	 */
	public static Routing getRouting(int periodNumber, Integer t) {
		Routing routing;
		int routingId = 0;

		ArrayList<RoutingAssignment> assignments;
		if (periodNumber == 0) {
			assignments = SettingsProvider.getPeriodSetting().getRoutingAssignments();

		} else {
			assignments = SettingsProvider.getPeriodSettingFollower(periodNumber).getRoutingAssignments();

		}

		// Look for routing assignment with appropriate t
		for (int i = 0; i < assignments.size(); i++) {
			if (assignments.get(i).getT() == t) {
				routingId = assignments.get(i).getRoutingId();
			}
		}

		if (routingId == 0)
			return null;
		System.out.println("routingId: " + routingId);
		routing = DataServiceProvider.getRoutingDataServiceImplInstance().getRoutingById(routingId);

		return routing;
	}

	public static ArrayList<NodeDistance> getDistances() {
		int regionId = SettingsProvider.getExperiment().getRegionId();
		ArrayList<NodeDistance> distances = DataServiceProvider.getRegionDataServiceImplInstance()
				.getNodeDistancesByRegionId(regionId);

		return distances;
	}

	public static ArrayList<Node> getNodes() {
		int regionId = SettingsProvider.getExperiment().getRegionId();
		ArrayList<Node> nodes = DataServiceProvider.getRegionDataServiceImplInstance().getNodesByRegionId(regionId);

		return nodes;
	}
}
