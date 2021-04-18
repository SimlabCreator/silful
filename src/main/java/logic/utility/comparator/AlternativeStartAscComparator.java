package logic.utility.comparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import data.entity.Alternative;
import data.entity.TimeWindow;

/**
 * Helps to sort alternatives regarding their start time
 * @author M. Lang
 *
 */
public class AlternativeStartAscComparator implements Comparator<Alternative> {
	   
    public int compare(Alternative a, Alternative b) {
    	
    	//Sort time windows of alternatives by earliest start time
    	ArrayList<TimeWindow> timeWindowsA = a.getTimeWindows();
    	ArrayList<TimeWindow> timeWindowsASorted = new ArrayList<TimeWindow>();
    	for(int i=0; i < timeWindowsA.size(); i++){
    		timeWindowsASorted.add(timeWindowsA.get(i));
    	}
    	Collections.sort(timeWindowsASorted, new TimeWindowStartAscComparator());
    	
    	ArrayList<TimeWindow> timeWindowsB = b.getTimeWindows();
    	ArrayList<TimeWindow> timeWindowsBSorted = new ArrayList<TimeWindow>();
    	for(int i=0; i < timeWindowsB.size(); i++){
    		timeWindowsBSorted.add( timeWindowsB.get(i));
    	}
    	Collections.sort(timeWindowsBSorted, new TimeWindowStartAscComparator());
    	
    	//If the earliest time window of an alternative starts before the earliest of the other, it should come first
        return timeWindowsASorted.get(0).getStartTime() < timeWindowsBSorted.get(0).getStartTime() ? -1 : timeWindowsASorted.get(0).getStartTime() == timeWindowsBSorted.get(0).getStartTime() ? 0 : 1;
    }
}
