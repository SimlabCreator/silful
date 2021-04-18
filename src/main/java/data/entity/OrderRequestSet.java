package data.entity;

import java.util.ArrayList;

import data.utility.DataServiceProvider;

public class OrderRequestSet extends SetEntity{
	

	private String name;
	private ArrayList<OrderRequest> orderRequests;
	private Integer customerSetId;
	private CustomerSet customerSet;
	private Integer bookingHorizon;
	private Boolean preferencesSampled;
	
	
	
	public Integer getCustomerSetId() {
		return customerSetId;
	}
	public void setCustomerSetId(Integer customerSetId) {
		this.customerSetId = customerSetId;
	}
	public CustomerSet getCustomerSet() {
		if(this.customerSet==null){
			this.customerSet=(CustomerSet) DataServiceProvider.getCustomerDataServiceImplInstance().getSetById(this.customerSetId);
		}
		return customerSet;
	}
	public void setCustomerSet(CustomerSet customerSet) {
		this.customerSet = customerSet;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public ArrayList<OrderRequest> getElements() {
		
		if(this.orderRequests==null){
			this.orderRequests=DataServiceProvider.getOrderRequestDataServiceImplInstance().getAllElementsBySetId(this.id);
		}
		
		return orderRequests;
	}

	
	public void setElements(ArrayList<OrderRequest> elements) {
		this.orderRequests=elements;
		
	}
	
	
	
	@Override
	public String toString() {
		
		return id+"; "+name;
	}
	public Integer getBookingHorizon() {
		return bookingHorizon;
	}
	public void setBookingHorizon(Integer bookingHorizon) {
		this.bookingHorizon = bookingHorizon;
	}
	public Boolean getPreferencesSampled() {
		return preferencesSampled;
	}
	public void setPreferencesSampled(Boolean preferencesSampled) {
		this.preferencesSampled = preferencesSampled;
	}
	

}
