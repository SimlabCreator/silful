package data.entity;

import data.utility.DataServiceProvider;

public class Vehicle extends Entity {

	private int vehicleTypeId;
	private VehicleType vehicleType;
	private Integer vehicleNo;
	private int experimentId;
	
	private int id;
	// No functionality, only for parent-class
	public int getId() {
		return id;
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
	public int getExperimentId() {
		return experimentId;
	}
	public void setExperimentId(int experimentId) {
		this.experimentId = experimentId;
	}

	public Vehicle copy() {
		Vehicle v = new Vehicle();
		v.setVehicleTypeId(vehicleTypeId);
		v.setVehicleType(vehicleType);
		v.setVehicleNo(vehicleNo.intValue());
		v.setExperimentId(experimentId);
		return v;
	}
}
