package data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import data.entity.SetEntity;
import data.entity.TravelTime;
import data.entity.TravelTimeSet;
import data.service.TravelTimeDataService;
import data.service.TravelTimeDataServiceImpl;
import data.utility.DataServiceProvider;
import data.utility.JDBCTemplateProvider;
import logic.service.support.ProbabilityDistributionService;
import logic.utility.exceptions.ParameterUnknownException;

public class SetEntityTestTravelTime {
	 
	private static TravelTimeDataService service;
	 
	@BeforeClass
	public static void start() {
		service = DataServiceProvider.getTravelTimeDataServiceImplInstance();

	}
 
	@AfterClass
	public static void end() {

	}
	
	@Test
	public void persistAndGetById() throws ParameterUnknownException {
		
		TravelTimeSet travelTimeSet = new TravelTimeSet();
		travelTimeSet.setName("test");
		travelTimeSet.setRegionId(1);
		
		TravelTime travelTime = new TravelTime();
		travelTime.setProbabilityDistributionId(2);;
		travelTime.setStart(1.00);
		travelTime.setEnd(2.00);
		
		TravelTime travelTime2 = new TravelTime();
		travelTime2.setProbabilityDistributionId(13);
		travelTime2.setStart(3.00);
		travelTime2.setEnd(7.00);
		
		
		ArrayList<TravelTime> entities = new ArrayList<TravelTime>();
		entities.add(travelTime);
		entities.add(travelTime2);
		
		travelTimeSet.setElements(entities);
	
		Integer id = service.persistCompleteEntitySet(travelTimeSet);
		
		ArrayList<SetEntity> persistedEntities = service.getAllSets();
		
		assertEquals(persistedEntities.get(0).getId()==0, false); 
		
		TravelTimeDataService service2= new TravelTimeDataServiceImpl();
		service2.setJdbcTemplate(JDBCTemplateProvider.getInstance());
		
		TravelTimeSet persistedSet = (TravelTimeSet) service2.getSetById(id);
		
		assertEquals(persistedSet.getName(), travelTimeSet.getName()); 
		assertEquals(((TravelTime) persistedSet.getElements().get(0))!=null, true);
		
		System.out.println(ProbabilityDistributionService.getMeanByProbabilityDistribution(((TravelTime)persistedSet.getElements().get(0)).getProbabilityDistribution()));
		System.out.println(ProbabilityDistributionService.getMeanByProbabilityDistribution(((TravelTime)persistedSet.getElements().get(1)).getProbabilityDistribution()));

	}
 
	

}