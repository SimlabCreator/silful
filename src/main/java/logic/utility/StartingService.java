package logic.utility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

import org.rosuda.JRI.Rengine;

import data.entity.Depot;
import data.entity.Entity;
import data.entity.Experiment;
import data.entity.GeneralParameterValue;
import data.entity.ObjectiveWeight;
import data.entity.OrderRequestSet;
import data.entity.ParameterType;
import data.entity.PeriodSetting;
import data.entity.Routing;
import data.entity.RoutingAssignment;
import data.entity.SetEntity;
import data.entity.Vehicle;
import data.entity.WeightingEntity;
import data.utility.DataServiceProvider;
import data.utility.PeriodSettingType;
import logic.process.IProcess;
import logic.utility.comparator.ExperimentIdAscComparator;

/**
 * Entry point of the program
 * 
 * @author M. Lang
 *
 */
public class StartingService {

	private Scanner scanner;
	private static Boolean repeatExperiment;


	public static void main(String[] args) {

		// Define settings of the new run
		StartingService service = new StartingService();
		IProcess process = service.defineExperimentAndRun();

		
		
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
			
			//Reset possible routing assignments (if they are output)
			if(repeatExperiment && SettingsProvider.getOutputs().contains(PeriodSettingType.FINALROUTING) ){
				ArrayList<RoutingAssignment> rAss = new ArrayList<RoutingAssignment>();
				for (int i = 0; i < SettingsProvider.getPeriodSetting().getRoutingAssignments().size(); i++) {
					if (SettingsProvider.getPeriodSetting().getRoutingAssignments().get(i).getT() == -1)
						rAss.add(SettingsProvider.getPeriodSetting().getRoutingAssignments().get(i));
				}
				SettingsProvider.getPeriodSetting().setRoutingAssignments(rAss);
			}
			if(repeatExperiment && SettingsProvider.getOutputs().contains(PeriodSettingType.INITIALROUTING)){
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
			if(SettingsProvider.saveResults) {
			if (repeatExperiment && !SettingsProvider.doesOrderRequestSetChange()) {
				DataServiceProvider.getSettingsDataServiceImplInstance().persistRunSettings(
						SettingsProvider.getExperiment(), SettingsProvider.getPeriodSetting(),
						SettingsProvider.getPeriodSettingFollowers(), SettingsProvider.getSettingRequest(),
						SettingsProvider.getOutputs(), totalTime);
			} else {
				if(!repeatExperiment){
					experimentId=DataServiceProvider.getSettingsDataServiceImplInstance().persistExperimentSettings(
							SettingsProvider.getExperiment(), SettingsProvider.getPeriodSetting(),
							SettingsProvider.getPeriodSettingFollowers(), SettingsProvider.getSettingRequest(),
							SettingsProvider.getOutputs(), totalTime);
				}else{
					SettingsProvider.getExperiment().setCopyExperimentId(experimentId);
					DataServiceProvider.getSettingsDataServiceImplInstance().persistExperimentSettings(
							SettingsProvider.getExperiment(), SettingsProvider.getPeriodSetting(),
							SettingsProvider.getPeriodSettingFollowers(), SettingsProvider.getSettingRequest(),
							SettingsProvider.getOutputs(), totalTime);
				}
				
			}
			}
			repeatExperiment = true;
			repetition++;
		}

		
	}

	/**
	 * Defines the basic settings of a run and returns the process that will be
	 * conducted
	 */
	private IProcess defineExperimentAndRun() {

		scanner = new Scanner(System.in); 

		ArrayList<Integer> possibleInputsYesNo = new ArrayList<Integer>();
		possibleInputsYesNo.add(0);
		possibleInputsYesNo.add(1);
		ArrayList<Integer> possibleInputsAll = new ArrayList<Integer>();

		System.out.print("Database: ");
		scanner.skip("\\W*");
		SettingsProvider.database=scanner.nextLine();
		
		System.out.println("New experiment? (0:no, 1:yes)");
		Integer newExperiment = getUserInputInt(possibleInputsYesNo);

		Experiment experiment;
		IProcess process;
		if (newExperiment == 1) {
			experiment = new Experiment();
			repeatExperiment = false;

			System.out.print("Name: ");
			scanner.skip("\\W*");
			experiment.setName(scanner.nextLine());

			
			
			System.out.print("Description: ");
			experiment.setDescription(scanner.nextLine());

			System.out.print("Responsible: ");
			experiment.setResponsible(scanner.nextLine());

			System.out.print("Occasion: ");
			experiment.setOccasion(scanner.nextLine());

			// Choose region
			ArrayList<Entity> regions = DataServiceProvider.getRegionDataServiceImplInstance().getAll();
			System.out.println("Regions: ");
			experiment.setRegionId(getUserInputAlternatives(regions, "region"));

			// Choose process type
			ArrayList<Entity> processTypes = DataServiceProvider.getProcessTypeDataServiceImplInstance().getAll();
			System.out.println("Process types: ");
			experiment.setProcessTypeId(getUserInputAlternatives(processTypes, "process types"));

			// Request settings depending on the process type
			process = ProcessProvider.getProcessByProcessTypeId(experiment.getProcessTypeId());

			if (process.multiplePeriodsPossible()) {
				System.out.print("Booking period number (int): ");

				experiment.setBookingPeriodNumber(getUserInputInt(possibleInputsAll));
			} else {
				experiment.setBookingPeriodNumber(1);
			}

			if (process.needBookingPeriodLength()) {
				System.out.print("Booking period length (int): ");

				experiment.setBookingPeriodLength(getUserInputInt(possibleInputsAll));
			}

			if (process.needDepotLocation()) {
				// Choose process type
				ArrayList<Depot> depots = DataServiceProvider.getDepotDataServiceImplInstance()
						.getAllDepotsByRegionId(experiment.getRegionId());
				System.out.println("Depots for region: ");
				experiment.setDepotId(getUserInputAlternatives(depots, "depot"));
			}

			// Choose incentive type
			if (process.needIncentiveType()) {
				ArrayList<Entity> incentiveTypes = DataServiceProvider.getIncentiveTypeDataServiceImplInstance()
						.getAll();
				System.out.println("Incentive type: ");
				experiment.setIncentiveTypeId(getUserInputAlternatives(incentiveTypes, "incentive type"));
			}

			if (process.multipleObjectivesPossible()) {
				// Exception control
				System.out.print("Set additional objectives? (0:no, 1:yes): ");
				int objectivesB = getUserInputInt(possibleInputsYesNo);

				if (objectivesB == 1) {

					ArrayList<Entity> objectivesAvailable = DataServiceProvider
							.getObjectiveTypeDataServiceImplInstance().getAll();
					ArrayList<ObjectiveWeight> objectivesSelected = new ArrayList<ObjectiveWeight>();

					while (objectivesB == 1) {
						// Exception control
						ObjectiveWeight objective = new ObjectiveWeight();
						objective.setObjectiveTypeId(getUserInputAlternatives(objectivesAvailable, "objective"));

						System.out.print("Choose objective weight (double 0.0): ");
						objective.setValue(getUserInputFloat(0, 100));
						objectivesSelected.add(objective);

						System.out.print("Set additional objectives? (0:no, 1:yes): ");
						objectivesB = getUserInputInt(possibleInputsYesNo);
					}

					experiment.setObjectives(objectivesSelected);
				}

			}

			SettingsProvider.setExperiment(experiment);
			this.definePeriodSettings(process);
		} else {

			repeatExperiment = true;
			ArrayList<Entity> experiments = DataServiceProvider.getSettingsDataServiceImplInstance()
					.getAllExperiments();

			// Exception control
			experiment = DataServiceProvider.getSettingsDataServiceImplInstance()
					.getExperimentById(getUserInputAlternatives(experiments, "experiment"));

			SettingsProvider.setExperiment(experiment);
			// Request settings depending on the process type
			process = ProcessProvider.getProcessByProcessTypeId(experiment.getProcessTypeId());
			SettingsProvider.setSettingRequest(process.getSettingRequest());
			SettingsProvider.setOutputs(process.getOutputs());
			PeriodSetting initialSetting = DataServiceProvider.getSettingsDataServiceImplInstance()
					.getInputPeriodSettingsInitialByExperiment(experiment.getId());
			SettingsProvider.setPeriodSetting(initialSetting);

			ArrayList<PeriodSetting> followers = new ArrayList<PeriodSetting>();
			if (experiment.getBookingPeriodNumber() > 1) {
				followers = DataServiceProvider.getSettingsDataServiceImplInstance()
						.getInputPeriodSettingFollowersByExperiment(experiment.getId());

			}
			SettingsProvider.setPeriodSettingFollowers(followers);
		}

		System.out.println("Number of repetitions:");
		SettingsProvider.setNoOfRepetitions(getUserInputInt(possibleInputsAll));

		// Reusing an experiment with only changing order request set is only
		// possible, if it is a one period process

		//TODO: Just ask for changing order request set if it is actually a primary input
		if (SettingsProvider.getNoOfRepetitions() > 1
				&& SettingsProvider.getPeriodSetting().getOrderRequestSetId() != null
				&& SettingsProvider.getPeriodSettingFollowers().size() == 0) {
			System.out.println("Does order request set change? (0:no, 1:yes):");

			if (getUserInputInt(possibleInputsYesNo) == 1) {

				System.out.println("Use all order request sets from the experiment that produced the selected one.");

				ArrayList<Integer> requestSetIds = DataServiceProvider.getSettingsDataServiceImplInstance()
						.getOrderRequestSetsPerExperiment(SettingsProvider.getPeriodSetting().getOrderRequestSetId());

				if (requestSetIds.size() != SettingsProvider.getNoOfRepetitions()) {
					SettingsProvider.setNoOfRepetitions(requestSetIds.size());
					System.out.println(
							"Adapted number of repetitions to available offer sets, which is: " + requestSetIds.size());
				}
				SettingsProvider.setOrderRequestSetsForRepetitions(requestSetIds);
			}
		}
		scanner.close();
		return process;
	}

	/**
	 * Defines settings that can change over periods
	 * 
	 * @param process
	 *            Relevant process that needs the setting information
	 */
	private void definePeriodSettings(IProcess process) {

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
			this.requestRelevantInitialSettingsFromUser(type, types.get(type), p1, scanner);
		}

		// Define general parameters that are variable per process, for
		// instance, alpha for exponential smoothing
		ArrayList<String> paraTypes = SettingsProvider.getSettingRequest().getParameters();
		for (int i = 0; i < paraTypes.size(); i++) {
			this.requestParameterValuesFromUser(paraTypes.get(i), p1, scanner);
		}

		SettingsProvider.setPeriodSetting(p1);

		ArrayList<PeriodSetting> periodSettingFollowers = new ArrayList<PeriodSetting>();

		int settingNumber = 1;
		while (settingNumber < SettingsProvider.getExperiment().getBookingPeriodNumber()) {
			PeriodSetting pFollower = new PeriodSetting();
			pFollower.setStartingPeriod(settingNumber++);

			Iterator<PeriodSettingType> typeIterator2 = types.keySet().iterator();
			while (typeIterator.hasNext()) {
				PeriodSettingType type = typeIterator2.next();
				this.requestRelevantFollowerSettingsFromUser(type, types.get(type), pFollower, scanner);
			}

			periodSettingFollowers.add(pFollower);
		}

		SettingsProvider.setPeriodSettingFollowers(periodSettingFollowers);

	}

	/**
	 * Defines the period setting for the respective type
	 * 
	 * @param type
	 *            Respective period setting type
	 * @param optional
	 *            Boolean if setting is optional
	 * @param p
	 *            Period setting to fill
	 * @param previousSetting
	 *            Previous period setting. Null if first setting.
	 * @param scanner
	 *            Scanner to read the input
	 */
	private void requestRelevantInitialSettingsFromUser(PeriodSettingType type, Boolean optional, PeriodSetting p,
			Scanner scanner) {

		// Exception control
		ArrayList<Integer> possibleInputsTemp = new ArrayList<Integer>();
		possibleInputsTemp.add(0);
		possibleInputsTemp.add(1);

		switch (type) {
		case ARRIVAL_PROBABILITY_DISTRIBUTION:
			if (optional) {
				System.out.println("Set arrival probability distribution? (0:no, 1:yes)");
				if (getUserInputInt(possibleInputsTemp) == 0) {
					break;
				}
			}
			this.requestArrivalProbabilityDistributionId(p, scanner);

			break;
		case TRAVELTIMESET:
			if (optional) {
				System.out.println("Set travel time set? (0:no, 1:yes)");
				if (getUserInputInt(possibleInputsTemp) == 0) {
					break;
				}
			}
			this.requestTravelTimeSetId(p, scanner);

			break;
		case DYNAMICPROGRAMMINGTREE:
			if (optional) {
				System.out.println("Set dynamic programming tree? (0:no, 1:yes)");
				if (getUserInputInt(possibleInputsTemp) == 0) {
					break;
				}
			}
			this.requestDynamicProgrammingTreeId(p, scanner);

			break;
		case DELIVERYAREASET:
			if (optional) {
				System.out.println("Set delivery area set? (0:no, 1:yes)");
				if (getUserInputInt(possibleInputsTemp) == 0) {
					break;
				}
			}
			this.requestDeliveryAreaSetId(p, scanner);

			break;
		case ALTERNATIVESET:
			if (optional) {
				System.out.println("Set alternative set? (0:no, 1:yes)");
				if (getUserInputInt(possibleInputsTemp) == 0) {
					break;
				}
			}
			this.requestAlternativeSetId(p, scanner);
			break;
		case CUSTOMERSET:
			if (optional) {
				System.out.println("Set customer set? (0:no, 1:yes)");
				if (getUserInputInt(possibleInputsTemp) == 0) {
					break;
				}
			}
			this.requestCustomerSetId(p, scanner);
			break;
		case SERVICESEGMENTWEIGHTING:
			if (optional) {
				System.out.println("Set service segment set? (0:no, 1:yes)");
				if (getUserInputInt(possibleInputsTemp) == 0) {
					break;
				}
			}
			this.requestServiceSegmentWeightingId(p, scanner);
			break;
		case ARRIVALPROCESS:
			if (optional) {
				System.out.println("Set arrival process? (0:no, 1:yes)");
				if (getUserInputInt(possibleInputsTemp) == 0) {
					break;
				}
			}
			this.requestArrivalProcessId(p, scanner);
			break;
		case DEMANDSEGMENTWEIGHTING:
			if (optional) {
				System.out.println("Set demand segment weighting? (0:no, 1:yes)");
				if (getUserInputInt(possibleInputsTemp) == 0) {
					break;
				}
			}
			this.requestDemandSegmentWeightingId(p, scanner);
			break;
		case DEMANDSEGMENTSET:
			if (optional) {
				System.out.println("Set demand segment set? (0:no, 1:yes)");
				if (getUserInputInt(possibleInputsTemp) == 0) {
					break;
				}
			}
			this.requestDemandSegmentSetId(p, scanner);
			break;
		case ORDERSET:
			if (optional) {
				System.out.println("Set order set? (0:no, 1:yes)");
				if (getUserInputInt(possibleInputsTemp) == 0) {
					break;
				}
			}
			this.requestOrderSetId(p, scanner);
			break;
		case ORDERREQUESTSET:
			if (optional) {
				System.out.println("Set order request set? (0:no, 1:yes)");
				if (getUserInputInt(possibleInputsTemp) == 0) {
					break;
				}
			}
			this.requestOrderRequestSetId(p, scanner);
			break;
		case LEARNING_ORDERREQUESTSET:
			if (optional) {
				System.out.println("Set learning order request sets? (0:no, 1:yes)");
				if (getUserInputInt(possibleInputsTemp) == 0) {
					break;
				}
			}
			this.requestOrderRequestSetsByExperimentId(p, scanner);
			break;
		case DEMANDFORECASTSET_VALUEBUCKETS:
			if (optional) {
				System.out.println("Set forcast (value buckets) set? (0:no, 1:yes)");
				if (getUserInputInt(possibleInputsTemp) == 0) {
					break;
				}
			}
			this.requestDemandForecastValueBucketsSetId(p, scanner);
			break;
		case DEMANDFORECASTSET_DEMANDSEGMENTS:
			if (optional) {
				System.out.println("Set forcast (demand segments) set? (0:no, 1:yes)");
				if (getUserInputInt(possibleInputsTemp) == 0) {
					break;
				}
			}
			this.requestDemandForecastSegmentsSetId(p, scanner);
			break;
		case CAPACITYSET:
			if (optional) {
				System.out.println("Set capacity set? (0:no, 1:yes)");
				if (getUserInputInt(possibleInputsTemp) == 0) {
					break;
				}
			}
			this.requestCapacitySetId(p, scanner);
			break;
		case CONTROLSET:
			if (optional) {
				System.out.println("Set control set? (0:no, 1:yes)");
				if (getUserInputInt(possibleInputsTemp) == 0) {
					break;
				}
			}
			this.requestControlSetId(p, scanner);
			break;
		case TIMEWINDOWSET:
			if (optional) {
				System.out.println("Set time window set? (0:no, 1:yes)");
				if (getUserInputInt(possibleInputsTemp) == 0) {
					break;
				}
			}
			this.requestTimeWindowSet(p, scanner);
			break;
		case HISTORICALORDERS:
			if (optional) {
				System.out.println("Set historical order set? (0:no, 1:yes)");
				if (getUserInputInt(possibleInputsTemp) == 0) {
					break;
				}
			}
			this.requestHistoricalOrderSet(p, scanner);
			break;
		case HISTORICALDELIVERIES:
			if (optional) {
				System.out.println("Set historical delivery set? (0:no, 1:yes)");
				if (getUserInputInt(possibleInputsTemp) == 0) {
					break;
				}
			}
			System.out.println("NOT WORKING!");
			break;
		case VALUEBUCKETSET:
			if (optional) {
				System.out.println("Set value bucket set? (0:no, 1:yes)");
				if (getUserInputInt(possibleInputsTemp) == 0) {
					break;
				}
			}
			this.requestValueBucketSet(p, scanner);
		case HISTORICALDEMANDFORECASTSET_VALUEBUCKETS:
			if (optional) {
				System.out.println("Set historical demand forecast set? (0:no, 1:yes)");
				if (getUserInputInt(possibleInputsTemp) == 0) {
					break;
				}
			}
			this.requestHistoricalDemandForecastValueBucketsSetId(p, scanner);
			break;
		case FINALROUTING:
			if (optional) {
				System.out.println("Set final routing? (0:no, 1:yes)");
				if (getUserInputInt(possibleInputsTemp) == 0) {
					break;
				}
			}
			this.requestFinalRoutingId(p, scanner);
			break;
		case INITIALROUTING:
			if (optional) {
				System.out.println("Set initial routing? (0:no, 1:yes)");
				if (getUserInputInt(possibleInputsTemp) == 0) {
					break;
				}
			}
			this.requestInitialRoutingId(p, scanner);
			break;
		case LEARNING_FINAL_ROUTING:
			if (optional) {
				System.out.println("Set learning routings? (0:no, 1:yes)");
				if (getUserInputInt(possibleInputsTemp) == 0) {
					break;
				}
			}
			this.requestRoutingsByExperimentId(p, scanner);
			break;
		case LEARNING_ORDER_SET:
			if (optional) {
				System.out.println("Set learning order sets? (0:no, 1:yes)");
				if (getUserInputInt(possibleInputsTemp) == 0) {
					break;
				}
			}
			this.requestLearningOrderSetsByExperimentId(p, scanner);
			break;
		case BENCHMARKING_FINAL_ROUTING:
			if (optional) {
				System.out.println("Set benchmarking routings? (0:no, 1:yes)");
				if (getUserInputInt(possibleInputsTemp) == 0) {
					break;
				}
			}
			this.requestBenchmarkingRoutingResults(p, scanner);
			break;
		case BENCHMARKING_ORDER_SET:
			if (optional) {
				System.out.println("Set benchmarking order sets? (0:no, 1:yes)");
				if (getUserInputInt(possibleInputsTemp) == 0) {
					break;
				}
			}
			this.requestBenchmarkingOrderSetResults(p, scanner);
			break;
		case VEHICLES:
			if (optional) {
				System.out.println("Set vehicles? (0:no, 1:yes)");
				if (getUserInputInt(possibleInputsTemp) == 0) {
					break;
				}
			}
			this.requestVehicles(p, scanner);
			break;
		case VEHICLE_ASSIGNMENT_SET:
			if (optional) {
				System.out.println("Set vehicle area assignment set? (0:no, 1:yes)");
				if (getUserInputInt(possibleInputsTemp) == 0) {
					break;
				}
			}
			this.requestVehicleAreaAssignment(p, scanner);
			break;
		case LINEAR_VALUE_FUNCTION_APPROXIMATION:
			if (optional) {
				System.out.println("Set linear value function model set? (0:no, 1:yes)");
				if (getUserInputInt(possibleInputsTemp) == 0) {
					break;
				}
			}
			this.requestLinearValueFunctionApproximation(p, scanner);
			break;
		default:
			break;
		}

	}

	/**
	 * For follow up period settings, only order requests have to be set (other
	 * settings are reused)
	 * 
	 * @param type
	 * @param optional
	 * @param pFollow
	 * @param scanner
	 */
	private void requestRelevantFollowerSettingsFromUser(PeriodSettingType type, Boolean optional,
			PeriodSetting pFollow, Scanner scanner) {

		ArrayList<Integer> possibleInnputsTemp = new ArrayList<Integer>();
		possibleInnputsTemp.add(0);
		possibleInnputsTemp.add(1);

		switch (type) {
		case ORDERREQUESTSET:
			if (optional) {
				System.out.println("Set order request set? (0:no, 1:yes)");
				if (getUserInputInt(possibleInnputsTemp) == 0) {
					break;
				}
			}
			this.requestOrderRequestSetFollowerId(pFollow, scanner);
			break;
		default:
			break;
		}

	}

	private void requestParameterValuesFromUser(String type, PeriodSetting p, Scanner scanner) {

		GeneralParameterValue paraValue = new GeneralParameterValue();

		System.out.print(type + ": ");

		// Exception Control Double
		paraValue.setValue(getUserInputFloat(0, 0));

		ArrayList<Entity> paraTypes = DataServiceProvider.getParameterTypeDataServiceImplInstance().getAll();

		for (int i = 0; i < paraTypes.size(); i++) {
			if (((ParameterType) paraTypes.get(i)).getName().equals(type)) {
				paraValue.setParameterType((ParameterType) paraTypes.get(i));
				paraValue.setParameterTypeId(paraValue.getParameterType().getId());
				break;
			}
		}
		p.addParameterValue(paraValue);

	}

	private void requestArrivalProbabilityDistributionId(PeriodSetting p, Scanner scanner) {

		if (p.getArrivalProbabilityDistributionId() == null) {

			System.out.println("Probability distributions: ");
			ArrayList<Entity> probabilityDistributions = DataServiceProvider.getProbabilityDistributionDataServiceImplInstance().getAll();

			p.setArrivalProbabilityDistributionId(getUserInputAlternatives(probabilityDistributions, "arrival probability distribution"));

		}
	}
	/**
	 * Defines the delivery area set as long as it is not already set in the
	 * period setting. Only sets from the current region are possible.
	 * 
	 * @param p
	 *            Respective period setting
	 * @param scanner
	 *            Scanner to read the input
	 */
	private void requestDeliveryAreaSetId(PeriodSetting p, Scanner scanner) {

		if (p.getDeliveryAreaSetId() == null) {

			System.out.println("Delivery area sets: ");
			ArrayList<SetEntity> deliveryAreaSets = DataServiceProvider.getDeliveryAreaDataServiceImplInstance()
					.getAllSetsByRegionId(SettingsProvider.getExperiment().getRegionId());

			p.setDeliveryAreaSetId(getUserInputAlternatives(deliveryAreaSets, "delivery area set"));

		}
	}

	/**
	 * Defines the time window set as long as it is not already set in the
	 * period setting.
	 * 
	 * @param p
	 *            Respective period setting
	 * @param scanner
	 *            Scanner to read the input
	 */
	private void requestTimeWindowSet(PeriodSetting p, Scanner scanner) {

		if (p.getTimeWindowSetId() == null) {
			System.out.println("Time window sets: ");
			ArrayList<SetEntity> timeWindowSets = DataServiceProvider.getTimeWindowDataServiceImplInstance()
					.getAllSets();

			p.setTimeWindowSetId(getUserInputAlternatives(timeWindowSets, "time window set"));
		}
	}

	/**
	 * Defines the alternative set as long as it is not already set in the
	 * period setting. A time window set needs to be defined first.
	 * 
	 * @param p
	 *            Respective period setting
	 * @param scanner
	 *            Scanner to read the input
	 */
	private void requestAlternativeSetId(PeriodSetting p, Scanner scanner) {

		if (p.getAlternativeSetId() == null) {

			// Define time window set
			this.requestTimeWindowSet(p, scanner);

			// Define alternative set
			System.out.println("Alternative sets: ");
			ArrayList<SetEntity> alternativeSets = DataServiceProvider.getAlternativeDataServiceImplInstance()
					.getAllSetsByTimeWindowSetId(p.getTimeWindowSetId());

			p.setAlternativeSetId(getUserInputAlternatives(alternativeSets, "alternative set"));
		}
	}

	/**
	 * Defines the service segment weighting and set as long as it is not
	 * already set in the period setting.
	 * 
	 * @param p
	 *            Respective period setting
	 * @param scanner
	 *            Scanner to read the input
	 */
	private void requestServiceSegmentWeightingId(PeriodSetting p, Scanner scanner) {

		if (p.getServiceSegmentWeightingId() == null) {

			System.out.println("Service time segment sets: ");
			// Define service time segment set
			ArrayList<SetEntity> serviceTimeSegmentSets = DataServiceProvider
					.getServiceTimeSegmentDataServiceImplInstance().getAllSets();

			Integer serviceTimeSegmentSetId = getUserInputAlternatives(serviceTimeSegmentSets,
					"service time segment set");

			// Define weighting
			System.out.println("Service time weightings sets: ");
			ArrayList<WeightingEntity> serviceTimeSegmentWeighting = DataServiceProvider
					.getServiceTimeSegmentDataServiceImplInstance().getAllWeightingsBySetId(serviceTimeSegmentSetId);

			p.setServiceSegmentWeightingId(getUserInputAlternatives(serviceTimeSegmentWeighting, "weighting"));
		}
	}

	/**
	 * Defines the demand segment weighting as long as it is not already set in
	 * the period setting.
	 * 
	 * @param p
	 *            Respective period setting
	 * @param scanner
	 *            Scanner to read the input
	 */
	private void requestDemandSegmentWeightingId(PeriodSetting p, Scanner scanner) {

		if (p.getDemandSegmentWeightingId() == null) {

			// The demand segment weighting depends on set

			this.requestDemandSegmentSetId(p, scanner);

			// Define weighting
			System.out.println("Demand segment weightings: ");
			ArrayList<WeightingEntity> demandSegmentWeighting = DataServiceProvider
					.getDemandSegmentDataServiceImplInstance().getAllWeightingsBySetId(p.getDemandSegmentSetId());

			p.setDemandSegmentWeightingId(getUserInputAlternatives(demandSegmentWeighting, "weighting"));
		}
	}

	/**
	 * Defines the dynamic programming tree as long as it is not already set in
	 * the period setting.
	 * 
	 * @param p
	 *            Respective period setting
	 * @param scanner
	 *            Scanner to read the input
	 */
	private void requestDynamicProgrammingTreeId(PeriodSetting p, Scanner scanner) {

		if (p.getDynamicProgrammingTreeId() == null) {

			this.requestDeliveryAreaSetId(p, scanner);
			this.requestCapacitySetId(p, scanner);
			this.requestDemandSegmentWeightingId(p, scanner);
			this.requestArrivalProcessId(p, scanner);

			// Define weighting
			System.out.println("Dynamic programming trees: ");
			ArrayList<Entity> trees = DataServiceProvider.getControlDataServiceImplInstance()
					.getAllDynamicProgrammingTreesByMultipleSelectionIds(
							SettingsProvider.getExperiment().getBookingPeriodLength(), p.getCapacitySetId(),
							p.getArrivalProcessId(), p.getDemandSegmentWeightingId(), p.getDeliveryAreaSetId());

			p.setDynamicProgrammingTreeId(getUserInputAlternatives(trees, "tree"));
		}
	}

	/**
	 * Defines the travel time set Id as long as it is not already set in the
	 * period setting.
	 * 
	 * @param p
	 *            Respective period setting
	 * @param scanner
	 *            Scanner to read the input
	 */
	private void requestTravelTimeSetId(PeriodSetting p, Scanner scanner) {

		if (p.getTravelTimeSetId() == null) {

			// Define weighting
			System.out.println("Travel time sets: ");
			ArrayList<SetEntity> travelTimeSet = DataServiceProvider.getTravelTimeDataServiceImplInstance()
					.getTravelTimeSetsByRegionId(SettingsProvider.getExperiment().getRegionId());

			p.setTravelTimeSetId(getUserInputAlternatives(travelTimeSet, "travel time set"));
		}
	}

	/**
	 * Defines the demand segment set as long as it is not already set in the
	 * period setting.
	 * 
	 * @param p
	 *            Respective period setting
	 * @param scanner
	 *            Scanner to read the input
	 */
	private void requestDemandSegmentSetId(PeriodSetting p, Scanner scanner) {

		if (p.getDemandSegmentSetId() == null) {

			// The demand segment set depends on the alternative set
			this.requestAlternativeSetId(p, scanner);

			// Define demand segment set
			System.out.println("Demand segment sets: ");
			ArrayList<SetEntity> demandSegmentSets = DataServiceProvider.getDemandSegmentDataServiceImplInstance()
					.getAllSetsByRegionAndAlternativeSetId(SettingsProvider.getExperiment().getRegionId(),
							p.getAlternativeSetId());

			Integer demandSegmentSetId = getUserInputAlternatives(demandSegmentSets,
					"demand segment set of current region and alternatives");
			p.setDemandSegmentSetId(demandSegmentSetId);
		}
	}

	/**
	 * Defines the arrival process as long as it is not already set in the
	 * period setting.
	 * 
	 * @param p
	 *            Respective period setting
	 * @param scanner
	 *            Scanner to read the input
	 */
	private void requestArrivalProcessId(PeriodSetting p, Scanner scanner) {

		if (p.getArrivalProcessId() == null) {
			System.out.println("Arrival processes: ");
			ArrayList<Entity> arrivalProcesses = DataServiceProvider.getArrivalProcessDataServiceImplInstance()
					.getAll();

			p.setArrivalProcessId(getUserInputAlternatives(arrivalProcesses, "arrival process"));
		}
	}

	/**
	 * Defines the capacity set as long as it is not already set in the period
	 * setting. Only sets from the current delivery area set and time window set
	 * are possible.
	 * 
	 * @param p
	 *            Respective period setting
	 * @param scanner
	 *            Scanner to read the input
	 */
	private void requestLinearValueFunctionApproximation(PeriodSetting p, Scanner scanner) {

		if (p.getValueFunctionModelSetId() == null) {

			this.requestDeliveryAreaSetId(p, scanner);

			this.requestTimeWindowSet(p, scanner);

			System.out.println("Linear value function approximation model sets: ");
			ArrayList<SetEntity> models = DataServiceProvider.getValueFunctionApproximationDataServiceImplInstance()
					.getAllSetsByDeliveryAreaSetAndTimeWindowSetId(p.getDeliveryAreaSetId(), p.getTimeWindowSetId(), 1);

			p.setValueFunctionModelSetId(getUserInputAlternatives(models, "value function approximation model"));
			;
		}
	}

	/**
	 * Defines the linear value function model as long as it is not already set
	 * in the period setting. Only from the current delivery area set and time
	 * window set and linear type are possible.
	 * 
	 * @param p
	 *            Respective period setting
	 * @param scanner
	 *            Scanner to read the input
	 */
	private void requestCapacitySetId(PeriodSetting p, Scanner scanner) {

		if (p.getCapacitySetId() == null) {

			// Capacities depend on delivery area set and time window set

			this.requestDeliveryAreaSetId(p, scanner);

			this.requestTimeWindowSet(p, scanner);
			System.out.println("Capacity sets: ");
			ArrayList<SetEntity> capacitySets = DataServiceProvider.getCapacityDataServiceImplInstance()
					.getAllSetsByDeliveryAreaSetAndTimeWindowSetId(p.getDeliveryAreaSetId(), p.getTimeWindowSetId());

			p.setCapacitySetId(getUserInputAlternatives(capacitySets, "capacity set"));
			;
		}
	}

	/**
	 * Defines the vehicle area assignment as long as it is not already set in
	 * the period setting. Only sets from the current delivery area set are
	 * possible.
	 * 
	 * @param p
	 *            Respective period setting
	 * @param scanner
	 *            Scanner to read the input
	 */
	private void requestVehicleAreaAssignment(PeriodSetting p, Scanner scanner) {

		if (p.getVehicleAssignmentSetId() == null) {

			// Capacities depend on delivery area set and time window set

			this.requestDeliveryAreaSetId(p, scanner);

			System.out.println("Vehicle area assignment sets: ");
			ArrayList<SetEntity> sets = DataServiceProvider.getVehicleAssignmentDataServiceImplInstance()
					.getAllSetsByDeliveryAreaSetId(p.getDeliveryAreaSetId());

			p.setVehicleAssignmentSetId(getUserInputAlternatives(sets, "vehicle area assignment set"));
			;
		}
	}

	/**
	 * Defines the control set as long as it is not already set in the period
	 * setting. Only sets from the current delivery area set and time window set
	 * and value bucket set are possible.
	 * 
	 * @param p
	 *            Respective period setting
	 * @param scanner
	 *            Scanner to read the input
	 */
	private void requestControlSetId(PeriodSetting p, Scanner scanner) {

		if (p.getControlSetId() == null) {

			// Controls depend on delivery area set and time window set and
			// value bucket set

			this.requestDeliveryAreaSetId(p, scanner);

			this.requestAlternativeSetId(p, scanner);

			this.requestValueBucketSet(p, scanner);
			System.out.println("Control sets: ");
			ArrayList<SetEntity> controlSets = DataServiceProvider.getControlDataServiceImplInstance()
					.getAllSetsByDeliveryAreaSetAndAlternativeSetAndValueBucketSetId(p.getDeliveryAreaSetId(),
							p.getAlternativeSetId(), p.getValueBucketSetId());

			p.setControlSetId(getUserInputAlternatives(controlSets, "control set"));
			;
		}
	}

	/**
	 * Defines the value bucket demand forecast set as long as it is not already
	 * set in the period setting. Only sets from the current delivery area set
	 * and time window set and value bucket set are possible.
	 * 
	 * @param p
	 *            Respective period setting
	 * @param scanner
	 *            Scanner to read the input
	 */
	private void requestDemandForecastValueBucketsSetId(PeriodSetting p, Scanner scanner) {

		if (p.getValueBucketForecastSetId() == null) {

			// Demand forecasts depend on delivery area set and time window set
			// and value bucket set

			this.requestDeliveryAreaSetId(p, scanner);

			this.requestTimeWindowSet(p, scanner);

			this.requestValueBucketSet(p, scanner);
			System.out.println("Value Bucket forecast sets: ");
			ArrayList<SetEntity> demandForecastSets = DataServiceProvider
					.getValueBucketForecastDataServiceImplInstance()
					.getAllSetsByDeliveryAreaSetAndTimeWindowSetAndValueBucketSetId(p.getDeliveryAreaSetId(),
							p.getTimeWindowSetId(), p.getValueBucketSetId());

			p.setValueBucketForecastSetId(
					getUserInputAlternatives(demandForecastSets, "demand forecast (value buckets) set"));
			;
		}
	}

	/**
	 * Defines the demand segment forecast set as long as it is not already set
	 * in the period setting. Only sets from the current delivery area set and
	 * demand segment set are possible.
	 * 
	 * @param p
	 *            Respective period setting
	 * @param scanner
	 *            Scanner to read the input
	 */
	private void requestDemandForecastSegmentsSetId(PeriodSetting p, Scanner scanner) {

		if (p.getDemandSegmentForecastSetId() == null) {

			// Demand forecasts depend on delivery area set and demand segment
			// set

			this.requestDeliveryAreaSetId(p, scanner);

			this.requestDemandSegmentSetId(p, scanner);

			System.out.println("Demand segment forecast sets: ");
			ArrayList<SetEntity> demandForecastSets = DataServiceProvider
					.getDemandSegmentForecastDataServiceImplInstance().getAllSetsByDeliveryAreaSetAndDemandSegmentSetId(
							p.getDeliveryAreaSetId(), p.getDemandSegmentSetId());

			p.setDemandSegmentForecastSetId(
					getUserInputAlternatives(demandForecastSets, "demand forecast (demand segments) set"));
			;
		}
	}

	/**
	 * Defines the initial routing as long as it is not already set in the
	 * period setting. Only sets from the forecasting set are possible.
	 * 
	 * @param p
	 *            Respective period setting
	 * @param scanner
	 *            Scanner to read the input
	 */
	private void requestInitialRoutingId(PeriodSetting p, Scanner scanner) {

		RoutingAssignment rAss = null;
		for (int i = 0; i < p.getRoutingAssignments().size(); i++) {
			if (p.getRoutingAssignments().get(i).getT() == -1)
				rAss = p.getRoutingAssignments().get(i);
		}

		if (rAss == null) {

			// Depends on demand forecasts

			this.requestTimeWindowSet(p, scanner);
			System.out.println("Initial routings: ");
			ArrayList<Routing> initialRoutings = DataServiceProvider.getRoutingDataServiceImplInstance()
					.getAllInitialRoutingsByTimeWindowSetId(p.getTimeWindowSetId());

			rAss = new RoutingAssignment();
			rAss.setRoutingId(getUserInputAlternatives(initialRoutings, "initial routing"));
			rAss.setT(-1);
			;
		}
	}

	/**
	 * Defines the final routing as long as it is not already set in the period
	 * setting. Only sets from the order set are possible.
	 * 
	 * @param p
	 *            Respective period setting
	 * @param scanner
	 *            Scanner to read the input
	 */
	private void requestFinalRoutingId(PeriodSetting p, Scanner scanner) {

		RoutingAssignment rAss = null;
		for (int i = 0; i < p.getRoutingAssignments().size(); i++) {
			if (p.getRoutingAssignments().get(i).getT() == -2)
				rAss = p.getRoutingAssignments().get(i);
		}

		if (rAss == null) {

			// Depends on order set and on depot location

			this.requestOrderSetId(p, scanner);
			System.out.println("Final routings: ");
			ArrayList<Routing> finalRoutings = DataServiceProvider.getRoutingDataServiceImplInstance()
					.getAllFinalRoutingsByOrderSetAndDepotId(p.getOrderRequestSetId(),
							SettingsProvider.getExperiment().getDepotId());

			rAss = new RoutingAssignment();
			rAss.setRoutingId(getUserInputAlternatives(finalRoutings, "final routing"));
			rAss.setT(-2);
			;
		}
	}

	/**
	 * Defines the historical forecast set as long as it is not already set in
	 * the period setting. Only sets from the current delivery area set and time
	 * window set and value bucket set are possible.
	 * 
	 * @param p
	 *            Respective period setting
	 * @param scanner
	 *            Scanner to read the input
	 */
	private void requestHistoricalDemandForecastValueBucketsSetId(PeriodSetting p, Scanner scanner) {

		if (p.getValueBucketForecastSetId() == null) {

			// Demand forecasts depend on delivery area set and time window set
			// and value bucket set

			this.requestDeliveryAreaSetId(p, scanner);

			this.requestTimeWindowSet(p, scanner);

			this.requestValueBucketSet(p, scanner);

			System.out.println("Demand forecast sets: ");
			ArrayList<SetEntity> demandForecastSets = DataServiceProvider
					.getValueBucketForecastDataServiceImplInstance()
					.getAllSetsByDeliveryAreaSetAndTimeWindowSetAndValueBucketSetId(p.getDeliveryAreaSetId(),
							p.getTimeWindowSetId(), p.getValueBucketSetId());

			p.setHistoricalDemandForecastValueBucketsSetId(
					getUserInputAlternatives(demandForecastSets, "demand forecast (value buckets) set"));
			;
		}
	}

	/**
	 * Defines the customer set as long as it is not already set in the period
	 * setting. Only sets from the current demand segment set are possible.
	 * 
	 * @param p
	 *            Respective period setting
	 * @param scanner
	 *            Scanner to read the input
	 */
	private void requestCustomerSetId(PeriodSetting p, Scanner scanner) {

		if (p.getCustomerSetId() == null) {

			// Customers depend on demand segment set

			this.requestDemandSegmentWeightingId(p, scanner);
			System.out.println("Customer sets: ");
			ArrayList<SetEntity> customerSets = DataServiceProvider.getCustomerDataServiceImplInstance()
					.getAllByOriginalDemandSegmentSetId(p.getDemandSegmentWeightingId());

			p.setCustomerSetId(getUserInputAlternatives(customerSets, "customer set"));
			;
		}
	}

	/**
	 * Defines the order request set as long as it is not already set in the
	 * period setting. Only sets from the current customer set are possible.
	 * 
	 * @param p
	 *            Respective period setting
	 * @param scanner
	 *            Scanner to read the input
	 */
	private void requestOrderRequestSetId(PeriodSetting p, Scanner scanner) {

		if (p.getOrderRequestSetId() == null) {

			ArrayList<SetEntity> orderRequestSets = new ArrayList<SetEntity>();

			// Order requests depend on customer set
			if (p.getCustomerSetId() != null) {
				
				orderRequestSets = DataServiceProvider.getOrderRequestDataServiceImplInstance()
						.getAllByCustomerSetId(p.getCustomerSetId());

			} else {
				// Has to fit to alternatives
				this.requestAlternativeSetId(p, scanner);

				// Has to fit to period length
				if (SettingsProvider.getExperiment().getBookingPeriodLength() != null) {
					
					//As there can be many results, first let choose experiment and set only afterwards
					ArrayList<Experiment> experiments = DataServiceProvider.getOrderRequestDataServiceImplInstance().getAllExperimentsWithOrderRequestSetOutputByAlternativeSetIdAndOrderHorizonLength(p.getAlternativeSetId(), SettingsProvider.getExperiment().getBookingPeriodLength());
					int expId = getUserInputAlternatives(experiments, "experiment with order request set output");
					orderRequestSets = (ArrayList<SetEntity>) DataServiceProvider.getOrderRequestDataServiceImplInstance()
							.getAllOrderRequestSetsByExperimentId(expId);
				} else {
					ArrayList<Experiment> experiments = DataServiceProvider.getOrderRequestDataServiceImplInstance().getAllExperimentsWithOrderRequestSetOutputByAlternativeSetId(p.getAlternativeSetId());
					int expId = getUserInputAlternatives(experiments, "experiment with order request set output");
					orderRequestSets = (ArrayList<SetEntity>) DataServiceProvider.getOrderRequestDataServiceImplInstance()
							.getAllOrderRequestSetsByExperimentId(expId);
				}

			}
			System.out.println("Order request sets: ");

			p.setOrderRequestSetId(getUserInputAlternatives(orderRequestSets, "order request set"));

			// Set fitting customer set
			if (p.getCustomerSetId() == null) {
				OrderRequestSet chosenSet = (OrderRequestSet) DataServiceProvider
						.getOrderRequestDataServiceImplInstance().getSetById(p.getOrderRequestSetId());
				p.setCustomerSetId(chosenSet.getCustomerSetId());
			}
			;
		}
	}

	/**
	 * Defines the tuple of order request sets as long as it is not already set
	 * in the period setting. Only sets from the current demand segment set are
	 * possible.
	 * 
	 * @param p
	 *            Respective period setting
	 * @param scanner
	 *            Scanner to read the input
	 */
	private void requestOrderRequestSetsByExperimentId(PeriodSetting p, Scanner scanner) {

		if (p.getLearningOutputRequestsExperimentId() == null) {

			ArrayList<Experiment> experiments = new ArrayList<Experiment>();

			// Has to fit to demand structure
			this.requestDemandSegmentSetId(p, scanner);

			// Has to fit to period length
			if (SettingsProvider.getExperiment().getBookingPeriodLength() != null) {
				experiments = DataServiceProvider.getOrderRequestDataServiceImplInstance()
						.getAllExperimentsWithOrderRequestSetOutputByDemandSegmentSetIdAndBookingHorizonLength(
								p.getDemandSegmentSetId(), SettingsProvider.getExperiment().getBookingPeriodLength());
			} else {

				experiments = DataServiceProvider.getOrderRequestDataServiceImplInstance()
						.getAllExperimentsWithOrderRequestSetOutputByDemandSegmentSetId(p.getDemandSegmentSetId());
			}

			System.out.println("Experiments: ");

			p.setLearningOutputRequestsExperimentId(
					getUserInputAlternatives(experiments, "experiments with order request sets"));

		}
	}

	/**
	 * Defines the tuple of routings as long as it is not already set in the
	 * period setting.
	 * 
	 * @param p
	 *            Respective period setting
	 * @param scanner
	 *            Scanner to read the input
	 */
	private void requestRoutingsByExperimentId(PeriodSetting p, Scanner scanner) {

		if (p.getLearningOutputFinalRoutingsExperimentIds() == null) {

			// Has to fit to demand segment set
			this.requestDemandSegmentSetId(p, scanner);

			ArrayList<Experiment> experiments = new ArrayList<Experiment>();

			experiments = DataServiceProvider.getRoutingDataServiceImplInstance()
					.getAllNonCopyExperimentsWithFinalRoutingOutputByDemandSegmentSetId(p.getDemandSegmentSetId());

			ArrayList<Integer> possibleInputsTemp = new ArrayList<Integer>();
			possibleInputsTemp.add(0);
			possibleInputsTemp.add(1);

			
			System.out.println("Experiments: ");
			int additionalExperiment =1;
			ArrayList<Integer> experimentIds = new ArrayList<Integer>();
			while(additionalExperiment==1){
				int firstExperiment = getUserInputAlternatives(experiments, "experiments with routing outputs");
				experimentIds.add(firstExperiment);
				experimentIds.addAll(DataServiceProvider.getSettingsDataServiceImplInstance().getAllCopyExperimentsPerExperiment(firstExperiment));
				System.out.println("Set additional experiment? (0:no, 1:yes)");
				additionalExperiment=getUserInputInt(possibleInputsTemp);
			}

			p.setLearningOutputFinalRoutingsExperimentIds(experimentIds);

		}
	}
	
	/**
	 * Defines the tuple of order set as long as it is not already set in the
	 * period setting.
	 * 
	 * @param p
	 *            Respective period setting
	 * @param scanner
	 *            Scanner to read the input
	 */
	private void requestLearningOrderSetsByExperimentId(PeriodSetting p, Scanner scanner) {

		if (p.getLearningOutputOrderSetsExperimentIds() == null) {

			// Has to fit to demand segment set
			this.requestDemandSegmentSetId(p, scanner);

			ArrayList<Experiment> experiments = new ArrayList<Experiment>();

			experiments = DataServiceProvider.getOrderDataServiceImplInstance()
					.getAllNonCopyExperimentsWithOrderSetOutputByDemandSegmentSetId(p.getDemandSegmentSetId());

			ArrayList<Integer> possibleInputsTemp = new ArrayList<Integer>();
			possibleInputsTemp.add(0);
			possibleInputsTemp.add(1);

			
			System.out.println("Experiments: ");
			int additionalExperiment =1;
			ArrayList<Integer> experimentIds = new ArrayList<Integer>();
			while(additionalExperiment==1){
				int firstExperiment = getUserInputAlternatives(experiments, "experiments with order set outputs");
				experimentIds.add(firstExperiment);
				experimentIds.addAll(DataServiceProvider.getSettingsDataServiceImplInstance().getAllCopyExperimentsPerExperiment(firstExperiment));
				System.out.println("Set additional experiment? (0:no, 1:yes)");
				additionalExperiment=getUserInputInt(possibleInputsTemp);
			}

			p.setLearningOutputOrderSetsExperimentIds(experimentIds);

		}
	}
	
	private void requestBenchmarkingRoutingResults(PeriodSetting p, Scanner scanner) {

		if (p.getBenchmarkingOutputFinalRoutingsExperimentIds() == null) {

			// Has to fit to demand segment set
			this.requestDemandSegmentSetId(p, scanner);

			ArrayList<Experiment> experiments = new ArrayList<Experiment>();

			experiments = DataServiceProvider.getRoutingDataServiceImplInstance()
					.getAllNonCopyExperimentsWithFinalRoutingOutputByDemandSegmentSetId(p.getDemandSegmentSetId());

			ArrayList<Integer> possibleInputsTemp = new ArrayList<Integer>();
			possibleInputsTemp.add(0);
			possibleInputsTemp.add(1);

			
			System.out.println("Experiments: ");
			int additionalExperiment =1;
			ArrayList<Integer> experimentIds = new ArrayList<Integer>();
			while(additionalExperiment==1){
				int firstExperiment = getUserInputAlternatives(experiments, "experiments with routing outputs for benchmarking");
				experimentIds.add(firstExperiment);
				experimentIds.addAll(DataServiceProvider.getSettingsDataServiceImplInstance().getAllCopyExperimentsPerExperiment(firstExperiment));
				System.out.println("Set additional experiment? (0:no, 1:yes)");
				additionalExperiment=getUserInputInt(possibleInputsTemp);
			}

			p.setBenchmarkingOutputFinalRoutingsExperimentIds(experimentIds);

		}
	}
	
	private void requestBenchmarkingOrderSetResults(PeriodSetting p, Scanner scanner) {

		if (p.getBenchmarkingOutputOrderSetsExperimentIds() == null) {

			// Has to fit to demand segment set
			this.requestDemandSegmentSetId(p, scanner);

			ArrayList<Experiment> experiments = new ArrayList<Experiment>();

			experiments = DataServiceProvider.getOrderDataServiceImplInstance()
					.getAllNonCopyExperimentsWithOrderSetOutputByDemandSegmentSetId(p.getDemandSegmentSetId());

			ArrayList<Integer> possibleInputsTemp = new ArrayList<Integer>();
			possibleInputsTemp.add(0);
			possibleInputsTemp.add(1);

			
			System.out.println("Experiments: ");
			int additionalExperiment =1;
			ArrayList<Integer> experimentIds = new ArrayList<Integer>();
			while(additionalExperiment==1){
				int firstExperiment = getUserInputAlternatives(experiments, "experiments with order set outputs for benchmarking");
				experimentIds.add(firstExperiment);
				experimentIds.addAll(DataServiceProvider.getSettingsDataServiceImplInstance().getAllCopyExperimentsPerExperiment(firstExperiment));
				System.out.println("Set additional experiment? (0:no, 1:yes)");
				additionalExperiment=getUserInputInt(possibleInputsTemp);
			}

			p.setBenchmarkingOutputOrderSetsExperimentIds(experimentIds);

		}
	}

	/**
	 * Defines the order request set as long as it is not already set in the
	 * period setting follower. Only sets fitting to the initial settings are
	 * possible
	 * 
	 * @param p
	 *            Respective period setting follower
	 * @param scanner
	 *            Scanner to read the input
	 */
	private void requestOrderRequestSetFollowerId(PeriodSetting p, Scanner scanner) {

		if (p.getOrderRequestSetId() == null) {

			ArrayList<SetEntity> orderRequestSets = new ArrayList<SetEntity>();

			// Has to fit to alternatives
			Integer alternativeSetId = SettingsProvider.getPeriodSetting().getAlternativeSetId();

			// Has to fit to period length
			if (SettingsProvider.getExperiment().getBookingPeriodLength() != null) {
				orderRequestSets = DataServiceProvider.getOrderRequestDataServiceImplInstance()
						.getAllByBookingPeriodLengthAndAlternativeSetId(
								SettingsProvider.getExperiment().getBookingPeriodLength(), alternativeSetId);
			} else {

				orderRequestSets = DataServiceProvider.getOrderRequestDataServiceImplInstance()
						.getAllByAlternativeSetId(alternativeSetId);
			}

			System.out.println("Order request sets: ");

			p.setOrderRequestSetId(getUserInputAlternatives(orderRequestSets,
					"order request set (id) for period " + p.getStartingPeriod()));
		}
	}

	/**
	 * Defines the order set as long as it is not already set in the period
	 * setting. Only sets from the current order request set are possible.
	 * 
	 * @param p
	 *            Respective period setting
	 * @param scanner
	 *            Scanner to read the input
	 */
	private void requestOrderSetId(PeriodSetting p, Scanner scanner) {

		if (p.getOrderSetId() == null) {

			// Orders depend on order request set
			this.requestOrderRequestSetId(p, scanner);
			System.out.println("Order sets: ");
			ArrayList<SetEntity> orderSets = DataServiceProvider.getOrderDataServiceImplInstance()
					.getAllByOrderRequestSetId(p.getOrderRequestSetId());
			p.setOrderSetId(getUserInputAlternatives(orderSets, "order set"));
			;
		}
	}

	/**
	 * Defines the order set that represent the historical orders if not yet
	 * set.
	 * 
	 * @param p
	 *            Respective period setting
	 * @param scanner
	 *            Scanner to read the input
	 */
	private void requestHistoricalOrderSet(PeriodSetting p, Scanner scanner) {

		if (p.getHistoricalOrderSetId() == null) {

			ArrayList<SetEntity> historicalOrderSets = DataServiceProvider.getOrderDataServiceImplInstance()
					.getAllSets();
			System.out.println("Order sets: ");
			p.setHistoricalOrderSetId(
					getUserInputAlternatives(historicalOrderSets, "order set as historical order data"));
			;
		}

	}

	/**
	 * Defines the value bucket set as long as it is not already set in the
	 * period setting.
	 * 
	 * @param p
	 *            Respective period setting
	 * @param scanner
	 *            Scanner to read the input
	 */
	private void requestValueBucketSet(PeriodSetting p, Scanner scanner) {

		if (p.getValueBucketSetId() == null) {

			ArrayList<SetEntity> valueBucketSets = DataServiceProvider.getValueBucketDataServiceImplInstance()
					.getAllSets();
			System.out.println("Value bucket sets: ");
			p.setValueBucketSetId(getUserInputAlternatives(valueBucketSets, "value bucket set"));
			;
		}
	}

	private void requestVehicles(PeriodSetting p, Scanner scanner) {

		// if (p.getVehicles() == null) {
		System.out.print("Choose overall no of vehicles (int): ");
		int noVehicles = getUserInputInt(new ArrayList<Integer>());

		ArrayList<Entity> vehicleTypes = DataServiceProvider.getVehicleTypeDataServiceImplInstance().getAll();

		while (noVehicles > 0) {
			Vehicle vehicle = new Vehicle();
			int typeId = getUserInputAlternatives(vehicleTypes, "vehicle type");

			// vehicles in sum = noVehicles
			vehicle.setVehicleTypeId(typeId);
			System.out.print("Choose number of vehicles for this type: ");

			ArrayList<Integer> possibleInputsTemp = new ArrayList<Integer>();
			for (int i = 1; i <= noVehicles; i++) {
				possibleInputsTemp.add(i);
			}

			int no = getUserInputInt(possibleInputsTemp);
			vehicle.setVehicleNo(no);
			p.addVehicle(vehicle);
			noVehicles = noVehicles - no;

		}
		// }
	}

	/**
	 * Read all possible alternatives from the data bank, and let the user to
	 * choose one. If the user has given a wrong Id, the user will be forced to
	 * choose again.
	 * 
	 * @param list
	 *            all possible alternative to choose (from data bank)
	 * @param listName
	 *            the name of the alternatives
	 */
	private int getUserInputAlternatives(ArrayList<? extends Entity> list, String listName) {
		try {
			int choosenId = 0;
			for (int i = 0; i < list.size(); i++) {
				System.out.println(list.get(i).toString());
			}
			System.out.print("Choose " + listName + " (id): ");

			choosenId = scanner.nextInt();
			Boolean inList = false;

			while (!inList) {
				for (int i = 0; i < list.size(); i++) {
					int currentId = list.get(i).getId();
					if (choosenId == currentId)
						inList = true;
				}
				if (!inList)
					System.out.print("Choose " + listName + " (id): Please choose a valid ID!\n");
				if (!inList)
					choosenId = scanner.nextInt();
			}
			return choosenId;

		} catch (Exception e) {
			System.out.print("Choose " + listName + " (id): Please choose a valid ID!\n");
			scanner.next();
			return getUserInputAlternatives(list, listName);
		}
	}

	/**
	 * Get a integer given by user. If the integer is not fit(not in the list),
	 * the user has to input a new integer. If the arraylist is empty, all
	 * integer is acceptable.
	 * 
	 * @param list
	 *            all fit integer in a arraylist
	 */
	private int getUserInputInt(ArrayList<Integer> list) {

		try {
			int choosenId = scanner.nextInt();
			Boolean NumberOK = false;

			while (!NumberOK) {
				if (list.isEmpty() && choosenId > 0)
					NumberOK = true;
				for (int i = 0; i < list.size(); i++) {
					int currentId = (Integer) list.get(i);
					if (choosenId == currentId)
						NumberOK = true;
				}

				if (!NumberOK)
					System.out.print("Please provide a valid integer!\n");
				if (!NumberOK)
					choosenId = scanner.nextInt();
			}
			return choosenId;

		} catch (Exception e) {
			System.out.print("Please provide a valid integer!\n");
			scanner.next();
			return getUserInputInt(list);
		}

	}

	/**
	 * Get a double given by user. If the double is not in the range of [a,b],
	 * the user has to input a new double. If the range is [0,0], all positive
	 * double is acceptable.
	 * 
	 * @param a
	 *            the left side of the range
	 * @param b
	 *            the right side of the range
	 */
	private Double getUserInputFloat(double a, double b) {

		try {
			Double givenFloat = scanner.nextDouble();
			Boolean NumberOK = false;

			while (!NumberOK) {
				if (a == 0 && b == 0 && givenFloat > 0) {
					NumberOK = true;
				} else if (givenFloat <= b && givenFloat >= a) {
					NumberOK = true;
				}

				if (!NumberOK)
					System.out.print("Please provide a valid double!!\n");
				if (!NumberOK)
					givenFloat = scanner.nextDouble();
			}
			return givenFloat;

		} catch (Exception e) {
			System.out.print("Please provide a valid double!\n");
			scanner.next();
			return getUserInputFloat(a, b);
		}

	}

}
