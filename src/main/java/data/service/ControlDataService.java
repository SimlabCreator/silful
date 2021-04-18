package data.service;

import java.util.ArrayList;
import java.util.HashMap;

import data.entity.Control;
import data.entity.DynamicProgrammingTree;
import data.entity.Entity;
import data.entity.SetEntity;

public abstract class ControlDataService extends SetDataService {

	/**
	 * Provides all control sets that fit to the respective delivery area and
	 * alternative set
	 * 
	 * @param deliveryAreaSetId
	 *            Respective delivery area set
	 * @param alternativeSetId
	 *            Respective alternative set
	 * @return
	 */
	public abstract ArrayList<SetEntity> getAllSetsByDeliveryAreaSetAndTimeWindowSetId(Integer deliveryAreaSetId,
			Integer alternativeSetId);

	/**
	 * Provides all control sets that fit to the respective delivery area and
	 * time window set and value bucket set
	 * 
	 * @param deliveryAreaSetId
	 *            Respective delivery area set
	 * @param alternativeSetId
	 *            Respective alternative set
	 * @param valueBucketSetId
	 *            Respective value bucket set
	 * @return
	 */
	public abstract ArrayList<SetEntity> getAllSetsByDeliveryAreaSetAndAlternativeSetAndValueBucketSetId(
			Integer deliveryAreaSetId, Integer alternativeSetId, Integer valueBucketSetId);

	/**
	 * Provides all dynamic programming trees that fit to the respective
	 * selection criteria
	 * 
	 * @param periodLength
	 *            T
	 * @param capacitySetId
	 *            Respective capacity set
	 * @param arrivalProcessId
	 *            Respective arrival process
	 * @param demandSegmentWeighting
	 *            Respective demand segment weighting
	 * @param deliveryAreaSetId
	 *            Respective delivery area set
	 * @return
	 */
	public abstract ArrayList<Entity> getAllDynamicProgrammingTreesByMultipleSelectionIds(Integer periodLength,
			Integer capacitySetId, Integer arrivalProcessId, Integer demandSegmentWeighting, Integer deliveryAreaSetId);

	/**
	 * Provides a specific dynamic programming tree
	 * 
	 * @param treeId
	 *            Respective tree id
	 * @return
	 */
	public abstract DynamicProgrammingTree getDynamicProgrammingTreeById(Integer treeId);

	/**
	 * Saves a dynamic programming tree
	 * 
	 * @param tree
	 *            Respective tree to save
	 */
	public abstract Integer persistDynamicProgrammingTree(Entity tree);
	
	/**
	 * Provides all subtrees by delivery area ids for a dynamic programming tree
	 * @param treeId Respective dynamic programming tree id
	 * @return
	 */
	public abstract HashMap<Integer,String> getAllTreesByDynamicProgrammingTreeId(Integer treeId);
	
	@Override
	public abstract ArrayList<Control> getAllElementsBySetId(int setId);
	
	@Override
	public abstract Control getElementById(int entityId);
}
