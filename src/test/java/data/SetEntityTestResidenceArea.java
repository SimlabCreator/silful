package data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import data.entity.Region;
import data.entity.ResidenceArea;
import data.entity.ResidenceAreaSet;
import data.entity.SetEntity;
import data.service.RegionDataServiceImpl;
import data.service.ResidenceAreaDataServiceImpl;
import data.utility.JDBCTemplateProvider;

public class SetEntityTestResidenceArea {
	 
	private static JdbcTemplate jdbcTemplateInstance;
	private static ResidenceAreaDataServiceImpl service;
	 
	@BeforeClass
	public static void start() {
		jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
		service = new ResidenceAreaDataServiceImpl();
		service.setJdbcTemplate(jdbcTemplateInstance);
	}
 
	@AfterClass
	public static void end() {
		jdbcTemplateInstance=null;
	}
	
	@Test
	public void persistAndGetById() {
		
		ResidenceAreaSet residenceAreaSet = new ResidenceAreaSet();
		residenceAreaSet.setName("test");
		RegionDataServiceImpl regionService = new RegionDataServiceImpl();
		regionService.setJdbcTemplate(jdbcTemplateInstance);
		Region region = (Region) regionService.getAll().get(0);
		residenceAreaSet.setRegionId(region.getId());
		
		ResidenceArea area1 = new ResidenceArea();
		area1.setLat1((float) 49.00000);
		area1.setLon1((float) 11.00000);
		area1.setLat2((float) 49.00000);
		area1.setLon2((float) 11.00000);

		
		ResidenceArea area2 = new ResidenceArea();
		area2.setLat1((float) 49.00000);
		area2.setLon1((float) 11.00000);
		area2.setLat2((float) 49.00000);
		area2.setLon2((float) 11.00000);

		
		ArrayList<ResidenceArea> entities = new ArrayList<ResidenceArea>();
		entities.add(area2);
		entities.add(area1);
		
		residenceAreaSet.setElements(entities);
	
		Integer id = service.persistCompleteEntitySet(residenceAreaSet);
		
		//Ensure loading from database because not loaded as list before
		service = new ResidenceAreaDataServiceImpl();
		service.setJdbcTemplate(jdbcTemplateInstance);
		
		ResidenceAreaSet persistedSet = (ResidenceAreaSet) service.getSetById(id);
		
		assertEquals(persistedSet.getName(), residenceAreaSet.getName()); 
		assertEquals(((ResidenceArea) persistedSet.getElements().get(0))!=null, true);
		
	}
 
	@Test
	public void getAll() {

		ArrayList<SetEntity> entities = service.getAllSets();
		
		assertEquals(entities.get(0).getId()==0, false); 
		

	}
	

}