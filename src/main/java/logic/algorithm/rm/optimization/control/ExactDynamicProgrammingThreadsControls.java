package logic.algorithm.rm.optimization.control;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import data.entity.Alternative;
import data.entity.AlternativeOffer;
import data.entity.ArrivalProcess;
import data.entity.CapacitySet;
import data.entity.DeliveryArea;
import data.entity.DemandSegment;
import data.entity.DemandSegmentSet;
import data.entity.DemandSegmentWeighting;
import data.entity.DynamicProgrammingTree;
import data.entity.Entity;
import data.entity.ObjectiveWeight;
import data.utility.DataServiceProvider;
import logic.entity.AlternativeCapacity;
import logic.service.support.ArrivalProcessService;
import logic.service.support.CapacityService;
import logic.service.support.CustomerDemandService;
import logic.service.support.ProbabilityDistributionService;
import logic.utility.MultiDimensionalArrayProducer;
import logic.utility.SubsetProducer;
import logic.utility.comparator.CapacityAlternativeStartAscComparator;
import logic.utility.exceptions.ParameterUnknownException;

public class ExactDynamicProgrammingThreadsControls implements DynamicProgrammingAlgorithm {

	private int numberOfThreads = 3; // TODO: Define centrally as default option.
	private int bookingHorizonLength;
	private int arrivalProcessId;
	private DemandSegmentWeighting overallDemandSegmentWeighting;
	private HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightings;
	private HashMap<DeliveryArea, Double> daWeights;
	private HashMap<Entity, Object> objectiveSpecificValues;
	private CapacitySet capacitySet;
	private HashMap<DeliveryArea, ArrayList<AlternativeCapacity>> daCapacities;
	// private HashMap<DeliveryArea, int[]> currentDimensions;
	private HashMap<DeliveryArea, Object[]> treesPerDeliveryArea;
	private DynamicProgrammingTree tree;
	private Double maximumRevenueValue;

	public ExactDynamicProgrammingThreadsControls(int bookingHorizonLength, int arrivalProcessId,
			DemandSegmentWeighting overallDemandSegmentWeighting, HashMap<DeliveryArea, Double> daWeights,
			HashMap<DeliveryArea, DemandSegmentWeighting> daSegmentWeightings, CapacitySet capacitySet,
			HashMap<Entity, Object> objectiveSpecificValues, Double maximumRevenueValue) {
		this.bookingHorizonLength = bookingHorizonLength;
		this.arrivalProcessId = arrivalProcessId;
		this.overallDemandSegmentWeighting = overallDemandSegmentWeighting;
		this.daSegmentWeightings = daSegmentWeightings;
		this.daWeights = daWeights;
		this.capacitySet = capacitySet;
		this.treesPerDeliveryArea = new HashMap<DeliveryArea, Object[]>();
		this.objectiveSpecificValues = objectiveSpecificValues;
		this.maximumRevenueValue = maximumRevenueValue;
	};

	public void start() {

		// Prepare tree buffers for the dynamic programming
		this.prepareTreeBuffers();

		System.out.println("Prepared buffers");

		// Recursively solve all subproblems to fill the buffers (per delivery
		// area). Afterwards,
		// they can be used to select the offer set once a request arrives.
		try {
			this.solvePossibleSubProblems();
		} catch (ParameterUnknownException e) {
			e.printStackTrace();
			System.exit(0);
		}
		System.out.println("Solved subproblems");

		// Put into final format
		tree = new DynamicProgrammingTree();
		tree.setArrivalProcessId(this.arrivalProcessId);
		tree.setCapacitySetId(this.capacitySet.getId());
		tree.setDeliveryAreaSetId(this.capacitySet.getDeliveryAreaSetId());
		tree.setCapacitySetId(this.capacitySet.getId());
		tree.setT(this.bookingHorizonLength);
		tree.setDemandSegmentWeightingId(this.overallDemandSegmentWeighting.getId());

		HashMap<Integer, String> trees = new HashMap<Integer, String>();
		Iterator<DeliveryArea> it = this.treesPerDeliveryArea.keySet().iterator();
		while (it.hasNext()) {
			DeliveryArea area = it.next();
			trees.put(area.getId(), MultiDimensionalArrayProducer.arrayToString(this.treesPerDeliveryArea.get(area)));

		}
		tree.setTrees(trees);

	}

	/**
	 * Builds the buffers that save the optimal solutions of subproblems (per
	 * delivery area)
	 */
	private void prepareTreeBuffers() {
		// As alternatives are offered, the capacities need to be transfered to
		// capacities per alternative
		DemandSegmentSet dsSet = (DemandSegmentSet) this.overallDemandSegmentWeighting.getSetEntity();
		ArrayList<AlternativeCapacity> capacities = CapacityService.getCapacitiesPerAlternative(capacitySet,
				dsSet.getAlternativeSet());

		// Per delivery area, a tree buffer is created to save the subproblem
		// solutions of the dynamic program
		this.daCapacities = CapacityService.getAlternativeCapacitiesPerDeliveryArea(capacities);
		Iterator<DeliveryArea> it = daCapacities.keySet().iterator();
		while (it.hasNext()) {// Go through delivery areas

			DeliveryArea currentDa = it.next();

			// Take relevant capacities of the delivery area and sort them
			Collections.sort(daCapacities.get(currentDa), new CapacityAlternativeStartAscComparator());

			/// The number of dimensions for the tree is 1 (for the
			/// bookingHorizonLength) + the number of alternatives available
			int[] arraySizes = new int[1 + daCapacities.get(currentDa).size()];
			arraySizes[0] = this.bookingHorizonLength + 1; // First dimension:
															// T. Needs to be
															// one higher
															// because can be 0
			for (int i = 1; i < arraySizes.length; i++) { // Flexible number of
															// further
															// dimensions based
															// on number of
															// alternative

				// The number of entries depends on the capacities maximally
				// available (+1 because capacity can be 0)
				arraySizes[i] = ((AlternativeCapacity) daCapacities.get(currentDa).get(i - 1)).getCapacityNumber() + 1;
			}

			/// Build the buffer
			this.treesPerDeliveryArea.put(currentDa, MultiDimensionalArrayProducer.createDoubleArray(arraySizes));

		}
	}

	/**
	 * @throws ParameterUnknownException 
	 * 
	 */
	private void solvePossibleSubProblems() throws ParameterUnknownException {

		// Go through all delivery areas
		Iterator<DeliveryArea> it = this.treesPerDeliveryArea.keySet().iterator();
		while (it.hasNext()) {

			//If there are more delivery areas, produce at most as many threads as defined in setting
			ArrayList<Thread> threads = new ArrayList<Thread>();
			ArrayList<int[]> dimensionsList = new ArrayList<int[]>();
			ArrayList<DeliveryArea> areas = new ArrayList<DeliveryArea>();
			ArrayList<DemandSegmentWeighting> weightingsList = new ArrayList<DemandSegmentWeighting>();
			for (int i = 0; i < this.numberOfThreads; i++) {

				if (it.hasNext()) { //Only build thread if actually needed

					DeliveryArea currentArea = it.next();
					areas.add(currentArea);
					/// Define maximum values (maximum T, overall capacities)
					/// and
					/// fill tree by getting the value for them
					int[] dimensions = new int[1 + this.daCapacities.get(currentArea).size()];
					dimensions[0] = this.bookingHorizonLength;
					for (int capID = 1; capID < dimensions.length; capID++) {
						dimensions[capID] = this.daCapacities.get(currentArea).get(capID - 1).getCapacityNumber();

					}
					dimensionsList.add(dimensions.clone());
					weightingsList.add(this.daSegmentWeightings.get(currentArea));
					System.out.println("Start with delivery area " + currentArea.getId());
					Thread thread = new Thread(new DynamicProgrammingSubProblemRunnable(currentArea, dimensions,
							treesPerDeliveryArea.get(currentArea), this.daSegmentWeightings.get(currentArea),
							bookingHorizonLength, this.daWeights.get(currentArea), arrivalProcessId,
							this.daCapacities.get(currentArea), objectiveSpecificValues, maximumRevenueValue));
					threads.add(thread);
					thread.start();
				}
			}
			for(Thread thread:threads){
				try {
					thread.join();
					
				} catch (InterruptedException e) {
					e.printStackTrace();
					System.exit(0);
				}
			}
			
			//Save tree in txt
			for(int areaID=0; areaID < areas.size(); areaID++){
				String tree = MultiDimensionalArrayProducer.arrayToString(this.treesPerDeliveryArea.get(areas.get(areaID)));
				PrintWriter out;
				try {
					out = new PrintWriter("area"+areas.get(areaID).getId()+"tree.txt");
					out.println(tree);
					out.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					System.out.println("Could not save in file.");
					e.printStackTrace();
				}
				
			}
			
			
			Double value = this.getValue(dimensionsList.get(0), weightingsList.get(0), areas.get(0));
			System.out.println("Overall values for area:" + areas.get(0).getId() + " is 1: " + value);
		if(dimensionsList.size()>1)	{
			Double value2 = this.getValue(dimensionsList.get(1), weightingsList.get(1), areas.get(1));
			System.out.println("Overall values for area:" + areas.get(1).getId() + " is 1: " + value2);
		}


		}

	}

	/**
	 * Provides the value of the subproblem defined by t and left-over
	 * capacities which are all saved in a dimensions-array
	 * 
	 * @param dimensions
	 *            Respective t and capacity values
	 * @param weighting
	 *            Weighting of respective customer segments
	 * @param currentDa
	 *            Delivery area
	 * @return
	 * @throws ParameterUnknownException 
	 */
	private Double getValue(int[] dimensions, DemandSegmentWeighting weighting, DeliveryArea currentDa) throws ParameterUnknownException {

		Double value =  0.0;


		// Constraints
		/// Booking horizon over?
		if (dimensions[0] == 0) {
			MultiDimensionalArrayProducer.writeToDoubleArray(value, this.treesPerDeliveryArea.get(currentDa), dimensions);

			return value;
		}
		/// No capacities left?
		int sumOfCapacities = 0;
		for (int capID = 1; capID < dimensions.length; capID++) {
			sumOfCapacities += dimensions[capID];
		}
		if (sumOfCapacities == 0) {
			MultiDimensionalArrayProducer.writeToDoubleArray(value, this.treesPerDeliveryArea.get(currentDa), dimensions);
			return value;
		}

		// Already calculated and saved in the buffer?
		Double subValue = MultiDimensionalArrayProducer.readDoubleArray(this.treesPerDeliveryArea.get(currentDa), dimensions);

		if (subValue != null) {
			return subValue;
		}

		// Determine

		double noArrivalValue;// No request in t
		double arrivalValue;// Request in t

		ArrivalProcess arrivalProcess = (ArrivalProcess) DataServiceProvider.getArrivalProcessDataServiceImplInstance().getById(arrivalProcessId);
		double arrivalProbability = (ArrivalProcessService.getArrivalProbability(
				dimensions[0],arrivalProcess)) * this.daWeights.get(currentDa);
	

		// If no request arrives, the value for t-1 is needed
		int[] dimensionsReducedT = dimensions.clone();
		dimensionsReducedT[0] -= 1;
		noArrivalValue = (1 - arrivalProbability) * this.getValue(dimensionsReducedT, weighting, currentDa);

		// If a request arrives, the value of all segments needs to be combined
		// in a weighted sum
		arrivalValue = 0.0;
		for (int segmentID = 0; segmentID < weighting.getWeights().size(); segmentID++) {

			arrivalValue += arrivalProbability * weighting.getWeights().get(segmentID).getWeight()
					* this.getValueForSegment(segmentID, dimensions, weighting, currentDa);

		}

		// The value of the respective t and capacities is the sum of no arrival
		// and arrival value
		value = noArrivalValue + arrivalValue;

		MultiDimensionalArrayProducer.writeToDoubleArray(value, this.treesPerDeliveryArea.get(currentDa), dimensions);
		return value;
	}

	private Double getValueForSegment(int segmentId, int[] dimensions, DemandSegmentWeighting weighting,
			DeliveryArea currentDa) throws ParameterUnknownException {

		// No purchase value
		int[] dimensionsReducedT = dimensions.clone();
		dimensionsReducedT[0] -= 1;
		Double valueNoPurchase = this.getValue(dimensionsReducedT, weighting, currentDa);

		// Determine available alternatives and their future value with reduced
		// capacity and t
		Set<Integer> available = new HashSet<Integer>();
		HashMap<Alternative, Double> leftOverValues = new HashMap<Alternative, Double>();
		for (int capID = 1; capID < dimensionsReducedT.length; capID++) {
			if (dimensionsReducedT[capID] > 0) {// Capacity left
				int[] dimensionsReduced = dimensionsReducedT.clone();
				dimensionsReduced[capID] -= 1;

				Double leftOverValue = this.getValue(dimensionsReduced, weighting, currentDa);

				leftOverValues.put(this.daCapacities.get(currentDa).get(capID - 1).getAlternative(), leftOverValue);
				available.add(capID - 1); // From the capacity set, it is one
											// before
			}
		}

		// The current maximum value is to not offer anything and directly go to
		// the next t (no-purchase prob = 1)
		Double maxValue = valueNoPurchase;

		for (Set<Integer> s : SubsetProducer.powerSet(available)) {
			if (s.size() > 0) { // Only look at non-empty subsets

				// Determine selection probabilities for the alternatives of the
				// respective subset
				ArrayList<AlternativeOffer> offeredAlternatives = new ArrayList<AlternativeOffer>();
				for (Integer index : s) {
					AlternativeOffer offer = new AlternativeOffer();
					offer.setAlternative(this.daCapacities.get(currentDa).get(index).getAlternative());
					offer.setAlternativeId(offer.getAlternative().getId());
					offeredAlternatives.add(offer);
				}

				DemandSegment segment = weighting.getWeights().get(segmentId)
						.getDemandSegment();
				HashMap<AlternativeOffer, Double> probs = CustomerDemandService.getProbabilitiesForModel("MNL_constant",
						offeredAlternatives, segment);

				// Determine purchase-value
				Double currentValue =  0.0;
				double noPurchaseProb =  1.0;
				Iterator<AlternativeOffer> it = probs.keySet().iterator();
				while (it.hasNext()) {
					AlternativeOffer currentAlt = it.next();
					if (currentAlt != null) {

						Double prob = probs.get(currentAlt);

						Double meanRevenue = ProbabilityDistributionService.getMeanByProbabilityDistribution(
								segment.getBasketValueDistribution()) / this.maximumRevenueValue;

						// If there are additional objectives, revenue needs to
						// be weighted and the other objective values need to be
						// added
						Double weightedValue = 0.0;
						Double weightRevenue = 1.0;
						if (this.objectiveSpecificValues.keySet().size() > 0) {

							for (Entity obj : this.objectiveSpecificValues.keySet()) {
								ObjectiveWeight ow = (ObjectiveWeight) obj;
								
								if (ow.getObjectiveType().getName().equals("social_impact_factor")) {
									weightRevenue -= ow.getValue();
									weightedValue += ow.getValue() * segment.getSocialImpactFactor();

								} else if (ow.getObjectiveType().getName().equals("local_visibility_factor")) {
//									// The specific values are an object array
									Object[] objectiveValues = (Object[]) this.objectiveSpecificValues.get(obj);
//									// The first entry are the local visibility
//									// values
//									@SuppressWarnings("unchecked")
									HashMap<DeliveryArea, HashMap<Alternative, Double>> localVisibilityValues = (HashMap<DeliveryArea, HashMap<Alternative, Double>>) objectiveValues[0];
//									// The second entry is the maximum value
									Double maximumLocalVisibility = (Double) objectiveValues[1];
									weightedValue += ow.getValue() * (maximumLocalVisibility
											- localVisibilityValues.get(currentDa).get(currentAlt.getAlternative()))
											/ maximumLocalVisibility;
								}
							}

						}
						weightedValue += weightRevenue * meanRevenue;

						Double probValue = prob * weightedValue; // Value now

						probValue += leftOverValues.get(currentAlt.getAlternative()) * prob; // Future
																								// value

						currentValue += probValue;
						noPurchaseProb -= prob;
					}

				}
				// Determine no-purchase value
				currentValue += valueNoPurchase * noPurchaseProb;

				// Update maximum value
				if (currentValue > maxValue) {
					maxValue = currentValue;

				}
			}

		}

		return maxValue;
	}

	public DynamicProgrammingTree getResult() {

		return tree;
	}

}
