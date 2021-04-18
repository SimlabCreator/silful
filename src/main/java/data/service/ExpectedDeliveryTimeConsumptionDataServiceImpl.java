package data.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;

import data.entity.Entity;
import data.entity.ExpectedDeliveryTimeConsumption;
import data.entity.ExpectedDeliveryTimeConsumptionSet;
import data.entity.SetEntity;
import data.mapper.ExpectedDeliveryTimeConsumptionMapper;
import data.mapper.ExpectedDeliveryTimeConsumptionSetMapper;

public class ExpectedDeliveryTimeConsumptionDataServiceImpl extends ExpectedDeliveryTimeConsumptionDataService {

	ExpectedDeliveryTimeConsumptionSet currentSet;

	@Override
	public ArrayList<SetEntity> getAllSets() {

		if (entitySets == null) {

			entitySets = DataLoadService.loadAllFromSetClass("expected_delivery_time_consumption_set",
					new ExpectedDeliveryTimeConsumptionSetMapper(), jdbcTemplate);

		}

		return entitySets;
	}

	@Override
	public SetEntity getSetById(int id) {

		SetEntity expectedDeliveryTimeConsumptionSet = new ExpectedDeliveryTimeConsumptionSet();

		if (entitySets == null) {
			expectedDeliveryTimeConsumptionSet = DataLoadService.loadBySetId("expected_delivery_time_consumption_set",
					"eds_id", id, new ExpectedDeliveryTimeConsumptionSetMapper(), jdbcTemplate);
		} else {

			for (int i = 0; i < entitySets.size(); i++) {
				if (entitySets.get(i).getId() == id) {
					expectedDeliveryTimeConsumptionSet = entitySets.get(i);
					return expectedDeliveryTimeConsumptionSet;
				}

			}

		}
		return expectedDeliveryTimeConsumptionSet;
	}

	@Override
	public ArrayList<ExpectedDeliveryTimeConsumption> getAllElementsBySetId(int setId) {

		ArrayList<ExpectedDeliveryTimeConsumption> entities = (ArrayList<ExpectedDeliveryTimeConsumption>) DataLoadService
				.loadMultipleRowsBySelectionId("expected_delivery_time_consumption", "edt_set", setId,
						new ExpectedDeliveryTimeConsumptionMapper(), jdbcTemplate);
		this.currentSet = (ExpectedDeliveryTimeConsumptionSet) this.getSetById(setId);
		this.currentSet.setElements(entities);
		return entities;
	}

	@Override
	public ExpectedDeliveryTimeConsumption getElementById(int entityId) {

		if (this.currentSet != null) {
			for (int i = 0; i < this.currentSet.getElements().size(); i++) {
				if (this.currentSet.getElements().get(i).getId() == entityId) {
					return this.currentSet.getElements().get(i);
				}
			}
		}

		Entity expectedDeliveryTimeConsumption = new ExpectedDeliveryTimeConsumption();

		expectedDeliveryTimeConsumption = DataLoadService.loadById("expected_delivery_time_consumption", "edt_id",
				entityId, new ExpectedDeliveryTimeConsumptionMapper(), jdbcTemplate);

		return (ExpectedDeliveryTimeConsumption) expectedDeliveryTimeConsumption;
	}

	@Override
	public Integer persistElement(Entity entity) {

		final ExpectedDeliveryTimeConsumption expectedDeliveryTimeConsumption = (ExpectedDeliveryTimeConsumption) entity;
		final String SQL = DataLoadService.buildInsertSQL("expected_delivery_time_consumption", 4,
				"edt_set,edt_tw,edt_delivery_area,edt_time");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "edt_id" });
				ps.setInt(1, expectedDeliveryTimeConsumption.getSetId());
				ps.setObject(2, expectedDeliveryTimeConsumption.getTimeWindowId(), Types.INTEGER);
				ps.setObject(3, expectedDeliveryTimeConsumption.getDeliveryAreaId(), Types.INTEGER);
				ps.setObject(4, expectedDeliveryTimeConsumption.getDeliveryTime(), Types.FLOAT);
				return ps;
			}
		}, jdbcTemplate);

		expectedDeliveryTimeConsumption.setId(id);

		return id;
	}

	@Override
	public Integer persistCompleteEntitySet(SetEntity setEntity) {

		final ArrayList<ExpectedDeliveryTimeConsumption> consumptions = ((ExpectedDeliveryTimeConsumptionSet) setEntity)
				.getElements();

		final int setId = this.persistEntitySet(setEntity);

		DataLoadService.persistAll("expected_delivery_time_consumption", 4, "edt_set,edt_tw,edt_delivery_area,edt_time",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return consumptions.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						ExpectedDeliveryTimeConsumption expectedDeliveryTimeConsumption = consumptions.get(i);
						ps.setInt(1, setId);
						ps.setObject(2, expectedDeliveryTimeConsumption.getTimeWindowId(), Types.INTEGER);
						ps.setObject(3, expectedDeliveryTimeConsumption.getDeliveryAreaId(), Types.INTEGER);
						ps.setObject(4, expectedDeliveryTimeConsumption.getDeliveryTime(), Types.FLOAT);

					}
				}, jdbcTemplate);

		this.currentSet = null;
		if (this.entitySets != null)
			this.entitySets.add(setEntity);
		return setId;

	}

	@Override
	protected Integer persistEntitySet(SetEntity setEntity) {

		final ExpectedDeliveryTimeConsumptionSet expectedDeliveryTimeConsumptionSet = (ExpectedDeliveryTimeConsumptionSet) setEntity;

		final String SQL = DataLoadService.buildInsertSQL("expected_delivery_time_consumption_set", 4,
				"eds_name, eds_tw_set, eds_delivery_area_set,eds_routing");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "eds_id" });
				ps.setObject(1, expectedDeliveryTimeConsumptionSet.getName(), Types.VARCHAR);
				ps.setObject(2, expectedDeliveryTimeConsumptionSet.getTimeWindowSetId(), Types.INTEGER);
				ps.setObject(3, expectedDeliveryTimeConsumptionSet.getDeliveryAreaSetId(), Types.INTEGER);
				ps.setObject(4, expectedDeliveryTimeConsumptionSet.getRoutingId(), Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);

		expectedDeliveryTimeConsumptionSet.setId(id);
		expectedDeliveryTimeConsumptionSet.setElements(null);
		return id;

	}

	@Override
	public ArrayList<SetEntity> getAllSetsByDeliveryAreaSetAndTimeWindowSetId(Integer deliveryAreaSetId,
			Integer timeWindowSetId) {
		ArrayList<SetEntity> sets = DataLoadService.loadSetsByMultipleSelectionIds("expected_delivery_time_consumption_set",
				new String[] { "eds_tw_set", "eds_delivery_area_set" },
				new Integer[] { timeWindowSetId, deliveryAreaSetId }, new ExpectedDeliveryTimeConsumptionSetMapper(), jdbcTemplate);
		return sets;
	}

}
