package logic.service.support;

import java.util.Comparator;

import logic.entity.InsertionCriterion;

public class ScoreComparator implements Comparator<InsertionCriterion> {

	public int compare (InsertionCriterion i1, InsertionCriterion i2){
		return i1.getCriterion().compareTo(i2.getCriterion());
	}
	
}



