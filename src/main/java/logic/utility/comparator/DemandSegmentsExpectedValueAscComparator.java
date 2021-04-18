package logic.utility.comparator;

import java.util.Comparator;
import java.util.HashMap;

import data.entity.DemandSegment;
import data.entity.Entity;
import logic.service.support.CustomerDemandService;

/**
 * Helps to sort alternatives regarding their start time
 * @author M. Lang
 *
 */
public class DemandSegmentsExpectedValueAscComparator implements Comparator<DemandSegment> {
	   private HashMap<Entity, Object> objectiveSpecificValues;
	   private double maximumRevenueValue;
    public DemandSegmentsExpectedValueAscComparator(double maximumRevenueValue, HashMap<Entity, Object> objectiveSpecificValues){
    	this.objectiveSpecificValues=objectiveSpecificValues;
    	this.maximumRevenueValue=maximumRevenueValue;
    }
	
	public int compare(DemandSegment a, DemandSegment b) {
    	
    
		double valueA = CustomerDemandService.calculateExpectedValue(maximumRevenueValue, objectiveSpecificValues, a);
		double valueB = CustomerDemandService.calculateExpectedValue(maximumRevenueValue, objectiveSpecificValues, b);

        return valueA<valueB? -1 : valueA == valueB ? 0 : 1;
    }
}
