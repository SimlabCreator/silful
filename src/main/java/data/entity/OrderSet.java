package data.entity;

import java.util.ArrayList;
import java.util.HashMap;

import data.utility.DataServiceProvider;

public class OrderSet extends SetEntity{
	
	
	private String name;
	private ArrayList<Order> orders;
	private OrderRequestSet orderRequestSet;
	private Integer orderRequestSetId;
	private ControlSet controlSet;
	private Integer controlSetId;
	private ArrayList<ReferenceRouting> referenceRoutingsPerDeliveryArea;
	

	public OrderRequestSet getOrderRequestSet() {
		if(this.orderRequestSet==null){
			this.orderRequestSet= (OrderRequestSet) DataServiceProvider.getOrderRequestDataServiceImplInstance().getSetById(this.orderRequestSetId);
		}
		return orderRequestSet;
	}
	public void setOrderRequestSet(OrderRequestSet orderRequestSet) {
		this.orderRequestSet = orderRequestSet;
	}
	public Integer getOrderRequestSetId() {
		return orderRequestSetId;
	}
	public void setOrderRequestSetId(Integer orderRequestSetId) {
		this.orderRequestSetId = orderRequestSetId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public ArrayList<Order> getElements() {
		
		if(this.orders==null){
			this.orders= DataServiceProvider.getOrderDataServiceImplInstance().getAllElementsBySetId(this.id);
		}
		
		return orders;
	}
	

	public void setElements(ArrayList<Order> elements) {
		this.orders=elements;
		
	}
	
	
	@Override
	public String toString() {
		
		return id+"; "+name;
	}
	public ControlSet getControlSet() {
		if(this.controlSet==null){
			this.controlSet=(ControlSet) DataServiceProvider.getControlDataServiceImplInstance().getSetById(this.controlSetId);
		}
		return controlSet;
	}
	public void setControlSet(ControlSet controlSet) {
		this.controlSet = controlSet;
	}
	public Integer getControlSetId() {
		return controlSetId;
	}
	public void setControlSetId(Integer controlSetId) {
		this.controlSetId = controlSetId;
	}
	
	public ArrayList<ReferenceRouting> getReferenceRoutingsPerDeliveryArea() {
		if(this.referenceRoutingsPerDeliveryArea==null){
			this.referenceRoutingsPerDeliveryArea = DataServiceProvider.getOrderDataServiceImplInstance().getReferenceRoutingsByOrderSetId(this.id);
		}
		return this.referenceRoutingsPerDeliveryArea;
	}
	
	public void setReferenceRoutingsPerDeliveryArea(ArrayList<ReferenceRouting> referenceRoutingsPerDeliveryArea) {
		this.referenceRoutingsPerDeliveryArea = referenceRoutingsPerDeliveryArea;
	}
	
	

}
