package logic.algorithm.rm.forecasting.independentDemand;

import java.util.ArrayList;

import data.entity.Alternative;
import data.entity.AlternativeSet;
import data.entity.Control;
import data.entity.ControlSet;
import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.Order;
import data.entity.OrderSet;
import data.entity.ValueBucket;
import data.entity.ValueBucketForecast;
import data.entity.ValueBucketForecastSet;
import data.entity.ValueBucketSet;
import logic.algorithm.rm.forecasting.ValueBucketForecastingAlgorithm;
import logic.entity.SoldUnits;
import logic.service.support.LocationService;
import logic.service.support.ValueBucketService;

/**
 * Exponential smoothing as proposed in Cleophas & Ehmke 2014
 * @author M. Lang
 *
 */
public class ExponentialSmoothing implements ValueBucketForecastingAlgorithm {

	private ArrayList<OrderSet> historicalOrders;
	private ValueBucketForecastSet historicalDemandForecastSet;
	private ValueBucketForecastSet demandForecastSet;
	private ValueBucketSet valueBucketSet;
	private DeliveryAreaSet deliveryAreaSet;
	private AlternativeSet alternativeSet;
	private Integer valueBucketNumber;
	private Double alpha;
	private static String[] paras = new String[]{"alpha", "B"};

	public ExponentialSmoothing(ArrayList<OrderSet> historicalOrders,
			ValueBucketForecastSet historicalDemandForecastSet, ValueBucketSet valueBucketSet, DeliveryAreaSet deliveryAreaSet,
			AlternativeSet alternativeSet, Integer valueBucketNumber, Double alpha) {
		this.historicalOrders = historicalOrders;
		this.historicalDemandForecastSet = historicalDemandForecastSet;
		this.valueBucketSet = valueBucketSet;
		this.alternativeSet = alternativeSet;
		this.deliveryAreaSet = deliveryAreaSet;
		this.valueBucketNumber = valueBucketNumber;
		this.alpha = alpha;
	}

	public void start() {

		// Determine value bucket set
		if (this.valueBucketSet == null) {
			this.valueBucketSet = ValueBucketService.getValueBucketSet(this.historicalOrders, this.valueBucketNumber, false);
		}

		// From most current historical set, determine accepted orders per
		// delivery area, alternative, and value bucket

		/// First: Prepare buffer for sold units per da/tw/vb
		ArrayList<SoldUnits> groupedOrders = new ArrayList<SoldUnits>();
		for (int daID = 0; daID < this.deliveryAreaSet.getElements().size(); daID++) {
			for (int twID = 0; twID < this.alternativeSet.getElements().size(); twID++) {
				for (int vbID = 0; vbID < this.valueBucketSet.getElements().size(); vbID++) {
					SoldUnits soldUnits = new SoldUnits();
					DeliveryArea da = this.deliveryAreaSet.getElements().get(daID);
					Alternative alt = this.alternativeSet.getElements().get(twID);
					ValueBucket vb = this.valueBucketSet.getElements().get(vbID);
					soldUnits.setDeliveryArea(da);
					soldUnits.setDeliveryAreaId(da.getId());
					soldUnits.setAlternative(alt);
					soldUnits.setAlternativeId(alt.getId());
					soldUnits.setValueBucket(vb);
					soldUnits.setValueBucketId(vb.getId());
					soldUnits.setSoldNumber(0);

					groupedOrders.add(soldUnits);
				}
			}
		}

		/// Second: Go through orders and assign to buffer
		for (int orderID = 0; orderID < this.historicalOrders.get(this.historicalOrders.size() - 1).getElements()
				.size(); orderID++) {
			Order order = this.historicalOrders.get(this.historicalOrders.size() - 1).getElements()
					.get(orderID);

			/// Only count if order was accepted
			if (order.getAccepted()) {
				DeliveryArea customerArea = LocationService.assignCustomerToDeliveryArea(this.deliveryAreaSet,
						order.getOrderRequest().getCustomer());
				Alternative alternative = order.getSelectedAlternative();
				ValueBucket orderVb = ValueBucketService.assignOrderRequestToValueBucket(valueBucketSet,
						order.getOrderRequest());
				////Assign to buffer
				for (int suID = 0; suID < groupedOrders.size(); suID++) {
					if ((customerArea.getId() == groupedOrders.get(suID).getDeliveryAreaId())
							&& (alternative.getId() == groupedOrders.get(suID).getAlternativeId())
							&& (orderVb.getId() == groupedOrders.get(suID).getValueBucketId())) {
						groupedOrders.get(suID).setSoldNumber(groupedOrders.get(suID).getSoldNumber() + 1);
					}
				}
			}
		}
		
		int sumSold =0; 
		for(int i=0; i< groupedOrders.size(); i++){
			
			sumSold+=groupedOrders.get(i).getSoldNumber();
		}
		System.out.println("Sum sold "+sumSold);
		//Estimate demand by unconstraining if controls were applied
		
		ControlSet controlSet = this.historicalOrders.get(this.historicalOrders.size()-1).getControlSet();
		if(controlSet.getElements().get(0).getValueBucketId()==null ||controlSet.getElements().get(0).getValueBucketId()==0){
			for(int i=0; i<groupedOrders.size(); i++){
				groupedOrders.get(i).setEstimatedNumber(groupedOrders.get(i).getSoldNumber());
			}
		}else{
		
		for(int daID=0; daID < this.deliveryAreaSet.getElements().size(); daID++){
			for(int altID=0; altID < this.alternativeSet.getElements().size(); altID++){
				DeliveryArea da =  this.deliveryAreaSet.getElements().get(daID);
				Alternative alt = this.alternativeSet.getElements().get(altID);
				ArrayList<Control> relevantControls = ValueBucketService.getControlsForDeliveryAreaAndAlternativeSortedByValueBucketAscending(da.getId(), alt.getId(), controlSet.getElements());
				ArrayList<SoldUnits> relevantSoldUnits = ValueBucketService.getSoldUnitsForDeliveryAreaAndAlternativeSortedByValueBucketAscending(da.getId(), alt.getId(), groupedOrders);
				int sumSoldUnits = relevantSoldUnits.get(0).getSoldNumber();
				System.out.println("relevant controls"+relevantControls.size());
				for(int conID=0; conID<relevantControls.size(); conID++){
					if(relevantControls.get(conID).getControlNumber()>sumSoldUnits){
						relevantSoldUnits.get(conID).setEstimatedNumber(relevantSoldUnits.get(conID).getSoldNumber());
					}else{
						int estimatedNumber = this.calculateAverageSoldUnitsOfPreviousPeriods(relevantSoldUnits.get(conID), this.historicalOrders);
						relevantSoldUnits.get(conID).setEstimatedNumber(estimatedNumber);
					}
					
					sumSoldUnits+=relevantSoldUnits.get(conID).getSoldNumber();
				}
				
			}
		}
		
		}
		//Exponential smoothing
		
		ArrayList<ValueBucketForecast> forecasts = new ArrayList<ValueBucketForecast>();
		
		for(int suID=0; suID <groupedOrders.size(); suID++){
			
			//New forecast
			ValueBucketForecast forecast = new ValueBucketForecast();
			forecast.setAlternativeId(groupedOrders.get(suID).getAlternativeId());
			forecast.setAlternative(groupedOrders.get(suID).getAlternative());
			forecast.setValueBucket(groupedOrders.get(suID).getValueBucket());
			forecast.setValueBucketId(groupedOrders.get(suID).getValueBucketId());
			forecast.setDeliveryArea(groupedOrders.get(suID).getDeliveryArea());
			forecast.setDeliveryAreaId(groupedOrders.get(suID).getDeliveryAreaId());
			
			if(this.historicalDemandForecastSet!=null){
				for(int i=0; i< this.historicalDemandForecastSet.getElements().size(); i++){
					
					ValueBucketForecast hDF =  this.historicalDemandForecastSet.getElements().get(i);
					if((hDF.getAlternativeId()==forecast.getAlternativeId()) && (hDF.getDeliveryAreaId()==forecast.getDeliveryAreaId()) && (hDF.getValueBucketId()==hDF.getValueBucketId())){
						forecast.setDemandNumber((int) Math.round((1-alpha)*hDF.getDemandNumber()+alpha*groupedOrders.get(suID).getEstimatedNumber()));
						break;
					}
				}
			}else{//If no historical forecast available, just use value itself
				forecast.setDemandNumber(groupedOrders.get(suID).getEstimatedNumber());
			}
			forecasts.add(forecast);
		}
		
		this.demandForecastSet = new ValueBucketForecastSet();
		this.demandForecastSet.setElements(forecasts);
		this.demandForecastSet.setAlternativeSet(this.alternativeSet);
		this.demandForecastSet.setAlternativeSetId(this.alternativeSet.getId());
		this.demandForecastSet.setDeliveryAreaSet(this.deliveryAreaSet);
		this.demandForecastSet.setDeliveryAreaSetId(this.deliveryAreaSet.getId());
		this.demandForecastSet.setValueBucketSet(this.valueBucketSet);
		this.demandForecastSet.setValueBucketSetId(this.valueBucketSet.getId());
		
	}
	
	private Integer calculateAverageSoldUnitsOfPreviousPeriods(SoldUnits soldUnitCurrent, ArrayList<OrderSet> historicalOrders){
		
		int sumOfSoldUnits=soldUnitCurrent.getSoldNumber();
		
		for(int i=0; i < historicalOrders.size()-1; i++){
			for (int orderID = 0; orderID < this.historicalOrders.get(i).getElements()
					.size(); orderID++) {
				Order order = this.historicalOrders.get(i).getElements()
						.get(orderID);

				/// Only count if order was accepted
				if (order.getAccepted()) {
					
					//And if it fits to da and alternative and bucket
					
					Alternative alternative = order.getSelectedAlternative();
					if(alternative.getId()==soldUnitCurrent.getAlternativeId()){
						DeliveryArea customerArea = LocationService.assignCustomerToDeliveryArea(this.deliveryAreaSet,
								order.getOrderRequest().getCustomer());
						
						if(customerArea.getId()==soldUnitCurrent.getDeliveryAreaId()){
							ValueBucket orderVb = ValueBucketService.assignOrderRequestToValueBucket(valueBucketSet,
									order.getOrderRequest());
							if(orderVb.getId()==soldUnitCurrent.getId()){
								sumOfSoldUnits++;
							}
						}
					}
					
				}	
					
			}
		}
		
		return Math.round(sumOfSoldUnits/historicalOrders.size());
	}

	public ValueBucketForecastSet getResult() {
		return this.demandForecastSet;
	}

	public static String[] getParameterSetting() {
		return paras;
	}





}
