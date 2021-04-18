package data.service;

import java.util.ArrayList;

import data.entity.Entity;
import data.entity.SetEntity;

/**
 * Interface for all data services for sets like demand segments that are not supposed to be saved by the java-application. Ensures common requests.
 * 
 * @author M. Lang
 *
 */
public abstract class SetReadOnlyDataService extends DataService{

	protected ArrayList<SetEntity> entitySets;
	protected SetEntity currentSet;
	
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
	 * Get all entities belonging to the respective set
	 * @param setId id of the set for which the entities are requested
	 * @return list of entities
	 */
	public abstract ArrayList<? extends Entity> getAllElementsBySetId(int setId);
	
	/**
	 * Get a specific element 
	 * @param entityId Id of the requested element
	 * @return Element
	 */
	public abstract Entity getElementById(int entityId);

	
	public SetEntity getCurrentSetEntity() {
		return this.currentSet;
	}

	public void setCurrentSetId(SetEntity currentSet) {
		this.currentSet = currentSet;
	}
	
}
