package data.entity;

import data.utility.DataServiceProvider;

public class TravelTime extends Entity {

	private Integer id;
	private Integer setId;
	private Double start;
	private Double end;
	private Integer probabilityDistributionId;
	private ProbabilityDistribution probabilityDistribution;

	public int getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getSetId() {
		return setId;
	}

	public void setSetId(Integer setId) {
		this.setId = setId;
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

	public Double getStart() {
		return start;
	}

	public void setStart(Double start) {
		this.start = start;
	}

	public Double getEnd() {
		return end;
	}

	public void setEnd(Double end) {
		this.end = end;
	}

	

}
