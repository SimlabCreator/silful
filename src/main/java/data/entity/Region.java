package data.entity;

import java.util.ArrayList;

import data.utility.DataServiceProvider;

public class Region extends Entity{
	
	private int id;
	private String name;
	private double lat1;
	private double lon1;
	private double lat2;
	private double lon2;
	private ArrayList<Node> nodes;
	private double averageKmPerHour;
	
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
	public double getLat1() {
		return lat1;
	}
	public void setLat1(double lat) {
		this.lat1 = lat;
	}
	public double getLon1() {
		return lon1;
	}
	public void setLon1(double lon) {
		this.lon1 = lon;
	}
	
	public double getLat2() {
		return lat2;
	}
	public void setLat2(double lat) {
		this.lat2 = lat;
	}
	public double getLon2() {
		return lon2;
	}
	public void setLon2(double lon) {
		this.lon2 = lon;
	}
	public ArrayList<Node> getNodes() {
		if(this.nodes==null){
			this.nodes= DataServiceProvider.getRegionDataServiceImplInstance().getNodesByRegionId(this.id);
		}
		return nodes;
	}
	public void setNodes(ArrayList<Node> nodes) {
		this.nodes = nodes;
	}
	
	public String toString(){
		String string = id+"; "+name;
		return string;
	}
	public double getAverageKmPerHour() {
		return averageKmPerHour;
	}
	public void setAverageKmPerHour(double averageKmPerHour) {
		this.averageKmPerHour = averageKmPerHour;
	}

}
