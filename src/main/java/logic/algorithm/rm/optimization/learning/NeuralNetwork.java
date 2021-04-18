package logic.algorithm.rm.optimization.learning;

import logic.service.support.ProbabilityDistributionService;

/**
 * Neural Network, based on
 * Neural Network Feedforward Backpropagation Neural Network Written in 2002 by
 * Jeff Heaton(http://www.jeffheaton.com)
 *
 * This class is released under the limited GNU public license (LGPL).
 *
 * @author Jeff Heaton
 * @version 1.0
 */

public class NeuralNetwork {

	private String[] inputTypes;
	private Integer[] inputSetIds;
	private Integer[] inputElementIds;
	private double[] maximumValuesElements;

	/**
	 * The global error for the training.
	 */
	protected double globalError;

	/**
	 * The number of input neurons.
	 */
	protected int inputCount;

	/**
	 * The number of hidden neurons.
	 */
	protected int hiddenCount;

	/**
	 * The number of output neurons
	 */
	protected int outputCount;

	/**
	 * The total number of neurons in the network.
	 */
	protected int neuronCount;

	/**
	 * The number of weights in the network.
	 */
	protected int weightCount;

	/**
	 * The learning rate.
	 */
	protected double learnRate;

	/**
	 * The outputs from the various levels.
	 */
	protected double fire[];

	public double[] getMatrix() {
		return matrix;
	}

	public void setMatrix(double[] matrix) {
		this.matrix = matrix;
	}

	/**
	 * The weight matrix this, along with the thresholds can be thought of as
	 * the "memory" of the neural network.
	 */
	protected double matrix[];

	/**
	 * The errors from the last calculation.
	 */
	protected double error[];

	/**
	 * Accumulates matrix delta's for training.
	 */
	protected double accMatrixDelta[];

	/**
	 * The thresholds, this value, along with the weight matrix can be thought
	 * of as the memory of the neural network.
	 */
	protected double thresholds[];

	/**
	 * The changes that should be applied to the weight matrix.
	 */
	protected double matrixDelta[];

	/**
	 * The accumulation of the threshold deltas.
	 */
	protected double accThresholdDelta[];

	/**
	 * The threshold deltas.
	 */
	protected double thresholdDelta[];

	/**
	 * The momentum for training.
	 */
	protected double momentum;

	/**
	 * The changes in the errors.
	 */
	protected double errorDelta[];

	private boolean hyperbolicTangensActivation;

	public boolean isHyperbolicTangensActivation() {
		return hyperbolicTangensActivation;
	}

	public void setHyperbolicTangensActivation(boolean hyperbolicTangensActivation) {
		this.hyperbolicTangensActivation = hyperbolicTangensActivation;
	}

	/**
	 * Construct the neural network.
	 *
	 * @param inputCount
	 *            The number of input neurons.
	 * @param hiddenCount
	 *            The number of hidden neurons
	 * @param outputCount
	 *            The number of output neurons
	 * @param learnRate
	 *            The learning rate to be used when training.
	 * @param momentum
	 *            The momentum to be used when training.
	 */
	public NeuralNetwork(int inputCount, int hiddenCount, int outputCount, double learnRate, double momentum,
			boolean hyperbolicTangensActivation) {

		this.learnRate = learnRate;
		this.momentum = momentum;

		this.inputCount = inputCount;
		this.hiddenCount = hiddenCount;
		this.outputCount = outputCount;
		neuronCount = inputCount + hiddenCount + outputCount;
		weightCount = (inputCount * hiddenCount) + (hiddenCount * outputCount);

		fire = new double[neuronCount];
		matrix = new double[weightCount];
		matrixDelta = new double[weightCount];
		thresholds = new double[neuronCount];
		errorDelta = new double[neuronCount];
		error = new double[neuronCount];
		accThresholdDelta = new double[neuronCount];
		accMatrixDelta = new double[weightCount];
		thresholdDelta = new double[neuronCount];

		this.hyperbolicTangensActivation = hyperbolicTangensActivation;
		reset();
	}

	/**
	 * Returns the root mean square error for a complete training set.
	 *
	 * @param len
	 *            The length of a complete training set.
	 * @return The current error for the neural network.
	 */
	public double getError(int len) {
		double err = Math.sqrt(globalError / (len * outputCount));
		globalError = 0; // clear the accumulator
		return err;

	}

	/**
	 * The threshold method. You may wish to override this class to provide
	 * other threshold methods. available: logistic function, hyperbolic tangens
	 *
	 * @param sum
	 *            The activation from the neuron.
	 * @return The activation applied to the threshold method.
	 */
	public double threshold(double sum) {

		if (!this.hyperbolicTangensActivation) {
			double output;

			output = 1.0 / (1.0 + Math.exp(-1.0 * sum));
			return output;
		} else {
			// scale input/output to -1: 1
			double output = (Math.exp(sum) - Math.exp(-sum)) / (Math.exp(sum) + Math.exp(-sum));
			return output;
		}
	}

	/**
	 * Compute the output for a given input to the neural network.
	 *
	 * @param input
	 *            The input provide to the neural network.
	 * @return The results from the output neurons.
	 */
	public double[] computeOutputs(double input[]) {
		int i, j;
		final int hiddenIndex = inputCount;
		final int outIndex = inputCount + hiddenCount;

		for (i = 0; i < inputCount; i++) {
			fire[i] = input[i];
		}

		// first layer
		int inx = 0;

		for (i = hiddenIndex; i < outIndex; i++) {
			double sum = thresholds[i];

			for (j = 0; j < inputCount; j++) {
				sum += fire[j] * matrix[inx++];
			}
			fire[i] = threshold(sum);
		}

		// hidden layer

		double result[] = new double[outputCount];

		for (i = outIndex; i < neuronCount; i++) {
			double sum = thresholds[i];

			for (j = hiddenIndex; j < outIndex; j++) {
				sum += fire[j] * matrix[inx++];
			}
			fire[i] = threshold(sum);
			result[i - outIndex] = fire[i];
		}

		return result;
	}

	/**
	 * Loss function: residual sum of squares -sum((obs-exp)^2) Calculate the
	 * error for the recogntion just done.
	 *
	 * @param ideal
	 *            What the output neurons should have yielded.
	 */
	public double calcError(double ideal[]) {
		int i, j;
		final int hiddenIndex = inputCount;
		final int outputIndex = inputCount + hiddenCount;
		double errorToReturn = 0;
		// clear hidden layer errors
		for (i = inputCount; i < neuronCount; i++) {
			error[i] = 0;
		}

		// layer errors and deltas for output layer
		for (i = outputIndex; i < neuronCount; i++) {
			error[i] = ideal[i - outputIndex] - fire[i];
			errorToReturn = error[i];
			globalError += error[i] * error[i];
			if (!this.hyperbolicTangensActivation) {
				errorDelta[i] = error[i] * fire[i] * (1 - fire[i]);
			} else {
				errorDelta[i] = error[i] * (1 - Math.pow(fire[i], 2));
			}

		}

		// hidden layer errors
		int winx = inputCount * hiddenCount;

		for (i = outputIndex; i < neuronCount; i++) {
			for (j = hiddenIndex; j < outputIndex; j++) {
				accMatrixDelta[winx] += errorDelta[i] * fire[j];
				error[j] += matrix[winx] * errorDelta[i];
				winx++;
			}
			accThresholdDelta[i] += errorDelta[i];
		}

		// hidden layer deltas
		for (i = hiddenIndex; i < outputIndex; i++) {

			if (!this.hyperbolicTangensActivation) {
				errorDelta[i] = error[i] * fire[i] * (1 - fire[i]);
			} else {
				errorDelta[i] = error[i] * (1 - Math.pow(fire[i], 2));
			}

		}

		// input layer errors
		winx = 0; // offset into weight array
		for (i = hiddenIndex; i < outputIndex; i++) {
			for (j = 0; j < hiddenIndex; j++) {
				accMatrixDelta[winx] += errorDelta[i] * fire[j];
				error[j] += matrix[winx] * errorDelta[i];
				winx++;
			}
			accThresholdDelta[i] += errorDelta[i];
		}

		return errorToReturn;
	}

	/**
	 * Modify the weight matrix and thresholds based on the last call to
	 * calcError.
	 */
	public void learn() {
		int i;

		// process the matrix
		for (i = 0; i < matrix.length; i++) {
			matrixDelta[i] = (learnRate * accMatrixDelta[i]) + (momentum * matrixDelta[i]);
			matrix[i] += matrixDelta[i];
			accMatrixDelta[i] = 0;
		}

		// process the thresholds
		for (i = inputCount; i < neuronCount; i++) {
			thresholdDelta[i] = learnRate * accThresholdDelta[i] + (momentum * thresholdDelta[i]);
			thresholds[i] += thresholdDelta[i];
			accThresholdDelta[i] = 0;
		}
	}

	/**
	 * Reset the weight matrix and the thresholds. Init weights according to
	 * Glorot, Xavier, and Yoshua Bengio. "Understanding the difficulty of
	 * training deep feedforward neural networks." International conference on
	 * artificial intelligence and statistics. 2010.
	 * 
	 */
	public void reset() {
		int i, j;

		for (i = 0; i < neuronCount; i++) {
			thresholds[i] = 0.5 - (Math.random());
			thresholdDelta[i] = 0;
			accThresholdDelta[i] = 0;
		}
//		for (i = 0; i < matrix.length; i++) {
//			matrix[i] = 0.5 - (Math.random());
//			matrixDelta[i] = 0;
//			accMatrixDelta[i] = 0;
//		}

		//Init weights
		final int hiddenIndex = inputCount;
		final int outIndex = inputCount + hiddenCount;
		double r;
		int inv=0;
		//Between input and hidden layer
		for (i = hiddenIndex; i < outIndex; i++) {
			
			r = Math.sqrt(6.0 / (inputCount + hiddenCount));
			if (!this.hyperbolicTangensActivation) {
				r = 4 * r;
			}
			
			for (j = 0; j < inputCount; j++) {
			
				matrix[inv] = ProbabilityDistributionService.getUniformRandomNumber(-r, r);
				matrixDelta[inv] = 0;
				accMatrixDelta[inv++] = 0;
			}
			
		}
		
		//Between hidden and output layer
		for (i = outIndex; i < neuronCount; i++) {
			r = Math.sqrt(6.0 / (outputCount+hiddenCount));
			if (!this.hyperbolicTangensActivation) {
				r = 4 * r;
			}
			
			for (j = hiddenIndex;j < outIndex; j++) {
				
				matrix[inv] = ProbabilityDistributionService.getUniformRandomNumber(-r, r);
				matrixDelta[inv] = 0;
				accMatrixDelta[inv++] = 0;
			}
		}

	//	System.out.println("Inv is "+inv);
	}
	
	public void initOutgoingWeightsNegative (int inputIndex){
		int index = inputIndex;
		for(int i=0; i < this.hiddenCount; i++){
			matrix[index] = -1.0*Math.abs(matrix[index]);
			index = index + this.inputCount;
		}
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

	public double[] getError() {
		return error;
	}

	public double getLearnRate() {
		return learnRate;
	}

	public void setLearnRate(double learnRate) {
		this.learnRate = learnRate;
	}

	public double[] getThresholds() {
		return thresholds;
	}

	public void setThresholds(double[] thresholds) {
		this.thresholds = thresholds;
	}

	public double[] getMaximumValuesElements() {
		return maximumValuesElements;
	}

	public void setMaximumValuesElements(double[] maximumValuesElements) {
		this.maximumValuesElements = maximumValuesElements;
	}

}
