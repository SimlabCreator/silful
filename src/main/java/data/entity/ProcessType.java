package data.entity;

public class ProcessType extends Entity{

	private int id;
	private String name;
	private String description;

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

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public String toString(){
		String string = id+"; "+name+"; "+description;
		return string;
	}
}
