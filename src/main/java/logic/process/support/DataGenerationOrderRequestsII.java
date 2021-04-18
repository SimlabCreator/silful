package logic.process.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

import org.apache.commons.math3.util.Pair;

import data.entity.Customer;
import data.entity.CustomerSet;
import data.entity.DemandSegment;
import data.entity.DemandSegmentSet;
import data.entity.DemandSegmentWeight;
import data.entity.DemandSegmentWeighting;
import data.entity.OrderRequest;
import data.entity.OrderRequestSet;
import data.entity.PeriodSetting;
import data.entity.ProbabilityDistribution;
import data.entity.ResidenceArea;
import data.entity.ResidenceAreaWeight;
import data.entity.ResidenceAreaWeighting;
import data.entity.ServiceTimeSegment;
import data.entity.ServiceTimeSegmentWeight;
import data.entity.ServiceTimeSegmentWeighting;
import data.utility.DataServiceProvider;
import data.utility.PeriodSettingType;
import data.utility.SettingRequest;
import logic.process.IProcess;
import logic.service.support.CustomerDemandService;
import logic.service.support.LocationService;
import logic.service.support.ParameterService;
import logic.service.support.ProbabilityDistributionService;
import logic.utility.NameProvider;
import logic.utility.SettingsProvider;
import logic.utility.comparator.PairDoubleValueAscComparator;
import logic.utility.comparator.PairDoubleValueDescComparator;
import logic.utility.exceptions.ParameterUnknownException;
import logic.utility.exceptions.ProbabilitiesDoNotSumUpToOneException;

/**
 * Data generation process, in which the requests are not generated per time
 * step in the arrival process but an overall customer base is generated and
 * arrival times are assigned afterwards
 * 
 * @author M. Lang
 *
 */
public class DataGenerationOrderRequestsII implements IProcess {

	private static String SAMPLE_PREFERENCES_PARAMETER = "samplePreferences";
	private static String EXACT_PARAMETER = "exactAssignments";

	public Boolean needIncentiveType() {

		return false;
	}

	public Boolean needBookingPeriodLength() {

		return true;
	}

	public SettingRequest getSettingRequest() {

		SettingRequest request = new SettingRequest();
		request.addPeriodSetting(PeriodSettingType.ARRIVAL_PROBABILITY_DISTRIBUTION, false);
		request.addPeriodSetting(PeriodSettingType.DEMANDSEGMENTWEIGHTING, false);
		request.addPeriodSetting(PeriodSettingType.SERVICESEGMENTWEIGHTING, false);
		request.addParameter(SAMPLE_PREFERENCES_PARAMETER);
		request.addParameter(EXACT_PARAMETER);
		return request;
	}

	public void start() {

		try {
			this.generateData();
		} catch (ParameterUnknownException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	private void generateData() throws ParameterUnknownException {
		PeriodSetting currentPeriodSetting = SettingsProvider.getPeriodSetting();
		DemandSegmentWeighting demandSegmentWeighting = (DemandSegmentWeighting) DataServiceProvider
				.getDemandSegmentDataServiceImplInstance()
				.getWeightingById(currentPeriodSetting.getDemandSegmentWeightingId());
		ServiceTimeSegmentWeighting serviceTimeWeighting = (ServiceTimeSegmentWeighting) DataServiceProvider
				.getServiceTimeSegmentDataServiceImplInstance()
				.getWeightingById(currentPeriodSetting.getServiceSegmentWeightingId());
		Double samplePreferences = ParameterService.getRespectiveParameterValue(SAMPLE_PREFERENCES_PARAMETER,
				currentPeriodSetting.getParameterValues());
		Double exactAssignments = ParameterService.getRespectiveParameterValue(EXACT_PARAMETER,
				currentPeriodSetting.getParameterValues());
		ProbabilityDistribution arrDistribution = (ProbabilityDistribution) DataServiceProvider
				.getProbabilityDistributionDataServiceImplInstance()
				.getById(currentPeriodSetting.getArrivalProbabilityDistributionId());
		int T = SettingsProvider.getExperiment().getBookingPeriodLength();
		Pair<CustomerSet, OrderRequestSet> result = this.generateData(T, arrDistribution, demandSegmentWeighting,
				serviceTimeWeighting, samplePreferences, exactAssignments.intValue(), true);

		currentPeriodSetting.setCustomerSetId(result.getKey().getId());
		currentPeriodSetting.setOrderRequestSetId(result.getValue().getId());
	}

	public Pair<CustomerSet, OrderRequestSet> generateData(int T, ProbabilityDistribution arrDistribution,
			DemandSegmentWeighting demandSegmentWeighting, ServiceTimeSegmentWeighting serviceTimeWeighting,
			double samplePreferences, int exactAssignments, boolean persist) throws ParameterUnknownException {

		// Initialize new customer set
		CustomerSet cSet = new CustomerSet();

		cSet.setOriginalDemandSegmentSetId(demandSegmentWeighting.getSetEntityId());
		int currentDummyResidenceAreaId = 0;

		ArrayList<Customer> customers = new ArrayList<Customer>();

		// Initialize new order request set
		OrderRequestSet orSet = new OrderRequestSet();

		orSet.setPreferencesSampled(samplePreferences != 0);

		ArrayList<OrderRequest> orderRequests = new ArrayList<OrderRequest>();

		orSet.setBookingHorizon(T);
		ArrayList<Integer> arrivalTimeCandidates = new ArrayList<Integer>();
		for (int i = 1; i <= T; i++) {
			arrivalTimeCandidates.add(i);
		}

		int numberOfArrivals;
		// For an exact assignment, the probabilities of, for instance, segment
		// weightings should be seen as proportions
		if (exactAssignments == 0.0) {
			numberOfArrivals = (int) (ProbabilityDistributionService
					.getRandomNumberByProbabilityDistribution(arrDistribution) * T);

			HashMap<Integer, ResidenceAreaWeighting> residenceAreaWeightingPerSegment = new HashMap<Integer, ResidenceAreaWeighting>();

			for (DemandSegment s : ((DemandSegmentSet) demandSegmentWeighting.getSetEntity()).getElements()) {
				ResidenceAreaWeighting weighting = new ResidenceAreaWeighting();
				ArrayList<ResidenceAreaWeight> weights = new ArrayList<ResidenceAreaWeight>();
				for (ResidenceAreaWeight w : s.getResidenceAreaWeighting().getWeights()) {
					// Separate into subareas?

					if (w.getResidenceArea().getReasonableSubareaNumber() != null
							&& w.getResidenceArea().getReasonableSubareaNumber() > 0) {
						ArrayList<ResidenceArea> areas = new ArrayList<ResidenceArea>();
						currentDummyResidenceAreaId = currentDummyResidenceAreaId
								- w.getResidenceArea().getReasonableSubareaNumber();
						areas = LocationService.determineSameSizeResidenceAreasWithDummyIds(currentDummyResidenceAreaId,
								w.getResidenceArea().getLat1(), w.getResidenceArea().getLat2(),
								w.getResidenceArea().getLon1(), w.getResidenceArea().getLon2(),
								(int) Math.sqrt(w.getResidenceArea().getReasonableSubareaNumber()),
								(int) Math.sqrt(w.getResidenceArea().getReasonableSubareaNumber()));

						for (ResidenceArea area : areas) {
							Double weightForArea = w.getWeight() / ((double) areas.size());
							ResidenceAreaWeight raW = new ResidenceAreaWeight();
							raW.setResidenceArea(area);
							raW.setWeight(weightForArea);
							weights.add(raW);
						}
					} else {
						weights.add(w);
					}

				}

				weighting.setWeights(weights);
				residenceAreaWeightingPerSegment.put(s.getId(), weighting);
			}

			for (int i = 0; i < numberOfArrivals; i++) {
				customers.add(this.generateCustomer(residenceAreaWeightingPerSegment, demandSegmentWeighting,
						serviceTimeWeighting));
			}

		} else {
			numberOfArrivals = (int) (ProbabilityDistributionService.getMeanByProbabilityDistribution(arrDistribution)
					* T);

			// Assign arrivals to demand segments according to weighting
			// proportions
			HashMap<DemandSegment, Integer> numberOfArrivalsPerSegment = new HashMap<DemandSegment, Integer>();
			this.assignArrivalsToSegmentsExactly(demandSegmentWeighting, numberOfArrivals, numberOfArrivalsPerSegment);

			// Assign arrivals to service time segments according to proportions
			ArrayList<Integer> serviceSegments = new ArrayList<Integer>();
			if (serviceTimeWeighting != null) {
				this.assignArrivalsToServiceSegmentsExactly(serviceTimeWeighting, numberOfArrivals, serviceSegments);
			}

			// Assign arrivals to locations according to proportions (and
			// subareas)
			for (DemandSegment s : numberOfArrivalsPerSegment.keySet()) {

				HashMap<ResidenceArea, Integer> numberPerArea = new HashMap<ResidenceArea, Integer>();
				ArrayList<Pair<ResidenceArea, Double>> roundingDifferences = new ArrayList<Pair<ResidenceArea, Double>>();
				int tempOverallSum = 0;
				for (ResidenceAreaWeight w : s.getResidenceAreaWeighting().getWeights()) {
					// Separate into subareas?
					ArrayList<ResidenceArea> areas = new ArrayList<ResidenceArea>();
					if (w.getResidenceArea().getReasonableSubareaNumber() != null
							&& w.getResidenceArea().getReasonableSubareaNumber() > 0) {
						currentDummyResidenceAreaId = currentDummyResidenceAreaId
								- w.getResidenceArea().getReasonableSubareaNumber();
						areas = LocationService.determineSameSizeResidenceAreasWithDummyIds(currentDummyResidenceAreaId,
								w.getResidenceArea().getLat1(), w.getResidenceArea().getLat2(),
								w.getResidenceArea().getLon1(), w.getResidenceArea().getLon2(),
								(int) Math.sqrt(w.getResidenceArea().getReasonableSubareaNumber()),
								(int) Math.sqrt(w.getResidenceArea().getReasonableSubareaNumber()));
					} else {
						areas.add(w.getResidenceArea());
					}

					for (ResidenceArea area : areas) {
						Double numberForArea = numberOfArrivalsPerSegment.get(s) * w.getWeight() / ((double) areas.size());
						double digits = numberForArea - Math.round(numberForArea);
						// If it was rounded up, the lower the value, the higher
						// the negative digits -> sort ascending and take first
						// If it was rounded down, the higher the value, the
						// higher the positive digits -> sort descending and
						// take first
						if(digits!=0.0){
							roundingDifferences.add(new Pair<ResidenceArea, Double>(area, digits));
						}					
						numberPerArea.put(area, numberForArea.intValue());
						tempOverallSum += numberForArea.intValue();
					}

				}
				Collections.shuffle(roundingDifferences);
				
				Collections.sort(roundingDifferences, new PairDoubleValueAscComparator());
				while (tempOverallSum > numberOfArrivalsPerSegment.get(s)) {
					int oldNumber = numberPerArea.get(roundingDifferences.get(0).getKey());
					numberPerArea.put(roundingDifferences.get(0).getKey(), --oldNumber);
					roundingDifferences.remove(0);
					tempOverallSum--;
				}

				Collections.sort(roundingDifferences, new PairDoubleValueDescComparator());
				while (tempOverallSum < numberOfArrivalsPerSegment.get(s)) {
					int oldNumber = numberPerArea.get(roundingDifferences.get(0).getKey());
					numberPerArea.put(roundingDifferences.get(0).getKey(), ++oldNumber);
					roundingDifferences.remove(0);
					tempOverallSum++;
				}

				for (ResidenceArea area : numberPerArea.keySet()) {
					// Divide area between the respective customers
					for (int i = 0; i < numberPerArea.get(area); i++) {
						Random r = new Random();
	
						Customer c;
						if (serviceSegments.size() > 0) {
							int randomIndexServiceSegment = r.nextInt(serviceSegments.size());
							c = this.generateCustomer(s, serviceSegments.get(randomIndexServiceSegment),
									area);
							serviceSegments.remove(randomIndexServiceSegment);
						} else {
							c = this.generateCustomer(s, null, area);
						}
						customers.add(c);
					}

				}
			}
		}

		cSet.setElements(customers);
		NameProvider.setNameCustomerSet(cSet);
		int customerSetId = 0;
		if (persist){
			customerSetId = DataServiceProvider.getCustomerDataServiceImplInstance().persistCompleteEntitySet(cSet);
			cSet.setId(customerSetId);
		}
			

		// For all customers, an order request is produced
		orSet.setCustomerSetId(customerSetId);
		ArrayList<Customer> customersWithIds = null;
		if (persist)
			customersWithIds = DataServiceProvider.getCustomerDataServiceImplInstance()
					.getAllElementsBySetId(customerSetId);

		for (int i = 0; i < customers.size(); i++) {
			Random r = new Random();
			int randomIndex = r.nextInt(arrivalTimeCandidates.size());
			int arrivalTime = arrivalTimeCandidates.get(randomIndex);
			OrderRequest or;
			if (persist) {
				or = this.generateOrderRequest(customersWithIds.get(i), samplePreferences, arrivalTime);
			} else {
				or = this.generateOrderRequest(customers.get(i), samplePreferences, arrivalTime);
			}
			orderRequests.add(or);
			arrivalTimeCandidates.remove(randomIndex);
		}

		orSet.setElements(orderRequests);
		NameProvider.setNameOrderRequestSet(orSet);
		if (persist){
			int orId = DataServiceProvider.getOrderRequestDataServiceImplInstance().persistCompleteEntitySet(orSet);
			orSet.setId(orId);
		}
			

		return new Pair<CustomerSet, OrderRequestSet>(cSet, orSet);
	}

	private Customer generateCustomer(DemandSegment demandSegment, Integer serviceTimeSegmentId, ResidenceArea area) {
		Customer customer = new Customer();
		customer.setOriginalDemandSegmentId(demandSegment.getId());
		customer.setServiceTimeSegmentId(serviceTimeSegmentId);
		customer.setLat(ProbabilityDistributionService.getUniformRandomNumber(area.getLat1(),area.getLat2()));
		customer.setLon(ProbabilityDistributionService.getUniformRandomNumber(area.getLon1(), area.getLon2()));
		return customer;
	}

	private Customer generateCustomer(HashMap<Integer, ResidenceAreaWeighting> residenceAreaWeightingPerSegment,
			DemandSegmentWeighting demandSegmentWeighting, ServiceTimeSegmentWeighting serviceTimeSegmentWeighting) {

		// Initialize new customer
		Customer customer = new Customer();

		// Determine demand segment
		DemandSegmentWeight weight = null;
		try {
			weight = (DemandSegmentWeight) ProbabilityDistributionService
					.getRandomWeightByWeighting(demandSegmentWeighting);

		} catch (ProbabilitiesDoNotSumUpToOneException e) {
			System.out.println("Determine demand segment weight do not sum up to 1!");
			e.printStackTrace();
			System.exit(0);
		}

		DemandSegment demandSegment = weight.getDemandSegment();
		customer.setOriginalDemandSegmentId(demandSegment.getId());

		// Determine customer location
		ArrayList<Double> location = new ArrayList<Double>();
		try {
			location = LocationService.getRandomLocationByResidenceAreaWeighting(
					residenceAreaWeightingPerSegment.get(demandSegment.getId()));
		} catch (ProbabilitiesDoNotSumUpToOneException e) {
			System.out.println("Customer location weight do not sum up to 1!");
			e.printStackTrace();
			System.exit(0);
		}
		customer.setLat(location.get(0));
		customer.setLon(location.get(1));

		// Determine customer service time segment
		if (serviceTimeSegmentWeighting != null) {
			ServiceTimeSegmentWeight stSegmentWeight = null;
			try {
				stSegmentWeight = (ServiceTimeSegmentWeight) ProbabilityDistributionService
						.getRandomWeightByWeighting(DataServiceProvider.getServiceTimeSegmentDataServiceImplInstance()
								.getWeightingById(SettingsProvider.getPeriodSetting().getServiceSegmentWeightingId()));
			} catch (ProbabilitiesDoNotSumUpToOneException e) {
				System.out.println("Customer service time segment weight do not sum up to 1!");
				e.printStackTrace();
				System.exit(0);
			}

			customer.setServiceTimeSegmentId(stSegmentWeight.getElementId());
		}
		return customer;
	}

	private OrderRequest generateOrderRequest(Customer customer, double samplePreferences, int arrivalTime)
			throws ParameterUnknownException {

		// Initialize new order request
		OrderRequest request = new OrderRequest();
		request.setCustomerId(customer.getId());
		if(customer.getId()==0){
			request.setCustomer(customer);
		}
		request.setArrivalTime(arrivalTime);

		// Determine basket value
		request.setBasketValue(ProbabilityDistributionService.getRandomNumberByProbabilityDistribution(
				customer.getOriginalDemandSegment().getBasketValueDistribution()));

		if (samplePreferences > 0) {
			request.setAlternativePreferences(
					CustomerDemandService.sampleAlternativePreferences(request, samplePreferences));
		}

		return request;
	}

	private void assignArrivalsToSegmentsExactly(DemandSegmentWeighting demandSegmentWeighting, int numberOfArrivals,
			HashMap<DemandSegment, Integer> numberOfArrivalsPerSegment) {
		ArrayList<Pair<DemandSegmentWeight, Double>> roundingDifferences = new ArrayList<Pair<DemandSegmentWeight, Double>>();
		int tempOverallSum = 0;
		for (DemandSegmentWeight weight : demandSegmentWeighting.getWeights()) {
			Double numberForSegment = numberOfArrivals * weight.getWeight();
			double digits = numberForSegment - Math.round(numberForSegment);
			// If it was rounded up, the lower the value, the higher the
			// negative digits -> sort ascending and take first
			// If it was rounded down, the higher the value, the higher the
			// positive digits -> sort descending and take first
			roundingDifferences.add(new Pair<DemandSegmentWeight, Double>(weight, digits));
			tempOverallSum += numberForSegment.intValue();
			numberOfArrivalsPerSegment.put(weight.getDemandSegment(), numberForSegment.intValue());
		}

		Collections.sort(roundingDifferences, new PairDoubleValueAscComparator());
		while (tempOverallSum > numberOfArrivals) {
			int oldNumber = numberOfArrivalsPerSegment.get(roundingDifferences.get(0).getKey().getDemandSegment());
			numberOfArrivalsPerSegment.put(roundingDifferences.get(0).getKey().getDemandSegment(), --oldNumber);
			roundingDifferences.remove(0);
			tempOverallSum--;
		}

		Collections.sort(roundingDifferences, new PairDoubleValueDescComparator());
		while (tempOverallSum < numberOfArrivals) {
			int oldNumber = numberOfArrivalsPerSegment.get(roundingDifferences.get(0).getKey().getDemandSegment());
			numberOfArrivalsPerSegment.put(roundingDifferences.get(0).getKey().getDemandSegment(), ++oldNumber);
			roundingDifferences.remove(0);
			tempOverallSum++;
		}
	}

	private void assignArrivalsToServiceSegmentsExactly(ServiceTimeSegmentWeighting serviceTimeSegmentWeighting,
			int numberOfArrivals, ArrayList<Integer> segments) {
		HashMap<ServiceTimeSegment, Integer> numberOfArrivalsPerServiceSegment = new HashMap<ServiceTimeSegment, Integer>();
		ArrayList<Pair<ServiceTimeSegmentWeight, Double>> roundingDifferences = new ArrayList<Pair<ServiceTimeSegmentWeight, Double>>();
		int tempOverallSum = 0;
		for (ServiceTimeSegmentWeight weight : serviceTimeSegmentWeighting.getWeights()) {
			Double numberForSegment = numberOfArrivals * weight.getWeight();
			double digits = numberForSegment - Math.round(numberForSegment);
			// If it was rounded up, the lower the value, the higher the
			// negative digits -> sort ascending and take first
			// If it was rounded down, the higher the value, the higher the
			// positive digits -> sort descending and take first
			roundingDifferences.add(new Pair<ServiceTimeSegmentWeight, Double>(weight, digits));
			tempOverallSum += numberForSegment.intValue();
			numberOfArrivalsPerServiceSegment.put(weight.getServiceTimeSegment(), numberForSegment.intValue());
		}

		Collections.sort(roundingDifferences, new PairDoubleValueAscComparator());
		while (tempOverallSum > numberOfArrivals) {
			int oldNumber = numberOfArrivalsPerServiceSegment
					.get(roundingDifferences.get(0).getKey().getServiceTimeSegment());
			numberOfArrivalsPerServiceSegment.put(roundingDifferences.get(0).getKey().getServiceTimeSegment(),
					--oldNumber);
			roundingDifferences.remove(0);
			tempOverallSum--;
		}

		Collections.sort(roundingDifferences, new PairDoubleValueDescComparator());
		while (tempOverallSum < numberOfArrivals) {
			int oldNumber = numberOfArrivalsPerServiceSegment
					.get(roundingDifferences.get(0).getKey().getServiceTimeSegment());
			numberOfArrivalsPerServiceSegment.put(roundingDifferences.get(0).getKey().getServiceTimeSegment(),
					++oldNumber);
			roundingDifferences.remove(0);
			tempOverallSum++;
		}

		for (ServiceTimeSegment s : numberOfArrivalsPerServiceSegment.keySet()) {
			for (int i = 0; i < numberOfArrivalsPerServiceSegment.get(s); i++) {
				segments.add(s.getId());
			}
		}
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
