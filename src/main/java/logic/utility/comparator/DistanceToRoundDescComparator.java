package logic.utility.comparator;

import java.util.Comparator;

import logic.entity.DistanceToRound;

/**
 * Helps to sort controls according to acending value
 * @author M. Lang
 *
 */
public class DistanceToRoundDescComparator implements Comparator<DistanceToRound> {

	public int compare(DistanceToRound o1, DistanceToRound o2) {
		
		return o1.getDistance()>o2.getDistance() ? -1: o1.getDistance()==o2.getDistance()? 0 :1;
	}
}
