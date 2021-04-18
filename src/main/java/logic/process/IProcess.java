package logic.process;

import java.util.ArrayList;

import data.utility.PeriodSettingType;
import data.utility.SettingRequest;

/**
 * Interface for all e-fulfillment and data generation processes
 * 
 * @author M. Lang
 *
 */
public interface IProcess {

	/**
	 * Asks process if the user can specify further objectives
	 * @return
	 */
	public abstract Boolean multipleObjectivesPossible();
	
	/**
	 * Asks process if it needs incentive type
	 * 
	 * @return
	 */
	public abstract Boolean needIncentiveType();

	/**
	 * Asks process if it needs booking period length
	 * 
	 * @return
	 */
	public abstract Boolean needBookingPeriodLength();
	
	/**
	 * Asks process if it needs a depot location for the routing
	 * @return
	 */
	public abstract Boolean needDepotLocation();
	
	/**
	 * Asks if process allows running for multiple periods
	 * @return
	 */
	public abstract Boolean multiplePeriodsPossible();

	/**
	 * Provides the relevant settings for the given process. Asks all internal
	 * algorithms for their setting needs.
	 * 
	 * @return
	 */
	public abstract SettingRequest getSettingRequest();
	
	/**
	 * Provides the outputs of the process. Asks all internal algorithms for their outputs. getSettingRequest() needs to be run before
	 * @return
	 */
	public abstract ArrayList<PeriodSettingType> getOutputs();

	/**
	 * Starts the process
	 */
	public abstract void start();

}
