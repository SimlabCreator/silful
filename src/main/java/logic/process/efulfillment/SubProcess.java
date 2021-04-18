package logic.process.efulfillment;

/**
 * Abstract class for all processes that do not run a whole loop and thus can only be conducted for one period
 * 
 * @author M. Lang
 *
 */
public abstract class SubProcess extends CompositionProcess{
	
	public Boolean multiplePeriodsPossible() {
		return false;
	}
}
