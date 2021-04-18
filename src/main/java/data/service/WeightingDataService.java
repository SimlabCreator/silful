package data.service;

import java.util.ArrayList;

import data.entity.Entity;
import data.entity.WeightEntity;
import data.entity.WeightingEntity;

/**
 * Interface for all data services for sets like service time segment and demand segment that have weightings associated with them. Ensures common requests.
 * 
 * @author M. Lang
 *
 */
public abstract class WeightingDataService extends SetDataService{
	
	protected ArrayList<WeightingEntity> weightings; 
	

	/**
	 * Get all possible weightings
	 * @return List of weightings
	 */
	public abstract ArrayList<WeightingEntity> getAllWeightings();
	
	/**
	 * Get all weightings that fit to the respective set
	 * @param setId Respective set
	 * @return List of weightings
	 */
	public abstract ArrayList<WeightingEntity> getAllWeightingsBySetId(int setId);
	
	/**
	 * Get a specific weighting set
	 * @param weightingId Set id
	 * @return Weighting
	 */
	public abstract WeightingEntity getWeightingById(int weightingId);
	
	/**
	 * Get all weights from the respective weighting
	 * @param weightingId Respective weighting
	 * @return List of weights
	 */
	public abstract ArrayList<? extends WeightEntity> getAllWeightsByWeightingId(int weightingId);
	
	/**
	 * Get a specific weight
	 * @param weightId Respective weight id
	 * @return Weight
	 */
	public abstract Entity getWeightById(int weightId);
	
	/**
	 * Save weight
	 * @param weight Respective weight
	 * @return id
	 */
	public abstract Integer persistWeight(WeightEntity weight);
	
	/**
	 * Save weighting
	 * @param weighting Respective weighting
	 * @return id
	 */
	protected abstract Integer persistWeighting(WeightingEntity weighting);
	
	/**
	 * Save complete weighting with weights
	 * @param weighting Respective weighting
	 * @return id
	 */
	public abstract Integer persistCompleteWeighting(WeightingEntity weighting);
	

}
