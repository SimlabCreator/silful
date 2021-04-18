package logic.utility.comparator;

import java.util.Comparator;

import logic.entity.ForecastedOrderRequest;

/**
 * Helps to sort order requests according to decreasing arrival time
 * @author M. Lang
 *
 */
public class ForecastedOrderRequestValueDescComparator implements Comparator<ForecastedOrderRequest> {
	   
    public int compare(ForecastedOrderRequest a, ForecastedOrderRequest b) {
        return a.getEstimatedValue()> b.getEstimatedValue() ? -1 : a.getEstimatedValue().equals(b.getEstimatedValue()) ? 0 : 1;
    }
}
