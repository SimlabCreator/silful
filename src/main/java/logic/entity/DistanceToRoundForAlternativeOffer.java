package logic.entity;

import data.entity.AlternativeOffer;

public class DistanceToRoundForAlternativeOffer extends DistanceToRound{

		private AlternativeOffer offer;
		private double distance;
		
		public DistanceToRoundForAlternativeOffer(AlternativeOffer offer, double distance){
			this.offer=offer;
			this.distance=distance;
		}
		public AlternativeOffer getOffer() {
			return offer;
		}

		public double getDistance() {
			return distance;
		}
		
		

}
