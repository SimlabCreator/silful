package data.service;

import java.util.ArrayList;

import data.entity.Entity;

/**
 * Interface for all data services for sets like delivery area set and customer set. Ensures common requests.
 * 
 * @author M. Lang
 *
 */
public abstract class EntityDataService extends DataService{

	protected ArrayList<Entity> entities;
	
	/**
	 * Get all available entities 
	 * @return list of entities
	 */
	public abstract  ArrayList<Entity> getAll();
	
	/**
	 * Get a specific entity
	 * @return the entity
	 */
	public abstract  Entity getById(int id);
	
	
}
