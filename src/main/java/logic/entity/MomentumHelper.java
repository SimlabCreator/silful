package logic.entity;

import java.util.ArrayList;
import java.util.HashMap;

import data.entity.ValueFunctionApproximationCoefficient;

public class MomentumHelper {

	private double basicCoefficientMomentum;
	private double timeCoefficientMomentum;
	HashMap<ValueFunctionApproximationCoefficient, Double> coefficientMomentums;
	private double remainingCapacityMomentum;
	private double areaPotentialMomentum;
	private double timeCapacitiyInteractionMomentum;
	private double acceptedCostMomentum;
	
	private double[] momentumPerAttribute;
	
	public MomentumHelper(ArrayList<ValueFunctionApproximationCoefficient> variableCoefficients){
		basicCoefficientMomentum=0.0;
		timeCoefficientMomentum=0.0;
		remainingCapacityMomentum=0.0;
		areaPotentialMomentum=0.0;
		coefficientMomentums = new HashMap<ValueFunctionApproximationCoefficient, Double>();
		for(ValueFunctionApproximationCoefficient c: variableCoefficients){
			coefficientMomentums.put(c, 0.0);
		}
		setTimeCapacitiyInteractionMomentum(0.0);
		acceptedCostMomentum=0.0;
	}
	
	public MomentumHelper(boolean basic, int numberOfAttributes){
		if(basic) basicCoefficientMomentum=0.0;
		momentumPerAttribute= new double[numberOfAttributes];
		for(int i= 0; i < numberOfAttributes; i++){
			this.momentumPerAttribute[i]=0.0;
		}
		
	}
	public double getBasicCoefficientMomentum() {
		return basicCoefficientMomentum;
	}
	public void setBasicCoefficientMomentum(double basicCoefficientMomentum) {
		this.basicCoefficientMomentum = basicCoefficientMomentum;
	}
	public double getTimeCoefficientMomentum() {
		return timeCoefficientMomentum;
	}
	public void setTimeCoefficientMomentum(double timeCoefficientMomentum) {
		this.timeCoefficientMomentum = timeCoefficientMomentum;
	}
	public HashMap<ValueFunctionApproximationCoefficient, Double> getCoefficientMomentums() {
		return coefficientMomentums;
	}
	public void setCoefficientMomentums(HashMap<ValueFunctionApproximationCoefficient, Double> coefficientMomentums) {
		this.coefficientMomentums = coefficientMomentums;
	}
	public double getRemainingCapacityMomentum() {
		return remainingCapacityMomentum;
	}
	public void setRemainingCapacityMomentum(double remainingCapacityMomentum) {
		this.remainingCapacityMomentum = remainingCapacityMomentum;
	}
	public double getAreaPotentialMomentum() {
		return areaPotentialMomentum;
	}
	public void setAreaPotentialMomentum(double areaPotentialMomentum) {
		this.areaPotentialMomentum = areaPotentialMomentum;
	}
	public double getTimeCapacitiyInteractionMomentum() {
		return timeCapacitiyInteractionMomentum;
	}
	public void setTimeCapacitiyInteractionMomentum(double timeCapacitiyInteractionMomentum) {
		this.timeCapacitiyInteractionMomentum = timeCapacitiyInteractionMomentum;
	}
	public double getAcceptedCostMomentum() {
		return acceptedCostMomentum;
	}
	public void setAcceptedCostMomentum(double acceptedCostMomentum) {
		this.acceptedCostMomentum = acceptedCostMomentum;
	}
	
	public double getMomentumForAttribute(int attributeIndex){
		return this.momentumPerAttribute[attributeIndex];
	}
	
	public void setMomentumForAttribute(int attributeIndex, double momentumValue){
		this.momentumPerAttribute[attributeIndex]=momentumValue;
	}
	
}
