package logic.utility.comparator;

import java.util.Comparator;

import data.entity.RouteElement;
import logic.service.support.ProbabilityDistributionService;
import logic.utility.exceptions.ParameterUnknownException;

/**
 * Helps to sort insertion elements in routing
 * 
 * @author M. Lang
 *
 */
public class RouteElementTempValueDescComparator implements Comparator<RouteElement> {

	public RouteElementTempValueDescComparator() {

	}

	public int compare(RouteElement a, RouteElement b) {

		//If the value is the same, the elements with the lower cost is better
		boolean chooseA = a.getTempValue() > b.getTempValue();
		boolean same = a.getTempValue() == b.getTempValue();
		return chooseA ? -1 : same ?  0 : 1;
	}
}
