package data.service;

import java.util.ArrayList;

import data.entity.Depot;

public abstract class DepotDataService extends MetaDataService{
	
	/**
	 * Provides all possible depots for a given region
	 * @param regionId Id of the respective region
	 * @return
	 */
	public abstract ArrayList<Depot> getAllDepotsByRegionId(Integer regionId);

}
