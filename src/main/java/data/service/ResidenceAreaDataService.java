package data.service;

import java.util.ArrayList;

import data.entity.ResidenceArea;
import data.entity.ResidenceAreaWeight;

public abstract class ResidenceAreaDataService extends WeightingDataService{
	
	@Override
	public abstract ArrayList<ResidenceArea> getAllElementsBySetId(int setId);
	
	@Override
	public abstract ResidenceArea getElementById(int entityId);
	
	@Override
	public abstract ArrayList<ResidenceAreaWeight> getAllWeightsByWeightingId(int weightingId);
	
	@Override
	public abstract ResidenceAreaWeight getWeightById(int weightId) ;
}
