package data.service;

import java.util.ArrayList;

import data.entity.Entity;
import data.entity.SetEntity;

/**
 * Interface for all data services for sets like delivery area set and customer set. Ensures common requests.
 * 
 * @author M. Lang
 *
 */
public abstract class SetDataService extends DataService{

	protected ArrayList<SetEntity> entitySets;
	
	/**
	 * Get all available sets 
	 * @return list of sets
	 */
	public abstract  ArrayList<SetEntity> getAllSets();
	
	/**
	 * Get a specific set
	 * Changes the current set
	 * @return the set
	 */
	public abstract  SetEntity getSetById(int id);
	
	
	/**
	 * Save entity belonging to a set
	 * Needs to have an entity id
	 * @param entity entity to persist
	 * @return id (primary Id) of the entity
	 */
	public abstract Integer persistElement(Entity entity);
	
	/**
	 * Save entity set with all respective entities
	 * @param setEntity entity set with all respective entities
	 * @return id of the set
	 */
	public abstract Integer persistCompleteEntitySet(final SetEntity setEntity);
	
	/**
	 * Save entity set only
	 * @param setEntity
	 */
	protected abstract Integer persistEntitySet(SetEntity setEntity);
	
	
	/**
	 * Returns all elements that belong to the respective set
	 * @param setId Id of the set
	 * @return
	 */
	public abstract ArrayList<? extends Entity> getAllElementsBySetId(int setId);
	
	/**
	 * Returns the respective element
	 * @param entityId Id of the element
	 * @return
	 */
	public abstract Entity getElementById(int entityId);
	
}
