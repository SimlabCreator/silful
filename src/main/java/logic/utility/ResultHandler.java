package logic.utility;

import java.util.ArrayList;
import java.util.HashMap;

import data.entity.Capacity;
import data.entity.CapacitySet;
import data.entity.ControlSet;
import data.entity.DeliveryArea;
import data.entity.DemandSegmentForecastSet;
import data.entity.DynamicProgrammingTree;
import data.entity.GeneralAtomicOutputValue;
import data.entity.Order;
import data.entity.OrderSet;
import data.entity.Route;
import data.entity.RouteElement;
import data.entity.Routing;
import data.entity.RoutingAssignment;
import data.entity.ValueBucketForecastSet;
import data.entity.ValueFunctionApproximationModelSet;
import data.utility.DataServiceProvider;
import logic.algorithm.AtomicOutputAlgorithm;
import logic.algorithm.rm.forecasting.DemandSegmentForecastingAlgorithm;
import logic.algorithm.rm.forecasting.ValueBucketForecastingAlgorithm;
import logic.algorithm.rm.optimization.acceptance.AcceptanceAlgorithm;
import logic.algorithm.rm.optimization.control.ControlAlgorithm;
import logic.algorithm.rm.optimization.control.DynamicProgrammingAlgorithm;
import logic.algorithm.rm.optimization.control.ValueFunctionApproximationAlgorithm;
import logic.algorithm.vr.RoutingAlgorithm;
import logic.algorithm.vr.capacity.CapacityAlgorithm;

/**
 * Helper that saves result in database and assigns to setting
 * 
 * @author M. Lang
 *
 */
public class ResultHandler {

	public static ControlSet organizeControlSetResult(ControlAlgorithm algo, int periodNumber) {
		if(!SettingsProvider.saveResults) return null;
		ControlSet cSet = algo.getResult();

		NameProvider.setNameControlSet(cSet);
		Integer controlSetId = DataServiceProvider.getControlDataServiceImplInstance().persistCompleteEntitySet(cSet);

		if (periodNumber == 0) {
			SettingsProvider.getPeriodSetting().setControlSetId(controlSetId);
		} else {
			SettingsProvider.getPeriodSettingFollower(periodNumber).setControlSetId(controlSetId);
		}
		return cSet;
	}

	public static DynamicProgrammingTree organizeDynamicProgrammingTreeResult(DynamicProgrammingAlgorithm algo,
			int periodNumber) {
		if(!SettingsProvider.saveResults) return null;
		DynamicProgrammingTree tree = algo.getResult();

		NameProvider.setNameDynamicProgrammingTree(tree);
		Integer treeId = DataServiceProvider.getControlDataServiceImplInstance().persistDynamicProgrammingTree(tree);

		if (periodNumber == 0) {
			SettingsProvider.getPeriodSetting().setDynamicProgrammingTreeId(treeId);
		} else {
			SettingsProvider.getPeriodSettingFollower(periodNumber).setDynamicProgrammingTreeId(treeId);
		}
		return tree;
	}

	public static CapacitySet organizeCapacitySetResult(CapacityAlgorithm algo, int periodNumber) {
		if(!SettingsProvider.saveResults) return null;
		CapacitySet cSet = algo.getResult();

		if (cSet.getDeliveryAreaSet() != null) {
			
			//If the delivery areas are not predefined, they need to be saved first
			HashMap<Integer, Integer> oldToNewAreaId = new HashMap<Integer, Integer>();
			boolean newAreas = false;
			if (!cSet.getDeliveryAreaSet().isHierarchy() && !cSet.getDeliveryAreaSet().isPredefined()) {
				newAreas = true;
				DataServiceProvider.getDeliveryAreaDataServiceImplInstance()
						.updateDeliveryAreaSetToPredefined(cSet.getDeliveryAreaSet());
				for (DeliveryArea area : cSet.getDeliveryAreaSet().getElements()) {
					area.setSetId(cSet.getDeliveryAreaSetId());
					oldToNewAreaId.put(area.getId(),
							DataServiceProvider.getDeliveryAreaDataServiceImplInstance().persistElement(area));
				}
			} else if (cSet.getDeliveryAreaSet().isHierarchy()) {

				for (DeliveryArea area : cSet.getDeliveryAreaSet().getElements()) {

					if (area.getSubset() != null && !area.getSubset().isPredefined()) {
						newAreas = true;
						DataServiceProvider.getDeliveryAreaDataServiceImplInstance()
						.updateDeliveryAreaSetToPredefined(area.getSubset());
						for (DeliveryArea areaS : area.getSubset().getElements()) {
							areaS.setSetId(area.getSubsetId());
							oldToNewAreaId.put(areaS.getId(),
									DataServiceProvider.getDeliveryAreaDataServiceImplInstance().persistElement(areaS));
						}

					}
				}
			}
			if (newAreas) {
				for (Capacity cap : cSet.getElements()) {
					cap.setDeliveryAreaId(oldToNewAreaId.get(cap.getDeliveryAreaId()));
					cap.setDeliveryArea(null);
				}
			}

		}

		NameProvider.setNameCapacitySet(cSet);
		Integer capacitySetId = DataServiceProvider.getCapacityDataServiceImplInstance().persistCompleteEntitySet(cSet);

		if (periodNumber == 0) {
			SettingsProvider.getPeriodSetting().setCapacitySetId(capacitySetId);
		} else {
			SettingsProvider.getPeriodSettingFollower(periodNumber).setCapacitySetId(capacitySetId);
		}
		return cSet;
	}
	
	public static GeneralAtomicOutputValue organizeAtomicOutputResult(AtomicOutputAlgorithm algo, int periodNumber){
		if(!SettingsProvider.saveResults) return null;
		GeneralAtomicOutputValue value = algo.getResult();
		if (periodNumber == 0) {
			SettingsProvider.getPeriodSetting().addAtomicOutput(value);
		} else {
			SettingsProvider.getPeriodSettingFollower(periodNumber).addAtomicOutput(value);
		}
		
		return value;
	}

	public static ValueBucketForecastSet organizeValueBucketForecastSetResult(ValueBucketForecastingAlgorithm algo,
			int periodNumber) {
		if(!SettingsProvider.saveResults) return null;
		ValueBucketForecastSet dSet = algo.getResult();

		NameProvider.setNameValueBucketForecastSet(dSet);

		Integer forecastSetId = DataServiceProvider.getValueBucketForecastDataServiceImplInstance()
				.persistCompleteEntitySet(dSet);

		if (periodNumber == 0) {
			SettingsProvider.getPeriodSetting().setValueBucketForecastSetId(forecastSetId);
		} else {
			SettingsProvider.getPeriodSettingFollower(periodNumber).setValueBucketForecastSetId(forecastSetId);
		}

		return dSet;
	}

	public static DemandSegmentForecastSet organizeDemandSegmentForecastSetResult(
			DemandSegmentForecastingAlgorithm algo, int periodNumber) {
		if(!SettingsProvider.saveResults) return null;
		DemandSegmentForecastSet dSet = algo.getResult();
		NameProvider.setNameDemandSegmentForecastSet(dSet);
		Integer forecastSetId = DataServiceProvider.getDemandSegmentForecastDataServiceImplInstance()
				.persistCompleteEntitySet(dSet);

		if (periodNumber == 0) {
			SettingsProvider.getPeriodSetting().setDemandSegmentForecastSetId(forecastSetId);
		} else {
			SettingsProvider.getPeriodSettingFollower(periodNumber).setDemandSegmentForecastSetId(forecastSetId);
		}

		return dSet;
	}

	public static OrderSet organizeOrderSetResult(AcceptanceAlgorithm algo, int periodNumber) {
		
		if(!SettingsProvider.saveResults) return null;
		OrderSet oSet = algo.getResult();

		NameProvider.setNameOrderSet(oSet);
		Integer orderSetId = DataServiceProvider.getOrderDataServiceImplInstance().persistCompleteEntitySet(oSet);

		if (periodNumber == 0) {
			SettingsProvider.getPeriodSetting().setOrderSetId(orderSetId);
		} else {
			SettingsProvider.getPeriodSettingFollower(periodNumber).setOrderSetId(orderSetId);
		}

		return oSet;
	}

	public static ValueFunctionApproximationModelSet organizeValueFunctionApproximationModelSetResult(
			ValueFunctionApproximationAlgorithm algo, int periodNumber) {
		if(!SettingsProvider.saveResults) return null;
		
		ValueFunctionApproximationModelSet modelSet = algo.getResult();

		NameProvider.setNameValueFunctionApproximationSet(modelSet);
		Integer modelSetId = DataServiceProvider.getValueFunctionApproximationDataServiceImplInstance()
				.persistCompleteEntitySet(modelSet);

		if (periodNumber == 0) {
			SettingsProvider.getPeriodSetting().setValueFunctionModelSetId(modelSetId);
		} else {
			SettingsProvider.getPeriodSettingFollower(periodNumber).setValueFunctionModelSetId(modelSetId);
		}

		return modelSet;
	}

	public static Routing organizeInitialRoutingResult(RoutingAlgorithm algo, int periodNumber) {
		if(!SettingsProvider.saveResults) return null;
		Routing routing = algo.getResult();
		NameProvider.setNameRouting(routing);
		Integer routingId = DataServiceProvider.getRoutingDataServiceImplInstance().persistCompleteRouting(routing);
		RoutingAssignment rAss = new RoutingAssignment();
		rAss.setRoutingId(routingId);
		rAss.setRouting(routing);
		rAss.setPeriod(periodNumber);
		rAss.setT(-1);// Because initial routing

		if (periodNumber == 0) {
			SettingsProvider.getPeriodSetting().addRoutingAssignment(rAss);
		} else {
			SettingsProvider.getPeriodSettingFollower(periodNumber).addRoutingAssignment(rAss);
		}

		return routing;
	}

	public static Routing organizeOrderSetAndRoutingResult(RoutingAlgorithm algo, int periodNumber) {
		if(!SettingsProvider.saveResults) return null;
		Routing routing = algo.getResult();
		return ResultHandler.organizeOrderSetAndRoutingResult(routing, periodNumber);
	}

	public static Routing organizeFinalRoutingResult(RoutingAlgorithm algo, int periodNumber) {
		if(!SettingsProvider.saveResults) return null;
		Routing routing = algo.getResult();
		return ResultHandler.organizeFinalRoutingResult(routing, periodNumber);
	}

	public static Routing organizeOrderSetAndRoutingResult(Routing routing, int periodNumber) {
		if(!SettingsProvider.saveResults) return null;
		OrderSet orderSet = routing.getOrderSet();
		ArrayList<Order> orders = orderSet.getElements();
		NameProvider.setNameOrderSet(orderSet);

		int orderSetId = DataServiceProvider.getOrderDataServiceImplInstance().persistEntitySet(orderSet);

		int lastOrderId = DataServiceProvider.getOrderDataServiceImplInstance().getHighestOrderId();

		final ArrayList<Order> ordersToSave = new ArrayList<Order>();

		for (Route route : routing.getRoutes()) {

			ArrayList<RouteElement> elements = route.getRouteElements();
			for (RouteElement element : elements) {
				Order order = element.getOrder();
				order.setSetId(orderSetId);
				order.setId(++lastOrderId);
				element.setOrderId(order.getId());
				ordersToSave.add(order);
			}
		}

		// Save orders that are not accepted (and thus not in the routes)
		for (Order order : orders) {
			if (!order.getAccepted()) {
				order.setSetId(orderSetId);
				order.setId(++lastOrderId);
				ordersToSave.add(order);
			}
		}

		//Save orders
		DataServiceProvider.getOrderDataServiceImplInstance().persistOrders(orderSetId, ordersToSave, true);


		routing.setOrderSetId(orderSetId);
		if (periodNumber == 0) {
			SettingsProvider.getPeriodSetting().setOrderSetId(orderSetId);
		} else {
			SettingsProvider.getPeriodSettingFollower(periodNumber).setOrderSetId(orderSetId);
		}
		return ResultHandler.organizeFinalRoutingResult(routing, periodNumber);
	}

	public static Routing organizeFinalRoutingResult(Routing routing, int periodNumber) {
		if(!SettingsProvider.saveResults) return null;
		NameProvider.setNameRouting(routing);

		Integer routingId = DataServiceProvider.getRoutingDataServiceImplInstance().persistCompleteRouting(routing);

		RoutingAssignment rAss = new RoutingAssignment();
		rAss.setRoutingId(routingId);
		rAss.setRouting(routing);
		rAss.setPeriod(periodNumber);
		rAss.setT(periodNumber);
		if (periodNumber == 0) {
			SettingsProvider.getPeriodSetting().addRoutingAssignment(rAss);
		} else {
			SettingsProvider.getPeriodSettingFollower(periodNumber).addRoutingAssignment(rAss);
		}

		return routing;
	}
}
