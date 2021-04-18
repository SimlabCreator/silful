package logic;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import logic.service.support.LocationService;
public class HaversineDistanceTest {



	
		@BeforeClass
		public static void start() {
			
		}
	 
		@AfterClass
		public static void end() {

		}
		
		@Test
		public void persistAndGet() {
			
			
			
			System.out.println(LocationService.calculateHaversineDistanceBetweenGPSPointsInKilometer(13.3, 52.45, 13.5, 52.55)/24*60);
			
		}
		

}
