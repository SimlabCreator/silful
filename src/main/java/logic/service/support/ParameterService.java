package logic.service.support;

import java.util.ArrayList;

import data.entity.GeneralParameterValue;

/**
 * Provides functionality relating to forecasting step
 * 
 * @author M. Lang
 *
 */
public class ParameterService {

	public static Double getRespectiveParameterValue(String parameterName, ArrayList<GeneralParameterValue> parameters){
		
		Double value= 0.0;
		for(int i=0; i<parameters.size(); i++){
			//System.out.println(parameterName);
			if(parameters.get(i).getParameterType().getName().equals(parameterName)){
				value=parameters.get(i).getValue();
				return value;
			}
		}
		
		return null;
	}
	
}
