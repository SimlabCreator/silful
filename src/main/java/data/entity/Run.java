package data.entity;

import java.util.Date;

public class Run extends Entity {

	private int id;
	//private String description;
	private Date date;
	private Integer experimentId;
	private Integer runtime;
//	private String responsible;
//	private String occasion;
//	private ArrayList<Entity> vehicles; //Not always needed, depends on process type
//	private Region region;
//	private Integer regionId;
//	private Integer processTypeId;
//	private ProcessType processType;
//	private Integer bookingPeriodLength;
//	private Integer bookingPeriodNumber;
//	private Integer incentiveTypeId;//Not always needed, depends on process type
//	private IncentiveType incentiveType;//Not always needed, depends on process type


	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

//	public String getResponsible() {
//		return responsible;
//	}
//
//	public void setResponsible(String responsible) {
//		this.responsible = responsible;
//	}
//
//	public String getOccasion() {
//		return occasion;
//	}
//
//	public void setOccasion(String occasion) {
//		this.occasion = occasion;
//	}
//
//	public Region getRegion() {
//
//		if (region == null) {
//			region = (Region) DataServiceProvider.getRegionDataServiceImplInstance().getById(regionId);
//		}
//		return region;
//	}
//
//	public void setRegion(Region region) {
//		this.region = region;
//	}
//
//	public Integer getRegionId() {
//		return regionId;
//	}
//
//	public void setRegionId(Integer regionId) {
//		this.regionId = regionId;
//	}
//
//	public Integer getProcessTypeId() {
//		return processTypeId;
//	}
//
//	public void setProcessTypeId(Integer processTypeId) {
//		this.processTypeId = processTypeId;
//	}
//
//	public ProcessType getProcessType() {
//		if (processType == null) {
//			processType = (ProcessType) DataServiceProvider.getProcessTypeDataServiceImplInstance().getById(this.processTypeId);
//		}
//		return processType;
//	}
//
//	public void setProcessType(ProcessType processType) {
//		this.processType = processType;
//	}
//
//	public Integer getBookingPeriodLength() {
//		return bookingPeriodLength;
//	}
//
//	public void setBookingPeriodLength(Integer bookingPeriodLength) {
//		this.bookingPeriodLength = bookingPeriodLength;
//	}
//
//	public Integer getIncentiveTypeId() {
//		return incentiveTypeId;
//	}
//
//	public void setIncentiveTypeId(Integer incentiveTypeId) {
//		this.incentiveTypeId = incentiveTypeId;
//	}
//
//	public IncentiveType getIncentiveType() {
//		if (incentiveType == null) {
//			incentiveType = (IncentiveType) DataServiceProvider.getIncentiveTypeDataServiceImplInstance()
//					.getById(incentiveTypeId);
//		}
//		return incentiveType;
//	}
//
//	public void setIncentiveType(IncentiveType incentiveType) {
//		this.incentiveType = incentiveType;
//	}
//
//	public String getDescription() {
//		return description;
//	}
//
//	public void setDescription(String description) {
//		this.description = description;
//	}
//
	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date=date;
		
	}
//
//	public ArrayList<Entity> getVehicles() {
//		
//		if(this.vehicles==null){
//			this.vehicles= DataServiceProvider.getSettingsDataServiceImplInstance().getVehiclesByRun(this);
//		}
//		return vehicles;
//	}
//
//	public void setVehicles(ArrayList<Entity> vehicles2) {
//		this.vehicles = vehicles2;
//	}
//	
//	public void addVehicle(Vehicle vehicle){
//		if(this.vehicles==null){
//			this.vehicles=new ArrayList<Entity>();
//		}
//		this.vehicles.add(vehicle);
//	}
//
//	public Integer getBookingPeriodNumber() {
//		return bookingPeriodNumber;
//	}
//
//	public void setBookingPeriodNumber(Integer bookingPeriodNumber) {
//		this.bookingPeriodNumber = bookingPeriodNumber;
//	}

	public Integer getExperimentId() {
		return experimentId;
	}

	public void setExperimentId(Integer experimentId) {
		this.experimentId = experimentId;
	}

	public Integer getRuntime() {
		return runtime;
	}

	public void setRuntime(Integer runtime) {
		this.runtime = runtime;
	}

	
}
