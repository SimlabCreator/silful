package data.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;

import data.entity.DeliveryArea;
import data.entity.DeliveryAreaSet;
import data.entity.Entity;
import data.entity.SetEntity;
import data.mapper.DeliveryAreaMapper;
import data.mapper.DeliveryAreaSetMapper;
import logic.utility.SettingsProvider;

public class DeliveryAreaDataServiceImpl extends DeliveryAreaDataService {

	private DeliveryAreaSet currentSet;

	@Override
	public ArrayList<SetEntity> getAllSets() {

		if (entitySets == null) {

			entitySets = DataLoadService.loadAllFromSetClass("delivery_area_set", new DeliveryAreaSetMapper(),
					jdbcTemplate);

		}

		return entitySets;
	}

	@Override
	public ArrayList<SetEntity> getAllSetsByRegionId(int regionId) {

		ArrayList<SetEntity> entities = new ArrayList<SetEntity>();
		if (this.entitySets == null) {
			entities = DataLoadService.loadMultipleSetsBySelectionId("delivery_area_set", "das_region", regionId,
					new DeliveryAreaSetMapper(), jdbcTemplate);
		} else {
			for (int i = 0; i < this.entitySets.size(); i++) {
				if (((DeliveryAreaSet) this.entitySets.get(i)).getRegionId() == regionId) {
					entities.add((DeliveryAreaSet) this.entitySets.get(i));

				}
			}
		}

		return entities;
	}

	@Override
	public SetEntity getSetById(int id) {

		SetEntity deliveryAreaSet = new DeliveryAreaSet();

		if (entitySets == null) {
			deliveryAreaSet = DataLoadService.loadBySetId("delivery_area_set", "das_id", id,
					new DeliveryAreaSetMapper(), jdbcTemplate);
		} else {

			for (int i = 0; i < entitySets.size(); i++) {
				if (entitySets.get(i).getId() == id) {
					deliveryAreaSet = entitySets.get(i);
					return deliveryAreaSet;
				}

			}

		}
		return deliveryAreaSet;
	}

	@Override
	public ArrayList<DeliveryArea> getAllElementsBySetId(int setId) {

		ArrayList<DeliveryArea> entities = (ArrayList<DeliveryArea>) DataLoadService.loadMultipleRowsBySelectionId(
				"delivery_area", "da_set", setId, new DeliveryAreaMapper(), jdbcTemplate);
		this.currentSet = (DeliveryAreaSet) this.getSetById(setId);
		this.currentSet.setElements(entities);
		return entities;
	}

	@Override
	public DeliveryArea getElementById(int entityId) {

		if (this.currentSet != null) {
			for (int i = 0; i < this.currentSet.getElements().size(); i++) {
				if (this.currentSet.getElements().get(i).getId() == entityId) {
					return this.currentSet.getElements().get(i);
				}
			}
		}

		Entity deliveryArea = new DeliveryArea();

		deliveryArea = DataLoadService.loadById("delivery_area", "da_id", entityId, new DeliveryAreaMapper(),
				jdbcTemplate);

		return (DeliveryArea) deliveryArea;
	}

	@Override
	public Integer persistElement(Entity entity) {

		final DeliveryArea deliveryArea = (DeliveryArea) entity;
		final String SQL = DataLoadService.buildInsertSQL("delivery_area", 8,
				"da_set, da_point1_lat, da_point1_long, da_point2_lat, da_point2_long, da_center_lat, da_center_long, da_subset");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "da_id" });
				ps.setInt(1, deliveryArea.getSetId());
				ps.setObject(2, deliveryArea.getLat1(), Types.FLOAT);
				ps.setObject(3, deliveryArea.getLon1(), Types.FLOAT);
				ps.setObject(4, deliveryArea.getLat2(), Types.FLOAT);
				ps.setObject(5, deliveryArea.getLon2(), Types.FLOAT);
				ps.setObject(6, deliveryArea.getCenterLat(), Types.FLOAT);
				ps.setObject(7, deliveryArea.getCenterLon(), Types.FLOAT);
				ps.setObject(8, deliveryArea.getSubsetId(), Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);

		return id;
	}

	public void updateDeliveryAreaSetToPredefined(DeliveryAreaSet set) {
		
		final String tempSQL = "update "+SettingsProvider.database+".delivery_area_set set das_predefined=1 where das_id=" + set.getId();
		DataLoadService.updateSetElementBySetId(tempSQL, jdbcTemplate);
		set.setPredefined(true);
	}

	@Override
	public Integer persistCompleteEntitySet(SetEntity setEntity) {
		final ArrayList<DeliveryArea> areas = ((DeliveryAreaSet) setEntity).getElements();

		// Check if there are potential delivery area subsets to save
		for (DeliveryArea area : areas) {
			if (area.getSubset() != null) {
				area.setSubsetId(this.persistCompleteEntitySet(area.getSubset()));
			}
		}

		final int setId = this.persistEntitySet(setEntity);

		DataLoadService.persistAll("delivery_area", 8,
				"da_set, da_point1_lat, da_point1_long, da_point2_lat, da_point2_long, da_center_lat, da_center_long, da_subset",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return areas.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						DeliveryArea deliveryArea = areas.get(i);
						ps.setInt(1, setId);
						ps.setObject(2, deliveryArea.getLat1(), Types.FLOAT);
						ps.setObject(3, deliveryArea.getLon1(), Types.FLOAT);
						ps.setObject(4, deliveryArea.getLat2(), Types.FLOAT);
						ps.setObject(5, deliveryArea.getLon2(), Types.FLOAT);
						ps.setObject(6, deliveryArea.getCenterLat(), Types.FLOAT);
						ps.setObject(7, deliveryArea.getCenterLon(), Types.FLOAT);
						ps.setObject(8, deliveryArea.getSubsetId(), Types.INTEGER);

					}
				}, jdbcTemplate);

		this.currentSet = null;
		if (this.entitySets != null)
			this.entitySets.add(setEntity);
		return setId;

	}

	@Override
	protected Integer persistEntitySet(SetEntity setEntity) {

		final DeliveryAreaSet deliveryAreaSet = (DeliveryAreaSet) setEntity;
		final String SQL = DataLoadService.buildInsertSQL("delivery_area_set", 5,
				"das_name, das_description, das_region, das_predefined, das_reasonable_area_no");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "das_id" });
				ps.setString(1, deliveryAreaSet.getName());
				ps.setString(2, deliveryAreaSet.getDescription());
				ps.setInt(3, deliveryAreaSet.getRegionId());
				ps.setBoolean(4, deliveryAreaSet.isPredefined());
				ps.setObject(5, deliveryAreaSet.getReasonableNumberOfAreas(), Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);

		deliveryAreaSet.setId(id);
		deliveryAreaSet.setElements(null);
		return id;

	}

	@Override
	public DeliveryArea getDeliveryAreaBySubsetId(int subsetId) {
		DeliveryArea deliveryArea = new DeliveryArea();

		deliveryArea = (DeliveryArea) DataLoadService.loadById("delivery_area", "da_subset", subsetId,
				new DeliveryAreaMapper(), jdbcTemplate);

		return deliveryArea;
	}

}
