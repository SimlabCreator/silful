package logic.utility.comparator;

import java.util.Comparator;

import org.apache.commons.math3.util.Pair;

/**
 * Helps to sort alternatives regarding their start time
 * @author M. Lang
 *
 */
public class ObjectValuePairDoubleValueDescComparator implements Comparator<Pair<? extends Object,Double>> {
	   
    public int compare(Pair<? extends Object,Double> a, Pair<? extends Object,Double> b) {
    	
    	
        return a.getValue() > b.getValue() ? -1 : a.getValue().equals(b.getValue()) ? 0 : 1;
    }
}
