package logic.utility;

import org.apache.commons.math3.distribution.BinomialDistribution;

import logic.service.support.LocationService;

public class Haversine {

	public static void main(String[] args) {
	
		System.out.println(LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(48.0982, 11.430, 48.2128, 11.652)/ 32 * 60);
		System.out.println(LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(48.0982, 11.430, 48.2128, 11.430)/ 32 * 60);
		System.out.println(LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(48.0982, 11.430, 48.0982, 11.652)/ 32 * 60);
		System.out.println(LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(48.1173, 11.467, 48.1937, 11.615)/ 32 * 60);
		System.out.println(LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(48.1173, 11.467, 48.1937, 11.467)/ 32 * 60);
		System.out.println(LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(48.1173, 11.467, 48.1173, 11.615)/ 32 * 60);

		System.out.println(LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(0.00000, 0.0, 0.054, 0.0)/ 24 * 60);
		System.out.println(LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(0.00000, 0.0, 0.0, 0.054)/ 24 * 60);

		
		System.out.println(LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(48.1173, 11.467, 48.1937, 11.615));
		System.out.println(LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(48.1173, 11.467, 48.1173, 11.615));
		System.out.println(LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(48.1173, 11.467, 48.1937,11.467));
		
		BinomialDistribution bd = new BinomialDistribution(200, 0.05);
		System.out.println(bd.inverseCumulativeProbability(0.4));
		
		BinomialDistribution bd2 = new BinomialDistribution(200, 0.025);
		System.out.println(bd2.inverseCumulativeProbability(0.1));
	}

}
