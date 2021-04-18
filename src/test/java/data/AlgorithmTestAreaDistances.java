package data;


import java.util.ArrayList;
import java.util.Collections;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.DemandSegmentForecastSet;
import data.entity.Depot;
import data.entity.Entity;
import data.entity.Experiment;
import data.entity.Node;
import data.entity.NodeDistance;
import data.entity.ObjectiveWeight;
import data.entity.TimeWindowSet;
import data.entity.TravelTimeSet;
import data.entity.Vehicle;
import data.utility.DataServiceProvider;
import logic.algorithm.vr.construction.Old_InsertionConstructionHeuristicAdaption;
import logic.entity.ForecastedOrderRequest;
import logic.service.support.AcceptanceService;
import logic.service.support.CapacityService;
import logic.service.support.LocationService;
import logic.utility.InputPreparator;
import logic.utility.SettingsProvider;
import logic.utility.comparator.ForecastedOrderRequestSelectedAlternativeDescComparator;
import logic.utility.exceptions.ParameterUnknownException;

public class AlgorithmTestAreaDistances {
	 
	
	 
	@BeforeClass
	public static void start() {

	}
 
	@AfterClass
	public static void end() {

	}
	
	@Test
	public void persistAndGet() throws ParameterUnknownException {
		
		DeliveryAreaSet deliveryAreaSet = (DeliveryAreaSet) DataServiceProvider.getDeliveryAreaDataServiceImplInstance().getSetById(2);
		Experiment experiment = new Experiment();
		experiment.setRegionId(1);
		experiment.setDepotId(1);
		experiment.setObjectives(new ArrayList<ObjectiveWeight>());
		SettingsProvider.setExperiment(experiment);
		ArrayList<Node> nodes = InputPreparator.getNodes();
		ArrayList<NodeDistance> distances = InputPreparator.getDistances();	
		
		double[][] distanceAreas=LocationService.computeDistancesBetweenAreas(nodes, distances, deliveryAreaSet);

		ArrayList<DeliveryArea> areas =deliveryAreaSet.getElements();
		for(Entity area: areas) {
			System.out.println(((DeliveryArea) area).getId());
		}
		
		for(int i  = 0; i < distanceAreas.length; i++) {
			for(int j =0; j < distanceAreas[i].length; j++){
				System.out.println("area1: "+i+"area2: "+j+" distance: "+distanceAreas[i][j]);
			}
		}
		
		DemandSegmentForecastSet demandForecastSet = (DemandSegmentForecastSet) DataServiceProvider.getDemandSegmentForecastDataServiceImplInstance().getSetById(1);
		TimeWindowSet timeWindowSet = demandForecastSet.getDemandSegmentSet().getAlternativeSet().getTimeWindowSet();
		TravelTimeSet travelTimeSet = (TravelTimeSet) DataServiceProvider.getTravelTimeDataServiceImplInstance().getSetById(1);
		Depot depot = SettingsProvider.getExperiment().getDepot();
		ArrayList<ObjectiveWeight> objectives = SettingsProvider.getExperiment().getObjectives();
		Double maximumRevenueValue=AcceptanceService.determineMaximumRevenueValueForNormalisation(demandForecastSet.getDemandSegmentSet());
		ArrayList<Vehicle> vehicles=new ArrayList<Vehicle>();
		
		// Transfer forecasts to pseudo-requests and set all service times constant as provided by the user

		ArrayList<ForecastedOrderRequest> requests = CapacityService
				.getForecastedOrderRequestsByDemandSegmentForecastSetRatioVisibilityHardConstraint(demandForecastSet, nodes,objectives,maximumRevenueValue);
		
		Collections.sort(requests, new ForecastedOrderRequestSelectedAlternativeDescComparator());
		Double serviceTime = 12.0;
		for(ForecastedOrderRequest request: requests){
			request.setServiceTime(serviceTime);

		}
		
		System.out.println("number of requests: "+requests.size());		

		// Construct routing
		Old_InsertionConstructionHeuristicAdaption algo = new Old_InsertionConstructionHeuristicAdaption(requests,
				timeWindowSet,
				depot, vehicles, distances,nodes, travelTimeSet, deliveryAreaSet, false);
		algo.start();
		
		//assertEquals(persistedSet.getName(), arrivalProcess.getName()); 
		
	}
	
	
	

}