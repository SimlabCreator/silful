package data.service;

import java.util.ArrayList;

import data.entity.ConsiderationSetAlternative;
import data.entity.DemandSegment;
import data.entity.DemandSegmentSet;
import data.entity.DemandSegmentWeight;
import data.entity.DemandSegmentWeighting;
import data.entity.Entity;
import data.entity.SetEntity;
import data.entity.VariableCoefficient;
import data.entity.WeightingEntity;
import data.mapper.ConsiderationSetAlternativeMapper;
import data.mapper.DemandSegmentMapper;
import data.mapper.DemandSegmentSetMapper;
import data.mapper.DemandSegmentWeightMapper;
import data.mapper.DemandSegmentWeightingMapper;
import data.mapper.VariableCoefficientMapper;
import logic.utility.SettingsProvider;

public class DemandSegmentDataServiceImpl extends DemandSegmentDataService {

	private DemandSegmentSet currentSet;
	private DemandSegmentWeighting currentWeighting;
	@Override
	public ArrayList<SetEntity> getAllSets() {

		if (entitySets == null) {

			entitySets = DataLoadService.loadAllFromSetClass("demand_segment_set", new DemandSegmentSetMapper(),
					jdbcTemplate);

		}

		return entitySets;
	}

	@Override
	public SetEntity getSetById(int id) {

		SetEntity demandSegmentSet = new DemandSegmentSet();

		if (entitySets == null) {
			demandSegmentSet = DataLoadService.loadBySetId("demand_segment_set", "dss_id", id,
					new DemandSegmentSetMapper(), jdbcTemplate);
		} else {

			for (int i = 0; i < entitySets.size(); i++) {
				if (entitySets.get(i).getId() == id) {
					demandSegmentSet = entitySets.get(i);
					return demandSegmentSet;
				}

			}

		}
		return demandSegmentSet;
	}

	@Override
	public ArrayList<DemandSegment> getAllElementsBySetId(int setId) {

		ArrayList<DemandSegment> entities = (ArrayList<DemandSegment>) DataLoadService.loadMultipleRowsBySelectionId("demand_segment", "dem_set", setId,
				new DemandSegmentMapper(), jdbcTemplate);
		this.currentSet = (DemandSegmentSet) this.getSetById(setId);
		this.currentSet.setElements(entities);
		return entities;
	}

	@Override
	public DemandSegment getElementById(int entityId) {

		if (this.currentSet != null) {
			for (int i = 0; i < this.currentSet.getElements().size(); i++) {
				if ( this.currentSet.getElements().get(i).getId() == entityId) {
					return this.currentSet.getElements().get(i);
				}
			}
		}

		Entity demandSegment = new DemandSegment();

		demandSegment = DataLoadService.loadById("demand_segment", "dem_id", entityId, new DemandSegmentMapper(),
				jdbcTemplate);

		return (DemandSegment) demandSegment;
	}

	@Override
	public ArrayList<WeightingEntity> getAllWeightings() {
		if (weightings == null) {

			weightings = DataLoadService.loadAllFromWeightingClass("demand_segment_weighting",
					new DemandSegmentWeightingMapper(), jdbcTemplate);

		}

		return weightings;
	}

	@Override
	public ArrayList<WeightingEntity> getAllWeightingsBySetId(int setId) {

		ArrayList<WeightingEntity> weightingsBySet = new ArrayList<WeightingEntity>();

		weightingsBySet = DataLoadService.loadMultipleWeightingsBySelectionId("demand_segment_weighting",
				"dsw_segment_set", setId, new DemandSegmentWeightingMapper(), jdbcTemplate);
		return weightingsBySet;
	}

	@Override
	public WeightingEntity getWeightingById(int weightingId) {
		WeightingEntity weighting = new DemandSegmentWeighting();

		if (weightings == null) {
			weighting = DataLoadService.loadByWeightingId("demand_segment_weighting", "dsw_id", weightingId,
					new DemandSegmentWeightingMapper(), jdbcTemplate);
		} else {

			for (int i = 0; i < weightings.size(); i++) {
				if (((WeightingEntity) weightings.get(i)).getId() == weightingId) {
					weighting = (WeightingEntity) weightings.get(i);
					return weighting;
				}

			}

		}
		return weighting;
	}

	@Override
	public ArrayList<DemandSegmentWeight> getAllWeightsByWeightingId(int weightingId) {

		ArrayList<DemandSegmentWeight> entities = (ArrayList<DemandSegmentWeight>) DataLoadService.loadMultipleWeightRowsBySelectionId("demand_segment_weight",
				"dw_set", weightingId, new DemandSegmentWeightMapper(), jdbcTemplate);
		currentWeighting= (DemandSegmentWeighting) this.getWeightingById(weightingId);
		currentWeighting.setWeights(entities);
		;
		return entities;
	}

	@Override
	public DemandSegmentWeight getWeightById(int weightId) {
		DemandSegmentWeight weight = new DemandSegmentWeight();

		weight = (DemandSegmentWeight) DataLoadService.loadByWeightId("demand_segment_weight", "dw_id", weightId,
				new DemandSegmentWeightMapper(), jdbcTemplate);

		return weight;
	}

	@Override
	public ArrayList<ConsiderationSetAlternative> getConsiderationSetAlternativesByDemandSegmentId(Integer demandSegmentId) {
		ArrayList<ConsiderationSetAlternative> entities = (ArrayList<ConsiderationSetAlternative>) DataLoadService.loadMultipleRowsBySelectionId("consideration_set_alternative",
				"csa_demand_segment", demandSegmentId, new ConsiderationSetAlternativeMapper(), jdbcTemplate);
		return entities;
	}
	
	@Override
	public ArrayList<ConsiderationSetAlternative> getConsiderationSetAlternativesBySetId(Integer setId) {
		ArrayList<ConsiderationSetAlternative> entities = (ArrayList<ConsiderationSetAlternative>) DataLoadService.loadMultipleRowsBySelectionId("consideration_set_alternative",
				"csa_set", setId, new ConsiderationSetAlternativeMapper(), jdbcTemplate);
		return entities;
	}

	@Override
	public ConsiderationSetAlternative getConsiderationSetAlternativeById(Integer considerationSetId) {

		ConsiderationSetAlternative csa = new ConsiderationSetAlternative();
		csa = (ConsiderationSetAlternative) DataLoadService.loadById("consideration_set_alternative", "csa_id", considerationSetId,
				new ConsiderationSetAlternativeMapper(), jdbcTemplate);
		return csa;
	}

	@Override
	public ArrayList<VariableCoefficient> getVariableCoefficientsByDemandSegmentId(Integer demandSegmentId) {
		ArrayList<VariableCoefficient> entities = (ArrayList<VariableCoefficient>) DataLoadService.loadMultipleRowsBySelectionId("r_demand_segment_v_variable_type",
				"dem_variable_dem", demandSegmentId, new VariableCoefficientMapper(), jdbcTemplate);
		return entities;
	}

	@Override
	public ArrayList<SetEntity> getAllSetsByRegionAndAlternativeSetId(Integer regionId, Integer alternativeSetId) {

		ArrayList<SetEntity> entities = new ArrayList<SetEntity>();
		if (this.entitySets == null) {

			String sql = "SELECT dss.* FROM "+SettingsProvider.database+".demand_segment_set AS dss "
					+ "LEFT JOIN "+SettingsProvider.database+".residence_area_set AS ras ON (dss.dss_residence_area_set=ras.ras_id)"
					+ "WHERE ras.ras_region = ? AND dss.dss_alternative_set=?";

			Object[] parameters = new Object[] { regionId , alternativeSetId};

			entities = (ArrayList<SetEntity>) DataLoadService.loadComplexPreparedStatementMultipleSetEntities(sql, parameters,
					new DemandSegmentSetMapper(), jdbcTemplate);

		} else {
			for (int i = 0; i < this.entitySets.size(); i++) {
				if (((DemandSegmentSet) this.entitySets.get(i)).getResidenceAreaSet().getRegionId() == regionId) {
					entities.add((DemandSegmentSet) this.entitySets.get(i));

				}
			}
		}

		return entities;
	}

}
