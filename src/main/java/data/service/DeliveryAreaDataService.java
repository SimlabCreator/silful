package data.service;

import java.util.ArrayList;

import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.SetEntity;

public abstract class DeliveryAreaDataService extends SetDataService{
	
	/**
	 * Provides all delivery sets that refer to the region
	 * @param regionId Respective region
	 * @return list of delivery area sets
	 */
	public abstract ArrayList<SetEntity> getAllSetsByRegionId(int regionId);
	
	@Override
	public abstract ArrayList<DeliveryArea> getAllElementsBySetId(int setId);

	@Override 
	public abstract DeliveryArea getElementById(int entityId);
	
	
	public abstract DeliveryArea getDeliveryAreaBySubsetId(int subsetId);
	
	public abstract void updateDeliveryAreaSetToPredefined(DeliveryAreaSet set);
}
