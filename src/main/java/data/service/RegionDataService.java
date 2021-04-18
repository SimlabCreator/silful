package data.service;

import java.util.ArrayList;

import data.entity.Entity;
import data.entity.Node;
import data.entity.NodeDistance;

public abstract class RegionDataService extends MetaDataService{
	

	/**
	 * Returns all nodes that belong to a region
	 * Should only be used by region.getNodes()
	 * @param regionId Respective region
	 * @return List of nodes
	 */
	public abstract ArrayList<Node> getNodesByRegionId(Integer regionId);
	
	/**
	 * Returns a specific node, has to belong to the region of the run
	 * Otherwise, null is returned
	 * @param nodeId Respective id
	 * @param regionId Region of the node
	 * @return Node
	 */
	public abstract Node getNodeById(Long nodeId, Integer regionId);
	
	/**
	 * Returns a specific node
	 * @param nodeId Respective id
	 * @param regionId Region of the node
	 * @return Node
	 */
	public abstract Node getNodeById(Long nodeId);
	
	/**
	 * Provides all distances to all other nodes for a respecitve node
	 * @param nodeId Long
	 * @return
	 */
	public abstract ArrayList<NodeDistance> getNodeDistancesByNodeId(Long nodeId);
	
	
	/**
	 * Get all entities within the respective table
	 * @return list of entities
	 */
	public abstract ArrayList<Entity> getAll();
	
	/**
	 * Provides all distances between all nodes of a region
	 * @param regionId
	 * @return
	 */
	public abstract ArrayList<NodeDistance> getNodeDistancesByRegionId(Integer regionId);
	

}
