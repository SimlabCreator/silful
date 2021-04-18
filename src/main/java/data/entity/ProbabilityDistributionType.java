package data.entity;

public class ProbabilityDistributionType extends Entity{

		private int id;
		private String name;
	//	private ArrayList<Entity> parameterTypes;
		
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
//		public ArrayList<Entity> getParameterTypes() {
//			
//			if(this.parameterTypes==null){
//				this.parameterTypes=DataServiceProvider.getProbabilityDistributionTypeDataServiceImplInstance().getParameterTypesByProbabilityDistributionTypeId(this.id);
//			}
//			return parameterTypes;
//		}
//		public void setParameterTypes(ArrayList<Entity> parameterTypes) {
//			this.parameterTypes = parameterTypes;
//		}
		
}
