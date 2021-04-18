package logic.utility.comparator;

import java.util.Comparator;

import logic.entity.ForecastedOrderRequest;

/**
 * Helps to sort order requests according to decreasing arrival time
 * @author M. Lang
 *
 */
public class ForecastedOrderRequestSelectedAlternativeDescComparator implements Comparator<ForecastedOrderRequest> {
	   
    public int compare(ForecastedOrderRequest a, ForecastedOrderRequest b) {
    	
    	 return a.getAlternativePreferenceList().get(1).getId()> b.getAlternativePreferenceList().get(1).getId() ? -1 : a.getAlternativePreferenceList().get(1).getId()==b.getAlternativePreferenceList().get(1).getId() ? 0 : 1;

    }
}
