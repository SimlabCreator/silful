package data.service;

import data.entity.Entity;

public abstract class ArrivalProcessDataService extends EntityDataService{
	
	/**
	 * Save a specific entity
	 * @param entity Respective entity
	 * @return id
	 */
	public abstract Integer persist(Entity entity);
	

}
