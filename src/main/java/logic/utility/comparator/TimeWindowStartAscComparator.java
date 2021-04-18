package logic.utility.comparator;

import java.util.Comparator;

import data.entity.TimeWindow;

/**
 * Helps to sort order requests according to decreasing arrival time
 * @author M. Lang
 *
 */
public class TimeWindowStartAscComparator implements Comparator<TimeWindow> {
	   
    public int compare(TimeWindow a, TimeWindow b) {
        return a.getStartTime() < b.getStartTime() ? -1 : a.getStartTime() == b.getStartTime() ? 0 : 1;
    }
}
