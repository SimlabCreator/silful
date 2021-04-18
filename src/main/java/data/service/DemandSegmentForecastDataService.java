package data.service;

import java.util.ArrayList;

import data.entity.DemandSegmentForecast;
import data.entity.SetEntity;

public abstract class DemandSegmentForecastDataService extends SetDataService{
	
	/**
	 * Provides all demand forecast sets that fit to the respective delivery area and demand segment set
	 * @param deliveryAreaSetId Respective delivery area set
	 * @param demandSegmentSetId Respective demand segment set
	 * @return
	 */
	public abstract ArrayList<SetEntity> getAllSetsByDeliveryAreaSetAndDemandSegmentSetId(Integer deliveryAreaSetId, Integer demandSegmentSetId);
	
	@Override
	public abstract ArrayList<DemandSegmentForecast> getAllElementsBySetId(int setId);
	
	@Override
	public abstract DemandSegmentForecast getElementById(int entityId);
}
