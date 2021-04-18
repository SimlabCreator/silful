package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.springframework.jdbc.core.RowMapper;

import data.entity.Entity;
import data.entity.PeriodSetting;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class InputPeriodSettingMapper implements RowMapper<Entity> {
	
	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		
		PeriodSetting periodSetting = new PeriodSetting();
		periodSetting.setAlternativeSetId(rs.getObject("exp_as_as", Integer.class));
		periodSetting.setArrivalProcessId(rs.getObject("exp_arp_arp", Integer.class));
		periodSetting.setCapacitySetId(rs.getObject("exp_cas_cas", Integer.class));
		periodSetting.setControlSetId(rs.getObject("exp_cos_cos", Integer.class));
		periodSetting.setCustomerSetId(rs.getObject("exp_cs_cs", Integer.class));
		periodSetting.setDeliveryAreaSetId(rs.getObject("exp_das_das", Integer.class));
		periodSetting.setValueBucketForecastSetId(rs.getObject("exp_vfs_vfs", Integer.class));
		periodSetting.setDemandSegmentForecastSetId(rs.getObject("exp_dfs_dfs", Integer.class));
		periodSetting.setDemandSegmentSetId(rs.getObject("exp_dss_dss", Integer.class));
		periodSetting.setDemandSegmentWeightingId(rs.getObject("exp_dsw_dsw", Integer.class));
		periodSetting.setOrderRequestSetId(rs.getObject("exp_ors_ors", Integer.class));
		periodSetting.setServiceSegmentWeightingId(rs.getObject("exp_ssw_ssw", Integer.class));
		periodSetting.setTimeWindowSetId(rs.getObject("exp_tws_tws", Integer.class));
		periodSetting.setValueBucketSetId(rs.getObject("exp_vbs_vbs", Integer.class));
		periodSetting.setDynamicProgrammingTreeId(rs.getObject("exp_dpt_dpt", Integer.class));
		periodSetting.setTravelTimeSetId(rs.getObject("exp_tts_tts", Integer.class));
		periodSetting.setVehicleAssignmentSetId(rs.getObject("exp_vas_vas", Integer.class));
		periodSetting.setLearningOutputRequestsExperimentId(rs.getObject("eVle.ele_exp_learning_input_experiment", Integer.class));
		periodSetting.setValueFunctionModelSetId(rs.getObject("exp_vfa_vfa", Integer.class));
		periodSetting.setArrivalProbabilityDistributionId(rs.getObject("exp_apd_apd",Integer.class));

		return periodSetting;
	}
	
}
