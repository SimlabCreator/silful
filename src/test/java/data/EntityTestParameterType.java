package data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import data.entity.Entity;
import data.entity.ParameterType;
import data.service.ParameterTypeDataService;
import data.utility.DataServiceProvider;

public class EntityTestParameterType {
	 
	private static ParameterTypeDataService service;
	 
	@BeforeClass
	public static void start() {
		service = DataServiceProvider.getParameterTypeDataServiceImplInstance();
	}
 
	@AfterClass
	public static void end() {
	}
	
	@Test
	public void getById() {
		
		ParameterType persistedEntity = (ParameterType) service.getById(1);
		
		assertEquals(persistedEntity.getName()!=null, true); 
	}
 
	@Test
	public void getAll() {

		ArrayList<Entity> entities = service.getAll();
		
		assertEquals(entities!=null, true); 
		assertEquals(((ParameterType) entities.get(0)).getName()!=null, true); 

	}
	

}