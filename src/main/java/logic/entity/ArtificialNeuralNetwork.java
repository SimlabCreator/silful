package logic.entity;

public class ArtificialNeuralNetwork {
	private  String[] inputTypes;
	private  Integer[] inputSetIds;
	private  Integer[] inputElementIds;
	private  double[] weights;
	private  double thresholds[];
	private double maximumValue;
	private double minimumValue;
	private double theftWeight;
	private double[] maxValuePerElement;
	private boolean thefting;
	private boolean considerConstant;
	private Integer numberOfHidden;
	private boolean considerDemandNeighbors;
	private boolean useHyperbolicTangens;
	
	
	public ArtificialNeuralNetwork(String[] inputTypes, Integer numberOfHidden, Integer[] inputSetIds,
			Integer[] inputElementIds, double weights[], double thresholds[], 
			double maximumValue, double minimumValue, double theftWeight, 
			boolean thefting, double[] maxValuePerElement, boolean considerConstant, boolean considerDemandNeighbors, boolean useHyperbolicTangens) {
		this.inputElementIds = inputElementIds;
		this.inputTypes = inputTypes;
		this.inputSetIds = inputSetIds;
		this.weights = weights;
		this.thresholds = thresholds;
		this.setMaximumValue(maximumValue);
		this.setMinimumValue(minimumValue);
		this.setTheftWeight(theftWeight);
		this.maxValuePerElement=maxValuePerElement;
		this.thefting=thefting;
		this.setConsiderConstant(considerConstant);
		this.setNumberOfHidden(numberOfHidden);
		this.setConsiderDemandNeighbors(considerDemandNeighbors);
		this.setUseHyperbolicTangens(useHyperbolicTangens);
	}
	
	public ArtificialNeuralNetwork(){
		
	}
	

	public String[] getInputTypes() {
		return inputTypes;
	}

	public void setInputTypes(String[] inputTypes) {
		this.inputTypes = inputTypes;
	}

	public Integer[] getInputSetIds() {
		return inputSetIds;
	}

	public void setInputSetIds(Integer[] inputSetIds) {
		this.inputSetIds = inputSetIds;
	}

	public Integer[] getInputElementIds() {
		return inputElementIds;
	}

	public void setInputElementIds(Integer[] inputElementIds) {
		this.inputElementIds = inputElementIds;
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
	public double getTheftWeight() {
		return theftWeight;
	}
	public void setTheftWeight(double theftWeight) {
		this.theftWeight = theftWeight;
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
	public boolean isThefting() {
		return thefting;
	}
	public void setThefting(boolean thefting) {
		this.thefting = thefting;
	}
	public boolean isConsiderConstant() {
		return considerConstant;
	}
	public void setConsiderConstant(boolean considerConstant) {
		this.considerConstant = considerConstant;
	}

	public Integer getNumberOfHidden() {
		return numberOfHidden;
	}

	public void setNumberOfHidden(Integer numberOfHidden) {
		this.numberOfHidden = numberOfHidden;
	}

	public boolean isConsiderDemandNeighbors() {
		return considerDemandNeighbors;
	}

	public void setConsiderDemandNeighbors(boolean considerDemandNeighbors) {
		this.considerDemandNeighbors = considerDemandNeighbors;
	}

	public boolean isUseHyperbolicTangens() {
		return useHyperbolicTangens;
	}

	public void setUseHyperbolicTangens(boolean useHyperbolicTangens) {
		this.useHyperbolicTangens = useHyperbolicTangens;
	}



}
