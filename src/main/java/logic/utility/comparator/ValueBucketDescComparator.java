package logic.utility.comparator;

import java.util.Comparator;

import data.entity.ValueBucket;

/**
 * Helps to sort controls according to acending value
 * @author M. Lang
 *
 */
public class ValueBucketDescComparator implements Comparator<ValueBucket> {

	public int compare(ValueBucket o1, ValueBucket o2) {
		
		return o1.getLowerBound()>o2.getLowerBound() ? -1: o1.getLowerBound()==o2.getLowerBound()? 0 :1;
	}
}
