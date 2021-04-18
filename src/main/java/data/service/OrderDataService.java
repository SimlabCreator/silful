package data.service;

import java.util.ArrayList;

import data.entity.Experiment;
import data.entity.Order;
import data.entity.OrderSet;
import data.entity.ReferenceRouting;
import data.entity.SetEntity;

public abstract class OrderDataService extends SetDataService{
	
	/**
	 * Provides all order sets that belong to the respective order request set
	 * @param orderRequestSetId Respective order request set
	 * @return List of order sets
	 */
	public abstract ArrayList<SetEntity> getAllByOrderRequestSetId(Integer orderRequestSetId);


	/**
	 * Allows to persist an OrderSet (without the elements)
	 */
	public abstract Integer persistEntitySet(SetEntity setEntity);
	
	@Override
	public abstract ArrayList<Order> getAllElementsBySetId(int setId);
	
	@Override
	public abstract Order getElementById(int entityId);
	
	/**
	 * Provides all reference routing informations for the respective order set
	 * @param orderSetId Respective order set id
	 * @return
	 */
	public abstract ArrayList<ReferenceRouting> getReferenceRoutingsByOrderSetId(int orderSetId);
	
	public abstract ArrayList<Experiment> getAllNonCopyExperimentsWithOrderSetOutputByDemandSegmentSetId(int demandSegmentSetId);
	
	/**
	 * Provides all  order sets that were produced by the respective experiments
	 * @param expId Respective experiment id
	 * @return
	 */
	public abstract ArrayList<OrderSet> getAllOrderSetsByExperimentId(int expId);

	public abstract int getHighestOrderId();

	public abstract void persistOrders(Integer setId, ArrayList<Order> orders, boolean predefinedIds);
}
