package logic.algorithm.rm.optimization.control;

import java.util.ArrayList;
import java.util.Collections;

import data.entity.Alternative;
import data.entity.AlternativeSet;
import data.entity.CapacitySet;
import data.entity.Control;
import data.entity.ControlSet;
import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.ValueBucket;
import data.entity.ValueBucketForecast;
import data.entity.ValueBucketForecastSet;
import logic.entity.AlternativeCapacity;
import logic.service.support.CapacityService;
import logic.service.support.ValueBucketService;
import logic.utility.comparator.ValueBucketDescComparator;

/**
 * Sets controls equal to capacities. No availability control except of capacities.
 * @author M. Lang
 *
 */
public class DeterministicEMSRb implements ControlAlgorithm{
	
	private CapacitySet capacitySet;
	private ValueBucketForecastSet demandForecastSet;
	private AlternativeSet alternativeSet;
	private DeliveryAreaSet deliveryAreaSet;
	private ArrayList<ValueBucket> valueBucketsDesc;
	private ControlSet controlSet;
	
	public DeterministicEMSRb(CapacitySet capacitySet, ValueBucketForecastSet demandForecastSet){
		this.capacitySet=capacitySet;
		this.demandForecastSet = demandForecastSet;
	}

	public void start() {
		
		ArrayList<Control> controls = new ArrayList<Control>();

		//Determine capacities per alternative
		this.alternativeSet=demandForecastSet.getAlternativeSet();
		ArrayList<AlternativeCapacity> altCaps = CapacityService.getCapacitiesPerAlternative(this.capacitySet, this.alternativeSet);
		
		//Produce a new control per alternative, delivery area, and value bucket
		
		///Prepare delivery area set and value buckets
		
		this.deliveryAreaSet=this.demandForecastSet.getDeliveryAreaSet();
		ArrayList<ValueBucket> valueBucketsDesc = this.demandForecastSet.getValueBucketSet().getElements();

		Collections.sort(this.valueBucketsDesc, new ValueBucketDescComparator());
		
		///Iterate over alternatives and delivery areas and determine controls for all value buckets
		for(int altID=0; altID<this.alternativeSet.getElements().size(); altID++){
			for(int daID=0; daID< this.deliveryAreaSet.getElements().size(); daID++){
				
				Alternative alt = this.alternativeSet.getElements().get(altID);
				DeliveryArea area = this.deliveryAreaSet.getElements().get(daID);
				AlternativeCapacity cap = new AlternativeCapacity();
				for(int capID=0; capID<altCaps.size(); capID++){
					if(alt.getId()==altCaps.get(capID).getAlternativeId() && area.getId()==altCaps.get(capID).getDeliveryAreaId()){
						cap = altCaps.get(capID);
						break;
					}
				}
				ArrayList<ValueBucketForecast> relevantForecastsDesc = ValueBucketService.getForecastsForDeliveryAreaAndAlternativeSortedByValueBucketDescending(area.getId(), alt.getId(), this.demandForecastSet.getElements());
								
				ArrayList<Control> controlsForDaAlt = new ArrayList<Control>();
								
				////Produce control for highest value bucket by setting it to capacity limit
				Control control = new Control();
				control.setAlternativeId(alt.getId());
				control.setAlternative(alt);
				control.setDeliveryArea(area);
				control.setDeliveryAreaId(area.getId());
				control.setValueBucket(this.valueBucketsDesc.get(0));
				control.setValueBucketId(this.valueBucketsDesc.get(0).getId());
				control.setControlNumber(cap.getCapacityNumber());
				controlsForDaAlt.add(control);
				
				////For lower value buckets, reduce control of upper bucket by the forecasted value for the upper bucket
				for(int vbID=1; vbID < this.valueBucketsDesc.size(); vbID++){
					Control controlFollower = new Control();
					controlFollower.setAlternativeId(alt.getId());
					controlFollower.setAlternative(alt);
					controlFollower.setDeliveryArea(area);
					controlFollower.setDeliveryAreaId(area.getId());
					controlFollower.setValueBucket(this.valueBucketsDesc.get(vbID));
					controlFollower.setValueBucketId(this.valueBucketsDesc.get(vbID).getId());
				//	System.out.println("Number of controls"+controlsForDaAlt.size());
					int controlValue = (controlsForDaAlt.get(vbID-1).getControlNumber()-relevantForecastsDesc.get(vbID-1).getDemandNumber())>0 ? (controlsForDaAlt.get(vbID-1).getControlNumber()-relevantForecastsDesc.get(vbID-1).getDemandNumber()) : 0;				
					controlFollower.setControlNumber(controlValue);
					controlsForDaAlt.add(controlFollower);
				}
				
				controls.addAll(controlsForDaAlt);
			}
		}
	
		
		this.controlSet = new ControlSet();
		controlSet.setDeliveryAreaSetId(capacitySet.getDeliveryAreaSetId());
		controlSet.setElements(controls);
		controlSet.setAlternativeSetId(capacitySet.getTimeWindowSetId());
		controlSet.setAlternativeCapacities(altCaps);
		controlSet.setValueBucketSet(this.demandForecastSet.getValueBucketSet());
		controlSet.setValueBucketSetId(this.demandForecastSet.getValueBucketSetId());
		
	}

	
	public ControlSet getResult() {
		return controlSet;
	}


	
}
