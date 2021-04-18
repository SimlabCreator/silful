package data.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;

import data.entity.Entity;
import data.entity.SetEntity;
import data.entity.TimeWindow;
import data.entity.TimeWindowSet;
import data.mapper.TimeWindowMapper;
import data.mapper.TimeWindowSetMapper;

public class TimeWindowDataServiceImpl extends TimeWindowDataService {

	private TimeWindowSet currentSet;
	@Override
	public ArrayList<SetEntity> getAllSets() {

		if (entitySets == null) {

			entitySets = DataLoadService.loadAllFromSetClass("time_window_set", new TimeWindowSetMapper(), jdbcTemplate);

		}

		return entitySets;
	}

	@Override
	public SetEntity getSetById(int id) {

		SetEntity timeWindowSet = new TimeWindowSet();

		if (entitySets == null) {
			timeWindowSet = DataLoadService.loadBySetId("time_window_set", "tws_id", id, new TimeWindowSetMapper(),
					jdbcTemplate);
		} else {

			for (int i = 0; i < entitySets.size(); i++) {
				if (((SetEntity) entitySets.get(i)).getId() == id) {
					timeWindowSet = (SetEntity) entitySets.get(i);
					return timeWindowSet;
				}

			}

		}
		return timeWindowSet;
	}

	@Override
	public ArrayList<TimeWindow> getAllElementsBySetId(int setId) {

		ArrayList<TimeWindow> entities = (ArrayList<TimeWindow>) DataLoadService.loadMultipleRowsBySelectionId("time_window", "tw_set", setId,
				new TimeWindowMapper(), jdbcTemplate);
		this.currentSet=(TimeWindowSet) this.getSetById(setId);
		this.currentSet.setElements(entities);
		return entities;
	}
	
	@Override
	public TimeWindow getElementById(int entityId) {
		
		if(this.currentSet!= null){
			for(int i=0; i <this.currentSet.getElements().size(); i++){
				if((this.currentSet.getElements().get(i)).getId()==entityId){
					return this.currentSet.getElements().get(i);
				}
			}
		}
		
		Entity timeWindow = new TimeWindow();
		
		timeWindow = DataLoadService.loadById("time_window", "tw_id", entityId, new TimeWindowMapper(), jdbcTemplate);
		
	    return (TimeWindow) timeWindow;
	}

	@Override
	public Integer persistElement(Entity entity) {

		final TimeWindow timeWindow = (TimeWindow) entity;
		final String SQL = DataLoadService.buildInsertSQL("time_window", 3,
				"tw_set, tw_start_time, tw_end_time");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "tw_id" });
				ps.setInt(1, timeWindow.getSetId());
				ps.setObject(2, timeWindow.getStartTime(), Types.FLOAT);
				ps.setObject(3, timeWindow.getEndTime(), Types.FLOAT);

				return ps;
			}
		}, jdbcTemplate);

		return id;
	}

	@Override
	public Integer persistCompleteEntitySet(SetEntity setEntity) {

		final ArrayList<TimeWindow> timeWindows = ((TimeWindowSet) setEntity).getElements();
		
		final int setId = this.persistEntitySet(setEntity);
		

		DataLoadService.persistAll("time_window", 3,
				"tw_set, tw_start_time, tw_end_time", new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return timeWindows.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						TimeWindow timeWindow = timeWindows.get(i);
						ps.setInt(1, setId);
						ps.setObject(2, timeWindow.getStartTime(), Types.FLOAT);
						ps.setObject(3, timeWindow.getEndTime(), Types.FLOAT);


					}
				}, jdbcTemplate);

		
		this.currentSet = null;
		if (this.entitySets != null)
			this.entitySets.add(setEntity);
		return setId;

	}

	@Override
	protected Integer persistEntitySet(SetEntity setEntity) {

		final TimeWindowSet timeWindowSet = (TimeWindowSet) setEntity;

		final String SQL = DataLoadService.buildInsertSQL("time_window_set", 4, "tws_name, tws_overlapping, tws_same_length, tws_continuous");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "tws_id" });
				ps.setString(1, timeWindowSet.getName());
				ps.setObject(2, timeWindowSet.getOverlapping(), Types.BOOLEAN);
				ps.setObject(3, timeWindowSet.getSameLength(), Types.BOOLEAN);
				ps.setObject(4, timeWindowSet.getContinuous(), Types.BOOLEAN);
				return ps;
			}
		}, jdbcTemplate);

		timeWindowSet.setId(id);
		timeWindowSet.setElements(null);

		return id;

	}

}
