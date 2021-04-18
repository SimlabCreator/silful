package data.entity;

public class IncentiveType extends Entity{

	private int id;
	private String name;

	public void setId(Integer id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	public String toString(){
		return id+"; "+name;
	}

}
