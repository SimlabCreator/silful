package data.service;

import java.util.ArrayList;

import data.entity.ValueBucket;

public abstract class ValueBucketDataService extends SetDataService{
	
	
	@Override
	public abstract ArrayList<ValueBucket> getAllElementsBySetId(int setId);
	
	@Override
	public abstract ValueBucket getElementById(int entityId);
}
