package data.entity;

import data.utility.DataServiceProvider;

public class DeliveryArea extends Entity{
	
	private int id;
	private int setId;
	private double lat1;
	private double lon1;
	private double lat2;
	private double lon2;
	private double centerLat;
	private double centerLon;
	private Node tempClosestNodeCenter;
	private Integer subsetId;
	private DeliveryAreaSet subset;
	private DeliveryArea deliveryAreaOfSet; //Needed if set is a subset
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getSetId() {
		return setId;
	}
	public void setSetId(int setId) {
		this.setId = setId;
	}
	public double getLat1() {
		return lat1;
	}
	public void setLat1(double lat1) {
		this.lat1 = lat1;
	}
	public double getLon1() {
		return lon1;
	}
	public void setLon1(double lon1) {
		this.lon1 = lon1;
	}
	public double getLat2() {
		return lat2;
	}
	public void setLat2(double lat2) {
		this.lat2 = lat2;
	}
	public double getLon2() {
		return lon2;
	}
	public void setLon2(double lon2) {
		this.lon2 = lon2;
	}
	public double getCenterLat() {
		return centerLat;
	}
	public void setCenterLat(double centerLat) {
		this.centerLat = centerLat;
	}
	public double getCenterLon() {
		return centerLon;
	}
	public void setCenterLon(double centerLon) {
		this.centerLon = centerLon;
	}
	public Node getTempClosestNodeCenter() {
		return tempClosestNodeCenter;
	}
	public void setTempClosestNodeCenter(Node tempClosestNodeCenter) {
		this.tempClosestNodeCenter = tempClosestNodeCenter;
	}
	public Integer getSubsetId() {
		return subsetId;
	}
	public void setSubsetId(Integer subsetId) {
		this.subsetId = subsetId;
	}
	public DeliveryAreaSet getSubset() {
		
		if(this.subset==null){
			if(this.setId!=0){
				this.subset = (DeliveryAreaSet) DataServiceProvider.getDeliveryAreaDataServiceImplInstance().getSetById(this.subsetId);
			}
		}
		return subset;
	}
	public void setSubset(DeliveryAreaSet subset) {
		this.subset = subset;
	}
	
	@Override
	public boolean equals(Object o){
	   if(this==o){
	      return true;
	   }
	   if(o instanceof DeliveryArea){
		   DeliveryArea other = (DeliveryArea) o;
	       return this.id == other.getId();//TODO: Reconsider if areas are created on the fly (no id yet)
	   }
	   return false;
	}
	
	@Override
	public int hashCode(){
	   return this.id;
	}
	public DeliveryArea getDeliveryAreaOfSet() {
		if(this.deliveryAreaOfSet==null){
			this.deliveryAreaOfSet=DataServiceProvider.getDeliveryAreaDataServiceImplInstance().getDeliveryAreaBySubsetId(this.setId);
		}
		return deliveryAreaOfSet;
	}
	public void setDeliveryAreaOfSet(DeliveryArea deliveryAreaOfSet) {
		this.deliveryAreaOfSet = deliveryAreaOfSet;
	}
	
	public String toString(){
		return "da"+this.id;
	}

}
