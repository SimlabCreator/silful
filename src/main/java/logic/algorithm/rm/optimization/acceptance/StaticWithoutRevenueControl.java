package logic.algorithm.rm.optimization.acceptance;

import java.util.ArrayList;
import java.util.Collections;

import data.entity.Alternative;
import data.entity.AlternativeOffer;
import data.entity.AlternativeSet;
import data.entity.CapacitySet;
import data.entity.ControlSet;
import data.entity.DeliveryArea;
import data.entity.Order;
import data.entity.OrderRequest;
import data.entity.OrderRequestSet;
import data.entity.OrderSet;
import logic.entity.AlternativeCapacity;
import logic.service.support.AcceptanceService;
import logic.service.support.CapacityService;
import logic.service.support.CustomerDemandService;
import logic.service.support.LocationService;
import logic.utility.comparator.OrderRequestArrivalTimeDescComparator;

/**
 * Static assignment without revenue control
 */

public class StaticWithoutRevenueControl implements AcceptanceAlgorithm{

	private ControlSet controlSet;
	private CapacitySet capacitySet;
	private OrderRequestSet orderRequestSet;
	private AlternativeSet alternativeSet;
	private OrderSet orderSet;

	public StaticWithoutRevenueControl(ControlSet controlSet, CapacitySet capacitySet,
			OrderRequestSet orderRequestSet, AlternativeSet alternativeSet) {
		this.controlSet = controlSet;
		this.capacitySet = capacitySet;
		this.orderRequestSet = orderRequestSet;
		this.alternativeSet = alternativeSet;
	}
	


	public void start() {

		orderSet = new OrderSet();
		orderSet.setOrderRequestSetId(this.orderRequestSet.getId());
		orderSet.setOrderRequestSet(this.orderRequestSet);
		orderSet.setControlSet(controlSet);
		orderSet.setControlSetId(controlSet.getId());
		ArrayList<Order> orders = new ArrayList<Order>();

		// Sort order requests (descending arrival time)
		ArrayList<OrderRequest> orderRequests = this.orderRequestSet.getElements();
		Collections.sort(orderRequests, new OrderRequestArrivalTimeDescComparator());

		// Determine capacities per alternatives if not yet available
		ArrayList<AlternativeCapacity> altCaps = this.controlSet.getAlternativeCapacities();
		if (altCaps == null) {
			altCaps = CapacityService.getCapacitiesPerAlternative(this.capacitySet, this.alternativeSet);
		}
		

		// Go through order requests and produce orders
		for (int requestIndex = 0; requestIndex < orderRequests.size(); requestIndex++) {

			OrderRequest request = orderRequests.get(requestIndex);

			// Initialize new order
			Order order = new Order();
			order.setOrderRequestId(request.getId());
			order.setOrderRequest(request);

			// Identify delivery area of customer location
			DeliveryArea area = LocationService.assignCustomerToDeliveryArea(this.capacitySet.getDeliveryAreaSet(),
					request.getCustomer());

			/// Identify available alternatives
			ArrayList<Alternative> availableAlternatives = AcceptanceService.getAvailableAlternatives(area, altCaps);
			order.setAvailableAlternatives(availableAlternatives);

			/// All available alternatives are offered
			ArrayList<AlternativeOffer> offeredAlternatives = new ArrayList<AlternativeOffer>();
			for(int i=0; i< availableAlternatives.size(); i++){
				AlternativeOffer offer=new AlternativeOffer();
				offer.setAlternativeId(availableAlternatives.get(i).getId());
				offer.setAlternative(availableAlternatives.get(i));
				offeredAlternatives.add(offer);
			}
			order.setOfferedAlternatives(offeredAlternatives);

			if (offeredAlternatives.size() > 0) { //If there are still time windows available
				// Sample selection from customer
				CustomerDemandService.sampleCustomerDemand(offeredAlternatives, order);

				// Check if selected is actually offered. If yes, accept. If
				// nothing
				// was selected or selected is not offered (possible in
				// independent
				// demand), do not accept.
				
				if (order.getSelectedAlternativeId() != null) {
					boolean offered = false;
					for (int i = 0; i < offeredAlternatives.size(); i++) {
						if (offeredAlternatives.get(i).getAlternativeId()== order.getSelectedAlternativeId()) {
							offered = true;
							
							// Reduce left over capacity
							AcceptanceService.reduceCapacityOfAlternative(altCaps, offeredAlternatives.get(i).getAlternativeId(), area.getId());
							break;
						}
					}
					
					order.setAccepted(offered);
					if(!offered){
						order.setReasonRejection("Independent choice not offered - not available");
					}
				}else{
					order.setAccepted(false);
					order.setReasonRejection("Customer chooses no-purchase option");
				}
				
			}else{//The customer can not choose anything because there are no capacities left
				order.setAccepted(false);
				order.setReasonRejection("No capacity left for delivery area");
			}
			
			orders.add(order);
		}
		
		orderSet.setElements(orders);

	}

	public OrderSet getResult() {
		return this.orderSet;
	}

	

}
