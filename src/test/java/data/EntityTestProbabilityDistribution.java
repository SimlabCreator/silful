package data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import data.entity.DistributionParameterValue;
import data.entity.ParameterType;
import data.entity.ProbabilityDistribution;
import data.entity.ProbabilityDistributionType;
import data.service.ProbabilityDistributionDataService;
import data.service.ProbabilityDistributionDataServiceImpl;
import data.utility.DataServiceProvider;
import data.utility.JDBCTemplateProvider;

public class EntityTestProbabilityDistribution {
	 
	private static ProbabilityDistributionDataService service;
	 
	@BeforeClass
	public static void start() {
		service = DataServiceProvider.getProbabilityDistributionDataServiceImplInstance();
	}
 
	@AfterClass
	public static void end() {
	}
	
	@Test
	public void getByIdAndAll() {
		
		ProbabilityDistribution dis = (ProbabilityDistribution) service.getAll().get(0);
		
		assertEquals(dis!=null, true); 
		
		assertEquals(dis.getProbabilityDistributionType().getName()!=null, true);
		
		assertEquals((dis.getParameterValues().get(0)).getValue()!=null, true);
		
		ProbabilityDistributionDataServiceImpl disService = new ProbabilityDistributionDataServiceImpl();
		disService.setJdbcTemplate(JDBCTemplateProvider.getInstance());
		ProbabilityDistribution dis2 =(ProbabilityDistribution)disService.getById(1);
		assertEquals(dis2!=null, true);
		
	}
 
	@Test
	public void persist() {

		ProbabilityDistribution dis = new ProbabilityDistribution();
		Integer disType = ((ProbabilityDistributionType) DataServiceProvider.getProbabilityDistributionTypeDataServiceImplInstance().getAll().get(0)).getId();
		dis.setProbabilityDistributionTypeId(disType);
		dis.setName("test");
		DistributionParameterValue para = new DistributionParameterValue();
		para.setValue(3.0);
		Integer paraType =((ParameterType) DataServiceProvider.getParameterTypeDataServiceImplInstance().getAll().get(0)).getId();
		para.setParameterTypeId(paraType);
		ArrayList<DistributionParameterValue> values = new ArrayList<DistributionParameterValue>();
		values.add(para);
		dis.setParameterValues(values);
		
		Integer id = service.persistProbabilityDistribution(dis);
		
		assertEquals(id!=null, true); 

	}
	

}