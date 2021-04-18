package logic.utility.exceptions;

public class ProbabilitiesDoNotSumUpToOneException extends Exception{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1732400286180856843L;
	private static String message = "Probabilities do not sum up to 1!";
	
	public ProbabilitiesDoNotSumUpToOneException()  {
		 super(message);  
	}               

	
}
