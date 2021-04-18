package data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.Region;
import data.entity.SetEntity;
import data.service.DeliveryAreaDataServiceImpl;
import data.service.RegionDataServiceImpl;
import data.utility.JDBCTemplateProvider;

public class SetEntityTestDeliveryArea {
	 
	private static JdbcTemplate jdbcTemplateInstance;
	private static DeliveryAreaDataServiceImpl service;
	 
	@BeforeClass
	public static void start() {
		jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
		service = new DeliveryAreaDataServiceImpl();
		service.setJdbcTemplate(jdbcTemplateInstance);
	}
 
	@AfterClass
	public static void end() {
		jdbcTemplateInstance=null;
	}
	
	@Test
	public void persistAndGetById() {
		
		DeliveryAreaSet deliveryAreaSet = new DeliveryAreaSet();
		deliveryAreaSet.setName("test");
		RegionDataServiceImpl regionService = new RegionDataServiceImpl();
		regionService.setJdbcTemplate(jdbcTemplateInstance);
		Region region = (Region) regionService.getAll().get(0);
		deliveryAreaSet.setRegionId(region.getId());
		
		DeliveryArea area1 = new DeliveryArea();
		area1.setLat1((float) 49.00000);
		area1.setLon1((float) 11.00000);
		area1.setLat2((float) 49.00000);
		area1.setLon2((float) 11.00000);
		area1.setCenterLat((float) 49.00000);
		area1.setCenterLon((float) 11.00000);
		
		DeliveryArea area2 = new DeliveryArea();
		area2.setLat1((float) 49.00000);
		area2.setLon1((float) 11.00000);
		area2.setLat2((float) 49.00000);
		area2.setLon2((float) 11.00000);
		area2.setCenterLat((float) 49.00000);
		area2.setCenterLon((float) 11.00000);
		
		ArrayList<DeliveryArea> entities = new ArrayList<DeliveryArea>();
		entities.add(area2);
		entities.add(area1);
		
		deliveryAreaSet.setElements(entities);
	
		Integer id = service.persistCompleteEntitySet(deliveryAreaSet);
		
		//Ensure loading from database because not loaded as list before
		service = new DeliveryAreaDataServiceImpl();
		service.setJdbcTemplate(jdbcTemplateInstance);
		
		DeliveryAreaSet persistedSet = (DeliveryAreaSet) service.getSetById(id);
		
		assertEquals(persistedSet.getName(), deliveryAreaSet.getName()); 
		assertEquals(((DeliveryArea) persistedSet.getElements().get(0))!=null, true);
		
	}
 
	@Test
	public void getAll() {

		ArrayList<SetEntity> entities = service.getAllSets();
		
		assertEquals(entities.get(0).getId()==0, false); 
		

	}
	

}