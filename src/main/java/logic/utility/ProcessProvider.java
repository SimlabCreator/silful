package logic.utility;

import logic.process.IProcess;
import logic.process.efulfillment.learning.*;
import logic.process.efulfillment.optimization.dynamic.CampbellSavelsbergh2005AcceptanceProcess;
import logic.process.efulfillment.optimization.dynamic.CampbellSavelsbergh2006FeasibilityOrCostAcceptanceProcess;
import logic.process.efulfillment.optimization.dynamic.Yang2016Algorithm1ForDynamicSlottingProcess;
import logic.process.efulfillment.orienteeringForLearning.AverageCapacityDeterminationProcess;
import logic.process.efulfillment.orienteeringForLearning.GRILSForLearningAdaptedProcess;
import logic.process.efulfillment.orienteeringForLearning.GRILSForLearningAdaptedWithMultipleAreasProcess;
import logic.process.efulfillment.orienteeringForLearning.GRILSForLearningWithMultipleAreasProcess;
import logic.process.efulfillment.orsProcesses.ALNSFinalRoutingWithInfeasibleProcess;
import logic.process.efulfillment.orsProcesses.DependentDemandAcceptanceORSProcess;
import logic.process.efulfillment.orsProcesses.DynamicProgrammingDecisionTreeORSProcess;
import logic.process.efulfillment.orsProcesses.EfficientRoutingORSProcess;
import logic.process.efulfillment.orsProcesses.FcfsORSProcess;
import logic.process.efulfillment.orsProcesses.FcfsORSRUNTIMECOMPARISONProcess;
import logic.process.efulfillment.orsProcesses.IndependentDemandAssumptionAcceptanceORSProcess;
import logic.process.efulfillment.orsProcesses.IndependentDemandAssumptionControlsORSProcess;
import logic.process.efulfillment.orsProcesses.ObjectiveBasedRoutingORSProcess;
import logic.process.efulfillment.orsProcesses.ObjectiveBasedRoutingORSRUNTIMECOMPARISONProcess;
import logic.process.efulfillment.orsProcesses.ObjectiveBasedRoutingORSWithMinimumRequestNumberProcess;
import logic.process.efulfillment.orsProcesses.PsychicDependentDemandForecastORSProcess;
import logic.process.efulfillment.tslConference_chicago.ObjectiveBasedOrienteeringFCFSProcess;
import logic.process.efulfillment.tslConference_chicago.ObjectiveBasedOrienteeringIteratedLocalSearchProcess;
import logic.process.efulfillment.tslConference_chicago.ObjectiveBasedOrienteeringPredictiveGreedyProcess;
import logic.process.support.DataGenerationOrderRequests;
import logic.process.support.DataGenerationOrderRequestsII;

/**
 * Provides a process object of the process type requested. Process id has to
 * fit to id in the database
 *
 * @author M. Lang
 *
 */
public class ProcessProvider {

    public static IProcess getProcessByProcessTypeId(Integer processTypeId) {
        IProcess process;
        switch (processTypeId) {
            case 1:
                process = new DataGenerationOrderRequests();
                break;
            case 2:
                process = new FcfsORSProcess();
                break;
            case 3:
                process = new IndependentDemandAssumptionControlsORSProcess();
                break;
            case 4:
                process = new IndependentDemandAssumptionAcceptanceORSProcess();
                break;
            case 5:
                process = new PsychicDependentDemandForecastORSProcess();
                break;
            case 6:
                process = new EfficientRoutingORSProcess();
                break;
            case 7:
                process = new ObjectiveBasedRoutingORSProcess();
                break;
            case 8:
                process = new DynamicProgrammingDecisionTreeORSProcess();
                break;
            case 9:
                process = new DependentDemandAcceptanceORSProcess();
                break;
            case 10:
                process = new FcfsORSRUNTIMECOMPARISONProcess();
                break;
            case 11:
                process = new ObjectiveBasedRoutingORSRUNTIMECOMPARISONProcess();
                break;
            case 12:
                process = new ObjectiveBasedRoutingORSWithMinimumRequestNumberProcess();
                break;
            case 13:
                process = new GRILSForLearningAdaptedProcess();
                break;
            case 14:
                process = new ObjectiveBasedOrienteeringIteratedLocalSearchProcess();
                break;
            case 15:
                process = new LinearValueFunctionAsYangTrainingProcess();
                break;
            case 16:
                process = new ObjectiveBasedOrienteeringFCFSProcess();
                break;
            case 17:
                process = new ObjectiveBasedOrienteeringPredictiveGreedyProcess();
                break;
            case 18:
                process = new GRILSForLearningAdaptedWithMultipleAreasProcess();
                break;
            case 19:
                process = new AverageCapacityDeterminationProcess();
                break;
            case 20:
                process = new ParallelFlightsProgrammingPreparationProcess();
                break;
            case 21:
                process = new DeterministicLinearProgrammingAcceptanceProcess();
                break;
            case 22:
                process = new LinearValueFunctionAsYangAcceptanceProcess();
                break;
            case 23:
                process = new DataGenerationOrderRequestsII();
                break;
            case 24:
                process = new CampbellSavelsbergh2005AcceptanceProcess();
                break;
            case 25:
                process = new TabularValueFunctionTrainingProcess();
                break;
            case 26:
                process = new LinearValueFunctionOrienteeringTrainingProcess();
                break;
            case 27:
                process = new LinearValueFunctionOrienteeringAcceptanceProcess();
                break;
            case 28:
                process = new OrienteeringAcceptanceProcess();
                break;
            case 29:
                process = new CampbellSavelsbergh2006FeasibilityOrCostAcceptanceProcess();
                break;
            case 30:
                process = new OrienteeringAcceptanceGRASPProcess();
                break;
            case 31:
                process = new ANNValueFunctionOrienteeringTrainingProcess();
                break;
            case 32:
                process = new ANNOrienteeringAcceptanceProcess();
                break;
            case 33:
                process = new ALNSFinalRoutingWithInfeasibleProcess();
                break;
            case 34:
                process = new GRILSForLearningWithMultipleAreasProcess();
                break;
            case 35:
                process = new DeterministicProgrammingAcceptanceProcess();
                break;
            case 36:
                process = new DeterministicProgrammingPreparationProcess();
                break;
            case 37:
                process = new OrienteeringAcceptanceWithALNSProcess();
                break;
            case 38:
                process = new DeterministicProgrammingAcceptanceWithALNSProcess();
                break;
            case 39:
                process = new ANNOrienteeringAcceptanceALNSProcess();
                break;
            case 40:
                process = new FCFSInsertionAndALNSFinalProcess();
                break;
            case 41:
                process = new MesoValueFunctionTrainingProcess();
                break;
            case 42:
                process = new MesoValueFunctionAcceptanceProcess();
                break;
            case 43:
            	process = new Yang2016Algorithm1ForDynamicSlottingProcess();
            	break;
            default:
                process = null;
                break;
        }
        return process;
    }
}
