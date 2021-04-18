package logic.service.support;

import java.util.ArrayList;

import data.entity.Control;
import data.entity.DemandSegmentSet;
import data.entity.Order;
import data.entity.OrderRequest;
import data.entity.OrderSet;
import data.entity.ValueBucket;
import data.entity.ValueBucketForecast;
import data.entity.ValueBucketSet;
import data.utility.DataServiceProvider;
import logic.entity.SoldUnits;
import logic.utility.NameProvider;
import logic.utility.comparator.ValueBucketControlAscComparator;
import logic.utility.comparator.ValueBucketForecastDescComparator;
import logic.utility.comparator.ValueBucketSoldUnitsAscComparator;
import logic.utility.exceptions.ParameterUnknownException;

/**
 * Provides functionality relating to forecasting step
 * 
 * @author M. Lang
 *
 */
public class ValueBucketService {

	/**
	 * Determine value bucket set as in basic paper (Cleophas&Ehmke 2014)
	 * @param historicalOrders Orders of earlier periods
	 * @param bucketNumber Required number of buckets
	 * @param considerNotAcceptedOrders If not accepted orders (->no orders) should be considered for the maximum value determination (makes sense for psychic forecast) 
	 * @param periodNumber Number of the current period
	 * @return
	 */
	public static ValueBucketSet getValueBucketSet(ArrayList<OrderSet> historicalOrders, Integer bucketNumber, Boolean considerNotAcceptedOrders){
		
		Double maximumValue= 0.0;
		
		for(int setId=0; setId < historicalOrders.size(); setId++){
			ArrayList<Order> orders = historicalOrders.get(setId).getElements();
			for(int orderId=0; orderId < orders.size(); orderId++){
				Order order = orders.get(orderId);
				if(order.getAccepted() ||considerNotAcceptedOrders){
					if(maximumValue<order.getOrderRequest().getBasketValue()) maximumValue = order.getOrderRequest().getBasketValue();
				}
				
			}
			
		}
		
		ArrayList<ValueBucket> valueBuckets = new ArrayList<ValueBucket>();
		for(int bucketId=1; bucketId<=bucketNumber; bucketId++){
			ValueBucket valueBucket = new ValueBucket();
			valueBucket.setLowerBound((maximumValue/bucketNumber)*(bucketId-1));
			valueBucket.setUpperBound((maximumValue/bucketNumber)*(bucketId));
			valueBuckets.add(valueBucket);
		}
		
		ValueBucketSet valueBucketSet = new ValueBucketSet();
		valueBucketSet.setElements(valueBuckets);
		NameProvider.setNameValueBucketSet(valueBucketSet);
		
		DataServiceProvider.getValueBucketDataServiceImplInstance().persistCompleteEntitySet(valueBucketSet);
		
		return valueBucketSet;
	}
	
	/**
	 * Determine value bucket set for dependent demand psychic
	 * @param bucketNumber Required number of buckets
	 * @param demandSegmentWeighting 
	 * @return
	 * @throws ParameterUnknownException 
	 */
	public static ValueBucketSet getValueBucketSetBasedOnBasketValueDistribution( Integer bucketNumber, DemandSegmentSet demandSegmentSet) throws ParameterUnknownException{
		
		Double maximumValue=AcceptanceService.determineMaximumRevenueValueForNormalisation(demandSegmentSet);
		
		ArrayList<ValueBucket> valueBuckets = new ArrayList<ValueBucket>();
		for(int bucketId=1; bucketId<=bucketNumber; bucketId++){
			ValueBucket valueBucket = new ValueBucket();
			valueBucket.setLowerBound((maximumValue/bucketNumber)*(bucketId-1));
			valueBucket.setUpperBound((maximumValue/bucketNumber)*(bucketId));
			valueBuckets.add(valueBucket);
		}
		
		ValueBucketSet valueBucketSet = new ValueBucketSet();
		valueBucketSet.setElements(valueBuckets);
		NameProvider.setNameValueBucketSet(valueBucketSet);
		
		DataServiceProvider.getValueBucketDataServiceImplInstance().persistCompleteEntitySet(valueBucketSet);
		
		return valueBucketSet;
	}
	
	/**
	 * Assigns order request to value bucket according to basket value
	 * @param valueBucketSet
	 * @param orderRequest
	 * @return
	 */
	public static ValueBucket assignOrderRequestToValueBucket(ValueBucketSet valueBucketSet, OrderRequest orderRequest){
		
		ArrayList<ValueBucket> valueBuckets = valueBucketSet.getElements();
		for(int i=0; i < (valueBuckets.size()-1); i++){
			ValueBucket valueBucket = valueBuckets.get(i);
			if((orderRequest.getBasketValue()>valueBucket.getLowerBound()) && (orderRequest.getBasketValue()<=valueBucket.getUpperBound())){
				return valueBucket;
			}
		}
		return valueBuckets.get(valueBuckets.size()-1);
	}
	
	/**
	 * Provides all controls fitting to the respective delivery area and alternative Id. Sorted ascending by lower bound of value bucket.
	 * @param deliveryAreaId
	 * @param alternativeId
	 * @param controls
	 * @return
	 */
	public static ArrayList<Control> getControlsForDeliveryAreaAndAlternativeSortedByValueBucketAscending(Integer deliveryAreaId, Integer alternativeId, ArrayList<Control> controls){
		
		ArrayList<Control> relevantControls = new ArrayList<Control>();
		for(int i= 0; i < controls.size(); i++){
			Control control = controls.get(i);
			if((control.getAlternativeId()==alternativeId) && (control.getDeliveryAreaId()==deliveryAreaId)){
				relevantControls.add(control);
			}
		}
		
		relevantControls.sort(new ValueBucketControlAscComparator());
		
		return relevantControls;
		
	}
	
	/**
	 * Provides all soldUnits fitting to the respective delivery area and alternative Id. Sorted ascending by lower bound of value bucket.
	 * @param deliveryAreaId
	 * @param alternativeId
	 * @param soldUnits
	 * @return
	 */
	public static ArrayList<SoldUnits> getSoldUnitsForDeliveryAreaAndAlternativeSortedByValueBucketAscending(Integer deliveryAreaId, Integer alternativeId, ArrayList<SoldUnits> soldUnits){
		
		ArrayList<SoldUnits> relevantunits = new ArrayList<SoldUnits>();
		for(int i= 0; i < soldUnits.size(); i++){
			if((soldUnits.get(i).getAlternativeId()==alternativeId) && (soldUnits.get(i).getDeliveryAreaId()==deliveryAreaId)){
				relevantunits.add(soldUnits.get(i));
			}
		}
		relevantunits.sort(new ValueBucketSoldUnitsAscComparator());
		
		return relevantunits;
		
	}
	
	/**
	 * Provides all forecasts fitting to the respective delivery area and alternative Id. Sorted descending by lower bound of value bucket.
	 * @param deliveryAreaId
	 * @param alternativeId
	 * @param controls
	 * @return
	 */
	public static ArrayList<ValueBucketForecast> getForecastsForDeliveryAreaAndAlternativeSortedByValueBucketDescending(Integer deliveryAreaId, Integer alternativeId, ArrayList<ValueBucketForecast> forecasts){
		
		ArrayList<ValueBucketForecast> relevantDemandForecasts = new ArrayList<ValueBucketForecast>();
		for(int i= 0; i < forecasts.size(); i++){
			ValueBucketForecast demandForecast = forecasts.get(i);
			if((demandForecast.getAlternativeId()==alternativeId) && (demandForecast.getDeliveryAreaId()==deliveryAreaId)){
				relevantDemandForecasts.add(demandForecast);
			}
		}
		
		relevantDemandForecasts.sort(new ValueBucketForecastDescComparator());
		
		return relevantDemandForecasts;
		
	}
}
