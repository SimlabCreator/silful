package data.entity;

public class ValueBucket extends Entity{
	
	private Integer id;
	private Integer setId;
	private Double upperBound;
	private Double lowerBound;
	
	public int getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public Integer getSetId() {
		return setId;
	}
	public void setSetId(Integer setId) {
		this.setId = setId;
	}
	public Double getUpperBound() {
		return upperBound;
	}
	public void setUpperBound(Double upperBound) {
		this.upperBound = upperBound;
	}
	public Double getLowerBound() {
		return lowerBound;
	}
	public void setLowerBound(Double lowerBound) {
		this.lowerBound = lowerBound;
	}
	
	
	@Override
	public boolean equals(Object o){
	   if(this==o){
	      return true;
	   }
	   if(o instanceof ValueBucket){
		   ValueBucket other = (ValueBucket) o;
	       return this.id == other.getId();
	   }
	   return false;
	}
	
	@Override
	public int hashCode(){
	   return this.id;
	}
	
	
}
