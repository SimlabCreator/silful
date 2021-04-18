package data.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;

import data.entity.DemandSegmentForecast;
import data.entity.DemandSegmentForecastSet;
import data.entity.Entity;
import data.entity.SetEntity;
import data.entity.ValueBucketForecast;
import data.entity.ValueBucketForecastSet;
import data.mapper.DemandSegmentForecastMapper;
import data.mapper.DemandSegmentForecastSetMapper;

public class DemandSegmentForecastDataServiceImpl extends DemandSegmentForecastDataService {

	private DemandSegmentForecastSet currentSet;
	
	@Override
	public ArrayList<SetEntity> getAllSets() {

		if (entitySets == null) {

			entitySets = DataLoadService.loadAllFromSetClass("demand_segment_forecast_set", new DemandSegmentForecastSetMapper(),
					jdbcTemplate);

		}

		return entitySets;
	}

	@Override
	public SetEntity getSetById(int id) {

		SetEntity demandForecastSet = new ValueBucketForecastSet();

		if (entitySets == null) {
			demandForecastSet = DataLoadService.loadBySetId("demand_segment_forecast_set", "dfs_id", id,
					new DemandSegmentForecastSetMapper(), jdbcTemplate);
		} else {

			for (int i = 0; i < entitySets.size(); i++) {
				if (entitySets.get(i).getId() == id) {
					demandForecastSet = (SetEntity) entitySets.get(i);
					return demandForecastSet;
				}

			}

		}
		return demandForecastSet;
	}

	@Override
	public ArrayList<DemandSegmentForecast> getAllElementsBySetId(int setId) {

		ArrayList<DemandSegmentForecast> entities = (ArrayList<DemandSegmentForecast>) DataLoadService.loadMultipleRowsBySelectionId("demand_segment_forecast", "df_set", setId,
				new DemandSegmentForecastMapper(), jdbcTemplate);
		this.currentSet = (DemandSegmentForecastSet) this.getSetById(setId);
		this.currentSet.setElements(entities);
		return entities;
	}

	@Override
	public DemandSegmentForecast getElementById(int entityId) {

		if (this.currentSet != null) {
			for (int i = 0; i < this.currentSet.getElements().size(); i++) {
				if ( this.currentSet.getElements().get(i).getId() == entityId) {
					return this.currentSet.getElements().get(i);
				}
			}
		}

		Entity demandForecast = new ValueBucketForecast();

		demandForecast = DataLoadService.loadById("demand_segment_forecast", "df_id", entityId, new DemandSegmentForecastMapper(),
				jdbcTemplate);

		return (DemandSegmentForecast) demandForecast;
	}

	@Override
	public Integer persistElement(Entity entity) {

		final DemandSegmentForecast demandForecast = (DemandSegmentForecast) entity;
		final String SQL = DataLoadService.buildInsertSQL("demand_segment_forecast", 4,
				"df_set,df_delivery_area, df_demand_segment,df_no");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "df_id" });
				ps.setInt(1, demandForecast.getSetId());
				ps.setObject(2, demandForecast.getDeliveryAreaId(), Types.INTEGER);
				ps.setObject(3, demandForecast.getDemandSegmentId(), Types.INTEGER);
				ps.setObject(4, demandForecast.getDemandNumber(), Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);

		demandForecast.setId(id);

		return id;
	}

	@Override
	public Integer persistCompleteEntitySet(SetEntity setEntity) {

		final ArrayList<DemandSegmentForecast> forecasts = ((DemandSegmentForecastSet) setEntity).getElements();
		
		final int setId = this.persistEntitySet(setEntity);
		

		DataLoadService.persistAll("demand_segment_forecast", 4,
				"df_set,df_delivery_area, df_demand_segment,df_no",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return forecasts.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						DemandSegmentForecast demandForecast =forecasts.get(i);
						ps.setInt(1, setId);
						ps.setObject(2, demandForecast.getDeliveryAreaId(), Types.INTEGER);
						ps.setObject(3, demandForecast.getDemandSegmentId(), Types.INTEGER);
						ps.setObject(4, demandForecast.getDemandNumber(), Types.INTEGER);

					}
				}, jdbcTemplate);

		
		this.currentSet = null;
		if (this.entitySets != null)
			this.entitySets.add(setEntity);
		return setId;

	}

	@Override
	protected Integer persistEntitySet(SetEntity setEntity) {

		final DemandSegmentForecastSet demandForecastSet = (DemandSegmentForecastSet) setEntity;

		final String SQL = DataLoadService.buildInsertSQL("demand_segment_forecast_set", 3,
				"dfs_name, dfs_delivery_area_set, dfs_demand_segment_set");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "dfs_id" });
				ps.setObject(1, demandForecastSet.getName(), Types.VARCHAR);
				ps.setObject(2, demandForecastSet.getDeliveryAreaSetId(), Types.INTEGER);
				ps.setObject(3, demandForecastSet.getDemandSegmentSetId(), Types.INTEGER);

				return ps;
			}
		}, jdbcTemplate);

		demandForecastSet.setId(id);
		demandForecastSet.setElements(null);

		return id;

	}

	@Override
	public ArrayList<SetEntity> getAllSetsByDeliveryAreaSetAndDemandSegmentSetId(Integer deliveryAreaSetId, Integer demandSegmentSetId){
		ArrayList<SetEntity> sets  = DataLoadService.loadSetsByMultipleSelectionIds("demand_segment_forecast_set", new String[]{"dfs_delivery_area_set", "dfs_demand_segment_set"}, new Integer[]{deliveryAreaSetId, demandSegmentSetId}, new DemandSegmentForecastSetMapper(), jdbcTemplate);
		return sets;
	}



}
