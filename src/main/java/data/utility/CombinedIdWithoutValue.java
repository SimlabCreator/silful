package data.utility;

/**
 * Cache for saving relation with two primary Ids and no value
 * Needed to persist all associations for different elements at once
 * For instance, needed to save all available alternatives for all orders that need to be saved
 * @author M. Lang
 *
 */
public class CombinedIdWithoutValue {
	
	Integer[] Ids;
	
	public CombinedIdWithoutValue(Integer[] Ids){
		this.Ids=Ids;
		
	}
	
	public Integer[] getIds(){
		return Ids;
	}

}
