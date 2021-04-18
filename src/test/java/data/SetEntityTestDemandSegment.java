package data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import data.entity.ConsiderationSetAlternative;
import data.entity.DemandModelType;
import data.entity.DemandSegment;
import data.entity.DemandSegmentSet;
import data.entity.ProbabilityDistribution;
import data.entity.ResidenceAreaWeight;
import data.entity.ResidenceAreaWeighting;
import data.service.DemandSegmentDataService;
import data.utility.DataServiceProvider;

public class SetEntityTestDemandSegment {
	 
	private static DemandSegmentDataService service;
	 
	@BeforeClass
	public static void start() {
		service = DataServiceProvider.getDemandSegmentDataServiceImplInstance();

	}
 
	@AfterClass
	public static void end() {

	}
	
	@Test
	public void getFirstSegment() {
		
		DemandSegmentSet test = (DemandSegmentSet)	service.getAllSets().get(0);
		System.out.println("Demand segment set name: "+test.getName());
		
		DemandModelType type = (DemandModelType) test.getDemandModelType();
		
		System.out.println("Demand model type: "+type.getName());
		assertEquals(type.getName().equals("independent"), true);
		
		ArrayList<DemandSegment> segments = test.getElements();
		System.out.println("Number of segments: "+segments.size());
		assertEquals(segments.size(), 2);
		
		DemandSegment firstSegment = (DemandSegment) segments.get(0);
		
		ResidenceAreaWeighting weighting = (ResidenceAreaWeighting) firstSegment.getResidenceAreaWeighting();
		System.out.println("Residence area weighting comprises the following number of weights: "+weighting.getWeights().size());
		assertEquals(weighting.getWeights().size(), 9);
		
		ResidenceAreaWeight firstWeight = (ResidenceAreaWeight) weighting.getWeights().get(0);
		System.out.println("The first residence area weight refers to a residence area with the following first lat: "+firstWeight.getResidenceArea().getLat1());
		
		ArrayList<ConsiderationSetAlternative> considerationSet = firstSegment.getConsiderationSet();
		System.out.println("The first segment considers the following number of alternatives: "+ considerationSet.size());
		assertEquals(considerationSet.size(), 13);
		
		ConsiderationSetAlternative alt = (ConsiderationSetAlternative) considerationSet.get(0);
		System.out.println("The first alternative weight for this consideration set is: "+alt.getWeight());
		
		ProbabilityDistribution pdBasket = (ProbabilityDistribution) firstSegment.getBasketValueDistribution();
		System.out.println("The basket value probability distribution is called: "+pdBasket.getName());
		assertEquals(pdBasket.getName().equals("ND mean 50, sd 5"), true);
		
	}
 
	

}