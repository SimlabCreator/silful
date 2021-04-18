package data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import data.entity.SetEntity;
import data.entity.TimeWindow;
import data.entity.TimeWindowSet;
import data.service.TimeWindowDataService;
import data.service.TimeWindowDataServiceImpl;
import data.utility.DataServiceProvider;
import data.utility.JDBCTemplateProvider;

public class SetEntityTestTimeWindow {
	 
	private static TimeWindowDataService service;
	 
	@BeforeClass
	public static void start() {
		service = DataServiceProvider.getTimeWindowDataServiceImplInstance();

	}
 
	@AfterClass
	public static void end() {

	}
	
	@Test
	public void persistAndGetById() {
		
		TimeWindowSet timeWindowSet = new TimeWindowSet();
		timeWindowSet.setName("test");
		
		
		TimeWindow timeWindow = new TimeWindow();
		timeWindow.setStartTime(9.0);
		
		TimeWindow timeWindow2 = new TimeWindow();
		timeWindow.setStartTime( 9.0);
		
		ArrayList<TimeWindow> entities = new ArrayList<TimeWindow>();
		entities.add(timeWindow);
		entities.add(timeWindow2);
		
		timeWindowSet.setElements(entities);
		
		Integer id = service.persistCompleteEntitySet(timeWindowSet);
		
		ArrayList<SetEntity> persistedEntities = service.getAllSets();
		
		assertEquals(persistedEntities.get(0).getId()==0, false); 
		
		TimeWindowDataService service2= new TimeWindowDataServiceImpl();
		service2.setJdbcTemplate(JDBCTemplateProvider.getInstance());
		TimeWindowSet persistedSet = (TimeWindowSet) service2.getSetById(id);
		
		assertEquals(persistedSet.getName(), timeWindowSet.getName()); 
		assertEquals(((TimeWindow) persistedSet.getElements().get(0))!=null, true);
		
	}
 
	

}