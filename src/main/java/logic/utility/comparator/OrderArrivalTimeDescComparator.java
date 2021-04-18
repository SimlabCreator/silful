package logic.utility.comparator;

import java.util.Comparator;

import data.entity.Order;
import data.entity.OrderRequest;

/**
 * Helps to sort order requests according to decreasing arrival time
 * @author M. Lang
 *
 */
public class OrderArrivalTimeDescComparator implements Comparator<Order> {
	   
    public int compare(Order a, Order b) {
        return a.getOrderRequest().getArrivalTime() > b.getOrderRequest().getArrivalTime() ? -1 : a.getOrderRequest().getArrivalTime() == b.getOrderRequest().getArrivalTime() ? 0 : 1;
    }
}
