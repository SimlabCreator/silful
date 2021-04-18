package data.service;

import java.util.ArrayList;

import data.entity.Entity;

/**
 * Interface for all data services for meta data like processes and kpi-types. Ensures common requests.
 * 
 * @author M. Lang
 *
 */
public abstract class MetaDataService extends DataService{

	/**
	 * Get all entities within the respective table
	 * @return list of entities
	 */
	public abstract ArrayList<Entity> getAll();
	
	/**
	 * Get entity by id (primary Id)
	 * @param id Id of the entity
	 * @return Requested entity
	 */
	public abstract Entity getById(Integer id);
	
}
