package data.service;

import java.util.ArrayList;

import data.entity.SetEntity;
import data.entity.TravelTime;

public abstract class TravelTimeDataService extends SetDataService{
	
	/**
	 * Provides all travel time sets that fit to the respective selector
	 * @param regionId  Respective id of the delivery region
	 * @return
	 */
	public abstract ArrayList<SetEntity> getTravelTimeSetsByRegionId(Integer regionId);
	
	
	@Override
	public abstract TravelTime getElementById(int entityId) ;
	
	@Override
	public abstract ArrayList<TravelTime> getAllElementsBySetId(int setId);

}
