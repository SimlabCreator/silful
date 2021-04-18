package logic.service.support;

import java.util.ArrayList;

import data.entity.RouteElement;
import data.entity.TimeWindow;
import data.entity.TimeWindowSet;
import logic.entity.AcceptedCustomersOnRouteAfter;
import logic.entity.AcceptedCustomersOnRouteBefore;

/**
 * Provides functionality relating to routing
 * 
 * @author C. Köhler
 *
 */
public class RoutingService {

	public static ArrayList<AcceptedCustomersOnRouteAfter> findFirstCustomerAfterPreferredTimeWindow(
			ArrayList<ArrayList<ArrayList<RouteElement>>> routes, int preferredTimeWindow) {

		ArrayList<AcceptedCustomersOnRouteAfter> after = new ArrayList<AcceptedCustomersOnRouteAfter>();

		for (int r = 0; r < routes.size(); r++) {
			int a = 1;

			// if (preferredTimeWindow == routes.get(r).size() - 1) {
			// a = 0;
			// } else {
			for (int i = preferredTimeWindow + 1; i < routes.get(r).size(); i++) {

				if (routes.get(r).get(i).isEmpty()) {
					a++;
				} else {
					break;
				}
				// }
			}
			AcceptedCustomersOnRouteAfter after1 = new AcceptedCustomersOnRouteAfter();
			after1.setAfter(a);
			after1.setRoute(r);
			after.add(after1);
		}
		return after;
	}

	public static ArrayList<AcceptedCustomersOnRouteBefore> findLastCustomerBeforePreferredTimeWindow(
			ArrayList<ArrayList<ArrayList<RouteElement>>> routes, int preferredTimeWindow) {

		ArrayList<AcceptedCustomersOnRouteBefore> before = new ArrayList<AcceptedCustomersOnRouteBefore>();

		for (int r = 0; r < routes.size(); r++) {
			int b = 1;

			// if (preferredTimeWindow == 0) {
			// b = 0;

			// } else {

			for (int i = preferredTimeWindow - 1; i > 0; i--) {
				if (routes.get(r).get(i).isEmpty()) {
					b++;
				} else {
					break;
				}
				// }
			}

			AcceptedCustomersOnRouteBefore before1 = new AcceptedCustomersOnRouteBefore();
			before1.setBefore(b);
			before1.setRoute(r);
			before.add(before1);
		}
		return before;
	}

	/**
	 * Calculates the overall length of the delivery period by considering start of first and end of last time window
	 * @param timeWindowSet Respective time window set
	 * @param timeMultiplier multiplier to get the right unit
	 * @return Length of route in respective unit
	 */
	public static void getPossibleDurationOfRouteByTimeWindowSet(TimeWindowSet timeWindowSet, double timeMultiplier){
		
		ArrayList<TimeWindow> timeWindowsE = timeWindowSet.getElements();
		Double routeStart=(timeWindowsE.get(0)).getStartTime();
		Double routeEnd=(timeWindowsE.get(0)).getEndTime();
		for(int t=1; t < timeWindowsE.size(); t++){
			TimeWindow tw = timeWindowsE.get(t);
			if(tw.getStartTime()<routeStart) routeStart=tw.getStartTime();
			if(tw.getEndTime()>routeEnd) routeEnd=tw.getEndTime();
		}
		
		timeWindowSet.setTempStartOfDeliveryPeriod(routeStart);
		timeWindowSet.setTempEndOfDeliveryPeriod(routeEnd);
		
		timeWindowSet.setTempLengthOfDeliveryPeriod((routeEnd-routeStart)*timeMultiplier);
	}
	
	
	/**
	 * Calculates the start time of the delivery period.
	 * @param timeWindowSet Respective time window set
	 * @return start time as double
	 */
	public static Double getDeliveryStartTimeByTimeWindowSet(TimeWindowSet timeWindowSet){
		
		ArrayList<TimeWindow> timeWindowsE = timeWindowSet.getElements();
		Double routeStart=(timeWindowsE.get(0)).getStartTime();
		for(int t=1; t < timeWindowsE.size(); t++){
			TimeWindow tw = timeWindowsE.get(t);
			if(tw.getStartTime()<routeStart) routeStart=tw.getStartTime();
		}
		
		return routeStart;
	}
}
