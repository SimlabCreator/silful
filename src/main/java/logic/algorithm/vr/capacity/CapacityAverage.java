package logic.algorithm.vr.capacity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.apache.commons.math3.util.Pair;

import data.entity.Capacity;
import data.entity.CapacitySet;
import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.TimeWindow;
import data.entity.TimeWindowSet;
import logic.utility.comparator.PairDoubleValueAscComparator;
import logic.utility.comparator.PairDoubleValueDescComparator;

/**
 * Obtains average capacities over multiple capacity sets (capacity sets have to
 * relate to same delivery area set and time window set
 * 
 * @author M. Lang
 *
 */
public class CapacityAverage implements CapacityAlgorithm {

	private ArrayList<CapacitySet> capacitySets;
	private DeliveryAreaSet daSet;
	private TimeWindowSet twSet;
	private CapacitySet result;
	private HashMap<Integer, HashMap<Integer, Integer>> sumsPerDeliveryAreaAndTimeWindow;

	public CapacityAverage(ArrayList<CapacitySet> capacitySets) {
		this.capacitySets = capacitySets;
	}

	public void start() {
		
		this.result=new CapacitySet();
		this.result.setDeliveryAreaSet(this.capacitySets.get(0).getDeliveryAreaSet());
		this.result.setDeliveryAreaSetId(this.capacitySets.get(0).getDeliveryAreaSet().getId());
		this.result.setTimeWindowSet(this.capacitySets.get(0).getTimeWindowSet());
		this.result.setTimeWindowSetId(this.capacitySets.get(0).getTimeWindowSet().getId());
		
		this.daSet=this.capacitySets.get(0).getDeliveryAreaSet();
		this.twSet=this.capacitySets.get(0).getTimeWindowSet();
				
		//Initialise hashmap with delivery areas and time windows
		this.sumsPerDeliveryAreaAndTimeWindow = new HashMap<Integer, HashMap<Integer, Integer>>();
		for(DeliveryArea area: this.daSet.getElements()){
			
			if(!this.daSet.isHierarchy()){
			HashMap<Integer,Integer> timeWindow=new HashMap<Integer, Integer>();
			for(TimeWindow window: this.twSet.getElements()){
				timeWindow.put(window.getId(), 0);
				
			}
			this.sumsPerDeliveryAreaAndTimeWindow.put(area.getId(), timeWindow);
			}else{
				for(DeliveryArea subArea: area.getSubset().getElements()){
					
					HashMap<Integer,Integer> timeWindow=new HashMap<Integer, Integer>();
					for(TimeWindow window: this.twSet.getElements()){
						timeWindow.put(window.getId(), 0);
						
					}
					this.sumsPerDeliveryAreaAndTimeWindow.put(subArea.getId(), timeWindow);
				}
			}
		}
		
		//Calculate sums over all capacity sets
		int overallSum=0;
		for(CapacitySet set: this.capacitySets){
			int overallCap=0;
			for(Capacity element: set.getElements()){
				int oldSum=this.sumsPerDeliveryAreaAndTimeWindow.get(element.getDeliveryAreaId()).get(element.getTimeWindowId());
				int newSum = oldSum+element.getCapacityNumber();
				this.sumsPerDeliveryAreaAndTimeWindow.get(element.getDeliveryAreaId()).put(element.getTimeWindowId(), newSum);
				overallSum+=element.getCapacityNumber();
				overallCap+=element.getCapacityNumber();
			}
			System.out.println("Overallcap:"+overallCap);
		}
		int overallAverageSumS=overallSum/this.capacitySets.size();
			
		
		//Create new capacity set with averages
		ArrayList<Capacity> capacities = new ArrayList<Capacity>();
		ArrayList<Pair<Capacity, Double>> roundingDifferences = new ArrayList<Pair<Capacity, Double>>(); 
		int overallAverageSumI = 0;
		for(Integer daId:this.sumsPerDeliveryAreaAndTimeWindow.keySet()){
			for(Integer twId:this.sumsPerDeliveryAreaAndTimeWindow.get(daId).keySet()){
				Capacity cap= new Capacity();
				cap.setTimeWindowId(twId);
				cap.setDeliveryAreaId(daId);
				double value = this.sumsPerDeliveryAreaAndTimeWindow.get(daId).get(twId)/(double) this.capacitySets.size();	
				int intValue = (int) Math.round(value);
				overallAverageSumI+=intValue;
				double digits = value-intValue;
				//If it was rounded up, the lower the value, the higher the negative digits -> sort ascending and take first
				// If it was rounded down, the higher the value, the higher the positive digits -> sort descending and take first
				roundingDifferences.add(new Pair<Capacity, Double>(cap, digits));
				cap.setCapacityNumber(intValue);
				capacities.add(cap);
			}
		}
		
		

		
		
		int tempOverallAverageSumI= overallAverageSumI;
		Collections.sort(roundingDifferences, new PairDoubleValueAscComparator());
		while(tempOverallAverageSumI>overallAverageSumS){
			roundingDifferences.get(0).getKey().setCapacityNumber(roundingDifferences.get(0).getKey().getCapacityNumber()-1);
			roundingDifferences.remove(0);
			tempOverallAverageSumI--;
		}
		
		Collections.sort(roundingDifferences, new PairDoubleValueDescComparator());
		while(tempOverallAverageSumI<overallAverageSumS){
			roundingDifferences.get(0).getKey().setCapacityNumber(roundingDifferences.get(0).getKey().getCapacityNumber()+1);
			roundingDifferences.remove(0);
			tempOverallAverageSumI++;
		}
		
		int sum3=0;
		for(Capacity cap: capacities){
			sum3+=cap.getCapacityNumber();
		}
		System.out.println("Sum3 "+sum3);
		
		this.result.setElements(capacities);
	}

	public CapacitySet getResult() {
		return result;
	}

	public ArrayList<String> getParameterRequest() {

		return new ArrayList<String>();
	}

}
