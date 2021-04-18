package logic.service.support;

import java.util.ArrayList;
import java.util.HashMap;

import data.entity.DeliveryArea;
import data.entity.DemandSegmentForecast;
import data.entity.DemandSegmentForecastSet;

/**
 * Provides functionality relating to forecasting step
 * 
 * @author M. Lang
 *
 */
public class ForecastingService {

	/**
	 * Groups demand segment forecasts by delivery area
	 * @param demandSegmentForecastSet Respective demand segment forecast set
	 * @return HashMap with forecasts per delivery area
	 */
	public static HashMap<DeliveryArea, ArrayList<DemandSegmentForecast>> groupDemandSegmentForecastsByDeliveryArea(DemandSegmentForecastSet demandSegmentForecastSet){
		HashMap<DeliveryArea, ArrayList<DemandSegmentForecast>> forecastsPerDa = new HashMap<DeliveryArea, ArrayList<DemandSegmentForecast>>();
		

		for(DemandSegmentForecast forecast:demandSegmentForecastSet.getElements()){
			
		
			if(forecastsPerDa.containsKey(forecast.getDeliveryArea())){
				forecastsPerDa.get(forecast.getDeliveryArea()).add(forecast);
			}else{
				ArrayList<DemandSegmentForecast> daForecasts= new ArrayList<DemandSegmentForecast>();
				daForecasts.add(forecast);
				forecastsPerDa.put(forecast.getDeliveryArea(), daForecasts);
			}
		}
		
		return forecastsPerDa;
	}
	
}
