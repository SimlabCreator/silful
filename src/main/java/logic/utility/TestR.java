package logic.utility;

import java.util.ArrayList;

import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

public class TestR {

	    public static void main(String a[]) throws RserveException, REXPMismatchException {

	        RConnection connection = null;
	        
	         try {
	             /* Create a connection to Rserve instance running
	              * on default port 6311
	              */
	             connection = new RConnection();
	 
	             String vector = "c(1,2,3,4)";
	             ArrayList<Double> test = new ArrayList<Double>();
	             test.add(1.0);
	             test.add(2.0);
	             test.add(3.0);
	             String listString = "c(";
	             for (int i=0; i < test.size()-1; i++)
	             {
	                 listString +=  test.get(i)+ ",";
	             }
	             listString +=  test.get(test.size()-1)+ ")";
	             
	             System.out.println(listString);
	             connection.eval("data1="+listString);
	             connection.eval("data2="+listString);
	             connection.eval("data3="+listString);
	             connection.eval("dataTest = cbind(data1, data2, data3)");
	             connection.eval("library(plm)");
	           
	             connection.eval("result = plm(data3~data1+data2, data = dataTest, model = \"within\")");
	            // System.out.println("The mean of given vector is=" + mean);
	         } catch (RserveException e) {
	             e.printStackTrace();
	         }finally{
	             connection.close();
	         }
	    }

}
