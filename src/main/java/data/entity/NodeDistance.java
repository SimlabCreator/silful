package data.entity;

import data.utility.DataServiceProvider;

public class NodeDistance extends Entity {

	private Long node1Id;
	private Node node1;
	private Long node2Id;
	private Node node2;
	private Integer regionId;
	private Region region;
	private Double distance;
	
	private int id;
	// No functionality, only for parent-class
	public int getId() {
		return id;
	}
	
	public Long getNode1Id() {
		return node1Id;
	}

	public void setNode1Id(Long node1Id) {
		this.node1Id = node1Id;
	}

	public Long getNode2Id() {
		return node2Id;
	}

	public void setNode2Id(Long node2Id) {
		this.node2Id = node2Id;
	}

	public Double getDistance() {
		return distance;
	}

	public void setDistance(Double distance) {
		this.distance = distance;
	}

	public Node getNode1() {
		if (node1 == null) {
			this.node1 = DataServiceProvider.getRegionDataServiceImplInstance().getNodeById(this.node1Id, this.regionId);
		}
		return node1;
	}

	public void setNode1(Node node1) {
		this.node1 = node1;
	}

	public Node getNode2() {
		if (node2 == null) {
			this.node2 = DataServiceProvider.getRegionDataServiceImplInstance().getNodeById(this.node2Id, this.regionId);
		}
		return node2;
	}

	public void setNode2(Node node2) {
		this.node2 = node2;
	}

	public Integer getRegionId() {
		return regionId;
	}

	public void setRegionId(Integer regionId) {
		this.regionId = regionId;
	}

	public Region getRegion() {
		if(this.region==null){
			this.region=(Region) DataServiceProvider.getRegionDataServiceImplInstance().getById(this.regionId);
		}
		return region;
	}

	public void setRegion(Region region) {
		this.region = region;
	}

	
}
