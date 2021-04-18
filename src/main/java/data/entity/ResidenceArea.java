package data.entity;

public class ResidenceArea extends Entity{
	
	private int id;
	private int setId;
	private double lat1;
	private double lon1;
	private double lat2;
	private double lon2;
	private Integer reasonableSubareaNumber; 
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getSetId() {
		return setId;
	}
	public void setSetId(int setId) {
		this.setId = setId;
	}
	public double getLat1() {
		return lat1;
	}
	public void setLat1(double lat1) {
		this.lat1 = lat1;
	}
	public double getLon1() {
		return lon1;
	}
	public void setLon1(double lon1) {
		this.lon1 = lon1;
	}
	public double getLat2() {
		return lat2;
	}
	public void setLat2(double lat2) {
		this.lat2 = lat2;
	}
	public double getLon2() {
		return lon2;
	}
	public void setLon2(double lon2) {
		this.lon2 = lon2;
	}

	@Override
	public boolean equals(Object o){
	   if(this==o){
	      return true;
	   }
	   if(o instanceof ResidenceArea){
		   ResidenceArea other = (ResidenceArea) o;
	       return this.id == other.getId();//TODO: Reconsider if areas are created on the fly (no id yet)
	   }
	   return false;
	}
	
	@Override
	public int hashCode(){
	   return this.id;
	}
	public Integer getReasonableSubareaNumber() {
		return reasonableSubareaNumber;
	}
	public void setReasonableSubareaNumber(Integer reasonableSubareaNumber) {
		this.reasonableSubareaNumber = reasonableSubareaNumber;
	}

}
