
package logic.process.support;

import java.util.ArrayList;
import java.util.Random;

import data.entity.ArrivalProcess;
import data.entity.Customer;
import data.entity.CustomerSet;
import data.entity.DemandSegment;
import data.entity.DemandSegmentWeight;
import data.entity.OrderRequest;
import data.entity.OrderRequestSet;
import data.entity.PeriodSetting;
import data.entity.ServiceTimeSegmentWeight;
import data.utility.DataServiceProvider;
import data.utility.PeriodSettingType;
import data.utility.SettingRequest;
import logic.process.IProcess;
import logic.service.support.ArrivalProcessService;
import logic.service.support.CustomerDemandService;
import logic.service.support.LocationService;
import logic.service.support.ParameterService;
import logic.service.support.ProbabilityDistributionService;
import logic.utility.NameProvider;
import logic.utility.SettingsProvider;
import logic.utility.exceptions.ParameterUnknownException;
import logic.utility.exceptions.ProbabilitiesDoNotSumUpToOneException;

public class DataGenerationOrderRequests implements IProcess {
	
	private static String PARAMETER = "samplePreferences";


	public Boolean needIncentiveType() {

		return false;
	}

	public Boolean needBookingPeriodLength() {

		return true;
	}

	public SettingRequest getSettingRequest() {

		SettingRequest request = new SettingRequest();
		request.addPeriodSetting(PeriodSettingType.ARRIVALPROCESS, false);
		request.addPeriodSetting(PeriodSettingType.DEMANDSEGMENTWEIGHTING, false);
		request.addPeriodSetting(PeriodSettingType.SERVICESEGMENTWEIGHTING, false);
		request.addParameter(PARAMETER);
		return request;
	}

	public void start() {

		System.out.println("I start");
		// Go through all booking periods
		//for (int i = 0; i < SettingsProvider.getCurrentRun().getBookingPeriodNumber(); i++) {
			try {
				this.generateData();
			} catch (ParameterUnknownException e) {
				e.printStackTrace();
				System.exit(0);
			}
	}

	private void generateData() throws ParameterUnknownException {
		
		PeriodSetting currentPeriodSetting = SettingsProvider.getPeriodSetting();
	
		// Initialize new customer set
		CustomerSet cSet = new CustomerSet();
		cSet.setOriginalDemandSegmentSetId(DataServiceProvider.getDemandSegmentDataServiceImplInstance()
				.getWeightingById(currentPeriodSetting.getDemandSegmentWeightingId())
				.getSetEntityId());


		// Initialize new order request set
		OrderRequestSet orSet = new OrderRequestSet();
		orSet.setCustomerSet(cSet);
		
		Double samplePreferences = ParameterService.getRespectiveParameterValue(PARAMETER,
				currentPeriodSetting.getParameterValues());

		orSet.setPreferencesSampled(samplePreferences!=0);
		
		ArrayList<OrderRequest> orderRequests = new ArrayList<OrderRequest>();
		
		// Go through all time steps and produce customers
		int tempT = SettingsProvider.getExperiment().getBookingPeriodLength();
		orSet.setBookingHorizon(tempT);
		ArrivalProcess arrivalProcess = (ArrivalProcess) DataServiceProvider.getArrivalProcessDataServiceImplInstance().getById(currentPeriodSetting.getArrivalProcessId());
	
		double startTime = System.currentTimeMillis();
		while (tempT > 0) {

			
			// Identify arrival probability for arrival process definition
			double arrivalProbability = ArrivalProcessService.getArrivalProbability(tempT,
					arrivalProcess);
			
			// Check if customer arrives
			Random rd1 = new Random();
			double randomNumber = rd1.nextDouble();
			
			if (randomNumber <= arrivalProbability) {
				
				Customer cus=this.generateCustomer(tempT);
				OrderRequest or=this.generateOrderRequest(cus, samplePreferences);
				orderRequests.add(or);
			}

			
			tempT--;
		}
		
	//	System.out.println("Build requests-time:"+ (System.currentTimeMillis()-startTime));
		orSet.setElements(orderRequests); 
		NameProvider.setNameOrderRequestSet(orSet);
		startTime= System.currentTimeMillis();
		int orderRequestSetId = DataServiceProvider.getOrderRequestDataServiceImplInstance().persistCompleteOrderRequestAndCustomerSet(orSet);
	//	System.out.println("Save order request set-time:"+ (System.currentTimeMillis()-startTime));
		currentPeriodSetting.setOrderRequestSetId(orderRequestSetId);

	}

	private Customer generateCustomer( int tempT){
		
		//Initialize new customer
				Customer customer=new Customer();
				customer.setTempT(tempT);
				
				//Determine demand segment
				DemandSegmentWeight weight = null;
				try {
					weight = (DemandSegmentWeight) ProbabilityDistributionService.getRandomWeightByWeighting(DataServiceProvider.getDemandSegmentDataServiceImplInstance()
							.getWeightingById(SettingsProvider.getPeriodSetting().getDemandSegmentWeightingId()));

				} catch (ProbabilitiesDoNotSumUpToOneException e) {
					System.out.println("Determine demand segment weight do not sum up to 1!");
					e.printStackTrace();
					System.exit(0);
				}
				
				DemandSegment demandSegment=weight.getDemandSegment();
				customer.setOriginalDemandSegmentId(demandSegment.getId());
				
				//Determine customer location
				ArrayList<Double> location =new ArrayList<Double>();
				try {
					location = LocationService.getRandomLocationByResidenceAreaWeighting(demandSegment.getResidenceAreaWeighting());
				} catch (ProbabilitiesDoNotSumUpToOneException e) {
					System.out.println("Customer location weight do not sum up to 1!");
					e.printStackTrace();
					System.exit(0);
				}
				customer.setLat(location.get(0));
				customer.setLon(location.get(1));
				
				//Determine customer service time segment
				ServiceTimeSegmentWeight stSegmentWeight = null;
				try {
					stSegmentWeight = (ServiceTimeSegmentWeight) ProbabilityDistributionService.getRandomWeightByWeighting(DataServiceProvider.getServiceTimeSegmentDataServiceImplInstance()
								.getWeightingById(SettingsProvider.getPeriodSetting().getServiceSegmentWeightingId()));
				} catch (ProbabilitiesDoNotSumUpToOneException e) {
					System.out.println("Customer service time segment weight do not sum up to 1!");
					e.printStackTrace();
					System.exit(0);
				}
				
				customer.setServiceTimeSegmentId(stSegmentWeight.getElementId());
				return customer;
	}
	
	private OrderRequest generateOrderRequest(Customer customer, Double samplePreferences) throws ParameterUnknownException {
		
		//Initialize new order request
		OrderRequest request = new OrderRequest();
		request.setCustomer(customer);
		request.setArrivalTime(customer.getTempT());
		
		//Determine basket value
		request.setBasketValue(ProbabilityDistributionService.getRandomNumberByProbabilityDistribution(customer.getOriginalDemandSegment().getBasketValueDistribution()));
		

		if (samplePreferences>0) {
			request.setAlternativePreferences(CustomerDemandService.sampleAlternativePreferences(request, samplePreferences));
		}
		
		return request;
	}

	public Boolean multiplePeriodsPossible() {
		
		return false;
	}

	public ArrayList<PeriodSettingType> getOutputs() {
		ArrayList<PeriodSettingType> outputs = new ArrayList<PeriodSettingType>();
		outputs.add(PeriodSettingType.CUSTOMERSET);
		outputs.add(PeriodSettingType.ORDERREQUESTSET);
		
		return outputs;
	}
	
	public Boolean needDepotLocation() {
		
		return false;
	}
	
public Boolean multipleObjectivesPossible() {
		
		return false;
	}

}
