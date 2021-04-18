package data.entity;

import java.util.ArrayList;

import data.utility.DataServiceProvider;

public class ConsiderationSet extends Entity {

	private int id;
	private Integer alternativeSetId;
	private AlternativeSet alternativeSet;
	private ArrayList<ConsiderationSetAlternative> csAlternatives;

	public Integer getAlternativeSetId() {
		return alternativeSetId;
	}

	public void setAlternativeSetId(Integer alternativeSetId) {
		this.alternativeSetId = alternativeSetId;
	}

	public AlternativeSet getAlternativeSet() {
		return alternativeSet;
	}

	public void setAlternativeSet(AlternativeSet alternativeSet) {
		this.alternativeSet = alternativeSet;
	}

	@Override
	public int getId() {
		return id;
	}

	public ArrayList<ConsiderationSetAlternative> getCsAlternatives() {
		
		if(csAlternatives==null)
			csAlternatives=DataServiceProvider.getDemandSegmentDataServiceImplInstance().
			getConsiderationSetAlternativesBySetId(this.id);
		return csAlternatives;
	}

	public void setCsAlternatives(ArrayList<ConsiderationSetAlternative> csAlternatives) {
		this.csAlternatives = csAlternatives;
	}

}
