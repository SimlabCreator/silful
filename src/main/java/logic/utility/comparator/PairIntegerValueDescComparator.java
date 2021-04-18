package logic.utility.comparator;

import java.util.Comparator;

import org.apache.commons.math3.util.Pair;

import data.entity.Entity;

/**
 * Helps to sort alternatives regarding their start time
 * @author M. Lang
 *
 */
public class PairIntegerValueDescComparator implements Comparator<Pair<? extends Entity,Integer>> {
	   
    public int compare(Pair<? extends Entity,Integer> a, Pair<? extends Entity,Integer> b) {
    	
    	
        return a.getValue() > b.getValue() ? -1 : a.getValue().equals(b.getValue()) ? 0 : 1;
    }
}
