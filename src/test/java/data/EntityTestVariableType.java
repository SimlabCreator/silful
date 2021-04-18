package data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import data.entity.Entity;
import data.entity.VariableType;
import data.service.VariableTypeDataService;
import data.utility.DataServiceProvider;

public class EntityTestVariableType {
	 
	private static VariableTypeDataService service;
	 
	@BeforeClass
	public static void start() {
		service = DataServiceProvider.getVariableTypeDataServiceImplInstance();
	}
 
	@AfterClass
	public static void end() {
	}
	
	@Test
	public void getById() {
		
		VariableType persistedEntity = (VariableType) service.getById(1);
		
		assertEquals(persistedEntity.getName()!=null, true); 
	}
 
	@Test
	public void getAll() {

		ArrayList<Entity> entities = service.getAll();
		
		assertEquals(entities!=null, true); 
		assertEquals(((VariableType) entities.get(0)).getName().equals("ord_basket_value"), true); 

	}
	

}