package data.entity;

import java.util.ArrayList;

import data.utility.DataServiceProvider;

public class Node extends Entity{
	
	private Long id;//Special id from openstreetmap or other
	private Double lat;
	private Double lon;
	private Integer regionId;
	private Region region;
	private ArrayList<NodeDistance> distances;
	
	/**
	 * Dummy function
	 * TODO: Solve parent class problem
	 */
	public int getId() {
		return 0;
	}
	
	public long getLongId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	public Double getLat() {
		return lat;
	}
	public void setLat(Double lat) {
		this.lat = lat;
	}
	public Double getLon() {
		return lon;
	}
	public void setLon(Double lon) {
		this.lon = lon;
	}
	public Integer getRegionId() {
		return regionId;
	}
	public void setRegionId(Integer regionId) {
		this.regionId = regionId;
	}
	public Region getRegion() {
		if(this.region==null){
			this.region = (Region) DataServiceProvider.getRegionDataServiceImplInstance().getById(this.regionId);
		}
		return this.region;
	}
	public void setRegion(Region region) {
		this.region = region;
	}
	public ArrayList<NodeDistance> getDistances() {
		if(this.distances==null){
			this.distances= DataServiceProvider.getRegionDataServiceImplInstance().getNodeDistancesByNodeId(this.id);
		}
		return distances;
	}
	public void setDistances(ArrayList<NodeDistance> distances) {
		this.distances = distances;
	}
	
	
}
