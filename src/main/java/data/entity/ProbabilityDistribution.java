package data.entity;

import java.util.ArrayList;

import data.utility.DataServiceProvider;

public class ProbabilityDistribution extends Entity{

		private int id;
		private String name;
		private Integer probabilityDistributionTypeId;
		private ProbabilityDistributionType probabilityDistributionType;
		private ArrayList<DistributionParameterValue> parameterValues;
		
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
		public ArrayList<DistributionParameterValue> getParameterValues() {
			
			if(this.parameterValues==null){
				this.parameterValues= DataServiceProvider.getProbabilityDistributionDataServiceImplInstance().getParameterValuesByProbabilityDistributionId(this.id);
			}
			return parameterValues;
		}
		public void setParameterValues(ArrayList<DistributionParameterValue> parameterValues) {
			this.parameterValues = parameterValues;
		}
		public Integer getProbabilityDistributionTypeId() {
			return probabilityDistributionTypeId;
		}
		public void setProbabilityDistributionTypeId(Integer probabilityDistributionTypeId) {
			this.probabilityDistributionTypeId = probabilityDistributionTypeId;
		}
		public ProbabilityDistributionType getProbabilityDistributionType() {
			
			if(this.probabilityDistributionType==null){
				this.probabilityDistributionType= (ProbabilityDistributionType) DataServiceProvider.getProbabilityDistributionTypeDataServiceImplInstance().getById(this.probabilityDistributionTypeId);
			}
			return probabilityDistributionType;
		}
		public void setProbabilityDistributionType(ProbabilityDistributionType probabilityDistributionType) {
			this.probabilityDistributionType = probabilityDistributionType;
		}
		
		public String toString(){
			return this.id+"; "+this.name;
		}
		
}
