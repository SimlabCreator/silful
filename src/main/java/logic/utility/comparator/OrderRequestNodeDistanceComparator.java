package logic.utility.comparator;

import java.util.Comparator;
import java.util.HashMap;

import data.entity.OrderRequest;

/**
 * Helps to sort order requests according to decreasing arrival time
 * 
 * @author M. Lang
 *
 */
public class OrderRequestNodeDistanceComparator implements Comparator<OrderRequest> {

	private OrderRequest request;
	private HashMap<Long, HashMap<Long, Double>> distanceMatrix;

	public OrderRequestNodeDistanceComparator(OrderRequest request, HashMap<Long, HashMap<Long, Double>> distanceMatrix) {
		this.request = request;
		this.distanceMatrix=distanceMatrix;
	}

	public int compare(OrderRequest a, OrderRequest b) {
		
		double distance1a = this.distanceMatrix.get(request.getCustomer().getClosestNodeId()).get(a.getCustomer().getClosestNodeId());
		double distance1b = this.distanceMatrix.get(a.getCustomer().getClosestNodeId()).get(request.getCustomer().getClosestNodeId());
		
		double distance2a = this.distanceMatrix.get(request.getCustomer().getClosestNodeId()).get(b.getCustomer().getClosestNodeId());
		double distance2b = this.distanceMatrix.get(b.getCustomer().getClosestNodeId()).get(request.getCustomer().getClosestNodeId());
		
		
		return (distance1a+distance1b < distance2a+distance2b) ? -1
				: (distance1a+distance1b == distance2a+distance2b) ? 0 : 1;
	}
}
