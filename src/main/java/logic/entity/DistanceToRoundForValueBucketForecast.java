package logic.entity;

import data.entity.ValueBucketForecast;

public class DistanceToRoundForValueBucketForecast extends DistanceToRound{

		private ValueBucketForecast valueBucketForecast;
		private double distance;
		
		public DistanceToRoundForValueBucketForecast(ValueBucketForecast valueBucketForecast, double distance){
			this.valueBucketForecast=valueBucketForecast;
			this.distance=distance;
		}

		public double getDistance() {
			return distance;
		}

		public ValueBucketForecast getValueBucketForecast() {
			return valueBucketForecast;
		}

		
		

}
