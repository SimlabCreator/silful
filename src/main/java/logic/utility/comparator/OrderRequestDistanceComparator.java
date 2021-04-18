package logic.utility.comparator;

import java.util.Comparator;

import data.entity.OrderRequest;
import logic.service.support.LocationService;

/**
 * Helps to sort order requests according to decreasing arrival time
 * 
 * @author M. Lang
 *
 */
public class OrderRequestDistanceComparator implements Comparator<OrderRequest> {

	private OrderRequest request;

	public OrderRequestDistanceComparator(OrderRequest request) {
		this.request = request;
	}

	public int compare(OrderRequest a, OrderRequest b) {
		
		double distance1 = LocationService.calculateHaversineDistanceBetweenCustomers(request.getCustomer(),a.getCustomer());
		
		double distance2 = LocationService.calculateHaversineDistanceBetweenCustomers(request.getCustomer(), b.getCustomer());
		
		
		return (distance1 < distance2) ? -1
				: (distance1 == distance2) ? 0 : 1;
	}
}
