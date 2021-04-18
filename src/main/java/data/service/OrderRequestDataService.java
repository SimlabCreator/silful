package data.service;

import java.util.ArrayList;
import java.util.HashMap;

import data.entity.Experiment;
import data.entity.OrderRequest;
import data.entity.OrderRequestSet;
import data.entity.SetEntity;

public abstract class OrderRequestDataService extends SetDataService{
	
	/**
	 * Save entity set with all respective entities
	 * @param setEntity entity set with all respective entities
	 * @return id of the set
	 */
	public abstract Integer persistCompleteOrderRequestAndCustomerSet(final SetEntity setEntity);
	
	/**
	 * Provides all order request sets that belong to the respective customer set
	 * @param customerSetId Respective customer set
	 * @return List of order request sets
	 */
	public abstract ArrayList<SetEntity> getAllByCustomerSetId(Integer customerSetId);
	
	/**
	 * Provides all order request sets that belong to a specific period length and alternative set
	 * @param periodLength Respective booking period length
	 * @param alternativeSetId Respective alternative set id
	 * @return List of order request sets
	 */
	public abstract ArrayList<SetEntity> getAllByBookingPeriodLengthAndAlternativeSetId(Integer periodLength, Integer alternativeSetId);
	
	/**
	 * Provides all order request sets that belong to and alternative set
	 * @param alternativeSetId Respective alternative set id
	 * @return List of order request sets
	 */
	public abstract ArrayList<SetEntity> getAllByAlternativeSetId(Integer alternativeSetId);
	
	/**
	 * Loads the sampled preferences from the order request
	 * @param orderRequestId Respective order request
	 * @return Preference list with alternative id and utility value
	 */
	public abstract HashMap<Integer,Double> getSampledPreferencesByElement(Integer orderRequestId);
	
	@Override
	public abstract ArrayList<OrderRequest> getAllElementsBySetId(int setId);
	
	@Override
	public abstract OrderRequest getElementById(int entityId);
	
	/**
	 * Provides all experiments that produces order request sets and fits to the respective alternative set
	 * @param alternativeSetId respective alternative set
	 * @return List of experiments
	 */
	public abstract ArrayList<Experiment> getAllExperimentsWithOrderRequestSetOutputByDemandSegmentSetId( int demandSegmentSetId);

	/**
	 * Provides all experiments that produces order request sets and fits to the respective alternative set and booking period length
	 * @param alternativeSetId respective alternative set
	 * @param bookingPeriodLength booking period length
	 * @return List of experiments
	 */
	public abstract ArrayList<Experiment> getAllExperimentsWithOrderRequestSetOutputByDemandSegmentSetIdAndBookingHorizonLength(int demandSegmentSetId, int bookingPeriodLength);

	/**
	 * Provides all order request sets that were produced by the respective experiments
	 * @param expId Respective experiment id
	 * @return
	 */
	public abstract ArrayList<? extends SetEntity> getAllOrderRequestSetsByExperimentId(int expId);
	
	/**
	 * Provides all experiments that created order request sets as outputs and fit to the respective alternative set
	 * @param alternativeSetId Respective alternative set id
	 * @return
	 */
	public abstract ArrayList<Experiment> getAllExperimentsWithOrderRequestSetOutputByAlternativeSetId(int alternativeSetId);

	/**
	 * Provides all experiments that created order request sets as outputs and fit to the respective alternative set and order horizon length
	 * @param alternativeSetId Respective alternative set id
	 * @param orderHorizonLength Respective order horizon length
	 * @return
	 */
	public abstract ArrayList<Experiment> getAllExperimentsWithOrderRequestSetOutputByAlternativeSetIdAndOrderHorizonLength(int alternativeSetId, int orderHorizonLength);

}
