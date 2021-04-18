package logic.entity;

public class InsertionCriterion {

	private int position;
	private double insertionCosts;
	private double score;
	private double criterion;
	
	public InsertionCriterion(){
		
	};
	
	
	public InsertionCriterion (int position, double insertionCosts, int score, double criterion){
		this.position = position;
		this.insertionCosts = insertionCosts;
		this.score = score;
		this.criterion = criterion;
	}
	
	public int getPosition() {
		return position;
	}


	public void setPosition(int position) {
		this.position = position;
	}
	
	
	public double getInsertionCosts() {
		return insertionCosts;
	}


	public void setInsertionCosts(double insertionCosts) {
		this.insertionCosts = insertionCosts;
	}


	public double getScore() {
		return score;
	}


	public void setScore(Double score) {
		this.score = score;
	}


	public Double getCriterion() {
		return criterion;
	}


	public void setCriterion(Double criterion) {
		this.criterion = criterion;
	}



	
	
	
	
	
	
}
