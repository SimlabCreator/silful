package logic.utility.exceptions;

public class ParameterUnknownException extends Exception {
    /**
	 * 
	 */
	private static final long serialVersionUID = 9153188317075833094L;
	private static String message = "Parameter unknown ";
	

	public ParameterUnknownException(String additionalMessage)  {
		 super(message+" ("+additionalMessage+").");  
	}               

}
