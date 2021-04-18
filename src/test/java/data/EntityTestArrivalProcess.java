package data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import data.entity.ArrivalProcess;
import data.entity.Entity;
import data.service.ArrivalProcessDataService;
import data.service.ArrivalProcessDataServiceImpl;
import data.utility.DataServiceProvider;
import data.utility.JDBCTemplateProvider;

public class EntityTestArrivalProcess {
	 
	private static ArrivalProcessDataService service;
	 
	@BeforeClass
	public static void start() {
		service = DataServiceProvider.getArrivalProcessDataServiceImplInstance();

	}
 
	@AfterClass
	public static void end() {

	}
	
	@Test
	public void persistAndGet() {
		
		
		
		ArrivalProcess arrivalProcess = new ArrivalProcess();
		arrivalProcess.setName("testen");
		arrivalProcess.setProbabilityDistributionId(1);
		arrivalProcess.setFactor( 3.0);
		
	
		Integer id = service.persist(arrivalProcess);
		
		ArrayList<Entity> persistedEntities = service.getAll();
		
		assertEquals(((ArrivalProcess)persistedEntities.get(0))==null, false); 
		
		ArrivalProcessDataService service2= new ArrivalProcessDataServiceImpl();
		service2.setJdbcTemplate(JDBCTemplateProvider.getInstance());
		
		ArrivalProcess persistedSet = (ArrivalProcess) service2.getById(id);

		assertEquals(persistedSet.getName(), arrivalProcess.getName()); 
		
	}
	
	
	

}