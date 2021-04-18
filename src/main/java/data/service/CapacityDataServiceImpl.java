package data.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;

import data.entity.Capacity;
import data.entity.CapacitySet;
import data.entity.Entity;
import data.entity.SetEntity;
import data.mapper.CapacityMapper;
import data.mapper.CapacitySetMapper;

public class CapacityDataServiceImpl extends CapacityDataService {

	CapacitySet currentSet;
	@Override
	public ArrayList<SetEntity> getAllSets() {

		if (entitySets == null) {

			entitySets = DataLoadService.loadAllFromSetClass("capacity_set", new CapacitySetMapper(), jdbcTemplate);

		}

		return entitySets;
	}

	@Override
	public SetEntity getSetById(int id) {

		SetEntity capacitySet = new CapacitySet();

		if (entitySets == null) {
			capacitySet = DataLoadService.loadBySetId("capacity_set", "cas_id", id, new CapacitySetMapper(),
					jdbcTemplate);
		} else {

			for (int i = 0; i < entitySets.size(); i++) {
				if ((entitySets.get(i)).getId() == id) {
					capacitySet =  entitySets.get(i);
					return capacitySet;
				}

			}

		}
		return capacitySet;
	}

	@Override
	public ArrayList<Capacity> getAllElementsBySetId(int setId) {

		ArrayList<Capacity> entities = (ArrayList<Capacity>) DataLoadService.loadMultipleRowsBySelectionId("capacity", "cap_set", setId,
				new CapacityMapper(), jdbcTemplate);
		this.currentSet=(CapacitySet) this.getSetById(setId);
		this.currentSet.setElements(entities);
		return entities;
	}
	
	@Override
	public Capacity getElementById(int entityId) {
		
		if(this.currentSet!= null){
			for(int i=0; i < this.currentSet.getElements().size(); i++){
				if(this.currentSet.getElements().get(i).getId()==entityId){
					return this.currentSet.getElements().get(i);
				}
			}
		}
		
		Entity capacity = new Capacity();
		
		capacity = DataLoadService.loadById("capacity", "cap_id", entityId, new CapacityMapper(), jdbcTemplate);
		
	    return (Capacity) capacity;
	}

	@Override
	public Integer persistElement(Entity entity) {

		final Capacity capacity = (Capacity) entity;
		final String SQL = DataLoadService.buildInsertSQL("capacity", 4,
				"cap_set,cap_tw,cap_delivery_area,cap_no");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "cap_id" });
				ps.setInt(1, capacity.getSetId());
				ps.setObject(2, capacity.getTimeWindowId(), Types.INTEGER);
				ps.setObject(3, capacity.getDeliveryAreaId(), Types.INTEGER);
				ps.setObject(4, capacity.getCapacityNumber(), Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);
		
		capacity.setId(id);

		return id;
	}

	@Override
	public Integer persistCompleteEntitySet(SetEntity setEntity) {

		final ArrayList<Capacity> capacities = ((CapacitySet) setEntity).getElements();
		
		final int setId = this.persistEntitySet(setEntity);
		

		DataLoadService.persistAll("capacity", 4,
				"cap_set,cap_tw,cap_delivery_area,cap_no",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return capacities.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						Capacity capacity = capacities.get(i);
						ps.setInt(1, setId);
						ps.setObject(2, capacity.getTimeWindowId(), Types.INTEGER);
						ps.setObject(3, capacity.getDeliveryAreaId(), Types.INTEGER);
						ps.setObject(4, capacity.getCapacityNumber(), Types.INTEGER);

					}
				}, jdbcTemplate);


		
		this.currentSet = null;
		if (this.entitySets != null)
			this.entitySets.add(setEntity);
		return setId;

	}

	@Override
	protected Integer persistEntitySet(SetEntity setEntity) {

		final CapacitySet capacitySet = (CapacitySet) setEntity;

		final String SQL = DataLoadService.buildInsertSQL("capacity_set", 5, "cas_name, cas_tw_set, cas_delivery_area_set,cas_routing, cas_weight");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "cas_id" });
				ps.setObject(1, capacitySet.getName(), Types.VARCHAR);
				ps.setObject(3, capacitySet.getDeliveryAreaSetId(), Types.INTEGER);
				ps.setObject(2, capacitySet.getTimeWindowSetId(), Types.INTEGER);
				ps.setObject(4, capacitySet.getRoutingId(),Types.INTEGER );
				ps.setObject(5, capacitySet.getWeight(), Types.FLOAT);
				return ps;
			}
		}, jdbcTemplate);

		capacitySet.setId(id);
		capacitySet.setElements(null);
		return id;

	}

	@Override
	public ArrayList<SetEntity> getAllSetsByDeliveryAreaSetAndTimeWindowSetId(Integer deliveryAreaSetId,
			Integer timeWindowSetId) {
		ArrayList<SetEntity> sets  =DataLoadService.loadSetsByMultipleSelectionIds("capacity_set", new String[]{"cas_tw_set", "cas_delivery_area_set"}, new Integer[]{timeWindowSetId, deliveryAreaSetId}, new CapacitySetMapper(), jdbcTemplate);
		return sets;
	}

}
