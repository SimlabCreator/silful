package logic.utility.comparator;

import java.util.Comparator;

import data.entity.RouteElement;
import logic.service.support.ProbabilityDistributionService;
import logic.utility.exceptions.ParameterUnknownException;

/**
 * Helps to sort insertion elements in routing
 * 
 * @author M. Lang
 *
 */
public class RouteElementTempAdditionalCostValueDescComparator implements Comparator<RouteElement> {

	
		private boolean actualValue;
		private boolean squared;

		public RouteElementTempAdditionalCostValueDescComparator(int actualValue, int squared) {
			this.actualValue = (actualValue==1.0);
			this.squared = (squared==1.0);
		}
		
		public RouteElementTempAdditionalCostValueDescComparator(boolean actualValue, boolean squared) {
			this.actualValue = actualValue;
			this.squared = squared;
		}

		public int compare(RouteElement a, RouteElement b) {

			double valueA=0.0;
			double valueB=0.0;

			if (this.actualValue) {
				valueA = a.getOrder().getOrderRequest().getBasketValue();
				valueB = b.getOrder().getOrderRequest().getBasketValue();
			} else {

				try {
					valueA = ProbabilityDistributionService.getMeanByProbabilityDistribution(a.getOrder().getOrderRequest()
							.getCustomer().getOriginalDemandSegment().getBasketValueDistribution());
				} catch (ParameterUnknownException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.exit(0);
				}
				try {
					valueB = ProbabilityDistributionService.getMeanByProbabilityDistribution(b.getOrder().getOrderRequest()
							.getCustomer().getOriginalDemandSegment().getBasketValueDistribution());
				} catch (ParameterUnknownException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.exit(0);
				}
			}

			if (this.squared) {
				valueA = Math.sqrt(valueA);
				valueB = Math.sqrt(valueB);
			}
			return valueA / a.getTempAdditionalCostsValue() > valueB / b.getTempAdditionalCostsValue() ? -1
					: valueA / a.getTempAdditionalCostsValue() == valueB / b.getTempAdditionalCostsValue() ? 0 : 1;
		}
	}

