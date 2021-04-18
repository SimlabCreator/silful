package data.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;

import data.entity.ValueBucketForecast;
import data.entity.ValueBucketForecastSet;
import data.entity.Entity;
import data.entity.SetEntity;
import data.mapper.ValueBucketForecastMapper;
import data.mapper.ValueBucketForecastSetMapper;

public class ValueBucketForecastDataServiceImpl extends ValueBucketForecastDataService {

	private ValueBucketForecastSet currentSet;
	
	@Override
	public ArrayList<SetEntity> getAllSets() {

		if (entitySets == null) {

			entitySets = DataLoadService.loadAllFromSetClass("value_bucket_forecast_set", new ValueBucketForecastSetMapper(),
					jdbcTemplate);

		}

		return entitySets;
	}

	@Override
	public SetEntity getSetById(int id) {

		SetEntity demandForecastSet = new ValueBucketForecastSet();

		if (entitySets == null) {
			demandForecastSet = DataLoadService.loadBySetId("value_bucket_forecast_set", "vfs_id", id,
					new ValueBucketForecastSetMapper(), jdbcTemplate);
		} else {

			for (int i = 0; i < entitySets.size(); i++) {
				if ((entitySets.get(i)).getId() == id) {
					demandForecastSet =  entitySets.get(i);
					return demandForecastSet;
				}

			}

		}
		return demandForecastSet;
	}

	@Override
	public ArrayList<ValueBucketForecast> getAllElementsBySetId(int setId) {

		ArrayList<ValueBucketForecast> entities = (ArrayList<ValueBucketForecast>) DataLoadService.loadMultipleRowsBySelectionId("value_bucket_forecast", "vf_set", setId,
				new ValueBucketForecastMapper(), jdbcTemplate);
		this.currentSet = (ValueBucketForecastSet) this.getSetById(setId);
		this.currentSet.setElements(entities);
		return entities;
	}

	@Override
	public ValueBucketForecast getElementById(int entityId) {

		if (this.currentSet != null) {
			for (int i = 0; i < this.currentSet.getElements().size(); i++) {
				if ((this.currentSet.getElements().get(i)).getId() == entityId) {
					return this.currentSet.getElements().get(i);
				}
			}
		}

		Entity demandForecast = new ValueBucketForecast();

		demandForecast = DataLoadService.loadById("value_bucket_forecast", "vf_id", entityId, new ValueBucketForecastMapper(),
				jdbcTemplate);

		return (ValueBucketForecast) demandForecast;
	}

	@Override
	public Integer persistElement(Entity entity) {

		final ValueBucketForecast demandForecast = (ValueBucketForecast) entity;
		final String SQL = DataLoadService.buildInsertSQL("value_bucket_forecast", 5,
				"vf_set,vf_alternative,vf_delivery_area, vf_value_bucket,vf_no");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "vf_id" });
				ps.setInt(1, demandForecast.getSetId());
				ps.setObject(2, demandForecast.getAlternativeId(), Types.INTEGER);
				ps.setObject(3, demandForecast.getDeliveryAreaId(), Types.INTEGER);
				ps.setObject(4, demandForecast.getValueBucketId(), Types.INTEGER);
				ps.setObject(5, demandForecast.getDemandNumber(), Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);

		demandForecast.setId(id);

		return id;
	}

	@Override
	public Integer persistCompleteEntitySet(SetEntity setEntity) {

		final ArrayList<ValueBucketForecast> forecasts = ((ValueBucketForecastSet) setEntity).getElements();
		
		final int setId = this.persistEntitySet(setEntity);
		
	

		DataLoadService.persistAll("value_bucket_forecast", 5, "vf_set,vf_alternative,vf_delivery_area, vf_value_bucket,vf_no",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return forecasts.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						ValueBucketForecast demandForecast = forecasts.get(i);
						ps.setInt(1, setId);
						ps.setObject(2, demandForecast.getAlternativeId(), Types.INTEGER);
						ps.setObject(3, demandForecast.getDeliveryAreaId(), Types.INTEGER);
						ps.setObject(4, demandForecast.getValueBucketId(), Types.INTEGER);
						
						ps.setObject(5, demandForecast.getDemandNumber(), Types.INTEGER);

					}
				}, jdbcTemplate);

		
		this.currentSet = null;
		if (this.entitySets != null)
			this.entitySets.add(setEntity);
		return setId;

	}

	@Override
	protected Integer persistEntitySet(SetEntity setEntity) {

		final ValueBucketForecastSet demandForecastSet = (ValueBucketForecastSet) setEntity;

		final String SQL = DataLoadService.buildInsertSQL("value_bucket_forecast_set", 4,
				"vfs_name, vfs_delivery_area_set, vfs_alternative_set, vfs_value_bucket_set");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "vfs_id" });
				ps.setObject(1, demandForecastSet.getName(), Types.VARCHAR);
				ps.setObject(2, demandForecastSet.getDeliveryAreaSetId(), Types.INTEGER);
				ps.setObject(3, demandForecastSet.getAlternativeSetId(), Types.INTEGER);
				ps.setObject(4, demandForecastSet.getValueBucketSetId(), Types.INTEGER);

				return ps;
			}
		}, jdbcTemplate);

		demandForecastSet.setId(id);
		demandForecastSet.setElements(null);

		return id;

	}

	@Override
	public ArrayList<SetEntity> getAllSetsByDeliveryAreaSetAndTimeWindowSetId(Integer deliveryAreaSetId,
			Integer timeWindowSetId) {
		ArrayList<SetEntity> sets  = DataLoadService.loadSetsByMultipleSelectionIds("value_bucket_forecast_set", new String[]{"vfs_alternative_set", "vfs_delivery_area_set"}, new Integer[]{timeWindowSetId, deliveryAreaSetId}, new ValueBucketForecastSetMapper(), jdbcTemplate);
		return sets;
	}

	@Override
	public ArrayList<SetEntity> getAllSetsByDeliveryAreaSetAndTimeWindowSetAndValueBucketSetId(
			Integer deliveryAreaSetId, Integer timeWindowSetId, Integer valueBucketSetId) {
		ArrayList<SetEntity> sets  = DataLoadService.loadSetsByMultipleSelectionIds("value_bucket_forecast_set", new String[]{"vfs_alternative_set", "vfs_delivery_area_set", "vfs_value_bucket_set"}, new Integer[]{timeWindowSetId, deliveryAreaSetId, valueBucketSetId}, new ValueBucketForecastSetMapper(), jdbcTemplate);
		return sets;
	}

}
