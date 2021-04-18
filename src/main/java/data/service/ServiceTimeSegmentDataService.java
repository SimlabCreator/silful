package data.service;

import java.util.ArrayList;

import data.entity.ServiceTimeSegment;
import data.entity.ServiceTimeSegmentWeight;

public abstract class ServiceTimeSegmentDataService extends WeightingDataService{
	

	@Override
	public abstract ServiceTimeSegment getElementById(int entityId);
	
	@Override
	public abstract ArrayList<ServiceTimeSegment> getAllElementsBySetId(int setId);
	
	@Override
	public abstract ArrayList<ServiceTimeSegmentWeight> getAllWeightsByWeightingId(int weightingId);
	
	@Override
	public abstract ServiceTimeSegmentWeight getWeightById(int weightId);

}
