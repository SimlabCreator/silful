package logic.process.efulfillment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import data.utility.PeriodSettingType;
import data.utility.SettingRequest;
import logic.process.IProcess;
import logic.service.algorithmProvider.AlgorithmProviderService;

/**
 * Abstract class for all processes that combine different algorithms
 * 
 * @author M. Lang
 *
 */
public abstract class CompositionProcess implements IProcess{
	
	protected HashMap<Integer, AlgorithmProviderService> algoServices;

	public SettingRequest getSettingRequest() {
		
		SettingRequest request = new SettingRequest();
		ArrayList<PeriodSettingType> outputs = new ArrayList<PeriodSettingType>();
		ArrayList<String> parameterNames = new ArrayList<String>();
		
		for(int i=0; i<algoServices.size(); i++){
			
			SettingRequest algoRequest = algoServices.get(i).getSettingRequest();
			
			for(String para: algoServices.get(i).getSettingRequest().getParameters()){
				if(!parameterNames.contains(para))
					parameterNames.add(para);
			}

			outputs.addAll(algoServices.get(i).getOutput().getOutputs());
			
			if(i==0){
				request.setPeriodSettings(algoRequest.getPeriodSettings());
				
			}else{
				
				
				//Go through settings of current request and check if really needed based on output of earlier process steps
				Iterator<PeriodSettingType> it = algoRequest.getPeriodSettings().keySet().iterator();
				while(it.hasNext()){
					boolean inOutput=false;
					
					PeriodSettingType type = it.next();
					for(int outputNo=0; outputNo<outputs.size(); outputNo++){
						if(outputs.get(outputNo).equals(type)){
							inOutput=true;
							break;
						}
					}
					if(!inOutput){
						if(request.getPeriodSettings().containsKey(type)){
							if(request.getPeriodSettings().get(type)==false || algoRequest.getPeriodSettings().get(type)==false){
								request.getPeriodSettings().put(type, false);
							}
						}else{
							request.addPeriodSetting(type, algoRequest.getPeriodSettings().get(type));
						}
					}
				}
				
		
			}
			
		}
		request.setParameters(parameterNames);
		return request;
	}

	public ArrayList<PeriodSettingType> getOutputs(){
		ArrayList<PeriodSettingType> outputs = new ArrayList<PeriodSettingType>();
		for(int i=0; i<algoServices.size(); i++){
			outputs.addAll(algoServices.get(i).getOutput().getOutputs());
		}
		
		return outputs;
	}
}
