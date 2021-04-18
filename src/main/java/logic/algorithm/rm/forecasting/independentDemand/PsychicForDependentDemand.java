package logic.algorithm.rm.forecasting.independentDemand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.DemandSegmentForecast;
import data.entity.DemandSegmentForecastSet;
import data.entity.DemandSegmentSet;
import data.entity.DemandSegmentWeight;
import data.entity.DemandSegmentWeighting;
import logic.algorithm.rm.forecasting.DemandSegmentForecastingAlgorithm;
import logic.entity.DistanceToRoundForDemandSegmentForecast;
import logic.service.support.ArrivalProcessService;
import logic.service.support.LocationService;
import logic.utility.comparator.DistanceToRoundAscComparator;
import logic.utility.comparator.DistanceToRoundDescComparator;

/**
 * Dependent demand forecast which exactly knows the order requests
 * 
 * @author M. Lang
 *
 */
public class PsychicForDependentDemand implements DemandSegmentForecastingAlgorithm {

	private DemandSegmentWeighting demandSegmentWeighting;
	private int arrivalProcessId;
	private DeliveryAreaSet deliveryAreaSet;
	private DemandSegmentForecastSet demandSegmentForecastSet;
	private HashMap<DeliveryArea, Double> daWeights;
	private HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightings;
	private int bookingHorizonLength;

	public PsychicForDependentDemand(int bookingHorizonLength, int arrivalProcessId,
			DemandSegmentWeighting demandSegmentWeighting, DeliveryAreaSet deliveryAreaSet) {
		this.demandSegmentWeighting = demandSegmentWeighting;
		this.deliveryAreaSet = deliveryAreaSet;
		this.arrivalProcessId = arrivalProcessId;
		this.daWeights = new HashMap<DeliveryArea, Double>();
		this.daSegmentWeightings = new HashMap<DeliveryArea, DemandSegmentWeighting>();
		this.bookingHorizonLength = bookingHorizonLength;
	}

	public void start() {

		// Determine arrival probability weights and segment weights per da
		LocationService.determineDeliveryAreaWeightAndDemandSegmentWeightingPerDeliveryAreaConsideringHierarchy(daWeights,
				daSegmentWeightings, this.deliveryAreaSet, demandSegmentWeighting);

		// Determine arrival probability overall
		double arrivalProbability =  ArrivalProcessService.getMeanArrivalProbability(arrivalProcessId);

		// Prepare forecast set
		this.demandSegmentForecastSet = new DemandSegmentForecastSet();
		this.demandSegmentForecastSet.setDeliveryAreaSet(this.deliveryAreaSet);
		this.demandSegmentForecastSet.setDeliveryAreaSetId(this.deliveryAreaSet.getId());
		DemandSegmentSet dsSet = (DemandSegmentSet) this.demandSegmentWeighting.getSetEntity();
		this.demandSegmentForecastSet.setDemandSegmentSet(dsSet);
		this.demandSegmentForecastSet.setDemandSegmentSetId(dsSet.getId());

		ArrayList<DemandSegmentForecast> forecasts = new ArrayList<DemandSegmentForecast>();

		for (DeliveryArea area : daWeights.keySet()) {
			int overallForecastDA = (int) Math
					.round(arrivalProbability * this.bookingHorizonLength * this.daWeights.get(area));
			int overAllForecasted = 0;
			ArrayList<DistanceToRoundForDemandSegmentForecast> distanceToRoundBorder = new ArrayList<DistanceToRoundForDemandSegmentForecast>();

			for (DemandSegmentWeight dsWeight : this.daSegmentWeightings.get(area).getWeights()) {

				

				DemandSegmentForecast forecast = new DemandSegmentForecast();
				forecast.setDeliveryArea(area);
				forecast.setDeliveryAreaId(area.getId());
				forecast.setDemandSegment(dsWeight.getDemandSegment());
				forecast.setDemandSegmentId(dsWeight.getDemandSegment().getId());

				forecast.setDemandNumber((int) Math.round(overallForecastDA * dsWeight.getWeight()));
				overAllForecasted += Math.round(overallForecastDA * dsWeight.getWeight());
				distanceToRoundBorder.add(new DistanceToRoundForDemandSegmentForecast(forecast,
						Math.round(overallForecastDA * dsWeight.getWeight())
								- overallForecastDA * dsWeight.getWeight()));
				forecasts.add(forecast);
			}

			if (overAllForecasted > overallForecastDA) {
				// First, suffle (not always decrease/increase first segments) and sort
				Collections.shuffle(distanceToRoundBorder);
				Collections.sort(distanceToRoundBorder, new DistanceToRoundDescComparator());
				int currentlyHighest = 0; // The first has the highest distance
											// to the next greater number (from
											// the numbers that were rounded up)
				while (overAllForecasted > overallForecastDA) {
					int relevantIndex = forecasts
							.indexOf(distanceToRoundBorder.get(currentlyHighest).getSegmentForecast());
					forecasts.get(relevantIndex).setDemandNumber(forecasts.get(relevantIndex).getDemandNumber() - 1);
					currentlyHighest++;
					overAllForecasted--;
				}
			} else if (overAllForecasted < overallForecastDA) {
				// If there are to few requests due to rounding, increase number
				// for the alternatives with the highest distance to the full
				// number
				// First, suffle (not always decrease/increase first segments) and sort
				Collections.shuffle(distanceToRoundBorder);
				Collections.sort(distanceToRoundBorder, new DistanceToRoundAscComparator());
				int currentlyHighest = 0; // The first has the highest distance
											// to the next lower number (from
											// the numbers that were rounded
											// down)
				while (overAllForecasted < overallForecastDA) {
					int relevantIndex = forecasts
							.indexOf(distanceToRoundBorder.get(currentlyHighest).getSegmentForecast());
					forecasts.get(relevantIndex).setDemandNumber(forecasts.get(relevantIndex).getDemandNumber() + 1);
					currentlyHighest++;
					overAllForecasted++;

				}

			}
		}

		
		this.demandSegmentForecastSet.setElements(forecasts);

	}

	public DemandSegmentForecastSet getResult() {
		return this.demandSegmentForecastSet;
	}

	public ArrayList<String> getParameterRequest() {

		return null;
	}

	public static String[] getParameterSetting() {
		return null;
	}

}
