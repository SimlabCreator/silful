package data.service;

import java.util.ArrayList;

import data.entity.ExpectedDeliveryTimeConsumption;
import data.entity.SetEntity;

public abstract class ExpectedDeliveryTimeConsumptionDataService extends SetDataService{
	
	/**
	 * Provides all delivery time consumption sets that fit to the respective delivery area and time window set
	 * @param deliveryAreaSetId Respective delivery area set
	 * @param timeWindowSetId Respective time window set
	 * @return
	 */
	public abstract ArrayList<SetEntity> getAllSetsByDeliveryAreaSetAndTimeWindowSetId(Integer deliveryAreaSetId, Integer timeWindowSetId);
	
	@Override
	public abstract ArrayList<ExpectedDeliveryTimeConsumption> getAllElementsBySetId(int setId);
	
	@Override
	public abstract ExpectedDeliveryTimeConsumption getElementById(int entityId);

}
