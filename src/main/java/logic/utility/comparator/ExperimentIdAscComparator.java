package logic.utility.comparator;

import java.util.Comparator;

import data.entity.Experiment;

/**
 * Helps to sort order requests according to decreasing arrival time
 * @author M. Lang
 *
 */
public class ExperimentIdAscComparator implements Comparator<Experiment> {
	   
    public int compare(Experiment a, Experiment b) {
        return a.getId() < b.getId() ? -1 : a.getId() == b.getId() ? 0 : 1;
    }
}
