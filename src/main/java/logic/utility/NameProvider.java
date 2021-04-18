package logic.utility;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import data.entity.CapacitySet;
import data.entity.ControlSet;
import data.entity.CustomerSet;
import data.entity.DeliveryAreaSet;
import data.entity.DemandSegmentForecastSet;
import data.entity.DynamicProgrammingTree;
import data.entity.OrderRequestSet;
import data.entity.OrderSet;
import data.entity.Routing;
import data.entity.ValueBucketForecastSet;
import data.entity.ValueBucketSet;
import data.entity.ValueFunctionApproximationModelSet;

/**
 * Provides a process object of the process type requested.
 * Process id has to fit to id in the database
 * @author M. Lang
 *
 */
public class NameProvider {
	
	private static DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

	public static void setNameDeliveryAreaSet(DeliveryAreaSet set) {
		set.setName("E: "+SettingsProvider.getExperiment().getName()+" Date: "+dateFormat.format(new Date()));
	}
	
	public static void setNameDynamicProgrammingTree(DynamicProgrammingTree tree) {
		tree.setName("E: "+SettingsProvider.getExperiment().getName()+" Date: "+dateFormat.format(new Date()));
	}
	
	public static void setNameCustomerSet(CustomerSet set) {
		set.setName("E: "+SettingsProvider.getExperiment().getName()+" Date: "+dateFormat.format(new Date()));
	}
	
	public static void setNameOrderRequestSet(OrderRequestSet set) {
		set.setName("E: "+SettingsProvider.getExperiment().getName()+" Date: "+dateFormat.format(new Date()));
	}
	
	public static void setNameValueBucketForecastSet(ValueBucketForecastSet set){
		set.setName("E: "+SettingsProvider.getExperiment().getName()+" Date: "+dateFormat.format(new Date()));
	}
	
	public static void setNameDemandSegmentForecastSet(DemandSegmentForecastSet set){
		set.setName("E: "+SettingsProvider.getExperiment().getName()+" Date: "+dateFormat.format(new Date()));
	}
	
	public static void setNameControlSet(ControlSet set){
		set.setName("E: "+SettingsProvider.getExperiment().getName()+" Date: "+dateFormat.format(new Date()));
	}
	
	public static void setNameCapacitySet(CapacitySet set){
		set.setName("E: "+SettingsProvider.getExperiment().getName()+" Date: "+dateFormat.format(new Date()));
	}
	
	public static void setNameOrderSet(OrderSet set){
		set.setName("E: "+SettingsProvider.getExperiment().getName()+" Date: "+dateFormat.format(new Date()));
	}
	
	public static void setNameValueFunctionApproximationSet(ValueFunctionApproximationModelSet set){
		set.setName("E: "+SettingsProvider.getExperiment().getName()+" Date: "+dateFormat.format(new Date()));
	}
	
	public static void setNameRouting(Routing routing){
		routing.setName("E: "+SettingsProvider.getExperiment().getName()+" Date: "+dateFormat.format(new Date()));
	}
	
	public static void setNameValueBucketSet(ValueBucketSet valueBucketSet){
		valueBucketSet.setName("E: "+SettingsProvider.getExperiment().getName()+" Date: "+dateFormat.format(new Date()));
	}
}
