package data.entity;

public class DemandModelType extends Entity{

		private int id;
		private String name;
		private Boolean parametric;
		private Boolean indepdentent;
		
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
		public Boolean getParametric() {
			return parametric;
		}
		public void setParametric(Boolean parametric) {
			this.parametric = parametric;
		}
		public Boolean getIndepdentent() {
			return indepdentent;
		}
		public void setIndepdentent(Boolean indepdentent) {
			this.indepdentent = indepdentent;
		}
		
		
}
