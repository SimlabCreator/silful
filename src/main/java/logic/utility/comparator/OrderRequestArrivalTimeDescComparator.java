package logic.utility.comparator;

import java.util.Comparator;

import data.entity.OrderRequest;

/**
 * Helps to sort order requests according to decreasing arrival time
 * @author M. Lang
 *
 */
public class OrderRequestArrivalTimeDescComparator implements Comparator<OrderRequest> {
	   
    public int compare(OrderRequest a, OrderRequest b) {
        return a.getArrivalTime() > b.getArrivalTime() ? -1 : a.getArrivalTime() == b.getArrivalTime() ? 0 : 1;
    }
}
