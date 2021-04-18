package data.service;

import java.util.ArrayList;

import data.entity.Entity;
import data.entity.Experiment;
import data.entity.Route;
import data.entity.RouteElement;
import data.entity.Routing;

public abstract class RoutingDataService extends DataService {

	/**
	 * Returns all routings
	 * 
	 * @return List of routings
	 */
	public abstract ArrayList<Entity> getAllRoutings();

	/**
	 * Returns a routing for the given id
	 * 
	 * @param id
	 *            Respective id
	 * @return Routing
	 */
	public abstract Routing getRoutingById(int id);

	/**
	 * Provides all routes that belong to a given routing
	 * 
	 * @param routingId
	 *            Respective routing id
	 * @return List of routes
	 */
	public abstract ArrayList<Route> getAllRoutesByRoutingId(int routingId);

	/**
	 * Provides a specific route
	 * 
	 * @param entityId
	 *            Id of the route
	 * @return Route
	 */
	public abstract Route getRouteById(int entityId);

	/**
	 * Provides all route elements of a specific route
	 * 
	 * @param routeId
	 *            Id of the respective route
	 * @return List of route elements
	 */
	public abstract ArrayList<RouteElement> getAllRouteElementsByRouteId(int routeId);

	/**
	 * Save a routing with all its routes and route elements
	 * 
	 * @param routing
	 */
	public abstract Integer persistCompleteRouting(Routing routing);

	/**
	 * Get a specific route element
	 * 
	 * @return
	 */
	public abstract RouteElement getRouteElementById(int routeElementId);
	
	/**
	 * Get all routings that were saved as initial routings (time window set associated)
	 * @return
	 */
	public abstract ArrayList<Routing> getAllInitialRoutingsByTimeWindowSetId(int timewindowSetId);
	
	/**
	 * Get all routings that are potentially final
	 * @return
	 */
	public abstract ArrayList<Routing> getAllFinalRoutingsByOrderSetAndDepotId(int orderSetId, int depotId);
	
	/**
	 * Get all experiments that have a final routing as output
	 * @param demandSegmentSetId Respective demand segment
	 * @return
	 */
	public abstract ArrayList<Experiment> getAllExperimentsWithFinalRoutingOutputByDemandSegmentSetId(int demandSegmentSetId);
	
	/**
	 * Get all experiments that have a final routing as output and are no copy
	 * @param demandSegmentSetId Respective demand segment
	 * @return
	 */
	public abstract ArrayList<Experiment> getAllNonCopyExperimentsWithFinalRoutingOutputByDemandSegmentSetId(int demandSegmentSetId);
	
	
	/**
	 * Provides all (potentially) final routings that were produced by the respective experiments
	 * @param expId Respective experiment id
	 * @return
	 */
	public abstract ArrayList<Routing> getAllRoutingsByExperimentId(int expId);
}
