package logic;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import logic.service.support.ProbabilityDistributionService;
public class DistributionTestGumbelDistribution {



	
		@BeforeClass
		public static void start() {
			
		}
	 
		@AfterClass
		public static void end() {

		}
		
		@Test
		public void persistAndGet() {
			
			
			
			System.out.println(ProbabilityDistributionService.getGumbelDistributedRandomNumber(0, 1));
			
		}
		

}
