package logic.utility.settings.largeRegion.overlapping;

import java.util.ArrayList;

import logic.utility.SettingsProvider;
import logic.utility.settings.RunExperimentHelper;

public class L11ovl405_2SUniLowHighFlexibleInflexible7525 {

	String db = "SimLabOVL";
	String basicName = "L11ovl405_2SUniLowHighFlexibleInflexible7525_";
	int bookingPeriodLength = 500;
	int regionId = 9;
	int depotId = 10;
	int deliveryAreaSetId = 37;
	int vehicleAssignmentSetId = 39;
//	int deliveryAreaSetId = 39;
//	int vehicleAssignmentSetId = 41;
	int noVehicles =3;
	int demandSegmentWeightingId = 54;
	int serviceSegmentWeightingId=1;
	int timeWindowSetId = 13;
	int arrivalProcess = 15;
	int arrivalProbabilityDistributionId=52;
	
	//Adapt!
	int firstOrderRequestSetIdForValiation = 10402;
//	int firstOrderRequestSetIdForTOP=31902;
	int firstOrderRequestSetIdForTOPExpected =10502;
	int learningRequestSetsExperimentId = 10;

	ArrayList<Integer> learningRoutingExperimentIds = new ArrayList<Integer>();
	ArrayList<Integer> learningRoutingExperimentIdsExpected = new ArrayList<Integer>();
	
	public L11ovl405_2SUniLowHighFlexibleInflexible7525() {
		SettingsProvider.database=db;
		learningRoutingExperimentIds.add(229);
		learningRoutingExperimentIdsExpected.add(229);
		
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
		
		String resultLog= RunExperimentHelper.runTOP(basicName, bookingPeriodLength,regionId, deliveryAreaSetId, 
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

	

		// Print summary
		return resultLog;
	}
	
	public String runANNExperiments() {
		String resultLog = RunExperimentHelper.runANNExperiments(basicName,regionId,bookingPeriodLength,  learningRequestSetsExperimentId, 
				 demandSegmentWeightingId,  deliveryAreaSetId, vehicleAssignmentSetId, 
				   learningRoutingExperimentIdsExpected, arrivalProcess, firstOrderRequestSetIdForValiation, noVehicles,depotId);


		// Print summary
		return resultLog;
	}

}
