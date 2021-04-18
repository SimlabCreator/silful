package logic.algorithm.rm.optimization.control;

import java.util.ArrayList;

import data.entity.AlternativeSet;
import data.entity.CapacitySet;
import data.entity.Control;
import data.entity.ControlSet;
import logic.entity.AlternativeCapacity;
import logic.service.support.CapacityService;

/**
 * Sets controls equal to capacities. No availability control except of capacities.
 * @author M. Lang
 *
 */
public class NoFurtherControls implements ControlAlgorithm{
	
	private CapacitySet capacitySet;
	private AlternativeSet alternativeSet;
	private ControlSet controlSet;
	
	public NoFurtherControls(CapacitySet capacitySet, AlternativeSet alternativeSet){
		this.capacitySet=capacitySet;
		this.alternativeSet = alternativeSet;
	}

	public void start() {
		
		ArrayList<Control> controls = new ArrayList<Control>();

		//Determine capacities per alternative
		ArrayList<AlternativeCapacity> altCaps = CapacityService.getCapacitiesPerAlternative(this.capacitySet, this.alternativeSet);
		//Produce a new control per alternative and delivery area (value bucket stays null because no further controls)
		for(int capIndex = 0; capIndex < altCaps.size(); capIndex++){
			AlternativeCapacity cap = altCaps.get(capIndex);
			Control control = new Control();
			control.setAlternative(cap.getAlternative());
			control.setAlternativeId(cap.getAlternativeId());
			control.setDeliveryArea(cap.getDeliveryArea());
			control.setDeliveryAreaId(cap.getDeliveryAreaId());
			control.setControlNumber(cap.getCapacityNumber());
			
			//Value bucket stays null because no restrictions
			
			controls.add(control);
			
		}
	
		
		this.controlSet = new ControlSet();
		controlSet.setDeliveryAreaSetId(capacitySet.getDeliveryAreaSetId());
		controlSet.setElements(controls);
		controlSet.setAlternativeSetId(capacitySet.getTimeWindowSetId());
		controlSet.setAlternativeCapacities(altCaps);
		//Leave value bucket set null because no limits

		
	}


	public ControlSet getResult() {
		return controlSet;
	}

	public ArrayList<String> getParameterRequest() {
		
		return new ArrayList<String>();
	}

	
}
