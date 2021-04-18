package data.entity;

import data.utility.DataServiceProvider;

/**
 * Utility coefficient of a parametric demand model variable
 * @author M. Lang
 *
 */
public class VariableCoefficient extends Entity{

		private Integer demandSegmentId;
		private DemandSegment demandSegment;
		private Integer variableTypeId;
		private VariableType variableType;
		private Double coefficientValue;
		
		private int id;
		// No functionality, only for parent-class
		public int getId() {
			return id;
		}
		
		public Integer getDemandSegmentId() {
			return demandSegmentId;
		}
		public void setDemandSegmentId(Integer demandSegmentId) {
			this.demandSegmentId = demandSegmentId;
		}
		public DemandSegment getDemandSegment() {
			if(this.demandSegment==null){
				this.demandSegment= DataServiceProvider.getDemandSegmentDataServiceImplInstance().getElementById(this.demandSegmentId);
			}
			return demandSegment;
		}
		public void setDemandSegment(DemandSegment demandSegment) {
			this.demandSegment = demandSegment;
		}
		public Integer getVariableTypeId() {
			return variableTypeId;
		}
		public void setVariableTypeId(Integer variableTypeId) {
			this.variableTypeId = variableTypeId;
		}
		public VariableType getVariableType() {
			if(this.variableType==null){
				this.variableType=(VariableType) DataServiceProvider.getVariableTypeDataServiceImplInstance().getById(this.variableTypeId);
			}
			return variableType;
		}
		public void setVariableType(VariableType variableType) {
			this.variableType = variableType;
		}
		public Double getCoefficientValue() {
			return coefficientValue;
		}
		public void setCoefficientValue(Double coefficientValue) {
			this.coefficientValue = coefficientValue;
		}

		
		
		
}
