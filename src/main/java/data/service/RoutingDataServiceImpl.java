package data.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;

import data.entity.Entity;
import data.entity.Experiment;
import data.entity.Route;
import data.entity.RouteElement;
import data.entity.Routing;
import data.mapper.ExperimentMapper;
import data.mapper.RouteElementMapper;
import data.mapper.RouteMapper;
import data.mapper.RoutingMapper;
import logic.utility.SettingsProvider;

public class RoutingDataServiceImpl extends RoutingDataService {

	private ArrayList<Entity> routings;
	private Routing currentRouting;

	@Override
	public ArrayList<Entity> getAllRoutings() {

		if (routings == null) {

			routings = DataLoadService.loadAllFromClass("routing", new RoutingMapper(), jdbcTemplate);

		}

		return routings;
	}

	@Override
	public Routing getRoutingById(int id) {

		Routing routing = new Routing();

		if (routings == null) {
			routing = (Routing) DataLoadService.loadById("routing", "rou_id", id, new RoutingMapper(), jdbcTemplate);
		} else {

			for (int i = 0; i < routings.size(); i++) {
				if (( routings.get(i)).getId() == id) {
					routing = (Routing) routings.get(i);
					return routing;
				}

			}

		}
		return routing;
	}

	@Override
	public ArrayList<Route> getAllRoutesByRoutingId(int routingId) {
		ArrayList<Route> entities = (ArrayList<Route>) DataLoadService.loadMultipleRowsBySelectionId("route", "route_routing", routingId,
				new RouteMapper(), jdbcTemplate);
		this.currentRouting = (Routing) this.getRoutingById(routingId);
		this.currentRouting.setRoutes(entities);
		return entities;
	}

	@Override
	public Route getRouteById(int entityId) {

		if (this.currentRouting != null) {
			for (int i = 0; i < this.currentRouting.getRoutes().size(); i++) {
				if (( this.currentRouting.getRoutes().get(i)).getId() == entityId) {
					return this.currentRouting.getRoutes().get(i);
				}
			}
		}

		Entity route = new Route();

		route = DataLoadService.loadById("route", "route_id", entityId, new RouteMapper(), jdbcTemplate);

		return (Route) route;
	}

	@Override
	public ArrayList<RouteElement> getAllRouteElementsByRouteId(int routeId) {

		ArrayList<RouteElement> entities = (ArrayList<RouteElement>) DataLoadService.loadMultipleRowsBySelectionId("route_element", "re_route", routeId,
				new RouteElementMapper(), jdbcTemplate);
		return entities;
	};

	@Override
	public Integer persistCompleteRouting(Routing routing) {

		// Save routing
		final int routingId = this.persistRouting(routing);

		// Save routes
		final Routing routingToSave = routing;
		ArrayList<RouteElement> routeElements = new ArrayList<RouteElement>();

		for (int i = 0; i < routingToSave.getRoutes().size(); i++) {
			Route currentRoute = routingToSave.getRoutes().get(i);
			currentRoute.setRoutingId(routingId);
			int routeId=this.persistRoute(currentRoute);
			for (int j = 0; j < currentRoute.getRouteElements().size(); j++) {
				RouteElement element = currentRoute.getRouteElements().get(j);
					element.setRouteId(routeId);
				routeElements.add(element);
			}

		}

		// Save routing elements

		final ArrayList<RouteElement> routeElementsToSave = routeElements;

		DataLoadService.persistAll("route_element", 10,
				"re_route, re_position, re_tw, re_area, re_order, re_travel_time, re_waiting_time, re_service_begin, re_service_time, re_slack",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return routeElementsToSave.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						RouteElement route = routeElementsToSave.get(i);
						ps.setInt(1, route.getRouteId());
						ps.setObject(2, route.getPosition(), Types.INTEGER);
						ps.setObject(3, route.getTimeWindowId(), Types.INTEGER);
						ps.setObject(4, route.getDeliveryAreaId(), Types.INTEGER);
						ps.setObject(5, route.getOrderId(), Types.INTEGER);
						ps.setObject(6, route.getTravelTime(), Types.DOUBLE);					
						ps.setObject(7, route.getWaitingTime(), Types.DOUBLE);
						ps.setObject(8, route.getServiceBegin(), Types.DOUBLE);
						ps.setObject(9, route.getServiceTime(), Types.DOUBLE);
						ps.setObject(10, route.getSlack(), Types.DOUBLE);
					}
				}, jdbcTemplate);

		// RouteElements in the routes are not up-to-date anymore because the
		// ids are not updated.
		for (int i = 0; i < routingToSave.getRoutes().size(); i++) {
			(routingToSave.getRoutes().get(i)).setRouteElements(null);

		}

		this.currentRouting = routingToSave;
		if (this.routings != null)
			this.routings.add(currentRouting);
		return routingId;
	}

	protected Integer persistRouting(Routing routing) {

		final Routing routingToSave = routing;

		final String SQL = DataLoadService.buildInsertSQL("routing", 11,
				"rou_possibly_final, rou_time_window_set, rou_name, rou_order_set, rou_depot, rou_information, rou_vehicle_area_assignment_set, rou_possibly_target, rou_additional_costs, rou_area_weighting, rou_area_ds_weighting");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "rou_id" });
				ps.setObject(1, routingToSave.isPossiblyFinalRouting(), Types.BOOLEAN);
				ps.setObject(2, routingToSave.getTimeWindowSetId(),Types.INTEGER);
				ps.setObject(3,  routingToSave.getName(), Types.VARCHAR);
				ps.setObject(4, routingToSave.getOrderSetId(), Types.INTEGER);
				ps.setObject(5, routingToSave.getDepotId(), Types.INTEGER);
				ps.setObject(6, routingToSave.getAdditionalInformation(), Types.VARCHAR);
				ps.setObject(7, routingToSave.getVehicleAreaAssignmentSetId(), Types.INTEGER);
				ps.setObject(8, routingToSave.isPossiblyTarget(), Types.BOOLEAN);
				ps.setObject(9, routingToSave.getAdditionalCosts(), Types.FLOAT);
				ps.setObject(10, routingToSave.getAreaWeighting(), Types.LONGVARCHAR);
				ps.setObject(11, routingToSave.getAreaDsWeighting(), Types.LONGVARCHAR);
				return ps;
			}
		}, jdbcTemplate);

		routingToSave.setId(id);

		return id;

	}

	protected Integer persistRoute(Route route) {

		final Route routeToSave = route;

		final String SQL = DataLoadService.buildInsertSQL("route", 3, "route_routing,route_vehicle,route_vehicle_area_assignment");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "route_id" });
				ps.setInt(1, routeToSave.getRoutingId());
				ps.setObject(2, routeToSave.getVehicleTypeId(), Types.INTEGER);
				ps.setObject(3, routeToSave.getVehicleAreaAssignmentId(), Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);

		routeToSave.setId(id);

		return id;

	}

	@Override
	public RouteElement getRouteElementById(int routeElementId) {

		RouteElement routeElement = new RouteElement();

		routeElement = (RouteElement) DataLoadService.loadById("route_element", "re_id", routeElementId,
				new RouteElementMapper(), jdbcTemplate);

		return routeElement;
	}

	@Override
	public ArrayList<Routing> getAllInitialRoutingsByTimeWindowSetId(int timewindowSetId){
		
		ArrayList<Routing> initialRoutings = (ArrayList<Routing>) DataLoadService.loadMultipleRowsBySelectionId("routing","rou_time_window_set",timewindowSetId, new RoutingMapper(), jdbcTemplate);
		return initialRoutings;
	}

	@Override
	public ArrayList<Routing> getAllFinalRoutingsByOrderSetAndDepotId(int orderSetId, int depotId){
		ArrayList<Routing> finalRoutings = (ArrayList<Routing>) DataLoadService.loadByMultipleSelectionIds("routing", new String[]{"rou_possibly_final", "rou_order_set", "rou_depot"}, new Integer[]{1, orderSetId, depotId}, new RoutingMapper(), jdbcTemplate);
		return finalRoutings;
	}
	
	@Override
	public ArrayList<Experiment> getAllExperimentsWithFinalRoutingOutputByDemandSegmentSetId(int demandSegmentSetId) {
		 String sql="SELECT distinct "+SettingsProvider.database+".experiment.* FROM "+SettingsProvider.database+".experiment "
				 +"LEFT JOIN "+SettingsProvider.database+".run ON (experiment.exp_id = run.run_experiment) "
				 +"LEFT JOIN "+SettingsProvider.database+".r_run_v_routing ON (run.run_id=r_run_v_routing.run_rou_run) "
				 +"LEFT JOIN "+SettingsProvider.database+".routing ON (routing.rou_id=r_run_v_routing.run_rou_rou)"
				 +"LEFT JOIN "+SettingsProvider.database+".order_set ON (routing.rou_order_set=order_set.os_id)"
				 +"LEFT JOIN "+SettingsProvider.database+".order_request_set ON (order_set.os_order_request_set=order_request_set.ors_id)"
				 +"LEFT JOIN "+SettingsProvider.database+".customer_set ON (order_request_set.ors_customer_set=customer_set.cs_id)"
				 +"WHERE "+SettingsProvider.database+".r_run_v_routing.run_rou_rou>0 AND "+SettingsProvider.database+".routing.rou_possibly_final=1 AND "+SettingsProvider.database+".customer_set.cs_original_demand_segment_set=?;";
		 
		ArrayList<Experiment> entities = (ArrayList<Experiment>) DataLoadService
				.loadComplexPreparedStatementMultipleEntities(sql, new Object[] {demandSegmentSetId},
						new ExperimentMapper(), jdbcTemplate);
		
		
		return entities;
	}

	@Override
	public ArrayList<Routing> getAllRoutingsByExperimentId(int expId) {
		ArrayList<Routing> routings=new ArrayList<Routing>();
		 String sql = "SELECT "+SettingsProvider.database+".routing.* from "+SettingsProvider.database+".routing "
				+"JOIN "+SettingsProvider.database+".r_run_v_routing ON (r_run_v_routing.run_rou_rou=routing.rou_id) "
				 +"LEFT JOIN "+SettingsProvider.database+".run ON (r_run_v_routing.run_rou_run=run.run_id) "
				 +"LEFT JOIN "+SettingsProvider.database+".experiment ON (experiment.exp_id=run.run_experiment) "
				 +"WHERE "+SettingsProvider.database+".experiment.exp_id=?";
		 
		 routings = (ArrayList<Routing>) DataLoadService
					.loadComplexPreparedStatementMultipleEntities(sql, new Object[] {expId},
							new RoutingMapper(), jdbcTemplate);

			return routings;
	}

	@Override
	public ArrayList<Experiment> getAllNonCopyExperimentsWithFinalRoutingOutputByDemandSegmentSetId(
			int demandSegmentSetId) {
		String sql="SELECT distinct "+SettingsProvider.database+".experiment.* FROM "+SettingsProvider.database+".experiment "
				 +"LEFT JOIN "+SettingsProvider.database+".run ON (experiment.exp_id = run.run_experiment) "
				 +"LEFT JOIN "+SettingsProvider.database+".r_run_v_routing ON (run.run_id=r_run_v_routing.run_rou_run) "
				 +"LEFT JOIN "+SettingsProvider.database+".routing ON (routing.rou_id=r_run_v_routing.run_rou_rou)"
				 +"LEFT JOIN "+SettingsProvider.database+".order_set ON (routing.rou_order_set=order_set.os_id)"
				 +"LEFT JOIN "+SettingsProvider.database+".order_request_set ON (order_set.os_order_request_set=order_request_set.ors_id)"
				 +"LEFT JOIN "+SettingsProvider.database+".customer_set ON (order_request_set.ors_customer_set=customer_set.cs_id)"
				 +"WHERE "+SettingsProvider.database+".experiment.exp_copy_exp IS NULL AND "+SettingsProvider.database+".r_run_v_routing.run_rou_rou>0 AND "+SettingsProvider.database+".routing.rou_possibly_final=1 AND "+SettingsProvider.database+".customer_set.cs_original_demand_segment_set=?;";
		 
		ArrayList<Experiment> entities = (ArrayList<Experiment>) DataLoadService
				.loadComplexPreparedStatementMultipleEntities(sql, new Object[] {demandSegmentSetId},
						new ExperimentMapper(), jdbcTemplate);
		
		
		return entities;
	}
}
