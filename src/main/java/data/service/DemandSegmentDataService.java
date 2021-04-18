package data.service;

import java.util.ArrayList;

import data.entity.ConsiderationSetAlternative;
import data.entity.DemandSegment;
import data.entity.DemandSegmentWeight;
import data.entity.SetEntity;
import data.entity.VariableCoefficient;

public abstract class DemandSegmentDataService extends WeightingReadOnlyDataService{	

	/**
	 * Provides all alternatives and their weighting/coefficients per demand segment that are within its consideration set
	 * @param demandSegmentId Respective demand segment
	 * @return alternatives of the consideration set
	 */
	public abstract ArrayList<ConsiderationSetAlternative> getConsiderationSetAlternativesByDemandSegmentId(Integer demandSegmentId);
	
	/**
	 * Provides a specific consideration set alternative
	 * @param considerationSetId Respective id
	 * @return consideration set alternative
	 */
	public abstract ConsiderationSetAlternative getConsiderationSetAlternativeById(Integer considerationSetId);
	
	
	/**
	 * Provides all variable coefficients that are needed for the utility determination within a parametric demand model
	 * @param demandSegmentId Respective demand segment
	 * @return variable coefficients
	 */
	public abstract ArrayList<VariableCoefficient> getVariableCoefficientsByDemandSegmentId(Integer demandSegmentId);
	

	/**
	 * Provides all demand segment sets that refer to a residence area set of the respective region
	 * @param regionId Respective region
	 * @return list of demand segment sets
	 */
	public abstract ArrayList<SetEntity> getAllSetsByRegionAndAlternativeSetId(Integer regionId, Integer alternativeSetId);
	
	@Override
	public abstract ArrayList<DemandSegment> getAllElementsBySetId(int setId);
	
	@Override
	public abstract DemandSegment getElementById(int entityId);
	
	@Override
	public abstract ArrayList<DemandSegmentWeight> getAllWeightsByWeightingId(int weightingId);
	
	public abstract ArrayList<ConsiderationSetAlternative> getConsiderationSetAlternativesBySetId(Integer setId) ;

}
