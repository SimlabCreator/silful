package logic.utility.comparator;

import java.util.Comparator;

import data.entity.TimeWindow;

/**
 * Helps to sort order requests according to decreasing arrival time
 * @author M. Lang
 *
 */
public class TimeWindowEndAscComparator implements Comparator<TimeWindow> {
	   
    public int compare(TimeWindow a, TimeWindow b) {
        return a.getEndTime() < b.getEndTime() ? -1 : a.getEndTime() == b.getEndTime() ? 0 : 1;
    }
}
