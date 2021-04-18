package logic.utility.settings.largestRegion.tw6;

import java.util.ArrayList;
import java.util.HashMap;

import logic.utility.settings.RunExperimentHelper;

public class Lst6l3_2S2c4cLowHighFlexibleInflexible7525_10 {
	
	String basicName = "Lst6l3_2S2c4cLowHighFlexibleInflexible7525_10_";
	int bookingPeriodLength = 2500;
	String db= "SimLab_revision";
	int regionId = 10;
	int depotId = 11;
	int deliveryAreaSetId = 41;
	int vehicleAssignmentSetId = 42;
	int noVehicles =10;
	int demandSegmentWeightingId = 59;
	int serviceSegmentWeightingId=1;
	int timeWindowSetId = 12;
	int arrivalProcess = 12;
	int arrivalProbabilityDistributionId=49;
	
	//Adapt!
	int firstOrderRequestSetIdForValiation = 2;
	int firstOrderRequestSetIdForTOPExpected =102;
	int learningRequestSetsExperimentId =4;

	ArrayList<Integer> learningRoutingExperimentIds = new ArrayList<Integer>();
	ArrayList<Integer> learningRoutingExperimentIdsExpected = new ArrayList<Integer>();
	
	public Lst6l3_2S2c4cLowHighFlexibleInflexible7525_10() {
		logic.utility.SettingsProvider.database=db;
		learningRoutingExperimentIds.add(6);
		learningRoutingExperimentIdsExpected.add(6);
		
	}
	
	
	public String runDG(){
		String resultLog = RunExperimentHelper.runDataGeneration(basicName, regionId,bookingPeriodLength,
				arrivalProcess, arrivalProbabilityDistributionId, demandSegmentWeightingId, serviceSegmentWeightingId);
		
		return resultLog;
		
		//Adapt sets in settings!
		//Sql statement to get first order request sets
//		select * from SimLab.r_run_v_order_request_set
//		left join SimLab.run on (run.run_id = r_run_v_order_request_set.run_ors_run)
//		where run_experiment =13842;
	
	}

//	public String runDGFix(int addedNumberOfSets){
//		String resultLog = RunExperimentHelper.runDGLearningWithConfiguredNumber(basicName, regionId,bookingPeriodLength,
//				arrivalProcess, demandSegmentWeightingId, serviceSegmentWeightingId, addedNumberOfSets);
//
//		return resultLog;
//	}
	
	public String runTOP(){
		
		String resultLog = "";

	//			RunExperimentHelper.runTOP(basicName, bookingPeriodLength,regionId, deliveryAreaSetId,
	//			vehicleAssignmentSetId, demandSegmentWeightingId, timeWindowSetId, firstOrderRequestSetIdForTOP,
	//			arrivalProcess, false);
	

		 resultLog += RunExperimentHelper.runTOP(basicName, bookingPeriodLength,regionId, deliveryAreaSetId, 
					vehicleAssignmentSetId, demandSegmentWeightingId, timeWindowSetId, firstOrderRequestSetIdForTOPExpected, 
					arrivalProcess, true);
		 
		return resultLog;
		//Adapt sets in settings!
	}
	
	public String runPruningAnn() {
		String resultLog=  RunExperimentHelper.pruningAnn(basicName,bookingPeriodLength,regionId,depotId, deliveryAreaSetId, 
				 vehicleAssignmentSetId, noVehicles, demandSegmentWeightingId,  timeWindowSetId,  firstOrderRequestSetIdForValiation, 
				 learningRequestSetsExperimentId,  learningRoutingExperimentIds,  learningRoutingExperimentIdsExpected, arrivalProcess);
		
		return resultLog;
	}

	public String runBenchmarking (boolean fcfs, boolean ann){
		String[]  names= new String[] {
		//		"#Tc", 
				"#IcTc",//"#EcaTc",
//				"#Tc_d0Type2", //"#Tc_d0Type2PerTw",
//				"#IcTc_d0Type2", //"#IcTc_d0Type2PerTw",
		//		"#EcaTc_d0Type2", //"#EcaTc_d0Type2PerTw",
//				//"#IcTc_d0Type2_s", "#IcTc_d0Type2PerTw_s", "#EcaTc_d0Type2_s", "#EcaTc_d0Type2PerTw_s",
//				"#Tc_d3Type3", "#Tc_d3Type3PerTw", "#IcTc_d3Type3", "#IcTc_d3Type3PerTw", "#EcaTc_d3Type3", "#EcaTc_d3Type3PerTw", 
////				"#IcTc_d3Type3_s", "#IcTc_d3Type3PerTw_s", 
////				"#EcaTc_d3Type3_s", "#EcaTc_d3Type3PerTw_s", 
//				"#IcTc_d3Type3_s1/3", "#IcTc_d3Type3PerTw_s1/3", "#EcaTc_d3Type3_s1/3", "#EcaTc_d3Type3PerTw_s1/3",
////				"#IcTc_d3Type3_s2/3", "#IcTc_d3Type3PerTw_s2/3",
////				"#EcaTc_d3Type3_s2/3", "#EcaTc_d3Type3PerTw_s2/3"
//				"#IcTc_dType4", "#IcTc_dType4PerTw",
	//			"#EcaTc_dType4", //"#EcaTc_dType4PerTw"
				};
		
		
		String resultLog = RunExperimentHelper.runBenchmarkingADPExperiments(basicName,bookingPeriodLength,regionId,depotId, deliveryAreaSetId, 
				 vehicleAssignmentSetId, noVehicles, demandSegmentWeightingId,  timeWindowSetId,  firstOrderRequestSetIdForValiation, 
				 learningRequestSetsExperimentId,  learningRoutingExperimentIds,  learningRoutingExperimentIdsExpected, arrivalProcess, fcfs, ann, names);

	

		// Print summary
		return resultLog;
	}
	
	
	public String runFCFSYang() {
		
		String resultLog = RunExperimentHelper.runFCFSYang(basicName, bookingPeriodLength, regionId, deliveryAreaSetId, 
				vehicleAssignmentSetId, timeWindowSetId, firstOrderRequestSetIdForValiation);


		// Print summary
		return resultLog;
		
	}
	
	public String runANNAcceptanceExperiments(Integer valueFunctionModelSetId){

		String resultLog=RunExperimentHelper.runANNAcceptanceExperiments(basicName,regionId,bookingPeriodLength,  learningRequestSetsExperimentId,
				 demandSegmentWeightingId,  deliveryAreaSetId, vehicleAssignmentSetId, 
				   learningRoutingExperimentIdsExpected, arrivalProcess, firstOrderRequestSetIdForValiation, noVehicles,depotId, valueFunctionModelSetId);
		
	

		// Print summary
		return resultLog;
	}

	public String runANNExperimentsWithoutFinalRouting(Integer valueFunctionModelSetId) {

		String resultLog=RunExperimentHelper.runANNExperimentsWithoutFinalRouting(basicName, regionId, bookingPeriodLength,
				learningRequestSetsExperimentId,
				demandSegmentWeightingId, deliveryAreaSetId, vehicleAssignmentSetId,
				learningRoutingExperimentIdsExpected, arrivalProcess, firstOrderRequestSetIdForValiation, noVehicles, depotId, valueFunctionModelSetId);

		return resultLog;
	}

	public String runDynamicADP(Integer valueFunctionModelSetId) {

		String resultLog=RunExperimentHelper.runDynamicADP(basicName, bookingPeriodLength, regionId,
				depotId, deliveryAreaSetId, vehicleAssignmentSetId, noVehicles, demandSegmentWeightingId,
				timeWindowSetId, firstOrderRequestSetIdForValiation, learningRequestSetsExperimentId, learningRoutingExperimentIds,
				learningRoutingExperimentIdsExpected,
				arrivalProcess, "#IcTc", valueFunctionModelSetId);


		return resultLog;
	}

}
