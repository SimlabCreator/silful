package data.service;

import java.util.ArrayList;

import data.entity.Entity;

/**
 * Interface for all data services for data like customers and orders. Ensures common requests.
 * 
 * @author M. Lang
 *
 */
public abstract class TransactionDataService extends DataService{
	
	/**
	 * Get an entity by its id from the database
	 * @param id database id
	 * @return entity
	 */
	public abstract Entity getById(Integer id);
	
	/**
	 * Save a single entity in the database
	 * @param entity to save
	 * @return id of the entity
	 */
	public abstract Integer persist(Entity entity);

	/**
	 * Persist all entities of a list in the database
	 * @param entities list of entities to save
	 */
	public abstract void persistAll(ArrayList<Entity> entities);

}
