package data.entity;

/**
 * Variables of parametric demand models
 * @author M. Lang
 *
 */
public class VariableType extends Entity{

		private int id;
		private String name;

		
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

		
}
