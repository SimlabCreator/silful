package logic.algorithm.rm.optimization.acceptance;

import java.util.ArrayList;
import java.util.Collections;

import data.entity.Alternative;
import data.entity.AlternativeOffer;
import data.entity.CapacitySet;
import data.entity.Control;
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
import logic.service.support.ValueBucketService;
import logic.utility.comparator.OrderRequestArrivalTimeDescComparator;

/**
 * Applies standard serial nesting to static controls
 * 
 * @author M. Lang
 *
 */
public class SerialNesting implements AcceptanceAlgorithm {

	private ControlSet controlSet;
	private OrderRequestSet orderRequestSet;
	private CapacitySet capacitySet; // Needed to determine available
										// alternatives
	private OrderSet orderSet;

	public SerialNesting(ControlSet controlSet, CapacitySet capacitySet, OrderRequestSet orderRequestSet) {
		this.controlSet = controlSet;
		this.orderRequestSet = orderRequestSet;
		this.capacitySet = capacitySet;
	
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
			altCaps = CapacityService.getCapacitiesPerAlternative(this.capacitySet,
					this.controlSet.getAlternativeSet());
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
			order.setAssignedDeliveryAreaId(area.getId());

			/// Identify available alternatives
			ArrayList<Alternative> availableAlternatives = AcceptanceService.getAvailableAlternatives(area, altCaps);
			order.setAvailableAlternatives(availableAlternatives);

			if (availableAlternatives.size() > 0) { // If there are still time
													// windows available
				/// Sample selection from customer (as it is handelned as if
				/// independent demand, sample for whole alternative set)
				// TODO: Bug if you want to use independent demand with psychic
				/// forecast because here we sample again
				ArrayList<AlternativeOffer> offeredAlternatives = new ArrayList<AlternativeOffer>();
				for (AlternativeCapacity altC : altCaps) {
					AlternativeOffer offer = new AlternativeOffer();
					offer.setAlternativeId(altC.getAlternativeId());
					offer.setAlternative(altC.getAlternative());
					offer.setOrder(order);
					offeredAlternatives.add(offer);
				}

				CustomerDemandService.sampleCustomerDemand(offeredAlternatives, order);

				/// Check if controls allow acceptance: (if did not choose no
				/// purchase alternative)

				if (order.getSelectedAlternativeId() != null) {
					/// 1. Get relevant controls
					ArrayList<Control> relevantControls = ValueBucketService
							.getControlsForDeliveryAreaAndAlternativeSortedByValueBucketAscending(area.getId(),
									order.getSelectedAlternative().getId(), this.controlSet.getElements());

					/// 2. Find relevant value bucket
					int valueBucketID = relevantControls.size() - 1;// Assign to
																	// highest
																	// if it
																	// does not
																	// fit
					for (int cID = 0; cID < relevantControls.size(); cID++) {
						if (relevantControls.get(cID).getValueBucket().getUpperBound() >= request.getBasketValue()) {
							valueBucketID = cID;
						}
					}

					/// 3. Check controls

					if (relevantControls.get(valueBucketID).getControlNumber() > 0) {// Accept
																						// request
						order.setAccepted(true);

						// Decrease limit of upper value buckets by one unit
						for (int cID = valueBucketID; cID < relevantControls.size(); cID++) {
							relevantControls.get(cID)
									.setControlNumber(relevantControls.get(cID).getControlNumber() - 1);
						}

						// Decrease limit of lower value buckets if they exceed
						// the limit of the next higher class (backwards!!)
						for (int cID = valueBucketID - 1; cID >= 0; cID--) {
							if (relevantControls.get(cID).getControlNumber() > relevantControls.get(cID + 1)
									.getControlNumber()) {
								relevantControls.get(cID)
										.setControlNumber(relevantControls.get(cID).getControlNumber() - 1);
							}
						}

						// Reduce capacities for available alternative
						// determination
						// Reduce left over capacity
						AcceptanceService.reduceCapacityOfAlternative(altCaps, order.getSelectedAlternativeId(),
								area.getId());

					} else {// Reject request
						order.setAccepted(false);
						order.setReasonRejection("Acceptance limit of 0");
					}
				} else {
					order.setAccepted(false);
					order.setReasonRejection("Customer selected no-purchase option");
				}

			} else {// The customer can not choose anything because there are no
					// capacities left
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
