package data.service;

import java.util.ArrayList;

import data.entity.SetEntity;
import data.entity.ValueBucketForecast;

public abstract class ValueBucketForecastDataService extends SetDataService{
	
	/**
	 * Provides all demand forecast sets that fit to the respective delivery area and time window set
	 * @param deliveryAreaSetId Respective delivery area set
	 * @param timeWindowSetId Respective time window set
	 * @return
	 */
	public abstract ArrayList<SetEntity> getAllSetsByDeliveryAreaSetAndTimeWindowSetId(Integer deliveryAreaSetId, Integer timeWindowSetId);
	
	/**
	 * Provides all demand forecast sets that fit to the respective delivery area and time window set and value bucket set
	 * @param deliveryAreaSetId Respective delivery area set
	 * @param timeWindowSetId Respective time window set
	 * @param valueBucketSetId Respective value bucket set
	 * @return
	 */
	public abstract ArrayList<SetEntity> getAllSetsByDeliveryAreaSetAndTimeWindowSetAndValueBucketSetId(Integer deliveryAreaSetId, Integer timeWindowSetId, Integer valueBucketSetId);

	
	@Override
	public abstract ArrayList<ValueBucketForecast> getAllElementsBySetId(int setId);
	
	@Override
	public abstract ValueBucketForecast getElementById(int entityId);
}
