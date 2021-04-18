package data.entity;

public class TimeWindow extends Entity{
	
	private Integer id;
	private Integer setId;
	private Double startTime; //Only need time
	private Double endTime;
	private boolean available; // if time window is available for a customer or not
	private TimeWindow tempFollower;
	private Double tempSlackFollower;
	
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
	public Double getStartTime() {
		return startTime;
	}
	public void setStartTime(Double startTime) {
		this.startTime = startTime;
	}
	public Double getEndTime() {
		return endTime;
	}
	public void setEndTime(Double endTime) {
		this.endTime = endTime;
	}
	
	public boolean available(){
		return available;
	}
	
	public void setAvailable(boolean available){
		this.available = available;
	}
	
	public boolean getAvailable(){
		return available;
	}
	
	@Override
	public boolean equals(Object o){
	   if(this==o){
	      return true;
	   }
	   if(o instanceof TimeWindow){
		   TimeWindow other = (TimeWindow) o;
	       return this.id == other.getId();
	   }
	   return false;
	}
	
	@Override
	public int hashCode(){
	   return this.id;
	}
	
	public String toString(){
		return "tw"+this.id;
	}
	public TimeWindow getTempFollower() {
		return tempFollower;
	}
	public void setTempFollower(TimeWindow tempFollower) {
		this.tempFollower = tempFollower;
	}
	public Double getTempSlackFollower() {
		return tempSlackFollower;
	}
	public void setTempSlackFollower(Double tempSlackFollower) {
		this.tempSlackFollower = tempSlackFollower;
	}
	
}
