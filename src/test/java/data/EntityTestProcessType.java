package data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import data.entity.Entity;
import data.entity.ProcessType;
import data.service.ProcessTypeDataServiceImpl;
import data.utility.JDBCTemplateProvider;

public class EntityTestProcessType {
	 
	private static JdbcTemplate jdbcTemplateInstance;
	private static ProcessTypeDataServiceImpl service;
	 
	@BeforeClass
	public static void start() {
		jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
		service = new ProcessTypeDataServiceImpl();
		service.setJdbcTemplate(jdbcTemplateInstance);
	}
 
	@AfterClass
	public static void end() {
		jdbcTemplateInstance=null;
	}
	
	@Test
	public void persistAndGetById() {
		
		ProcessType process = new ProcessType();
		process.setName("test");
		process.setDescription("destest");
	
		Integer id = service.persist(process);
		
		service = new ProcessTypeDataServiceImpl();
		service.setJdbcTemplate(jdbcTemplateInstance);
		ProcessType persistedEntity = (ProcessType) service.getById(id);
		
		assertEquals(persistedEntity.getName(), process.getName()); 
	}
 
	@Test
	public void persistAndGetAll() {
		ProcessType process = new ProcessType();
		process.setName("test");
		process.setDescription("destest");
		ProcessType process2 = new ProcessType();
		process2.setName("test");
		process2.setDescription("destest");
		ArrayList<Entity> list = new ArrayList<Entity>();
		list.add(process);
		list.add(process2);
		service.persistAll(list);
		ArrayList<Entity> entities = service.getAll();
		
		assertEquals(process.getName(), ((ProcessType) entities.get(entities.size()-2)).getName()); 
		assertEquals(process2.getName(), ((ProcessType) entities.get(entities.size()-1)).getName()); 

	}
	

}