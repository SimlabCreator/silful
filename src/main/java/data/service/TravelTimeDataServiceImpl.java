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
import data.entity.TravelTime;
import data.entity.TravelTimeSet;
import data.mapper.TravelTimeMapper;
import data.mapper.TravelTimeSetMapper;

public class TravelTimeDataServiceImpl extends TravelTimeDataService {

	private TravelTimeSet currentSet;
	
	@Override
	public ArrayList<SetEntity> getAllSets() {

		if (entitySets == null) {

			entitySets = DataLoadService.loadAllFromSetClass("travel_time_set", new TravelTimeSetMapper(), jdbcTemplate);

		}

		return entitySets;
	}

	@Override
	public SetEntity getSetById(int id) {

		SetEntity travelTimeSet = new TravelTimeSet();

		if (entitySets == null) {
			travelTimeSet = DataLoadService.loadBySetId("travel_time_set", "tts_id", id, new TravelTimeSetMapper(),
					jdbcTemplate);
		} else {

			for (int i = 0; i < entitySets.size(); i++) {
				if (((SetEntity) entitySets.get(i)).getId() == id) {
					travelTimeSet = (SetEntity) entitySets.get(i);
					return travelTimeSet;
				}

			}

		}
		return travelTimeSet;
	}

	@Override
	public ArrayList<TravelTime> getAllElementsBySetId(int setId) {

		ArrayList<TravelTime> entities = (ArrayList<TravelTime>) DataLoadService.loadMultipleRowsBySelectionId("travel_time", "tra_set", setId,
				new TravelTimeMapper(), jdbcTemplate);
		this.currentSet=(TravelTimeSet) this.getSetById(setId);
		this.currentSet.setElements(entities);
		return entities;
	}
	
	@Override
	public TravelTime getElementById(int entityId) {
		
		if(this.currentSet!= null){
			for(int i=0; i < this.currentSet.getElements().size(); i++){
				if(( this.currentSet.getElements().get(i)).getId()==entityId){
					return this.currentSet.getElements().get(i);
				}
			}
		}
		
		Entity travelTime = new TravelTime();
		
		travelTime = DataLoadService.loadById("travel_time", "tra_id", entityId, new TravelTimeMapper(), jdbcTemplate);
		
	    return (TravelTime) travelTime;
	}

	@Override
	public Integer persistElement(Entity entity) {

		final TravelTime travelTime = (TravelTime) entity;
		final String SQL = DataLoadService.buildInsertSQL("travel_time",4,
				"tra_set,tra_start, tra_end,tra_pd");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "tra_id" });
				ps.setObject(1, travelTime.getSetId(), Types.INTEGER);
				ps.setObject(2, travelTime.getStart(), Types.FLOAT);
				ps.setObject(3, travelTime.getEnd(), Types.FLOAT);
				ps.setObject(4, travelTime.getProbabilityDistributionId(), Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);
		
		travelTime.setId(id);

		return id;
	}

	@Override
	public Integer persistCompleteEntitySet(SetEntity setEntity) {
		final ArrayList<TravelTime> travelTimes = ((TravelTimeSet) setEntity).getElements();
		final int setId = this.persistEntitySet(setEntity);
		

		DataLoadService.persistAll("travel_time", 4,
				"tra_set,tra_start, tra_end,tra_pd",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return travelTimes.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						TravelTime travelTime = travelTimes.get(i);
						ps.setInt(1, setId);
						ps.setObject(2, travelTime.getStart(), Types.FLOAT);
						ps.setObject(3, travelTime.getEnd(), Types.FLOAT);
						ps.setObject(4, travelTime.getProbabilityDistributionId(), Types.INTEGER);

					}
				}, jdbcTemplate);


		
		this.currentSet = null;
		if (this.entitySets != null)
			this.entitySets.add(setEntity);
		return setId;

	}

	@Override
	protected Integer persistEntitySet(SetEntity setEntity) {

		final TravelTimeSet travelTimeSet = (TravelTimeSet) setEntity;

		final String SQL = DataLoadService.buildInsertSQL("travel_time_set", 2, "tts_name, tts_region");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "tts_id" });
				ps.setObject(1, travelTimeSet.getName(), Types.VARCHAR);
				ps.setObject(2, travelTimeSet.getRegionId(), Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);

		travelTimeSet.setId(id);
		travelTimeSet.setElements(null);

		return id;

	}

	@Override
	public ArrayList<SetEntity> getTravelTimeSetsByRegionId(Integer regionId) {
		ArrayList<SetEntity> sets  = DataLoadService.loadMultipleSetsBySelectionId("travel_time_set", "tts_region", regionId, new TravelTimeSetMapper(), jdbcTemplate);
		return sets;
	}

}
