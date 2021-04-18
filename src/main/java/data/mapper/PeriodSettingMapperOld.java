package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.PeriodSetting;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class PeriodSettingMapperOld implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		
		PeriodSetting periodSetting = new PeriodSetting();
		periodSetting.setAlternativeSetId(rs.getInt("as_id"));
		periodSetting.setArrivalProcessId(rs.getInt("arr_id"));
		periodSetting.setCapacitySetId(rs.getInt("cas_id"));
		periodSetting.setControlSetId(rs.getInt("cos_id"));
		periodSetting.setCustomerSetId(rs.getInt("cs_id"));
		periodSetting.setDeliveryAreaSetId(rs.getInt("das_id"));
		periodSetting.setValueBucketForecastSetId(rs.getInt("dfs_id"));
		periodSetting.setDemandSegmentWeightingId(rs.getInt("dsw_id"));
		periodSetting.setOrderRequestSetId(rs.getInt("ors_id"));
		periodSetting.setOrderSetId(rs.getInt("os_id"));
		periodSetting.setServiceSegmentWeightingId(rs.getInt("ssw_id"));
		periodSetting.setStartingPeriod(rs.getInt("period"));
		periodSetting.setRunId(rs.getInt("run_id"));
		periodSetting.setTimeWindowSetId(rs.getInt("tws_id"));
		periodSetting.setHistoricalOrderSetId(rs.getInt("os_historical_id"));
		periodSetting.setValueBucketSetId(rs.getInt("vbs_id"));
			
		return periodSetting;
	}
	
}
