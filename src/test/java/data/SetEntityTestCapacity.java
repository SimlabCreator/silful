package data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import data.entity.Capacity;
import data.entity.CapacitySet;
import data.entity.SetEntity;
import data.service.CapacityDataService;
import data.service.CapacityDataServiceImpl;
import data.utility.DataServiceProvider;
import data.utility.JDBCTemplateProvider;

public class SetEntityTestCapacity {
	 
	private static CapacityDataService service;
	 
	@BeforeClass
	public static void start() {
		service = DataServiceProvider.getCapacityDataServiceImplInstance();

	}
 
	@AfterClass
	public static void end() {

	}
	
	@Test
	public void persistAndGetById() {
		
		CapacitySet capacitySet = new CapacitySet();
		capacitySet.setName("test");
		
		Capacity capacity = new Capacity();
		capacity.setCapacityNumber(3);
		capacity.setTimeWindowId(1);
		capacity.setDeliveryAreaId(1);
		
		Capacity capacity2 = new Capacity();
		capacity2.setCapacityNumber(1);
		capacity2.setTimeWindowId(1);
		capacity2.setDeliveryAreaId(1);
		
		
		ArrayList<Capacity> entities = new ArrayList<Capacity>();
		entities.add(capacity);
		entities.add(capacity2);
		
		capacitySet.setElements(entities);
	
		Integer id = service.persistCompleteEntitySet(capacitySet);
		
		ArrayList<SetEntity> persistedEntities = service.getAllSets();
		
		assertEquals(persistedEntities.get(0).getId()==0, false); 
		
		CapacityDataService service2= new CapacityDataServiceImpl();
		service2.setJdbcTemplate(JDBCTemplateProvider.getInstance());
		
		CapacitySet persistedSet = (CapacitySet) service.getSetById(id);
		
		assertEquals(persistedSet.getName(), capacitySet.getName()); 
		assertEquals(((Capacity) persistedSet.getElements().get(0))!=null, true);
		
	}
 
	

}