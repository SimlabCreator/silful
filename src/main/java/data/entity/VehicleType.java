package data.entity;

public class VehicleType extends Entity{
	
	private Integer id;
	private Double capacityVolume;
	private Integer capacityNumber;
	private Boolean cooling;
	private Boolean freezer;
	
	
	public int getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public Double getCapacityVolume() {
		return capacityVolume;
	}
	public void setCapacityVolume(Double capacityVolume) {
		this.capacityVolume = capacityVolume;
	}
	public Integer getCapacityNumber() {
		return capacityNumber;
	}
	public void setCapacityNumber(Integer capacityNumber) {
		this.capacityNumber = capacityNumber;
	}
	public Boolean getCooling() {
		return cooling;
	}
	public void setCooling(Boolean cooling) {
		this.cooling = cooling;
	}
	public Boolean getFreezer() {
		return freezer;
	}
	public void setFreezer(Boolean freezer) {
		this.freezer = freezer;
	}
	
	public String toString(){
		return id+"; Capacity volume: "+this.capacityVolume+"; Capacity number: "+this.capacityNumber+"; Cooling: "+this.cooling+"; Freezer: "+this.freezer;
	}
	
	
}
