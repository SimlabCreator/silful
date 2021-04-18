package logic.utility;

import java.util.ArrayList;

import org.rosuda.JRI.Rengine;

import data.entity.Experiment;
import data.entity.PeriodSetting;
import data.entity.Run;
import data.utility.PeriodSettingType;
import data.utility.SettingRequest;

/**
 * Provides the settings of the current run and period
 * 
 * @author M. Lang
 *
 */
public class SettingsProvider {
	
	public static String database="SimLab";
	public static Boolean saveResults =true;
	private static Rengine re;

	public static Rengine getRe() {
		if(re==null){
			re = new Rengine(new String[] { "--no-save" }, false, new Solver());
		}
		
		return re;
	}

	private static Experiment experiment;
	private static Run currentRun;
	private static PeriodSetting initialPeriodSetting;
	private static ArrayList<PeriodSetting> periodSettingFollowers;
	private static SettingRequest request;
	private static ArrayList<PeriodSettingType> outputs;
	private static Integer noOfRepetitions;
	private static int currentRepetition;
	private static ArrayList<Integer> orderRequestSetsForRepetitions;

	public static Run getCurrentRun() {
		return currentRun;
	}

	public static void setCurrentRun(Run currentRun) {
		SettingsProvider.currentRun = currentRun;
	}

	/**
	 * Provides the initial period setting
	 * 
	 * @return PeriodSetting
	 */
	public static PeriodSetting getPeriodSetting() {

		return initialPeriodSetting;
	}

	public static void setPeriodSetting(PeriodSetting initialPeriodSetting) {
		SettingsProvider.initialPeriodSetting = initialPeriodSetting;
	}

	public static PeriodSetting getPeriodSettingFollower(int periodNumber) {
		return SettingsProvider.periodSettingFollowers.get(periodNumber - 1);
	}

	public static ArrayList<PeriodSetting> getPeriodSettingFollowers() {
		return SettingsProvider.periodSettingFollowers;
	}

	public static void setPeriodSettingFollowers(ArrayList<PeriodSetting> periodSettingFollowers) {
		SettingsProvider.periodSettingFollowers = periodSettingFollowers;
	}

	public static Experiment getExperiment() {
		return experiment;
	}

	public static void setExperiment(Experiment experiment) {
		SettingsProvider.experiment = experiment;
	}

	public static SettingRequest getSettingRequest() {
		return request;
	}

	public static void setSettingRequest(SettingRequest request) {
		SettingsProvider.request = request;
	}

	public static ArrayList<PeriodSettingType> getOutputs() {
		return outputs;
	}

	public static void setOutputs(ArrayList<PeriodSettingType> outputs) {
		SettingsProvider.outputs = outputs;
	}

	public static Integer getNoOfRepetitions() {
		return noOfRepetitions;
	}

	public static void setNoOfRepetitions(Integer noOfRepetitions) {
		SettingsProvider.noOfRepetitions = noOfRepetitions;
	}

	public static int getCurrentRepetition() {
		return currentRepetition;
	}

	public static void setCurrentRepetition(int currentRepetition) {
		SettingsProvider.currentRepetition = currentRepetition;
	}

	public static ArrayList<Integer> getOrderRequestSetsForRepetitions() {
		return orderRequestSetsForRepetitions;
	}

	public static void setOrderRequestSetsForRepetitions(ArrayList<Integer> orderRequestSetsForRepetitions) {
		SettingsProvider.orderRequestSetsForRepetitions = orderRequestSetsForRepetitions;
	}

	public static Integer getOrderRequestSetIdForRepetition(Integer repetitionNo) {
		return SettingsProvider.orderRequestSetsForRepetitions.get(repetitionNo);
	}

	public static Boolean doesOrderRequestSetChange() {
		if (SettingsProvider.orderRequestSetsForRepetitions != null) {
			return true;
		}
		return false;
	}

}
