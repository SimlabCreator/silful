package logic.utility.exceptions;

public class DivideByZeroException extends Exception{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1732400286180856843L;
	private static String message = "Try to divide by zero";
	
	public DivideByZeroException(String additionalMessage)  {
		 super(message+" ("+additionalMessage+")");  
	}               

	
}
