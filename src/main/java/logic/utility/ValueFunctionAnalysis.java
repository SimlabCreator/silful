package logic.utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.DemandSegment;
import data.entity.DemandSegmentSet;
import data.entity.DemandSegmentWeight;
import data.entity.DemandSegmentWeighting;
import data.entity.Entity;
import data.entity.ObjectiveWeight;
import data.entity.TimeWindowSet;
import data.entity.ValueFunctionApproximationModel;
import data.service.DeliveryAreaDataService;
import data.service.DeliveryAreaDataServiceImpl;
import data.service.DemandSegmentDataService;
import data.service.DemandSegmentDataServiceImpl;
import data.service.TimeWindowDataService;
import data.service.TimeWindowDataServiceImpl;
import data.service.ValueFunctionApproximationDataService;
import data.service.ValueFunctionApproximationDataServiceImpl;
import data.utility.JDBCTemplateProvider;
import logic.algorithm.rm.optimization.learning.NeuralNetwork;
import logic.entity.ArtificialNeuralNetwork;
import logic.service.support.AcceptanceService;
import logic.service.support.CustomerDemandService;
import logic.service.support.LocationService;
import logic.utility.comparator.DemandSegmentsExpectedValueAscComparator;
import logic.utility.exceptions.ParameterUnknownException;

public class ValueFunctionAnalysis {

	public static int valueFunctionModelId = 12;
	public static int demandSegmentWeighting =36;
	public static int deliveryAreaSet = 18;
	public static int T = 220;
	public static double arrivalProbability = 0.5;
	public static int deliveryAreaInFocus = 48;
	public static int[] initialCapacityForFocusAreaTime = new int[] { 1,1,1,1,1,1,2,1,1,3,2,1};

	public static int[] initialCapacityForFocusAreaCap = new int[] {1,1,1,1,1,1,2,1,1,3,2,1};
	
	//97={48=2, 49=1, 50=2, 51=1, 40=1, 41=1, 42=1, 43=1, 44=1, 45=1, 46=1, 47=1}
	//99={48=7, 49=6, 50=8, 51=4, 40=3, 41=2, 42=2, 43=4, 44=2, 45=2, 46=2, 47=5}, T=431, Area = 99
	
	public static int popularIndex = 9;
	public static int unPopularIndex = 6;
	public static int popularOtherIndex=6;
	public static int unPopularOtherIndex=0;
	public static int[] timeStepsCap = new int[] {220,170,120,70,20};
	public static boolean takeZeroAsAppr=true;

	public static void main(String[] args) {

		// Read value function model
		ValueFunctionApproximationDataService vfService = new ValueFunctionApproximationDataServiceImpl();
		JdbcTemplate jdbcTemplateInstance = JDBCTemplateProvider.getInstance();
		vfService.setJdbcTemplate(jdbcTemplateInstance);
		ValueFunctionApproximationModel model = vfService.getElementById(valueFunctionModelId);

		ObjectMapper mapper = new ObjectMapper();
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		ArtificialNeuralNetwork ann = null;
		try {
			ann = mapper.readValue(model.getComplexModelJSON(), ArtificialNeuralNetwork.class);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		int numberOfInput = ann.getInputElementIds().length;
		if(ann.isConsiderConstant())numberOfInput++;
		NeuralNetwork annToUse = new NeuralNetwork(numberOfInput,
				(int) ann.getNumberOfHidden(), 1, 0.0, 0.0, ann.isUseHyperbolicTangens());
		annToUse.setMatrix(ann.getWeights());
		annToUse.setThresholds(ann.getThresholds());

		// Read delivery area set
		DeliveryAreaDataService daService = new DeliveryAreaDataServiceImpl();
		daService.setJdbcTemplate(jdbcTemplateInstance);
		DeliveryAreaSet daSet = (DeliveryAreaSet) daService.getSetById(deliveryAreaSet);

		// Read time window set
		TimeWindowDataService twService = new TimeWindowDataServiceImpl();
		twService.setJdbcTemplate(jdbcTemplateInstance);
		TimeWindowSet twSet = (TimeWindowSet) twService.getSetById(ann.getInputSetIds()[1]);

		// Read demand segment
		DemandSegmentDataService dsService = new DemandSegmentDataServiceImpl();
		dsService.setJdbcTemplate(jdbcTemplateInstance);
		DemandSegmentWeighting dsWeighting = (DemandSegmentWeighting) dsService
				.getWeightingById(demandSegmentWeighting);
		ArrayList<DemandSegment> demandSegments = (ArrayList<DemandSegment>) dsWeighting.getSetEntity().getElements();
		Double maximumRevenueValue = null;
		try {
			maximumRevenueValue = AcceptanceService
					.determineMaximumRevenueValueForNormalisation((DemandSegmentSet) dsWeighting.getSetEntity());
		} catch (ParameterUnknownException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		HashMap<DeliveryArea, Double> daWeights = new HashMap<DeliveryArea, Double>();
		HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightings = new HashMap<DeliveryArea, DemandSegmentWeighting>();
		LocationService.determineDeliveryAreaWeightAndDemandSegmentWeightingPerDeliveryAreaConsideringHierarchy(
				daWeights, daSegmentWeightings, daSet, dsWeighting);

		HashMap<Integer, HashMap<Integer, Double>> maximumExpectedMultiplierPerDemandSegmentAndTimeWindow = CustomerDemandService
				.determineMaximumProbabilityPerDemandSegmentAndTimeWindow(dsWeighting, twSet);
		HashMap<Integer, HashMap<Integer, Double>> minimumExpectedMultiplierPerDemandSegmentAndTimeWindow = CustomerDemandService
				.determineMinimumProbabilityPerDemandSegmentAndTimeWindow(dsWeighting, twSet);

		HashMap<Entity, Object> objectiveSpecificValues = new HashMap<Entity, Object>();

		DeliveryArea subArea = null;
		for (DeliveryArea area : daWeights.keySet()) {
			if (area.getId() == deliveryAreaInFocus)
				subArea = area;

		}

		StringBuilder sb = new StringBuilder();
		sb.append("t,value");
		sb.append("\n");
		for (int t = T; t >= 0; t--) {
			if (takeZeroAsAppr || t > 0) {
				double[] inputRow = new double[numberOfInput];

				double expectedArrivalsDeliveryArea = t * arrivalProbability * daWeights.get(subArea);

				int currentId = 0;
				if(ann.isConsiderConstant()) inputRow[numberOfInput-1]=1.0;
				for (DemandSegmentWeight w : daSegmentWeightings.get(subArea).getWeights()) {
					inputRow[currentId] = expectedArrivalsDeliveryArea * w.getWeight()
							/ ann.getMaxValuePerElement()[currentId];
					currentId++;
				}

				int leftOverCapacityOverall = 0;
				for (int i = 0; i < initialCapacityForFocusAreaTime.length; i++) {

					int remainingCap = initialCapacityForFocusAreaTime[i];

					inputRow[currentId] = remainingCap / (double) ann.getMaxValuePerElement()[currentId];

					currentId++;

					leftOverCapacityOverall += remainingCap;
				}

				if (leftOverCapacityOverall == 0) {
					sb.append(t + "," + 0.0);
					sb.append("\n");
				} else {

					double[] output = annToUse.computeOutputs(inputRow);

					double value = output[0];
					if(ann.isUseHyperbolicTangens()){
						value=(value+1.0)/2.0;
					}
					value=value * (ann.getMaximumValue() + ann.getMinimumValue()) - ann.getMinimumValue();
					sb.append(t + "," + value);
					sb.append("\n");
				}

			} else {

				
				
					sb.append(t + "," + 0.0);
					sb.append("\n");
				

			}

		}

		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new File("valueFunctionAnalysis_timeDevelopment.csv"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		pw.write(sb.toString());
		pw.close();

		sb = new StringBuilder();
		StringBuilder sbO = new StringBuilder();
		StringBuilder sbOther = new StringBuilder();
		sb.append("t,cap,value");
		sb.append("\n");
		sbO.append("t,cap,value");
		sbO.append("\n");
		sbOther.append("t,cap,capOther,value");
		sbOther.append("\n");
		for (int i = 0; i < timeStepsCap.length; i++) {
			int t = timeStepsCap[i];

			for (int cap = initialCapacityForFocusAreaCap[popularIndex]; cap >= 0; cap--) {

				double[] inputRow = new double[numberOfInput];

				double expectedArrivalsDeliveryArea = t * arrivalProbability * daWeights.get(subArea);

				int currentId = 0;
				if(ann.isConsiderConstant()) inputRow[numberOfInput-1]=1.0;
				for (DemandSegmentWeight w : daSegmentWeightings.get(subArea).getWeights()) {
					inputRow[currentId] = expectedArrivalsDeliveryArea * w.getWeight()
							/ ann.getMaxValuePerElement()[currentId];
					currentId++;
				}

				int leftOverCapacityOverall = 0;
				for (int j = 0; j < initialCapacityForFocusAreaCap.length; j++) {

					int remainingCap = initialCapacityForFocusAreaCap[j];
					if (j == popularIndex)
						remainingCap = cap;

					inputRow[currentId] = remainingCap / (double) ann.getMaxValuePerElement()[currentId];

					currentId++;

					leftOverCapacityOverall += remainingCap;
				}

				if (!takeZeroAsAppr && leftOverCapacityOverall == 0) {
					sb.append(t + "," + cap + "," + 0.0);
					sb.append("\n");
				} else {

					double[] output = annToUse.computeOutputs(inputRow);

					double value = output[0];
					if(ann.isUseHyperbolicTangens()){
						value=(value+1.0)/2.0;
					}
					value=value * (ann.getMaximumValue() + ann.getMinimumValue()) - ann.getMinimumValue();
					sb.append(t + "," + cap + "," + value);
					sb.append("\n");

					// Determine opportunity costs
					if (cap > 0) {
						inputRow = new double[numberOfInput];
						if(ann.isConsiderConstant()) inputRow[numberOfInput-1]=1.0;
						currentId = 0;
						for (DemandSegmentWeight w : daSegmentWeightings.get(subArea).getWeights()) {
							inputRow[currentId] = expectedArrivalsDeliveryArea * w.getWeight()
									/ ann.getMaxValuePerElement()[currentId];
							currentId++;
						}

						leftOverCapacityOverall = 0;
						for (int j = 0; j < initialCapacityForFocusAreaCap.length; j++) {

							int remainingCap = initialCapacityForFocusAreaCap[j];
							if (j == popularIndex)
								remainingCap = cap - 1;

							inputRow[currentId] = remainingCap / (double) ann.getMaxValuePerElement()[currentId];

							currentId++;

							leftOverCapacityOverall += remainingCap;
						}

						if (!takeZeroAsAppr && leftOverCapacityOverall == 0) {
							sbO.append(t + "," + cap + "," + (value - 0.0));
							sbO.append("\n");
						} else {

							output = annToUse.computeOutputs(inputRow);

							
							double value2 = output[0];
							if(ann.isUseHyperbolicTangens()){
								value2=(value2+1.0)/2.0;
							}
							value2=value2 * (ann.getMaximumValue() + ann.getMinimumValue()) - ann.getMinimumValue();
							sbO.append(t + "," + cap + "," + (value - value2));
							sbO.append("\n");
						}
						
						//Opportunity costs for other capacity change

						for (int capOther = initialCapacityForFocusAreaCap[ValueFunctionAnalysis.popularOtherIndex]; capOther >= 0; capOther--) {
							inputRow = new double[numberOfInput];
							double[] inputRow2 = new double[numberOfInput];
							if(ann.isConsiderConstant()){
								inputRow[numberOfInput-1]=1.0;
								inputRow2[numberOfInput-1]=1.0;
							}
							currentId = 0;
							for (DemandSegmentWeight w : daSegmentWeightings.get(subArea).getWeights()) {
								inputRow[currentId] = expectedArrivalsDeliveryArea * w.getWeight()
										/ ann.getMaxValuePerElement()[currentId];
								inputRow2[currentId] = expectedArrivalsDeliveryArea * w.getWeight()
										/ ann.getMaxValuePerElement()[currentId];
								currentId++;
							}

					
							for (int j = 0; j < initialCapacityForFocusAreaCap.length; j++) {

								int remainingCap = initialCapacityForFocusAreaCap[j];
								if (j == popularOtherIndex)
									remainingCap = capOther;
								if (j == popularIndex)
									remainingCap = cap;
								inputRow[currentId] = remainingCap / (double) ann.getMaxValuePerElement()[currentId];
								if (j == popularIndex)
									remainingCap = cap-1;
								inputRow2[currentId] = remainingCap / (double) ann.getMaxValuePerElement()[currentId];

								currentId++;

							
							}

							

								output = annToUse.computeOutputs(inputRow);
								double[] output2 = annToUse.computeOutputs(inputRow2);

								
								
								double valueOther = output[0];
								if(ann.isUseHyperbolicTangens()){
									valueOther=(valueOther+1.0)/2.0;
								}
								valueOther=valueOther * (ann.getMaximumValue() + ann.getMinimumValue()) - ann.getMinimumValue(); 
								double valueOther2 = output2[0];
								if(ann.isUseHyperbolicTangens()){
									valueOther2=(valueOther2+1.0)/2.0;
								}
								valueOther2=valueOther2 * (ann.getMaximumValue() + ann.getMinimumValue()) - ann.getMinimumValue(); 
								
								sbOther.append(t + "," + cap + "," +capOther+"," + (valueOther - valueOther2));
								sbOther.append("\n");
								
							
						}
					}

				}
				
				
			}
		}
		pw = null;
		try {
			pw = new PrintWriter(new File("valueFunctionAnalysis_popularTwDevelopment.csv"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		pw.write(sb.toString());
		pw.close();

		pw = null;
		try {
			pw = new PrintWriter(new File("valueFunctionAnalysis_popularTwOpportunityCosts.csv"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		pw.write(sbO.toString());
		pw.close();
		
		pw = null;
		try {
			pw = new PrintWriter(new File("valueFunctionAnalysis_popularTwOtherDevelopment.csv"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		pw.write(sbOther.toString());
		pw.close();

		sb = new StringBuilder();
		sbO = new StringBuilder();
		sbOther = new StringBuilder();
		sb.append("t,cap,value");
		sb.append("\n");
		sbO.append("t,cap,value");
		sbO.append("\n");
		sbOther.append("t,cap,capOther,value");
		sbOther.append("\n");
		for (int i = 0; i < timeStepsCap.length; i++) {
			int t = timeStepsCap[i];

			for (int cap = initialCapacityForFocusAreaCap[unPopularIndex]; cap >= 0; cap--) {

				double[] inputRow = new double[numberOfInput];

				double expectedArrivalsDeliveryArea = t * arrivalProbability * daWeights.get(subArea);

				int currentId = 0;
				if(ann.isConsiderConstant()) inputRow[numberOfInput-1]=1.0;
				for (DemandSegmentWeight w : daSegmentWeightings.get(subArea).getWeights()) {
					inputRow[currentId] = expectedArrivalsDeliveryArea * w.getWeight()
							/ ann.getMaxValuePerElement()[currentId];
					currentId++;
				}

				int leftOverCapacityOverall = 0;
				for (int j = 0; j < initialCapacityForFocusAreaCap.length; j++) {

					int remainingCap = initialCapacityForFocusAreaCap[j];
					if (j == unPopularIndex)
						remainingCap = cap;

					inputRow[currentId] = remainingCap / (double) ann.getMaxValuePerElement()[currentId];

					currentId++;

					leftOverCapacityOverall += remainingCap;
				}

				if (leftOverCapacityOverall == 0) {
					sb.append(t + "," + cap + "," + 0.0);
					sb.append("\n");
				} else {

					double[] output = annToUse.computeOutputs(inputRow);

					
					
					double value = output[0];
					if(ann.isUseHyperbolicTangens()){
						value=(value+1.0)/2.0;
					}
					value=value * (ann.getMaximumValue() + ann.getMinimumValue()) - ann.getMinimumValue();
					sb.append(t + "," + cap + "," + value);
					sb.append("\n");
					
					if (cap > 0) {
						inputRow = new double[numberOfInput];

						currentId = 0;
						if(ann.isConsiderConstant()) inputRow[numberOfInput-1]=1.0;
						for (DemandSegmentWeight w : daSegmentWeightings.get(subArea).getWeights()) {
							inputRow[currentId] = expectedArrivalsDeliveryArea * w.getWeight()
									/ ann.getMaxValuePerElement()[currentId];
							currentId++;
						}

						leftOverCapacityOverall = 0;
						for (int j = 0; j < initialCapacityForFocusAreaCap.length; j++) {

							int remainingCap = initialCapacityForFocusAreaCap[j];
							if (j == unPopularIndex)
								remainingCap = cap - 1;

							inputRow[currentId] = remainingCap / (double) ann.getMaxValuePerElement()[currentId];

							currentId++;

							leftOverCapacityOverall += remainingCap;
						}

						if (leftOverCapacityOverall == 0) {
							sbO.append(t + "," + cap + "," + (value - 0.0));
							sbO.append("\n");
						} else {

							output = annToUse.computeOutputs(inputRow);

							
							double value2 = output[0];
							if(ann.isUseHyperbolicTangens()){
								value2=(value2+1.0)/2.0;
							}
							value2=value2 * (ann.getMaximumValue() + ann.getMinimumValue()) - ann.getMinimumValue();
							sbO.append(t + "," + cap + "," + (value - value2));
							sbO.append("\n");
						}
						
						//Opportunity costs for other capacity change
						for (int capOther = initialCapacityForFocusAreaCap[ValueFunctionAnalysis.unPopularOtherIndex]; capOther >= 0; capOther--) {
							inputRow = new double[numberOfInput];
							double[] inputRow2 = new double[numberOfInput];
							if(ann.isConsiderConstant()){
								inputRow[numberOfInput-1]=1.0;
								inputRow2[numberOfInput-1]=1.0;
							}
							currentId = 0;
							for (DemandSegmentWeight w : daSegmentWeightings.get(subArea).getWeights()) {
								inputRow[currentId] = expectedArrivalsDeliveryArea * w.getWeight()
										/ ann.getMaxValuePerElement()[currentId];
								inputRow2[currentId] = expectedArrivalsDeliveryArea * w.getWeight()
										/ ann.getMaxValuePerElement()[currentId];
								currentId++;
							}

							
							for (int j = 0; j < initialCapacityForFocusAreaCap.length; j++) {

								int remainingCap = initialCapacityForFocusAreaCap[j];
								if (j == unPopularOtherIndex)
									remainingCap = capOther;
								if (j == unPopularIndex)
									remainingCap = cap;
								inputRow[currentId] = remainingCap / (double) ann.getMaxValuePerElement()[currentId];
								if (j == unPopularIndex)
									remainingCap = cap-1;
								inputRow2[currentId] = remainingCap / (double) ann.getMaxValuePerElement()[currentId];

								currentId++;

								
							}

							

								output = annToUse.computeOutputs(inputRow);
								double[] output2 = annToUse.computeOutputs(inputRow2);

								
								
								double valueOther = output[0];
								if(ann.isUseHyperbolicTangens()){
									valueOther=(valueOther+1.0)/2.0;
								}
								valueOther=valueOther * (ann.getMaximumValue() + ann.getMinimumValue()) - ann.getMinimumValue();
								
								
								double valueOther2 = output2[0];
								if(ann.isUseHyperbolicTangens()){
									valueOther2=(valueOther2+1.0)/2.0;
								}
								valueOther2=valueOther2 * (ann.getMaximumValue() + ann.getMinimumValue()) - ann.getMinimumValue();
								sbOther.append(t + "," + cap + "," +capOther+"," + (valueOther - valueOther2));
								sbOther.append("\n");
								
							
						}
					}
				}

			}
		}

		pw = null;
		try {
			pw = new PrintWriter(new File("valueFunctionAnalysis_unPopularTwDevelopment.csv"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		pw.write(sb.toString());
		pw.close();
		
		pw = null;
		try {
			pw = new PrintWriter(new File("valueFunctionAnalysis_unPopularTwOpportunityCosts.csv"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		pw.write(sbO.toString());
		pw.close();
		
		pw = null;
		try {
			pw = new PrintWriter(new File("valueFunctionAnalysis_unPopularTwOtherDevelopment.csv"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		pw.write(sbOther.toString());
		pw.close();
	}
}
