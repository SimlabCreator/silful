package logic.utility.comparator;

import java.util.Comparator;

import data.entity.RouteElement;

/**
 * Helps to sort insertion elements in routing
 * 
 * @author M. Lang
 *
 */
public class RouteElementTempShiftWithoutWaitingAscComparator implements Comparator<RouteElement> {

	public int compare(RouteElement a, RouteElement b) {

		return a.getTempShiftWithoutWait() < b.getTempShiftWithoutWait() ? -1
				: a.getTempShiftWithoutWait() == b.getTempShiftWithoutWait() ? 0 : 1;
	}
}
