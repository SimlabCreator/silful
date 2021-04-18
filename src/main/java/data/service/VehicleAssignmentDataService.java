package data.service;

import java.util.ArrayList;

import data.entity.SetEntity;
import data.entity.VehicleAreaAssignment;

public abstract class VehicleAssignmentDataService extends SetDataService{
	
	@Override
	public abstract ArrayList<VehicleAreaAssignment> getAllElementsBySetId(int setId);
	
	@Override
	public abstract VehicleAreaAssignment getElementById(int entityId);

	/**
	 * Returns all sets that fit to the respective DeliveryAreaSet
	 * @param deliveryAreaSetId respective DeliveryAreaSet
	 * @return
	 */
	public abstract ArrayList<SetEntity> getAllSetsByDeliveryAreaSetId(Integer deliveryAreaSetId);
	

}
