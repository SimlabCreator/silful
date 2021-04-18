package data.entity;

import data.utility.DataServiceProvider;

public class VehicleAreaAssignment extends Entity {

	private int id;
	private int setId;
	private int vehicleTypeId;
	private VehicleType vehicleType;
	private Integer vehicleNo;
	private DeliveryArea deliveryArea;
	private Integer deliveryAreaId;
	private double startingLocationLat;
	private double startingLocationLon;
	private double endingLocationLat;
	private double endingLocationLon;
	private double startTime;
	private double endTime;
	
	public int getId() {
		return id;
	}
	

	public void setId(int id) {
		this.id = id;
	}
	
	public int getVehicleTypeId() {
		return vehicleTypeId;
	}
	public void setVehicleTypeId(int vehicleTypeId) {
		this.vehicleTypeId = vehicleTypeId;
	}
	public VehicleType getVehicleType() {
		
		if(this.vehicleType==null){
			this.vehicleType = (VehicleType) DataServiceProvider.getVehicleTypeDataServiceImplInstance().getById(this.vehicleTypeId);
		}
		return vehicleType;
	}
	public void setVehicleType(VehicleType vehicleType) {
		this.vehicleType = vehicleType;
	}
	public Integer getVehicleNo() {
		return vehicleNo;
	}
	public void setVehicleNo(Integer vehicleNo) {
		this.vehicleNo = vehicleNo;
	}

	public int getSetId() {
		return setId;
	}

	public void setSetId(int setId) {
		this.setId = setId;
	}

	public DeliveryArea getDeliveryArea() {
		if(this.deliveryArea==null){
			this.deliveryArea = DataServiceProvider.getDeliveryAreaDataServiceImplInstance().getElementById(this.deliveryAreaId);
		}
		return deliveryArea;
	}

	public void setDeliveryArea(DeliveryArea deliveryArea) {
		this.deliveryArea = deliveryArea;
	}

	public Integer getDeliveryAreaId() {
		return deliveryAreaId;
	}

	public void setDeliveryAreaId(Integer deliveryAreaId) {
		this.deliveryAreaId = deliveryAreaId;
	}

	public double getStartingLocationLat() {
		return startingLocationLat;
	}

	public void setStartingLocationLat(double startingLocationLat) {
		this.startingLocationLat = startingLocationLat;
	}

	public double getStartingLocationLon() {
		return startingLocationLon;
	}

	public void setStartingLocationLon(double startingLocationLon) {
		this.startingLocationLon = startingLocationLon;
	}

	public double getStartTime() {
		return startTime;
	}

	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}

	public double getEndTime() {
		return endTime;
	}

	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}


	public double getEndingLocationLat() {
		return endingLocationLat;
	}


	public void setEndingLocationLat(double endingLocationLat) {
		this.endingLocationLat = endingLocationLat;
	}


	public double getEndingLocationLon() {
		return endingLocationLon;
	}


	public void setEndingLocationLon(double endingLocationLon) {
		this.endingLocationLon = endingLocationLon;
	}

	@Override
	public boolean equals(Object o){
	   if(this==o){
	      return true;
	   }
	   if(o instanceof VehicleAreaAssignment){
		   VehicleAreaAssignment other = (VehicleAreaAssignment) o;
	       return this.id == other.getId();
	   }
	   return false;
	}
	
	@Override
	public int hashCode(){
	   return this.id;
	}
	

	
}
