package data.entity;

import java.util.ArrayList;

import data.utility.DataServiceProvider;

public class DeliverySet extends SetEntity {

	private String name;
	private ArrayList<Delivery> deliveries;

	@Override
	public ArrayList<Delivery> getElements() {
		if (this.deliveries == null) {
			this.deliveries =  DataServiceProvider.getDeliveryDataServiceImplInstance().getAllElementsBySetId(this.id);
		}
		return deliveries;
	}


	public void setElements(ArrayList<Delivery> elements) {
		this.deliveries =  elements;
	}


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		
		return id+"; "+name;
	}
}
