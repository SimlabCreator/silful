package data.entity;

import java.util.ArrayList;

public class PeriodSetting extends Entity {

	private Integer startingPeriod;
	private Integer runId;
	private Integer deliveryAreaSetId;
	private ArrayList<GeneralParameterValue> parameterValues = new ArrayList<GeneralParameterValue>();
	private ArrayList<GeneralAtomicOutputValue> atomicOutputs = new ArrayList<GeneralAtomicOutputValue>();
	private Integer alternativeSetId;
	private Integer timeWindowSetId;
	private Integer customerSetId;
	private Integer serviceSegmentWeightingId; // TODO Add later that you can
												// have
												// both a producing and an
												// approximated weighting of
												// service
												// time set
	private Integer arrivalProcessId;// TODO Add later that you can have both a
	// producing and an approximated arrival rate
	private Integer arrivalProbabilityDistributionId;
	private Integer demandSegmentWeightingId;// TODO Add later that you can have
												// both a producing and an
												// approximated weighting of
												// demand
												// segment set
												// TODO Add that you can have
												// several weightings per period
												// if
												// changing weights over sales
												// horizon
	private Integer demandSegmentSetId;
	private Integer orderSetId;
	private Integer historicalOrderSetId;
	private Integer orderRequestSetId;
	private Integer historicalDeliverySetId;
	private Integer valueBucketSetId;
	private Integer valueBucketForecastSetId;
	private Integer demandSegmentForecastSetId;
	private Integer historicalDemandForecastSetId;
	private Integer capacitySetId;
	private Integer controlSetId;
	private Integer dynamicProgrammingTreeId;
	private Integer travelTimeSetId;
	private ArrayList<RoutingAssignment> routingAssignments;
	private ArrayList<Vehicle> vehicles; // Not always needed, depends on
											// process type
	private Integer vehicleAssignmentSetid;

	private ArrayList<Kpi> kpiValues = new ArrayList<Kpi>();
	private Integer learningOutputRequestsExperimentId;
	private ArrayList<Integer> learningOutputFinalRoutingsExperimentIds;
	private ArrayList<Integer> benchmarkingOutputFinalRoutingsExperimentIds;
	private ArrayList<Integer> learningOutputOrderSetsExperimentIds;
	private ArrayList<Integer> benchmarkingOutputOrderSetsExperimentIds;
	private Integer valueFunctionModelId;

	private int id;

	// TODO: No functionality, only for parent-class
	public int getId() {
		return id;
	}

	public Integer getStartingPeriod() {
		return startingPeriod;
	}

	public void setStartingPeriod(Integer startingPeriod) {
		this.startingPeriod = startingPeriod;
	}

	public Integer getDeliveryAreaSetId() {
		return deliveryAreaSetId;
	}

	public void setDeliveryAreaSetId(Integer deliveryAreaId) {
		this.deliveryAreaSetId = deliveryAreaId;
	}

	public ArrayList<GeneralParameterValue> getParameterValues() {
		return parameterValues;
	}

	public void setParameterValues(ArrayList<GeneralParameterValue> parameterValues) {
		this.parameterValues = parameterValues;
	}

	/**
	 * Add a parameter value to the parameter value list
	 * 
	 * @param parameter
	 *            The respective parameter
	 */
	public void addParameterValue(GeneralParameterValue parameter) {
		this.parameterValues.add(parameter);
	};

	public Integer getAlternativeSetId() {
		return alternativeSetId;
	}

	public void setAlternativeSetId(Integer alternativeSetId) {
		this.alternativeSetId = alternativeSetId;
	}

	public Integer getCustomerSetId() {
		return customerSetId;
	}

	public void setCustomerSetId(Integer customerSetId) {
		this.customerSetId = customerSetId;
	}

	public Integer getArrivalProcessId() {
		return arrivalProcessId;
	}

	public void setArrivalProcessId(Integer arrivalRateId) {
		this.arrivalProcessId = arrivalRateId;
	}

	public Integer getDemandSegmentWeightingId() {
		return demandSegmentWeightingId;
	}

	public void setDemandSegmentWeightingId(Integer demandSegmentWeightingId) {
		this.demandSegmentWeightingId = demandSegmentWeightingId;
	}

	public Integer getOrderSetId() {
		return orderSetId;
	}

	public void setOrderSetId(Integer orderSetId) {
		this.orderSetId = orderSetId;
	}

	public Integer getOrderRequestSetId() {
		return orderRequestSetId;
	}

	public void setOrderRequestSetId(Integer orderRequestSetId) {
		this.orderRequestSetId = orderRequestSetId;
	}

	public Integer getValueBucketForecastSetId() {
		return valueBucketForecastSetId;
	}

	public void setValueBucketForecastSetId(Integer demandForecastSetId) {
		this.valueBucketForecastSetId = demandForecastSetId;
	}

	public Integer getCapacitySetId() {
		return capacitySetId;
	}

	public void setCapacitySetId(Integer capacitySetId) {
		this.capacitySetId = capacitySetId;
	}

	public Integer getControlSetId() {
		return controlSetId;
	}

	public void setControlSetId(Integer controlSetId) {
		this.controlSetId = controlSetId;
	}

	public Integer getServiceSegmentWeightingId() {
		return serviceSegmentWeightingId;
	}

	public void setServiceSegmentWeightingId(Integer serviceSegmentWeightingId) {
		this.serviceSegmentWeightingId = serviceSegmentWeightingId;
	}

	public Integer getRunId() {
		return runId;
	}

	public void setRunId(Integer runId) {
		this.runId = runId;
	}

	/**
	 * Add a kpi value
	 * 
	 * @param kpi
	 *            Respective kpi
	 * @param value
	 *            Respective value
	 */
	public void addKpi(Kpi kpi) {
		this.kpiValues.add(kpi);
	}

	public ArrayList<Kpi> getKpis() {
		return this.kpiValues;
	}

	public ArrayList<RoutingAssignment> getRoutingAssignments() {
		return routingAssignments;
	}

	public void setRoutingAssignments(ArrayList<RoutingAssignment> routingAssignments) {
		this.routingAssignments = routingAssignments;
	}

	public void addRoutingAssignment(RoutingAssignment routingAssignment) {
		if (this.routingAssignments == null)
			this.routingAssignments = new ArrayList<RoutingAssignment>();
		this.routingAssignments.add(routingAssignment);
	}

	public Integer getTimeWindowSetId() {
		return timeWindowSetId;
	}

	public void setTimeWindowSetId(Integer timeWindowSetId) {
		this.timeWindowSetId = timeWindowSetId;
	}

	public Integer getHistoricalOrderSetId() {
		return historicalOrderSetId;
	}

	public void setHistoricalOrderSetId(Integer historicalOrderSetId) {
		this.historicalOrderSetId = historicalOrderSetId;
	}

	public Integer getHistoricalDeliverySetId() {
		return historicalDeliverySetId;
	}

	public void setHistoricalDeliverySetId(Integer historicalDeliverySetId) {
		this.historicalDeliverySetId = historicalDeliverySetId;
	}

	public Integer getValueBucketSetId() {
		return valueBucketSetId;
	}

	public void setValueBucketSetId(Integer valueBucketSetId) {
		this.valueBucketSetId = valueBucketSetId;
	}

	public Integer getHistoricalDemandForecastValueBucketsSetId() {
		return historicalDemandForecastSetId;
	}

	public void setHistoricalDemandForecastValueBucketsSetId(Integer historicalDemandForecastSetId) {
		this.historicalDemandForecastSetId = historicalDemandForecastSetId;
	}

	public Integer getDemandSegmentForecastSetId() {
		return demandSegmentForecastSetId;
	}

	public void setDemandSegmentForecastSetId(Integer demandSegmentForecastSetId) {
		this.demandSegmentForecastSetId = demandSegmentForecastSetId;
	}

	public Integer getDemandSegmentSetId() {
		return demandSegmentSetId;
	}

	public void setDemandSegmentSetId(Integer demandSegmentSetId) {
		this.demandSegmentSetId = demandSegmentSetId;
	}

	public Integer getDynamicProgrammingTreeId() {
		return dynamicProgrammingTreeId;
	}

	public void setDynamicProgrammingTreeId(Integer dynamicProgrammingTreeId) {
		this.dynamicProgrammingTreeId = dynamicProgrammingTreeId;
	}

	public Integer getTravelTimeSetId() {
		return travelTimeSetId;
	}

	public void setTravelTimeSetId(Integer travelTimeSetId) {
		this.travelTimeSetId = travelTimeSetId;
	}

	public ArrayList<Vehicle> getVehicles() {
		if(this.vehicles==null) this.vehicles=new ArrayList<Vehicle>();
		return vehicles;
	}

	public void setVehicles(ArrayList<Vehicle> vehicles) {
		this.vehicles = vehicles;
	}
	
	public void addVehicle(Vehicle vehicle){
		if(this.vehicles==null) this.vehicles=new ArrayList<Vehicle>();
		this.vehicles.add(vehicle);
	}

	public Integer getVehicleAssignmentSetId() {
		return vehicleAssignmentSetid;
	}

	public void setVehicleAssignmentSetId(Integer vehicleAssignmentSetid) {
		this.vehicleAssignmentSetid = vehicleAssignmentSetid;
	}

	public Integer getLearningOutputRequestsExperimentId() {
		return learningOutputRequestsExperimentId;
	}

	public void setLearningOutputRequestsExperimentId(Integer learningExperimentId) {
		this.learningOutputRequestsExperimentId = learningExperimentId;
	}

	public Integer getValueFunctionModelSetId() {
		return valueFunctionModelId;
	}

	public void setValueFunctionModelSetId(Integer valueFunctionModelId) {
		this.valueFunctionModelId = valueFunctionModelId;
	}

	public ArrayList<Integer> getLearningOutputFinalRoutingsExperimentIds() {
		return learningOutputFinalRoutingsExperimentIds;
	}

	public void setLearningOutputFinalRoutingsExperimentIds(ArrayList<Integer> learningOutputFinalRoutingsExperimentIds) {
		this.learningOutputFinalRoutingsExperimentIds = learningOutputFinalRoutingsExperimentIds;
	}

	public Integer getArrivalProbabilityDistributionId() {
		return arrivalProbabilityDistributionId;
	}

	public void setArrivalProbabilityDistributionId(Integer arrivalProbabilityDistributionId) {
		this.arrivalProbabilityDistributionId = arrivalProbabilityDistributionId;
	}

	public ArrayList<GeneralAtomicOutputValue> getAtomicOutputs() {
		return atomicOutputs;
	}

	public void setAtomicOutputs(ArrayList<GeneralAtomicOutputValue> atomicOutputs) {
		this.atomicOutputs = atomicOutputs;
	}
	
	public void addAtomicOutput(GeneralAtomicOutputValue output){
		if(this.atomicOutputs==null){
			this.atomicOutputs = new ArrayList<GeneralAtomicOutputValue>();
		}
		this.atomicOutputs.add(output);
	}

	public ArrayList<Integer> getBenchmarkingOutputFinalRoutingsExperimentIds() {
		return benchmarkingOutputFinalRoutingsExperimentIds;
	}

	public void setBenchmarkingOutputFinalRoutingsExperimentIds(
			ArrayList<Integer> benchmarkingOutputFinalRoutingsExperimentIds) {
		this.benchmarkingOutputFinalRoutingsExperimentIds = benchmarkingOutputFinalRoutingsExperimentIds;
	}

	public ArrayList<Integer> getLearningOutputOrderSetsExperimentIds() {
		return learningOutputOrderSetsExperimentIds;
	}

	public void setLearningOutputOrderSetsExperimentIds(ArrayList<Integer> learningOutputOrderSetsExperimentIds) {
		this.learningOutputOrderSetsExperimentIds = learningOutputOrderSetsExperimentIds;
	}

	public ArrayList<Integer> getBenchmarkingOutputOrderSetsExperimentIds() {
		return benchmarkingOutputOrderSetsExperimentIds;
	}

	public void setBenchmarkingOutputOrderSetsExperimentIds(ArrayList<Integer> benchmarkingOutputOrderSetsExperimentIds) {
		this.benchmarkingOutputOrderSetsExperimentIds = benchmarkingOutputOrderSetsExperimentIds;
	}

}
