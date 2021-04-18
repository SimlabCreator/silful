package data.entity;

import java.util.ArrayList;

import data.utility.DataServiceProvider;

public class Alternative extends Entity {

	private int id;
	private Integer setId;
	private String timeOfDay;
	private ArrayList<TimeWindow> timeWindows;
	private boolean noPurchaseAlternative;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getSetId() {
		return setId;
	}

	public void setSetId(int setId) {
		this.setId = setId;
	}

	public String getTimeOfDay() {
		return timeOfDay;
	}

	public void setTimeOfDay(String timeOfDay) {
		this.timeOfDay = timeOfDay;
	}

	public ArrayList<TimeWindow> getTimeWindows() {
		if (this.timeWindows == null) {
			this.timeWindows = DataServiceProvider.getAlternativeDataServiceImplInstance()
					.getTimeWindowsByAlternativeId(this.id);
		}
		return timeWindows;
	}

	public void setTimeWindows(ArrayList<TimeWindow> timeWindows) {
		this.timeWindows = timeWindows;
	}

	public boolean getNoPurchaseAlternative() {
		return noPurchaseAlternative;
	}

	public void setNoPurchaseAlternative(boolean noPurchaseAlternative) {
		this.noPurchaseAlternative = noPurchaseAlternative;
	}
	
	@Override
	public boolean equals(Object o){
	   if(this==o){
	      return true;
	   }
	   if(o instanceof Alternative){
		   Alternative other = (Alternative) o;
	       return this.id == other.getId();
	   }
	   return false;
	}
	
	@Override
	public int hashCode(){
	   return this.id;
	}

}
