package logic.utility.comparator;

import java.util.Comparator;

import data.entity.RouteElement;

/**
 * Helps to sort insertion elements in routing
 * 
 * @author M. Lang
 *
 */
public class RouteElementTempShiftValueAscComparator implements Comparator<RouteElement> {

	public int compare(RouteElement a, RouteElement b) {

		return a.getTempShift() < b.getTempShift() ? -1
				: a.getTempShift() == b.getTempShift() ? 0 : 1;
	}
}
