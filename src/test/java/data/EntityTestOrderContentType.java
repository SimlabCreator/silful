package data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import data.entity.Entity;
import data.entity.OrderContentType;
import data.service.OrderContentTypeDataService;
import data.utility.DataServiceProvider;

public class EntityTestOrderContentType {
	 
	private static OrderContentTypeDataService service;
	 
	@BeforeClass
	public static void start() {
		service = DataServiceProvider.getOrderContentTypeDataServiceImplInstance();
	}
 
	@AfterClass
	public static void end() {
	}
	
	@Test
	public void getById() {
		
		OrderContentType persistedEntity = (OrderContentType) service.getById(1);
		
		assertEquals(persistedEntity.getName()!=null, true); 
	}
 
	@Test
	public void getAll() {

		ArrayList<Entity> entities = service.getAll();
		
		assertEquals(entities!=null, true); 
		assertEquals(((OrderContentType) entities.get(0)).getName()!=null, true); 

	}
	

}