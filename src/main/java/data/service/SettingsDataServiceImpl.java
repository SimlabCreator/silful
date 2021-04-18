package data.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;

import data.entity.Entity;
import data.entity.Experiment;
import data.entity.GeneralAtomicOutputValue;
import data.entity.GeneralParameterValue;
import data.entity.Kpi;
import data.entity.ObjectiveWeight;
import data.entity.PeriodSetting;
import data.entity.RoutingAssignment;
import data.entity.Run;
import data.entity.Vehicle;
import data.mapper.ExperimentMapper;
import data.mapper.GeneralParameterMapper;
import data.mapper.InputPeriodSettingMapper;
import data.mapper.ObjectiveWeightMapper;
import data.mapper.RoutingAssignmentMapper;
import data.mapper.VehicleMapper;
import data.utility.PeriodSettingType;
import data.utility.SettingRequest;
import logic.utility.SettingsProvider;

/**
 * Implementation for MySQL database
 * 
 * @author M. Lang
 *
 */
public class SettingsDataServiceImpl extends SettingsDataService {

	ArrayList<Entity> experiments;

	@Override
	public ArrayList<Entity> getAllExperiments() {
		if (this.experiments == null) {

			experiments = DataLoadService.loadAllFromClass("experiment", new ExperimentMapper(), jdbcTemplate);
		}
		return this.experiments;
	}

	@Override
	public Experiment getExperimentById(int expId) {

		Experiment experiment = null;
		if (this.experiments == null) {
			experiment = (Experiment) DataLoadService.loadById("experiment", "exp_id", expId, new ExperimentMapper(),
					jdbcTemplate);
		} else {
			for (int i = 0; i < this.experiments.size(); i++) {
				if (((Experiment) this.experiments.get(i)).getId() == expId) {
					experiment = (Experiment) this.experiments.get(i);
					break;
				}
			}
		}

		return experiment;
	}

	@Override
	public PeriodSetting getInputPeriodSettingsInitialByExperiment(int expId) {

		// Load all settings of the first period

		/// Load all elements that can be 0 or 1 (e.g. at most one alternative
		/// set, at most one delivery area set, ...)

		String sql = "SELECT eVa.exp_as_as, eVar.exp_arp_arp, eVca.exp_cas_cas, eVco.exp_cos_cos, eVcu.exp_cs_cs, eVd.exp_das_das, eVvf.exp_vfs_vfs,"
				+ "eVdf.exp_dfs_dfs, eVdfs.exp_dss_dss, eVdfw.exp_dsw_dsw, eVor.exp_ors_ors, eVs.exp_sss_sss, eVsw.exp_ssw_ssw, eVt.exp_tws_tws, "
				+ "eVv.exp_vbs_vbs, eVo.exp_os_os, eVoH.exp_os_os AS exp_os_os_h, eVdpt.exp_dpt_dpt, eVtt.exp_tts_tts, eVva.exp_vas_vas, "
				+ "eVle.ele_exp_learning_input_experiment, eVvfa.exp_vfa_vfa,eVapd.exp_apd_apd"
				+ "FROM (SELECT exp_id FROM "+SettingsProvider.database+".experiment WHERE exp_id=?) AS exp "
				+ "LEFT JOIN (SELECT * FROM "+SettingsProvider.database+".r_experiment_v_alternative_set WHERE exp_as_period=0) AS eVa ON (eVa.exp_as_exp=exp.exp_id) "
				+ "LEFT JOIN (SELECT * FROM "+SettingsProvider.database+".r_experiment_v_arrival_process WHERE exp_arp_period=0) AS eVar ON (eVar.exp_arp_exp=exp.exp_id) "
				+ "LEFT JOIN (SELECT * FROM "+SettingsProvider.database+".r_experiment_v_capacity_set WHERE exp_cas_period=0) AS eVca ON (eVca.exp_cas_exp=exp.exp_id) "
				+ "LEFT JOIN (SELECT * FROM "+SettingsProvider.database+".r_experiment_v_control_set WHERE exp_cos_period=0) AS eVco ON (eVco.exp_cos_exp=exp.exp_id) "
				+ "LEFT JOIN (SELECT * FROM "+SettingsProvider.database+".r_experiment_v_customer_set WHERE exp_cs_period=0) AS eVcu ON (eVcu.exp_cs_exp=exp.exp_id) "
				+ "LEFT JOIN (SELECT * FROM "+SettingsProvider.database+".r_experiment_v_delivery_area_set WHERE exp_das_period=0) AS eVd ON (eVd.exp_das_exp=exp.exp_id) "
				+ "LEFT JOIN (SELECT * FROM "+SettingsProvider.database+".r_experiment_v_value_bucket_forecast_set WHERE exp_vfs_period=0) AS eVvf ON (eVvf.exp_vfs_exp=exp.exp_id) "
				+ "LEFT JOIN (SELECT * FROM "+SettingsProvider.database+".r_experiment_v_demand_segment_forecast_set WHERE exp_dfs_period=0) AS eVdf ON (eVdf.exp_dfs_exp=exp.exp_id) "
				+ "LEFT JOIN (SELECT * FROM "+SettingsProvider.database+".r_experiment_v_demand_segment_set WHERE exp_dss_period=0) AS eVdfs ON (eVdfs.exp_dss_exp=exp.exp_id) "
				+ "LEFT JOIN (SELECT * FROM "+SettingsProvider.database+".r_experiment_v_demand_segment_weighting WHERE exp_dsw_period=0) AS eVdfw ON (eVdfw.exp_dsw_exp=exp.exp_id) "
				+ "LEFT JOIN (SELECT * FROM "+SettingsProvider.database+".r_experiment_v_order_request_set WHERE exp_ors_period=0) AS eVor ON (eVor.exp_ors_exp=exp.exp_id) "
				+ "LEFT JOIN (SELECT * FROM "+SettingsProvider.database+".r_experiment_v_service_segment_set WHERE exp_sss_period=0) AS eVs ON (eVs.exp_sss_exp=exp.exp_id) "
				+ "LEFT JOIN (SELECT * FROM "+SettingsProvider.database+".r_experiment_v_service_segment_weighting WHERE exp_ssw_period=0) AS eVsw ON (eVsw.exp_ssw_exp=exp.exp_id) "
				+ "LEFT JOIN (SELECT * FROM "+SettingsProvider.database+".r_experiment_v_time_window_set WHERE exp_tws_period=0) AS eVt ON (eVt.exp_tws_exp=exp.exp_id) "
				+ "LEFT JOIN (SELECT * FROM "+SettingsProvider.database+".r_experiment_v_value_bucket_set WHERE exp_vbs_period=0) AS eVv ON (eVv.exp_vbs_exp=exp.exp_id) "
				+ "LEFT JOIN (SELECT * FROM "+SettingsProvider.database+".r_experiment_v_order_set WHERE exp_os_period=0 AND exp_os_historical=0) AS eVo ON (eVo.exp_os_exp=exp.exp_id) "
				+ "LEFT JOIN (SELECT * FROM "+SettingsProvider.database+".r_experiment_v_order_set WHERE exp_os_period=0 AND exp_os_historical=1) AS eVoH ON (eVoH.exp_os_exp=exp.exp_id) "
				+ "LEFT JOIN (SELECT * FROM "+SettingsProvider.database+".r_experiment_v_dynamic_programming_tree WHERE exp_dpt_period=0 ) AS eVdpt ON (eVdpt.exp_dpt_exp=exp.exp_id) "
				+ "LEFT JOIN (SELECT * FROM "+SettingsProvider.database+".r_experiment_v_travel_time_set WHERE exp_tts_period=0) AS eVtt ON (eVtt.exp_tts_exp=exp.exp_id) "
				+ "LEFT JOIN (SELECT * FROM "+SettingsProvider.database+".r_experiment_v_vehicle_area_assignment_set WHERE exp_vas_period=0) AS eVva ON (eVva.exp_vas_exp=exp.exp_id) "
				+ "LEFT JOIN (SELECT * FROM "+SettingsProvider.database+".r_experiment_v_learning_experiment WHERE ele_exp_period=0 AND ele_exp_requests=1) AS eVle ON (eVle.ele_exp_experiment=exp.exp_id) "
				+ "LEFT JOIN (SELECT * FROM "+SettingsProvider.database+".r_experiment_v_value_function_approximation_model_set WHERE exp_vfa_period=0) AS eVvfa ON (eVvfa.exp_vfa_exp=exp.exp_id) "
				+ "LEFT JOIN (SELECT * FROM "+SettingsProvider.database+".r_experiment_v_arrival_probability_distribution WHERE exp_apd_period=0) AS eVapd ON (eVapd.exp_apd_exp=exp.exp_id) ";

		ArrayList<PeriodSetting> entities = (ArrayList<PeriodSetting>) DataLoadService
				.loadComplexPreparedStatementMultipleEntities(sql, new Object[] { expId },
						new InputPeriodSettingMapper(), jdbcTemplate);

		/// Load Parameters
		ArrayList<GeneralParameterValue> parameters = this.getParameterValues(expId);

		ArrayList<RoutingAssignment> routingAssignments = this.getRoutingAssignments(expId);
		ArrayList<Vehicle> vehicles = this.getVehicles(expId);
		ArrayList<Integer> learningRoutings = this.getLearningRoutingExperimentIds(expId);
		ArrayList<Integer> benchmarkingRoutings = this.getBenchmarkingRoutingExperimentIds(expId);
		ArrayList<Integer> learningOrderSets = this.getLearningOrderSetExperimentIds(expId);
		ArrayList<Integer> benchmarkingOrderSets = this.getBenchmarkingOrderSetExperimentIds(expId);
		// Combine
		PeriodSetting initialSetting = entities.get(0);
		initialSetting.setParameterValues(parameters);
		initialSetting.setRoutingAssignments(routingAssignments);
		initialSetting.setVehicles(vehicles);
		initialSetting.setStartingPeriod(0);
		initialSetting.setLearningOutputFinalRoutingsExperimentIds(learningRoutings);
		initialSetting.setBenchmarkingOutputFinalRoutingsExperimentIds(benchmarkingRoutings);
		initialSetting.setLearningOutputOrderSetsExperimentIds(learningOrderSets);
		initialSetting.setBenchmarkingOutputOrderSetsExperimentIds(benchmarkingOrderSets);
		return initialSetting;
	}

	private ArrayList<Integer> getLearningRoutingExperimentIds(int expId) {
		ArrayList<Integer> experimentIds = DataLoadService.loadListOfIds(
				"SELECT ele_exp_learning_input_experiment FROM r_experiment_v_learning_experiment WHERE ele_exp_routings=1 and ele_exp_experiment=?",
				new Object[] { expId }, jdbcTemplate);
		return experimentIds;
	}
	
	private ArrayList<Integer> getLearningOrderSetExperimentIds(int expId) {
		ArrayList<Integer> experimentIds = DataLoadService.loadListOfIds(
				"SELECT ele_exp_learning_input_experiment FROM r_experiment_v_learning_experiment WHERE ele_exp_order_sets=1 and ele_exp_experiment=?",
				new Object[] { expId }, jdbcTemplate);
		return experimentIds;
	}
	
	private ArrayList<Integer> getBenchmarkingRoutingExperimentIds(int expId) {
		ArrayList<Integer> experimentIds = DataLoadService.loadListOfIds(
				"SELECT ele_exp_learning_input_experiment FROM r_experiment_v_learning_experiment WHERE ele_exp_routings_benchmarking=1 and ele_exp_experiment=?",
				new Object[] { expId }, jdbcTemplate);
		return experimentIds;
	}
	
	private ArrayList<Integer> getBenchmarkingOrderSetExperimentIds(int expId) {
		ArrayList<Integer> experimentIds = DataLoadService.loadListOfIds(
				"SELECT ele_exp_learning_input_experiment FROM r_experiment_v_learning_experiment WHERE ele_exp_order_sets_benchmarking=1 and ele_exp_experiment=?",
				new Object[] { expId }, jdbcTemplate);
		return experimentIds;
	}

	@Override
	public ArrayList<PeriodSetting> getInputPeriodSettingFollowersByExperiment(int expId) {
		// For now, it can only be order requests

		String sql = "SELECT * FROM (SELECT exp_id FROM "+SettingsProvider.database+".experiment WHERE exp_id=?) AS exp"
				+ "LEFT JOIN (SELECT * FROM "+SettingsProvider.database+".r_experiment_v_order_request_set WHERE exp_ors_period>0) AS eVor ON (eVor.exp_ors_exp=exp.exp_id)";

		ArrayList<PeriodSetting> entities = (ArrayList<PeriodSetting>) DataLoadService
				.loadComplexPreparedStatementMultipleEntities(sql, new Object[] { expId },
						new InputPeriodSettingMapper(), jdbcTemplate);

		return entities;
	}

	/**
	 * Get vehicle setting of experiment (vehicle type + number)
	 * 
	 * @param experiment
	 * @return
	 */
	public ArrayList<Vehicle> getVehicles(int experiment) {

		ArrayList<Vehicle> vehicles = (ArrayList<Vehicle>) DataLoadService.loadMultipleRowsBySelectionId(
				"r_experiment_v_vehicle_type", "exp_vehicle_exp", experiment, new VehicleMapper(), jdbcTemplate);
		return vehicles;

	}

	@Override
	public ArrayList<ObjectiveWeight> getObjectivesByExperiment(Experiment experiment) {
		ArrayList<ObjectiveWeight> vehicles = (ArrayList<ObjectiveWeight>) DataLoadService
				.loadMultipleRowsBySelectionId("r_experiment_v_objective", "exp_obj_exp", experiment.getId(),
						new ObjectiveWeightMapper(), jdbcTemplate);
		return vehicles;
	}

	/**
	 * Load the parameters of a given experiment
	 * 
	 * @param experimentId
	 * @return List of parameters
	 */
	private ArrayList<GeneralParameterValue> getParameterValues(int expId) {

		ArrayList<GeneralParameterValue> parameters = (ArrayList<GeneralParameterValue>) DataLoadService
				.loadMultipleRowsBySelectionId("r_experiment_v_parameter_type", "exp_parameter_exp", expId,
						new GeneralParameterMapper(), jdbcTemplate);

		return parameters;
	}

	/**
	 * Load the input routings of a given experiment
	 * 
	 * @param expId
	 * @return List of rountingsassignments
	 */
	private ArrayList<RoutingAssignment> getRoutingAssignments(int expId) {

		ArrayList<RoutingAssignment> routingAssignments = (ArrayList<RoutingAssignment>) DataLoadService
				.loadMultipleRowsBySelectionId("r_experiment_v_routing", "exp_rou_exp", expId,
						new RoutingAssignmentMapper(), jdbcTemplate);

		return routingAssignments;
	}

	@Override
	public Integer persistExperimentSettings(Experiment experiment, PeriodSetting initialSetting,
			ArrayList<PeriodSetting> followerSettings, SettingRequest request, ArrayList<PeriodSettingType> outputs,
			long runtime) {

		// Persist experiment itself and produce run
		final Experiment experimentToSave = this.persistExperiment(experiment);
		final Run runToSave = this.persistRun(experimentToSave.getId(), runtime);

		// Persist input and output of initial setting

		/// Parameters are always input
		this.persistParameterValuesOfExperiment(initialSetting, experimentToSave.getId());

		/// Vehicles are always input
		this.persistVehicles(experimentToSave.getId(), initialSetting.getVehicles());

		/// Objectives are always input
		this.persistObjectives(experimentToSave);

		/// Kpi values are always output. Can directly save from all periods
		ArrayList<PeriodSetting> settings = new ArrayList<PeriodSetting>();
		settings.add(initialSetting);
		settings.addAll(followerSettings);
		this.persistKpiValuesOfRun(settings, runToSave.getId());

		// Atomic outputs are always outputs
		this.persistAtomicOutputsOfRun(settings, runToSave.getId());

		/// Delivery area set can be both
		if (request.getPeriodSettings().containsKey(PeriodSettingType.DELIVERYAREASET)) {// Input
																							// and
																							// reused
																							// for
																							// all
																							// periods
			this.persistDeliveryAreaSetByExperiment(experimentToSave.getId(), initialSetting.getDeliveryAreaSetId());
		} else {// Output for all periods
			boolean save = false;
			for (int i = 0; i < outputs.size(); i++) {
				if (outputs.get(i).equals(PeriodSettingType.DELIVERYAREASET)) {
					save = true;
					break;
				}
			}
			if (save)
				this.persistDeliveryAreaSetByRun(runToSave.getId(), settings);
		}

		/// Value function approximation can be both
		if (request.getPeriodSettings().containsKey(PeriodSettingType.LINEAR_VALUE_FUNCTION_APPROXIMATION)) {// Input
			// and
			// reused
			// for
			// all
			// periods
			this.persistValueFunctionApproximationModelByExperiment(experimentToSave.getId(),
					initialSetting.getValueFunctionModelSetId());
		} else {// Output for all periods
			boolean save = false;
			for (int i = 0; i < outputs.size(); i++) {
				if (outputs.get(i).equals(PeriodSettingType.LINEAR_VALUE_FUNCTION_APPROXIMATION)) {
					save = true;
					break;
				}
			}
			if (save)
				this.persistValueFunctionApproximationModelByRun(runToSave.getId(), settings);
		}

		// Alternative set can be both
		if (request.getPeriodSettings().containsKey(PeriodSettingType.ALTERNATIVESET)) {// Input
																						// and
																						// reused
																						// for
																						// all
																						// periods
			this.persistAlternativeSetByExperiment(experimentToSave.getId(), initialSetting.getAlternativeSetId());
		} else {// Output for all periods
			boolean save = false;
			for (int i = 0; i < outputs.size(); i++) {
				if (outputs.get(i).equals(PeriodSettingType.ALTERNATIVESET)) {
					save = true;
					break;
				}
			}
			if (save)
				this.persistAlternativeSetByRun(runToSave.getId(), settings);
		}

		// Time window set can be both
		if (request.getPeriodSettings().containsKey(PeriodSettingType.TIMEWINDOWSET)) {// Input
																						// and
																						// reused
																						// for
																						// all
																						// periods
			this.persistTimeWindowSetByExperiment(experimentToSave.getId(), initialSetting.getTimeWindowSetId());
		} else {// Output for all periods
			boolean save = false;
			for (int i = 0; i < outputs.size(); i++) {
				if (outputs.get(i).equals(PeriodSettingType.TIMEWINDOWSET)) {
					save = true;
					break;
				}
			}
			if (save)
				this.persistTimeWindowSetByRun(runToSave.getId(), settings);
		}

		// Customer set can be both
		if (request.getPeriodSettings().containsKey(PeriodSettingType.CUSTOMERSET)) {
			this.persistCustomerSetByExperiment(experimentToSave.getId(), initialSetting.getCustomerSetId());
		} else {// Output for all periods
			boolean save = false;
			for (int i = 0; i < outputs.size(); i++) {
				if (outputs.get(i).equals(PeriodSettingType.CUSTOMERSET)) {
					save = true;
					break;
				}
			}
			if (save)
				this.persistCustomerSetByRun(runToSave.getId(), settings);
		}
		// ServiceSegmentWeighting is always input
		if (request.getPeriodSettings().containsKey(PeriodSettingType.SERVICESEGMENTWEIGHTING)) {// Input
			// and
			// reused
			// for
			// all
			// periods
			this.persistServiceSegmentWeightingByExperiment(experimentToSave.getId(),
					initialSetting.getServiceSegmentWeightingId());
		}

		// TravelTimeSet is always input
		if (request.getPeriodSettings().containsKey(PeriodSettingType.TRAVELTIMESET)) {// Input
			// and
			// reused
			// for
			// all
			// periods
			this.persistTravelTimeSetByExperiment(experimentToSave.getId(), initialSetting.getTravelTimeSetId());
		}

		// Arrival process is always input
		if (request.getPeriodSettings().containsKey(PeriodSettingType.ARRIVALPROCESS)) {// Input
																						// and
																						// reused
																						// for
																						// all
																						// periods
			this.persistArrivalProcessByExperiment(experimentToSave.getId(), initialSetting.getArrivalProcessId());
		}

		// Arrival probability is always input
		if (request.getPeriodSettings().containsKey(PeriodSettingType.ARRIVAL_PROBABILITY_DISTRIBUTION)) {// Input
			// and
			// reused
			// for
			// all
			// periods
			this.persistArrivalProbabilityDistributionByExperiment(experimentToSave.getId(),
					initialSetting.getArrivalProbabilityDistributionId());
		}

		// Demand segment weighting can be both
		// TODO Adapt such that several weightings per period (t) are possible
		// (add run_dsw_t)
		if (request.getPeriodSettings().containsKey(PeriodSettingType.DEMANDSEGMENTWEIGHTING)) {// Input
			// and
			// reused
			// for
			// all
			// periods
			this.persistDSWeightingByExperiment(experimentToSave.getId(), initialSetting.getDemandSegmentWeightingId());
		} else {// Output for all periods
			boolean save = false;
			for (int i = 0; i < outputs.size(); i++) {
				if (outputs.get(i).equals(PeriodSettingType.DEMANDSEGMENTWEIGHTING)) {
					save = true;
					break;
				}
			}
			if (save)
				this.persistDSWeightingByRun(runToSave.getId(), settings);
		}

		// Demand segment set can be both

		if (request.getPeriodSettings().containsKey(PeriodSettingType.DEMANDSEGMENTSET)) {// Input
			// and
			// reused
			// for
			// all
			// periods
			this.persistDSSetByExperiment(experimentToSave.getId(), initialSetting.getDemandSegmentSetId());
		} else {// Output for all periods
			boolean save = false;
			for (int i = 0; i < outputs.size(); i++) {
				if (outputs.get(i).equals(PeriodSettingType.DEMANDSEGMENTSET)) {
					save = true;
					break;
				}
			}
			if (save)
				this.persistDSSetByRun(runToSave.getId(), settings);
		}

		// Dynamic programming Tree can be both

		if (request.getPeriodSettings().containsKey(PeriodSettingType.DYNAMICPROGRAMMINGTREE)) {// Input
			// and
			// reused
			// for
			// all
			// periods
			this.persistDynamicProgrammingTreeByExperiment(experimentToSave.getId(),
					initialSetting.getDynamicProgrammingTreeId());
		} else {// Output for all periods
			boolean save = false;
			for (int i = 0; i < outputs.size(); i++) {
				if (outputs.get(i).equals(PeriodSettingType.DYNAMICPROGRAMMINGTREE)) {
					save = true;
					break;
				}
			}
			if (save)
				this.persistDynamicProgrammingTreeByRun(runToSave.getId(), settings);
		}

		// Order set can be both
		if (request.getPeriodSettings().containsKey(PeriodSettingType.ORDERSET)) {
			this.persistOrderSetByExperiment(experimentToSave.getId(), initialSetting.getOrderSetId(), false);
		} else {// Output for all periods
			boolean save = false;
			for (int i = 0; i < outputs.size(); i++) {
				if (outputs.get(i).equals(PeriodSettingType.ORDERSET)) {
					save = true;
					break;
				}
			}
			if (save)
				this.persistOrderSetByRun(runToSave.getId(), settings);
		}

		// Historical Order set can only be input
		if (request.getPeriodSettings().containsKey(PeriodSettingType.HISTORICALORDERS)) {
			this.persistOrderSetByExperiment(experimentToSave.getId(), initialSetting.getHistoricalOrderSetId(), true);
		}

		// Value bucket forecast set can be both
		if (request.getPeriodSettings().containsKey(PeriodSettingType.DEMANDFORECASTSET_VALUEBUCKETS)) {// Input

			this.persistValueBucketForecastSetByExperiment(experimentToSave.getId(),
					initialSetting.getValueBucketForecastSetId());
		} else {// Output for all periods
			boolean save = false;
			for (int i = 0; i < outputs.size(); i++) {
				if (outputs.get(i).equals(PeriodSettingType.DEMANDFORECASTSET_VALUEBUCKETS)) {
					save = true;
					break;
				}
			}
			if (save)
				this.persistValueBucketForecastSetByRun(runToSave.getId(), settings);
		}

		// Forecast set can be both
		if (request.getPeriodSettings().containsKey(PeriodSettingType.DEMANDFORECASTSET_DEMANDSEGMENTS)) {// Input

			this.persistDemandSegmentForecastSetByExperiment(experimentToSave.getId(),
					initialSetting.getDemandSegmentForecastSetId());
		} else {// Output for all periods
			boolean save = false;
			for (int i = 0; i < outputs.size(); i++) {
				if (outputs.get(i).equals(PeriodSettingType.DEMANDFORECASTSET_DEMANDSEGMENTS)) {
					save = true;
					break;
				}
			}
			if (save)
				this.persistDemandSegmentForecastSetByRun(runToSave.getId(), settings);
		}

		// Capacity set can be both
		if (request.getPeriodSettings().containsKey(PeriodSettingType.CAPACITYSET)) {// Input

			this.persistCapacitySetByExperiment(experimentToSave.getId(), initialSetting.getCapacitySetId());
		} else {// Output for all periods
			boolean save = false;
			for (int i = 0; i < outputs.size(); i++) {
				if (outputs.get(i).equals(PeriodSettingType.CAPACITYSET)) {
					save = true;
					break;
				}
			}
			if (save)
				this.persistCapacitySetByRun(runToSave.getId(), settings);
		}

		// Control set can be both
		if (request.getPeriodSettings().containsKey(PeriodSettingType.CONTROLSET)) {// Input

			this.persistControlSetByExperiment(experimentToSave.getId(), initialSetting.getControlSetId());
		} else {// Output for all periods
			boolean save = false;
			for (int i = 0; i < outputs.size(); i++) {
				if (outputs.get(i).equals(PeriodSettingType.CONTROLSET)) {
					save = true;
					break;
				}
			}
			if (save)
				this.persistControlSetByRun(runToSave.getId(), settings);
		}

		// Value bucket set can be both
		if (request.getPeriodSettings().containsKey(PeriodSettingType.VALUEBUCKETSET)) {// Input
			// Can still be null if optional
			if (initialSetting.getValueBucketSetId() != null) {
				this.persistValueBucketSetByExperiment(experimentToSave.getId(), initialSetting.getValueBucketSetId());
			}

		} else {// Output for all periods
			boolean save = false;
			for (int i = 0; i < outputs.size(); i++) {
				if (outputs.get(i).equals(PeriodSettingType.VALUEBUCKETSET)) {
					save = true;
					break;
				}
			}
			if (save)
				this.persistValueBucketSetByRun(runToSave.getId(), settings);
		}

		// Vehicle assignment set can be both
		if (request.getPeriodSettings().containsKey(PeriodSettingType.VEHICLE_ASSIGNMENT_SET)) {// Input
			// Can still be null if optional
			if (initialSetting.getVehicleAssignmentSetId() != null) {
				this.persistVehicleAssignmentSetByExperiment(experimentToSave.getId(),
						initialSetting.getVehicleAssignmentSetId());
			}

		} else {// Output for all periods
			boolean save = false;
			for (int i = 0; i < outputs.size(); i++) {
				if (outputs.get(i).equals(PeriodSettingType.VEHICLE_ASSIGNMENT_SET)) {
					save = true;
					break;
				}
			}
			if (save)
				this.persistVehicleAssignmentSetByRun(runToSave.getId(), settings);
		}

		// Order request sets can be input and output of multiple periods
		if (request.getPeriodSettings().containsKey(PeriodSettingType.ORDERREQUESTSET)) {// Input

			this.persistOrderRequestSetByExperiment(experimentToSave.getId(), settings);
		} else {// Output for all periods
			boolean save = false;
			for (int i = 0; i < outputs.size(); i++) {
				if (outputs.get(i).equals(PeriodSettingType.ORDERREQUESTSET)) {
					save = true;
					break;
				}
			}
			if (save)
				this.persistOrderRequestSetByRun(runToSave.getId(), settings);
		}

		// Learning order requests can only be input
		if (request.getPeriodSettings().containsKey(PeriodSettingType.LEARNING_ORDERREQUESTSET)) {
			this.persistLearningOrderRequestSetsByExperiment(experimentToSave.getId(), settings);
		}

		// Learning routings can only be input
		if (request.getPeriodSettings().containsKey(PeriodSettingType.LEARNING_FINAL_ROUTING)) {
			this.persistLearningFinalRoutingsByExperiment(experimentToSave.getId(), settings);
		}
		
		// Benchmarking routings can only be input
				if (request.getPeriodSettings().containsKey(PeriodSettingType.BENCHMARKING_FINAL_ROUTING)) {
					this.persistBenchmarkingFinalRoutingsByExperiment(experimentToSave.getId(), settings);
				}
				
		// Learning routings can only be input
		if (request.getPeriodSettings().containsKey(PeriodSettingType.LEARNING_ORDER_SET)) {
			this.persistLearningOrderSetsByExperiment(experimentToSave.getId(), settings);
		}
		
		// Benchmarking routings can only be input
		if (request.getPeriodSettings().containsKey(PeriodSettingType.BENCHMARKING_ORDER_SET)) {
			this.persistBenchmarkingOrderSetsByExperiment(experimentToSave.getId(), settings);
		}

		// One routing can be input but several routings per period can be
		// output
		if (request.getPeriodSettings().containsKey(PeriodSettingType.INITIALROUTING)) {// Input

			this.persistRoutingByExperiment(experimentToSave.getId(), initialSetting, true);
		}
		if (request.getPeriodSettings().containsKey(PeriodSettingType.FINALROUTING)) {// Input

			this.persistRoutingByExperiment(experimentToSave.getId(), initialSetting, false);
		}

		this.persistRoutingByRun(runToSave.getId(), settings,
				request.getPeriodSettings().containsKey(PeriodSettingType.INITIALROUTING),
				request.getPeriodSettings().containsKey(PeriodSettingType.FINALROUTING));

		return experimentToSave.getId();
	}

	private void persistRoutingByExperiment(int expId, PeriodSetting setting, Boolean initialRouting) {
		final Boolean initialRoutingToSave = initialRouting;
		final Integer expToSave = expId;
		ArrayList<RoutingAssignment> routings = setting.getRoutingAssignments();
		RoutingAssignment input = new RoutingAssignment();
		for (int i = 0; i < routings.size(); i++) {
			if (initialRouting && routings.get(i).getT() == -1) {
				input = routings.get(i);
				break;
			}
			if (!initialRouting && routings.get(i).getT() == -2) {
				input = routings.get(i);
				break;
			}
		}

		final Integer rIdToSave = input.getRoutingId();
		final Integer period = 0;

		final String SQL = DataLoadService.buildInsertSQL("r_experiment_v_routing", 4,
				"exp_rou_exp, exp_rou_rou, exp_rou_period, exp_rou_t");

		DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] {});
				ps.setObject(1, expToSave, Types.INTEGER);
				ps.setObject(2, rIdToSave, Types.INTEGER);
				ps.setObject(3, period, Types.INTEGER);
				if (initialRoutingToSave) {
					ps.setObject(4, -1, Types.INTEGER);
				} else {
					ps.setObject(4, -2, Types.INTEGER);
				}
				return ps;
			}
		}, jdbcTemplate);
	}

	/**
	 * Saves routing outputs. As all routings are in one list, the booleans
	 * indicate if the initial or final routing are inputs and should thus not
	 * be saved in the run
	 * 
	 * @param runId
	 * @param settings
	 * @param initialAsInput
	 * @param finalAsInput
	 */
	private void persistRoutingByRun(int runId, ArrayList<PeriodSetting> settings, Boolean initialAsInput,
			Boolean finalAsInput) {

		ArrayList<RoutingAssignment> routings = new ArrayList<RoutingAssignment>();

		for (int i = 0; i < settings.size(); i++) {
			if (settings.get(i).getRoutingAssignments() != null) {
				for (int rAssID = 0; rAssID < settings.get(i).getRoutingAssignments().size(); rAssID++) {
					settings.get(i).getRoutingAssignments().get(rAssID).setPeriod(settings.get(i).getStartingPeriod());
				}
				if (settings.get(i).getStartingPeriod() == 0) {// Initial period
																// setting

					for (int a = 0; a < settings.get(i).getRoutingAssignments().size(); a++) { // Check
																								// for
																								// each
																								// routing
																								// if
																								// it
																								// should
																								// be
																								// saved
																								// for
																								// the
																								// run
						if (initialAsInput && settings.get(i).getRoutingAssignments().get(a).getT() == -1) {
							// Do not save because references experiment
						} else if (finalAsInput && settings.get(i).getRoutingAssignments().get(a).getT() == -2) {
							// Do not save because references experiment
						} else {
							routings.add(settings.get(i).getRoutingAssignments().get(a));
						}
					}

				} else {
					routings.addAll(settings.get(i).getRoutingAssignments());
				}

			}
		}

		final ArrayList<RoutingAssignment> routingsToSave = routings;
		final int runToSave = runId;

		DataLoadService.persistAll("r_run_v_routing", 4, "run_rou_run, run_rou_rou, run_rou_period,run_rou_t",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return routingsToSave.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						RoutingAssignment routing = (RoutingAssignment) routingsToSave.get(i);
						ps.setInt(1, runToSave);
						ps.setInt(2, routing.getRoutingId());
						ps.setInt(3, routing.getPeriod());
						ps.setInt(4, routing.getT());

					}
				}, jdbcTemplate);
	}

	private void persistOrderRequestSetByExperiment(int expId, ArrayList<PeriodSetting> settings) {

		final int expToSave = expId;
		ArrayList<PeriodSetting> relevantSettings = new ArrayList<PeriodSetting>();
		for (int i = 0; i < settings.size(); i++) {
			if (settings.get(i).getOrderRequestSetId() != null) {
				if (settings.get(i).getOrderRequestSetId() != 0) {
					relevantSettings.add(settings.get(i));
				}
			}
		}
		final ArrayList<PeriodSetting> relevantSettingsOR = relevantSettings;

		DataLoadService.persistAll("r_experiment_v_order_request_set", 3, "exp_ors_exp, exp_ors_ors, exp_ors_period",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return relevantSettingsOR.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						PeriodSetting periodSetting = (PeriodSetting) relevantSettingsOR.get(i);
						ps.setInt(1, expToSave);
						ps.setInt(2, periodSetting.getOrderRequestSetId());
						ps.setInt(3, periodSetting.getStartingPeriod());

					}
				}, jdbcTemplate);
	}

	private void persistLearningOrderRequestSetsByExperiment(int expId, ArrayList<PeriodSetting> settings) {

		final int expToSave = expId;
		ArrayList<PeriodSetting> relevantSettings = new ArrayList<PeriodSetting>();
		for (int i = 0; i < settings.size(); i++) {
			if (settings.get(i).getLearningOutputRequestsExperimentId() != null) {
				if (settings.get(i).getLearningOutputRequestsExperimentId() != 0) {
					relevantSettings.add(settings.get(i));
				}
			}
		}
		final ArrayList<PeriodSetting> relevantSettingsOR = relevantSettings;

		DataLoadService.persistAll("r_experiment_v_learning_experiment", 6,
				"ele_exp_experiment, ele_exp_learning_input_experiment, ele_exp_period, ele_exp_requests, ele_exp_routings, ele_exp_routings_benchmarking",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return relevantSettingsOR.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						PeriodSetting periodSetting = (PeriodSetting) relevantSettingsOR.get(i);
						ps.setInt(1, expToSave);
						ps.setInt(2, periodSetting.getLearningOutputRequestsExperimentId());
						ps.setInt(3, periodSetting.getStartingPeriod());
						ps.setInt(4, 1);
						ps.setInt(5, 0);
						ps.setInt(6, 0);

					}
				}, jdbcTemplate);
	}

	private void persistLearningFinalRoutingsByExperiment(int expId, ArrayList<PeriodSetting> settings) {

		final int expToSave = expId;
		ArrayList<Integer> experiments = new ArrayList<Integer>();
		ArrayList<Integer> startingPeriods = new ArrayList<Integer>();
		for (int i = 0; i < settings.size(); i++) {
			if (settings.get(i).getLearningOutputFinalRoutingsExperimentIds() != null) {

				ArrayList<Integer> experimentIds = settings.get(i).getLearningOutputFinalRoutingsExperimentIds();
				experiments.addAll(experimentIds);
				for(int j=0; j<experimentIds.size(); j++){
					startingPeriods.add(settings.get(i).getStartingPeriod());
					startingPeriods.add(settings.get(i).getStartingPeriod());
				}
				
			}
		}
		final ArrayList<Integer> experimentsFinal = experiments;
		final ArrayList<Integer> startingPeriodsFinal = startingPeriods;

		DataLoadService.persistAll("r_experiment_v_learning_experiment", 6,
				"ele_exp_experiment, ele_exp_learning_input_experiment, ele_exp_period,ele_exp_requests, ele_exp_routings, ele_exp_routings_benchmarking",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return experimentsFinal.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						ps.setInt(1, expToSave);
						ps.setInt(2, experimentsFinal.get(i));
						ps.setInt(3, startingPeriodsFinal.get(i));
						ps.setInt(4, 0);
						ps.setInt(5, 1);
						ps.setInt(6, 0);
					}
				}, jdbcTemplate);
	}
	
	private void persistLearningOrderSetsByExperiment(int expId, ArrayList<PeriodSetting> settings) {

		final int expToSave = expId;
		ArrayList<Integer> experiments = new ArrayList<Integer>();
		ArrayList<Integer> startingPeriods = new ArrayList<Integer>();
		for (int i = 0; i < settings.size(); i++) {
			if (settings.get(i).getLearningOutputOrderSetsExperimentIds() != null) {

				ArrayList<Integer> experimentIds = settings.get(i).getLearningOutputOrderSetsExperimentIds();
				experiments.addAll(experimentIds);
				for(int j=0; j<experimentIds.size(); j++){
					startingPeriods.add(settings.get(i).getStartingPeriod());
					startingPeriods.add(settings.get(i).getStartingPeriod());
				}
				
			}
		}
		final ArrayList<Integer> experimentsFinal = experiments;
		final ArrayList<Integer> startingPeriodsFinal = startingPeriods;

		DataLoadService.persistAll("r_experiment_v_learning_experiment", 5,
				"ele_exp_experiment, ele_exp_learning_input_experiment, ele_exp_period,ele_exp_requests,ele_exp_order_sets",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return experimentsFinal.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						ps.setInt(1, expToSave);
						ps.setInt(2, experimentsFinal.get(i));
						ps.setInt(3, startingPeriodsFinal.get(i));
						ps.setInt(4, 0);
						ps.setInt(5, 1);
					}
				}, jdbcTemplate);
	}
	
	private void persistBenchmarkingFinalRoutingsByExperiment(int expId, ArrayList<PeriodSetting> settings) {

		final int expToSave = expId;
		ArrayList<Integer> experiments = new ArrayList<Integer>();
		ArrayList<Integer> startingPeriods = new ArrayList<Integer>();
		for (int i = 0; i < settings.size(); i++) {
			if (settings.get(i).getBenchmarkingOutputFinalRoutingsExperimentIds() != null) {

				ArrayList<Integer> experimentIds = settings.get(i).getBenchmarkingOutputFinalRoutingsExperimentIds();
				experiments.addAll(experimentIds);
				for(int j=0; j<experimentIds.size(); j++){
					startingPeriods.add(settings.get(i).getStartingPeriod());
					startingPeriods.add(settings.get(i).getStartingPeriod());
				}
				
			}
		}
		final ArrayList<Integer> experimentsFinal = experiments;
		final ArrayList<Integer> startingPeriodsFinal = startingPeriods;

		DataLoadService.persistAll("r_experiment_v_learning_experiment", 6,
				"ele_exp_experiment, ele_exp_learning_input_experiment, ele_exp_period,ele_exp_requests, ele_exp_routings, ele_exp_routings_benchmarking",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return experimentsFinal.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						ps.setInt(1, expToSave);
						ps.setInt(2, experimentsFinal.get(i));
						ps.setInt(3, startingPeriodsFinal.get(i));
						ps.setInt(4, 0);
						ps.setInt(5, 0);
						ps.setInt(6, 1);
					}
				}, jdbcTemplate);
	}
	
	private void persistBenchmarkingOrderSetsByExperiment(int expId, ArrayList<PeriodSetting> settings) {

		final int expToSave = expId;
		ArrayList<Integer> experiments = new ArrayList<Integer>();
		ArrayList<Integer> startingPeriods = new ArrayList<Integer>();
		for (int i = 0; i < settings.size(); i++) {
			if (settings.get(i).getBenchmarkingOutputOrderSetsExperimentIds() != null) {

				ArrayList<Integer> experimentIds = settings.get(i).getBenchmarkingOutputOrderSetsExperimentIds();
				experiments.addAll(experimentIds);
				for(int j=0; j<experimentIds.size(); j++){
					startingPeriods.add(settings.get(i).getStartingPeriod());
					startingPeriods.add(settings.get(i).getStartingPeriod());
				}
				
			}
		}
		final ArrayList<Integer> experimentsFinal = experiments;
		final ArrayList<Integer> startingPeriodsFinal = startingPeriods;

		DataLoadService.persistAll("r_experiment_v_learning_experiment", 5,
				"ele_exp_experiment, ele_exp_learning_input_experiment, ele_exp_period,ele_exp_requests, ele_exp_order_sets_benchmarking",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return experimentsFinal.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						ps.setInt(1, expToSave);
						ps.setInt(2, experimentsFinal.get(i));
						ps.setInt(3, startingPeriodsFinal.get(i));
						ps.setInt(4, 0);
						ps.setInt(5, 1);
					}
				}, jdbcTemplate);
	}

	private void persistOrderRequestSetByRun(int runId, ArrayList<PeriodSetting> settings) {

		final int runToSave = runId;
		ArrayList<PeriodSetting> relevantSettings = new ArrayList<PeriodSetting>();
		for (int i = 0; i < settings.size(); i++) {
			if (settings.get(i).getOrderRequestSetId() != null) {
				if (settings.get(i).getOrderRequestSetId() != 0) {
					relevantSettings.add(settings.get(i));
				}
			}
		}
		final ArrayList<PeriodSetting> relevantSettingsOR = relevantSettings;

		DataLoadService.persistAll("r_run_v_order_request_set", 3, "run_ors_run, run_ors_ors, run_ors_period",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return relevantSettingsOR.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						PeriodSetting periodSetting = (PeriodSetting) relevantSettingsOR.get(i);
						ps.setInt(1, runToSave);
						ps.setInt(2, periodSetting.getOrderRequestSetId());
						ps.setInt(3, periodSetting.getStartingPeriod());

					}
				}, jdbcTemplate);
	}

	private void persistValueBucketSetByExperiment(int expId, int vbId) {
		final Integer expToSave = expId;
		final Integer vbIdToSave = vbId;
		final Integer period = 0;

		final String SQL = DataLoadService.buildInsertSQL("r_experiment_v_value_bucket_set", 3,
				"exp_vbs_exp, exp_vbs_vbs, exp_vbs_period");

		DataLoadService.persistWithoutId(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL);
				ps.setObject(1, expToSave, Types.INTEGER);
				ps.setObject(2, vbIdToSave, Types.INTEGER);
				ps.setObject(3, period, Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);
	}

	private void persistValueBucketSetByRun(int runId, ArrayList<PeriodSetting> settings) {

		final int runToSave = runId;
		ArrayList<PeriodSetting> relevantSettings = new ArrayList<PeriodSetting>();
		for (int i = 0; i < settings.size(); i++) {
			if (settings.get(i).getValueBucketSetId() != null) {
				if (settings.get(i).getValueBucketSetId() != 0) {
					relevantSettings.add(settings.get(i));
				}
			}
		}
		final ArrayList<PeriodSetting> relevantSettingsVBS = relevantSettings;

		DataLoadService.persistAll("r_run_v_value_bucket_set", 3, "run_vbs_run, run_vbs_vbs, run_vbs_period",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return relevantSettingsVBS.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						PeriodSetting periodSetting = (PeriodSetting) relevantSettingsVBS.get(i);
						ps.setInt(1, runToSave);
						ps.setInt(2, periodSetting.getValueBucketSetId());
						ps.setInt(3, periodSetting.getStartingPeriod());

					}
				}, jdbcTemplate);
	}

	private void persistVehicleAssignmentSetByExperiment(int expId, int vasId) {
		final Integer expToSave = expId;
		final Integer vasIdToSave = vasId;
		final Integer period = 0;

		final String SQL = DataLoadService.buildInsertSQL("r_experiment_v_vehicle_area_assignment_set", 3,
				"exp_vas_exp, exp_vas_vas, exp_vas_period");

		DataLoadService.persistWithoutId(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL);
				ps.setObject(1, expToSave, Types.INTEGER);
				ps.setObject(2, vasIdToSave, Types.INTEGER);
				ps.setObject(3, period, Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);
	}

	private void persistVehicleAssignmentSetByRun(int runId, ArrayList<PeriodSetting> settings) {

		final int runToSave = runId;
		ArrayList<PeriodSetting> relevantSettings = new ArrayList<PeriodSetting>();
		for (int i = 0; i < settings.size(); i++) {
			if (settings.get(i).getVehicleAssignmentSetId() != null) {
				if (settings.get(i).getVehicleAssignmentSetId() != 0) {
					relevantSettings.add(settings.get(i));
				}
			}
		}
		final ArrayList<PeriodSetting> relevantSettingsVBS = relevantSettings;

		DataLoadService.persistAll("r_run_v_vehicle_area_assignment_set", 3, "run_vas_run,run_vas_vas,run_vas_period",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return relevantSettingsVBS.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						PeriodSetting periodSetting = (PeriodSetting) relevantSettingsVBS.get(i);
						ps.setInt(1, runToSave);
						ps.setInt(2, periodSetting.getVehicleAssignmentSetId());
						ps.setInt(3, periodSetting.getStartingPeriod());

					}
				}, jdbcTemplate);
	}

	private void persistControlSetByExperiment(int expId, int coId) {
		final Integer expToSave = expId;
		final Integer coIdToSave = coId;
		final Integer period = 0;

		final String SQL = DataLoadService.buildInsertSQL("r_experiment_v_control_set", 3,
				"exp_cos_exp, exp_cos_cos, exp_cos_period");

		DataLoadService.persistWithoutId(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL);
				ps.setObject(1, expToSave, Types.INTEGER);
				ps.setObject(2, coIdToSave, Types.INTEGER);
				ps.setObject(3, period, Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);
	}

	private void persistControlSetByRun(int runId, ArrayList<PeriodSetting> settings) {

		final int runToSave = runId;
		ArrayList<PeriodSetting> relevantSettings = new ArrayList<PeriodSetting>();
		for (int i = 0; i < settings.size(); i++) {
			if (settings.get(i).getControlSetId() != null) {
				if (settings.get(i).getControlSetId() != 0) {
					relevantSettings.add(settings.get(i));
				}
			}
		}
		final ArrayList<PeriodSetting> relevantSettingsCOS = relevantSettings;

		DataLoadService.persistAll("r_run_v_control_set", 3, "run_cos_run, run_cos_cos, run_cos_period",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return relevantSettingsCOS.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						PeriodSetting periodSetting = (PeriodSetting) relevantSettingsCOS.get(i);
						ps.setInt(1, runToSave);
						ps.setInt(2, periodSetting.getControlSetId());
						ps.setInt(3, periodSetting.getStartingPeriod());

					}
				}, jdbcTemplate);
	}

	private void persistCapacitySetByExperiment(int expId, int caId) {
		final Integer expToSave = expId;
		final Integer caIdToSave = caId;
		final Integer period = 0;

		final String SQL = DataLoadService.buildInsertSQL("r_experiment_v_capacity_set", 3,
				"exp_cas_exp, exp_cas_cas, exp_cas_period");

		DataLoadService.persistWithoutId(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL);
				ps.setObject(1, expToSave, Types.INTEGER);
				ps.setObject(2, caIdToSave, Types.INTEGER);
				ps.setObject(3, period, Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);
	}

	private void persistCapacitySetByRun(int runId, ArrayList<PeriodSetting> settings) {

		final int runToSave = runId;
		ArrayList<PeriodSetting> relevantSettings = new ArrayList<PeriodSetting>();
		for (int i = 0; i < settings.size(); i++) {
			if (settings.get(i).getCapacitySetId() != null) {
				if (settings.get(i).getCapacitySetId() != 0) {
					relevantSettings.add(settings.get(i));
				}
			}
		}
		final ArrayList<PeriodSetting> relevantSettingsCAS = relevantSettings;

		DataLoadService.persistAll("r_run_v_capacity_set", 3, "run_cas_run, run_cas_cas, run_cas_period",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return relevantSettingsCAS.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						PeriodSetting periodSetting = (PeriodSetting) relevantSettingsCAS.get(i);
						ps.setInt(1, runToSave);
						ps.setInt(2, periodSetting.getCapacitySetId());
						ps.setInt(3, periodSetting.getStartingPeriod());

					}
				}, jdbcTemplate);
	}

	private void persistValueBucketForecastSetByExperiment(int expId, int dfId) {
		final Integer expToSave = expId;
		final Integer dfIdToSave = dfId;
		final Integer period = 0;

		final String SQL = DataLoadService.buildInsertSQL("r_experiment_v_value_bucket_forecast_set", 3,
				"exp_vfs_exp, exp_vfs_vfs, exp_vfs_period");

		DataLoadService.persistWithoutId(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL);
				ps.setObject(1, expToSave, Types.INTEGER);
				ps.setObject(2, dfIdToSave, Types.INTEGER);
				ps.setObject(3, period, Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);
	}

	private void persistValueBucketForecastSetByRun(int runId, ArrayList<PeriodSetting> settings) {

		final int runToSave = runId;
		ArrayList<PeriodSetting> relevantSettings = new ArrayList<PeriodSetting>();
		for (int i = 0; i < settings.size(); i++) {
			if (settings.get(i).getValueBucketForecastSetId() != null) {
				if (settings.get(i).getValueBucketForecastSetId() != 0) {
					relevantSettings.add(settings.get(i));
				}
			}
		}
		final ArrayList<PeriodSetting> relevantSettingsDFS = relevantSettings;

		DataLoadService.persistAll("r_run_v_value_bucket_forecast_set", 3, "run_vfs_run, run_vfs_vfs, run_vfs_period",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return relevantSettingsDFS.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						PeriodSetting periodSetting = (PeriodSetting) relevantSettingsDFS.get(i);
						ps.setInt(1, runToSave);
						ps.setInt(2, periodSetting.getValueBucketForecastSetId());
						ps.setInt(3, periodSetting.getStartingPeriod());

					}
				}, jdbcTemplate);
	}

	private void persistDemandSegmentForecastSetByExperiment(int expId, int dsfId) {
		final Integer expToSave = expId;
		final Integer dsfIdToSave = dsfId;
		final Integer period = 0;

		final String SQL = DataLoadService.buildInsertSQL("r_experiment_v_demand_segment_forecast_set", 3,
				"exp_dfs_exp, exp_dfs_dfs, exp_dfs_period");

		DataLoadService.persistWithoutId(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL);
				ps.setObject(1, expToSave, Types.INTEGER);
				ps.setObject(2, dsfIdToSave, Types.INTEGER);
				ps.setObject(3, period, Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);
	}

	private void persistDemandSegmentForecastSetByRun(int runId, ArrayList<PeriodSetting> settings) {

		final int runToSave = runId;
		ArrayList<PeriodSetting> relevantSettings = new ArrayList<PeriodSetting>();
		for (int i = 0; i < settings.size(); i++) {
			if (settings.get(i).getDemandSegmentForecastSetId() != null) {
				if (settings.get(i).getDemandSegmentForecastSetId() != 0) {
					relevantSettings.add(settings.get(i));
				}
			}
		}
		final ArrayList<PeriodSetting> relevantSettingsDFS = relevantSettings;

		DataLoadService.persistAll("r_run_v_demand_segment_forecast_set", 3, "run_dfs_run, run_dfs_dfs, run_dfs_period",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return relevantSettingsDFS.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						PeriodSetting periodSetting = (PeriodSetting) relevantSettingsDFS.get(i);
						ps.setInt(1, runToSave);
						ps.setInt(2, periodSetting.getDemandSegmentForecastSetId());
						ps.setInt(3, periodSetting.getStartingPeriod());

					}
				}, jdbcTemplate);
	}

	private void persistOrderSetByExperiment(int expId, int orId, Boolean historical) {
		final Integer expToSave = expId;
		final Integer orIdToSave = orId;
		final Integer period = 0;
		final Boolean historicalToSave = historical;

		final String SQL = DataLoadService.buildInsertSQL("r_experiment_v_order_set", 4,
				"exp_os_exp, exp_os_os, exp_os_period, exp_os_historical");

		DataLoadService.persistWithoutId(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL);
				ps.setObject(1, expToSave, Types.INTEGER);
				ps.setObject(2, orIdToSave, Types.INTEGER);
				ps.setObject(3, period, Types.INTEGER);
				ps.setObject(4, historicalToSave, Types.BOOLEAN);
				return ps;
			}
		}, jdbcTemplate);
	}

	private void persistOrderSetByRun(int runId, ArrayList<PeriodSetting> settings) {

		final int runToSave = runId;
		ArrayList<PeriodSetting> relevantSettings = new ArrayList<PeriodSetting>();
		for (int i = 0; i < settings.size(); i++) {
			if (settings.get(i).getOrderSetId() != null) {
				if (settings.get(i).getOrderSetId() != 0) {
					relevantSettings.add(settings.get(i));
				}
			}
		}
		final ArrayList<PeriodSetting> relevantSettingsOS = relevantSettings;

		DataLoadService.persistAll("r_run_v_order_set", 3, "run_os_run, run_os_os, run_os_period",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return relevantSettingsOS.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						PeriodSetting periodSetting = (PeriodSetting) relevantSettingsOS.get(i);
						ps.setInt(1, runToSave);

						ps.setInt(2, periodSetting.getOrderSetId());

						ps.setInt(3, periodSetting.getStartingPeriod());

					}
				}, jdbcTemplate);

	}

	private void persistDSWeightingByExperiment(int expId, int dwId) {
		final Integer expToSave = expId;
		final Integer dwIdToSave = dwId;
		final Integer period = 0;

		final String SQL = DataLoadService.buildInsertSQL("r_experiment_v_demand_segment_weighting", 3,
				"exp_dsw_exp, exp_dsw_dsw, exp_dsw_period");

		DataLoadService.persistWithoutId(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL);
				ps.setObject(1, expToSave, Types.INTEGER);
				ps.setObject(2, dwIdToSave, Types.INTEGER);
				ps.setObject(3, period, Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);
	}

	private void persistDSSetByExperiment(int expId, int dsId) {
		final Integer expToSave = expId;
		final Integer dsIdToSave = dsId;
		final Integer period = 0;

		final String SQL = DataLoadService.buildInsertSQL("r_experiment_v_demand_segment_set", 3,
				"exp_dss_exp, exp_dss_dss, exp_dss_period");

		DataLoadService.persistWithoutId(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL);
				ps.setObject(1, expToSave, Types.INTEGER);
				ps.setObject(2, dsIdToSave, Types.INTEGER);
				ps.setObject(3, period, Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);
	}

	private void persistDynamicProgrammingTreeByExperiment(int expId, int dptId) {
		final Integer expToSave = expId;
		final Integer dptIdToSave = dptId;
		final Integer period = 0;

		final String SQL = DataLoadService.buildInsertSQL("r_experiment_v_dynamic_programming_tree", 3,
				"exp_dpt_exp, exp_dpt_dpt, exp_dpt_period");

		DataLoadService.persistWithoutId(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL);
				ps.setObject(1, expToSave, Types.INTEGER);
				ps.setObject(2, dptIdToSave, Types.INTEGER);
				ps.setObject(3, period, Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);
	}

	private void persistDynamicProgrammingTreeByRun(int runId, ArrayList<PeriodSetting> settings) {

		final int runToSave = runId;
		ArrayList<PeriodSetting> relevantSettings = new ArrayList<PeriodSetting>();

		for (int i = 0; i < settings.size(); i++) {
			if (settings.get(i).getDynamicProgrammingTreeId() != null) {
				if (settings.get(i).getDynamicProgrammingTreeId() != 0) {
					relevantSettings.add(settings.get(i));
				}
			}
		}
		final ArrayList<PeriodSetting> relevantSettingsDPT = relevantSettings;

		DataLoadService.persistAll("r_run_v_dynamic_programming_tree", 3, "run_dpt_run, run_dpt_dpt, run_dpt_period",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return relevantSettingsDPT.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						PeriodSetting periodSetting = (PeriodSetting) relevantSettingsDPT.get(i);
						ps.setInt(1, runToSave);
						ps.setInt(2, periodSetting.getDynamicProgrammingTreeId());
						ps.setInt(3, periodSetting.getStartingPeriod());

					}
				}, jdbcTemplate);
	}

	private void persistDSWeightingByRun(int runId, ArrayList<PeriodSetting> settings) {

		final int runToSave = runId;
		ArrayList<PeriodSetting> relevantSettings = new ArrayList<PeriodSetting>();

		for (int i = 0; i < settings.size(); i++) {
			if (settings.get(i).getDemandSegmentWeightingId() != null) {
				if (settings.get(i).getDemandSegmentWeightingId() != 0) {
					relevantSettings.add(settings.get(i));
				}
			}
		}
		final ArrayList<PeriodSetting> relevantSettingsDSW = relevantSettings;

		DataLoadService.persistAll("r_run_v_demand_segment_weighting", 3, "run_dsw_run, run_dsw_dsw, run_dsw_period",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return relevantSettingsDSW.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						PeriodSetting periodSetting = (PeriodSetting) relevantSettingsDSW.get(i);
						ps.setInt(1, runToSave);
						ps.setInt(2, periodSetting.getDemandSegmentWeightingId());
						ps.setInt(3, periodSetting.getStartingPeriod());

					}
				}, jdbcTemplate);
	}

	private void persistDSSetByRun(int runId, ArrayList<PeriodSetting> settings) {

		final int runToSave = runId;
		ArrayList<PeriodSetting> relevantSettings = new ArrayList<PeriodSetting>();

		for (int i = 0; i < settings.size(); i++) {
			if (settings.get(i).getDemandSegmentSetId() != null) {
				if (settings.get(i).getDemandSegmentSetId() != 0) {
					relevantSettings.add(settings.get(i));
				}
			}
		}
		final ArrayList<PeriodSetting> relevantSettingsDSS = relevantSettings;

		DataLoadService.persistAll("r_run_v_demand_segment_set", 3, "run_dss_run, run_dss_dss, run_dss_period",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return relevantSettingsDSS.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						PeriodSetting periodSetting = (PeriodSetting) relevantSettingsDSS.get(i);
						ps.setInt(1, runToSave);
						ps.setInt(2, periodSetting.getDemandSegmentSetId());
						ps.setInt(3, periodSetting.getStartingPeriod());

					}
				}, jdbcTemplate);
	}

	private void persistArrivalProcessByExperiment(int expId, int arId) {
		final Integer expToSave = expId;
		final Integer arIdToSave = arId;
		final Integer period = 0;

		final String SQL = DataLoadService.buildInsertSQL("r_experiment_v_arrival_process", 3,
				"exp_arp_exp, exp_arp_arp, exp_arp_period");

		DataLoadService.persistWithoutId(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL);
				ps.setObject(1, expToSave, Types.INTEGER);
				ps.setObject(2, arIdToSave, Types.INTEGER);
				ps.setObject(3, period, Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);
	}

	private void persistArrivalProbabilityDistributionByExperiment(int expId, int arId) {
		final Integer expToSave = expId;
		final Integer arIdToSave = arId;
		final Integer period = 0;

		final String SQL = DataLoadService.buildInsertSQL("r_experiment_v_arrival_probability_distribution", 3,
				"exp_apd_exp, exp_apd_apd, exp_apd_period");

		DataLoadService.persistWithoutId(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL);
				ps.setObject(1, expToSave, Types.INTEGER);
				ps.setObject(2, arIdToSave, Types.INTEGER);
				ps.setObject(3, period, Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);
	}

	private void persistServiceSegmentWeightingByExperiment(int expId, int sswId) {
		final Integer expToSave = expId;
		final Integer sswIdToSave = sswId;
		final Integer period = 0;

		final String SQL = DataLoadService.buildInsertSQL("r_experiment_v_service_segment_weighting", 3,
				"exp_ssw_exp, exp_ssw_ssw, exp_ssw_period");

		DataLoadService.persistWithoutId(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL);
				ps.setObject(1, expToSave, Types.INTEGER);
				ps.setObject(2, sswIdToSave, Types.INTEGER);
				ps.setObject(3, period, Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);
	}

	private void persistTravelTimeSetByExperiment(int expId, int ttsId) {
		final Integer expToSave = expId;
		final Integer ttsIdToSave = ttsId;
		final Integer period = 0;

		final String SQL = DataLoadService.buildInsertSQL("r_experiment_v_travel_time_set", 3,
				"exp_tts_exp, exp_tts_tts, exp_tts_period");

		DataLoadService.persistWithoutId(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL);
				ps.setObject(1, expToSave, Types.INTEGER);
				ps.setObject(2, ttsIdToSave, Types.INTEGER);
				ps.setObject(3, period, Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);
	}

	// private void persistServiceSegmentWeightingByRun(int runId,
	// ArrayList<PeriodSetting> settings) {
	//
	// final int runToSave = runId;
	// ArrayList<PeriodSetting> relevantSettings = new
	// ArrayList<PeriodSetting>();
	// for (int i = 0; i < settings.size(); i++) {
	// if (settings.get(i).getServiceSegmentWeightingId() != null ||
	// settings.get(i).getServiceSegmentWeightingId() != 0) {
	// relevantSettings.add(settings.get(i));
	// }
	// }
	// final ArrayList<PeriodSetting> relevantSettingsSSW = relevantSettings;
	//
	// DataLoadService.persistAll("r_run_v_service_segment_weighting", 3,
	// "run_ssw_run, run_ssw_ssw, run_ssw_period",
	// new BatchPreparedStatementSetter() {
	//
	// public int getBatchSize() {
	// return relevantSettingsSSW.size();
	// }
	//
	// public void setValues(PreparedStatement ps, int i) throws SQLException {
	//
	// PeriodSetting periodSetting = (PeriodSetting) relevantSettingsSSW.get(i);
	// ps.setInt(1, runToSave);
	// ps.setInt(2, periodSetting.getServiceSegmentWeightingId());
	// ps.setInt(3, periodSetting.getStartingPeriod());
	//
	// }
	// }, jdbcTemplate);
	// }

	private void persistCustomerSetByExperiment(int expId, int cusId) {
		final Integer expToSave = expId;
		final Integer cusIdToSave = cusId;
		final Integer period = 0;

		final String SQL = DataLoadService.buildInsertSQL("r_experiment_v_customer_set", 3,
				"exp_cs_exp, exp_cs_cs, exp_cs_period");

		DataLoadService.persistWithoutId(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL);
				ps.setObject(1, expToSave, Types.INTEGER);
				ps.setObject(2, cusIdToSave, Types.INTEGER);
				ps.setObject(3, period, Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);
	}

	private void persistCustomerSetByRun(int runId, ArrayList<PeriodSetting> settings) {

		final int runToSave = runId;
		ArrayList<PeriodSetting> relevantSettings = new ArrayList<PeriodSetting>();
		for (int i = 0; i < settings.size(); i++) {
			if (settings.get(i).getCustomerSetId() != null) {
				if (settings.get(i).getCustomerSetId() != 0) {
					relevantSettings.add(settings.get(i));
				}
			}
		}
		final ArrayList<PeriodSetting> relevantSettingsCS = relevantSettings;

		DataLoadService.persistAll("r_run_v_customer_set", 3, "run_cs_run, run_cs_cs, run_cs_period",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return relevantSettingsCS.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						PeriodSetting periodSetting = (PeriodSetting) relevantSettingsCS.get(i);
						ps.setInt(1, runToSave);
						ps.setInt(2, periodSetting.getCustomerSetId());
						ps.setInt(3, periodSetting.getStartingPeriod());

					}
				}, jdbcTemplate);
	}

	private void persistTimeWindowSetByExperiment(int expId, int twId) {
		final Integer expToSave = expId;
		final Integer twIdToSave = twId;
		final Integer period = 0;

		final String SQL = DataLoadService.buildInsertSQL("r_experiment_v_time_window_set", 3,
				"exp_tws_exp, exp_tws_tws, exp_tws_period");

		DataLoadService.persistWithoutId(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL);
				ps.setObject(1, expToSave, Types.INTEGER);
				ps.setObject(2, twIdToSave, Types.INTEGER);
				ps.setObject(3, period, Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);
	}

	private void persistTimeWindowSetByRun(int runId, ArrayList<PeriodSetting> settings) {

		final int runToSave = runId;
		ArrayList<PeriodSetting> relevantSettings = new ArrayList<PeriodSetting>();
		for (int i = 0; i < settings.size(); i++) {
			if (settings.get(i).getTimeWindowSetId() != null) {
				if (settings.get(i).getTimeWindowSetId() != 0) {
					relevantSettings.add(settings.get(i));
				}
			}
		}
		final ArrayList<PeriodSetting> relevantSettingsTWS = relevantSettings;

		DataLoadService.persistAll("r_run_v_time_window_set", 3, "run_tws_run, run_tws_tws, run_tws_period",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return relevantSettingsTWS.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						PeriodSetting periodSetting = (PeriodSetting) relevantSettingsTWS.get(i);
						ps.setInt(1, runToSave);
						ps.setInt(2, periodSetting.getTimeWindowSetId());
						ps.setInt(3, periodSetting.getStartingPeriod());

					}
				}, jdbcTemplate);
	}

	private void persistAlternativeSetByExperiment(int expId, int altId) {
		final Integer expToSave = expId;
		final Integer altToSave = altId;
		final Integer period = 0;

		final String SQL = DataLoadService.buildInsertSQL("r_experiment_v_alternative_set", 3,
				"exp_as_exp, exp_as_as, exp_as_period");

		DataLoadService.persistWithoutId(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL);
				ps.setObject(1, expToSave, Types.INTEGER);
				ps.setObject(2, altToSave, Types.INTEGER);
				ps.setObject(3, period, Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);
	}

	private void persistAlternativeSetByRun(int runId, ArrayList<PeriodSetting> settings) {

		final int runToSave = runId;
		ArrayList<PeriodSetting> relevantSettings = new ArrayList<PeriodSetting>();
		for (int i = 0; i < settings.size(); i++) {
			if (settings.get(i).getAlternativeSetId() != null) {
				if (settings.get(i).getAlternativeSetId() != 0) {
					relevantSettings.add(settings.get(i));
				}
			}
		}
		final ArrayList<PeriodSetting> relevantSettingsDAS = relevantSettings;

		DataLoadService.persistAll("r_run_v_alternative_set", 3, "run_as_run, run_as_as, run_as_period",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return relevantSettingsDAS.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						PeriodSetting periodSetting = (PeriodSetting) relevantSettingsDAS.get(i);
						ps.setInt(1, runToSave);
						ps.setInt(2, periodSetting.getAlternativeSetId());
						ps.setInt(3, periodSetting.getStartingPeriod());

					}
				}, jdbcTemplate);
	}

	private void persistDeliveryAreaSetByExperiment(int expId, int daId) {
		final Integer expToSave = expId;
		final Integer daToSave = daId;
		final Integer period = 0;

		final String SQL = DataLoadService.buildInsertSQL("r_experiment_v_delivery_area_set", 3,
				"exp_das_exp, exp_das_das, exp_das_period");

		DataLoadService.persistWithoutId(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL);
				ps.setObject(1, expToSave, Types.INTEGER);
				ps.setObject(2, daToSave, Types.INTEGER);
				ps.setObject(3, period, Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);
	}

	private void persistDeliveryAreaSetByRun(int runId, ArrayList<PeriodSetting> settings) {

		final int runToSave = runId;
		ArrayList<PeriodSetting> relevantSettings = new ArrayList<PeriodSetting>();
		for (int i = 0; i < settings.size(); i++) {

			if (settings.get(i).getDeliveryAreaSetId() != null) {
				if (settings.get(i).getDeliveryAreaSetId() != 0) {
					relevantSettings.add(settings.get(i));
				}
			}
		}
		final ArrayList<PeriodSetting> relevantSettingsDAS = relevantSettings;

		DataLoadService.persistAll("r_run_v_delivery_area_set", 3, "run_das_run, run_das_das, run_das_period",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return relevantSettingsDAS.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						PeriodSetting periodSetting = (PeriodSetting) relevantSettingsDAS.get(i);
						ps.setInt(1, runToSave);
						ps.setInt(2, periodSetting.getDeliveryAreaSetId());
						ps.setInt(3, periodSetting.getStartingPeriod());

					}
				}, jdbcTemplate);
	}

	private void persistValueFunctionApproximationModelByExperiment(int expId, int modelId) {
		final Integer expToSave = expId;
		final Integer modelIdToSave = modelId;
		final Integer period = 0;

		final String SQL = DataLoadService.buildInsertSQL("r_experiment_v_value_function_approximation_model_set", 3,
				"exp_vfa_exp, exp_vfa_vfa, exp_vfa_period");

		DataLoadService.persistWithoutId(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL);
				ps.setObject(1, expToSave, Types.INTEGER);
				ps.setObject(2, modelIdToSave, Types.INTEGER);
				ps.setObject(3, period, Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);
	}

	private void persistValueFunctionApproximationModelByRun(int runId, ArrayList<PeriodSetting> settings) {

		final int runToSave = runId;
		ArrayList<PeriodSetting> relevantSettings = new ArrayList<PeriodSetting>();
		for (int i = 0; i < settings.size(); i++) {

			if (settings.get(i).getDeliveryAreaSetId() != null) {
				if (settings.get(i).getValueFunctionModelSetId() != 0) {
					relevantSettings.add(settings.get(i));
				}
			}
		}
		final ArrayList<PeriodSetting> relevantSettingsMod = relevantSettings;

		DataLoadService.persistAll("r_run_v_value_function_approximation_model_set", 3,
				"run_vfa_run, run_vfa_vfa, run_vfa_period", new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return relevantSettingsMod.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						PeriodSetting periodSetting = (PeriodSetting) relevantSettingsMod.get(i);
						ps.setInt(1, runToSave);
						ps.setInt(2, periodSetting.getValueFunctionModelSetId());
						ps.setInt(3, periodSetting.getStartingPeriod());

					}
				}, jdbcTemplate);
	}

	private void persistVehicles(int expId, ArrayList<Vehicle> vehicles) {

		final ArrayList<Vehicle> vehiclesToSave = vehicles;
		final int expIdFinal = expId;

		DataLoadService.persistAll("r_experiment_v_vehicle_type", 3, "exp_vehicle_exp,exp_vehicle_veh,exp_vehicle_no",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return vehiclesToSave.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						Vehicle vehicle = vehiclesToSave.get(i);
						ps.setInt(1, expIdFinal);
						ps.setInt(2, vehicle.getVehicleTypeId());
						ps.setObject(3, vehicle.getVehicleNo());

					}
				}, jdbcTemplate);
	}

	private void persistObjectives(Experiment experiment) {

		final ArrayList<ObjectiveWeight> objToSave = experiment.getObjectives();
		final int expId = experiment.getId();

		DataLoadService.persistAll("r_experiment_v_objective", 3, "exp_obj_exp,exp_obj_obj,exp_obj_weight",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return objToSave.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						ObjectiveWeight weight = objToSave.get(i);
						ps.setInt(1, expId);
						ps.setInt(2, weight.getObjectiveTypeId());
						ps.setObject(3, weight.getValue());

					}
				}, jdbcTemplate);
	}

	private void persistKpiValuesOfRun(ArrayList<PeriodSetting> periods, Integer runId) {

		ArrayList<Kpi> kpiValues = new ArrayList<Kpi>();

		for (int i = 0; i < periods.size(); i++) {
			kpiValues.addAll(((PeriodSetting) periods.get(i)).getKpis());
		}

		final ArrayList<Kpi> kpiValuesToSave = kpiValues;
		final int runIdToSave = runId;
		DataLoadService.persistAll("r_run_v_kpi", 4, "run_kpi_run, run_kpi_kpi, run_kpi_value, run_kpi_period",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return kpiValuesToSave.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						Kpi kpi = (Kpi) kpiValuesToSave.get(i);
						ps.setInt(1, runIdToSave);
						ps.setObject(2, kpi.getKpyTypeId(), Types.INTEGER);
						ps.setObject(3, kpi.getValue(), Types.FLOAT);
						ps.setObject(4, kpi.getPeriod(), Types.INTEGER);

					}
				}, jdbcTemplate);

	}

	/**
	 * Saves all parameter values of the provided period
	 * 
	 * @param initialSetting
	 * @param expId
	 */
	private void persistParameterValuesOfExperiment(PeriodSetting initialSetting, Integer expId) {

		final ArrayList<GeneralParameterValue> parametersToSave = initialSetting.getParameterValues();
		final int expToSave = expId;

		DataLoadService.persistAll("r_experiment_v_parameter_type", 3,
				"exp_parameter_exp, exp_parameter_parameter, exp_parameter_value", new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return parametersToSave.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						GeneralParameterValue parameter = (GeneralParameterValue) parametersToSave.get(i);
						ps.setInt(1, expToSave);
						ps.setInt(2, parameter.getParameterTypeId());
						ps.setObject(3, parameter.getValue(), Types.FLOAT);

					}
				}, jdbcTemplate);

	}

	/**
	 * Saves all atomic output values of the provided period
	 * 
	 * @param initialSetting
	 * @param expId
	 */
	private void persistAtomicOutputsOfRun(ArrayList<PeriodSetting> settings, Integer runId) {

		final int runToSave = runId;
		ArrayList<GeneralAtomicOutputValue> relevantSettings = new ArrayList<GeneralAtomicOutputValue>();
		for (int i = 0; i < settings.size(); i++) {

			if (settings.get(i).getAtomicOutputs() != null) {
				if (settings.get(i).getAtomicOutputs().size() != 0) {
					for (PeriodSetting s : settings) {
						for (GeneralAtomicOutputValue g : s.getAtomicOutputs()) {
							g.setPeriodNo(s.getStartingPeriod());
						}
					}
					relevantSettings.addAll(settings.get(i).getAtomicOutputs());
				}
			}
		}
		final ArrayList<GeneralAtomicOutputValue> relevantSettingsAt = relevantSettings;

		DataLoadService.persistAll("r_run_v_parameter_type", 4,
				"run_parameter_run, run_parameter_parameter, run_parameter_value, run_period",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return relevantSettingsAt.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						GeneralAtomicOutputValue ov = (GeneralAtomicOutputValue) relevantSettingsAt.get(i);

						ps.setInt(1, runToSave);
						ps.setInt(2, ov.getParameterTypeId());
						ps.setObject(3, ov.getValue(), Types.FLOAT);
						ps.setObject(4, ov.getPeriodNo(), Types.INTEGER);

					}
				}, jdbcTemplate);

	}

	private Experiment persistExperiment(Experiment experiment) {
		final Experiment experimentToSave = experiment;
		final String SQL = DataLoadService.buildInsertSQL("experiment", 11,
				"exp_description, exp_responsible, exp_occasion, exp_region, exp_processType, exp_booking_period_length, exp_incentive_type, exp_booking_period_no, exp_name, exp_depot,exp_copy_exp");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "exp_id" });
				ps.setObject(1, experimentToSave.getDescription(), Types.VARCHAR);
				ps.setObject(2, experimentToSave.getResponsible(), Types.VARCHAR);
				ps.setObject(3, experimentToSave.getOccasion(), Types.VARCHAR);
				ps.setObject(4, experimentToSave.getRegionId(), Types.INTEGER);
				ps.setObject(5, experimentToSave.getProcessTypeId(), Types.INTEGER);
				ps.setObject(6, experimentToSave.getBookingPeriodLength(), Types.INTEGER);
				ps.setObject(7, experimentToSave.getIncentiveTypeId(), java.sql.Types.INTEGER);
				ps.setObject(8, experimentToSave.getBookingPeriodNumber(), Types.INTEGER);
				ps.setObject(9, experimentToSave.getName(), Types.VARCHAR);
				ps.setObject(10, experimentToSave.getDepotId(), Types.INTEGER);
				ps.setObject(11, experimentToSave.getCopyExperimentId(), Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);

		experimentToSave.setId(id);

		return experimentToSave;
	}

	private Run persistRun(Integer expId, long runtime) {
		final Integer expIdToSave = expId;
		final long runtimeToSave = runtime;
		final String SQL = DataLoadService.buildInsertSQL("run", 2, "run_experiment, run_length");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "run_id" });
				ps.setObject(1, expIdToSave, Types.INTEGER);
				ps.setObject(2, runtimeToSave, Types.BIGINT);
				return ps;
			}
		}, jdbcTemplate);

		Run run = new Run();
		run.setExperimentId(expIdToSave);
		run.setId(id);

		return run;
	}

	@Override
	public Integer persistRunSettings(Experiment experiment, PeriodSetting initialSetting,
			ArrayList<PeriodSetting> followerSettings, SettingRequest request, ArrayList<PeriodSettingType> outputs,
			long runtime) {
		// Produce run
		final Run runToSave = this.persistRun(experiment.getId(), runtime);

		// Persist output of settings

		/// Kpi values are always output. Can directly save from all periods
		ArrayList<PeriodSetting> settings = new ArrayList<PeriodSetting>();
		settings.add(initialSetting);
		if (followerSettings != null) {
			settings.addAll(followerSettings);
		}

		this.persistKpiValuesOfRun(settings, runToSave.getId());

		// Atomic outputs are always outputs
		this.persistAtomicOutputsOfRun(settings, runToSave.getId());

		/// Delivery area set can be both
		boolean save = false;
		for (int i = 0; i < outputs.size(); i++) {
			if (outputs.get(i).equals(PeriodSettingType.DELIVERYAREASET)) {
				save = true;
				break;
			}
		}
		if (save) {
			this.persistDeliveryAreaSetByRun(runToSave.getId(), settings);
		}

		// Alternative set can be both
		save = false;
		for (int i = 0; i < outputs.size(); i++) {
			if (outputs.get(i).equals(PeriodSettingType.ALTERNATIVESET)) {
				save = true;
				break;
			}
		}
		if (save) {
			this.persistAlternativeSetByRun(runToSave.getId(), settings);
		}

		// Time window set can be both
		save = false;
		for (int i = 0; i < outputs.size(); i++) {
			if (outputs.get(i).equals(PeriodSettingType.TIMEWINDOWSET)) {
				save = true;
				break;
			}
		}
		if (save) {
			this.persistTimeWindowSetByRun(runToSave.getId(), settings);
		}

		// Customer set can be both
		save = false;
		for (int i = 0; i < outputs.size(); i++) {
			if (outputs.get(i).equals(PeriodSettingType.CUSTOMERSET)) {
				save = true;
				break;
			}
		}
		if (save) {
			this.persistCustomerSetByRun(runToSave.getId(), settings);
		}

		// Demand segment weighting can be both
		// TODO Adapt such that several weightings per period (t) are possible
		// (add run_dsw_t)
		save = false;
		for (int i = 0; i < outputs.size(); i++) {
			if (outputs.get(i).equals(PeriodSettingType.DEMANDSEGMENTWEIGHTING)) {
				save = true;
				break;
			}
		}
		if (save) {
			this.persistDSWeightingByRun(runToSave.getId(), settings);
		}

		// Order set can be both
		save = false;
		for (int i = 0; i < outputs.size(); i++) {
			if (outputs.get(i).equals(PeriodSettingType.ORDERSET)) {
				save = true;
				break;
			}
		}
		if (save) {
			this.persistOrderSetByRun(runToSave.getId(), settings);
		}

		// Forecast set can be both
		save = false;
		for (int i = 0; i < outputs.size(); i++) {
			if (outputs.get(i).equals(PeriodSettingType.DEMANDFORECASTSET_VALUEBUCKETS)) {
				save = true;
				break;
			}
		}
		if (save) {
			this.persistValueBucketForecastSetByRun(runToSave.getId(), settings);
		}

		// Capacity set can be both
		save = false;
		for (int i = 0; i < outputs.size(); i++) {
			if (outputs.get(i).equals(PeriodSettingType.CAPACITYSET)) {
				save = true;
				break;
			}
		}
		if (save) {
			this.persistCapacitySetByRun(runToSave.getId(), settings);
		}

		// Control set can be both
		save = false;
		for (int i = 0; i < outputs.size(); i++) {
			if (outputs.get(i).equals(PeriodSettingType.CONTROLSET)) {
				save = true;
				break;
			}
		}
		if (save) {
			this.persistControlSetByRun(runToSave.getId(), settings);
		}

		// Value bucket set can be both
		save = false;
		for (int i = 0; i < outputs.size(); i++) {
			if (outputs.get(i).equals(PeriodSettingType.VALUEBUCKETSET)) {
				save = true;
				break;
			}
		}
		if (save) {
			this.persistValueBucketSetByRun(runToSave.getId(), settings);
		}

		// Order request sets can be input and output of multiple periods
		save = false;
		for (int i = 0; i < outputs.size(); i++) {
			if (outputs.get(i).equals(PeriodSettingType.ORDERREQUESTSET)) {
				save = true;
				break;
			}
		}
		if (save) {
			this.persistOrderRequestSetByRun(runToSave.getId(), settings);
		}

		// One routing can be input but several routings per period can be
		// output
		this.persistRoutingByRun(runToSave.getId(), settings,
				request.getPeriodSettings().containsKey(PeriodSettingType.INITIALROUTING),
				request.getPeriodSettings().containsKey(PeriodSettingType.FINALROUTING));

		return runToSave.getId();
	}

	@Override
	public ArrayList<Integer> getOrderRequestSetsPerExperiment(Integer orderRequestSetId) {
		String sql = "SELECT run_ors_ors FROM "+SettingsProvider.database+".r_run_v_order_request_set JOIN (SELECT run_id FROM "+SettingsProvider.database+".run WHERE run_experiment=(SELECT run_experiment FROM "+SettingsProvider.database+".run WHERE run_id=(SELECT run_ors_run FROM "+SettingsProvider.database+".r_run_v_order_request_set WHERE run_ors_ors=?))) AS run_selector ON (run_selector.run_id=r_run_v_order_request_set.run_ors_run)";
		ArrayList<Integer> entities = DataLoadService.loadIntegersComplexPreparedStatementMultipleEntities(sql,
				new Object[] { orderRequestSetId }, jdbcTemplate);
		return entities;
	}

	@Override
	public ArrayList<Integer> getAllCopyExperimentsPerExperiment(Integer experimentId) {
		String sql = "SELECT exp_id FROM "+SettingsProvider.database+".experiment where exp_copy_exp=?";
		ArrayList<Integer> entities = DataLoadService.loadIntegersComplexPreparedStatementMultipleEntities(sql,
				new Object[] { experimentId }, jdbcTemplate);
		return entities;
	}

}
