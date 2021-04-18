package logic.algorithm.rm.forecasting.independentDemand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import data.entity.Alternative;
import data.entity.AlternativeOffer;
import data.entity.AlternativeSet;
import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.DemandSegmentSet;
import data.entity.DemandSegmentWeight;
import data.entity.DemandSegmentWeighting;
import data.entity.ValueBucket;
import data.entity.ValueBucketForecast;
import data.entity.ValueBucketForecastSet;
import data.entity.ValueBucketSet;
import logic.algorithm.rm.forecasting.ValueBucketForecastingAlgorithm;
import logic.entity.DistanceToRoundForValueBucketForecast;
import logic.service.support.ArrivalProcessService;
import logic.service.support.CustomerDemandService;
import logic.service.support.LocationService;
import logic.service.support.ProbabilityDistributionService;
import logic.service.support.ValueBucketService;
import logic.utility.comparator.DistanceToRoundAscComparator;
import logic.utility.comparator.DistanceToRoundDescComparator;
import logic.utility.exceptions.ParameterUnknownException;

/**
 * Independent demand forecast which exactly knows the order requests, but demand is actually dependent
 * @author M. Lang
 *
 */
public class PsychicForIndependentDemandAssumptionWhenDependent implements ValueBucketForecastingAlgorithm {

	private ValueBucketForecastSet demandForecastSet;
	private ValueBucketSet valueBucketSet;
	private DeliveryAreaSet deliveryAreaSet;
	private AlternativeSet alternativeSet;
	private Integer valueBucketNumber;
	private DemandSegmentWeighting demandSegmentWeighting;
	private HashMap<DeliveryArea, Double> daWeights;
	private HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightings;
	private int arrivalProcessId;
	private static String[] paras = new String[]{"B"};
	private int bookingHorizonLength;

	public PsychicForIndependentDemandAssumptionWhenDependent(int bookingHorizonLength, int arrivalProcessId,
			DemandSegmentWeighting demandSegmentWeighting, DeliveryAreaSet deliveryAreaSet, Integer valueBucketNumber, ValueBucketSet valueBucketSet) {
		
		this.valueBucketSet = valueBucketSet;
		this.deliveryAreaSet = deliveryAreaSet;
		this.valueBucketNumber = valueBucketNumber;
		this.demandSegmentWeighting=demandSegmentWeighting;
		this.alternativeSet=((DemandSegmentSet)this.demandSegmentWeighting.getSetEntity()).getAlternativeSet();
		this.daWeights = new HashMap<DeliveryArea, Double>();
		this.daSegmentWeightings = new HashMap<DeliveryArea, DemandSegmentWeighting>();
		this.arrivalProcessId=arrivalProcessId;
		this.bookingHorizonLength=bookingHorizonLength;
	}

	public void start() {

		// Determine value bucket set
		if (this.valueBucketSet == null) {
			System.out.println("value bucket set is null");
			try {
				this.valueBucketSet = ValueBucketService.getValueBucketSetBasedOnBasketValueDistribution(this.valueBucketNumber, (DemandSegmentSet) demandSegmentWeighting.getSetEntity());
			} catch (ParameterUnknownException e) {
				e.printStackTrace();
				System.exit(0);
			}
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

		
		//Determine arrival probability weights and segment weights per da 
		LocationService.determineDeliveryAreaWeightAndDemandSegmentWeightingPerDeliveryAreaConsideringHierarchy(this.daWeights,
				this.daSegmentWeightings, this.deliveryAreaSet, demandSegmentWeighting);

		
		//Determine arrival probability overall
		double arrivalProbability = ArrivalProcessService.getMeanArrivalProbability(this.arrivalProcessId);

		
		//Go through delivery areas and determine DA-specific forecasts
		for (DeliveryArea area:this.daWeights.keySet()) {
			HashMap<ValueBucket, HashMap<Alternative,Double>> forecastsForDA =new HashMap<ValueBucket, HashMap<Alternative,Double>>();
			int forecastForDaOverall=(int) (arrivalProbability*this.bookingHorizonLength*daWeights.get(area));
			double noPurchaseForecast = 0.0f;
			//Go through segments
		//	double forecastDa=0.0f;
			for(DemandSegmentWeight dsWeight:this.daSegmentWeightings.get(area).getWeights()){
				
				
				// Produce pseudo-offer set to determine probabilities per
				// alternative
				ArrayList<Alternative> csSet = this.alternativeSet.getElements();
				ArrayList<AlternativeOffer> offers = new ArrayList<AlternativeOffer>();
				for (int csID = 0; csID < csSet.size(); csID++) {
					AlternativeOffer offer = new AlternativeOffer();
					offer.setAlternative( csSet.get(csID));
					offer.setAlternativeId(offer.getAlternative().getId());
					offers.add(offer);
				}

				HashMap<AlternativeOffer, Double> probabilities = CustomerDemandService.getProbabilitiesForModel(
						((DemandSegmentSet)this.daSegmentWeightings.get(area).getSetEntity()).getDemandModelType().getName(), offers,
						dsWeight.getDemandSegment());

			//	double forecastedForSegment =0.0f;
				//Go through alternatives
				for(AlternativeOffer altO: probabilities.keySet()){
					if(altO!=null){
						//Go through value buckets 
				//		double probValueBucket=0.0f;
						for(ValueBucket valueBucket : this.valueBucketSet.getElements()){
							
							//Determine probability that request has basket value within the respective value bucket
							double probabilityUpperBound;
							try {
								probabilityUpperBound = ProbabilityDistributionService
										.getQuantileByCummulativeDistribution(dsWeight.getDemandSegment().getBasketValueDistribution(), ( valueBucket).getUpperBound());

							double probabilityLowerBound;

								probabilityLowerBound = ProbabilityDistributionService
										.getQuantileByCummulativeDistribution(dsWeight.getDemandSegment().getBasketValueDistribution(), ( valueBucket).getLowerBound());
							
								double valueBucketPart = probabilityUpperBound-probabilityLowerBound;
								//	probValueBucket+=valueBucketPart;
									double currentForecast = forecastForDaOverall*dsWeight.getWeight()*valueBucketPart*probabilities.get(altO);
								//	forecastedForSegment+=currentForecast;
								
									//	System.out.println("Probability for value bucket "+((ValueBucket) valueBucket).getId()+" and demand segment "+dsWeight.getDemandSegment().getId()+ " and alternative "+altO.getAlternative().getId()+" is "+valueBucketPart);
									if(forecastsForDA.containsKey( valueBucket)){

										// Already examined for previous demand segment

										if(forecastsForDA.get( valueBucket).containsKey(altO.getAlternative())){
											//Already examined for this alternative
											//			System.out.println("Alternative is already there "+altO.getAlternative().getId()+" old value is "+forecastsForDA.get((ValueBucket) valueBucket).get(altO.getAlternative())+" and current value is "+currentForecast+"new value is "+(forecastsForDA.get((ValueBucket) valueBucket).get(altO.getAlternative())+currentForecast));
														
											forecastsForDA.get( valueBucket).put(altO.getAlternative(), forecastsForDA.get(valueBucket).get(altO.getAlternative())+currentForecast);
											//System.out.println("and it is also in the hashmap "+forecastsForDA.get((ValueBucket) valueBucket).get(altO.getAlternative()));
										}else{
											//Alternative was not in consideration set of previous demand segments
											forecastsForDA.get(valueBucket).put(altO.getAlternative(), currentForecast);
										}
										
									}else{
										HashMap<Alternative, Double> alternativeValue = new HashMap<Alternative, Double>();
										alternativeValue.put(altO.getAlternative(), currentForecast);
										forecastsForDA.put( valueBucket, alternativeValue);
									}
							
							} catch (ParameterUnknownException e) {
								e.printStackTrace();
								System.exit(0);
							}

						}
						
						//System.out.println("Overall value bucket probability "+probValueBucket);
					}else{
						noPurchaseForecast+=forecastForDaOverall*dsWeight.getWeight()*probabilities.get(altO);
					}
	
				
				
			}	
			//	System.out.println("Forecasted for segment "+dsWeight.getDemandSegment().getId()+" for da "+area.getId()+" value "+forecastedForSegment);
				//forecastDa+=forecastedForSegment;
			}
		//	System.out.println("Forecasted for da "+area.getId()+" value "+forecastDa);
		
			
			int forecastedOverall=0;
			int noPurchaseRounded = (int) noPurchaseForecast;
			ArrayList<DistanceToRoundForValueBucketForecast> distanceToRoundBorder = new ArrayList<DistanceToRoundForValueBucketForecast>();
			for(ValueBucket bucket: forecastsForDA.keySet()){
				for(Alternative alt: forecastsForDA.get(bucket).keySet()){
					ValueBucketForecast forecast = new ValueBucketForecast();
					forecast.setDeliveryArea(area);
					forecast.setDeliveryAreaId(area.getId());
					forecast.setAlternative(alt);
					forecast.setAlternativeId(alt.getId());
					forecast.setValueBucket(bucket);
					forecast.setValueBucketId(bucket.getId());
					forecast.setDemandNumber((int) Math.round(forecastsForDA.get(bucket).get(alt)));
					forecastedOverall+=Math.round(forecastsForDA.get(bucket).get(alt));
					distanceToRoundBorder.add(new DistanceToRoundForValueBucketForecast(forecast,
							( Math.round(forecastsForDA.get(bucket).get(alt))
									- forecastsForDA.get(bucket).get(alt))));
					
					forecasts.add(forecast);
				}
			}
			
			if (forecastedOverall > forecastForDaOverall-noPurchaseRounded) {
				// First, sort
				Collections.sort(distanceToRoundBorder, new DistanceToRoundDescComparator());
				int currentlyHighest = 0; // The first has the highest distance
											// to the next greater number (from
											// the numbers that were rounded up)
				while (forecastedOverall > forecastForDaOverall-noPurchaseRounded) {
					int relevantIndex = forecasts
							.indexOf(distanceToRoundBorder.get(currentlyHighest).getValueBucketForecast());
					forecasts.get(relevantIndex).setDemandNumber(forecasts.get(relevantIndex).getDemandNumber() - 1);
					currentlyHighest++;
					forecastedOverall--;

				}
			} else if (forecastedOverall < forecastForDaOverall-noPurchaseRounded) {
				// If there are too few requests due to rounding, increase number
				// for the alternatives with the highest distance to the full
				// number
				// First, sort
				Collections.sort(distanceToRoundBorder, new DistanceToRoundAscComparator());
				int currentlyHighest = 0; // The first has the highest distance
											// to the next lower number (from
											// the numbers that were rounded
											// down)
				while (forecastedOverall < forecastForDaOverall-noPurchaseRounded) {
					int relevantIndex = forecasts
							.indexOf(distanceToRoundBorder.get(currentlyHighest).getValueBucketForecast());
					forecasts.get(relevantIndex).setDemandNumber(forecasts.get(relevantIndex).getDemandNumber() + 1);
					currentlyHighest++;
					forecastedOverall++;

				}

			}
			
			
			
				
			}
			
	
		
		
	
		this.demandForecastSet.setElements(forecasts);

		
		}
		
		
		
	
	

	public ValueBucketForecastSet getResult() {
		return this.demandForecastSet;
	}


	
	public static String[] getParameterSetting() {
		return paras;
	}


}
