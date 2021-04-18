package data.service;

import java.util.ArrayList;

import data.entity.Alternative;
import data.entity.AlternativeOffer;
import data.entity.Order;
import data.entity.SetEntity;
import data.entity.TimeWindow;

public abstract class AlternativeDataService extends SetDataService {

	/**
	 * Returns all alternatives that were available on order acceptance of the
	 * respective order
	 * 
	 * @param orderId
	 *            Respective order
	 * @return List of alternatives
	 */
	public abstract ArrayList<Alternative> getAvailableAlternativesByOrderId(Integer orderId);

	/**
	 * Saves all alternatives that were available on order acceptance
	 * Does not save the alternatives themselves but the allocation
	 * @param order
	 */
	public abstract void persistAvailableAlternatives(Order order);
	
	/**
	 * Returns all alternatives that were offered on order acceptance of the
	 * respective order
	 * 
	 * @param orderId
	 *            Respective order
	 * @return List of alternatives
	 */
	public abstract ArrayList<AlternativeOffer> getOfferedAlternativesByOrderId(Integer orderId);
	
	/**
	 * Saves all AlternativeOffers that were available on order acceptance 
	 * @param order
	 */
	public abstract void persistOfferedAlternatives(Order order);
	
	/**
	 * Returns all alternative sets that refer to the respective time window set
	 * @param timeWindowId Respective time window set id
	 * @return List of sets
	 */
	public abstract ArrayList<SetEntity> getAllSetsByTimeWindowSetId(Integer timeWindowSetId);
	
	/**
	 * Returns all time windows that are referenced by the respective alternative
	 * @param alternativeId Respective alternative
	 * @return
	 */
	public abstract ArrayList<TimeWindow> getTimeWindowsByAlternativeId(Integer alternativeId);
	
	/**
	 * Saves the time windows that belong to the respective alternatives
	 * @param alternatives Respective alternatives with time windows
	 */
	public abstract void persistTimeWindowsOfAlternatives(ArrayList<Alternative> alternatives);
	
	
	@Override
	public abstract ArrayList<Alternative> getAllElementsBySetId(int setId);
	
	@Override
	public abstract Alternative getElementById(int entityId);
}
