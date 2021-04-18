package logic.utility.comparator;

import java.util.Comparator;

import data.entity.ValueBucketForecast;

/**
 * Helps to sort forecasts according to descending value
 * @author M. Lang
 *
 */
public class ValueBucketForecastDescComparator implements Comparator<ValueBucketForecast> {
	   
	public int compare(ValueBucketForecast o1, ValueBucketForecast o2) {
		
		return o1.getValueBucket().getLowerBound()>o2.getValueBucket().getLowerBound() ? -1: o1.getValueBucket().getLowerBound()==o2.getValueBucket().getLowerBound()? 0 :1;
	}
}
