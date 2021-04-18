package data.entity;

import java.util.ArrayList;

public abstract class SetEntity extends Entity{
	
	protected int id;
	
	public void setId(Integer id) {
		this.id = id;
	}

	public int getId() {
		return id;
	} 
	
	/**
	 * Get all elements from the given set. E.g. all customers of a customer set
	 * @return Elements
	 */
	public abstract ArrayList<? extends Entity> getElements();
	
	
	/**
	 * Allows to print the information of the respective set
	 */
	public abstract String toString();


	
	

}
