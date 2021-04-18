package data.service;

import java.util.ArrayList;

import data.entity.Customer;
import data.entity.SetEntity;

public abstract class CustomerDataService extends SetDataService{
	
	/**
	 * Provides all customer sets that originate from the respective demand segment set
	 * @param demandSegmentSetId Respective demand segment set
	 * @return List of customer sets
	 */
	public abstract ArrayList<SetEntity> getAllByOriginalDemandSegmentSetId(Integer demandSegmentSetId);
	
	
	@Override
	public abstract ArrayList<Customer> getAllElementsBySetId(int setId);
	
	@Override
	public abstract Customer getElementById(int entityId);

}
