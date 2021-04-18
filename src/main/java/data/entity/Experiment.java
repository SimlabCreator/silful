package data.entity;

import java.util.ArrayList;

import data.utility.DataServiceProvider;

public class Experiment extends Entity {

	private int id;
	private String name;
	private String description;
	private String responsible;
	private String occasion;
	private Region region;
	private Integer regionId;
	private Integer processTypeId;
	private ProcessType processType;
	private Integer bookingPeriodLength;
	private Integer bookingPeriodNumber;
	private Integer incentiveTypeId;//Not always needed, depends on process type
	private IncentiveType incentiveType;//Not always needed, depends on process type
	private Integer depotId;
	private Depot depot; 
	private ArrayList<ObjectiveWeight> objectives;
	private Integer copyExperimentId; 


	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
 
	public String getResponsible() {
		return responsible;
	}

	public void setResponsible(String responsible) {
		this.responsible = responsible;
	}

	public String getOccasion() {
		return occasion;
	}

	public void setOccasion(String occasion) {
		this.occasion = occasion;
	}

	public Region getRegion() {

		if (region == null) {
			region = (Region) DataServiceProvider.getRegionDataServiceImplInstance().getById(regionId);
		}
		return region;
	}

	public void setRegion(Region region) {
		this.region = region;
	}

	public Integer getRegionId() {
		return regionId;
	}

	public void setRegionId(Integer regionId) {
		this.regionId = regionId;
	}

	public Integer getProcessTypeId() {
		return processTypeId;
	}

	public void setProcessTypeId(Integer processTypeId) {
		this.processTypeId = processTypeId;
	}

	public ProcessType getProcessType() {
		if (processType == null) {
			processType = (ProcessType) DataServiceProvider.getProcessTypeDataServiceImplInstance().getById(this.processTypeId);
		}
		return processType;
	}

	public void setProcessType(ProcessType processType) {
		this.processType = processType;
	}

	public Integer getBookingPeriodLength() {
		return bookingPeriodLength;
	}

	public void setBookingPeriodLength(Integer bookingPeriodLength) {
		this.bookingPeriodLength = bookingPeriodLength;
	}

	public Integer getIncentiveTypeId() {
		return incentiveTypeId;
	}

	public void setIncentiveTypeId(Integer incentiveTypeId) {
		this.incentiveTypeId = incentiveTypeId;
	}

	public IncentiveType getIncentiveType() {
		if (incentiveType == null) {
			incentiveType = (IncentiveType) DataServiceProvider.getIncentiveTypeDataServiceImplInstance()
					.getById(incentiveTypeId);
		}
		return incentiveType;
	}

	public void setIncentiveType(IncentiveType incentiveType) {
		this.incentiveType = incentiveType;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}



	
	public Integer getBookingPeriodNumber() {
		return bookingPeriodNumber;
	}

	public void setBookingPeriodNumber(Integer bookingPeriodNumber) {
		this.bookingPeriodNumber = bookingPeriodNumber;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String toString(){
		return this.id+"; "+this.name+"; "+this.occasion+"; "+this.description;
	}

	public Integer getDepotId() {
		return depotId;
	}

	public void setDepotId(Integer depotId) {
		this.depotId = depotId;
	}

	public Depot getDepot() {
		if(this.depot==null){
			
			this.depot=(Depot) DataServiceProvider.getDepotDataServiceImplInstance().getById(this.depotId);
		}
		return depot;
	}

	public void setDepot(Depot depot) {
		this.depot = depot;
	}

	public ArrayList<ObjectiveWeight> getObjectives() {
		
		if(this.objectives==null){
			this.objectives=DataServiceProvider.getSettingsDataServiceImplInstance().getObjectivesByExperiment(this);
		}
		return objectives;
	}

	public void setObjectives(ArrayList<ObjectiveWeight> objectives) {
		this.objectives = objectives;
	}

	public Integer getCopyExperimentId() {
		return copyExperimentId;
	}

	public void setCopyExperimentId(Integer copyExperimentId) {
		this.copyExperimentId = copyExperimentId;
	}
	
	
}
