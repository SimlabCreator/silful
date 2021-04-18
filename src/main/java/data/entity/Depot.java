package data.entity;

import data.utility.DataServiceProvider;

public class Depot extends Entity{
private Integer id;
private Double lat;
private Double lon;
private Integer regionId;
private Region region;
public int getId() {
	return id;
}
public void setId(Integer id) {
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
	return region;
}
public void setRegion(Region region) {
	this.region = region;
}

public String toString(){
	return this.id+"; Lat:"+this.lat+"; Lon:"+this.lon;
}

}
