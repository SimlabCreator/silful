package logic.utility.settings.smallRegion.tw12;

import java.util.ArrayList;

import logic.utility.settings.RunExperimentHelper;

public class S12l3_2S2c2cLowLowFlexibleInflexible7525 {

	String basicName = "S12l3_2S2c2cLowLowFlexibleInflexible7525_";
	int bookingPeriodLength = 500;
	int regionId = 8;
	int depotId = 9;
	int deliveryAreaSetId = 35;
	int vehicleAssignmentSetId = 36;
	int noVehicles =2;
	int demandSegmentWeightingId = 45;
	int serviceSegmentWeightingId=1;
	int timeWindowSetId = 11;
	int arrivalProcess = 12;
	int arrivalProbabilityDistributionId=49;
	
	//Adapt!
	int firstOrderRequestSetIdForValiation = 47702;
	//int firstOrderRequestSetIdForTOP=21304;
	int firstOrderRequestSetIdForTOPExpected =47802;
	int learningRequestSetsExperimentId =23653;

	ArrayList<Integer> learningRoutingExperimentIds = new ArrayList<Integer>();
	ArrayList<Integer> learningRoutingExperimentIdsExpected = new ArrayList<Integer>();
	
	public S12l3_2S2c2cLowLowFlexibleInflexible7525() {
		learningRoutingExperimentIds.add(23678);
	
		learningRoutingExperimentIdsExpected.add(23678);
		
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
	
	public String runPruningAnn() {
		String resultLog=  RunExperimentHelper.pruningAnn(basicName,bookingPeriodLength,regionId,depotId, deliveryAreaSetId, 
				 vehicleAssignmentSetId, noVehicles, demandSegmentWeightingId,  timeWindowSetId,  firstOrderRequestSetIdForValiation, 
				 learningRequestSetsExperimentId,  learningRoutingExperimentIds,  learningRoutingExperimentIdsExpected, arrivalProcess);
		
		return resultLog;
	}

	public String runBenchmarking (boolean fcfs, boolean ann){
		String[]  names= new String[] {
//				"#Tc", "#IcTc","#EcaTc",
//				"#Tc_d0Type2", "#Tc_d0Type2PerTw",
//				"#IcTc_d0Type2", "#IcTc_d0Type2PerTw", 
//				"#EcaTc_d0Type2", "#EcaTc_d0Type2PerTw",
//				//"#IcTc_d0Type2_s", "#IcTc_d0Type2PerTw_s", "#EcaTc_d0Type2_s", "#EcaTc_d0Type2PerTw_s",
//				"#Tc_d3Type3", "#Tc_d3Type3PerTw", "#IcTc_d3Type3", "#IcTc_d3Type3PerTw", "#EcaTc_d3Type3", "#EcaTc_d3Type3PerTw", 
////				"#IcTc_d3Type3_s", "#IcTc_d3Type3PerTw_s", 
////				"#EcaTc_d3Type3_s", "#EcaTc_d3Type3PerTw_s", 
//		//		"#IcTc_d3Type3_s1/3", "#IcTc_d3Type3PerTw_s1/3", "#EcaTc_d3Type3_s1/3", "#EcaTc_d3Type3PerTw_s1/3",
////				"#IcTc_d3Type3_s2/3", "#IcTc_d3Type3PerTw_s2/3",
////				"#EcaTc_d3Type3_s2/3", "#EcaTc_d3Type3PerTw_s2/3"
//				"#IcTc_dType4", "#IcTc_dType4PerTw",
//				"#EcaTc_dType4", "#EcaTc_dType4PerTw"
				};
		
		
		String resultLog = RunExperimentHelper.runBenchmarkingADPExperiments(basicName,bookingPeriodLength,regionId,depotId, deliveryAreaSetId, 
				 vehicleAssignmentSetId, noVehicles, demandSegmentWeightingId,  timeWindowSetId,  firstOrderRequestSetIdForValiation, 
				 learningRequestSetsExperimentId,  learningRoutingExperimentIds,  learningRoutingExperimentIdsExpected, arrivalProcess, fcfs, ann, names);


	

		// Print summary
		return resultLog;
	}

}
