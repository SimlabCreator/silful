package logic.service.algorithmProvider;

import data.utility.Output;
import data.utility.SettingRequest;

public interface AlgorithmProviderService {

	/**
	 * Provides the relevant settings for the given algorithm.
	 * @return
	 */
	public abstract SettingRequest getSettingRequest();
	
	/**
	 * Provides the information about the output this algorithm provides in terms of period settings
	 * @return
	 */
	public abstract Output getOutput();
}
