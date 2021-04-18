package logic.utility.settings.smallRegion.tw6.forVFA;

import java.util.ArrayList;
import java.util.HashMap;

import logic.utility.SettingsProvider;
import logic.utility.settings.RunExperimentHelper;

public class S6l45_2S2c4cLowHighFlexibleInflexible7525_VFA {

	String db = "SimLabS3V";
	String basicName = "S6l45_2S2c4cLowHighFlexibleInflexible7525_VFA_";
	int bookingPeriodLength = 500;
	int regionId = 8;
	int depotId = 9;
	int deliveryAreaSetId = 35;
	int vehicleAssignmentSetId = 37;
	int noVehicles =3;
	int demandSegmentWeightingId = 42;
	int serviceSegmentWeightingId=1;
	int timeWindowSetId = 12;
	int arrivalProcess = 14;
	int arrivalProbabilityDistributionId=51;
	
	
	int firstOrderRequestSetIdForValiation = 62412;
//	int firstOrderRequestSetIdForTOP=10503;
	int firstOrderRequestSetIdForTOPExpected =36502;
	int learningRequestSetsExperimentId = 25;
	
	ArrayList<Integer> learningRoutingExperimentIds = new ArrayList<Integer>();
	ArrayList<Integer> learningRoutingExperimentIdsExpected = new ArrayList<Integer>();
	
	public S6l45_2S2c4cLowHighFlexibleInflexible7525_VFA() {
		SettingsProvider.database=db;
		learningRoutingExperimentIds.add(738);
		learningRoutingExperimentIdsExpected.add(738);
		
	}
	
	
	public String runDG(){
		RunExperimentHelper helper;
		HashMap<String, Double> paras;
		String resultLog = "Result: ";

		// Validation data
		helper = new RunExperimentHelper();
		helper.experimentName = basicName + "DG_Validation";
		helper.experimentDescription = " ";
		helper.experimentOccasion = " ";
		helper.experimentResponsible = " ";
		helper.noRepetitions = 10;
		helper.regionId = regionId;
		helper.processTypeId = 1;
		helper.bookingPeriodNumber = 1;
		helper.bookingPeriodLength = bookingPeriodLength;
		helper.arrivalProcessId = arrivalProcess;
		helper.demandSegmentWeightingId = demandSegmentWeightingId;
		helper.serviceSegmentWeightingId = serviceSegmentWeightingId;

		paras = new HashMap<String, Double>();
		paras.put("samplePreferences", 1.0);
		helper.start(paras);
		resultLog += "(" + helper.experimentName + ":" + SettingsProvider.getExperiment().getId() + ",os_"
				+ (SettingsProvider.getPeriodSetting().getOrderRequestSetId() - 10 + 1) + ")";
		return resultLog;
	
	}
	
	
	
	public String runANN(Integer valueFunctionModelSetId) {
		// For this setting: VFM is 71
		String resultLog = "";
		String adpName = "ADP_";

		// Can do this for more experiments...
		RunExperimentHelper helper = new RunExperimentHelper();
		// // Define settings
		helper.experimentName = basicName + adpName + "Ann_" + deliveryAreaSetId + "_Acc";
		helper.experimentDescription = " ";
		helper.experimentOccasion = " ";
		helper.experimentResponsible = " ";
		helper.noRepetitions = 10;
		helper.regionId = regionId;
		helper.processTypeId = 39;
		helper.bookingPeriodNumber = 1;
		helper.bookingPeriodLength = bookingPeriodLength;
		helper.valueFunctionModelSetId = valueFunctionModelSetId;
		helper.orderRequestSetId = firstOrderRequestSetIdForValiation;
		helper.orderRequestSetChanges = true;
		helper.demandSegmentWeightingId = demandSegmentWeightingId;
		helper.vehicleAssignmentSetId = vehicleAssignmentSetId;
		helper.deliveryAreaSetId = deliveryAreaSetId;
		helper.learningRoutingsExperimentIds = learningRoutingExperimentIdsExpected;
		helper.arrivalProcessId = arrivalProcess;
		helper.noVehicles = noVehicles;
		helper.vehicleTypes = new ArrayList<Integer>();
		helper.vehicleTypes.add(1);
		helper.vehicleTypesNos = new ArrayList<Integer>();
		helper.vehicleTypesNos.add(noVehicles);
		helper.depotId = depotId;

		HashMap<String, Double> paras = new HashMap<String, Double>();
		paras.put("actualBasketValue", 1.0);
		paras.put("Constant_service_time", 12.0);
		paras.put("samplePreferences", 1.0);
		paras.put("theft-based", 0.0);
		paras.put("theft-based-advanced", 0.0);
		paras.put("theft-based-tw", 0.0);
		paras.put("consider_left_over_penalty", 0.0);
		paras.put("discounting_factor_probability", 1.0);
		paras.put("oc_for_feasible", 0.0);
		paras.put("stop_once_feasible", 1.0);

		// Run experiment
		helper.start(paras);
		int firstExpId = SettingsProvider.getExperiment().getId() - 10 + 1;
		resultLog += "(" + helper.experimentName + ": " + firstExpId + ") ";
		return resultLog;
	}
	
	public String runTOP(){
		
//		
//		String resultLog = RunExperimentHelper.runTOP(basicName, bookingPeriodLength,regionId, deliveryAreaSetId, 
//				vehicleAssignmentSetId, demandSegmentWeightingId, timeWindowSetId, firstOrderRequestSetIdForTOP, 
//				arrivalProcess, false);
	

		String  resultLog = RunExperimentHelper.runTOP(basicName, bookingPeriodLength,regionId, deliveryAreaSetId, 
					vehicleAssignmentSetId, demandSegmentWeightingId, timeWindowSetId, firstOrderRequestSetIdForTOPExpected, 
					arrivalProcess, true);
		 
		return resultLog;
		//Adapt sets in settings!
	}

	public String runBenchmarking (boolean fcfs, boolean ann){
		String[]  names= new String[] {
				"#Tc", "#IcTc",
				//"#EcaTc",
				"#Tc_d0Type2", "#IcTc_d0Type2","#EcaTc_d0Type2", 
//				"#Tc_d0Type2PerTw","#IcTc_d0Type2PerTw", "#EcaTc_d0Type2PerTw",
//				//"#IcTc_d0Type2_s", "#IcTc_d0Type2PerTw_s", "#EcaTc_d0Type2_s", "#EcaTc_d0Type2PerTw_s",
//				"#Tc_d3Type3", "#Tc_d3Type3PerTw", "#IcTc_d3Type3", "#IcTc_d3Type3PerTw", "#EcaTc_d3Type3", "#EcaTc_d3Type3PerTw", 
////				"#IcTc_d3Type3_s", "#IcTc_d3Type3PerTw_s", 
////				"#EcaTc_d3Type3_s", "#EcaTc_d3Type3PerTw_s", 
//				"#IcTc_d3Type3_s1/3", "#IcTc_d3Type3PerTw_s1/3", "#EcaTc_d3Type3_s1/3", "#EcaTc_d3Type3PerTw_s1/3",
////				"#IcTc_d3Type3_s2/3", "#IcTc_d3Type3PerTw_s2/3",
////				"#EcaTc_d3Type3_s2/3", "#EcaTc_d3Type3PerTw_s2/3"
				"#IcTc_dType4", "#IcTc_dType4PerTw","#EcaTc_dType4","#EcaTc_dType4PerTw"
				};
		
		
		String resultLog = RunExperimentHelper.runBenchmarkingADPExperiments(basicName,bookingPeriodLength,regionId,depotId, deliveryAreaSetId, 
				 vehicleAssignmentSetId, noVehicles, demandSegmentWeightingId,  timeWindowSetId,  firstOrderRequestSetIdForValiation, 
				 learningRequestSetsExperimentId,  learningRoutingExperimentIds,  learningRoutingExperimentIdsExpected, arrivalProcess, fcfs, ann, names);

//		String adpName = "ADP";
//		RunExperimentHelper helper;
//		HashMap<String, Double> paras;
//		String resultLog = "Result: ";
		
//		// FCFS
//		helper = new RunExperimentHelper();
//		helper.experimentName = basicName + "FCFS";
//		helper.experimentDescription = " ";
//		helper.experimentOccasion = " ";
//		helper.experimentResponsible = " ";
//		helper.noRepetitions = 100;
//		helper.regionId = regionId;
//		helper.processTypeId = 29;
//		helper.bookingPeriodNumber = 1;
//		helper.bookingPeriodLength = bookingPeriodLength;
//		helper.orderRequestSetId = firstOrderRequestSetIdForValiation;
//		helper.deliveryAreaSetId = deliveryAreaSetId;
//		helper.vehicleAssignmentSetId = vehicleAssignmentSetId;
//		helper.timeWindowSetId = timeWindowSetId;
//		helper.orderRequestSetChanges = true;
//
//		paras = new HashMap<String, Double>();
//		paras.put("Constant_service_time", 12.0);
//		paras.put("samplePreferences", 1.0);
//		paras.put("includeDriveFromStartingPosition", 0.0);
//		paras.put("no_routing_candidates", 0.0);
//		paras.put("no_insertion_candidates", 1.0);
//		paras.put("consider_profit", 0.0);
//		helper.start(paras);
//
//		int firstExpId = SettingsProvider.getExperiment().getId() - 100 + 1;
//		resultLog += "(" + helper.experimentName + ": " + firstExpId + ") ";

		// ANN
//		helper = new RunExperimentHelper();
//		paras = new HashMap<String, Double>();
//		// Define settings
//		helper.experimentName = basicName + adpName + "Ann";
//		helper.experimentDescription = " ";
//		helper.experimentOccasion = " ";
//		helper.experimentResponsible = "ML";
//		helper.noRepetitions = 1;
//		helper.regionId = regionId;
//		helper.processTypeId = 31;
//		helper.bookingPeriodNumber = 1;
//		helper.bookingPeriodLength = bookingPeriodLength;
//		helper.learningRequestSetsExperimentId = learningRequestSetsExperimentId;
//		helper.demandSegmentWeightingId = demandSegmentWeightingId;
//		helper.deliveryAreaSetId = deliveryAreaSetId;
//		helper.vehicleAssignmentSetId = vehicleAssignmentSetId;
//		helper.learningRoutingsExperimentIds = new ArrayList<Integer>();
//		helper.learningRoutingsExperimentIds.add(learningRoutingExperimentId);
//		helper.arrivalProcessId = arrivalProcess;
//		helper.benchmarkingOrderSetsExperimentIds = new ArrayList<Integer>();
//		helper.benchmarkingOrderSetsExperimentIds.add(13637);
//
//		paras.put("Constant_service_time", 12.0);
//		paras.put("actualBasketValue", 1.0);
//		paras.put("samplePreferences", 1.0);
//		paras.put("includeDriveFromStartingPosition", 0.0);
//		paras.put("exploration_(0:on-policy,1:conservative-factor,2:e-greedy)", 2.0);
//		paras.put("stepsize_adp_learning", 0.1);
//		paras.put("annealing_temperature_(Negative:no_annealing)", 4000.0);
//		paras.put("momentum_weight", 0.2);
//		paras.put("theft-based", 1.0);
//		paras.put("theft-based-advanced", 1.0);
//		paras.put("consider_left_over_penalty", 0.0);
//		paras.put("discounting_factor", 1.0);
//		paras.put("discounting_factor_probability", 1.0);
//		paras.put("consider_constant", 1.0);
//		paras.put("additional_hidden_nodes", 3.0);
//		paras.put("consider_demand_neighbors", 0.0);
//		paras.put("oc_for_feasible", 0.0);
//		paras.put("hTan_activation", 1.0);
//		helper.start(paras);

		// Can do this for more experiments...
//		helper = new RunExperimentHelper();
//		// Define settings
//		helper.experimentName = basicName + adpName+"Ann_Acc";
//		helper.experimentDescription = " ";
//		helper.experimentOccasion = " ";
//		helper.experimentResponsible = " ";
//		helper.noRepetitions = 100;
//		helper.regionId = regionId;
//		helper.processTypeId = 32;
//		helper.bookingPeriodNumber = 1;
//		helper.bookingPeriodLength = bookingPeriodLength;
//		helper.valueFunctionModelSetId = SettingsProvider.getPeriodSetting().getValueFunctionModelSetId();
//		helper.orderRequestSetId = firstOrderRequestSetIdForValiation;
//		helper.orderRequestSetChanges = true;
//		helper.demandSegmentWeightingId = demandSegmentWeightingId;
//		helper.vehicleAssignmentSetId = vehicleAssignmentSetId;
//		helper.deliveryAreaSetId = deliveryAreaSetId;
//		helper.learningRoutingsExperimentIds = new ArrayList<Integer>();
//		helper.learningRoutingsExperimentIds.add(learningRoutingExperimentId);
//		helper.arrivalProcessId = arrivalProcess;
//
//		paras = new HashMap<String, Double>();
//		paras.put("actualBasketValue", 1.0);
//		paras.put("samplePreferences", 1.0);
//		paras.put("theft-based", 1.0);
//		paras.put("theft-based-advanced", 1.0);
//		paras.put("consider_left_over_penalty", 0.0);
//		paras.put("discounting_factor_probability", 1.0);
//		paras.put("oc_for_feasible", 0.0);
//		
//		// Run experiment
//		helper.start(paras);
//		int firstExpId = SettingsProvider.getExperiment().getId() - 100 + 1;
//		resultLog += "(" + helper.experimentName + ": " + firstExpId + ") ";
		
		
//		String[] names = new String[] { "#Tc", "#IcTc", "#EcTc" };
//		Double[] ic = new Double[] { 0.0, 1.0, 0.0 };
//		Double[] ec = new Double[] { 0.0,  0.0, 1.0 };
//		Double[] tc = new Double[] {1.0,  1.0,  1.0 };
//		Double[] distanceType = new Double[] { 0.0, 0.0, 0.0};
//		Double[] distanceMeasurePerTw = new Double[] { 0.0, 0.0, 0.0};
//		Double[] maximumDistanceIncrease = new Double[] { 0.0, 0.0, 0.0 };
//		Double[] switchTime = new Double[] { 0.0, 0.0, 0.0};
//		
////		String[] names = new String[] { "#", "#Tc", "#Ic", "#IcTc", "#Ec", "#EcTc" };
////		Double[] ic = new Double[] { 0.0, 0.0, 1.0, 1.0, 0.0, 0.0 };
////		Double[] ec = new Double[] { 0.0, 0.0, 0.0, 0.0, 1.0, 1.0 };
////		Double[] tc = new Double[] { 0.0, 1.0, 0.0, 1.0, 0.0, 1.0 };
////		Double[] distanceType = new Double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
////		Double[] distanceMeasurePerTw = new Double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
////		Double[] maximumDistanceIncrease = new Double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
////		Double[] switchTime = new Double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
//
//		
//
//		for (int i = 0; i < names.length; i++) {
//			helper = new RunExperimentHelper();
//			paras = new HashMap<String, Double>();
//			// Define settings
//			helper.experimentName = basicName + adpName + names[i];
//			helper.experimentDescription = " ";
//			helper.experimentOccasion = " ";
//			helper.experimentResponsible = "ML";
//			helper.noRepetitions = 1;
//			helper.regionId = regionId;
//			helper.processTypeId = 41;
//			helper.bookingPeriodNumber = 1;
//			helper.bookingPeriodLength = bookingPeriodLength;
//			helper.learningRequestSetsExperimentId = learningRequestSetsExperimentId;
//			helper.demandSegmentWeightingId = demandSegmentWeightingId;
//			helper.deliveryAreaSetId = deliveryAreaSetId;
//			helper.vehicleAssignmentSetId = vehicleAssignmentSetId;
//			helper.learningRoutingsExperimentIds = new ArrayList<Integer>();
//			helper.learningRoutingsExperimentIds.add(learningRoutingExperimentId);
//
//			paras.put("Constant_service_time", 12.0);
//			paras.put("actualBasketValue", 1.0);
//			paras.put("samplePreferences", 1.0);
//			paras.put("includeDriveFromStartingPosition", 0.0);
//			paras.put("consider_overall_remaining_capacity", ic[i]);
//			paras.put("consider_overall_accepted_insertion_costs", ec[i]);
//			paras.put("time_cap_interaction", tc[i]);
//			paras.put("exploration_(0:on-policy,1:wheel,2:e-greedy)", 2.0);
//			paras.put("no_routing_candidates", 0.0);
//			paras.put("no_insertion_candidates", 1.0);
//			paras.put("distance_type", distanceType[i]);
//			paras.put("distance_measure_per_tw", distanceMeasurePerTw[i]);
//			paras.put("maximum_distance_measure_increase", maximumDistanceIncrease[i]);
//			paras.put("switch_distance_off_point", switchTime[i]);
//			paras.put("meso_weight_lf", 1.0);
//			paras.put("stepsize_adp_learning", 0.0001);
//			paras.put("annealing_temperature_(Negative:no_annealing)", 4000.0);
//			paras.put("momentum_weight", 0.9);
//			paras.put("no_repetitions_sample", 1.0);
//
//			// Run experiment
//			helper.start(paras);
//
//			// Can do this for more experiments...
//			helper = new RunExperimentHelper();
//			// Define settings
//			helper.experimentName = basicName + adpName + names[i] + "Acc";
//			helper.experimentDescription = " ";
//			helper.experimentOccasion = " ";
//			helper.experimentResponsible = " ";
//			helper.noRepetitions = 100;
//			helper.regionId = regionId;
//			helper.processTypeId = 42;
//			helper.bookingPeriodNumber = 1;
//			helper.bookingPeriodLength = bookingPeriodLength;
//			helper.valueFunctionModelSetId = SettingsProvider.getPeriodSetting().getValueFunctionModelSetId();
//			helper.orderRequestSetId = firstOrderRequestSetIdForValiation;
//			helper.orderRequestSetChanges = true;
//			helper.demandSegmentWeightingId = demandSegmentWeightingId;
//			helper.vehicleAssignmentSetId = vehicleAssignmentSetId;
//			helper.deliveryAreaSetId = deliveryAreaSetId;
//			helper.learningRoutingsExperimentIds = new ArrayList<Integer>();
//			helper.learningRoutingsExperimentIds.add(learningRoutingExperimentId);
//
//			paras = new HashMap<String, Double>();
//			paras.put("Constant_service_time", 12.0);
//			paras.put("samplePreferences", 1.0);
//			paras.put("includeDriveFromStartingPosition", 0.0);
//			paras.put("no_routing_candidates", 0.0);
//			paras.put("no_insertion_candidates", 1.0);
//			paras.put("distance_type", distanceType[i]);
//			paras.put("distance_measure_per_tw", distanceMeasurePerTw[i]);
//			paras.put("maximum_distance_measure_increase", maximumDistanceIncrease[i]);
//			paras.put("switch_distance_off_point", switchTime[i]);
//
//			// Run experiment
//			helper.start(paras);
//			int firstExpId = SettingsProvider.getExperiment().getId() - 100 + 1;
//			resultLog += "(" + helper.experimentName + ": " + firstExpId + ") ";
//		}

	

		// Print summary
		return resultLog;
	}

}
