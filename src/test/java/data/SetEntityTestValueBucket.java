package data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import data.entity.SetEntity;
import data.entity.ValueBucket;
import data.entity.ValueBucketSet;
import data.service.ValueBucketDataService;
import data.service.ValueBucketDataServiceImpl;
import data.utility.DataServiceProvider;
import data.utility.JDBCTemplateProvider;

public class SetEntityTestValueBucket {
	 
	private static ValueBucketDataService service;
	 
	@BeforeClass
	public static void start() {
		service = DataServiceProvider.getValueBucketDataServiceImplInstance();

	}
 
	@AfterClass
	public static void end() {

	}
	
	@Test
	public void persistAndGetById() {
		
		ValueBucketSet valueBucketSet = new ValueBucketSet();
		valueBucketSet.setName("test");
		
		
		ValueBucket valueBucket = new ValueBucket();
		valueBucket.setUpperBound( 50.0);
		valueBucket.setLowerBound( 0.0);
		
		ValueBucket valueBucket2 = new ValueBucket();
		valueBucket2.setUpperBound( 100.0);
		valueBucket2.setLowerBound(50.0);
		
		ArrayList<ValueBucket> entities = new ArrayList<ValueBucket>();
		entities.add(valueBucket);
		entities.add(valueBucket2);
		
		valueBucketSet.setElements(entities);
		
		Integer id = service.persistCompleteEntitySet(valueBucketSet);
		
		ArrayList<SetEntity> persistedEntities = service.getAllSets();
		
		assertEquals(persistedEntities.get(0).getId()==0, false); 
		
		ValueBucketDataService service2= new ValueBucketDataServiceImpl();
		service2.setJdbcTemplate(JDBCTemplateProvider.getInstance());
		ValueBucketSet persistedSet = (ValueBucketSet) service2.getSetById(id);
		
		assertEquals(persistedSet.getName(), valueBucketSet.getName()); 
		assertEquals(((ValueBucket) persistedSet.getElements().get(0))!=null, true);
		
	}
 
	

}