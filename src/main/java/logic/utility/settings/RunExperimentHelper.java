package logic.utility.settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import data.entity.Entity;
import data.entity.Experiment;
import data.entity.GeneralParameterValue;
import data.entity.OrderRequestSet;
import data.entity.ParameterType;
import data.entity.PeriodSetting;
import data.entity.RoutingAssignment;
import data.entity.Vehicle;
import data.utility.DataServiceProvider;
import data.utility.PeriodSettingType;
import logic.process.IProcess;
import logic.utility.ProcessProvider;
import logic.utility.SettingsProvider;

public class RunExperimentHelper {

	public String experimentName;//
	public String experimentDescription;//
	public String experimentResponsible;//
	public String experimentOccasion;//
	public int regionId;//
	public int processTypeId;//
	public Integer bookingPeriodNumber;
	public Integer bookingPeriodLength;
	public Integer depotId;
	public Integer incentiveTypeId;
	public Integer arrivalProbDistributionId;
	public Integer travelTimeSetId;
	public Integer dpTreeId;
	public Integer deliveryAreaSetId;
	public Integer capSetId;
	public Integer controlSetId;
	public Integer timeWindowSetId;
	public Integer demandSegmentWeightingId;
	public Integer demandSegmentSetId;
	public Integer alternativeSetId;
	public Integer arrivalProcessId;
	public Integer customerSetId;
	public Integer serviceSegmentWeightingId;
	public Integer orderSetId;
	public Integer historicalOrderSetId;
	public ArrayList<Integer> learningOrderSetsExperimentIds;
	public Integer orderRequestSetId;
	public Integer learningRequestSetsExperimentId;
	public Integer valueBucketForecastSetId;
	public Integer valueBucketSetId;
	public Integer demandSegmentForcastSetId;
	public Integer historicalDemandForecastValueBucketsSetId;
	public Integer finalRoutingId;
	public Integer initialRoutingId;
	public ArrayList<Integer> learningRoutingsExperimentIds;
	public ArrayList<Integer> benchmarkingFinalRoutingsExperimentIds;
	public ArrayList<Integer> benchmarkingOrderSetsExperimentIds;
	public Integer noVehicles;
	public ArrayList<Integer> vehicleTypes;
	public ArrayList<Integer> vehicleTypesNos;
	public Integer vehicleAssignmentSetId;
	public Integer valueFunctionModelSetId;
	public Integer noRepetitions;
	public boolean orderRequestSetChanges;

	public void start(HashMap<String, Double> paras) {

		Experiment experiment = new Experiment();
		IProcess process;

		// Always requested
		experiment.setName(experimentName);
		experiment.setDescription(experimentDescription);
		experiment.setResponsible(experimentResponsible);
		experiment.setOccasion(experimentOccasion);
		experiment.setRegionId(regionId);
		experiment.setProcessTypeId(processTypeId);

		// Request settings depending on the process type
		process = ProcessProvider.getProcessByProcessTypeId(experiment.getProcessTypeId());
		experiment.setBookingPeriodNumber(bookingPeriodNumber);
		experiment.setBookingPeriodLength(bookingPeriodLength);
		experiment.setDepotId(depotId);

		// Choose incentive type
		experiment.setIncentiveTypeId(incentiveTypeId);

		SettingsProvider.setExperiment(experiment);

		PeriodSetting p1 = new PeriodSetting();

		p1.setStartingPeriod(0);

		// Define period settings like delivery area set, customer weighting,
		// ...
		SettingsProvider.setSettingRequest(process.getSettingRequest());
		SettingsProvider.setOutputs(process.getOutputs());
		HashMap<PeriodSettingType, Boolean> types = SettingsProvider.getSettingRequest().getPeriodSettings();

		Iterator<PeriodSettingType> typeIterator = types.keySet().iterator();
		while (typeIterator.hasNext()) {
			PeriodSettingType type = typeIterator.next();
			defineRelevantSettings(type, types.get(type), p1);
		}

		// General parameters

		setGeneralParameters(paras, p1);
		SettingsProvider.setPeriodSetting(p1);

		// TODO Only works for one booking period per experiment
		ArrayList<PeriodSetting> periodSettingFollowers = new ArrayList<PeriodSetting>();

		SettingsProvider.setPeriodSettingFollowers(periodSettingFollowers);

		SettingsProvider.setNoOfRepetitions(noRepetitions);

		if (noRepetitions > 0 && orderRequestSetChanges && orderRequestSetId != null) {
			ArrayList<Integer> requestSetIds = DataServiceProvider.getSettingsDataServiceImplInstance()
					.getOrderRequestSetsPerExperiment(SettingsProvider.getPeriodSetting().getOrderRequestSetId());

			if (requestSetIds.size() != SettingsProvider.getNoOfRepetitions()) {
				SettingsProvider.setNoOfRepetitions(requestSetIds.size());
				System.out
						.println("Adapted number of repetitions to available sets, which is: " + requestSetIds.size());
			}
			SettingsProvider.setOrderRequestSetsForRepetitions(requestSetIds);
		} else {
			SettingsProvider.setOrderRequestSetsForRepetitions(null);
		}

		// Start process
		System.out.println("Process starts now");

		int repetition = 0;
		Integer experimentId = null;
		while (repetition < SettingsProvider.getNoOfRepetitions()) {

			System.out.println("Start repetition: " + repetition);
			long startTime = System.currentTimeMillis();
			if (SettingsProvider.doesOrderRequestSetChange()) {
				System.out.println("Request set Id: " + SettingsProvider.getOrderRequestSetIdForRepetition(repetition));
				SettingsProvider.getPeriodSetting()
						.setOrderRequestSetId(SettingsProvider.getOrderRequestSetIdForRepetition(repetition));
			}

			// Reset possible routing assignments (if they are output)
			if (repetition > 0 && SettingsProvider.getOutputs().contains(PeriodSettingType.FINALROUTING) && SettingsProvider.saveResults) {
				ArrayList<RoutingAssignment> rAss = new ArrayList<RoutingAssignment>();
				for (int i = 0; i < SettingsProvider.getPeriodSetting().getRoutingAssignments().size(); i++) {
					if (SettingsProvider.getPeriodSetting().getRoutingAssignments().get(i).getT() == -1)
						rAss.add(SettingsProvider.getPeriodSetting().getRoutingAssignments().get(i));
				}
				SettingsProvider.getPeriodSetting().setRoutingAssignments(rAss);
			}

			if (repetition > 0 && SettingsProvider.getOutputs().contains(PeriodSettingType.INITIALROUTING)) {
				ArrayList<RoutingAssignment> rAss = new ArrayList<RoutingAssignment>();
				for (int i = 0; i < SettingsProvider.getPeriodSetting().getRoutingAssignments().size(); i++) {
					if (SettingsProvider.getPeriodSetting().getRoutingAssignments().get(i).getT() != -1)
						rAss.add(SettingsProvider.getPeriodSetting().getRoutingAssignments().get(i));
				}
				SettingsProvider.getPeriodSetting().setRoutingAssignments(rAss);
			}

			process.start();

			long endTime = System.currentTimeMillis();
			long totalTime = endTime - startTime;

			// Save run settings

			if (repetition > 0 && !SettingsProvider.doesOrderRequestSetChange()) {
				DataServiceProvider.getSettingsDataServiceImplInstance().persistRunSettings(
						SettingsProvider.getExperiment(), SettingsProvider.getPeriodSetting(),
						SettingsProvider.getPeriodSettingFollowers(), SettingsProvider.getSettingRequest(),
						SettingsProvider.getOutputs(), totalTime);
			} else {
				if (!(repetition > 0)) {
					experimentId = DataServiceProvider.getSettingsDataServiceImplInstance().persistExperimentSettings(
							SettingsProvider.getExperiment(), SettingsProvider.getPeriodSetting(),
							SettingsProvider.getPeriodSettingFollowers(), SettingsProvider.getSettingRequest(),
							SettingsProvider.getOutputs(), totalTime);
				} else {
					SettingsProvider.getExperiment().setCopyExperimentId(experimentId);
					DataServiceProvider.getSettingsDataServiceImplInstance().persistExperimentSettings(
							SettingsProvider.getExperiment(), SettingsProvider.getPeriodSetting(),
							SettingsProvider.getPeriodSettingFollowers(), SettingsProvider.getSettingRequest(),
							SettingsProvider.getOutputs(), totalTime);
				}

			}

			repetition++;
		}
	}

	private void setGeneralParameters(HashMap<String, Double> paras, PeriodSetting p) {

		ArrayList<Entity> paraTypes = DataServiceProvider.getParameterTypeDataServiceImplInstance().getAll();
		for (String para : paras.keySet()) {

			GeneralParameterValue paraValue = new GeneralParameterValue();
			for (int i = 0; i < paraTypes.size(); i++) {
				if (((ParameterType) paraTypes.get(i)).getName().equals(para)) {
					paraValue.setParameterType((ParameterType) paraTypes.get(i));
					paraValue.setParameterTypeId(paraValue.getParameterType().getId());
					break;
				}
			}
			paraValue.setValue(paras.get(para));
			p.addParameterValue(paraValue);

		}
	}

	private void defineRelevantSettings(PeriodSettingType type, Boolean optional, PeriodSetting p) {

		switch (type) {
		case ARRIVAL_PROBABILITY_DISTRIBUTION:
			setArrivalProbabilityDistributionId(p);
			break;
		case TRAVELTIMESET:
			setTravelTimeSetId(p);
			break;
		case DYNAMICPROGRAMMINGTREE:
			setDynamicProgrammingTreeId(p);
			break;
		case DELIVERYAREASET:
			setDeliveryAreaSetId(p);
			break;
		case ALTERNATIVESET:
			setAlternativeSetId(p);
			break;
		case CUSTOMERSET:
			setCustomerSetId(p);
			break;
		case SERVICESEGMENTWEIGHTING:
			setServiceSegmentWeightingId(p);
			break;
		case ARRIVALPROCESS:
			setArrivalProcessId(p);
			break;
		case DEMANDSEGMENTWEIGHTING:
			setDemandSegmentWeightingId(p);
			break;
		case DEMANDSEGMENTSET:
			setDemandSegmentSetId(p);
			break;
		case ORDERSET:
			setOrderSetId(p);
			break;
		case ORDERREQUESTSET:
			setOrderRequestSetId(p);
			break;
		case LEARNING_ORDERREQUESTSET:
			setOrderRequestSetsByExperimentId(p);
			break;
		case DEMANDFORECASTSET_VALUEBUCKETS:
			setDemandForecastValueBucketsSetId(p);
			break;
		case DEMANDFORECASTSET_DEMANDSEGMENTS:
			setDemandForecastSegmentsSetId(p);
			break;
		case CAPACITYSET:
			setCapacitySetId(p);
			break;
		case CONTROLSET:
			setControlSetId(p);
			break;
		case TIMEWINDOWSET:
			setTimeWindowSet(p);
			break;
		case HISTORICALORDERS:
			setHistoricalOrderSet(p);
			break;
		case HISTORICALDELIVERIES:
			System.out.println("NOT WORKING!");
			break;
		case VALUEBUCKETSET:
			setValueBucketSet(p);
			break;
		case HISTORICALDEMANDFORECASTSET_VALUEBUCKETS:
			setHistoricalDemandForecastValueBucketsSetId(p);
			break;
		case FINALROUTING:
			setFinalRoutingId(p);
			break;
		case INITIALROUTING:
			setInitialRoutingId(p);
			break;
		case LEARNING_FINAL_ROUTING:
			setRoutingsByExperimentId(p);
			break;
		case LEARNING_ORDER_SET:
			setLearningOrderSetsByExperimentId(p);
			break;
		case BENCHMARKING_FINAL_ROUTING:
			setBenchmarkingRoutingResults(p);
			break;
		case BENCHMARKING_ORDER_SET:
			setBenchmarkingOrderSetResults(p);
			break;
		case VEHICLES:
			setVehicles(p);
			break;
		case VEHICLE_ASSIGNMENT_SET:
			setVehicleAreaAssignment(p);
			break;
		case LINEAR_VALUE_FUNCTION_APPROXIMATION:
			setLinearValueFunctionApproximation(p);
			break;
		default:
			break;
		}

	}

	private void setArrivalProbabilityDistributionId(PeriodSetting p) {

		if (p.getArrivalProbabilityDistributionId() == null && arrivalProbDistributionId != null) {

			p.setArrivalProbabilityDistributionId(arrivalProbDistributionId);

		}
	}

	private void setTravelTimeSetId(PeriodSetting p) {

		if (p.getTravelTimeSetId() == null && travelTimeSetId != null) {
			p.setTravelTimeSetId(travelTimeSetId);
		}
	}

	private void setDynamicProgrammingTreeId(PeriodSetting p) {

		if (p.getDynamicProgrammingTreeId() == null && dpTreeId != null) {

			// setDeliveryAreaSetId(p);
			// setCapacitySetId(p);
			// setDemandSegmentWeightingId(p);
			// setArrivalProcessId(p);

			p.setDynamicProgrammingTreeId(dpTreeId);
		}
	}

	private void setDeliveryAreaSetId(PeriodSetting p) {

		if (p.getDeliveryAreaSetId() == null && deliveryAreaSetId != null) {

			p.setDeliveryAreaSetId(deliveryAreaSetId);

		}
	}

	private void setCapacitySetId(PeriodSetting p) {

		if (p.getCapacitySetId() == null && capSetId != null) {

			// Capacities depend on delivery area set and time window set

			// setDeliveryAreaSetId(p);
			//
			// setTimeWindowSet(p);

			p.setCapacitySetId(capSetId);
			;
		}
	}

	private void setTimeWindowSet(PeriodSetting p) {

		if (p.getTimeWindowSetId() == null && timeWindowSetId != null) {
			p.setTimeWindowSetId(timeWindowSetId);
		}
	}

	private void setDemandSegmentWeightingId(PeriodSetting p) {

		if (p.getDemandSegmentWeightingId() == null && demandSegmentWeightingId != null) {

			// The demand segment weighting depends on set

			// setDemandSegmentSetId(p);

			p.setDemandSegmentWeightingId(demandSegmentWeightingId);
		}
	}

	private void setDemandSegmentSetId(PeriodSetting p) {

		if (p.getDemandSegmentSetId() == null && demandSegmentSetId != null) {

			// The demand segment set depends on the alternative set
			// setAlternativeSetId(p);

			p.setDemandSegmentSetId(demandSegmentSetId);
		}
	}

	private void setAlternativeSetId(PeriodSetting p) {

		if (p.getAlternativeSetId() == null && alternativeSetId != null) {

			// Define time window set
			// setTimeWindowSet(p);

			p.setAlternativeSetId(alternativeSetId);
		}
	}

	private void setArrivalProcessId(PeriodSetting p) {

		if (p.getArrivalProcessId() == null && arrivalProcessId != null) {

			p.setArrivalProcessId(arrivalProcessId);
		}
	}

	private void setCustomerSetId(PeriodSetting p) {

		if (p.getCustomerSetId() == null && customerSetId != null) {

			// Customers depend on demand segment set

			// setDemandSegmentWeightingId(p);

			p.setCustomerSetId(customerSetId);
			;
		}
	}

	private void setServiceSegmentWeightingId(PeriodSetting p) {

		if (p.getServiceSegmentWeightingId() == null && serviceSegmentWeightingId != null) {

			p.setServiceSegmentWeightingId(serviceSegmentWeightingId);
		}
	}

	private void setOrderSetId(PeriodSetting p) {

		if (p.getOrderSetId() == null && orderSetId != null) {

			// Orders depend on order request set
			// setOrderRequestSetId(p);
			p.setOrderSetId(orderSetId);
			;
		}
	}

	private void setOrderRequestSetId(PeriodSetting p) {

		if (p.getOrderRequestSetId() == null && orderRequestSetId != null) {

			// Has to fit to alternatives
			// setAlternativeSetId(p);

			p.setOrderRequestSetId(orderRequestSetId);

			// Set fitting customer set
			if (p.getCustomerSetId() == null) {
				OrderRequestSet chosenSet = (OrderRequestSet) DataServiceProvider
						.getOrderRequestDataServiceImplInstance().getSetById(p.getOrderRequestSetId());
				p.setCustomerSetId(chosenSet.getCustomerSetId());
			}
			;
		}
	}

	private void setOrderRequestSetsByExperimentId(PeriodSetting p) {

		if (p.getLearningOutputRequestsExperimentId() == null && learningRequestSetsExperimentId != null) {

			// Has to fit to demand structure
			// setDemandSegmentSetId(p);

			p.setLearningOutputRequestsExperimentId(learningRequestSetsExperimentId);

		}
	}

	private void setDemandForecastValueBucketsSetId(PeriodSetting p) {

		if (p.getValueBucketForecastSetId() == null && valueBucketForecastSetId != null) {

			// Demand forecasts depend on delivery area set and time window set
			// and value bucket set

			// setDeliveryAreaSetId(p);
			//
			// setTimeWindowSet(p);
			//
			// setValueBucketSet(p);

			p.setValueBucketForecastSetId(valueBucketForecastSetId);
			;
		}
	}

	private void setValueBucketSet(PeriodSetting p) {

		if (p.getValueBucketSetId() == null && valueBucketSetId != null) {

			p.setValueBucketSetId(valueBucketSetId);
			;
		}
	}

	private void setDemandForecastSegmentsSetId(PeriodSetting p) {

		if (p.getDemandSegmentForecastSetId() == null && demandSegmentForcastSetId != null) {

			// Demand forecasts depend on delivery area set and demand segment
			// set

			// setDeliveryAreaSetId(p);
			//
			// setDemandSegmentSetId(p);

			p.setDemandSegmentForecastSetId(demandSegmentForcastSetId);
			;
		}
	}

	private void setControlSetId(PeriodSetting p) {

		if (p.getControlSetId() == null && controlSetId != null) {

			// Controls depend on delivery area set and time window set and
			// value bucket set

			// setDeliveryAreaSetId(p);
			//
			// setAlternativeSetId(p);
			//
			// setValueBucketSet(p);

			p.setControlSetId(controlSetId);
			;
		}
	}

	private void setHistoricalOrderSet(PeriodSetting p) {

		if (p.getHistoricalOrderSetId() == null && historicalOrderSetId != null) {

			p.setHistoricalOrderSetId(historicalOrderSetId);
			;
		}

	}

	private void setHistoricalDemandForecastValueBucketsSetId(PeriodSetting p) {

		if (p.getValueBucketForecastSetId() == null && historicalDemandForecastValueBucketsSetId != null) {

			// Demand forecasts depend on delivery area set and time window set
			// and value bucket set

			// setDeliveryAreaSetId(p);
			//
			// setTimeWindowSet(p);
			//
			// setValueBucketSet(p);

			p.setHistoricalDemandForecastValueBucketsSetId(historicalDemandForecastValueBucketsSetId);
			;
		}
	}

	private void setFinalRoutingId(PeriodSetting p) {

		if (finalRoutingId != null) {
			RoutingAssignment rAss = null;
			for (int i = 0; i < p.getRoutingAssignments().size(); i++) {
				if (p.getRoutingAssignments().get(i).getT() == -2)
					rAss = p.getRoutingAssignments().get(i);
			}

			if (rAss == null) {

				// Depends on order set and on depot location
				setOrderSetId(p);
				rAss = new RoutingAssignment();
				rAss.setRoutingId(finalRoutingId);
				rAss.setT(-2);
				;
			}

		}
	}

	private void setInitialRoutingId(PeriodSetting p) {

		if (initialRoutingId != null) {
			RoutingAssignment rAss = null;
			for (int i = 0; i < p.getRoutingAssignments().size(); i++) {
				if (p.getRoutingAssignments().get(i).getT() == -1)
					rAss = p.getRoutingAssignments().get(i);
			}

			if (rAss == null) {

				setTimeWindowSet(p);
				rAss = new RoutingAssignment();
				rAss.setRoutingId(initialRoutingId);
				rAss.setT(-1);
				;
			}
		}
	}

	private void setRoutingsByExperimentId(PeriodSetting p) {

		if (p.getLearningOutputFinalRoutingsExperimentIds() == null && learningRoutingsExperimentIds != null) {

			// Has to fit to demand segment set
			// setDemandSegmentSetId(p);
			ArrayList<Integer> experimentIds = new ArrayList<Integer>();
			for (Integer expId : learningRoutingsExperimentIds) {
				experimentIds.add(expId);
				experimentIds.addAll(DataServiceProvider.getSettingsDataServiceImplInstance()
						.getAllCopyExperimentsPerExperiment(expId));
			}

			p.setLearningOutputFinalRoutingsExperimentIds(experimentIds);

		}
	}

	private void setLearningOrderSetsByExperimentId(PeriodSetting p) {

		if (p.getLearningOutputOrderSetsExperimentIds() == null && learningOrderSetsExperimentIds != null) {

			// Has to fit to demand segment set
			// setDemandSegmentSetId(p);

			ArrayList<Integer> experimentIds = new ArrayList<Integer>();
			for (Integer expId : learningOrderSetsExperimentIds) {
				experimentIds.add(expId);
				experimentIds.addAll(DataServiceProvider.getSettingsDataServiceImplInstance()
						.getAllCopyExperimentsPerExperiment(expId));
			}

			p.setLearningOutputOrderSetsExperimentIds(experimentIds);

		}
	}

	private void setBenchmarkingRoutingResults(PeriodSetting p) {

		if (p.getBenchmarkingOutputFinalRoutingsExperimentIds() == null
				&& benchmarkingFinalRoutingsExperimentIds != null) {

			// Has to fit to demand segment set
			// setDemandSegmentSetId(p);

			ArrayList<Integer> experimentIds = new ArrayList<Integer>();
			for (Integer expId : benchmarkingFinalRoutingsExperimentIds) {
				experimentIds.add(expId);
				experimentIds.addAll(DataServiceProvider.getSettingsDataServiceImplInstance()
						.getAllCopyExperimentsPerExperiment(expId));
			}

			p.setBenchmarkingOutputFinalRoutingsExperimentIds(experimentIds);

		}
	}

	private void setBenchmarkingOrderSetResults(PeriodSetting p) {

		if (p.getBenchmarkingOutputOrderSetsExperimentIds() == null && benchmarkingOrderSetsExperimentIds != null) {

			// Has to fit to demand segment set
			// setDemandSegmentSetId(p);

			ArrayList<Integer> experimentIds = new ArrayList<Integer>();
			for (Integer expId : benchmarkingOrderSetsExperimentIds) {
				experimentIds.add(expId);
				experimentIds.addAll(DataServiceProvider.getSettingsDataServiceImplInstance()
						.getAllCopyExperimentsPerExperiment(expId));
			}

			p.setBenchmarkingOutputOrderSetsExperimentIds(experimentIds);

		}
	}

	private void setVehicles(PeriodSetting p) {

		if (noVehicles != null) {
			int noVehiclesH = noVehicles;

			int currentType = 0;
			while (noVehiclesH > 0) {
				Vehicle vehicle = new Vehicle();
				int typeId = vehicleTypes.get(currentType);

				// vehicles in sum = noVehicles
				vehicle.setVehicleTypeId(typeId);

				int no = vehicleTypesNos.get(currentType);
				vehicle.setVehicleNo(no);
				p.addVehicle(vehicle);
				noVehiclesH = noVehiclesH - no;
				currentType++;

			}
		}
	}

	private void setVehicleAreaAssignment(PeriodSetting p) {

		if (p.getVehicleAssignmentSetId() == null && vehicleAssignmentSetId != null) {

			// Capacities depend on delivery area set and time window set

			// setDeliveryAreaSetId(p);

			p.setVehicleAssignmentSetId(vehicleAssignmentSetId);
			;
		}
	}

	private void setLinearValueFunctionApproximation(PeriodSetting p) {

		if (p.getValueFunctionModelSetId() == null && valueFunctionModelSetId != null) {

			// setDeliveryAreaSetId(p);
			//
			// setTimeWindowSet(p);

			p.setValueFunctionModelSetId(valueFunctionModelSetId);
			;
		}
	}

	public static String runDataGeneration(String basicName, int regionId, int bookingPeriodLength,
			int arrivalProcess, int arrivalProbabilityDistributionId, int demandSegmentWeightingId,
			int serviceSegmentWeightingId) {
		RunExperimentHelper helper;
		HashMap<String, Double> paras;
		String resultLog = "Result: ";

		// Validation data
		helper = new RunExperimentHelper();
		helper.experimentName = basicName + "DG_Validation";
		helper.experimentDescription = " ";
		helper.experimentOccasion = " ";
		helper.experimentResponsible = " ";
		helper.noRepetitions = 100;
		helper.regionId = regionId;
		helper.processTypeId = 1;
		helper.bookingPeriodNumber = 1;
		helper.bookingPeriodLength = bookingPeriodLength;
		helper.arrivalProcessId = arrivalProcess;
		helper.demandSegmentWeightingId = demandSegmentWeightingId;
		helper.serviceSegmentWeightingId = serviceSegmentWeightingId;

		paras = new HashMap<String, Double>();
		paras.put("samplePreferences", 1.0);
		helper.start(paras);
		resultLog += "(" + helper.experimentName + ":" + SettingsProvider.getExperiment().getId() + ",os_"
				+ (SettingsProvider.getPeriodSetting().getOrderRequestSetId() - 100 + 1) + ")";

		// TOP data
//		helper = new RunExperimentHelper();
//		helper.experimentName = basicName + "DG_TOP";
//		helper.experimentDescription = " ";
//		helper.experimentOccasion = " ";
//		helper.experimentResponsible = " ";
//		helper.noRepetitions = 100;
//		helper.regionId = regionId;
//		helper.processTypeId = 23;
//		helper.bookingPeriodNumber = 1;
//		helper.bookingPeriodLength = bookingPeriodLength;
//		helper.arrivalProbDistributionId = arrivalProbabilityDistributionId;
//		helper.demandSegmentWeightingId = demandSegmentWeightingId;
//		helper.serviceSegmentWeightingId = serviceSegmentWeightingId;
//
//		paras = new HashMap<String, Double>();
//		paras.put("samplePreferences", 1.0);
//		paras.put("exactAssignments", 0.0);
//		helper.start(paras);
//		resultLog += "(" + helper.experimentName + ":" + SettingsProvider.getExperiment().getId() + ",os_"
//				+ (SettingsProvider.getPeriodSetting().getOrderRequestSetId() - 100 + 1) + ")";
	
		// TOP data expected
				helper = new RunExperimentHelper();
				helper.experimentName = basicName + "DG_TOP_expected";
				helper.experimentDescription = " ";
				helper.experimentOccasion = " ";
				helper.experimentResponsible = " ";
				helper.noRepetitions = 100;
				helper.regionId = regionId;
				helper.processTypeId = 23;
				helper.bookingPeriodNumber = 1;
				helper.bookingPeriodLength = bookingPeriodLength;
				helper.arrivalProbDistributionId = arrivalProbabilityDistributionId;
				helper.demandSegmentWeightingId = demandSegmentWeightingId;
				helper.serviceSegmentWeightingId = serviceSegmentWeightingId;

				paras = new HashMap<String, Double>();
				paras.put("samplePreferences", 1.0);
				paras.put("exactAssignments", 1.0);
				helper.start(paras);
				resultLog += "(" + helper.experimentName + ":" + SettingsProvider.getExperiment().getId() + ",os_"
						+ (SettingsProvider.getPeriodSetting().getOrderRequestSetId() - 100 + 1) + ")";

		// Training data
		helper = new RunExperimentHelper();
		helper.experimentName = basicName + "DG_Training";
		helper.experimentDescription = " ";
		helper.experimentOccasion = " ";
		helper.experimentResponsible = " ";
		helper.noRepetitions = 5000;
		helper.regionId = regionId;
		helper.processTypeId = 1;
		helper.bookingPeriodNumber = 1;
		helper.bookingPeriodLength = bookingPeriodLength;
		helper.arrivalProcessId = arrivalProcess;
		helper.demandSegmentWeightingId = demandSegmentWeightingId;
		helper.serviceSegmentWeightingId = serviceSegmentWeightingId;

		paras = new HashMap<String, Double>();
		paras.put("samplePreferences", 1.0);
		helper.start(paras);
		resultLog += "(" + helper.experimentName + ":" + SettingsProvider.getExperiment().getId() + ")";

		return resultLog;
	}

	public static String runTOP(String basicName, int bookingPeriodLength, int regionId, int deliveryAreaSetId,
			int vehicleAssignmentSetId, int demandSegmentWeightingId, int timeWindowSetId,
			int firstOrderRequestSetIdForTOP, int arrivalProcess, boolean expected) {

		RunExperimentHelper helper;
		HashMap<String, Double> paras;
		String resultLog = "Result: ";

		helper = new RunExperimentHelper();
		helper.experimentName = basicName + "TOP";
		if(expected) helper.experimentName +="_expected";
		helper.experimentDescription = " ";
		helper.experimentOccasion = " ";
		helper.experimentResponsible = " ";
		helper.noRepetitions = 100;
		helper.regionId = regionId;
		helper.processTypeId = 34;
		helper.bookingPeriodNumber = 1;
		helper.bookingPeriodLength = bookingPeriodLength;
		helper.orderRequestSetId = firstOrderRequestSetIdForTOP;
		helper.deliveryAreaSetId = deliveryAreaSetId;
		helper.vehicleAssignmentSetId = vehicleAssignmentSetId;
		helper.timeWindowSetId = timeWindowSetId;
		helper.arrivalProcessId = arrivalProcess;
		helper.demandSegmentWeightingId = demandSegmentWeightingId;
		helper.orderRequestSetChanges = true;

		paras = new HashMap<String, Double>();
		paras.put("Constant_service_time", 12.0);
		paras.put("includeDriveFromStartingPosition", 0.0);
		paras.put("greediness_upperBound", 1.0);
		paras.put("greediness_lowerBound", 0.99);
		paras.put("greediness_stepsize", 0.1);
		paras.put("maximumRoundsWithoutImprovement", 60.0);
		paras.put("maximumNumberOfSolutions", 1000.0);
		paras.put("squaredValue", 1.0);
		paras.put("actualBasketValue", 0.0);
		paras.put("directDistances", 1.0);
		paras.put("duplicate_segments", 0.0);
		paras.put("weight_time_window_prob", 0.0);
		paras.put("weight_arrival_prob", 0.0);
		paras.put("imp_for_insertion", 0.0);
		helper.start(paras);

		int firstExpId = SettingsProvider.getExperiment().getId() - 100 + 1;
		resultLog += "(" + helper.experimentName + ": " + firstExpId + ") ";
		return resultLog;

	}
	
	public static String pruningAnn(String basicName, int bookingPeriodLength, int regionId, int depotId, 
			int deliveryAreaSetId, int vehicleAssignmentSetId, int noVehicles, int demandSegmentWeightingId, int timeWindowSetId,
			int firstOrderRequestSetIdForValiation, int learningRequestSetsExperimentId,
			ArrayList<Integer> learningRoutingExperimentIds, ArrayList<Integer> learningRoutingExperimentIdsExpected, int arrivalProcess) {
		String adpName = "ADP_ann";
		RunExperimentHelper helper;
		HashMap<String, Double> paras;
		String resultLog = "Result: ";
		
		 helper = new RunExperimentHelper();
		 paras = new HashMap<String, Double>();
		 // Define settings
		 helper.experimentName = basicName + adpName + "_n0";
		 helper.experimentDescription = " ";
		 helper.experimentOccasion = " ";
		 helper.experimentResponsible = "ML";
		 helper.noRepetitions = 1;
		 helper.regionId = regionId;
		 helper.processTypeId = 31;
		 helper.bookingPeriodNumber = 1;
		 helper.bookingPeriodLength = bookingPeriodLength;
		 helper.learningRequestSetsExperimentId =learningRequestSetsExperimentId;
		 helper.demandSegmentWeightingId = demandSegmentWeightingId;
		 helper.deliveryAreaSetId = deliveryAreaSetId;
		 helper.vehicleAssignmentSetId = vehicleAssignmentSetId;
		 helper.learningRoutingsExperimentIds =learningRoutingExperimentIdsExpected;
		 helper.arrivalProcessId = arrivalProcess;
		//
		 paras.put("Constant_service_time", 12.0);
		 paras.put("actualBasketValue", 1.0);
		 paras.put("samplePreferences", 1.0);
		 paras.put("includeDriveFromStartingPosition", 0.0);
		 paras.put("exploration_(0:on-policy,1:conservative-factor,2:e-greedy)",
		 2.0);
		 paras.put("stepsize_adp_learning", 0.0001);
		 paras.put("annealing_temperature_(Negative:no_annealing)", 4000.0);
		 paras.put("momentum_weight", 0.9);
		 paras.put("theft-based", 1.0);
		 paras.put("theft-based-advanced", 1.0);
		 paras.put("theft-based-tw", 1.0);
		 paras.put("consider_left_over_penalty", 0.0);
		 paras.put("discounting_factor", 1.0);
		 paras.put("discounting_factor_probability", 1.0);
		 paras.put("consider_constant", 1.0);
		 paras.put("additional_hidden_nodes", 0.0);
		 paras.put("consider_demand_neighbors", 0.0);
		 paras.put("oc_for_feasible", 0.0);
		 paras.put("hTan_activation", 1.0);
		 helper.start(paras);

		// Can do this for more experiments...
		 helper = new RunExperimentHelper();
		// // Define settings
		 helper.experimentName = basicName + adpName+"_n0_acc";
		 helper.experimentDescription = " ";
		 helper.experimentOccasion = " ";
		 helper.experimentResponsible = " ";
		 helper.noRepetitions = 100;
		 helper.regionId = regionId;
		 helper.processTypeId = 39;
		 helper.bookingPeriodNumber = 1;
		 helper.bookingPeriodLength = bookingPeriodLength;
		 helper.valueFunctionModelSetId = SettingsProvider.getPeriodSetting().getValueFunctionModelSetId();
		 helper.orderRequestSetId = firstOrderRequestSetIdForValiation;
		 helper.orderRequestSetChanges = true;
		 helper.demandSegmentWeightingId = demandSegmentWeightingId;
		 helper.vehicleAssignmentSetId = vehicleAssignmentSetId;
		 helper.deliveryAreaSetId = deliveryAreaSetId;
		 helper.learningRoutingsExperimentIds =learningRoutingExperimentIdsExpected;
		 helper.arrivalProcessId = arrivalProcess;
		 helper.noVehicles=noVehicles;
		 helper.vehicleTypes = new ArrayList<Integer>();
		 helper.vehicleTypes.add(1);
		 helper.vehicleTypesNos = new ArrayList<Integer>();
		 helper.vehicleTypesNos.add(noVehicles);
		 helper.depotId=depotId;
		
		 paras = new HashMap<String, Double>();
		 paras.put("actualBasketValue", 1.0);
		 paras.put("Constant_service_time", 12.0);
		 paras.put("samplePreferences", 1.0);
		 paras.put("theft-based", 1.0);
		 paras.put("theft-based-advanced", 1.0);
		 paras.put("theft-based-tw", 1.0);
		 paras.put("consider_left_over_penalty", 0.0);
		 paras.put("discounting_factor_probability", 1.0);
		 paras.put("oc_for_feasible", 0.0);
		 paras.put("stop_once_feasible", 1.0);
		
		 // Run experiment
		 helper.start(paras);
		 int firstExpId = SettingsProvider.getExperiment().getId() - 100 + 1;
		 resultLog += "(" + helper.experimentName + ": " + firstExpId + ") ";
		 
		 helper = new RunExperimentHelper();
		 paras = new HashMap<String, Double>();
		 // Define settings
		 helper.experimentName = basicName + adpName + "_n1";
		 helper.experimentDescription = " ";
		 helper.experimentOccasion = " ";
		 helper.experimentResponsible = "ML";
		 helper.noRepetitions = 1;
		 helper.regionId = regionId;
		 helper.processTypeId = 31;
		 helper.bookingPeriodNumber = 1;
		 helper.bookingPeriodLength = bookingPeriodLength;
		 helper.learningRequestSetsExperimentId =learningRequestSetsExperimentId;
		 helper.demandSegmentWeightingId = demandSegmentWeightingId;
		 helper.deliveryAreaSetId = deliveryAreaSetId;
		 helper.vehicleAssignmentSetId = vehicleAssignmentSetId;
		 helper.learningRoutingsExperimentIds =learningRoutingExperimentIdsExpected;
		 helper.arrivalProcessId = arrivalProcess;
		//
		 paras.put("Constant_service_time", 12.0);
		 paras.put("actualBasketValue", 1.0);
		 paras.put("samplePreferences", 1.0);
		 paras.put("includeDriveFromStartingPosition", 0.0);
		 paras.put("exploration_(0:on-policy,1:conservative-factor,2:e-greedy)",
		 2.0);
		 paras.put("stepsize_adp_learning", 0.0001);
		 paras.put("annealing_temperature_(Negative:no_annealing)", 4000.0);
		 paras.put("momentum_weight", 0.9);
		 paras.put("theft-based", 1.0);
		 paras.put("theft-based-advanced", 1.0);
		 paras.put("theft-based-tw", 1.0);
		 paras.put("consider_left_over_penalty", 0.0);
		 paras.put("discounting_factor", 1.0);
		 paras.put("discounting_factor_probability", 1.0);
		 paras.put("consider_constant", 1.0);
		 paras.put("additional_hidden_nodes", 1.0);
		 paras.put("consider_demand_neighbors", 0.0);
		 paras.put("oc_for_feasible", 0.0);
		 paras.put("hTan_activation", 1.0);
		 helper.start(paras);

		// Can do this for more experiments...
		 helper = new RunExperimentHelper();
		// // Define settings
		 helper.experimentName = basicName + adpName+"_n1_acc";
		 helper.experimentDescription = " ";
		 helper.experimentOccasion = " ";
		 helper.experimentResponsible = " ";
		 helper.noRepetitions = 100;
		 helper.regionId = regionId;
		 helper.processTypeId = 39;
		 helper.bookingPeriodNumber = 1;
		 helper.bookingPeriodLength = bookingPeriodLength;
		 helper.valueFunctionModelSetId = SettingsProvider.getPeriodSetting().getValueFunctionModelSetId();
		 helper.orderRequestSetId = firstOrderRequestSetIdForValiation;
		 helper.orderRequestSetChanges = true;
		 helper.demandSegmentWeightingId = demandSegmentWeightingId;
		 helper.vehicleAssignmentSetId = vehicleAssignmentSetId;
		 helper.deliveryAreaSetId = deliveryAreaSetId;
		 helper.learningRoutingsExperimentIds =learningRoutingExperimentIdsExpected;
		 helper.arrivalProcessId = arrivalProcess;
		 helper.noVehicles=noVehicles;
		 helper.vehicleTypes = new ArrayList<Integer>();
		 helper.vehicleTypes.add(1);
		 helper.vehicleTypesNos = new ArrayList<Integer>();
		 helper.vehicleTypesNos.add(noVehicles);
		 helper.depotId=depotId;
		
		 paras = new HashMap<String, Double>();
		 paras.put("actualBasketValue", 1.0);
		 paras.put("Constant_service_time", 12.0);
		 paras.put("samplePreferences", 1.0);
		 paras.put("theft-based", 1.0);
		 paras.put("theft-based-advanced", 1.0);
		 paras.put("theft-based-tw", 1.0);
		 paras.put("consider_left_over_penalty", 0.0);
		 paras.put("discounting_factor_probability", 1.0);
		 paras.put("oc_for_feasible", 0.0);
		 paras.put("stop_once_feasible", 1.0);
		
		 // Run experiment
		 helper.start(paras);
		 firstExpId = SettingsProvider.getExperiment().getId() - 100 + 1;
		 resultLog += "(" + helper.experimentName + ": " + firstExpId + ") ";
		 
		 helper = new RunExperimentHelper();
		 paras = new HashMap<String, Double>();
		 // Define settings
		 helper.experimentName = basicName + adpName + "_n2";
		 helper.experimentDescription = " ";
		 helper.experimentOccasion = " ";
		 helper.experimentResponsible = "ML";
		 helper.noRepetitions = 1;
		 helper.regionId = regionId;
		 helper.processTypeId = 31;
		 helper.bookingPeriodNumber = 1;
		 helper.bookingPeriodLength = bookingPeriodLength;
		 helper.learningRequestSetsExperimentId =learningRequestSetsExperimentId;
		 helper.demandSegmentWeightingId = demandSegmentWeightingId;
		 helper.deliveryAreaSetId = deliveryAreaSetId;
		 helper.vehicleAssignmentSetId = vehicleAssignmentSetId;
		 helper.learningRoutingsExperimentIds =learningRoutingExperimentIdsExpected;
		 helper.arrivalProcessId = arrivalProcess;
		//
		 paras.put("Constant_service_time", 12.0);
		 paras.put("actualBasketValue", 1.0);
		 paras.put("samplePreferences", 1.0);
		 paras.put("includeDriveFromStartingPosition", 0.0);
		 paras.put("exploration_(0:on-policy,1:conservative-factor,2:e-greedy)",
		 2.0);
		 paras.put("stepsize_adp_learning", 0.0001);
		 paras.put("annealing_temperature_(Negative:no_annealing)", 4000.0);
		 paras.put("momentum_weight", 0.9);
		 paras.put("theft-based", 1.0);
		 paras.put("theft-based-advanced", 1.0);
		 paras.put("theft-based-tw", 1.0);
		 paras.put("consider_left_over_penalty", 0.0);
		 paras.put("discounting_factor", 1.0);
		 paras.put("discounting_factor_probability", 1.0);
		 paras.put("consider_constant", 1.0);
		 paras.put("additional_hidden_nodes", 2.0);
		 paras.put("consider_demand_neighbors", 0.0);
		 paras.put("oc_for_feasible", 0.0);
		 paras.put("hTan_activation", 1.0);
		 helper.start(paras);

		// Can do this for more experiments...
		 helper = new RunExperimentHelper();
		// // Define settings
		 helper.experimentName = basicName + adpName+"_n2_acc";
		 helper.experimentDescription = " ";
		 helper.experimentOccasion = " ";
		 helper.experimentResponsible = " ";
		 helper.noRepetitions = 100;
		 helper.regionId = regionId;
		 helper.processTypeId = 39;
		 helper.bookingPeriodNumber = 1;
		 helper.bookingPeriodLength = bookingPeriodLength;
		 helper.valueFunctionModelSetId = SettingsProvider.getPeriodSetting().getValueFunctionModelSetId();
		 helper.orderRequestSetId = firstOrderRequestSetIdForValiation;
		 helper.orderRequestSetChanges = true;
		 helper.demandSegmentWeightingId = demandSegmentWeightingId;
		 helper.vehicleAssignmentSetId = vehicleAssignmentSetId;
		 helper.deliveryAreaSetId = deliveryAreaSetId;
		 helper.learningRoutingsExperimentIds =learningRoutingExperimentIdsExpected;
		 helper.arrivalProcessId = arrivalProcess;
		 helper.noVehicles=noVehicles;
		 helper.vehicleTypes = new ArrayList<Integer>();
		 helper.vehicleTypes.add(1);
		 helper.vehicleTypesNos = new ArrayList<Integer>();
		 helper.vehicleTypesNos.add(noVehicles);
		 helper.depotId=depotId;
		
		 paras = new HashMap<String, Double>();
		 paras.put("actualBasketValue", 1.0);
		 paras.put("Constant_service_time", 12.0);
		 paras.put("samplePreferences", 1.0);
		 paras.put("theft-based", 1.0);
		 paras.put("theft-based-advanced", 1.0);
		 paras.put("theft-based-tw", 1.0);
		 paras.put("consider_left_over_penalty", 0.0);
		 paras.put("discounting_factor_probability", 1.0);
		 paras.put("oc_for_feasible", 0.0);
		 paras.put("stop_once_feasible", 1.0);
		
		 // Run experiment
		 helper.start(paras);
		  firstExpId = SettingsProvider.getExperiment().getId() - 100 + 1;
		 resultLog += "(" + helper.experimentName + ": " + firstExpId + ") ";
		 
		return resultLog;
	}

	public static String runDynamicADP(String basicName, int bookingPeriodLength, int regionId, int depotId, 
			int deliveryAreaSetId, int vehicleAssignmentSetId, int noVehicles, int demandSegmentWeightingId, int timeWindowSetId,
			int firstOrderRequestSetIdForValiation, int learningRequestSetsExperimentId,
			ArrayList<Integer> learningRoutingExperimentIds, 
			ArrayList<Integer> learningRoutingExperimentIdsExpected, int arrivalProcess, String benchmark, int valueFunctionModelSetId) {

		String adpName = "ADP_";
		RunExperimentHelper helper;
		HashMap<String, Double> paras;
		String resultLog = "Result: ";
		
		HashMap<String, double[]> settings = new HashMap<String, double[]>();
		// Order: ic, ec, tc, distanceType, distanceMeasurePerTw,
		// maximumDistanceIncrease, switchTime
		settings.put("#", new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 });
		settings.put("#Tc", new double[] { 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0 });
		settings.put("#Ic", new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 });
		settings.put("#IcTc", new double[] { 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0 });
		settings.put("#Ec", new double[] { 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0 });
		settings.put("#EcTc", new double[] { 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0 });
		settings.put("#Eca", new double[] { 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0 });
		settings.put("#EcaTc", new double[] { 0.0, 2.0, 1.0, 0.0, 0.0, 0.0, 0.0 });

		// Dist 0
		settings.put("#Tc_d0", new double[] { 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0 });
		settings.put("#Tc_d0Type2", new double[] { 0.0, 0.0, 1.0, 2.0, 0.0, 0.0, 0.0 });
		settings.put("#Tc_d0Type2PerTw", new double[] { 0.0, 0.0, 1.0, 2.0, 1.0, 0.0, 0.0 });
		settings.put("#IcTc_d0", new double[] { 1.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0 });
		settings.put("#IcTc_d0Type2", new double[] { 1.0, 0.0, 1.0, 2.0, 0.0, 0.0, 0.0 });
		settings.put("#IcTc_d0Type2PerTw", new double[] { 1.0, 0.0, 1.0, 2.0, 1.0, 0.0, 0.0 });
		settings.put("#EcaTc_d0", new double[] { 0.0, 2.0, 1.0, 1.0, 0.0, 0.0, 0.0 });
		settings.put("#EcaTc_d0Type2", new double[] { 0.0, 2.0, 1.0, 2.0, 0.0, 0.0, 0.0 });
		settings.put("#EcaTc_d0Type2PerTw", new double[] { 0.0, 2.0, 1.0, 2.0, 1.0, 0.0, 0.0 });

		settings.put("#Tc_d0_s", new double[] { 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, bookingPeriodLength / 2.0 });
		settings.put("#Tc_d0Type2_s", new double[] { 0.0, 0.0, 1.0, 2.0, 0.0, 0.0, bookingPeriodLength / 2.0 });
		settings.put("#Tc_d0Type2PerTw_s", new double[] { 0.0, 0.0, 1.0, 2.0, 1.0, 0.0, bookingPeriodLength / 2.0 });
		settings.put("#IcTc_d0_s", new double[] { 1.0, 0.0, 1.0, 1.0, 0.0, 0.0, bookingPeriodLength / 2.0 });
		settings.put("#IcTc_d0Type2_s", new double[] { 1.0, 0.0, 1.0, 2.0, 0.0, 0.0, bookingPeriodLength / 2.0 });
		settings.put("#IcTc_d0Type2PerTw_s", new double[] { 1.0, 0.0, 1.0, 2.0, 1.0, 0.0, bookingPeriodLength / 2.0 });
		settings.put("#EcaTc_d0_s", new double[] { 0.0, 2.0, 1.0, 1.0, 0.0, 0.0, bookingPeriodLength / 2.0 });
		settings.put("#EcaTc_d0Type2_s", new double[] { 0.0, 2.0, 1.0, 2.0, 0.0, 0.0, bookingPeriodLength / 2.0 });
		settings.put("#EcaTc_d0Type2PerTw_s", new double[] { 0.0, 2.0, 1.0, 2.0, 1.0, 0.0, bookingPeriodLength / 2.0 });

		// Dist 3
		settings.put("#Tc_d3Type3", new double[] { 0.0, 0.0, 1.0, 3.0, 0.0, 3.0, 0.0 });
		settings.put("#Tc_d3Type3PerTw", new double[] { 0.0, 0.0, 1.0, 3.0, 1.0, 3.0, 0.0 });
		settings.put("#IcTc_d3Type3", new double[] { 1.0, 0.0, 1.0, 3.0, 0.0, 3.0, 0.0 });
		settings.put("#IcTc_d3Type3PerTw", new double[] { 1.0, 0.0, 1.0, 3.0, 1.0, 3.0, 0.0 });
		settings.put("#EcaTc_d3Type3", new double[] { 0.0, 2.0, 1.0, 3.0, 0.0, 3.0, 0.0 });
		settings.put("#EcaTc_d3Type3PerTw", new double[] { 0.0, 2.0, 1.0, 3.0, 1.0, 3.0, 0.0 });

		settings.put("#Tc_d3Type3_s", new double[] { 0.0, 0.0, 1.0, 3.0, 0.0, 3.0, bookingPeriodLength / 2.0 });
		settings.put("#Tc_d3Type3PerTw_s", new double[] { 0.0, 0.0, 1.0, 3.0, 1.0, 3.0, bookingPeriodLength / 2.0 });
		settings.put("#IcTc_d3Type3_s", new double[] { 1.0, 0.0, 1.0, 3.0, 0.0, 3.0, bookingPeriodLength / 2.0 });
		settings.put("#IcTc_d3Type3PerTw_s", new double[] { 1.0, 0.0, 1.0, 3.0, 1.0, 3.0, bookingPeriodLength / 2.0 });
		settings.put("#EcaTc_d3Type3_s", new double[] { 0.0, 2.0, 1.0, 3.0, 0.0, 3.0, bookingPeriodLength / 2.0 });
		settings.put("#EcaTc_d3Type3PerTw_s", new double[] { 0.0, 2.0, 1.0, 3.0, 1.0, 3.0, bookingPeriodLength / 2.0 });

		settings.put("#Tc_d3Type3_s1/3", new double[] { 0.0, 0.0, 1.0, 3.0, 0.0, 3.0, bookingPeriodLength / 3.0 });
		settings.put("#Tc_d3Type3PerTw_s1/3", new double[] { 0.0, 0.0, 1.0, 3.0, 1.0, 3.0, bookingPeriodLength / 3.0 });
		settings.put("#IcTc_d3Type3_s1/3", new double[] { 1.0, 0.0, 1.0, 3.0, 0.0, 3.0, bookingPeriodLength / 3.0 });
		settings.put("#IcTc_d3Type3PerTw_s1/3",
				new double[] { 1.0, 0.0, 1.0, 3.0, 1.0, 3.0, bookingPeriodLength / 3.0 });
		settings.put("#EcaTc_d3Type3_s1/3", new double[] { 0.0, 2.0, 1.0, 3.0, 0.0, 3.0, bookingPeriodLength / 3.0 });
		settings.put("#EcaTc_d3Type3PerTw_s1/3",
				new double[] { 0.0, 2.0, 1.0, 3.0, 1.0, 3.0, bookingPeriodLength / 3.0 });
		
		//Dist 4
		settings.put("#Tc_dType4", new double[] { 0.0, 0.0, 1.0, 4.0, 0.0, 3.0, 0.0 });
		settings.put("#Tc_dType4PerTw", new double[] { 0.0, 0.0, 1.0, 4.0, 1.0, 3.0, 0.0 });
		settings.put("#IcTc_dType4", new double[] { 1.0, 0.0, 1.0, 4.0, 0.0, 3.0, 0.0 });
		settings.put("#IcTc_dType4PerTw", new double[] { 1.0, 0.0, 1.0, 4.0, 1.0, 3.0, 0.0 });
		settings.put("#EcaTc_dType4", new double[] { 0.0, 2.0, 1.0, 4.0, 0.0, 3.0, 0.0 });
		settings.put("#EcaTc_dType4PerTw", new double[] { 0.0, 2.0, 1.0, 4.0, 1.0, 3.0, 0.0 });

		settings.put("#Tc_d3Type3_s2/3",
				new double[] { 0.0, 0.0, 1.0, 3.0, 0.0, 3.0, bookingPeriodLength * 2.0 / 3.0 });
		settings.put("#Tc_d3Type3PerTw_s2/3",
				new double[] { 0.0, 0.0, 1.0, 3.0, 1.0, 3.0, bookingPeriodLength * 2.0 / 3.0 });
		settings.put("#IcTc_d3Type3_s2/3",
				new double[] { 1.0, 0.0, 1.0, 3.0, 0.0, 3.0, bookingPeriodLength * 2.0 / 3.0 });
		settings.put("#IcTc_d3Type3PerTw_s2/3",
				new double[] { 1.0, 0.0, 1.0, 3.0, 1.0, 3.0, bookingPeriodLength * 2.0 / 3.0 });
		settings.put("#EcaTc_d3Type3_s2/3",
				new double[] { 0.0, 2.0, 1.0, 3.0, 0.0, 3.0, bookingPeriodLength * 2.0 / 3.0 });
		settings.put("#EcaTc_d3Type3PerTw_s2/3",
				new double[] { 0.0, 2.0, 1.0, 3.0, 1.0, 3.0, bookingPeriodLength * 2.0 / 3.0 });

		


			// Can do this for more experiments...
			helper = new RunExperimentHelper();
			// Define settings
			helper.experimentName = basicName + adpName + benchmark + "Acc";
			helper.experimentDescription = " ";
			helper.experimentOccasion = " ";
			helper.experimentResponsible = " ";
			helper.noRepetitions = 100;
			helper.regionId = regionId;
			helper.processTypeId = 42;
			helper.bookingPeriodNumber = 1;
			helper.bookingPeriodLength = bookingPeriodLength;
			helper.valueFunctionModelSetId = valueFunctionModelSetId;
			helper.orderRequestSetId = firstOrderRequestSetIdForValiation;
			helper.orderRequestSetChanges = true;
			helper.demandSegmentWeightingId = demandSegmentWeightingId;
			helper.vehicleAssignmentSetId = vehicleAssignmentSetId;
			helper.deliveryAreaSetId = deliveryAreaSetId;
			helper.learningRoutingsExperimentIds = learningRoutingExperimentIds;

			paras = new HashMap<String, Double>();
			paras.put("Constant_service_time", 12.0);
			paras.put("samplePreferences", 1.0);
			paras.put("includeDriveFromStartingPosition", 0.0);
			paras.put("no_routing_candidates", 0.0);
			paras.put("no_insertion_candidates", 1.0);
			paras.put("distance_type", settings.get(benchmark)[3]);
			paras.put("distance_measure_per_tw", settings.get(benchmark)[4]);
			paras.put("maximum_distance_measure_increase", settings.get(benchmark)[5]);
			paras.put("switch_distance_off_point", settings.get(benchmark)[6]);

			// Run experiment
			helper.start(paras);
			int firstExpIda = SettingsProvider.getExperiment().getId() - 100 + 1;
			resultLog += "(" + helper.experimentName + ": " + firstExpIda + ") ";
		
		
		return resultLog;
	}
	
	public static String runFCFSYang(String basicName, int bookingPeriodLength, int regionId, int deliveryAreaSetId, int vehicleAssignmentSetId, int timeWindowSetId,
			int firstOrderRequestSetIdForValiation) {
		RunExperimentHelper helper;
		HashMap<String, Double> paras;
		String resultLog = "Result: ";
		
		helper = new RunExperimentHelper();
		helper.experimentName = basicName + "YangFCFS";
		helper.experimentDescription = " ";
		helper.experimentOccasion = " ";
		helper.experimentResponsible = " ";
		helper.noRepetitions = 100;
		helper.regionId = regionId;
		helper.processTypeId = 43;
		helper.bookingPeriodNumber = 1;
		helper.bookingPeriodLength = bookingPeriodLength;
		helper.orderRequestSetId = firstOrderRequestSetIdForValiation;
		helper.deliveryAreaSetId = deliveryAreaSetId;
		helper.vehicleAssignmentSetId = vehicleAssignmentSetId;
		helper.timeWindowSetId = timeWindowSetId;
		helper.orderRequestSetChanges = true;

		paras = new HashMap<String, Double>();
		paras.put("Constant_service_time", 12.0);
		paras.put("samplePreferences", 1.0);
		paras.put("includeDriveFromStartingPosition", 0.0);
		paras.put("no_routing_candidates", 10.0);
		helper.start(paras);

		int firstExpId = SettingsProvider.getExperiment().getId() - 100 + 1;
		resultLog += "(" + helper.experimentName + ": " + firstExpId + ") ";
		return resultLog;
	}
	
	public static String runBenchmarkingADPExperiments(String basicName, int bookingPeriodLength, int regionId, int depotId, 
			int deliveryAreaSetId, int vehicleAssignmentSetId, int noVehicles, int demandSegmentWeightingId, int timeWindowSetId,
			int firstOrderRequestSetIdForValiation, int learningRequestSetsExperimentId,
			ArrayList<Integer> learningRoutingExperimentIds, ArrayList<Integer> learningRoutingExperimentIdsExpected, int arrivalProcess, boolean fcfs, boolean ann, String[] names) {

		String adpName = "ADP_";
		RunExperimentHelper helper;
		HashMap<String, Double> paras;
		String resultLog = "Result: ";

		// FCFS
		if (fcfs) {
			
			helper = new RunExperimentHelper();
			helper.experimentName = basicName + "FCFS";
			helper.experimentDescription = " ";
			helper.experimentOccasion = " ";
			helper.experimentResponsible = " ";
			helper.noRepetitions = 100;
			helper.regionId = regionId;
			helper.processTypeId = 29;
			helper.bookingPeriodNumber = 1;
			helper.bookingPeriodLength = bookingPeriodLength;
			helper.orderRequestSetId = firstOrderRequestSetIdForValiation;
			helper.deliveryAreaSetId = deliveryAreaSetId;
			helper.vehicleAssignmentSetId = vehicleAssignmentSetId;
			helper.timeWindowSetId = timeWindowSetId;
			helper.orderRequestSetChanges = true;

			paras = new HashMap<String, Double>();
			paras.put("Constant_service_time", 12.0);
			paras.put("samplePreferences", 1.0);
			paras.put("includeDriveFromStartingPosition", 0.0);
			paras.put("no_routing_candidates", 0.0);
			paras.put("no_insertion_candidates", 1.0);
			paras.put("consider_profit", 0.0);
			helper.start(paras);

			int firstExpId = SettingsProvider.getExperiment().getId() - 100 + 1;
			resultLog += "(" + helper.experimentName + ": " + firstExpId + ") ";
		}
		// ANN
		if(ann) {
		
			 helper = new RunExperimentHelper();
			 paras = new HashMap<String, Double>();
			resultLog= RunExperimentHelper.runANNExperiments(helper, paras, basicName, adpName, regionId, bookingPeriodLength, 
					learningRequestSetsExperimentId, demandSegmentWeightingId, deliveryAreaSetId, vehicleAssignmentSetId, 
					learningRoutingExperimentIdsExpected, arrivalProcess, firstOrderRequestSetIdForValiation, noVehicles, depotId, resultLog, null, 3.0, true);
		}	 
		
		
		HashMap<String, double[]> settings = new HashMap<String, double[]>();
		// Order: ic, ec, tc, distanceType, distanceMeasurePerTw,
		// maximumDistanceIncrease, switchTime
		settings.put("#", new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 });
		settings.put("#Tc", new double[] { 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0 });
		settings.put("#Ic", new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 });
		settings.put("#IcTc", new double[] { 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0 });
		settings.put("#Ec", new double[] { 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0 });
		settings.put("#EcTc", new double[] { 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0 });
		settings.put("#Eca", new double[] { 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0 });
		settings.put("#EcaTc", new double[] { 0.0, 2.0, 1.0, 0.0, 0.0, 0.0, 0.0 });

		// Dist 0
		settings.put("#Tc_d0", new double[] { 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0 });
		settings.put("#Tc_d0Type2", new double[] { 0.0, 0.0, 1.0, 2.0, 0.0, 0.0, 0.0 });
		settings.put("#Tc_d0Type2PerTw", new double[] { 0.0, 0.0, 1.0, 2.0, 1.0, 0.0, 0.0 });
		settings.put("#IcTc_d0", new double[] { 1.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0 });
		settings.put("#IcTc_d0Type2", new double[] { 1.0, 0.0, 1.0, 2.0, 0.0, 0.0, 0.0 });
		settings.put("#IcTc_d0Type2PerTw", new double[] { 1.0, 0.0, 1.0, 2.0, 1.0, 0.0, 0.0 });
		settings.put("#EcaTc_d0", new double[] { 0.0, 2.0, 1.0, 1.0, 0.0, 0.0, 0.0 });
		settings.put("#EcaTc_d0Type2", new double[] { 0.0, 2.0, 1.0, 2.0, 0.0, 0.0, 0.0 });
		settings.put("#EcaTc_d0Type2PerTw", new double[] { 0.0, 2.0, 1.0, 2.0, 1.0, 0.0, 0.0 });

		settings.put("#Tc_d0_s", new double[] { 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, bookingPeriodLength / 2.0 });
		settings.put("#Tc_d0Type2_s", new double[] { 0.0, 0.0, 1.0, 2.0, 0.0, 0.0, bookingPeriodLength / 2.0 });
		settings.put("#Tc_d0Type2PerTw_s", new double[] { 0.0, 0.0, 1.0, 2.0, 1.0, 0.0, bookingPeriodLength / 2.0 });
		settings.put("#IcTc_d0_s", new double[] { 1.0, 0.0, 1.0, 1.0, 0.0, 0.0, bookingPeriodLength / 2.0 });
		settings.put("#IcTc_d0Type2_s", new double[] { 1.0, 0.0, 1.0, 2.0, 0.0, 0.0, bookingPeriodLength / 2.0 });
		settings.put("#IcTc_d0Type2PerTw_s", new double[] { 1.0, 0.0, 1.0, 2.0, 1.0, 0.0, bookingPeriodLength / 2.0 });
		settings.put("#EcaTc_d0_s", new double[] { 0.0, 2.0, 1.0, 1.0, 0.0, 0.0, bookingPeriodLength / 2.0 });
		settings.put("#EcaTc_d0Type2_s", new double[] { 0.0, 2.0, 1.0, 2.0, 0.0, 0.0, bookingPeriodLength / 2.0 });
		settings.put("#EcaTc_d0Type2PerTw_s", new double[] { 0.0, 2.0, 1.0, 2.0, 1.0, 0.0, bookingPeriodLength / 2.0 });

		// Dist 3
		settings.put("#Tc_d3Type3", new double[] { 0.0, 0.0, 1.0, 3.0, 0.0, 3.0, 0.0 });
		settings.put("#Tc_d3Type3PerTw", new double[] { 0.0, 0.0, 1.0, 3.0, 1.0, 3.0, 0.0 });
		settings.put("#IcTc_d3Type3", new double[] { 1.0, 0.0, 1.0, 3.0, 0.0, 3.0, 0.0 });
		settings.put("#IcTc_d3Type3PerTw", new double[] { 1.0, 0.0, 1.0, 3.0, 1.0, 3.0, 0.0 });
		settings.put("#EcaTc_d3Type3", new double[] { 0.0, 2.0, 1.0, 3.0, 0.0, 3.0, 0.0 });
		settings.put("#EcaTc_d3Type3PerTw", new double[] { 0.0, 2.0, 1.0, 3.0, 1.0, 3.0, 0.0 });

		settings.put("#Tc_d3Type3_s", new double[] { 0.0, 0.0, 1.0, 3.0, 0.0, 3.0, bookingPeriodLength / 2.0 });
		settings.put("#Tc_d3Type3PerTw_s", new double[] { 0.0, 0.0, 1.0, 3.0, 1.0, 3.0, bookingPeriodLength / 2.0 });
		settings.put("#IcTc_d3Type3_s", new double[] { 1.0, 0.0, 1.0, 3.0, 0.0, 3.0, bookingPeriodLength / 2.0 });
		settings.put("#IcTc_d3Type3PerTw_s", new double[] { 1.0, 0.0, 1.0, 3.0, 1.0, 3.0, bookingPeriodLength / 2.0 });
		settings.put("#EcaTc_d3Type3_s", new double[] { 0.0, 2.0, 1.0, 3.0, 0.0, 3.0, bookingPeriodLength / 2.0 });
		settings.put("#EcaTc_d3Type3PerTw_s", new double[] { 0.0, 2.0, 1.0, 3.0, 1.0, 3.0, bookingPeriodLength / 2.0 });

		settings.put("#Tc_d3Type3_s1/3", new double[] { 0.0, 0.0, 1.0, 3.0, 0.0, 3.0, bookingPeriodLength / 3.0 });
		settings.put("#Tc_d3Type3PerTw_s1/3", new double[] { 0.0, 0.0, 1.0, 3.0, 1.0, 3.0, bookingPeriodLength / 3.0 });
		settings.put("#IcTc_d3Type3_s1/3", new double[] { 1.0, 0.0, 1.0, 3.0, 0.0, 3.0, bookingPeriodLength / 3.0 });
		settings.put("#IcTc_d3Type3PerTw_s1/3",
				new double[] { 1.0, 0.0, 1.0, 3.0, 1.0, 3.0, bookingPeriodLength / 3.0 });
		settings.put("#EcaTc_d3Type3_s1/3", new double[] { 0.0, 2.0, 1.0, 3.0, 0.0, 3.0, bookingPeriodLength / 3.0 });
		settings.put("#EcaTc_d3Type3PerTw_s1/3",
				new double[] { 0.0, 2.0, 1.0, 3.0, 1.0, 3.0, bookingPeriodLength / 3.0 });
		
		//Dist 4
		settings.put("#Tc_dType4", new double[] { 0.0, 0.0, 1.0, 4.0, 0.0, 3.0, 0.0 });
		settings.put("#Tc_dType4PerTw", new double[] { 0.0, 0.0, 1.0, 4.0, 1.0, 3.0, 0.0 });
		settings.put("#IcTc_dType4", new double[] { 1.0, 0.0, 1.0, 4.0, 0.0, 3.0, 0.0 });
		settings.put("#IcTc_dType4PerTw", new double[] { 1.0, 0.0, 1.0, 4.0, 1.0, 3.0, 0.0 });
		settings.put("#EcaTc_dType4", new double[] { 0.0, 2.0, 1.0, 4.0, 0.0, 3.0, 0.0 });
		settings.put("#EcaTc_dType4PerTw", new double[] { 0.0, 2.0, 1.0, 4.0, 1.0, 3.0, 0.0 });

		settings.put("#Tc_d3Type3_s2/3",
				new double[] { 0.0, 0.0, 1.0, 3.0, 0.0, 3.0, bookingPeriodLength * 2.0 / 3.0 });
		settings.put("#Tc_d3Type3PerTw_s2/3",
				new double[] { 0.0, 0.0, 1.0, 3.0, 1.0, 3.0, bookingPeriodLength * 2.0 / 3.0 });
		settings.put("#IcTc_d3Type3_s2/3",
				new double[] { 1.0, 0.0, 1.0, 3.0, 0.0, 3.0, bookingPeriodLength * 2.0 / 3.0 });
		settings.put("#IcTc_d3Type3PerTw_s2/3",
				new double[] { 1.0, 0.0, 1.0, 3.0, 1.0, 3.0, bookingPeriodLength * 2.0 / 3.0 });
		settings.put("#EcaTc_d3Type3_s2/3",
				new double[] { 0.0, 2.0, 1.0, 3.0, 0.0, 3.0, bookingPeriodLength * 2.0 / 3.0 });
		settings.put("#EcaTc_d3Type3PerTw_s2/3",
				new double[] { 0.0, 2.0, 1.0, 3.0, 1.0, 3.0, bookingPeriodLength * 2.0 / 3.0 });

		for (int i = 0; i < names.length; i++) {
			helper = new RunExperimentHelper();
			paras = new HashMap<String, Double>();
			// Define settings
			helper.experimentName = basicName + adpName + names[i];
			helper.experimentDescription = " ";
			helper.experimentOccasion = " ";
			helper.experimentResponsible = "ML";
			helper.noRepetitions = 1;
			helper.regionId = regionId;
			helper.processTypeId = 41;
			helper.bookingPeriodNumber = 1;
			helper.bookingPeriodLength = bookingPeriodLength;
			helper.learningRequestSetsExperimentId = learningRequestSetsExperimentId;
			helper.demandSegmentWeightingId = demandSegmentWeightingId;
			helper.deliveryAreaSetId = deliveryAreaSetId;
			helper.vehicleAssignmentSetId = vehicleAssignmentSetId;
			helper.learningRoutingsExperimentIds = learningRoutingExperimentIds;

			paras.put("Constant_service_time", 12.0);
			paras.put("actualBasketValue", 1.0);
			paras.put("samplePreferences", 1.0);
			paras.put("includeDriveFromStartingPosition", 0.0);
			paras.put("consider_overall_remaining_capacity", settings.get(names[i])[0]);
			paras.put("consider_overall_accepted_insertion_costs", settings.get(names[i])[1]);
			paras.put("time_cap_interaction", settings.get(names[i])[2]);
			paras.put("exploration_(0:on-policy,1:wheel,2:e-greedy)", 2.0);
			paras.put("no_routing_candidates", 0.0);
			paras.put("no_insertion_candidates", 1.0);
			paras.put("distance_type", settings.get(names[i])[3]);
			paras.put("distance_measure_per_tw", settings.get(names[i])[4]);
			paras.put("maximum_distance_measure_increase", settings.get(names[i])[5]);
			paras.put("switch_distance_off_point", settings.get(names[i])[6]);
			paras.put("meso_weight_lf", 1.0);
			paras.put("stepsize_adp_learning", 0.0001);
			paras.put("annealing_temperature_(Negative:no_annealing)", 4000.0);
			paras.put("momentum_weight", 0.9);
			paras.put("no_repetitions_sample", 1.0);

			// Run experiment
			helper.start(paras);

			// Can do this for more experiments...
			helper = new RunExperimentHelper();
			// Define settings
			helper.experimentName = basicName + adpName + names[i] + "Acc";
			helper.experimentDescription = " ";
			helper.experimentOccasion = " ";
			helper.experimentResponsible = " ";
			helper.noRepetitions = 100;
			helper.regionId = regionId;
			helper.processTypeId = 42;
			helper.bookingPeriodNumber = 1;
			helper.bookingPeriodLength = bookingPeriodLength;
			helper.valueFunctionModelSetId = SettingsProvider.getPeriodSetting().getValueFunctionModelSetId();
			helper.orderRequestSetId = firstOrderRequestSetIdForValiation;
			helper.orderRequestSetChanges = true;
			helper.demandSegmentWeightingId = demandSegmentWeightingId;
			helper.vehicleAssignmentSetId = vehicleAssignmentSetId;
			helper.deliveryAreaSetId = deliveryAreaSetId;
			helper.learningRoutingsExperimentIds = learningRoutingExperimentIds;

			paras = new HashMap<String, Double>();
			paras.put("Constant_service_time", 12.0);
			paras.put("samplePreferences", 1.0);
			paras.put("includeDriveFromStartingPosition", 0.0);
			paras.put("no_routing_candidates", 0.0);
			paras.put("no_insertion_candidates", 1.0);
			paras.put("distance_type", settings.get(names[i])[3]);
			paras.put("distance_measure_per_tw", settings.get(names[i])[4]);
			paras.put("maximum_distance_measure_increase", settings.get(names[i])[5]);
			paras.put("switch_distance_off_point", settings.get(names[i])[6]);

			// Run experiment
			helper.start(paras);
			int firstExpIda = SettingsProvider.getExperiment().getId() - 100 + 1;
			resultLog += "(" + helper.experimentName + ": " + firstExpIda + ") ";
		
		}
		return resultLog;
	}
	
	public static String runANNExperiments(RunExperimentHelper helper, HashMap<String, Double> paras, String basicName, String adpName, int regionId, int bookingPeriodLength,
			int learningRequestSetsExperimentId, int demandSegmentWeightingId, int deliveryAreaSetId, int vehicleAssignmentSetId,  
			ArrayList<Integer> learningRoutingExperimentIdsExpected,int arrivalProcess, int firstOrderRequestSetIdForValiation,int noVehicles, int depotId, String resultLog,
			Integer valueFunctionModelSetId, double additionalNodes) {
		return RunExperimentHelper.runANNExperiments(helper, paras, basicName, 
				adpName, regionId, bookingPeriodLength, learningRequestSetsExperimentId, 
				demandSegmentWeightingId, deliveryAreaSetId, vehicleAssignmentSetId, learningRoutingExperimentIdsExpected, arrivalProcess, 
				firstOrderRequestSetIdForValiation, noVehicles, depotId, resultLog, valueFunctionModelSetId, additionalNodes, true);
		
	}
	public static String runANNExperiments(RunExperimentHelper helper, HashMap<String, Double> paras, String basicName, 
			String adpName, int regionId, int bookingPeriodLength,
			int learningRequestSetsExperimentId, int demandSegmentWeightingId, int deliveryAreaSetId, int vehicleAssignmentSetId,  
			ArrayList<Integer> learningRoutingExperimentIdsExpected,int arrivalProcess, int firstOrderRequestSetIdForValiation,int noVehicles,
			int depotId, String resultLog,
			Integer valueFunctionModelSetId, double additionalNodes, boolean withFinalRouting) {
	
		int valueFunctionModelId;
		if(valueFunctionModelSetId==null || valueFunctionModelSetId.equals(0)) {
			 // Define settings
			 helper.experimentName = basicName + adpName + "Ann_"+deliveryAreaSetId;
			 helper.experimentDescription = " ";
			 helper.experimentOccasion = " ";
			 helper.experimentResponsible = "ML";
			 helper.noRepetitions = 1;
			 helper.regionId = regionId;
			 helper.processTypeId = 31;
			 helper.bookingPeriodNumber = 1;
			 helper.bookingPeriodLength = bookingPeriodLength;
			 helper.learningRequestSetsExperimentId =learningRequestSetsExperimentId;
			 helper.demandSegmentWeightingId = demandSegmentWeightingId;
			 helper.deliveryAreaSetId = deliveryAreaSetId;
			 helper.vehicleAssignmentSetId = vehicleAssignmentSetId;
			 helper.learningRoutingsExperimentIds =learningRoutingExperimentIdsExpected;
			 helper.arrivalProcessId = arrivalProcess;
			//
			 paras.put("Constant_service_time", 12.0);
			 paras.put("actualBasketValue", 1.0);
			 paras.put("samplePreferences", 1.0);
			 paras.put("includeDriveFromStartingPosition", 0.0);
			 paras.put("exploration_(0:on-policy,1:conservative-factor,2:e-greedy)",
			 2.0);
			 paras.put("stepsize_adp_learning", 0.0001);
			 paras.put("annealing_temperature_(Negative:no_annealing)", 4000.0);
			 paras.put("momentum_weight", 0.9);
			 paras.put("theft-based", 1.0);
			 paras.put("theft-based-advanced", 1.0);
			 paras.put("theft-based-tw", 1.0);
			 paras.put("consider_left_over_penalty", 0.0);
			 paras.put("discounting_factor", 1.0);
			 paras.put("discounting_factor_probability", 1.0);
			 paras.put("consider_constant", 1.0);
			 paras.put("additional_hidden_nodes", additionalNodes);
			 paras.put("consider_demand_neighbors", 0.0);
			 paras.put("oc_for_feasible", 0.0);
			 paras.put("hTan_activation", 1.0);
			 helper.start(paras);
			 valueFunctionModelId=SettingsProvider.getPeriodSetting().getValueFunctionModelSetId();
		}else {
			 valueFunctionModelId=valueFunctionModelSetId;
		}
	// Can do this for more experiments...
	 helper = new RunExperimentHelper();
	 int acceptanceProcess= 32;
	 if(withFinalRouting) acceptanceProcess=39;
	// // Define settings
	 helper.experimentName = basicName + adpName+"Ann_"+deliveryAreaSetId+"_Acc";
	 helper.experimentDescription = " ";
	 helper.experimentOccasion = " ";
	 helper.experimentResponsible = " ";
	 helper.noRepetitions = 100;
	 helper.regionId = regionId;
	 helper.processTypeId = acceptanceProcess;
	 helper.bookingPeriodNumber = 1;
	 helper.bookingPeriodLength = bookingPeriodLength;
	 helper.valueFunctionModelSetId = valueFunctionModelId;
	 helper.orderRequestSetId = firstOrderRequestSetIdForValiation;
	 helper.orderRequestSetChanges = true;
	 helper.demandSegmentWeightingId = demandSegmentWeightingId;
	 helper.vehicleAssignmentSetId = vehicleAssignmentSetId;
	 helper.deliveryAreaSetId = deliveryAreaSetId;
	 helper.learningRoutingsExperimentIds =learningRoutingExperimentIdsExpected;
	 helper.arrivalProcessId = arrivalProcess;
	 helper.noVehicles=noVehicles;
	 helper.vehicleTypes = new ArrayList<Integer>();
	 helper.vehicleTypes.add(1);
	 helper.vehicleTypesNos = new ArrayList<Integer>();
	 helper.vehicleTypesNos.add(noVehicles);
	 helper.depotId=depotId;
	
	 paras = new HashMap<String, Double>();
	 paras.put("actualBasketValue", 1.0);
	 paras.put("Constant_service_time", 12.0);
	 paras.put("samplePreferences", 1.0);
	 paras.put("theft-based", 0.0);
	 paras.put("theft-based-advanced", 0.0);
	 paras.put("theft-based-tw", 0.0);
	 paras.put("consider_left_over_penalty", 0.0);
	 paras.put("discounting_factor_probability", 1.0);
	 paras.put("oc_for_feasible", 0.0);
	 paras.put("stop_once_feasible", 1.0);
	
	 // Run experiment
	 helper.start(paras);
	 int firstExpId = SettingsProvider.getExperiment().getId() - 100 + 1;
	 resultLog += "(" + helper.experimentName + ": " + firstExpId + ") ";
	 
	// Can do this for more experiments...
			 helper = new RunExperimentHelper();
			// // Define settings
			 helper.experimentName = basicName + adpName+"Ann_"+deliveryAreaSetId+"_TheftS_Acc";
			 helper.experimentDescription = " ";
			 helper.experimentOccasion = " ";
			 helper.experimentResponsible = " ";
			 helper.noRepetitions = 100;
			 helper.regionId = regionId;
			 helper.processTypeId = acceptanceProcess;
			 helper.bookingPeriodNumber = 1;
			 helper.bookingPeriodLength = bookingPeriodLength;
			 helper.valueFunctionModelSetId = valueFunctionModelId;
			 helper.orderRequestSetId = firstOrderRequestSetIdForValiation;
			 helper.orderRequestSetChanges = true;
			 helper.demandSegmentWeightingId = demandSegmentWeightingId;
			 helper.vehicleAssignmentSetId = vehicleAssignmentSetId;
			 helper.deliveryAreaSetId = deliveryAreaSetId;
			 helper.learningRoutingsExperimentIds =learningRoutingExperimentIdsExpected;
			 helper.arrivalProcessId = arrivalProcess;
			 helper.noVehicles=noVehicles;
			 helper.vehicleTypes = new ArrayList<Integer>();
			 helper.vehicleTypes.add(1);
			 helper.vehicleTypesNos = new ArrayList<Integer>();
			 helper.vehicleTypesNos.add(noVehicles);
			 helper.depotId=depotId;
			
			 paras = new HashMap<String, Double>();
			 paras.put("actualBasketValue", 1.0);
			 paras.put("Constant_service_time", 12.0);
			 paras.put("samplePreferences", 1.0);
			 paras.put("theft-based", 1.0);
			 paras.put("theft-based-advanced", 1.0);
			 paras.put("theft-based-tw", 0.0);
			 paras.put("consider_left_over_penalty", 0.0);
			 paras.put("discounting_factor_probability", 1.0);
			 paras.put("oc_for_feasible", 0.0);
			 paras.put("stop_once_feasible", 1.0);
			
			 // Run experiment
			 helper.start(paras);
			  firstExpId = SettingsProvider.getExperiment().getId() - 100 + 1;
			 resultLog += "(" + helper.experimentName + ": " + firstExpId + ") ";
			 
			// Can do this for more experiments...
			 helper = new RunExperimentHelper();
			// // Define settings
			 helper.experimentName = basicName + adpName+"Ann_"+deliveryAreaSetId+"_TheftST_Acc";
			 helper.experimentDescription = " ";
			 helper.experimentOccasion = " ";
			 helper.experimentResponsible = " ";
			 helper.noRepetitions = 100;
			 helper.regionId = regionId;
			 helper.processTypeId = acceptanceProcess;
			 helper.bookingPeriodNumber = 1;
			 helper.bookingPeriodLength = bookingPeriodLength;
			 helper.valueFunctionModelSetId =valueFunctionModelId;
			 helper.orderRequestSetId = firstOrderRequestSetIdForValiation;
			 helper.orderRequestSetChanges = true;
			 helper.demandSegmentWeightingId = demandSegmentWeightingId;
			 helper.vehicleAssignmentSetId = vehicleAssignmentSetId;
			 helper.deliveryAreaSetId = deliveryAreaSetId;
			 helper.learningRoutingsExperimentIds =learningRoutingExperimentIdsExpected;
			 helper.arrivalProcessId = arrivalProcess;
			 helper.noVehicles=noVehicles;
			 helper.vehicleTypes = new ArrayList<Integer>();
			 helper.vehicleTypes.add(1);
			 helper.vehicleTypesNos = new ArrayList<Integer>();
			 helper.vehicleTypesNos.add(noVehicles);
			 helper.depotId=depotId;
			
			 paras = new HashMap<String, Double>();
			 paras.put("actualBasketValue", 1.0);
			 paras.put("Constant_service_time", 12.0);
			 paras.put("samplePreferences", 1.0);
			 paras.put("theft-based", 1.0);
			 paras.put("theft-based-advanced", 1.0);
			 paras.put("theft-based-tw", 1.0);
			 paras.put("consider_left_over_penalty", 0.0);
			 paras.put("discounting_factor_probability", 1.0);
			 paras.put("oc_for_feasible", 0.0);
			 paras.put("stop_once_feasible", 1.0);
			
			 // Run experiment
			 helper.start(paras);
			 firstExpId = SettingsProvider.getExperiment().getId() - 100 + 1;
			 resultLog += "(" + helper.experimentName + ": " + firstExpId + ") ";
			 
			 return resultLog;
}
	
	public static String runANNExperiments(String basicName, int regionId, int bookingPeriodLength,
			int learningRequestSetsExperimentId, int demandSegmentWeightingId, int deliveryAreaSetId, int vehicleAssignmentSetId,  
			ArrayList<Integer> learningRoutingExperimentIdsExpected,int arrivalProcess, int firstOrderRequestSetIdForValiation,int noVehicles, int depotId) {
		
		String adpName = "ADP_";
		RunExperimentHelper helper = new RunExperimentHelper();
		HashMap<String, Double> paras = new HashMap<String, Double>();
		String resultLog = "Result: ";
		
		resultLog=RunExperimentHelper.runANNExperiments(helper, paras, basicName, adpName, regionId, bookingPeriodLength, 
				learningRequestSetsExperimentId, demandSegmentWeightingId, deliveryAreaSetId, vehicleAssignmentSetId, 
				learningRoutingExperimentIdsExpected, arrivalProcess, firstOrderRequestSetIdForValiation,
				noVehicles, depotId, resultLog, null, 3.0, true);
		
		return resultLog;
	}
	
	public static String runANNAcceptanceExperiments(String basicName, int regionId, int bookingPeriodLength,
			int learningRequestSetsExperimentId, int demandSegmentWeightingId, int deliveryAreaSetId, int vehicleAssignmentSetId,  
			ArrayList<Integer> learningRoutingExperimentIdsExpected,int arrivalProcess, int firstOrderRequestSetIdForValiation,int noVehicles, int depotId, Integer valueFunctionModelSetId) {
		
		String adpName = "ADP_";
		RunExperimentHelper helper = new RunExperimentHelper();
		HashMap<String, Double> paras = new HashMap<String, Double>();
		String resultLog = "Result: ";
		
		resultLog=RunExperimentHelper.runANNExperiments(helper, paras, basicName, adpName, regionId, bookingPeriodLength, 
				learningRequestSetsExperimentId, demandSegmentWeightingId, deliveryAreaSetId, vehicleAssignmentSetId, 
				learningRoutingExperimentIdsExpected, arrivalProcess, firstOrderRequestSetIdForValiation,
				noVehicles, depotId, resultLog, valueFunctionModelSetId, 3.0, true);
		
		return resultLog;
	}
	
	public static String runANNExperimentsWithoutFinalRouting(String basicName, int regionId, int bookingPeriodLength,
			int learningRequestSetsExperimentId, int demandSegmentWeightingId, int deliveryAreaSetId, int vehicleAssignmentSetId,  
			ArrayList<Integer> learningRoutingExperimentIdsExpected,int arrivalProcess, int firstOrderRequestSetIdForValiation,int noVehicles, int depotId,
			int valueFunctionModelSetId) {
		
		String adpName = "ADP_";
		RunExperimentHelper helper = new RunExperimentHelper();
		HashMap<String, Double> paras = new HashMap<String, Double>();
		String resultLog = "Result: ";
		
		resultLog=RunExperimentHelper.runANNExperiments(helper, paras, basicName, adpName, regionId, bookingPeriodLength, 
				learningRequestSetsExperimentId, demandSegmentWeightingId, deliveryAreaSetId, vehicleAssignmentSetId, 
				learningRoutingExperimentIdsExpected, arrivalProcess, firstOrderRequestSetIdForValiation,
				noVehicles, depotId, resultLog, valueFunctionModelSetId, 3.0, false);
		
		return resultLog;
	}
}
