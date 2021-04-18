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
public class RouteElementProfitDescComparator implements Comparator<RouteElement> {

	private double costMultiplier;
	
	public RouteElementProfitDescComparator(double costMultiplier) {
		this.costMultiplier = costMultiplier;
	}

	public int compare(RouteElement a, RouteElement b) {

		double valueA= a.getOrder().getOrderRequest().getBasketValue()-(a.getTempShiftWithoutWait()-a.getServiceTime())*costMultiplier;
		double valueB = b.getOrder().getOrderRequest().getBasketValue()-(b.getTempShiftWithoutWait()-b.getServiceTime())*costMultiplier;

		return valueA  > valueB  ? -1
				: valueA  == valueB  ? 0 : 1;
	}
}
