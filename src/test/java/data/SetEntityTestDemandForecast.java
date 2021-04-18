package data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import data.entity.SetEntity;
import data.entity.ValueBucketForecast;
import data.entity.ValueBucketForecastSet;
import data.service.ValueBucketForecastDataService;
import data.service.ValueBucketForecastDataServiceImpl;
import data.utility.DataServiceProvider;
import data.utility.JDBCTemplateProvider;

public class SetEntityTestDemandForecast {
	 
	private static ValueBucketForecastDataService service;
	 
	@BeforeClass
	public static void start() {
		service = DataServiceProvider.getValueBucketForecastDataServiceImplInstance();

	}
 
	@AfterClass
	public static void end() {

	}
	
	@Test
	public void persistAndGetById() {
		
		ValueBucketForecastSet demandForecastSet = new ValueBucketForecastSet();
		demandForecastSet.setName("test");
		
		ValueBucketForecast demandForecast = new ValueBucketForecast();
		demandForecast.setDemandNumber(10);
		
		
		ValueBucketForecast demandForecast2 = new ValueBucketForecast();
		demandForecast2.setDemandNumber(8);
		
		
		ArrayList<ValueBucketForecast> entities = new ArrayList<ValueBucketForecast>();
		entities.add(demandForecast);
		entities.add(demandForecast2);
		
		demandForecastSet.setElements(entities);
	
		Integer id = service.persistCompleteEntitySet(demandForecastSet);
		
		ArrayList<SetEntity> persistedEntities = service.getAllSets();
		
		assertEquals(persistedEntities.get(0).getId()==0, false); 
		
		ValueBucketForecastDataService service2= new ValueBucketForecastDataServiceImpl();
		service2.setJdbcTemplate(JDBCTemplateProvider.getInstance());
		
		ValueBucketForecastSet persistedSet = (ValueBucketForecastSet) service.getSetById(id);
		
		assertEquals(persistedSet.getName(), demandForecastSet.getName()); 
		assertEquals(((ValueBucketForecast) persistedSet.getElements().get(0))!=null, true);
		
	}
 
	

}