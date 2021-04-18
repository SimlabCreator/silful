package logic.entity;

public class NonParametricValueFunctionAddon {

	private Double[][] lookupTable;

	private Double[] lookupArray;
	private double weight;
	private double[] maxValuePerAttribute;

	public NonParametricValueFunctionAddon(Double[][] lookupTable, Double[] lookupArray, 
			double weight, double[] maxValuePerAttribute) {
		this.lookupArray = lookupArray;
		this.lookupTable = lookupTable;
		this.weight = weight;
		this.setMaxValuePerAttribute(maxValuePerAttribute);
	}
	
	public NonParametricValueFunctionAddon(){
		
	}

	public Double[][] getLookupTable() {
		return lookupTable;
	}

	public void setLookupTable(Double[][] lookupTable) {
		this.lookupTable = lookupTable;
	}

	public Double[] getLookupArray() {
		return lookupArray;
	}

	public void setLookupArray(Double[] lookupArray) {
		this.lookupArray = lookupArray;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}


	public double[] getMaxValuePerAttribute() {
		return maxValuePerAttribute;
	}

	public void setMaxValuePerAttribute(double[] maxValuePerAttribute) {
		this.maxValuePerAttribute = maxValuePerAttribute;
	}

}
