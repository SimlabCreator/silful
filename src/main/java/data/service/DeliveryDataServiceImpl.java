package data.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;

import data.entity.Delivery;
import data.entity.DeliverySet;
import data.entity.Entity;
import data.entity.SetEntity;
import data.mapper.CapacitySetMapper;
import data.mapper.DeliveryMapper;
import data.mapper.DeliverySetMapper;

public class DeliveryDataServiceImpl extends DeliveryDataService {

	private DeliverySet currentSet;
	
	@Override
	public ArrayList<SetEntity> getAllSets() {

		if (entitySets == null) {

			entitySets = DataLoadService.loadAllFromSetClass("delivery_set", new DeliverySetMapper(), jdbcTemplate);

		}

		return entitySets;
	}

	@Override
	public SetEntity getSetById(int id) {

		SetEntity deliverySet = new DeliverySet();

		if (entitySets == null) {
			deliverySet = DataLoadService.loadBySetId("delivery_set", "des_id", id, new CapacitySetMapper(),
					jdbcTemplate);
		} else {

			for (int i = 0; i < entitySets.size(); i++) {
				if (entitySets.get(i).getId() == id) {
					deliverySet =  entitySets.get(i);
					return deliverySet;
				}

			}

		}
		return deliverySet;
	}

	@Override
	public ArrayList<Delivery> getAllElementsBySetId(int setId) {

		ArrayList<Delivery> entities = (ArrayList<Delivery>) DataLoadService.loadMultipleRowsBySelectionId("delivery", "del_set", setId,
				new DeliveryMapper(), jdbcTemplate);
		this.currentSet=(DeliverySet) this.getSetById(setId);
		this.currentSet.setElements(entities);
		return entities;
	}
	
	@Override
	public Delivery getElementById(int entityId) {
		
		if(this.currentSet!= null){
			for(int i=0; i < this.currentSet.getElements().size(); i++){
				if(this.currentSet.getElements().get(i).getId()==entityId){
					return this.currentSet.getElements().get(i);
				}
			}
		}
		
		Entity delivery = new Delivery();
		
		delivery = DataLoadService.loadById("delivery", "del_id", entityId, new DeliveryMapper(), jdbcTemplate);
		
	    return (Delivery) delivery;
	}

	@Override
	public Integer persistElement(Entity entity) {

		final Delivery delivery = (Delivery) entity;
		final String SQL = DataLoadService.buildInsertSQL("delivery", 8,
				"del_set, del_re_id, del_travel_time, del_arrival_time, del_waiting_time, del_service_begin, del_service_time, del_buffer_before");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "del_id" });
				ps.setInt(1,delivery.getSetId());
				ps.setObject(2, delivery.getRouteElementId(), Types.INTEGER);
				ps.setObject(3, delivery.getTravelTime(), Types.INTEGER);
				ps.setObject(4, delivery.getArrivalTime(), Types.FLOAT);
				ps.setObject(5, delivery.getWaitingTime(), Types.INTEGER);
				ps.setObject(6, delivery.getServiceBegin(), Types.FLOAT);
				ps.setObject(7, delivery.getServiceTime(), Types.INTEGER);
				ps.setObject(8, delivery.getBufferBefore(), Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);
		
		delivery.setId(id);

		return id;
	}

	@Override
	public Integer persistCompleteEntitySet(SetEntity setEntity) {
		final ArrayList<Delivery> deliveries = ((DeliverySet) setEntity).getElements();
		final int setId = this.persistEntitySet(setEntity);
		

		DataLoadService.persistAll("delivery", 8,
				"del_set, del_re_id, del_travel_time, del_arrival_time, del_waiting_time, del_service_begin, del_service_time, del_buffer_before",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return deliveries.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						Delivery delivery = deliveries.get(i);
						ps.setInt(1,setId);
						ps.setObject(2, delivery.getRouteElementId(), Types.INTEGER);
						ps.setObject(3, delivery.getTravelTime(), Types.INTEGER);
						ps.setObject(4, delivery.getArrivalTime(), Types.FLOAT);
						ps.setObject(5, delivery.getWaitingTime(), Types.INTEGER);
						ps.setObject(6, delivery.getServiceBegin(), Types.FLOAT);
						ps.setObject(7, delivery.getServiceTime(), Types.INTEGER);
						ps.setObject(8, delivery.getBufferBefore(), Types.INTEGER);

					}
				}, jdbcTemplate);


		
		this.currentSet = null;
		if (this.entitySets != null)
			this.entitySets.add(setEntity);
		return setId;

	}

	@Override
	protected Integer persistEntitySet(SetEntity setEntity) {

		final DeliverySet deliverySet = (DeliverySet) setEntity;

		final String SQL = DataLoadService.buildInsertSQL("delivery_set", 1, "des_name");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "des_id" });
				ps.setObject(1, deliverySet.getName(), Types.VARCHAR);
				return ps;
			}
		}, jdbcTemplate);

		deliverySet.setId(id);
		deliverySet.setElements(null);

		return id;

	}

}
