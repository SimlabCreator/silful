package logic.service.support;

import java.util.ArrayList;
import java.util.HashMap;

import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.Entity;
import data.entity.Order;
import data.entity.OrderRequestSet;
import data.entity.OrderSet;
import data.entity.Route;
import data.entity.RouteElement;
import data.entity.Routing;

public class EvaluationService {

	public static void determineAverageOrderValuePerDeliveryArea(
			HashMap<Integer, ArrayList<Routing>> routingsForBenchmarking,
			HashMap<Integer, ArrayList<OrderSet>> orderSetsForBenchmarking,
			HashMap<DeliveryArea, ArrayList<Double>> benchmarkValues,
			ArrayList<OrderRequestSet> benchmarkingOrderRequestSets, DeliveryAreaSet das, double maximumRevenueValue, HashMap<Entity, Object> objectiveSpecificValues) {

		boolean collectOrderRequestSets=true;
		if(routingsForBenchmarking!=null){
		for (Integer expId : routingsForBenchmarking.keySet()) {
			HashMap<DeliveryArea, Double> values = new HashMap<DeliveryArea, Double>();
			for(Routing r: routingsForBenchmarking.get(expId)){
				if(collectOrderRequestSets) benchmarkingOrderRequestSets.add(r.getOrderSet().getOrderRequestSet());
				for(Route rou : r.getRoutes()){
					for(RouteElement e: rou.getRouteElements()){
						DeliveryArea area = LocationService
								.assignCustomerToDeliveryArea(das, e.getOrder().getOrderRequest().getCustomer());
						if(!values.containsKey(area)){
							values.put(area, 0.0);
						}
						values.put(area,values.get(area)+CustomerDemandService.calculateOrderValue(e.getOrder().getOrderRequest(),
								maximumRevenueValue, objectiveSpecificValues));
					}
					
				}
			
			}
			
			for(DeliveryArea a: values.keySet()){
				if(!benchmarkValues.containsKey(a)){
					benchmarkValues.put(a, new ArrayList<Double>());
				}
				benchmarkValues.get(a).add(values.get(a)/routingsForBenchmarking.get(expId).size());
			}
			collectOrderRequestSets=false;
		}
		}
		if(orderSetsForBenchmarking!=null){
			for (Integer expId : orderSetsForBenchmarking.keySet()) {
				HashMap<DeliveryArea, Double> values = new HashMap<DeliveryArea, Double>();
				for(OrderSet os: orderSetsForBenchmarking.get(expId)){
					if(collectOrderRequestSets) benchmarkingOrderRequestSets.add(os.getOrderRequestSet());
					for(Order o: os.getElements()){
						
						if(o.getAccepted()){
							DeliveryArea area = LocationService
									.assignCustomerToDeliveryArea(das, o.getOrderRequest().getCustomer());
							if(!values.containsKey(area)){
								values.put(area, 0.0);
							}
							values.put(area,values.get(area)+CustomerDemandService.calculateOrderValue(o.getOrderRequest(),
									maximumRevenueValue, objectiveSpecificValues));
						}
					}
				}	
					
				for(DeliveryArea a: values.keySet()){
					if(!benchmarkValues.containsKey(a)){
						benchmarkValues.put(a, new ArrayList<Double>());
					}
					benchmarkValues.get(a).add(values.get(a)/orderSetsForBenchmarking.get(expId).size());
				}
				collectOrderRequestSets=false;
			}
		}
	}
}
