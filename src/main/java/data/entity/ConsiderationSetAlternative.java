package data.entity;

import data.utility.DataServiceProvider;

public class ConsiderationSetAlternative extends Entity {

	private int id;
	private Integer demandSegmentId;
	private DemandSegment demandSegment;
	private int alternativeId;
	private Alternative alternative;
	private Double weight; // Only needed if parametric and independent of offer
							// set
	private Integer predecessorId; // Only needed of non-parametric
	private ConsiderationSetAlternative predecessor; // Only needed for
														// non-parametric
	private Double coefficient; // Only needed if parametric and no direct
								// weights are given

	private Integer setId;
	
	public int getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getDemandSegmentId() {
		return demandSegmentId;
	}

	public void setDemandSegmentId(Integer demandSegmentId) {
		this.demandSegmentId = demandSegmentId;
	}

	public DemandSegment getDemandSegment() {
		
		if(this.demandSegment==null){
			this.demandSegment= DataServiceProvider.getDemandSegmentDataServiceImplInstance().getElementById(this.demandSegmentId);
		}
		return demandSegment;
	}

	public void setDemandSegment(DemandSegment demandSegment) {
		this.demandSegment = demandSegment;
	}

	public int getAlternativeId() {
		return alternativeId;
	}

	public void setAlternativeId(int alternativeId) {
		this.alternativeId = alternativeId;
	}

	public Alternative getAlternative() {
		if (this.alternative == null) {
			this.alternative =  DataServiceProvider.getAlternativeDataServiceImplInstance()
					.getElementById(this.alternativeId);
		}
		return alternative;
	}

	public void setAlternative(Alternative alternative) {
		this.alternative = alternative;
	}

	public Double getWeight() {
		return weight;
	}

	public void setWeight(Double weight) {
		this.weight = weight;
	}

	public Integer getPredecessorId() {
		return predecessorId;
	}

	public void setPredecessorId(Integer predecessor) {
		this.predecessorId = predecessor;
	}

	public void setPredecessor(ConsiderationSetAlternative alt) {
		this.predecessor = alt;
	}

	public ConsiderationSetAlternative getPredecessor(){
		if(this.predecessor==null){
			this.predecessor= (ConsiderationSetAlternative) DataServiceProvider.getDemandSegmentDataServiceImplInstance().getConsiderationSetAlternativeById(this.demandSegmentId);
		}
		return this.predecessor;
	}

	public Double getCoefficient() {
		return coefficient;
	}

	public void setCoefficient(Double coefficient) {
		this.coefficient = coefficient;
	}
	
	@Override
	public boolean equals(Object o){
	   if(this==o){
	      return true;
	   }
	   if(o instanceof ConsiderationSetAlternative){
		   ConsiderationSetAlternative other = (ConsiderationSetAlternative) o;
	       return (this.id == other.getId() && this.demandSegmentId==other.getDemandSegmentId() && this.alternativeId==other.getAlternativeId());
	   }
	   return false;
	}
	
	@Override
	public int hashCode(){
	   return this.id+this.demandSegmentId+this.alternativeId;
	}

	public Integer getSetId() {
		return setId;
	}

	public void setSetId(Integer setId) {
		this.setId = setId;
	}

}
