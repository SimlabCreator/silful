package logic.utility.comparator;

import java.util.Comparator;

import logic.entity.SoldUnits;

/**
 * Helps to sort controls according to acending value
 * @author M. Lang
 *
 */
public class ValueBucketSoldUnitsAscComparator implements Comparator<SoldUnits> {

	public int compare(SoldUnits o1, SoldUnits o2) {
		
		return o1.getValueBucket().getLowerBound()<o2.getValueBucket().getLowerBound() ? -1: o1.getValueBucket().getLowerBound()==o2.getValueBucket().getLowerBound()? 0 :1;
	}
}
