package data.service;

import java.util.ArrayList;

import data.entity.SetEntity;
import data.entity.ValueFunctionApproximationCoefficient;
import data.entity.ValueFunctionApproximationModel;
import data.entity.ValueFunctionApproximationType;

public abstract class ValueFunctionApproximationDataService extends SetDataService{
	
	/**
	 * Provides all models that fit to the respective delivery area, time window set, and model type
	 * @param deliveryAreaSetId Respective delivery area set
	 * @param timeWindowSetId Respective time window set
	 * @param modelType  Respective type of the approximation model
	 * @return
	 */
	public abstract ArrayList<SetEntity> getAllSetsByDeliveryAreaSetAndTimeWindowSetId(Integer deliveryAreaSetId, Integer timeWindowSetId, Integer modelTypeId);
	
	/**
	 * Provides all coefficients of the respective model
	 * @param modelId Respective model id
	 * @return
	 */
	public abstract ArrayList<ValueFunctionApproximationCoefficient> getAllCoefficients(int modelId);
	

	/**
	 * Provides a model type
	 * @param modelTypeId Respective model type id
	 * @return
	 */
	public abstract ValueFunctionApproximationType getModelTypeById(int modelTypeId);
	
	
	@Override
	public abstract ArrayList<ValueFunctionApproximationModel> getAllElementsBySetId(int setId);
	
	@Override
	public abstract ValueFunctionApproximationModel getElementById(int entityId);

}
