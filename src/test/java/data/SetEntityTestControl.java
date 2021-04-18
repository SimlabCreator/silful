package data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import data.entity.Control;
import data.entity.ControlSet;
import data.entity.SetEntity;
import data.service.ControlDataService;
import data.service.ControlDataServiceImpl;
import data.utility.DataServiceProvider;
import data.utility.JDBCTemplateProvider;

public class SetEntityTestControl {
	 
	private static ControlDataService service;
	 
	@BeforeClass
	public static void start() {
		service = DataServiceProvider.getControlDataServiceImplInstance();

	}
 
	@AfterClass
	public static void end() {

	}
	
	@Test
	public void persistAndGetById() {
		
		ControlSet controlSet = new ControlSet();
		controlSet.setName("test");
		
		Control control = new Control();
		control.setControlNumber(3);
		control.setAlternativeId(1);
		control.setDeliveryAreaId(1);
		
		Control control2 = new Control();
		control2.setControlNumber(3);
		control2.setAlternativeId(1);
		control2.setDeliveryAreaId(1);
		
		
		ArrayList<Control> entities = new ArrayList<Control>();
		entities.add(control);
		entities.add(control2);
		
		controlSet.setElements(entities);
	
		Integer id = service.persistCompleteEntitySet(controlSet);
		
		ArrayList<SetEntity> persistedEntities = service.getAllSets();
		
		assertEquals(persistedEntities.get(0).getId()==0, false); 
		
		ControlDataService service2= new ControlDataServiceImpl();
		service2.setJdbcTemplate(JDBCTemplateProvider.getInstance());
		
		ControlSet persistedSet = (ControlSet) service.getSetById(id);
		
		assertEquals(persistedSet.getName(), controlSet.getName()); 
		assertEquals(((Control) persistedSet.getElements().get(0))!=null, true);
		
	}
 
	

}