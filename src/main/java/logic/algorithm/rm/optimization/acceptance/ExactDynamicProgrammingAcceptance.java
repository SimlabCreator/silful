package logic.algorithm.rm.optimization.acceptance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import data.entity.Alternative;
import data.entity.AlternativeOffer;
import data.entity.CapacitySet;
import data.entity.DeliveryArea;
import data.entity.DemandSegmentSet;
import data.entity.DynamicProgrammingTree;
import data.entity.Entity;
import data.entity.ObjectiveWeight;
import data.entity.Order;
import data.entity.OrderRequest;
import data.entity.OrderRequestSet;
import data.entity.OrderSet;
import logic.entity.AlternativeCapacity;
import logic.service.support.AcceptanceService;
import logic.service.support.CapacityService;
import logic.service.support.CustomerDemandService;
import logic.service.support.LocationService;
import logic.utility.MultiDimensionalArrayProducer;
import logic.utility.SubsetProducer;
import logic.utility.comparator.CapacityAlternativeStartAscComparator;
import logic.utility.comparator.OrderRequestArrivalTimeDescComparator;

public class ExactDynamicProgrammingAcceptance implements AcceptanceAlgorithm {

	private OrderSet orderSet;
	private DynamicProgrammingTree tree;
	private int bookingHorizonLength;
	private DemandSegmentSet demandSegmentSet;
	private CapacitySet capacitySet;
	private OrderRequestSet orderRequestSet;
	private HashMap<DeliveryArea, ArrayList<AlternativeCapacity>> daCapacities;
	private HashMap<DeliveryArea, int[]> currentDimensions;
	private HashMap<DeliveryArea, Object[]> treesPerDeliveryArea;
	private HashMap<Entity, Object> objectiveSpecificValues;
	private Double maximumRevenueValue;

	public ExactDynamicProgrammingAcceptance(DynamicProgrammingTree tree, int bookingHorizonLength,
			DemandSegmentSet demandSegmentSet, CapacitySet capacitySet, OrderRequestSet orderRequestSet,
			HashMap<Entity, Object> objectiveSpecificValues, Double maximumRevenueValue) {
		this.bookingHorizonLength = bookingHorizonLength;
		this.demandSegmentSet = demandSegmentSet;
		this.capacitySet = capacitySet;
		this.orderRequestSet = orderRequestSet;
		this.treesPerDeliveryArea = new HashMap<DeliveryArea, Object[]>();
		this.currentDimensions = new HashMap<DeliveryArea, int[]>();
		this.tree = tree;
		this.objectiveSpecificValues = objectiveSpecificValues;
		this.maximumRevenueValue = maximumRevenueValue;
	};

	public void start() {

		// Read the dynamic programming tree to get controls
		this.prepareTrees();

		
		// Let requests arrive, decide for offer set based on pre-calculated
		// subsolutions, and add to orders
		orderSet = new OrderSet();
		orderSet.setOrderRequestSetId(this.orderRequestSet.getId());
		orderSet.setOrderRequestSet(this.orderRequestSet);

		ArrayList<Order> orders = new ArrayList<Order>();

		// Sort order requests (descending arrival time)
		ArrayList<OrderRequest> orderRequests = this.orderRequestSet.getElements();
		Collections.sort(orderRequests, new OrderRequestArrivalTimeDescComparator());

		// Go through order requests and produce orders
		for (int requestIndex = 0; requestIndex < orderRequests.size(); requestIndex++) {

			OrderRequest request =  orderRequests.get(requestIndex);
			// Set appropriate t for future value
			Iterator<DeliveryArea> it = this.currentDimensions.keySet().iterator();
			while (it.hasNext()) {
				this.currentDimensions.get(it.next())[0] = request.getArrivalTime() - 1;
			}

			// Initialize new order
			Order order = new Order();
			order.setOrderRequestId(request.getId());
			order.setOrderRequest(request);
			

			// Identify delivery area of customer location
			DeliveryArea area = LocationService.assignCustomerToDeliveryArea(this.capacitySet.getDeliveryAreaSet(),
					request.getCustomer());
			order.setAssignedDeliveryAreaId(area.getId());
		
			// Identify available alternatives
			ArrayList<Alternative> availableAlternatives = AcceptanceService
					.getAvailableAlternatives(this.daCapacities.get(area), this.currentDimensions.get(area));
			order.setAvailableAlternatives(availableAlternatives);

			if (availableAlternatives.size() > 0) {
				// Define offered alternatives
				ArrayList<AlternativeOffer> offeredAlternatives = this.defineOfferSet(order, area);
				order.setOfferedAlternatives(offeredAlternatives);

				// Determine demand
				if (offeredAlternatives.size() > 0) { // If windows are offered

					// Sample selection from customer
					AlternativeOffer selectedAlt = CustomerDemandService.sampleCustomerDemand(offeredAlternatives,
							order);
					if (selectedAlt != null) {
						// Customer selected an offered alternative (did not
						// choose to purchase nothing)
						order.setAccepted(true);
						// Reduce respective capacity
						this.currentDimensions.get(area)[selectedAlt.getTempAlternativeNo() + 1] -= 1;

					} else {
						order.setAccepted(false);
						order.setReasonRejection("Customer selected no-purchase option");
					}
				} else {
					// The customer can not choose anything because nothing was
					// offered
					order.setAccepted(false);
					order.setReasonRejection("Most valuable to not offer anything");
				}
			} else {
				// The customer can not choose anything because there are no
				// capacities left
				order.setAccepted(false);
				order.setReasonRejection("No capacity left for delivery area");
			}

			orders.add(order);

		}

		orderSet.setElements(orders);
	}

	private void prepareTrees() {

		// Find out the dimensions of the trees by identifying the capacities
		// per alternative

		ArrayList<AlternativeCapacity> capacities = CapacityService.getCapacitiesPerAlternative(capacitySet,
				this.demandSegmentSet.getAlternativeSet());
	

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

			String treeString = "";
			Iterator<Integer> it2 = this.tree.getTrees().keySet().iterator();

			while (it2.hasNext()) {
				Integer daid = it2.next();

				if (daid == currentDa.getId()) {
					treeString = this.tree.getTrees().get(daid);
				}
			}
			/// Read the tree from the string
			this.treesPerDeliveryArea.put(currentDa,
					MultiDimensionalArrayProducer.stringToDoubleArray(treeString, arraySizes));

			/// Define maximum values (maximum T, overall capacities)
			int[] dimensions = new int[1 + this.daCapacities.get(currentDa).size()];

			dimensions[0] = this.bookingHorizonLength;
			for (int capID = 1; capID < dimensions.length; capID++) {
				dimensions[capID] = this.daCapacities.get(currentDa).get(capID - 1).getCapacityNumber();

			}

			// Set starting dimensions
			this.currentDimensions.put(currentDa, dimensions);

		}

	}

	/**
	 * Determines the best offer set for a specific order request
	 * 
	 * @param order
	 *            Respective order details
	 * @param area
	 *            Respective delivery area
	 * @return List of alternatives to offer
	 */
	private ArrayList<AlternativeOffer> defineOfferSet(Order order, DeliveryArea area) {

		// No purchase value
		// int[] dimensionsReducedT = this.currentDimensions.get(area).clone();
		// dimensionsReducedT[0] -= 1;
		Double valueNoPurchase = MultiDimensionalArrayProducer.readDoubleArray(this.treesPerDeliveryArea.get(area),
				this.currentDimensions.get(area));

		//System.out.println("value of no purchase is"+valueNoPurchase);
		// Determine available alternatives set
		Set<Integer> available = new HashSet<Integer>();
		for (int capID = 1; capID < this.currentDimensions.get(area).length; capID++) {
			if (this.currentDimensions.get(area)[capID] > 0) {
				available.add(capID - 1);
			}
		}

		// The current maximum value is to not offer anything and directly go to
		// the next t (no-purchase prob = 1)
		Double maxValue = valueNoPurchase;
		ArrayList<AlternativeOffer> bestSet = new ArrayList<AlternativeOffer>();

		for (Set<Integer> s : SubsetProducer.powerSet(available)) {
			if (s.size() > 0) { // Only look at non-empty subsets

				// Determine selection probabilities for the alternatives of the
				// respective subset
				ArrayList<AlternativeOffer> offeredAlternatives = new ArrayList<AlternativeOffer>();
				for (Integer index : s) {
					AlternativeOffer offer = new AlternativeOffer();
					offer.setAlternative(this.daCapacities.get(area).get(index).getAlternative());
					offer.setAlternativeId(offer.getAlternative().getId());
					offer.setTempAlternativeNo(index);
					offeredAlternatives.add(offer);
				}

				HashMap<AlternativeOffer, Double> probs = CustomerDemandService.getProbabilitiesForModel("MNL_constant",
						offeredAlternatives, order.getOrderRequest().getCustomer().getOriginalDemandSegment());

				// Determine purchase-value
				Double currentValue = 0.0;
				double noPurchaseProb = 1.0;
				Iterator<AlternativeOffer> it = probs.keySet().iterator();
				while (it.hasNext()) {

					AlternativeOffer currentAlt = it.next();
					if (currentAlt != null) {
						Double prob = probs.get(currentAlt);
						Double revenue = order.getOrderRequest().getBasketValue() / this.maximumRevenueValue; // Use
						// actual
						// value

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
									weightedValue += ow.getValue() * order.getOrderRequest().getCustomer()
											.getOriginalDemandSegment().getSocialImpactFactor();
								}else if(ow.getObjectiveType().getName().equals("local_visibility_factor")){
//									//The specific values are an object array
//									Object[] objectiveValues = (Object[]) this.objectiveSpecificValues.get(obj);
//									//The first entry are the local visibility values
//									@SuppressWarnings("unchecked")
//									HashMap<DeliveryArea, HashMap<Alternative, Double>> localVisibilityValues = (HashMap<DeliveryArea, HashMap<Alternative, Double>>) objectiveValues[0];
//									//The second entry is the maximum value
//									Double maximumLocalVisibility = objectiveValues[1];
//									weightedValue+=ow.getValue() * (maximumLocalVisibility-localVisibilityValues.get(area).get(currentAlt.getAlternative()))/maximumLocalVisibility;
//
								}
							}

						}
						weightedValue += weightRevenue * revenue;
						order.setAssignedValue(weightedValue);

						Double probValue = prob * weightedValue; // Value now
						int[] resultingDimensions = this.currentDimensions.get(area).clone();
						resultingDimensions[currentAlt.getTempAlternativeNo() + 1] -= 1;

						probValue += MultiDimensionalArrayProducer.readDoubleArray(this.treesPerDeliveryArea.get(area),
								resultingDimensions) * prob; // Future value

						currentValue += probValue;
						noPurchaseProb -= prob;
					}
				}
				// Determine no-purchase value
				currentValue += valueNoPurchase * noPurchaseProb;

				// Update maximum value
				if (currentValue > maxValue) {
					maxValue = currentValue;
					bestSet = offeredAlternatives;

				}
			}

		}
	//	System.out.println("Max value is"+maxValue);
		return bestSet;
	};
	
	public OrderSet getResult() {

		return orderSet;
	}

}
