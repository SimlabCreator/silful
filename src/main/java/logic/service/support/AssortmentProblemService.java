package logic.service.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

import data.entity.Alternative;
import data.entity.AlternativeOffer;
import data.entity.Entity;
import data.entity.OrderRequest;
import data.entity.TimeWindow;
import logic.entity.AssortmentAlgorithm;
import logic.utility.SubsetProducer;
import logic.utility.comparator.PairDoubleValueDescComparator;

public class AssortmentProblemService {

	
	/**
	 * Determines the best offer set for
	 * 
	 * @param twsWithValueAdd
	 * @param segment
	 * @return
	 */
	public static Pair<ArrayList<AlternativeOffer>, Double> determineBestOfferSet(OrderRequest request,
			HashMap<TimeWindow, Double> opportunityCostsPerTw, 
			double maximumRevenueValue, HashMap<Entity, Object> objectiveSpecificValues, AssortmentAlgorithm algo,
			HashMap<TimeWindow, Alternative> alternativesToTimeWindows, boolean possiblyLargeOfferSet,
			boolean actualValue, double relaxingFactor) {

		double precisionMultiplier = 10000000d;
		// Determine time windows with value add
		HashMap<TimeWindow, Double> twsWithValueAdd = new HashMap<TimeWindow, Double>();
		double orderValue;
		if (actualValue) {
			orderValue = CustomerDemandService.calculateOrderValue(request, maximumRevenueValue,
					objectiveSpecificValues);
		} else {
			orderValue = CustomerDemandService.calculateExpectedValue(maximumRevenueValue, objectiveSpecificValues,
					request.getCustomer().getOriginalDemandSegment());
		}
		
		for (TimeWindow tw : opportunityCostsPerTw.keySet()) {	

			double opportunityCosts = opportunityCostsPerTw.get(tw);
			
			if ((opportunityCosts - orderValue < 0) || (Math.abs(
					(double) Math.round((opportunityCosts - orderValue)
							* precisionMultiplier) / precisionMultiplier)) < 1.0 / (precisionMultiplier / 10.0))
				twsWithValueAdd.put(tw,
						(double) Math
								.round((orderValue - opportunityCosts)
										* precisionMultiplier)
								/ precisionMultiplier);
		}

		Pair<ArrayList<AlternativeOffer>, Double> result = null;
		switch (algo) {
		case REVENUE_ORDERED_SET:
			result = AssortmentProblemService.determineBestRevenueOrderedOfferSet(request, twsWithValueAdd,
					alternativesToTimeWindows, possiblyLargeOfferSet, relaxingFactor);
			break;
		case ENUMERATION:
			result = AssortmentProblemService.determineBestOfferSetEnumeration(request, twsWithValueAdd,
					alternativesToTimeWindows, possiblyLargeOfferSet);
			break;
		case ALL_VALUE_ADD:
			result = AssortmentProblemService.determineOfferSetOfAllValueAdd(request, twsWithValueAdd,
					alternativesToTimeWindows);
		default:
			break;
		}

		return result;
	}
	
	/**
	 * Determines the best offer set for
	 * 
	 * @param twsWithValueAdd
	 * @param segment
	 * @return
	 */
	public static Pair<ArrayList<AlternativeOffer>, Double> determineBestOfferSet(OrderRequest request,
			HashMap<TimeWindow, Double> futureValueLessCapacities, double futureValueSameCapacities,
			double maximumRevenueValue, HashMap<Entity, Object> objectiveSpecificValues, AssortmentAlgorithm algo,
			HashMap<TimeWindow, Alternative> alternativesToTimeWindows, boolean possiblyLargeOfferSet,
			boolean actualValue, boolean considerFallbackConservative, HashMap<TimeWindow, Double> fallbackValuesConservative, HashMap<TimeWindow, Double> currentMultiplier, double discountingFactor) {

		double precisionMultiplier = 10000000d;
		// Determine time windows with value add
		HashMap<TimeWindow, Double> twsWithValueAdd = new HashMap<TimeWindow, Double>();
		double orderValue;
		if (actualValue) {
			orderValue = CustomerDemandService.calculateOrderValue(request, maximumRevenueValue,
					objectiveSpecificValues);
		} else {
			orderValue = CustomerDemandService.calculateExpectedValue(maximumRevenueValue, objectiveSpecificValues,
					request.getCustomer().getOriginalDemandSegment());
		}
		
		for (TimeWindow tw : futureValueLessCapacities.keySet()) {	

			double opportunityCosts = futureValueSameCapacities - futureValueLessCapacities.get(tw);
			if(considerFallbackConservative){
				if(opportunityCosts>fallbackValuesConservative.get(tw)){
					opportunityCosts=(fallbackValuesConservative.get(tw)+opportunityCosts)/2.0;
				}
			}
			if(currentMultiplier!=null){
				
					opportunityCosts=opportunityCosts*currentMultiplier.get(tw);
				
				
			}
			opportunityCosts=discountingFactor*opportunityCosts;
			if ((opportunityCosts - orderValue < 0) || (Math.abs(
					(double) Math.round((opportunityCosts - orderValue)
							* precisionMultiplier) / precisionMultiplier)) < 1.0 / (precisionMultiplier / 10.0))
				twsWithValueAdd.put(tw,
						(double) Math
								.round((orderValue - opportunityCosts)
										* precisionMultiplier)
								/ precisionMultiplier);
		}

		Pair<ArrayList<AlternativeOffer>, Double> result = null;
		switch (algo) {
		case REVENUE_ORDERED_SET:
			result = AssortmentProblemService.determineBestRevenueOrderedOfferSet(request, twsWithValueAdd,
					alternativesToTimeWindows, possiblyLargeOfferSet, 1.0);
			break;
		case ENUMERATION:
			result = AssortmentProblemService.determineBestOfferSetEnumeration(request, twsWithValueAdd,
					alternativesToTimeWindows, possiblyLargeOfferSet);
			break;
		case ALL_VALUE_ADD:
			result = AssortmentProblemService.determineOfferSetOfAllValueAdd(request, twsWithValueAdd,
					alternativesToTimeWindows);
		default:
			break;
		}

		return result;
	}

	public static Pair<ArrayList<AlternativeOffer>, Double> determineOfferSetOfAllValueAdd(OrderRequest request,
			HashMap<TimeWindow, Double> twsWithValueAdd, HashMap<TimeWindow, Alternative> alternativesToTimeWindows) {

		ArrayList<AlternativeOffer> offeredAlternatives = new ArrayList<AlternativeOffer>();
		for (TimeWindow tw : twsWithValueAdd.keySet()) {
			AlternativeOffer offer = new AlternativeOffer();
			offer.setAlternative(alternativesToTimeWindows.get(tw));
			offer.setAlternativeId(alternativesToTimeWindows.get(tw).getId());
			offeredAlternatives.add(offer);
		}

		HashMap<AlternativeOffer, Double> probs = CustomerDemandService.getProbabilitiesForModel(
				request.getCustomer().getOriginalDemandSegment().getSet().getDemandModelType().getName(),
				offeredAlternatives, request.getCustomer().getOriginalDemandSegment());

		// Determine purchase-value
		Double currentValue = 0.0;
		Iterator<AlternativeOffer> it = probs.keySet().iterator();
		while (it.hasNext()) {
			AlternativeOffer currentAlt = it.next();
			if (currentAlt != null) {

				Double prob = probs.get(currentAlt);

				Double probValue = prob * twsWithValueAdd.get(currentAlt.getAlternative().getTimeWindows().get(0)); // Value
																													// now

				currentValue += probValue;
			}

		}

		return new Pair<ArrayList<AlternativeOffer>, Double>(offeredAlternatives, currentValue);
	}

	public static Pair<ArrayList<AlternativeOffer>, Double> determineBestRevenueOrderedOfferSet(OrderRequest request,
			HashMap<TimeWindow, Double> twsWithValueAdd, HashMap<TimeWindow, Alternative> alternativesToTimeWindows,
			boolean possiblyLargeOfferSet, double relaxingFactor) {

		double maxValue = 0.0;
		ArrayList<AlternativeOffer> bestOfferedAlternatives = new ArrayList<AlternativeOffer>();

		ArrayList<Pair<TimeWindow, Double>> twsWithValueAddAsPair = new ArrayList<Pair<TimeWindow, Double>>();
		for (TimeWindow tw : twsWithValueAdd.keySet()) {
			twsWithValueAddAsPair.add(new Pair<TimeWindow, Double>(tw, twsWithValueAdd.get(tw)));
		}
		Collections.sort(twsWithValueAddAsPair, new PairDoubleValueDescComparator());

		// Go through revenue-ordered sets and determine best
		for (int i = 0; i < twsWithValueAddAsPair.size(); i++) {

			// Determine selection probabilities for the alternatives of
			// the respective subset
			ArrayList<AlternativeOffer> offeredAlternatives = new ArrayList<AlternativeOffer>();
			for (int twIndex = 0; twIndex <= i; twIndex++) {
				AlternativeOffer offer = new AlternativeOffer();
				offer.setAlternative(alternativesToTimeWindows.get(twsWithValueAddAsPair.get(twIndex).getKey()));
				offer.setAlternativeId(
						alternativesToTimeWindows.get(twsWithValueAddAsPair.get(twIndex).getKey()).getId());
				offeredAlternatives.add(offer);
			}

			HashMap<AlternativeOffer, Double> probs = CustomerDemandService.getProbabilitiesForModel(
					request.getCustomer().getOriginalDemandSegment().getSet().getDemandModelType().getName(),
					offeredAlternatives, request.getCustomer().getOriginalDemandSegment());

			// Determine purchase-value
			Double currentValue = 0.0;
			Iterator<AlternativeOffer> it = probs.keySet().iterator();
			while (it.hasNext()) {
				AlternativeOffer currentAlt = it.next();
				if (currentAlt != null) {

					Double prob = probs.get(currentAlt);

					Double probValue = prob * twsWithValueAdd.get(currentAlt.getAlternative().getTimeWindows().get(0)); // Value
																														// now

					currentValue += probValue;
				}

			}

			// Update maximum value
			if (currentValue > maxValue || (currentValue == maxValue && possiblyLargeOfferSet) || (currentValue >= maxValue*relaxingFactor)) {
				if(currentValue > maxValue){
					maxValue = currentValue;
				}else{
					System.out.println("Offer more because of insecurity");
				}
					
				bestOfferedAlternatives = offeredAlternatives;
			}
		}

		return new Pair<ArrayList<AlternativeOffer>, Double>(bestOfferedAlternatives, maxValue);
	}

	public static Pair<ArrayList<AlternativeOffer>, Double> determineBestOfferSetEnumeration(OrderRequest request,
			HashMap<TimeWindow, Double> twsWithValueAdd, HashMap<TimeWindow, Alternative> alternativesToTimeWindows,
			boolean possiblyLargeOfferSet) {

		double maxValue = 0.0;
		ArrayList<AlternativeOffer> bestOfferedAlternatives = new ArrayList<AlternativeOffer>();

		// Determine best subset

		for (Set<TimeWindow> s : SubsetProducer.powerSet(twsWithValueAdd.keySet())) {
			if (s.size() > 0) { // Only look at non-empty subsets

				// Determine selection probabilities for the alternatives of
				// the
				// respective subset
				ArrayList<AlternativeOffer> offeredAlternatives = new ArrayList<AlternativeOffer>();
				for (TimeWindow index : s) {
					AlternativeOffer offer = new AlternativeOffer();
					offer.setAlternative(alternativesToTimeWindows.get(index));
					offer.setAlternativeId(alternativesToTimeWindows.get(index).getId());
					offeredAlternatives.add(offer);
				}

				HashMap<AlternativeOffer, Double> probs = CustomerDemandService.getProbabilitiesForModel(
						request.getCustomer().getOriginalDemandSegment().getSet().getDemandModelType().getName(),
						offeredAlternatives, request.getCustomer().getOriginalDemandSegment());

				// Determine purchase-value
				Double currentValue = 0.0;
				Iterator<AlternativeOffer> it = probs.keySet().iterator();
				while (it.hasNext()) {
					AlternativeOffer currentAlt = it.next();
					if (currentAlt != null) {

						Double prob = probs.get(currentAlt);

						Double probValue = prob
								* twsWithValueAdd.get(currentAlt.getAlternative().getTimeWindows().get(0)); // Value
																											// now

						currentValue += probValue;
					}

				}

				// Update maximum value
				if (currentValue > maxValue || (currentValue == maxValue && possiblyLargeOfferSet
						&& offeredAlternatives.size() > bestOfferedAlternatives.size())) {
					maxValue = currentValue;
					bestOfferedAlternatives = offeredAlternatives;
				}
			}

		}

		return new Pair<ArrayList<AlternativeOffer>, Double>(bestOfferedAlternatives, maxValue);
	}
}
