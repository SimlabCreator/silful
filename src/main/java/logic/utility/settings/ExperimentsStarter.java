package logic.utility.settings;

import logic.utility.settings.largeRegion.tw6.veh6.L6l27_2SUni2cLowHighFlexibleInflexible7525_6;
import logic.utility.settings.largeRegion.tw6.veh6.L6l3_2SUni2cLowHighFlexibleInflexible7525_6;
import logic.utility.settings.largestRegion.tw6.Lst6l27_2SUni2cLowHighFlexibleInflexible7525_10;
import logic.utility.settings.largestRegion.tw6.Lst6l3_2S2c4cLowHighFlexibleInflexible7525_10;
import logic.utility.settings.largestRegion.tw6.Lst6l3_2SUni2cLowHighFlexibleInflexible7525_10;


public class ExperimentsStarter {

	public static void main(String[] args) {


		
		String stringLog="";

		Lst6l3_2S2c4cLowHighFlexibleInflexible7525_10 exp = new Lst6l3_2S2c4cLowHighFlexibleInflexible7525_10();
		stringLog+=exp.runDynamicADP(3);
		
		System.out.println(stringLog);

	}

}
