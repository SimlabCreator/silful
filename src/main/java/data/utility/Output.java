package data.utility;

import java.util.ArrayList;

/**
 * Output of the process or algorithm like an order set or delivery set, such that it is not needed to be set by the user for the next step
 * 
 * @author M. Lang
 *
 */
public class Output {

	ArrayList<PeriodSettingType> outputs;
	

	public Output(){
		this.outputs=new ArrayList<PeriodSettingType>();
	}
	
	public ArrayList<PeriodSettingType> getOutputs() {
		return outputs;
	}

	public void setOutputs(ArrayList<PeriodSettingType> outputs) {
		this.outputs = outputs;
	}
	
	public void addOutput(PeriodSettingType type){
		this.outputs.add(type);
	}

}
