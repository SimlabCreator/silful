package logic.utility.comparator;

import java.util.Comparator;

import org.apache.commons.math3.util.Pair;

import data.entity.Entity;

/**
 * Helps to sort alternatives regarding their start time
 * @author M. Lang
 *
 */
public class PairDoubleValueDescComparator implements Comparator<Pair<? extends Entity,Double>> {
	   
    public int compare(Pair<? extends Entity,Double> a, Pair<? extends Entity,Double> b) {
    	
    	
        return a.getValue() > b.getValue() ? -1 : a.getValue().equals(b.getValue()) ? 0 : 1;
    }
}
