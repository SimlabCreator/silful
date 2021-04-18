package data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import data.entity.Entity;
import data.entity.IncentiveType;
import data.service.IncentiveTypeDataServiceImpl;
import data.utility.JDBCTemplateProvider;

public class EntityTestIncentiveType {
	 
	private static JdbcTemplate jdbcTemplateInstance;
	private static IncentiveTypeDataServiceImpl service;
	 
	@BeforeClass
	public static void start() {
		jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
		service = new IncentiveTypeDataServiceImpl();
		service.setJdbcTemplate(jdbcTemplateInstance);
	}
 
	@AfterClass
	public static void end() {
		jdbcTemplateInstance=null;
	}
	
	@Test
	public void persistAndGetById() {
		
		IncentiveType incentive = new IncentiveType();
		incentive.setName("test");

	
		Integer id = service.persist(incentive);
		
		//Ensure loading from database because not loaded as list before
		service = new IncentiveTypeDataServiceImpl();
		service.setJdbcTemplate(jdbcTemplateInstance);
		
		IncentiveType persistedEntity = (IncentiveType) service.getById(id);
		
		assertEquals(persistedEntity.getName(), incentive.getName()); 
		
	}
 
	@Test
	public void persistAndGetAll() {
		IncentiveType incentive = new IncentiveType();
		incentive.setName("test");
		IncentiveType incentive2 = new IncentiveType();
		incentive2.setName("test2");
		ArrayList<Entity> list = new ArrayList<Entity>();
		list.add(incentive);
		list.add(incentive2);
		service.persistAll(list);
		ArrayList<Entity> entities = service.getAll();
		
		assertEquals(incentive.getName(), ((IncentiveType) entities.get(entities.size()-2)).getName()); 
		assertEquals(incentive2.getName(), ((IncentiveType) entities.get(entities.size()-1)).getName()); 
		

	}
	

}