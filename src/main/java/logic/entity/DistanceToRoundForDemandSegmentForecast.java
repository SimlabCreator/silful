package logic.entity;

import data.entity.DemandSegmentForecast;

public class DistanceToRoundForDemandSegmentForecast extends DistanceToRound{

		private DemandSegmentForecast segmentForecast;
		private double distance;
		
		public DistanceToRoundForDemandSegmentForecast(DemandSegmentForecast segmentForecast, double distance){
			this.segmentForecast=segmentForecast;
			this.distance=distance;
		}

		public double getDistance() {
			return distance;
		}

		public DemandSegmentForecast getSegmentForecast() {
			return segmentForecast;
		}

		
		

}
