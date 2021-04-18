package data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import data.entity.DemandModelType;
import data.entity.Entity;
import data.service.DemandModelTypeDataService;
import data.utility.DataServiceProvider;

public class EntityTestDemandModelType {
	 
	private static DemandModelTypeDataService service;
	 
	@BeforeClass
	public static void start() {
		service = DataServiceProvider.getDemandModelTypeDataServiceImplInstance();
	}
 
	@AfterClass
	public static void end() {
	}
	
	@Test
	public void getById() {
		
		DemandModelType persistedEntity = (DemandModelType) service.getById(1);
		
		assertEquals(persistedEntity.getName()!=null, true); 
	}
 
	@Test
	public void getAll() {

		ArrayList<Entity> entities = service.getAll();
		
		assertEquals(entities!=null, true); 
		System.out.println(((DemandModelType) entities.get(0)).getName());
		assertEquals(((DemandModelType) entities.get(0)).getName().equals("MNL"), true); 

	}
	

}