package data.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import data.utility.DataServiceProvider;
import logic.utility.comparator.TimeWindowEndAscComparator;
import logic.utility.comparator.TimeWindowStartAscComparator;

public class TimeWindowSet extends SetEntity{
	
	private String name;
	private Boolean overlapping;
	private Boolean sameLength;
	private Boolean continuous;
	private ArrayList<TimeWindow> timeWindows;
	private Double tempStartOfDeliveryPeriod;
	private Double tempEndOfDeliveryPeriod;
	private Double tempLengthOfDeliveryPeriod;
	HashMap<Integer, TimeWindow> timeWindowsById;
	
	public double getLengthOfLongestTimeWindow(double timeMultiplier){
		double length = 0.0;
		for(TimeWindow tw: timeWindows){
			if(tw.getEndTime()-tw.getStartTime()> length) length = tw.getEndTime()-tw.getStartTime();
		}
		return length*timeMultiplier;
	}
	
	@Override
	public ArrayList<TimeWindow> getElements() {
		if(this.timeWindows==null){
			this.timeWindows= DataServiceProvider.getTimeWindowDataServiceImplInstance().getAllElementsBySetId(id);
		}
		return this.timeWindows;
	}

	public void setElements(ArrayList<TimeWindow> elements) {
		this.timeWindows= elements;
		
	}

	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Boolean getOverlapping() {
		return overlapping;
	}
	public void setOverlapping(Boolean overlapping) {
		this.overlapping = overlapping;
	}
	public Boolean getSameLength() {
		return sameLength;
	}
	public void setSameLength(Boolean sameLength) {
		this.sameLength = sameLength;
	}
	public Boolean getContinuous() {
		return continuous;
	}
	public void setContinuous(Boolean continuous) {
		this.continuous = continuous;
	} 
	
	@Override
	public String toString() {
		
		return id+"; "+name;
	}
	public Double getTempStartOfDeliveryPeriod() {
		return tempStartOfDeliveryPeriod;
	}
	public void setTempStartOfDeliveryPeriod(Double tempStartOfDeliveryPeriod) {
		this.tempStartOfDeliveryPeriod = tempStartOfDeliveryPeriod;
	}
	public Double getTempEndOfDeliveryPeriod() {
		return tempEndOfDeliveryPeriod;
	}
	public void setTempEndOfDeliveryPeriod(Double tempEndOfDeliveryPeriod) {
		this.tempEndOfDeliveryPeriod = tempEndOfDeliveryPeriod;
	}
	public Double getTempLengthOfDeliveryPeriod() {
		return tempLengthOfDeliveryPeriod;
	}
	public void setTempLengthOfDeliveryPeriod(Double lengthOfDeliveryPeriod) {
		this.tempLengthOfDeliveryPeriod = lengthOfDeliveryPeriod;
	}
	
	@Override
	public boolean equals(Object o){
	   if(this==o){
	      return true;
	   }
	   if(o instanceof TimeWindowSet){
		   TimeWindowSet other = (TimeWindowSet) o;
	       return this.id == other.getId();
	   }
	   return false;
	}
	
	@Override
	public int hashCode(){

	   return this.id;
	}
	
	public TimeWindow getTimeWindowById(int timeWindowId){
		if(timeWindowsById==null){
			this.timeWindowsById = new HashMap<Integer, TimeWindow>();
			for(TimeWindow w: this.getElements()){
				this.timeWindowsById.put(w.getId(), w);
			};
			
		}
		
		return timeWindowsById.get(timeWindowId);
	}

	public void sortElementsAsc(){
		Collections.sort(this.getElements(), new TimeWindowStartAscComparator());
	}
	
	public void sortElementsByEndAsc(){
		Collections.sort(this.getElements(), new TimeWindowEndAscComparator());
	}
	
	public HashMap<TimeWindow, ArrayList<TimeWindow>> defineNeighborTimeWindows() {
		HashMap<TimeWindow, ArrayList<TimeWindow>> neighborsTw = new HashMap<TimeWindow, ArrayList<TimeWindow>>();
		for (TimeWindow tw : this.getElements()) {
			neighborsTw.put(tw, new ArrayList<TimeWindow>());
			for (TimeWindow twN : this.getElements()) {
				if (tw.getId() != twN.getId()) {
					if (twN.getStartTime() <= tw.getEndTime() && twN.getStartTime() >= tw.getStartTime()) {
						neighborsTw.get(tw).add(twN);
					} else if (twN.getEndTime() >= tw.getStartTime() && twN.getEndTime() <= tw.getEndTime()) {
						neighborsTw.get(tw).add(twN);
					}
				}
			}
		}
		
		return neighborsTw;
	}
}
