package data.entity;

import java.util.ArrayList;

import data.utility.DataServiceProvider;

public class CustomerSet extends SetEntity{
	
	private Integer id;
	private String name;
	private Boolean panel;
	private Boolean extension;
	private ArrayList<Customer> customers;
	private DemandSegmentSet originalDemandSegmentSet;
	private Integer originalDemandSegmentSetId;
	
	
	
	@Override
	public ArrayList<Customer> getElements() {
		if(this.customers==null){
			this.customers=DataServiceProvider.getCustomerDataServiceImplInstance().getAllElementsBySetId(id);
		}
		return this.customers;
	}
	
	public void setElements(ArrayList<Customer> elements) {
		this.customers=elements;
		
	}
	
	
	public Boolean getPanel() {
		return panel;
	}
	
	public void setPanel(Boolean panel) {
		this.panel = panel;
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Boolean getExtension() {
		return extension;
	}
	public void setExtension(Boolean extension) {
		this.extension = extension;
	}
	
	@Override
	public String toString() {
		
		return id+"; "+name+"; Panel:"+panel;
	}

	public DemandSegmentSet getOriginalDemandSegmentSet() {
		if(this.originalDemandSegmentSet==null){
			this.originalDemandSegmentSet=(DemandSegmentSet) DataServiceProvider.getDemandSegmentDataServiceImplInstance().getSetById(this.getOriginalDemandSegmentSetId());
		}
		return originalDemandSegmentSet;
	}

	public void setOriginalDemandSegmentSet(DemandSegmentSet originalDemandSegmentSet) {
		this.originalDemandSegmentSet = originalDemandSegmentSet;
	}

	public Integer getOriginalDemandSegmentSetId() {
		return originalDemandSegmentSetId;
	}

	public void setOriginalDemandSegmentSetId(Integer originalDemandSegmentSetId) {
		this.originalDemandSegmentSetId = originalDemandSegmentSetId;
	}

}
