package data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import data.entity.Entity;
import data.entity.Route;
import data.entity.RouteElement;
import data.entity.Routing;
import data.service.RoutingDataService;
import data.utility.DataServiceProvider;

public class SetEntityTestRouting {
	 
	private static RoutingDataService service;
	 
	@BeforeClass
	public static void start() {
		service = DataServiceProvider.getRoutingDataServiceImplInstance();

	}
 
	@AfterClass
	public static void end() {

	}
	
	@Test
	public void persistAndGetById() {
		
		Routing routing = new Routing();
		
		
		Route route = new Route();
		Route route2 = new Route();

		RouteElement routeE = new RouteElement();
		
		routeE.setPosition(1);
		routeE.setTimeWindowId(1);
		routeE.setDeliveryAreaId(1);
		
		RouteElement routeE2 = new RouteElement();
		
		routeE2.setPosition(1);
		routeE2.setTimeWindowId(1);
		routeE2.setDeliveryAreaId(2);
		
		ArrayList<RouteElement> entities = new ArrayList<RouteElement>();
		entities.add(routeE);
		entities.add(routeE2);
		
		route.setRouteElements(entities);
		route2.setRouteElements(entities);
		
		ArrayList<Route> routes = new ArrayList<Route>();
		routes.add(route);
		routes.add(route2);
		
		routing.setRoutes(routes);
		service.persistCompleteRouting(routing);
		
		ArrayList<Entity> persistedEntities = service.getAllRoutings();
		
		Routing persistedRouting = (Routing) persistedEntities.get(0);
		assertEquals(persistedRouting==null, false); 
		
		
		assertEquals(persistedRouting.getRoutes().get(0)!=null, true); 
		assertEquals(((Route)persistedRouting.getRoutes().get(0)).getRouteElements()!=null, true);
		
	}
 
	

}