package data.entity;

import data.utility.DataServiceProvider;

public class ArrivalProcess extends Entity {

	private Integer id;
	private String name;
	private Double factor;
	private Integer probabilityDistributionId;
	private ProbabilityDistribution probabilityDistribution;

	

	public int getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}


	public Integer getProbabilityDistributionId() {
		return probabilityDistributionId;
	}

	public void setProbabilityDistributionId(Integer probabilityDistributionId) {
		this.probabilityDistributionId = probabilityDistributionId;
	}

	public ProbabilityDistribution getProbabilityDistribution() {
		if (this.probabilityDistribution == null) {
			this.probabilityDistribution = (ProbabilityDistribution) DataServiceProvider
					.getProbabilityDistributionDataServiceImplInstance().getById(this.probabilityDistributionId);
		}
		return probabilityDistribution;
	}

	public void setProbabilityDistribution(ProbabilityDistribution probabilityDistribution) {
		this.probabilityDistribution = probabilityDistribution;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Double getFactor() {
		return factor;
	}

	public void setFactor(Double factor) {
		this.factor = factor;
	}

	public String toString(){
		return id+"; "+name;
	}
	
}
