package data.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;

import data.entity.Entity;
import data.entity.ResidenceArea;
import data.entity.ResidenceAreaSet;
import data.entity.ResidenceAreaWeight;
import data.entity.ResidenceAreaWeighting;
import data.entity.SetEntity;
import data.entity.WeightEntity;
import data.entity.WeightingEntity;
import data.mapper.ResidenceAreaMapper;
import data.mapper.ResidenceAreaSetMapper;
import data.mapper.ResidenceAreaWeightMapper;
import data.mapper.ResidenceAreaWeightingMapper;

public class ResidenceAreaDataServiceImpl extends ResidenceAreaDataService {

	private ResidenceAreaSet currentSet;
	private ResidenceAreaWeighting currentWeighting;

	@Override
	public ArrayList<SetEntity> getAllSets() {

		if (entitySets == null) {

			entitySets = DataLoadService.loadAllFromSetClass("residence_area_set", new ResidenceAreaSetMapper(),
					jdbcTemplate);

		}

		return entitySets;
	}

	@Override
	public SetEntity getSetById(int id) {

		SetEntity residenceAreaSet = new ResidenceAreaSet();

		if (entitySets == null) {
			residenceAreaSet = DataLoadService.loadBySetId("residence_area_set", "ras_id", id,
					new ResidenceAreaSetMapper(), jdbcTemplate);
		} else {

			for (int i = 0; i < entitySets.size(); i++) {
				if (((SetEntity) entitySets.get(i)).getId() == id) {
					residenceAreaSet = (SetEntity) entitySets.get(i);
					return residenceAreaSet;
				}

			}

		}
		return residenceAreaSet;
	}

	@Override
	public ArrayList<ResidenceArea> getAllElementsBySetId(int setId) {

		ArrayList<ResidenceArea> entities = (ArrayList<ResidenceArea>) DataLoadService.loadMultipleRowsBySelectionId(
				"residence_area", "res_residence_area_set", setId, new ResidenceAreaMapper(), jdbcTemplate);
		this.currentSet = (ResidenceAreaSet) this.getSetById(setId);
		this.currentSet.setElements(entities);
		return entities;
	}

	@Override
	public ResidenceArea getElementById(int entityId) {

		if (this.currentSet != null) {
			for (int i = 0; i < this.currentSet.getElements().size(); i++) {
				if ((this.currentSet.getElements().get(i)).getId() == entityId) {
					return this.currentSet.getElements().get(i);
				}
			}
		}

		Entity residenceArea = new ResidenceArea();

		residenceArea = DataLoadService.loadById("residence_area", "res_id", entityId, new ResidenceAreaMapper(),
				jdbcTemplate);

		return (ResidenceArea) residenceArea;
	}

	@Override
	public Integer persistElement(Entity entity) {

		final ResidenceArea residenceArea = (ResidenceArea) entity;
		final String SQL = DataLoadService.buildInsertSQL("residence_area", 6,
				"res_residence_area_set,res_point1_lat,res_point1_long,res_point2_lat,res_point2_long,res_reasonable_subarea_no");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "res_id" });
				ps.setInt(1, residenceArea.getSetId());
				ps.setObject(2, residenceArea.getLat1(), Types.FLOAT);
				ps.setObject(3, residenceArea.getLon1(), Types.FLOAT);
				ps.setObject(4, residenceArea.getLat2(), Types.FLOAT);
				ps.setObject(5, residenceArea.getLon2(), Types.FLOAT);
				ps.setObject(6, residenceArea.getReasonableSubareaNumber(), Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);

		return id;
	}

	@Override
	public Integer persistCompleteEntitySet(SetEntity setEntity) {

		final ArrayList<ResidenceArea> areas = ((ResidenceAreaSet) setEntity).getElements();
		final int setId = this.persistEntitySet(setEntity);

		DataLoadService.persistAll("residence_area", 6,
				"res_residence_area_set,res_point1_lat,res_point1_long,res_point2_lat,res_point2_long,res_reasonable_subarea_no",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return areas.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						ResidenceArea residenceArea = areas.get(i);
						ps.setInt(1, setId);
						ps.setObject(2, residenceArea.getLat1(), Types.FLOAT);
						ps.setObject(3, residenceArea.getLon1(), Types.FLOAT);
						ps.setObject(4, residenceArea.getLat2(), Types.FLOAT);
						ps.setObject(5, residenceArea.getLon2(), Types.FLOAT);
						ps.setObject(6, residenceArea.getReasonableSubareaNumber(), Types.INTEGER);
					}
				}, jdbcTemplate);

		this.currentSet = null;
		if (this.entitySets != null)
			this.entitySets.add(setEntity);
		return setId;

	}

	@Override
	protected Integer persistEntitySet(SetEntity setEntity) {

		final ResidenceAreaSet residenceAreaSet = (ResidenceAreaSet) setEntity;
		final String SQL = DataLoadService.buildInsertSQL("residence_area_set", 3,
				"ras_name,ras_description, ras_region");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "das_id" });
				ps.setObject(1, residenceAreaSet.getName(), Types.VARCHAR);
				ps.setObject(2, residenceAreaSet.getDescription(), Types.VARCHAR);
				ps.setObject(3, residenceAreaSet.getRegionId(), Types.INTEGER);

				return ps;
			}
		}, jdbcTemplate);

		residenceAreaSet.setId(id);
		residenceAreaSet.setElements(null);

		return id;

	}

	@Override
	public ArrayList<WeightingEntity> getAllWeightings() {
		if (weightings == null) {

			weightings = DataLoadService.loadAllFromWeightingClass("residence_area_weighting",
					new ResidenceAreaWeightingMapper(), jdbcTemplate);

		}

		return weightings;
	}

	@Override
	public ArrayList<WeightingEntity> getAllWeightingsBySetId(int setId) {
		ArrayList<WeightingEntity> weightingsBySet = new ArrayList<WeightingEntity>();

		weightingsBySet = DataLoadService.loadMultipleWeightingsBySelectionId("residence_area_weighting",
				"rws_residence_set", setId, new ResidenceAreaWeightingMapper(), jdbcTemplate);
		return weightingsBySet;
	}

	@Override
	public WeightingEntity getWeightingById(int weightingId) {
		WeightingEntity weighting = new ResidenceAreaWeighting();

		if (weightings == null) {
			weighting = DataLoadService.loadByWeightingId("residence_area_weighting", "rws_id", weightingId,
					new ResidenceAreaWeightingMapper(), jdbcTemplate);
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
	public ArrayList<ResidenceAreaWeight> getAllWeightsByWeightingId(int weightingId) {
		ArrayList<ResidenceAreaWeight> entities = (ArrayList<ResidenceAreaWeight>) DataLoadService
				.loadMultipleWeightRowsBySelectionId("residence_area_weight", "raw_set", weightingId,
						new ResidenceAreaWeightMapper(), jdbcTemplate);
		this.currentWeighting = (ResidenceAreaWeighting) this.getWeightingById(weightingId);
		this.currentWeighting.setWeights(entities);
		;
		return entities;
	}

	@Override
	public ResidenceAreaWeight getWeightById(int weightId) {
		WeightEntity weight = new ResidenceAreaWeight();

		weight = DataLoadService.loadByWeightId("residence_area_weight", "raw_id", weightId,
				new ResidenceAreaWeightMapper(), jdbcTemplate);

		return (ResidenceAreaWeight) weight;
	}

	@Override
	public Integer persistWeight(WeightEntity weight) {
		final WeightEntity weightToSave = weight;
		final String SQL = DataLoadService.buildInsertSQL("residence_area_weight", 3,
				"raw_set, raw_residence_area, raw_weight");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "raw_id" });
				ps.setObject(1, weightToSave.getSetId(), Types.INTEGER);
				ps.setObject(2, weightToSave.getElementId(), Types.INTEGER);
				ps.setObject(3, weightToSave.getWeight(), Types.FLOAT);
				return ps;
			}
		}, jdbcTemplate);

		weight.setId(id);

		return id;
	}

	@Override
	protected Integer persistWeighting(WeightingEntity weighting) {

		final ResidenceAreaWeighting weightingToSave = (ResidenceAreaWeighting) weighting;

		final String SQL = DataLoadService.buildInsertSQL("residence_area_weighting", 2, "rws_name, rws_residence_set");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "rws_id" });
				ps.setObject(1, weightingToSave.getName(), Types.VARCHAR);
				ps.setObject(2, weightingToSave.getSetEntityId(), Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);

		weightingToSave.setId(id);
		weightingToSave.setWeights(null);

		return id;
	}

	@Override
	public Integer persistCompleteWeighting(WeightingEntity weighting) {

		final ArrayList<ResidenceAreaWeight> weights = ((ResidenceAreaWeighting) weighting).getWeights();
		final int setId = this.persistWeighting(weighting);

		DataLoadService.persistAll("residence_area_weight", 3, "raw_set, raw_residence_area, raw_weight",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return weights.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						WeightEntity weightEntity = weights.get(i);
						ps.setInt(1, setId);
						ps.setObject(2, weightEntity.getElementId(), Types.INTEGER);
						ps.setObject(3, weightEntity.getWeight(), Types.FLOAT);

					}
				}, jdbcTemplate);

		if (this.weightings != null)
			this.weightings.add(weighting);
		return setId;
	}

}
