package data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import data.entity.Entity;
import data.entity.KpiType;
import data.service.KpiTypeDataService;
import data.utility.DataServiceProvider;

public class EntityTestKPIType {
	 
	private static KpiTypeDataService service;
	 
	@BeforeClass
	public static void start() {
		service = DataServiceProvider.getKpiTypeDataServiceImplInstance();
	}
 
	@AfterClass
	public static void end() {
	}
	
	@Test
	public void getById() {
		
		KpiType persistedEntity = (KpiType) service.getById(1);
		
		assertEquals(persistedEntity.getName()!=null, true); 
	}
 
	@Test
	public void getAll() {

		ArrayList<Entity> entities = service.getAll();
		
		assertEquals(entities!=null, true); 
		assertEquals(((KpiType) entities.get(0)).getName()!=null, true); 

	}
	

}