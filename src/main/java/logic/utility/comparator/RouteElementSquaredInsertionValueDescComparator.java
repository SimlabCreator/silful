package logic.utility.comparator;

import java.util.Comparator;

import data.entity.RouteElement;

/**
 * Helps to sort insertion elements in routing
 * @author M. Lang
 *
 */
public class RouteElementSquaredInsertionValueDescComparator implements Comparator<RouteElement> {
	   
    public int compare(RouteElement a, RouteElement b) {
 
        return Math.sqrt(a.getOrder().getOrderRequest().getBasketValue())/a.getTempShift() > Math.sqrt(b.getOrder().getOrderRequest().getBasketValue())/b.getTempShift() ? -1 : Math.sqrt(a.getOrder().getOrderRequest().getBasketValue())/a.getTempShift() == Math.sqrt(b.getOrder().getOrderRequest().getBasketValue())/b.getTempShift() ? 0 : 1;
    }
}
