package data.service;

import java.util.ArrayList;

import data.entity.Entity;
import data.entity.Experiment;
import data.entity.ObjectiveWeight;
import data.entity.PeriodSetting;
import data.utility.PeriodSettingType;
import data.utility.SettingRequest;

/**
 * Provides settings, i.e. run and period settings
 * 
 * @author M. Lang
 *
 */
public abstract class SettingsDataService extends DataService {

	/**
	 * Get all previously saved experiments (for possible reuse of settings)
	 * 
	 * @return list of experiments
	 */
	public abstract ArrayList<Entity> getAllExperiments();

	/**
	 * Get a specific experiment
	 * 
	 * @param expId
	 * @return
	 */
	public abstract Experiment getExperimentById(int expId);

	/**
	 * Get initial period settings from a given experiment
	 * 
	 * @param expId
	 *            Run
	 * @return initial period setting
	 */
	public abstract PeriodSetting getInputPeriodSettingsInitialByExperiment(int expId);

	/**
	 * Get follower period settings from a given experiment
	 * 
	 * @param expId
	 * @return list of follower settings
	 */
	public abstract ArrayList<PeriodSetting> getInputPeriodSettingFollowersByExperiment(int expId);

	/**
	 * Save an experiment and its settings in the database
	 * 
	 * @param experiment
	 *            Experiment
	 * @param initialSetting
	 *            Initial period settings
	 * @param followerSettings
	 *            Settings of later periods
	 * @param request
	 *            Requested settings, thus, input settings
	 * @return
	 */
	public abstract Integer persistExperimentSettings(Experiment experiment, PeriodSetting initialSetting,
			ArrayList<PeriodSetting> followerSettings, SettingRequest request, ArrayList<PeriodSettingType> outputs, long runtime);

	/**
	 * Persist a run and its settings in the database. Experiment already
	 * persisted.
	 * 
	 * @param experiment
	 *            Experiment
	 * @param initialSetting
	 *            Initial period settings
	 * @param followerSettings
	 *            Settings of later periods
	 * @param request
	 *            Requested settings, thus, input settings
	 * @return
	 */
	public abstract Integer persistRunSettings(Experiment experiment, PeriodSetting initialSetting,
			ArrayList<PeriodSetting> followerSettings, SettingRequest request, ArrayList<PeriodSettingType> outputs, long runtime);



	/**
	 * Get objectives of experiment (type + weight)
	 * 
	 * @param experiment
	 * @return
	 */
	public abstract ArrayList<ObjectiveWeight> getObjectivesByExperiment(Experiment experiment);
	
	/**
	 * Get all order request set ids by experiment
	 * @orderRequestSetId Id of one order request belonging to that experiment
	 */

	public abstract ArrayList<Integer> getOrderRequestSetsPerExperiment(Integer orderRequestSetId);
	
	/**
	 * Provides all experiments that are a copy of the provided one
	 * @param experimentId
	 * @return
	 */
	public abstract ArrayList<Integer> getAllCopyExperimentsPerExperiment(Integer experimentId);
}
