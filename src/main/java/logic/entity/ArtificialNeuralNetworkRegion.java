package logic.entity;

public class ArtificialNeuralNetworkRegion {

	private  double[] weights;
	private  double thresholds[];
	private double maximumValue;
	private double minimumValue;
	private double[] maxValuePerElement;
	private int constant;
	private int distanceMeasure;
	private int remainingBudget;
	private int remainingTime;
	private int[][] demandSegments;
	private int[][] timeWindows;
	private Integer numberOfHidden;
	private boolean useHyperbolicTangens;
	
	
	public ArtificialNeuralNetworkRegion(Integer numberOfHidden, double weights[], double thresholds[], 
			double maximumValue, double minimumValue, double[] maxValuePerElement, int constant, int distanceMeasure,
			int remainingBudget,int remainingTime,int[][] demandSegments, int[][] timeWindows,
			boolean useHyperbolicTangens) {
		this.weights = weights;
		this.thresholds = thresholds;
		this.setMaximumValue(maximumValue);
		this.setMinimumValue(minimumValue);
		this.maxValuePerElement=maxValuePerElement;
		this.constant=constant;
		this.setNumberOfHidden(numberOfHidden);
		this.setUseHyperbolicTangens(useHyperbolicTangens);
		this.distanceMeasure=distanceMeasure;
		this.remainingBudget=remainingBudget;
		this.remainingTime=remainingTime;
		this.demandSegments=demandSegments;
		this.timeWindows=timeWindows;
	}
	
	public ArtificialNeuralNetworkRegion(){
		
	}
	

	public double[] getWeights() {
		return weights;
	}

	public void setWeights(double[] weights) {
		this.weights = weights;
	}

	public double[] getThresholds() {
		return thresholds;
	}

	public void setThresholds(double thresholds[]) {
		this.thresholds = thresholds;
	}

	public double getMaximumValue() {
		return maximumValue;
	}

	public void setMaximumValue(double maximumValue) {
		this.maximumValue = maximumValue;
	}
	
	public int getConstant() {
		return constant;
	}

	public void setConstant(int constant) {
		this.constant = constant;
	}

	public int getDistanceMeasure() {
		return distanceMeasure;
	}

	public void setDistanceMeasure(int distanceMeasure) {
		this.distanceMeasure = distanceMeasure;
	}

	public int getRemainingBudget() {
		return remainingBudget;
	}

	public void setRemainingBudget(int remainingBudget) {
		this.remainingBudget = remainingBudget;
	}

	public int getRemainingTime() {
		return remainingTime;
	}

	public void setRemainingTime(int remainingTime) {
		this.remainingTime = remainingTime;
	}

	public int[][] getDemandSegments() {
		return demandSegments;
	}

	public void setDemandSegments(int[][] demandSegments) {
		this.demandSegments = demandSegments;
	}

	public int[][] getTimeWindows() {
		return timeWindows;
	}

	public void setTimeWindows(int[][] timeWindows) {
		this.timeWindows = timeWindows;
	}

	public double getMinimumValue() {
		return minimumValue;
	}
	public void setMinimumValue(double minimumValue) {
		this.minimumValue = minimumValue;
	}
	public double[] getMaxValuePerElement() {
		return maxValuePerElement;
	}
	public void setMaxValuePerElement(double[] maxValuePerElement) {
		this.maxValuePerElement = maxValuePerElement;
	}


	public Integer getNumberOfHidden() {
		return numberOfHidden;
	}

	public void setNumberOfHidden(Integer numberOfHidden) {
		this.numberOfHidden = numberOfHidden;
	}

	public boolean isUseHyperbolicTangens() {
		return useHyperbolicTangens;
	}

	public void setUseHyperbolicTangens(boolean useHyperbolicTangens) {
		this.useHyperbolicTangens = useHyperbolicTangens;
	}



}
