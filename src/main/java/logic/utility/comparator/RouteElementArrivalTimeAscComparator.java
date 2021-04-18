package logic.utility.comparator;

import java.util.Comparator;

import data.entity.Order;
import data.entity.OrderRequest;
import data.entity.RouteElement;

/**
 * Helps to sort order requests according to decreasing arrival time
 * @author M. Lang
 *
 */
public class RouteElementArrivalTimeAscComparator implements Comparator<RouteElement> {
	   
    public int compare(RouteElement a, RouteElement b) {
        return a.getOrder().getOrderRequest().getArrivalTime() < b.getOrder().getOrderRequest().getArrivalTime() ? -1 : a.getOrder().getOrderRequest().getArrivalTime() == b.getOrder().getOrderRequest().getArrivalTime() ? 0 : 1;
    }
}
