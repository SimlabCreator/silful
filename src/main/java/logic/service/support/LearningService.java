package logic.service.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

import data.entity.Alternative;
import data.entity.AlternativeOffer;
import data.entity.TimeWindow;
import logic.utility.SubsetProducer;

public class LearningService {

	public static void chooseOfferSetBasedOnEGreedyStrategy(Set<TimeWindow> timeWindows, double greedyValue,
			double highestGreedyValue, int currentIteration, int numberOfIterations, double lowerBoundGreedyIncrease, double upperBoundGreedyIncrease,
			ArrayList<AlternativeOffer> selectedOfferedAlternatives,
			HashMap<TimeWindow, Alternative> alternativesToTimeWindows) {

		// E-greedy search:also allows non-value-add offers

		Double[] probabilities = new Double[2];

		/// All but best offer set get same fraction of
		/// non-greedy
		/// probability;
		double currentGreedyValue = greedyValue;
		// If we are later in the training phase, there should
		// be
		// more greedy
		if (currentIteration > lowerBoundGreedyIncrease * numberOfIterations) {
			if (currentIteration > upperBoundGreedyIncrease* numberOfIterations) {
				currentGreedyValue = highestGreedyValue;
			} else {
				currentGreedyValue = greedyValue + (currentIteration - lowerBoundGreedyIncrease * numberOfIterations)
						/ (upperBoundGreedyIncrease * numberOfIterations-lowerBoundGreedyIncrease * numberOfIterations) * (highestGreedyValue - greedyValue);
			}
		}
	
		probabilities[0] = currentGreedyValue;

		
			probabilities[1] = (1.0 - currentGreedyValue) ;
		

		int selectedGroup = 0;

		try {
			selectedGroup = ProbabilityDistributionService.getRandomGroupIndexByProbabilityArray(probabilities);
		} catch (Exception e) {

			e.printStackTrace();
		}

		/// If the selected offer set is not the best offer set
		if (selectedGroup != 0) {
			Random r = new Random();
			//Go through all time windows and randomly decide if they are in the subset
			ArrayList<TimeWindow> selectedSet = new ArrayList<TimeWindow>();
			for(TimeWindow tw: timeWindows) {
				if(r.nextDouble()>=0.5) {
					selectedSet.add(tw);
				}
			}
			
		

			///	selectedSet.addAll(SubsetProducer.powerSet(timeWindows));
	
			/// Choose the selected subset. If it is equal to
			/// the best set or - in case the best set is the
			/// empty
			/// set-
			/// the empty set, than choose the last
			

			/// Equal?
//			boolean equal = (selectedSet.size() == selectedOfferedAlternatives.size());
//			if (equal == true) {
//				for (AlternativeOffer ao : selectedOfferedAlternatives) {
//					if (!selectedSet.contains(ao.getAlternative().getTimeWindows().get(0).getId())) {
//						equal = false;
//						break;
//					}
//				}
//			}
//			if (equal == true)
//				selectedSet = possibleSets.get(possibleSets.size() - 1);
			// Determine selection probabilities for the
			// alternatives of
			// the
			// respective subset
			selectedOfferedAlternatives.clear();
			for (TimeWindow tw : selectedSet) {
				AlternativeOffer offer = new AlternativeOffer();
				offer.setAlternative(alternativesToTimeWindows.get(tw));
				offer.setAlternativeId(alternativesToTimeWindows.get(tw).getId());
				selectedOfferedAlternatives.add(offer);
			}
		}
	}

}
