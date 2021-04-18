package logic.algorithm.vr.ALNS;

import java.util.Arrays;
import java.util.Random;

/**
 * 
 * Roulette wheel selection
 * @author J. Haferkamp
 *
 */

public class RouletteWheelSelection {
	
	private Random random = new Random();
	private int[] rouletteWheel;
	private int[] currentScores;
	private int[] overallScores;
	private int[] heuristicsFrequency;
	private int wheelSize;
	private double lastScoreInfluence;
	private int selcetedHeuristic;

	// initialized roulette wheel and scores 
	public RouletteWheelSelection (int number) {	
		heuristicsFrequency = new int[number];
		currentScores = new int[number];
		overallScores = new int[number];
		rouletteWheel = new int[number];
		wheelSize = number;
		lastScoreInfluence = 0.9;
		Arrays.fill(heuristicsFrequency, 1);
		Arrays.fill(overallScores, 1);

		
		for (int i = 0; i < rouletteWheel.length; i++) rouletteWheel[i] = i;
	} 
	
	// update scores from last used heuristic
	public void updateScore(int reason) {
		switch (reason) {
			case 1: currentScores[selcetedHeuristic] = currentScores[selcetedHeuristic] + 33;
					break;
	    	case 2: currentScores[selcetedHeuristic] = currentScores[selcetedHeuristic] + 9;
	    			break;
	    	case 3: currentScores[selcetedHeuristic] = currentScores[selcetedHeuristic] + 13;
	    			break;
		}
	}
	
	// calculates new wheel segments for each Heuristic
	public void updateWheel() {
		
		overallScores[0] = (int) Math.ceil((overallScores[0]*lastScoreInfluence) + (currentScores[0]/heuristicsFrequency[0])); 
		rouletteWheel[0] = overallScores[0];
				
		for (int i = 1; i < overallScores.length; i++) {				
			overallScores[i] = (int) Math.ceil((overallScores[i]*lastScoreInfluence) + (currentScores[i]/heuristicsFrequency[i])); 
			rouletteWheel[i] =  rouletteWheel[i-1] + overallScores[i];
		}
		
		wheelSize = rouletteWheel[rouletteWheel.length-1];
		currentScores = new int[6];
		Arrays.fill(heuristicsFrequency, 1); 
	}
	
	// selects heuristic via current roulette-wheel 
	public int selectHeuristic() {		
		int rouletteSelection;
		if (wheelSize > 1) rouletteSelection = random.nextInt(wheelSize);
		else  rouletteSelection = 0;
		
		for (int i = 0; i < rouletteWheel.length; i++) {
			if (rouletteWheel[i] >= rouletteSelection) {
				selcetedHeuristic = i;
				break;
			}
		} 
		heuristicsFrequency[selcetedHeuristic] = heuristicsFrequency[selcetedHeuristic] + 1;
		return selcetedHeuristic; 
	}
}