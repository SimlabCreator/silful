package logic.service.support;

import data.entity.ArrivalProcess;
import data.utility.DataServiceProvider;

/**
 * Provides functionality relating to arrival processes
 * @author M. Lang
 *
 */
public class ArrivalProcessService {

	/**
	 * Determines the arrival probability of time step t according to the respective arrival process
	 * @param processId Respective arrival process id
	 * @param t Respective time step
	 * @return
	 */
	public static double getArrivalProbability(Integer t, ArrivalProcess arrivalProcess){
		
		
		
		
		if(arrivalProcess.getProbabilityDistributionId()==null||arrivalProcess.getProbabilityDistributionId()==0){ //Constant arrival probability
			return arrivalProcess.getFactor();
		}else{
			//TODO: Probability according to a discreteProbability distribution,multiplied by the respective lambda factor
			return 0.0;
		}
		
	}
	
	/**
	 * Determines the mean arrival probability according to the respective arrival process
	 * @param processId Respective arrival process id
	 * @return
	 */
	public static double getMeanArrivalProbability(Integer processId){
		
		ArrivalProcess arrivalProcess = (ArrivalProcess) DataServiceProvider.getArrivalProcessDataServiceImplInstance().getById(processId);
		
		
		if(arrivalProcess.getProbabilityDistributionId()==null||arrivalProcess.getProbabilityDistributionId()==0){ //Constant arrival probability
			return arrivalProcess.getFactor();
		}else{
			//TODO: Calculate, consider lambda factor
			return 0.0;
		}
		
	}

}
