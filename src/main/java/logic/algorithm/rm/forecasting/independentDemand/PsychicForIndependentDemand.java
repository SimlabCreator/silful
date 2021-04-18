package logic.algorithm.rm.forecasting.independentDemand;

import java.util.ArrayList;

import data.entity.Alternative;
import data.entity.AlternativeSet;
import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.Order;
import data.entity.OrderSet;
import data.entity.ValueBucket;
import data.entity.ValueBucketForecast;
import data.entity.ValueBucketForecastSet;
import data.entity.ValueBucketSet;
import logic.algorithm.rm.forecasting.ValueBucketForecastingAlgorithm;
import logic.service.support.LocationService;
import logic.service.support.ValueBucketService;

/**
 * Independent demand forecast which exactly knows the order requests
 * @author M. Lang
 *
 */
public class PsychicForIndependentDemand implements ValueBucketForecastingAlgorithm {

	private OrderSet historicalOrders;
	private ValueBucketForecastSet demandForecastSet;
	private ValueBucketSet valueBucketSet;
	private DeliveryAreaSet deliveryAreaSet;
	private AlternativeSet alternativeSet;
	private Integer valueBucketNumber;
	private static String[] paras = new String[]{"B"};

	public PsychicForIndependentDemand(OrderSet historicalOrders, ValueBucketSet valueBucketSet, DeliveryAreaSet deliveryAreaSet,
			AlternativeSet alternativeSet, Integer valueBucketNumber) {
		this.historicalOrders = historicalOrders;
		
		this.valueBucketSet = valueBucketSet;
		this.alternativeSet = alternativeSet;
		this.deliveryAreaSet = deliveryAreaSet;
		this.valueBucketNumber = valueBucketNumber;
	}

	public void start() {

		// Determine value bucket set
		//As it is a psychic forecast, use all orders, not only the previously accepted ones
		ArrayList<OrderSet> historicalOrdersList = new ArrayList<OrderSet>();
		historicalOrdersList.add(historicalOrders);
		if (this.valueBucketSet == null) {
			System.out.println("value bucket set is null");
			this.valueBucketSet = ValueBucketService.getValueBucketSet(historicalOrdersList, this.valueBucketNumber, true);
		}

		// Count orders per delivery area, alternative, and value bucket

		/// First: Prepare forecast set
		this.demandForecastSet = new ValueBucketForecastSet();
		this.demandForecastSet.setAlternativeSet(this.alternativeSet);
		this.demandForecastSet.setAlternativeSetId(this.alternativeSet.getId());
		this.demandForecastSet.setDeliveryAreaSet(this.deliveryAreaSet);
		this.demandForecastSet.setDeliveryAreaSetId(this.deliveryAreaSet.getId());
		this.demandForecastSet.setValueBucketSet(this.valueBucketSet);
		this.demandForecastSet.setValueBucketSetId(this.valueBucketSet.getId());
		
		ArrayList<ValueBucketForecast> forecasts = new ArrayList<ValueBucketForecast>();
		for (int daID = 0; daID < this.deliveryAreaSet.getElements().size(); daID++) {
			for (int twID = 0; twID < this.alternativeSet.getElements().size(); twID++) {
				for (int vbID = 0; vbID < this.valueBucketSet.getElements().size(); vbID++) {
					ValueBucketForecast forecast = new ValueBucketForecast();
					DeliveryArea da = this.deliveryAreaSet.getElements().get(daID);
					Alternative alt =  this.alternativeSet.getElements().get(twID);
					ValueBucket vb =  this.valueBucketSet.getElements().get(vbID);
					forecast.setDeliveryArea(da);
					forecast.setDeliveryAreaId(da.getId());
					forecast.setAlternative(alt);
					forecast.setAlternativeId(alt.getId());
					forecast.setValueBucket(vb);
					forecast.setValueBucketId(vb.getId());
					forecast.setDemandNumber(0);

					forecasts.add(forecast);
				}
			}
		}

		/// Second: Go through orders and assign to forecast
		for (int orderID = 0; orderID < this.historicalOrders.getElements()
				.size(); orderID++) {
			Order order =  this.historicalOrders.getElements()
					.get(orderID);

				///Determine dimensions
				DeliveryArea customerArea = LocationService.assignCustomerToDeliveryArea(this.deliveryAreaSet,
						order.getOrderRequest().getCustomer());
				Alternative alternative = order.getSelectedAlternative();
				ValueBucket orderVb = ValueBucketService.assignOrderRequestToValueBucket(valueBucketSet,
						order.getOrderRequest());
				////Assign to forecast and increase counter
				for (int fID = 0; fID < forecasts.size(); fID++) {
					if ((customerArea.getId() == forecasts.get(fID).getDeliveryAreaId())
							&& (alternative.getId() == forecasts.get(fID).getAlternativeId())
							&& (orderVb.getId() == forecasts.get(fID).getValueBucketId())) {
						forecasts.get(fID).setDemandNumber(forecasts.get(fID).getDemandNumber()+1);
					}
				}
		}
		
		int sumSold =0; 
		for(int i=0; i< forecasts.size(); i++){
			
			sumSold+=forecasts.get(i).getDemandNumber();
		}
		System.out.println("Sum sold "+sumSold);
		
	
		
		this.demandForecastSet.setElements(forecasts);
		
		
		
	}
	
	

	public ValueBucketForecastSet getResult() {
		return this.demandForecastSet;
	}


	
	public static String[] getParameterSetting() {
		return paras;
	}


}
