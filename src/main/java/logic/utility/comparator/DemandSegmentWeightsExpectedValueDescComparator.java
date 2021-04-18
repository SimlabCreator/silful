package logic.utility.comparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import data.entity.DemandSegmentWeight;
import data.entity.Entity;
import data.entity.TimeWindow;
import logic.service.support.CustomerDemandService;

/**
 * Helps to sort alternatives regarding their start time
 * @author M. Lang
 *
 */
public class DemandSegmentWeightsExpectedValueDescComparator implements Comparator<DemandSegmentWeight> {
	   private HashMap<Entity, Object> objectiveSpecificValues;
	   private double maximumRevenueValue;
    public DemandSegmentWeightsExpectedValueDescComparator(double maximumRevenueValue, HashMap<Entity, Object> objectiveSpecificValues){
    	this.objectiveSpecificValues=objectiveSpecificValues;
    	this.maximumRevenueValue=maximumRevenueValue;
    }
	
	public int compare(DemandSegmentWeight a, DemandSegmentWeight b) {
    	
    
		double valueA = CustomerDemandService.calculateExpectedValue(maximumRevenueValue, objectiveSpecificValues, a.getDemandSegment());
		double valueB = CustomerDemandService.calculateExpectedValue(maximumRevenueValue, objectiveSpecificValues, b.getDemandSegment());
    	
        return valueA>valueB? -1 : valueA == valueB ? 0 : 1;
    }
}
