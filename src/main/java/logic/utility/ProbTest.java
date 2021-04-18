package logic.utility;

import data.entity.ProbabilityDistribution;
import data.utility.DataServiceProvider;
import logic.service.support.ProbabilityDistributionService;
import logic.utility.exceptions.ParameterUnknownException;

public class ProbTest {

	public static void main(String[] args) {
	
		ProbabilityDistribution pd=(ProbabilityDistribution) DataServiceProvider.getProbabilityDistributionDataServiceImplInstance().getById(35);
		try {
			System.out.println(ProbabilityDistributionService.getRandomNumberByProbabilityDistribution(pd));
			System.out.println(ProbabilityDistributionService.getRandomNumberByProbabilityDistribution(pd));
			System.out.println(ProbabilityDistributionService.getRandomNumberByProbabilityDistribution(pd));
		} catch (ParameterUnknownException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		 pd=(ProbabilityDistribution) DataServiceProvider.getProbabilityDistributionDataServiceImplInstance().getById(36);
		try {
			System.out.println(ProbabilityDistributionService.getRandomNumberByProbabilityDistribution(pd));
			System.out.println(ProbabilityDistributionService.getRandomNumberByProbabilityDistribution(pd));
			System.out.println(ProbabilityDistributionService.getRandomNumberByProbabilityDistribution(pd));
		} catch (ParameterUnknownException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 pd=(ProbabilityDistribution) DataServiceProvider.getProbabilityDistributionDataServiceImplInstance().getById(33);
		try {
			System.out.println(ProbabilityDistributionService.getQuantileByCummulativeDistribution(pd, (double) 45));
			System.out.println(ProbabilityDistributionService.getXByCummulativeDistributionQuantile(pd, 0.5));
		} catch (ParameterUnknownException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
