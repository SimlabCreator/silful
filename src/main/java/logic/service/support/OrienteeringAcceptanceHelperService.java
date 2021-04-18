package logic.service.support;

import java.util.ArrayList;
import java.util.HashMap;

import data.entity.DeliveryArea;
import data.entity.Order;
import data.entity.Routing;
import data.entity.TimeWindow;

public class OrienteeringAcceptanceHelperService {

	public static void updateOrienteeringNoInformation(DeliveryArea subArea, Order order, TimeWindow relevantAcceptedTw,
			HashMap<Routing, HashMap<DeliveryArea, HashMap<TimeWindow, Integer>>> aggregateInformationNo,
			HashMap<Integer, HashMap<Integer, Integer>> alreadyAcceptedPerSubDeliveryAreaAndTimeWindow,
			HashMap<TimeWindow, HashMap<Routing, DeliveryArea>> stealingAreaPerTimeWindowAndRouting,
			HashMap<TimeWindow, HashMap<Routing, ArrayList<DeliveryArea>>> advancedStealingAreasPerTimeWindowAndRouting,
			boolean thefting, boolean theftingAdvanced,
			HashMap<Routing, HashMap<DeliveryArea, ArrayList<Order>>> acceptedOrdersPerTOP,
			HashMap<Integer, HashMap<Routing, Integer>> amountTheftsPerDeliveryArea,
			HashMap<Integer, HashMap<Routing, Integer>> amountAdvancedTheftsPerDeliveryArea,
			HashMap<Integer, HashMap<Routing, Integer>> amountTimeTheftsPerDeliveryArea,
			HashMap<Integer, HashMap<Routing, Integer>> firstTheftsPerDeliveryArea,
			HashMap<Integer, HashMap<Routing, Integer>> firstAdvancedTheftsPerDeliveryArea,
			HashMap<Integer, HashMap<Routing, Integer>> firstTimeTheftsPerDeliveryArea) {

		ArrayList<Routing> toRemove = new ArrayList<Routing>();
		for (Routing r : aggregateInformationNo.keySet()) {
			if (acceptedOrdersPerTOP != null && !acceptedOrdersPerTOP.containsKey(r)) {
				acceptedOrdersPerTOP.put(r, new HashMap<DeliveryArea, ArrayList<Order>>());
			}

			HashMap<DeliveryArea, HashMap<TimeWindow, Integer>> cap = aggregateInformationNo.get(r);
			boolean capacityTaken = false;
			if (cap.containsKey(subArea) && cap.get(subArea).containsKey(relevantAcceptedTw)) {
				int currentCap = cap.get(subArea).get(relevantAcceptedTw);
				if (currentCap > 0) {
					cap.get(subArea).put(relevantAcceptedTw, currentCap - 1);
					if (acceptedOrdersPerTOP != null && !acceptedOrdersPerTOP.get(r).containsKey(subArea)) {
						acceptedOrdersPerTOP.get(r).put(subArea, new ArrayList<Order>());
					}
					if (acceptedOrdersPerTOP != null) {
						acceptedOrdersPerTOP.get(r).get(subArea).add(order);
					}

					capacityTaken = true;
				}
			}
			// Already accepted in this area? -> can steal
			// from neighbors
			if (!capacityTaken && thefting
					&& stealingAreaPerTimeWindowAndRouting.containsKey(relevantAcceptedTw)
					&& stealingAreaPerTimeWindowAndRouting.get(relevantAcceptedTw).containsKey(r)) {

				// Steal from neighbor with lowest opportunity costs

				cap.get(stealingAreaPerTimeWindowAndRouting.get(relevantAcceptedTw).get(r)).put(
						relevantAcceptedTw,
						cap.get(stealingAreaPerTimeWindowAndRouting.get(relevantAcceptedTw).get(r))
								.get(relevantAcceptedTw) - 1);
				if (amountTheftsPerDeliveryArea != null
						&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.containsKey(subArea.getId())
						&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId())
								.containsKey(relevantAcceptedTw.getId())
						&& alreadyAcceptedPerSubDeliveryAreaAndTimeWindow.get(subArea.getId())
								.get(relevantAcceptedTw.getId()) > 0) {
					amountTheftsPerDeliveryArea.get(subArea.getDeliveryAreaOfSet().getId()).put(r,
							amountTheftsPerDeliveryArea.get(subArea.getDeliveryAreaOfSet().getId()).get(r) + 1);
					if(!firstTheftsPerDeliveryArea.get(subArea.getDeliveryAreaOfSet().getId()).containsKey(r)){
						firstTheftsPerDeliveryArea.get(subArea.getDeliveryAreaOfSet().getId()).put(r, order.getOrderRequestId());
					}

				} else if (amountTimeTheftsPerDeliveryArea != null) {
					amountTimeTheftsPerDeliveryArea.get(subArea.getDeliveryAreaOfSet().getId()).put(r,
							amountTimeTheftsPerDeliveryArea.get(subArea.getDeliveryAreaOfSet().getId()).get(r) + 1);
					if(!firstTimeTheftsPerDeliveryArea.get(subArea.getDeliveryAreaOfSet().getId()).containsKey(r)){
						firstTimeTheftsPerDeliveryArea.get(subArea.getDeliveryAreaOfSet().getId()).put(r, order.getOrderRequestId());
					}

				}
				capacityTaken = true;
				if (acceptedOrdersPerTOP != null && !acceptedOrdersPerTOP.get(r)
						.containsKey(stealingAreaPerTimeWindowAndRouting.get(relevantAcceptedTw).get(r))) {
					acceptedOrdersPerTOP.get(r).put(
							stealingAreaPerTimeWindowAndRouting.get(relevantAcceptedTw).get(r),
							new ArrayList<Order>());
				}
				if (acceptedOrdersPerTOP != null) {
					acceptedOrdersPerTOP.get(r)
							.get(stealingAreaPerTimeWindowAndRouting.get(relevantAcceptedTw).get(r)).add(order);
				}

			} else if (!capacityTaken && theftingAdvanced
					&& advancedStealingAreasPerTimeWindowAndRouting.containsKey(relevantAcceptedTw)
					&& advancedStealingAreasPerTimeWindowAndRouting.get(relevantAcceptedTw).containsKey(r)) {
				if (amountAdvancedTheftsPerDeliveryArea != null) {
					amountAdvancedTheftsPerDeliveryArea.get(subArea.getDeliveryAreaOfSet().getId()).put(r,
							amountAdvancedTheftsPerDeliveryArea.get(subArea.getDeliveryAreaOfSet().getId()).get(r) + 1);
					if(!firstAdvancedTheftsPerDeliveryArea.get(subArea.getDeliveryAreaOfSet().getId()).containsKey(r)){
						firstAdvancedTheftsPerDeliveryArea.get(subArea.getDeliveryAreaOfSet().getId()).put(r, order.getOrderRequestId());
					}
				}
				if (advancedStealingAreasPerTimeWindowAndRouting.get(relevantAcceptedTw).get(r).size() > 1) {
					cap.get(advancedStealingAreasPerTimeWindowAndRouting.get(relevantAcceptedTw).get(r).get(0))
							.put(relevantAcceptedTw, cap
											.get(advancedStealingAreasPerTimeWindowAndRouting
													.get(relevantAcceptedTw).get(r).get(0))
											.get(relevantAcceptedTw) - 1);
					cap.get(advancedStealingAreasPerTimeWindowAndRouting.get(relevantAcceptedTw).get(r).get(1))
							.put(relevantAcceptedTw, cap
											.get(advancedStealingAreasPerTimeWindowAndRouting
													.get(relevantAcceptedTw).get(r).get(1))
											.get(relevantAcceptedTw) - 1);
				} else {
					cap.get(advancedStealingAreasPerTimeWindowAndRouting.get(relevantAcceptedTw).get(r).get(0))
							.put(relevantAcceptedTw, cap
											.get(advancedStealingAreasPerTimeWindowAndRouting
													.get(relevantAcceptedTw).get(r).get(0))
											.get(relevantAcceptedTw) - 2);
				}
				capacityTaken = true;
			}

			if (!capacityTaken) {

				toRemove.add(r);
			}
		}

		for (Routing r : toRemove) {
			aggregateInformationNo.remove(r);
			if (acceptedOrdersPerTOP != null)
				acceptedOrdersPerTOP.remove(r);
		}
	}
}
