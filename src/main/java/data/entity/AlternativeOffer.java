package data.entity;

import data.utility.DataServiceProvider;

public class AlternativeOffer extends Entity{

	private int id;
	private Integer alternativeId;
	private Alternative alternative;
	private Integer orderId;
	private Order order;
	private double incentive;
	private Integer tempAlternativeNo;
	
	//No functionality, only for parent-class
	public int getId(){
		return id;
	}
	
	public Integer getAlternativeId() {
		return alternativeId;
	}
	public void setAlternativeId(Integer alternativeId) {
		this.alternativeId = alternativeId;
	}
	public Integer getOrderId() {
		return orderId;
	}
	public void setOrderId(Integer orderId) {
		this.orderId = orderId;
	}
	public double getIncentive() {
		return incentive;
	}
	public void setIncentive(double incentive) {
		this.incentive = incentive;
	}
	public Alternative getAlternative() {
		
		if(this.alternative==null){
			this.alternative= DataServiceProvider.getAlternativeDataServiceImplInstance().getElementById(this.alternativeId);
		}
		return alternative;
	}
	public void setAlternative(Alternative alternative) {
		this.alternative = alternative;
	}
	public Order getOrder() {
		if(this.order==null){
			this.order=DataServiceProvider.getOrderDataServiceImplInstance().getElementById(this.orderId);
		}
		return order;
	}
	public void setOrder(Order order) {
		this.order = order;
	}
	public Integer getTempAlternativeNo() {
		return tempAlternativeNo;
	}
	public void setTempAlternativeNo(Integer tempAlternativeNo) {
		this.tempAlternativeNo = tempAlternativeNo;
	}
	
	@Override
	public boolean equals(Object o){
	   if(this==o){
	      return true;
	   }
	   if(o instanceof AlternativeOffer){
		   AlternativeOffer other = (AlternativeOffer) o;
	       return (this.alternativeId == other.getAlternativeId() && this.tempAlternativeNo==other.getTempAlternativeNo() && this.incentive==other.getIncentive());
	   }
	   return false;
	}
	
	@Override
	public int hashCode(){
		int result = 17;
		result = 31 * result + this.alternativeId;
        result = (int) (31 * result + Math.round(this.incentive));
	   return result;
	}
	
	
}
