package data.service;

import java.util.ArrayList;

import data.entity.Capacity;
import data.entity.SetEntity;

public abstract class CapacityDataService extends SetDataService{
	
	/**
	 * Provides all capacity sets that fit to the respective delivery area and time window set
	 * @param deliveryAreaSetId Respective delivery area set
	 * @param timeWindowSetId Respective time window set
	 * @return
	 */
	public abstract ArrayList<SetEntity> getAllSetsByDeliveryAreaSetAndTimeWindowSetId(Integer deliveryAreaSetId, Integer timeWindowSetId);
	
	@Override
	public abstract ArrayList<Capacity> getAllElementsBySetId(int setId);
	
	@Override
	public abstract Capacity getElementById(int entityId);

}
