package logic.service.support;

import java.util.ArrayList;
import java.util.HashMap;

import data.entity.Customer;
import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.DemandSegment;
import data.entity.DemandSegmentSet;
import data.entity.DemandSegmentWeight;
import data.entity.DemandSegmentWeighting;
import data.entity.Node;
import data.entity.NodeDistance;
import data.entity.Region;
import data.entity.ResidenceArea;
import data.entity.ResidenceAreaWeight;
import data.entity.ResidenceAreaWeighting;
import logic.utility.exceptions.ProbabilitiesDoNotSumUpToOneException;

/**
 * Provides functionality relating to customer locations and nodes
 * 
 * @author M. Lang
 *
 */
public class LocationService {

	/**
	 * Provides a random location based on the residence area weighting (of a
	 * demand segment)
	 * 
	 * @param residenceAreaWeighting
	 *            Respective residence area weighting
	 * @return Arraylist with lat as first and lon as second entry
	 * @throws Exception
	 */
	public static ArrayList<Double> getRandomLocationByResidenceAreaWeighting(
			ResidenceAreaWeighting residenceAreaWeighting) throws ProbabilitiesDoNotSumUpToOneException {

		// Select a random weight
		ResidenceAreaWeight randomWeight = (ResidenceAreaWeight) ProbabilityDistributionService
				.getRandomWeightByWeighting(residenceAreaWeighting);

		// Get a random location within the respective residence area
		ResidenceArea residenceArea = randomWeight.getResidenceArea();

		return getUniformRandomLocationInResidenceArea(residenceArea);
	}

	public static ArrayList<Double> getUniformRandomLocationInResidenceArea(ResidenceArea area) {
		double lat;
		if (area.getLat1() < area.getLat2()) {
			lat = ProbabilityDistributionService.getUniformRandomNumber(area.getLat1(), area.getLat2());
		} else {
			lat = ProbabilityDistributionService.getUniformRandomNumber(area.getLat2(), area.getLat1());
		}

		double lon;

		if (area.getLon1() < area.getLon2()) {
			lon = ProbabilityDistributionService.getUniformRandomNumber(area.getLon1(), area.getLon2());
		} else {
			lon = ProbabilityDistributionService.getUniformRandomNumber(area.getLon2(), area.getLon1());
		}

		ArrayList<Double> position = new ArrayList<Double>();
		position.add(lat);
		position.add(lon);

		return position;
	}

	/**
	 * Returns an allocation of neighbor delivery areas (8) for every delivery
	 * area
	 * 
	 * @param deliveryAreaSet
	 * @return
	 */
	public static HashMap<DeliveryArea, ArrayList<DeliveryArea>> getNeighborDeliveryAreasForDeliveryAreaSetWithSameSizeAreasConsideringHierarchy(
			DeliveryAreaSet deliveryAreaSet) {

		if(deliveryAreaSet.getElements().get(0).getSubset()!=null){
			HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors = new HashMap<DeliveryArea, ArrayList<DeliveryArea>>();
			for(DeliveryArea area: deliveryAreaSet.getElements()){
				neighbors.putAll(LocationService.getNeighborDeliveryAreasForDeliveryAreaSetWithSameSizeAreas(
					 area.getSubset()));
			}
			return neighbors;
		}else{
			return LocationService.getNeighborDeliveryAreasForDeliveryAreaSetWithSameSizeAreas(
					 deliveryAreaSet);
		}
		
	}
	
	/**
	 * Returns an allocation of neighbor delivery areas (8) for every delivery
	 * area
	 * 
	 * @param deliveryAreaSet
	 * @return
	 */
	public static HashMap<DeliveryArea, ArrayList<DeliveryArea>> getNeighborDeliveryAreasForDeliveryAreaSetWithSameSizeAreas(
			DeliveryAreaSet deliveryAreaSet) {
		HashMap<DeliveryArea, ArrayList<DeliveryArea>> neighbors = new HashMap<DeliveryArea, ArrayList<DeliveryArea>>();

		for (DeliveryArea area : deliveryAreaSet.getElements()) {

			
			neighbors.put(area, new ArrayList<DeliveryArea>());

			// Check in longitude direction
			double goalLat = area.getCenterLat();
			double goalLon1 = area.getCenterLon() + Math.abs(area.getLon1() - area.getLon2());
			double goalLon2 = area.getCenterLon() - Math.abs(area.getLon1() - area.getLon2());

			DeliveryArea areaLon1 = LocationService.assignLocationToDeliveryArea(deliveryAreaSet, goalLat, goalLon1);
			if (areaLon1 != null)
				neighbors.get(area).add(areaLon1);
			DeliveryArea areaLon2 = LocationService.assignLocationToDeliveryArea(deliveryAreaSet, goalLat, goalLon2);
			if (areaLon2 != null)
				neighbors.get(area).add(areaLon2);

			// Check in latitude direction
			double goalLon = area.getCenterLon();
			double goalLat1 = area.getCenterLat() + Math.abs(area.getLat1() - area.getLat2());
			double goalLat2 = area.getCenterLat() - Math.abs(area.getLat1() - area.getLat2());

			DeliveryArea areaLat1 = LocationService.assignLocationToDeliveryArea(deliveryAreaSet, goalLat1, goalLon);
			if (areaLat1 != null)
				neighbors.get(area).add(areaLat1);
			DeliveryArea areaLat2 = LocationService.assignLocationToDeliveryArea(deliveryAreaSet, goalLat2, goalLon);
			if (areaLat2 != null)
				neighbors.get(area).add(areaLat2);

			// Determine corner areas (following latitude direction areas)
			if (areaLat1 != null) {
				 goalLat = areaLat1.getCenterLat();
				 goalLon1 = area.getCenterLon() + Math.abs(areaLat1.getLon1() - areaLat1.getLon2());
				 goalLon2 = area.getCenterLon() - Math.abs(areaLat1.getLon1() - areaLat1.getLon2());

				areaLon1 = LocationService.assignLocationToDeliveryArea(deliveryAreaSet, goalLat, goalLon1);
				if (areaLon1 != null)
					neighbors.get(area).add(areaLon1);
				areaLon2 = LocationService.assignLocationToDeliveryArea(deliveryAreaSet, goalLat, goalLon2);
				if (areaLon2 != null)
					neighbors.get(area).add(areaLon2);
			}
			
			if (areaLat2 != null) {
				 goalLat = areaLat2.getCenterLat();
				 goalLon1 = area.getCenterLon() + Math.abs(areaLat2.getLon1() - areaLat2.getLon2());
				 goalLon2 = area.getCenterLon() - Math.abs(areaLat2.getLon1() - areaLat2.getLon2());

				areaLon1 = LocationService.assignLocationToDeliveryArea(deliveryAreaSet, goalLat, goalLon1);
				if (areaLon1 != null)
					neighbors.get(area).add(areaLon1);
				areaLon2 = LocationService.assignLocationToDeliveryArea(deliveryAreaSet, goalLat, goalLon2);
				if (areaLon2 != null)
					neighbors.get(area).add(areaLon2);
			}

		}

		return neighbors;
		
	}

	/**
	 * Determines delivery area of customer. Lower bound of area belongs to area
	 * (>=), upper bound does not(<) (only for outer region). Returns null, if
	 * customer does not belong to any.
	 * 
	 * @param deliveryAreaSet
	 *            Respective delivery area set
	 * @param customer
	 *            Respective customer
	 * @return Delivery area of customer
	 */
	public static DeliveryArea assignCustomerToDeliveryArea(DeliveryAreaSet deliveryAreaSet, Customer customer) {

		ArrayList<DeliveryArea> areas = deliveryAreaSet.getElements();

		double highestLat = Double.MAX_VALUE*(-1.0);
		double lowestLat = Double.MAX_VALUE;
		double highestLon = Double.MAX_VALUE*(-1.0);
		double lowestLon = Double.MAX_VALUE;
		for(DeliveryArea area: areas) {
			if(area.getLat1()> highestLat) highestLat=area.getLat1();
			if(area.getLat1()<lowestLat) lowestLat=area.getLat1();
			if(area.getLat2()> highestLat) highestLat=area.getLat2();
			if(area.getLat2()<lowestLat) lowestLat=area.getLat2();
			if(area.getLon1()> highestLon) highestLon=area.getLon1();
			if(area.getLon1()<lowestLon) lowestLon=area.getLon1();
			if(area.getLon2()> highestLon) highestLon=area.getLon2();
			if(area.getLon2()<lowestLon) lowestLon=area.getLon2();
			
		}
		return LocationService.assignCustomerToDeliveryArea(areas, customer,lowestLat, highestLat,
				 lowestLon, highestLon);
	}

	public static DeliveryArea assignCustomerToDeliveryArea(ArrayList<DeliveryArea> areas, Customer customer,
			double lat1, double lat2, double lon1, double lon2) {
		double tolerance = 0.0001;
		double highestLat = lat1;
		double lowestLat = lat2;
		if (lat2 > highestLat){
			highestLat = lat2;
			lowestLat=lat1;
		}
			
		double highestLon = lon1;
		double lowestLon = lon2;
		if (lon2 > highestLon){
			highestLon = lon2;
			lowestLon=lon1;
		}
			
		
		

		for (int areaIndex = 0; areaIndex < areas.size(); areaIndex++) {
			DeliveryArea area = areas.get(areaIndex);
			double higherLat = area.getLat2();
			double lowerLat = area.getLat2();
			if (higherLat < area.getLat1()) {
				higherLat = area.getLat1();
			} else {
				lowerLat = area.getLat1();
			}
			if ((lowerLat <= customer.getLat() && higherLat > customer.getLat())
					|| (lowerLat <= customer.getLat() && higherLat > highestLat-tolerance && customer.getLat() < higherLat+tolerance)
					|| (lowerLat < lowestLat+tolerance && customer.getLat() > lowerLat-tolerance && higherLat > customer.getLat())) {

				double higherLon = area.getLon2();
				double lowerLon = area.getLon2();
				if (higherLon < area.getLon1()) {
					higherLon = area.getLon1();
				} else {
					lowerLon = area.getLon1();
				}

				if ((lowerLon <= customer.getLon() && higherLon > customer.getLon())
						|| (lowerLon <= customer.getLon() && higherLon > highestLon-tolerance && customer.getLon() < higherLon+tolerance)
						|| (lowerLon < lowestLon+tolerance && customer.getLon() > lowerLon-tolerance && higherLon > customer.getLon())) {
					return area;
				}
			}
		}
		return null;
	}

	/**
	 * Determines delivery area of customer. Delivery area set can be a
	 * hierarchy. Lower bound of area belongs to area (>=), upper bound does
	 * not(<) (only for outer region). Returns null, if customer does not belong
	 * to any.
	 * 
	 * @param deliveryAreaSet
	 *            Respective delivery area set (can be hierarchy)
	 * @param customer
	 *            Respective customer
	 * @return Delivery area of customer
	 */
	// TODO: Consider using this everywhere
	public static DeliveryArea assignCustomerToDeliveryAreaConsideringHierarchy(DeliveryAreaSet deliveryAreaSet,
			Customer customer) {

		if (!deliveryAreaSet.isHierarchy()) {
			return LocationService.assignCustomerToDeliveryArea(deliveryAreaSet, customer);
		} else {
			for (DeliveryArea area : deliveryAreaSet.getElements()) {
				DeliveryArea subArea = LocationService.assignCustomerToDeliveryArea(area.getSubset(), customer);
				if (subArea != null)
					return subArea;
			}

			return null;
		}
	}

	/**
	 * Determines delivery area of a location. Lower bound of area belongs to
	 * area (>=), upper bound does not(<) (only for outer region). Returns null,
	 * if location does not belong to any.
	 * 
	 * @param deliveryAreaSet
	 *            Respective delivery area set
	 * @param lat
	 *            Respective latitude
	 * @param lon
	 *            Respective longitude
	 * @return Delivery area
	 */
	public static DeliveryArea assignLocationToDeliveryArea(DeliveryAreaSet deliveryAreaSet, double lat, double lon) {

		double tolerance = 0.0001;
		ArrayList<DeliveryArea> areas = deliveryAreaSet.getElements();

		Region region = deliveryAreaSet.getRegion();
		double highestLat = region.getLat1();
		if (region.getLat2() > highestLat)
			highestLat = region.getLat2();
		double highestLon = region.getLon1();
		if (region.getLon2() > highestLon)
			highestLon = region.getLon2();

		for (int areaIndex = 0; areaIndex < areas.size(); areaIndex++) {
			DeliveryArea area = areas.get(areaIndex);
			double higherLat = area.getLat2();
			double lowerLat = area.getLat2();
			if (higherLat < area.getLat1()) {
				higherLat = area.getLat1();
			} else {
				lowerLat = area.getLat1();
			}
			if ((lowerLat <= lat + tolerance && higherLat > lat - tolerance)
					|| (higherLat == highestLat && lat == higherLat)) {

				double higherLon = area.getLon2();
				double lowerLon = area.getLon2();
				if (higherLon < area.getLon1()) {
					higherLon = area.getLon1();
				} else {
					lowerLon = area.getLon1();
				}

				if ((lowerLon <= lon + tolerance && higherLon > lon - tolerance)
						|| (higherLon == highestLon && lon == higherLon)) {
					return area;
				}
			}
		}
		return null;
	}

	/**
	 * Determines delivery area of a location. Can be delivery area hierarchy.
	 * Lower bound of area belongs to area (>=), upper bound does not(<) (only
	 * for outer region). Returns null, if location does not belong to any.
	 * 
	 * @param deliveryAreaSet
	 *            Respective delivery area set (possibly hierarchy)
	 * @param lat
	 *            Respective latitude
	 * @param lon
	 *            Respective longitude
	 * @return Delivery area
	 */
	public static DeliveryArea assignLocationToDeliveryAreaConsideringHierarchy(DeliveryAreaSet deliveryAreaSet,
			double lat, double lon) {
		if (!deliveryAreaSet.isHierarchy()) {
			return LocationService.assignLocationToDeliveryArea(deliveryAreaSet, lat, lon);
		} else {
			for (DeliveryArea area : deliveryAreaSet.getElements()) {
				DeliveryArea subArea = LocationService.assignLocationToDeliveryArea(area.getSubset(), lat, lon);
				if (subArea != null)
					return subArea;
			}

			return null;
		}
	}

	/**
	 * Identifies the closest node for the given position. Uses euclidean
	 * distance based on lat and lon.
	 * 
	 * @param nodes
	 *            List of nodes
	 * @param lat
	 *            Latitude
	 * @param lon
	 *            Longitude
	 * @return closest node
	 */
	public static Node findClosestNode(ArrayList<Node> nodes, Double lat, Double lon) {

		// Initialize with first node
		double minDistance = Math.hypot((nodes.get(0)).getLat() - lat, (nodes.get(0)).getLon() - lon);
		Node closestNode = nodes.get(0);

		for (int nodeID = 1; nodeID < nodes.size(); nodeID++) {
			// Replace by other node if the distance is smaller.
			// Update smallest distance found so far.
			if (Math.hypot((nodes.get(nodeID)).getLat() - lat, (nodes.get(nodeID)).getLon() - lon) < minDistance) {
				minDistance = Math.hypot((nodes.get(nodeID)).getLat() - lat, (nodes.get(nodeID)).getLon() - lon);
				closestNode = nodes.get(nodeID);
			}
		}

		return closestNode;
	}

	/**
	 * Determines delivery area of a node. Lower bound of area belongs to area
	 * (>=), upper bound does not(<). Returns null, if node does not belong to
	 * any.
	 * 
	 * @param deliveryAreaSet
	 *            Respective delivery area set
	 * @param node1
	 *            Respective request node
	 * @return Delivery area of node
	 */
	public static DeliveryArea assignNodeToDeliveryArea(DeliveryAreaSet deliveryAreaSet, Node node1) {

		ArrayList<DeliveryArea> areas = deliveryAreaSet.getElements();

		for (int areaIndex = 0; areaIndex < areas.size(); areaIndex++) {
			DeliveryArea area = areas.get(areaIndex);

			if ((area.getLat1() <= node1.getLat() && area.getLat2() >= node1.getLat())
					|| (area.getLat2() <= node1.getLat() && area.getLat1() >= node1.getLat())) {
				if ((area.getLon1() <= node1.getLon() && area.getLon2() >= node1.getLon())
						|| (area.getLon2() <= node1.getLon() && area.getLon1() >= node1.getLon())) {

					return area;
				}
			}
		}

		// If no assignment was possible, try tolerance in highest lat and lon
		// values due to rouding problems
		double tolerance = 0.00001;
		for (int areaIndex = 0; areaIndex < areas.size(); areaIndex++) {
			DeliveryArea area = areas.get(areaIndex);

			if ((area.getLat1() <= node1.getLat() + tolerance && area.getLat2() >= node1.getLat() - tolerance)
					|| (area.getLat2() <= node1.getLat() + tolerance && area.getLat1() >= node1.getLat() - tolerance)) {
				if ((area.getLon1() <= node1.getLon() + tolerance && area.getLon2() >= node1.getLon() - tolerance)
						|| (area.getLon2() <= node1.getLon() + tolerance
								&& area.getLon1() >= node1.getLon() - tolerance)) {

					return area;
				}
			}
		}

		System.out.println("null");
		return null;
	}

	/**
	 * Computes all distances between two areas
	 * 
	 * @param deliveryAreaSet
	 *            Respective delivery area set
	 * @param distances
	 *            List of distances
	 * @return distance between Areas
	 */
	public static double[][] computeDistancesBetweenAreas(ArrayList<Node> nodes, ArrayList<NodeDistance> distances,
			DeliveryAreaSet deliveryAreaSet) {

		ArrayList<DeliveryArea> areas = deliveryAreaSet.getElements();

		// ArrayList for which Node belongs to which area
		ArrayList<ArrayList<Node>> nodesInArea = new ArrayList<ArrayList<Node>>();

		// ArrayLists for Areas
		for (int areaNo = 0; areaNo < areas.size(); areaNo++) {
			ArrayList<Node> inArea = new ArrayList<Node>();
			nodesInArea.add(inArea);
		}

		// add node to area list it belongs to
		for (Node node : nodes) {

			for (int areaIndex = 0; areaIndex < areas.size(); areaIndex++) {
				DeliveryArea area = areas.get(areaIndex);
				if ((area.getLat1() <= node.getLat() && area.getLat2() > node.getLat())
						|| (area.getLat2() <= node.getLat() && area.getLat1() > node.getLat())) {
					if ((area.getLon1() <= node.getLon() && area.getLon2() > node.getLon())
							|| (area.getLon2() <= node.getLon() && area.getLon1() > node.getLon())) {
						nodesInArea.get(areaIndex).add(node);
						break;// End inner loop and start with next node
					}
				}
			}
		}

		// Prepare distance matrix between nodes -> do not have to run through
		// all every time later
		HashMap<Long, HashMap<Long, Double>> distanceMatrixNodes = LocationService
				.getDistanceMatrixBetweenNodes(distances);

		// Distance Matrix for distances between all areas
		double[][] distanceMatrixAreas = new double[areas.size()][areas.size()];

		// Go through areas
		for (int area1 = 0; area1 < nodesInArea.size(); area1++) {
			for (int area2 = 0; area2 < nodesInArea.size(); area2++) {

				// Sum up distances between nodes for this combination
				double distanceSum = 0f;
				for (int i = 0; i < nodesInArea.get(area1).size(); i++) {

					for (int j = 0; j < nodesInArea.get(area2).size(); j++) {

						if (nodesInArea.get(area1).get(i).getLongId() == nodesInArea.get(area2).get(j).getLongId()) {
							distanceSum = distanceSum + 0f;
						} else {

							distanceSum = distanceSum
									+ distanceMatrixNodes.get(nodesInArea.get(area1).get(i).getLongId())
											.get(nodesInArea.get(area2).get(j).getLongId());

						}
					}

				}

				if (area1 == area2) { // Other divisor for same area because
										// added 0 twice
					distanceMatrixAreas[area1][area2] = distanceSum
							/ (nodesInArea.get(area1).size() * (nodesInArea.get(area2).size() - 1));
				} else {
					distanceMatrixAreas[area1][area2] = distanceSum
							/ (nodesInArea.get(area1).size() * nodesInArea.get(area2).size());

				}

			}
		}

		double meanDistanceOverall = 0f;
		for (int i = 0; i < areas.size(); i++) {
			for (int j = 0; j < areas.size(); j++) {
				meanDistanceOverall += distanceMatrixAreas[i][j];

			}
		}
		meanDistanceOverall = meanDistanceOverall / (areas.size() * areas.size());
		return distanceMatrixAreas;
	}

	public static HashMap<Long, HashMap<Long, Double>> getDistanceMatrixBetweenNodes(
			ArrayList<NodeDistance> distances) {
		// Prepare distance matrix between nodes -> do not have to run through
		// all every time later
		HashMap<Long, HashMap<Long, Double>> distanceMatrixNodes = new HashMap<Long, HashMap<Long, Double>>();
		for (NodeDistance dis : distances) {

			// One direction

			if (distanceMatrixNodes.containsKey(dis.getNode1Id())) { // Already
																		// one
																		// distance
																		// for
																		// this
																		// node
				distanceMatrixNodes.get(dis.getNode1Id()).put(dis.getNode2Id(), dis.getDistance());
			} else { // If not, new inner hashmap
				HashMap<Long, Double> distanceNode = new HashMap<Long, Double>();
				distanceNode.put(dis.getNode2Id(), dis.getDistance());
				distanceMatrixNodes.put(dis.getNode1Id(), distanceNode);
			}

		}

		return distanceMatrixNodes;
	}

	/**
	 * Provides the distance between two nodes (= areas for aggregation)
	 * 
	 * @param distances
	 *            List of distances
	 * @param node1
	 *            Respective node 1
	 * @param node2
	 *            Respective node 2
	 * @param deliveryAreaSet
	 *            Set with delivery areas
	 * @param distanceMatrixAreas
	 *            Distances between areas of the delivery area set
	 * @return distance
	 */
	public static Double getDistanceBetweenAreas(ArrayList<NodeDistance> distances, Node node1, Node node2,
			DeliveryAreaSet deliveryAreaSet, double[][] distanceMatrixAreas) {

		DeliveryArea area1;
		DeliveryArea area2;

		// to which area nodes belong to
		area1 = LocationService.assignNodeToDeliveryArea(deliveryAreaSet, node1);
		area2 = LocationService.assignNodeToDeliveryArea(deliveryAreaSet, node2);

		// get Index of Area
		ArrayList<DeliveryArea> areas = deliveryAreaSet.getElements();

		int areaMatrix1 = 0;
		int areaMatrix2 = 0;

		for (int areaIndex = 0; areaIndex < areas.size(); areaIndex++) {
			if (area1 == areas.get(areaIndex)) {
				areaMatrix1 = areaIndex;
			}
		}

		for (int areaIndex = 0; areaIndex < areas.size(); areaIndex++) {
			if (area2 == areas.get(areaIndex)) {
				areaMatrix2 = areaIndex;
			}
		}

		return distanceMatrixAreas[areaMatrix1][areaMatrix2];

	}

	/**
	 * Provides direction between two areas 0: in -> in 1: in -> out 2: out ->
	 * in 3: out -> out
	 * 
	 * @param cus1
	 *            Respective customer 1
	 * @param cus2
	 *            Respective customer 2
	 * @param deliveryAreaSet
	 *            Set with delivery areas
	 * @param centerArea
	 *            Area with the depot
	 * @param useDirectDistances
	 *            boolean if use nodes or actual locations for distances
	 * @return distance
	 */
	public static int getDirection(Customer cus1, Customer cus2, DeliveryAreaSet deliveryAreaSet,
			DeliveryArea centerArea, boolean useDirectDistances) {

		DeliveryArea area1;
		DeliveryArea area2;

		if (!useDirectDistances) {
			area1 = LocationService.assignNodeToDeliveryArea(deliveryAreaSet, cus1.getClosestNode());
			area2 = LocationService.assignNodeToDeliveryArea(deliveryAreaSet, cus2.getClosestNode());
		} else {
			area1 = LocationService.assignCustomerToDeliveryArea(deliveryAreaSet, cus1);
			area2 = LocationService.assignCustomerToDeliveryArea(deliveryAreaSet, cus2);
		}

		if (centerArea.getId() == area1.getId()) {
			if (centerArea.getId() == area2.getId()) {
				return 0;
			} else {
				return 1;
			}
		} else {
			if (centerArea.getId() == area2.getId()) {
				return 2;
			} else {
				return 3;
			}
		}
	}

	/**
	 * Provides direction between two areas 0: in -> in 1: in -> out 2: out ->
	 * in 3: out -> out
	 * 
	 * @param node1
	 *            Respective node 1
	 * @param node2
	 *            Respective node 2
	 * @param deliveryAreaSet
	 *            Set with delivery areas
	 * @param centerArea
	 *            Area with the depot
	 * @param useDirectDistances
	 *            boolean if use nodes or actual locations for distances
	 * @return distance
	 */
	public static int getDirection(Node node1, Node node2, DeliveryAreaSet deliveryAreaSet, DeliveryArea centerArea) {

		DeliveryArea area1;
		DeliveryArea area2;

		area1 = LocationService.assignNodeToDeliveryArea(deliveryAreaSet, node1);
		area2 = LocationService.assignNodeToDeliveryArea(deliveryAreaSet, node2);

		if (centerArea.getId() == area1.getId()) {
			if (centerArea.getId() == area2.getId()) {
				return 0;
			} else {
				return 1;
			}
		} else {
			if (centerArea.getId() == area2.getId()) {
				return 2;
			} else {
				return 3;
			}
		}
	}

	/**
	 * Provides the distance between two nodes
	 * 
	 * @param distances
	 *            List of distances
	 * @param node1
	 *            Respective node 1
	 * @param node2
	 *            Respective node 2
	 * @return distance
	 */
	public static double getDistanceBetweenNodes(ArrayList<NodeDistance> distances, Long node1, Long node2) {

		double distance = 0f;

		// If it is the same node, return 0
		if (node1.longValue() == node2.longValue()) {
			// TODO adjust some day
			// System.out.println("TRUE");
			distance = 0.0f;

		} else {
			// Go through nodes
			for (NodeDistance dis : distances) {

				// if the first node of the distance needs to fit to the first
				// provided node and the other respectively
				if (dis.getNode1Id().longValue() == node1.longValue()) {
					// System.out.println("TRUE2");

					// The other node of the distance also has to fit
					if (dis.getNode2Id().longValue() == node2.longValue()) {
						System.out.println("TRUE3");

						distance = dis.getDistance();

						System.out.println("############: " + dis.getDistance());
						return distance;

					}
				}
			}
		}
		System.out.println(distance);
		return distance;
	}

	/**
	 * Fills delivery area weights and demand segment weighting per delivery
	 * area. Considers delivery area hierarchies.
	 * 
	 * @param lambdas
	 *            Respective overall weights of delivery areas
	 * @param weightings
	 *            Respective demand segment weightings of delivery areas
	 * @param daSet
	 *            Delivery area set
	 * @param overallWeighting
	 *            Overall weighting
	 */
	public static void determineDeliveryAreaWeightAndDemandSegmentWeightingPerDeliveryAreaConsideringHierarchy(
			HashMap<DeliveryArea, Double> deliveryAreaWeights, HashMap<DeliveryArea, DemandSegmentWeighting> weightings,
			DeliveryAreaSet daSet, DemandSegmentWeighting overallWeighting) {
		HashMap<DeliveryArea, ArrayList<DemandSegmentWeight>> weights = new HashMap<DeliveryArea, ArrayList<DemandSegmentWeight>>();

		LocationService.determineDeliveryAreaWeightAndDemandSegmentWeightsPerDeliveryArea(deliveryAreaWeights, weights,
				daSet, overallWeighting, true);

		for (DeliveryArea area : weights.keySet()) {
			DemandSegmentWeighting daWeighting = new DemandSegmentWeighting();
			daWeighting.setSetEntity(overallWeighting.getSetEntity());
			daWeighting.setSetEntityId(overallWeighting.getSetEntity().getId());
			daWeighting.setWeights(weights.get(area));
			weightings.put(area, daWeighting);
		}
	}

	public static void determineDeliveryAreaWeightAndDemandSegmentWeightingPerDeliveryAreaNotConsideringHierarchy(
			HashMap<DeliveryArea, Double> deliveryAreaWeights, HashMap<DeliveryArea, DemandSegmentWeighting> weightings,
			DeliveryAreaSet daSet, DemandSegmentWeighting overallWeighting) {
		HashMap<DeliveryArea, ArrayList<DemandSegmentWeight>> weights = new HashMap<DeliveryArea, ArrayList<DemandSegmentWeight>>();

		LocationService.determineDeliveryAreaWeightAndDemandSegmentWeightsPerDeliveryArea(deliveryAreaWeights, weights,
				daSet, overallWeighting, false);

		for (DeliveryArea area : weights.keySet()) {
			DemandSegmentWeighting daWeighting = new DemandSegmentWeighting();
			daWeighting.setSetEntity(overallWeighting.getSetEntity());
			daWeighting.setSetEntityId(overallWeighting.getSetEntity().getId());
			daWeighting.setWeights(weights.get(area));
			weightings.put(area, daWeighting);
		}
	}

	public static void determineDeliveryAreaWeightAndDemandSegmentWeightsPerDeliveryAreaAndDemandSegmentConsideringHierarchy(
			HashMap<DeliveryArea, Double> deliveryAreaWeights,
			HashMap<DeliveryArea, HashMap<DemandSegment, Double>> weightings, DeliveryAreaSet daSet,
			DemandSegmentWeighting overallWeighting) {

		HashMap<DeliveryArea, ArrayList<DemandSegmentWeight>> weights = new HashMap<DeliveryArea, ArrayList<DemandSegmentWeight>>();

		LocationService.determineDeliveryAreaWeightAndDemandSegmentWeightsPerDeliveryArea(deliveryAreaWeights, weights,
				daSet, overallWeighting, true);

		for (DeliveryArea area : weights.keySet()) {
			HashMap<DemandSegment, Double> weightsMap = new HashMap<DemandSegment, Double>();
			weightings.put(area, weightsMap);
			for (DemandSegmentWeight w : weights.get(area)) {
				weightings.get(area).put(w.getDemandSegment(), w.getWeight());
			}
		}

	}

	public static void determineDeliveryAreaWeightAndDemandSegmentWeightsPerDeliveryAreaAndDemandSegmentNotConsideringHierarchy(
			HashMap<DeliveryArea, Double> deliveryAreaWeights,
			HashMap<DeliveryArea, HashMap<DemandSegment, Double>> weightings, DeliveryAreaSet daSet,
			DemandSegmentWeighting overallWeighting) {

		HashMap<DeliveryArea, ArrayList<DemandSegmentWeight>> weights = new HashMap<DeliveryArea, ArrayList<DemandSegmentWeight>>();

		LocationService.determineDeliveryAreaWeightAndDemandSegmentWeightsPerDeliveryArea(deliveryAreaWeights, weights,
				daSet, overallWeighting, false);

		for (DeliveryArea area : weights.keySet()) {
			HashMap<DemandSegment, Double> weightsMap = new HashMap<DemandSegment, Double>();
			weightings.put(area, weightsMap);
			for (DemandSegmentWeight w : weights.get(area)) {
				weightings.get(area).put(w.getDemandSegment(), w.getWeight());
			}
		}

	}

	private static void determineDeliveryAreaWeightAndDemandSegmentWeightsPerDeliveryArea(
			HashMap<DeliveryArea, Double> deliveryAreaWeights,
			HashMap<DeliveryArea, ArrayList<DemandSegmentWeight>> weightings, DeliveryAreaSet daSet,
			DemandSegmentWeighting overallWeighting, boolean considerHierarchy) {

		// Go through delivery areas and define lambda and segment weighting per
		// area
		ArrayList<DeliveryArea> deliveryAreas = new ArrayList<DeliveryArea>();

		// Hierarchy?
		for (DeliveryArea area : daSet.getElements()) {
			if (area.getSubsetId() != 0 && considerHierarchy) {
				deliveryAreas.addAll(area.getSubset().getElements());
			} else {
				deliveryAreas.add(area);
			}
		}

		for (DeliveryArea da : deliveryAreas) {

			ArrayList<DemandSegmentWeight> dswDa = new ArrayList<DemandSegmentWeight>();

			/// Go through global segment weights and determine ratio in
			/// respective delivery area
			Double ratioInDa = 0.0;
			ArrayList<DemandSegmentWeight> demandSegmentWeights = overallWeighting.getWeights();

			for (DemandSegmentWeight dsWeight : demandSegmentWeights) {
				DemandSegment segment = dsWeight.getDemandSegment();
				ArrayList<ResidenceAreaWeight> raWeights = segment.getResidenceAreaWeighting().getWeights();

				double daSpecificSegmentWeight = 0.0;

				/// Find weight of segment for the delivery area
				for (ResidenceAreaWeight raWeight : raWeights) {
					ResidenceArea area = raWeight.getResidenceArea();

					// Check if residence area overlaps with delivery area
					double overlappingRatio = LocationService
							.determineRatioOfResidenceAreaThatFallsIntoDeliveryArea(area, da);

					/// Weight delivery area weight with demand segment
					/// weight and add to delivery area probability
					ratioInDa += dsWeight.getWeight() * raWeight.getWeight() * overlappingRatio;

					// and to SegmentProbability for this da

					daSpecificSegmentWeight += dsWeight.getWeight() * raWeight.getWeight() * overlappingRatio;

				}

				/// Produce new demand segment weight with old
				/// demand segment but weight for delivery area
				/// The weight still needs to be divided by the
				/// delivery area probability later
				DemandSegmentWeight dsw = new DemandSegmentWeight();
				dsw.setDemandSegment(segment);
				dsw.setElementId(segment.getId());
				dsw.setWeight(daSpecificSegmentWeight);
				dswDa.add(dsw);
			}

			// Divide weights by delivery area weight and add to new demand
			// segment weighting
			ArrayList<DemandSegmentWeight> dswDaEntity = new ArrayList<DemandSegmentWeight>();
			for (DemandSegmentWeight w : dswDa) {
				w.setWeight(w.getWeight() / ratioInDa);
				dswDaEntity.add(w);
			}

			// Add lambda of delivery area to map
			deliveryAreaWeights.put(da, ratioInDa);
			weightings.put(da, dswDaEntity);
		}

	}

	private static double determineRatioOfResidenceAreaThatFallsIntoDeliveryArea(ResidenceArea rArea,
			DeliveryArea dArea) {

		double lowerRaLat = rArea.getLat1();
		double upperRaLat = rArea.getLat2();
		if (lowerRaLat > upperRaLat) {
			lowerRaLat = upperRaLat;
			upperRaLat = rArea.getLat1();
		}

		double lowerRaLon = rArea.getLon1();
		double upperRaLon = rArea.getLon2();
		if (lowerRaLon > upperRaLon) {
			lowerRaLon = upperRaLon;
			upperRaLon = rArea.getLon1();
		}

		double lowerDaLat = dArea.getLat1();
		double upperDaLat = dArea.getLat2();
		if (lowerDaLat > upperDaLat) {
			lowerDaLat = upperDaLat;
			upperDaLat = dArea.getLat1();
		}

		double lowerDaLon = dArea.getLon1();
		double upperDaLon = dArea.getLon2();
		if (lowerDaLon > upperDaLon) {
			lowerDaLon = upperDaLon;
			upperDaLon = dArea.getLon1();
		}

		double ratio = 0.0;

		// Overlapping?
		double overlappingAreaSize = 0.0;
		if (upperRaLat >= lowerDaLat && lowerRaLat <= upperDaLat) {
			if (upperRaLon >= lowerDaLon && lowerRaLon <= upperDaLon) {

				// How much?
				double upperLat = upperRaLat;
				if (upperDaLat < upperLat)
					upperLat = upperDaLat;

				double lowerLat = lowerRaLat;
				if (lowerDaLat > lowerLat)
					lowerLat = lowerDaLat;

				double upperLon = upperRaLon;
				if (upperDaLon < upperLon)
					upperLon = upperDaLon;

				double lowerLon = lowerRaLon;
				if (lowerDaLon > lowerLon)
					lowerLon = lowerDaLon;

				overlappingAreaSize = (upperLon - lowerLon) * (upperLat - lowerLat);
			}
		}

		ratio = overlappingAreaSize / ((upperRaLat - lowerRaLat) * (upperRaLon - lowerRaLon));
		return ratio;
	}

	public final static double AVERAGE_RADIUS_OF_EARTH_KM = 6371;
//TODO: 6378.137?
	/**
	 * Calculates the Haversine distance between to gps points
	 * 
	 * @param userLat
	 * @param userLng
	 * @param venueLat
	 * @param venueLng
	 * @return
	 */
	public static double calculateHaversineDistanceBetweenGPSPointsInKilometer(double userLat, double userLng,
			double venueLat, double venueLng) {

		double latDistance = Math.toRadians(userLat - venueLat);
		double lngDistance = Math.toRadians(userLng - venueLng);

		double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + Math.cos(Math.toRadians(userLat))
				* Math.cos(Math.toRadians(venueLat)) * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

		return AVERAGE_RADIUS_OF_EARTH_KM * c;
	}

	/**
	 * Calculates euclidean distance (Haversine distance) between two customers
	 * (beedistance)
	 * 
	 * @param a
	 *            Customer 1
	 * @param b
	 *            Customer 2
	 * @return beedistance between locations
	 */
	public static double calculateHaversineDistanceBetweenCustomers(Customer a, Customer b) {
		double beedistance = LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(a.getLat(),
				a.getLon(), b.getLat(), b.getLon());
		return beedistance;
	}

	public static ArrayList<DeliveryArea> determineSameSizeDeliveryAreasWithDummyIds(int setId, double lat1,
			double lat2, double lon1, double lon2, int numberLat, int numberLon) {

		double lowerLat = lat1;
		double higherLat = lat2;
		if (lowerLat > higherLat) {
			lowerLat = higherLat;
			higherLat = lat1;
		}

		double lowerLon = lon1;
		double higherLon = lon2;
		if (lowerLon > higherLon) {
			lowerLon = higherLon;
			higherLon = lon1;
		}

		ArrayList<DeliveryArea> areas = new ArrayList<DeliveryArea>();
		int currentId = -1;
		double currentLowerLat = lowerLat;
		double latInterval = (higherLat - lowerLat) / numberLat;
		double lonInterval = (higherLon - lowerLon) / numberLon;

		for (int lat = 0; lat < numberLat; lat++) {
			double currentLowerLon = lowerLon;
			for (int lon = 0; lon < numberLon; lon++) {
				DeliveryArea area = new DeliveryArea();
				area.setId(currentId);
				area.setSetId(setId);
				area.setLat1(currentLowerLat);
				area.setLat2(currentLowerLat + latInterval);
				area.setLon1(currentLowerLon);
				area.setLon2(currentLowerLon + lonInterval);
				area.setCenterLat(currentLowerLat + latInterval / 2.0);
				area.setCenterLon(currentLowerLon + lonInterval / 2.0);
				currentId = currentId - 1;
				currentLowerLon = currentLowerLon + lonInterval;
				areas.add(area);
			}

			currentLowerLat = currentLowerLat + latInterval;

		}

		return areas;
	}

	public static ArrayList<ResidenceArea> determineSameSizeResidenceAreasWithDummyIds(int firstId, double lat1,
			double lat2, double lon1, double lon2, int numberLat, int numberLon) {

		double lowerLat = lat1;
		double higherLat = lat2;
		if (lowerLat > higherLat) {
			lowerLat = higherLat;
			higherLat = lat1;
		}

		double lowerLon = lon1;
		double higherLon = lon2;
		if (lowerLon > higherLon) {
			lowerLon = higherLon;
			higherLon = lon1;
		}

		ArrayList<ResidenceArea> areas = new ArrayList<ResidenceArea>();
		int currentId = firstId;
		double currentLowerLat = lowerLat;
		double latInterval = (higherLat - lowerLat) / numberLat;
		double lonInterval = (higherLon - lowerLon) / numberLon;

		for (int lat = 0; lat < numberLat; lat++) {
			double currentLowerLon = lowerLon;
			for (int lon = 0; lon < numberLon; lon++) {
				ResidenceArea area = new ResidenceArea();
				area.setId(currentId);
				area.setLat1(currentLowerLat);
				area.setLat2(currentLowerLat + latInterval);
				area.setLon1(currentLowerLon);
				area.setLon2(currentLowerLon + lonInterval);
				currentId = currentId + 1;
				currentLowerLon = currentLowerLon + lonInterval;
				areas.add(area);
			}

			currentLowerLat = currentLowerLat + latInterval;

		}

		return areas;
	}

	public static ArrayList<DeliveryArea> determineDeliveryAreasIncludedInRegion(ArrayList<DeliveryArea> potentialAreas,
			double lowerLat, double upperLat, double lowerLon, double upperLon) {
		ArrayList<DeliveryArea> areas = new ArrayList<DeliveryArea>();
		double tolerance = 0.0001;
		for (DeliveryArea area : potentialAreas) {
			if ((area.getLat1() >= lowerLat - tolerance && area.getLat2() <= upperLat + tolerance)
					|| (area.getLat2() >= lowerLat - tolerance && area.getLat1() <= upperLat + tolerance)) {
				if ((area.getLon1() >= lowerLon - tolerance && area.getLon2() <= upperLon + tolerance)
						|| (area.getLon2() >= lowerLon - tolerance && area.getLon1() <= upperLon + tolerance)) {
					areas.add(area);
				}
			}
		}
		return areas;
	}

}