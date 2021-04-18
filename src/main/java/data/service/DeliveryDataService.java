package data.service;

import java.util.ArrayList;

import data.entity.Delivery;

public abstract class DeliveryDataService extends SetDataService{
	
	@Override
	public abstract ArrayList<Delivery> getAllElementsBySetId(int setId);
	
	@Override
	public abstract Delivery getElementById(int entityId);

}
