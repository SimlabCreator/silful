package data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import data.entity.Alternative;
import data.entity.AlternativeSet;
import data.entity.SetEntity;
import data.service.AlternativeDataService;
import data.service.AlternativeDataServiceImpl;
import data.utility.DataServiceProvider;
import data.utility.JDBCTemplateProvider;

public class SetEntityTestAlternative {
	 
	private static AlternativeDataService service;
	 
	@BeforeClass
	public static void start() {
		service = DataServiceProvider.getAlternativeDataServiceImplInstance();

	}
 
	@AfterClass
	public static void end() {

	}
	
	@Test
	public void persistAndGetById() {
		
		AlternativeSet alternativeSet = new AlternativeSet();
		alternativeSet.setName("test");
		alternativeSet.setTimeWindowSetId(1);
		
		
		Alternative alternative = new Alternative();
		alternative.setTimeOfDay("morning");
		
		Alternative alternative2 = new Alternative();
		alternative2.setTimeOfDay("evening");
		
		ArrayList<Alternative> entities = new ArrayList<Alternative>();
		entities.add(alternative);
		entities.add(alternative2);
		
		alternativeSet.setElements(entities);
		
		Integer id = service.persistCompleteEntitySet(alternativeSet);
		
		ArrayList<SetEntity> persistedEntities = service.getAllSets();
		
		assertEquals(persistedEntities.get(0).getId()==0, false); 
		
		AlternativeDataService service2= new AlternativeDataServiceImpl();
		service2.setJdbcTemplate(JDBCTemplateProvider.getInstance());
		AlternativeSet persistedSet = (AlternativeSet) service2.getSetById(id);
		
		assertEquals(persistedSet.getName(), alternativeSet.getName()); 
		assertEquals(((Alternative) persistedSet.getElements().get(0))!=null, true);
		
	}
 
	

}