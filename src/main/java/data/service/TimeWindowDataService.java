package data.service;

import java.util.ArrayList;

import data.entity.TimeWindow;

public abstract class TimeWindowDataService extends SetDataService{
	
	@Override
	public abstract ArrayList<TimeWindow> getAllElementsBySetId(int setId);
	
	@Override
	public abstract TimeWindow getElementById(int entityId);
}
