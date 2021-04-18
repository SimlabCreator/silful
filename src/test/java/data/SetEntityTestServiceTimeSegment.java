package data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import data.entity.ServiceTimeSegment;
import data.entity.ServiceTimeSegmentSet;
import data.entity.ServiceTimeSegmentWeight;
import data.entity.ServiceTimeSegmentWeighting;
import data.entity.SetEntity;
import data.entity.WeightingEntity;
import data.service.ServiceTimeSegmentDataService;
import data.service.ServiceTimeSegmentDataServiceImpl;
import data.utility.DataServiceProvider;
import data.utility.JDBCTemplateProvider;

public class SetEntityTestServiceTimeSegment {
	 
	private static ServiceTimeSegmentDataService service;
	 
	@BeforeClass
	public static void start() {
		service = DataServiceProvider.getServiceTimeSegmentDataServiceImplInstance();

	}
 
	@AfterClass
	public static void end() {

	}
	
	@Test
	public void persistAndGetServiceSegments() {
		
		ServiceTimeSegmentSet serviceTimeSegmentSet = new ServiceTimeSegmentSet();
		serviceTimeSegmentSet.setName("test");
		
		ServiceTimeSegment serviceTimeSegment = new ServiceTimeSegment();
		serviceTimeSegment.setProbabilityDistributionId(1);
		
		ServiceTimeSegment serviceTimeSegment2 = new ServiceTimeSegment();
		serviceTimeSegment2.setProbabilityDistributionId(2);
			
		ArrayList<ServiceTimeSegment> entities = new ArrayList<ServiceTimeSegment>();
		entities.add(serviceTimeSegment);
		entities.add(serviceTimeSegment2);
		
		serviceTimeSegmentSet.setElements(entities);
	
		Integer id = service.persistCompleteEntitySet(serviceTimeSegmentSet);
		
		ArrayList<SetEntity> persistedEntities = service.getAllSets();
		
		assertEquals(persistedEntities.get(0).getId()==0, false); 
		
		ServiceTimeSegmentDataService service2= new ServiceTimeSegmentDataServiceImpl();
		service2.setJdbcTemplate(JDBCTemplateProvider.getInstance());
		
		ServiceTimeSegmentSet persistedSet = (ServiceTimeSegmentSet) service2.getSetById(id);
		
		
		
		assertEquals(persistedSet.getName(), serviceTimeSegmentSet.getName()); 
		assertEquals(((ServiceTimeSegment) persistedSet.getElements().get(0))!=null, true);
		
		ServiceTimeSegmentDataService service3= new ServiceTimeSegmentDataServiceImpl();
		service3.setJdbcTemplate(JDBCTemplateProvider.getInstance());
		assertEquals(((ServiceTimeSegment)((ServiceTimeSegmentWeight)service3.getWeightById(1)).getServiceTimeSegment()).getProbabilityDistribution().getId(), 1);
	}
	
	@Test
	public void persistAndGetServiceSegmentWeights() {
		
		ServiceTimeSegmentWeighting ssw = new ServiceTimeSegmentWeighting();
		ssw.setName("test");
		ssw.setSetEntityId(1);
		
		ServiceTimeSegmentWeight serviceTimeSegmentWeight = new ServiceTimeSegmentWeight();
		serviceTimeSegmentWeight.setElementId(1);
		serviceTimeSegmentWeight.setWeight( 0.3);
		
		ServiceTimeSegmentWeight serviceTimeSegmentWeight2 = new ServiceTimeSegmentWeight();
		serviceTimeSegmentWeight2.setElementId(1);
		serviceTimeSegmentWeight2.setWeight(0.3);
			
		ArrayList<ServiceTimeSegmentWeight> entities = new ArrayList<ServiceTimeSegmentWeight>();
		entities.add(serviceTimeSegmentWeight);
		entities.add(serviceTimeSegmentWeight2);
		
		ssw.setWeights(entities);
	
		Integer id = service.persistCompleteWeighting(ssw);
		
		ArrayList<WeightingEntity> persistedEntities = service.getAllWeightings();
		
		assertEquals(persistedEntities.get(0).getId()==0, false); 
		
		ServiceTimeSegmentDataService service2= new ServiceTimeSegmentDataServiceImpl();
		service2.setJdbcTemplate(JDBCTemplateProvider.getInstance());
		
		ServiceTimeSegmentWeighting persistedSet = (ServiceTimeSegmentWeighting) service2.getWeightingById(id);
		assertEquals(persistedSet.getName(), ssw.getName()); 
		assertEquals(((ServiceTimeSegmentWeight) persistedSet.getWeights().get(0))!=null, true);
		
	}
 
	

}