package logic.utility.comparator;

import java.util.Comparator;

import data.entity.Control;

/**
 * Helps to sort controls according to ascending value
 * @author M. Lang
 *
 */
public class ValueBucketControlAscComparator implements Comparator<Control> {
	   
	public int compare(Control o1, Control o2) {
		
		return o1.getValueBucket().getLowerBound()<o2.getValueBucket().getLowerBound() ? -1: o1.getValueBucket().getLowerBound()==o2.getValueBucket().getLowerBound()? 0 :1;
	}
}
