package data.utility;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Request with all relevant settings for a given process or algorithm. Each process can
 * provide such a request.
 * 
 * @author M. Lang
 *
 */
public class SettingRequest {

	HashMap<PeriodSettingType, Boolean> periodSettings; //Setting type with respective boolean, if it is optional
	ArrayList<String> parameterNames;

	public SettingRequest(){
		this.periodSettings=new HashMap<PeriodSettingType, Boolean>();
		this.parameterNames=new ArrayList<String>();
	}
	
	public HashMap<PeriodSettingType, Boolean> getPeriodSettings() {
		return periodSettings;
	}

	public void setPeriodSettings(HashMap<PeriodSettingType, Boolean> periodSettings) {
		this.periodSettings = periodSettings;
	}
	
	public void addPeriodSetting(PeriodSettingType type, Boolean optional){
		this.periodSettings.put(type, optional);
	}

	public ArrayList<String> getParameters() {
		return parameterNames;
	}

	public void setParameters(ArrayList<String> parameters) {
		this.parameterNames = parameters;
	}
	
	public void addParameter(String para){
		this.parameterNames.add(para);
	}

}
