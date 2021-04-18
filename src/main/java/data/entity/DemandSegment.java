package data.entity;

import java.util.ArrayList;

import data.utility.DataServiceProvider;

public class DemandSegment extends Entity {

	private Integer id;
	private Integer setId;
	private DemandSegmentSet set;
	private Boolean panel;
	private Integer basketValueVolumeRatio;
	private Integer basketValueNoRatio;
	private Integer basketValueDistributionId;
	private ProbabilityDistribution basketValueDistribution;
	private Integer returnProbabilityDistributionId;
	private ProbabilityDistribution returnProbabilityDistribution;
	private Integer residenceAreaWeightingId; 
	private ResidenceAreaWeighting residenceAreaWeighting;
	private Double socialImpactFactor;
	private ArrayList<ConsiderationSetAlternative> considerationSetAlternatives;
	private Integer considerationSetId;
	private ArrayList<VariableCoefficient> variableCoefficients; //utility coefficients for specific attributes like price
	private double segmentSpecificCoefficient;
	private Integer tempOriginalSegment;

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

	public Boolean getPanel() {
		return panel;
	}

	public void setPanel(Boolean panel) {
		this.panel = panel;
	}

	public Integer getBasketValueVolumeRatio() {
		return basketValueVolumeRatio;
	}

	public void setBasketValueVolumeRatio(Integer basketValueVolumeRatio) {
		this.basketValueVolumeRatio = basketValueVolumeRatio;
	}

	public Integer getBasketValueNoRatio() {
		return basketValueNoRatio;
	}

	public void setBasketValueNoRatio(Integer basketValueNoRatio) {
		this.basketValueNoRatio = basketValueNoRatio;
	}
	
	public Integer getBasketValueDistributionId() {
		return basketValueDistributionId;
	}

	public void setBasketValueDistributionId(Integer basketValueDistributionId) {
		this.basketValueDistributionId = basketValueDistributionId;
	}

	public ProbabilityDistribution getBasketValueDistribution() {
		
		if (this.basketValueDistribution == null) {
			this.basketValueDistribution = (ProbabilityDistribution) DataServiceProvider
					.getProbabilityDistributionDataServiceImplInstance().getById(this.basketValueDistributionId);
		}
		
		return basketValueDistribution;
	}

	public void setBasketValueDistribution(ProbabilityDistribution basketValueDistribution) {
		this.basketValueDistribution = basketValueDistribution;
	}

	public Integer getReturnProbabilityDistributionId() {
		return returnProbabilityDistributionId;
	}

	public void setReturnProbabilityDistributionId(Integer returnProbabilityDistributionId) {
		this.returnProbabilityDistributionId = returnProbabilityDistributionId;
	}

	public ProbabilityDistribution getReturnProbabilityDistribution() {
		
		if (this.returnProbabilityDistribution == null) {
			this.returnProbabilityDistribution = (ProbabilityDistribution) DataServiceProvider
					.getProbabilityDistributionDataServiceImplInstance().getById(this.returnProbabilityDistributionId);
		}
		
		
		return returnProbabilityDistribution;
	}

	public void setReturnProbabilityDistribution(ProbabilityDistribution returnProbabilityDistribution) {
		this.returnProbabilityDistribution = returnProbabilityDistribution;
	}

	public Integer getResidenceAreaWeightingId() {
		return residenceAreaWeightingId;
	}

	public void setResidenceAreaWeightingId(Integer residenceAreaWeightingId) {
		this.residenceAreaWeightingId = residenceAreaWeightingId;
	}

	public ArrayList<ConsiderationSetAlternative> getConsiderationSet() {
		if(this.considerationSetAlternatives==null && this.considerationSetId==null){
			this.considerationSetAlternatives= DataServiceProvider.getDemandSegmentDataServiceImplInstance().getConsiderationSetAlternativesByDemandSegmentId(this.id);
		}else if(this.considerationSetAlternatives==null){
			this.considerationSetAlternatives= DataServiceProvider.getDemandSegmentDataServiceImplInstance().getConsiderationSetAlternativesBySetId(this.considerationSetId);
		}
		return considerationSetAlternatives;
	}

	public void setConsiderationSet(ArrayList<ConsiderationSetAlternative> considerationSet) {
		this.considerationSetAlternatives = considerationSet;
	}

	public ResidenceAreaWeighting getResidenceAreaWeighting() {
		if(this.residenceAreaWeighting==null){
			this.residenceAreaWeighting= (ResidenceAreaWeighting) DataServiceProvider.getResidenceAreaDataServiceImplInstance().getWeightingById(this.residenceAreaWeightingId);
		}
		return residenceAreaWeighting;
	}

	public void setResidenceAreaWeighting(ResidenceAreaWeighting residenceAreaWeighting) {
		this.residenceAreaWeighting = residenceAreaWeighting;
	}

	public ArrayList<VariableCoefficient> getVariableCoefficients() {
		if(this.variableCoefficients==null){
			this.variableCoefficients=DataServiceProvider.getDemandSegmentDataServiceImplInstance().getVariableCoefficientsByDemandSegmentId(this.id);
		}
		return variableCoefficients;
	}
	
	public VariableCoefficient getVariableCoefficientByName(String name){
		ArrayList<VariableCoefficient> coeffs= this.getVariableCoefficients();
		for(VariableCoefficient c: coeffs){
			if(c.getVariableType().getName().equals(name)) return c;
		}
		return null;
	}

	public void setVariableCoefficients(ArrayList<VariableCoefficient> variableCoefficients) {
		this.variableCoefficients = variableCoefficients;
	}

	public DemandSegmentSet getSet() {
		
		if(this.set==null){
			this.set=(DemandSegmentSet) DataServiceProvider.getDemandSegmentDataServiceImplInstance().getSetById(this.setId);
		}
		return set;
	}

	public void setSet(DemandSegmentSet set) {
		this.set = set;
	}

	public Double getSocialImpactFactor() {
		return socialImpactFactor;
	}

	public void setSocialImpactFactor(Double socialImpactFactor) {
		this.socialImpactFactor = socialImpactFactor;
	}

	public double getSegmentSpecificCoefficient() {
		return segmentSpecificCoefficient;
	}

	public void setSegmentSpecificCoefficient(double segmentSpecificCoefficient) {
		this.segmentSpecificCoefficient = segmentSpecificCoefficient;
	}

	@Override
	public boolean equals(Object o){
	   if(this==o){
	      return true;
	   }
	   if(o instanceof DemandSegment){
		   DemandSegment other = (DemandSegment) o;
	       return this.id == other.getId();
	   }
	   return false;
	}
	
	@Override
	public int hashCode(){
	   return this.id;
	}

	public DemandSegment copyWithoutId(int demandSegmentSetId){
		DemandSegment ds = new DemandSegment();
		ds.setSetId(demandSegmentSetId);
		ds.setBasketValueDistributionId(this.getBasketValueDistributionId());
		ds.setConsiderationSet(this.getConsiderationSet());
		ds.setPanel(this.panel);
		ds.setResidenceAreaWeightingId(this.residenceAreaWeightingId);
		ds.setReturnProbabilityDistributionId(this.returnProbabilityDistributionId);
		ds.setSegmentSpecificCoefficient(this.segmentSpecificCoefficient);
		ds.setSocialImpactFactor(this.socialImpactFactor);
		ds.setBasketValueNoRatio(this.basketValueNoRatio);
		ds.setBasketValueVolumeRatio(this.basketValueVolumeRatio);
		ds.setVariableCoefficients(this.getVariableCoefficients());
		return ds;
	}

	public Integer getTempOriginalSegment() {
		return tempOriginalSegment;
	}

	public void setTempOriginalSegment(Integer tempOriginalSegment) {
		this.tempOriginalSegment = tempOriginalSegment;
	}

	public Integer getConsiderationSetId() {
		return considerationSetId;
	}

	public void setConsiderationSetId(Integer considerationSetId) {
		this.considerationSetId = considerationSetId;
	}

	
}
