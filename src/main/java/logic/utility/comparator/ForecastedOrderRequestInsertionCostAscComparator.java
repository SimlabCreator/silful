package logic.utility.comparator;

import java.util.Comparator;

import logic.entity.ForecastedOrderRequest;

/**
 * Helps to sort order requests according to decreasing arrival time
 * @author M. Lang
 *
 */
public class ForecastedOrderRequestInsertionCostAscComparator implements Comparator<ForecastedOrderRequest> {
	   
    public int compare(ForecastedOrderRequest a, ForecastedOrderRequest b) {
        return a.getLowestPossibleInsertionCosts()< b.getLowestPossibleInsertionCosts() ? -1 : a.getLowestPossibleInsertionCosts().equals(b.getLowestPossibleInsertionCosts()) ? 0 : 1;
    }
}
