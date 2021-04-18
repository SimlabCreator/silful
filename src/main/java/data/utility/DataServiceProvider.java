package data.utility;

import org.springframework.jdbc.core.JdbcTemplate;

import data.service.AlternativeDataService;
import data.service.AlternativeDataServiceImpl;
import data.service.ArrivalProcessDataService;
import data.service.ArrivalProcessDataServiceImpl;
import data.service.CapacityDataService;
import data.service.CapacityDataServiceImpl;
import data.service.ControlDataService;
import data.service.ControlDataServiceImpl;
import data.service.CustomerDataService;
import data.service.CustomerDataServiceImpl;
import data.service.DeliveryAreaDataService;
import data.service.DeliveryAreaDataServiceImpl;
import data.service.DeliveryDataService;
import data.service.DeliveryDataServiceImpl;
import data.service.DemandModelTypeDataService;
import data.service.DemandModelTypeDataServiceImpl;
import data.service.DemandSegmentDataService;
import data.service.DemandSegmentDataServiceImpl;
import data.service.DemandSegmentForecastDataService;
import data.service.DemandSegmentForecastDataServiceImpl;
import data.service.DepotDataService;
import data.service.DepotDataServiceImpl;
import data.service.ExpectedDeliveryTimeConsumptionDataService;
import data.service.ExpectedDeliveryTimeConsumptionDataServiceImpl;
import data.service.IncentiveTypeDataService;
import data.service.IncentiveTypeDataServiceImpl;
import data.service.KpiTypeDataService;
import data.service.KpiTypeDataServiceImpl;
import data.service.ObjectiveTypeDataService;
import data.service.ObjectiveTypeDataServiceImpl;
import data.service.OrderContentTypeDataService;
import data.service.OrderContentTypeDataServiceImpl;
import data.service.OrderDataService;
import data.service.OrderDataServiceImpl;
import data.service.OrderRequestDataService;
import data.service.OrderRequestDataServiceImpl;
import data.service.ParameterTypeDataService;
import data.service.ParameterTypeDataServiceImpl;
import data.service.ProbabilityDistributionDataService;
import data.service.ProbabilityDistributionDataServiceImpl;
import data.service.ProbabilityDistributionTypeDataService;
import data.service.ProbabilityDistributionTypeDataServiceImpl;
import data.service.ProcessTypeDataService;
import data.service.ProcessTypeDataServiceImpl;
import data.service.RegionDataService;
import data.service.RegionDataServiceImpl;
import data.service.ResidenceAreaDataService;
import data.service.ResidenceAreaDataServiceImpl;
import data.service.RoutingDataService;
import data.service.RoutingDataServiceImpl;
import data.service.ServiceTimeSegmentDataService;
import data.service.ServiceTimeSegmentDataServiceImpl;
import data.service.SettingsDataService;
import data.service.SettingsDataServiceImpl;
import data.service.TimeWindowDataService;
import data.service.TimeWindowDataServiceImpl;
import data.service.TravelTimeDataService;
import data.service.TravelTimeDataServiceImpl;
import data.service.ValueBucketDataService;
import data.service.ValueBucketDataServiceImpl;
import data.service.ValueBucketForecastDataService;
import data.service.ValueBucketForecastDataServiceImpl;
import data.service.ValueFunctionApproximationDataService;
import data.service.ValueFunctionApproximationDataServiceImpl;
import data.service.VariableTypeDataService;
import data.service.VariableTypeDataServiceImpl;
import data.service.VehicleAssignmentDataService;
import data.service.VehicleAssignmentDataServiceImpl;
import data.service.VehicleTypeDataService;
import data.service.VehicleTypeDataServiceImpl;

/**
 * Provides instances of the different data services
 * @author M. Lang
 *
 */
public class DataServiceProvider {

	private static DeliveryAreaDataServiceImpl deliveryAreaDataServiceImpl;
	private static ResidenceAreaDataServiceImpl residenceAreaDataServiceImpl;
	private static IncentiveTypeDataServiceImpl incentiveTypeDataServiceImpl;
	private static ProcessTypeDataServiceImpl processTypeDataServiceImpl;
	private static RegionDataServiceImpl regionDataServiceImpl;
	private static SettingsDataServiceImpl settingsDataServiceImpl;
	private static ParameterTypeDataServiceImpl parameterTypeDataServiceImpl;
	private static ProbabilityDistributionTypeDataServiceImpl probabilityDistributionTypeDataServiceImpl;
	private static ProbabilityDistributionDataServiceImpl probabilityDistributionDataServiceImpl;
	private static KpiTypeDataServiceImpl kpiTypeDataServiceImpl;
	private static DemandModelTypeDataServiceImpl demandModelTypeDataServiceImpl;
	private static CustomerDataServiceImpl customerDataServiceImpl;
	private static OrderContentTypeDataServiceImpl orderContentTypeDataServiceImpl;
	private static OrderRequestDataServiceImpl orderRequestDataServiceImpl;
	private static OrderDataServiceImpl orderDataServiceImpl;
	private static TimeWindowDataServiceImpl timeWindowDataServiceImpl;
	private static AlternativeDataServiceImpl alternativeDataServiceImpl;
	private static VehicleTypeDataServiceImpl vehicleTypeDataServiceImpl;
	private static RoutingDataServiceImpl routingDataServiceImpl;
	private static CapacityDataServiceImpl capacityDataServiceImpl;
	private static ValueBucketForecastDataServiceImpl valueBucketForecastDataServiceImpl;
	private static DemandSegmentForecastDataServiceImpl demandSegmentForecastDataServiceImpl;
	private static ControlDataServiceImpl controlDataServiceImpl;
	private static DeliveryDataServiceImpl deliveryDataServiceImpl;
	private static TravelTimeDataServiceImpl travelTimeDataServiceImpl; 
	private static ServiceTimeSegmentDataServiceImpl serviceTimeSegmentDataServiceImpl; 
	private static ArrivalProcessDataServiceImpl arrivalProcessDataServiceImpl;
	private static DemandSegmentDataServiceImpl demandSegmentDataServiceImpl;
	private static VariableTypeDataServiceImpl variableTypeDataServiceImpl;
	private static ValueBucketDataServiceImpl valueBucketDataServiceImpl;
	private static DepotDataServiceImpl depotDataServiceImpl;
	private static ObjectiveTypeDataServiceImpl  objectiveTypeDataServiceImpl;
	private static VehicleAssignmentDataServiceImpl vehicleAssignmentDataServiceImpl;
	private static ExpectedDeliveryTimeConsumptionDataServiceImpl expectedDeliveryTimeConsumptionDataServiceImpl;
	private static ValueFunctionApproximationDataServiceImpl valueFunctionApproximationDataServiceImpl;

	public static DeliveryAreaDataService getDeliveryAreaDataServiceImplInstance() {

		if (deliveryAreaDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			deliveryAreaDataServiceImpl = new DeliveryAreaDataServiceImpl();
			deliveryAreaDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return deliveryAreaDataServiceImpl;

	}
	
	public static ResidenceAreaDataService getResidenceAreaDataServiceImplInstance() {

		if (residenceAreaDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			residenceAreaDataServiceImpl = new ResidenceAreaDataServiceImpl();
			residenceAreaDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return residenceAreaDataServiceImpl;

	}

	public static IncentiveTypeDataService getIncentiveTypeDataServiceImplInstance() {

		if (incentiveTypeDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			incentiveTypeDataServiceImpl = new IncentiveTypeDataServiceImpl();
			incentiveTypeDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return incentiveTypeDataServiceImpl;

	}

	public static ProcessTypeDataService getProcessTypeDataServiceImplInstance() {

		if (processTypeDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			processTypeDataServiceImpl = new ProcessTypeDataServiceImpl();
			processTypeDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return processTypeDataServiceImpl;

	}

	public static RegionDataService getRegionDataServiceImplInstance() {

		if (regionDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			regionDataServiceImpl = new RegionDataServiceImpl();
			regionDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return regionDataServiceImpl;

	}
	
	public static SettingsDataService getSettingsDataServiceImplInstance() {

		if (settingsDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			settingsDataServiceImpl = new SettingsDataServiceImpl();
			settingsDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return settingsDataServiceImpl;

	}
	
	public static ParameterTypeDataService getParameterTypeDataServiceImplInstance() {

		if (parameterTypeDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			parameterTypeDataServiceImpl = new ParameterTypeDataServiceImpl();
			parameterTypeDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return parameterTypeDataServiceImpl;

	}
	
	public static KpiTypeDataService getKpiTypeDataServiceImplInstance() {

		if (kpiTypeDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			kpiTypeDataServiceImpl = new KpiTypeDataServiceImpl();
			kpiTypeDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return kpiTypeDataServiceImpl;
	

	}
	
	
	
	
	public static DemandModelTypeDataService getDemandModelTypeDataServiceImplInstance() {

		if (demandModelTypeDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			demandModelTypeDataServiceImpl = new DemandModelTypeDataServiceImpl();
			demandModelTypeDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return demandModelTypeDataServiceImpl;
	

	}
	
	public static CustomerDataService getCustomerDataServiceImplInstance() {

		if (customerDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			customerDataServiceImpl = new CustomerDataServiceImpl();
			customerDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return customerDataServiceImpl;
	

	}
	
	public static OrderContentTypeDataService getOrderContentTypeDataServiceImplInstance() {

		if (orderContentTypeDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			orderContentTypeDataServiceImpl = new OrderContentTypeDataServiceImpl();
			orderContentTypeDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return orderContentTypeDataServiceImpl;
	

	}
	
	public static OrderRequestDataService getOrderRequestDataServiceImplInstance() {

		if (orderRequestDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			orderRequestDataServiceImpl = new OrderRequestDataServiceImpl();
			orderRequestDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return orderRequestDataServiceImpl;
	

	}
	
	public static OrderDataService getOrderDataServiceImplInstance() {

		if (orderDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			orderDataServiceImpl = new OrderDataServiceImpl();
			orderDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return orderDataServiceImpl;
	

	}
	
	public static TimeWindowDataService getTimeWindowDataServiceImplInstance() {

		if (timeWindowDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			timeWindowDataServiceImpl = new TimeWindowDataServiceImpl();
			timeWindowDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return timeWindowDataServiceImpl;
	

	}
	
	public static AlternativeDataService getAlternativeDataServiceImplInstance() {

		if (alternativeDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			alternativeDataServiceImpl = new AlternativeDataServiceImpl();
			alternativeDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return alternativeDataServiceImpl;
	

	}
	
	public static VehicleTypeDataService getVehicleTypeDataServiceImplInstance() {

		if (vehicleTypeDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			vehicleTypeDataServiceImpl = new VehicleTypeDataServiceImpl();
			vehicleTypeDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return vehicleTypeDataServiceImpl;
	

	}
	
	public static RoutingDataService getRoutingDataServiceImplInstance() {

		if (routingDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			routingDataServiceImpl = new RoutingDataServiceImpl();
			routingDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return routingDataServiceImpl;
	

	}
	
	public static CapacityDataService getCapacityDataServiceImplInstance() {

		if (capacityDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			capacityDataServiceImpl = new CapacityDataServiceImpl();
			capacityDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return capacityDataServiceImpl;
	

	}
	
	public static ValueBucketForecastDataService getValueBucketForecastDataServiceImplInstance() {

		if (valueBucketForecastDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			valueBucketForecastDataServiceImpl = new ValueBucketForecastDataServiceImpl();
			valueBucketForecastDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return valueBucketForecastDataServiceImpl;
	

	}
	
	public static DemandSegmentForecastDataService getDemandSegmentForecastDataServiceImplInstance() {

		if (demandSegmentForecastDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			demandSegmentForecastDataServiceImpl = new DemandSegmentForecastDataServiceImpl();
			demandSegmentForecastDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return demandSegmentForecastDataServiceImpl;
	

	}
	
	public static ControlDataService getControlDataServiceImplInstance() {

		if (controlDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			controlDataServiceImpl = new ControlDataServiceImpl();
			controlDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return controlDataServiceImpl;
	

	}
	
	public static DeliveryDataService getDeliveryDataServiceImplInstance() {

		if (deliveryDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			deliveryDataServiceImpl = new DeliveryDataServiceImpl();
			deliveryDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return deliveryDataServiceImpl;
	

	}
	
	public static ProbabilityDistributionTypeDataService getProbabilityDistributionTypeDataServiceImplInstance() {

		if (probabilityDistributionTypeDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			probabilityDistributionTypeDataServiceImpl = new ProbabilityDistributionTypeDataServiceImpl();
			probabilityDistributionTypeDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return probabilityDistributionTypeDataServiceImpl;
	

	}
	
	public static ProbabilityDistributionDataService getProbabilityDistributionDataServiceImplInstance() {

		if (probabilityDistributionDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			probabilityDistributionDataServiceImpl = new ProbabilityDistributionDataServiceImpl();
			probabilityDistributionDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return probabilityDistributionDataServiceImpl;
	

	}
	
	public static TravelTimeDataService getTravelTimeDataServiceImplInstance() {

		if (travelTimeDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			travelTimeDataServiceImpl = new TravelTimeDataServiceImpl();
			travelTimeDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return travelTimeDataServiceImpl;
	

	}
	
	public static ServiceTimeSegmentDataService getServiceTimeSegmentDataServiceImplInstance() {

		if (serviceTimeSegmentDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			serviceTimeSegmentDataServiceImpl = new ServiceTimeSegmentDataServiceImpl();
			serviceTimeSegmentDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return serviceTimeSegmentDataServiceImpl;
	

	}
	
	public static ArrivalProcessDataService getArrivalProcessDataServiceImplInstance() {

		if (arrivalProcessDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			arrivalProcessDataServiceImpl = new ArrivalProcessDataServiceImpl();
			arrivalProcessDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return arrivalProcessDataServiceImpl;
	

	}
	
	public static DemandSegmentDataService getDemandSegmentDataServiceImplInstance() {

		if (demandSegmentDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			demandSegmentDataServiceImpl = new DemandSegmentDataServiceImpl();
			demandSegmentDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return demandSegmentDataServiceImpl;
	

	}
	
	public static VariableTypeDataService getVariableTypeDataServiceImplInstance() {

		if (variableTypeDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			variableTypeDataServiceImpl = new VariableTypeDataServiceImpl();
			variableTypeDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return variableTypeDataServiceImpl;
	

	}
	
	public static ValueBucketDataService getValueBucketDataServiceImplInstance() {

		if (valueBucketDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			valueBucketDataServiceImpl = new ValueBucketDataServiceImpl();
			valueBucketDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return valueBucketDataServiceImpl;
	

	}
	

	public static DepotDataService getDepotDataServiceImplInstance() {

		if (depotDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			depotDataServiceImpl = new DepotDataServiceImpl();
			depotDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return depotDataServiceImpl;
	

	}
	
	
	public static ObjectiveTypeDataService getObjectiveTypeDataServiceImplInstance() {

		if (objectiveTypeDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			objectiveTypeDataServiceImpl = new ObjectiveTypeDataServiceImpl();
			objectiveTypeDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return objectiveTypeDataServiceImpl;
	

	}
	
	public static VehicleAssignmentDataService getVehicleAssignmentDataServiceImplInstance() {

		if (vehicleAssignmentDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			vehicleAssignmentDataServiceImpl = new VehicleAssignmentDataServiceImpl();
			vehicleAssignmentDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return vehicleAssignmentDataServiceImpl;
	

	}
	
	public static ExpectedDeliveryTimeConsumptionDataService getExpectedDeliveryTimeConsumptionDataServiceImplInstance() {

		if (expectedDeliveryTimeConsumptionDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			expectedDeliveryTimeConsumptionDataServiceImpl = new ExpectedDeliveryTimeConsumptionDataServiceImpl();
			expectedDeliveryTimeConsumptionDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return expectedDeliveryTimeConsumptionDataServiceImpl;
	

	}
	
	public static ValueFunctionApproximationDataService getValueFunctionApproximationDataServiceImplInstance() {

		if (valueFunctionApproximationDataServiceImpl == null) {

			JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
			valueFunctionApproximationDataServiceImpl = new ValueFunctionApproximationDataServiceImpl();
			valueFunctionApproximationDataServiceImpl.setJdbcTemplate(jdbcTemplateInstance);
		}

		return valueFunctionApproximationDataServiceImpl;
	

	}
}
