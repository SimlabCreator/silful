package data.entity;

import java.util.ArrayList;

import data.utility.DataServiceProvider;

public class DemandSegmentSet extends SetEntity {

	private String name;
	private ArrayList<DemandSegment> demandSegments;
	private Boolean panel;
	private Integer demandModelTypeId;
	private DemandModelType demandModelType;
	private ResidenceAreaSet residenceAreaSet;
	private Integer residenceAreaSetId;
	private Integer alternativeSetId;
	private AlternativeSet alternativeSet;
	
	
	public DemandSegment getDemandSegmentById(int demandSegmentId){
		if(this.demandSegments==null){
			this.getElements();
		}
		
		for(DemandSegment ds: this.demandSegments){
			if(ds.getId()==demandSegmentId) return ds;
		}
		return null;
	}

	public Integer getAlternativeSetId() {
		return alternativeSetId;
	}

	public void setAlternativeSetId(Integer alternativeSetId) {
		this.alternativeSetId = alternativeSetId;
	}

	public AlternativeSet getAlternativeSet() {
		
		if(alternativeSet==null){
			this.alternativeSet=(AlternativeSet) DataServiceProvider.getAlternativeDataServiceImplInstance().getSetById(this.alternativeSetId);
		}
		return alternativeSet;
	}

	public void setAlternativeSet(AlternativeSet alternativeSet) {
		this.alternativeSet = alternativeSet;
	}

	@Override
	public ArrayList<DemandSegment> getElements() {
		
		if(this.demandSegments==null){
			
			this.demandSegments =  DataServiceProvider.getDemandSegmentDataServiceImplInstance().getAllElementsBySetId(this.id);
		}
		return demandSegments;
	}


	public void setElements(ArrayList<DemandSegment> elements) {
		this.demandSegments = elements;

	}


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}


	public Boolean getPanel() {
		return panel;
	}

	public void setPanel(Boolean panel) {
		this.panel = panel;
	}

	public Integer getDemandModelTypeId() {
		return demandModelTypeId;
	}

	public void setDemandModelTypeId(Integer demandModelTypeId) {
		this.demandModelTypeId = demandModelTypeId;
	}

	public DemandModelType getDemandModelType() {
		if(demandModelType==null){
			this.demandModelType=(DemandModelType) DataServiceProvider.getDemandModelTypeDataServiceImplInstance().getById(this.demandModelTypeId);
		}
		
		return this.demandModelType;
	}

	public void setDemandModelType(DemandModelType demandModelType) {
		this.demandModelType = demandModelType;
	}

	public ResidenceAreaSet getResidenceAreaSet() {

		if(residenceAreaSet==null){
			this.residenceAreaSet=(ResidenceAreaSet) DataServiceProvider.getResidenceAreaDataServiceImplInstance().getSetById(this.residenceAreaSetId);
		}
		return residenceAreaSet;
	}

	public void setResidenceAreaSet(ResidenceAreaSet residenceAreaSet) {
		
		
		this.residenceAreaSet = residenceAreaSet;
	}

	public Integer getResidenceAreaSetId() {
		return residenceAreaSetId;
	}

	public void setResidenceAreaSetId(Integer residenceAreaSetId) {
		this.residenceAreaSetId = residenceAreaSetId;
	}

	@Override
	public String toString() {
		
		return id+"; "+name+"; Panel:"+panel;
	}
	
	@Override
	public boolean equals(Object o){
	   if(this==o){
	      return true;
	   }
	   if(o instanceof DemandSegmentSet){
		   DemandSegmentSet other = (DemandSegmentSet) o;
	       return this.id == other.getId();
	   }
	   return false;
	}
	
	@Override
	public int hashCode(){
	   return this.id;
	}

	public DemandSegmentSet copyWithoutIdAndElements(){
		DemandSegmentSet set = new DemandSegmentSet();
		set.setAlternativeSetId(this.alternativeSetId);
		set.setDemandModelTypeId(this.demandModelTypeId);
		set.setPanel(this.panel);
		set.setResidenceAreaSetId(this.residenceAreaSetId);
		set.setElements(new ArrayList<DemandSegment>());
		
		return set;
	}
}
