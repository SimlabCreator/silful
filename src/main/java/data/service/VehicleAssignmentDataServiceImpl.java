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
import data.entity.VehicleAreaAssignmentSet;
import data.entity.VehicleAreaAssignment;
import data.mapper.VehicleAreaAssignmentMapper;
import data.mapper.VehicleAreaAssignmentSetMapper;

public class VehicleAssignmentDataServiceImpl extends VehicleAssignmentDataService{
	
private VehicleAreaAssignmentSet currentSet;
	
	@Override
	public ArrayList<SetEntity> getAllSets() {

		if (entitySets == null) {

			entitySets = DataLoadService.loadAllFromSetClass("vehicle_area_assignment_set", new VehicleAreaAssignmentSetMapper(), jdbcTemplate);

		}

		return entitySets;
	}

	@Override
	public SetEntity getSetById(int id) {

		SetEntity set = new VehicleAreaAssignmentSet();

		if (entitySets == null) {
			set = DataLoadService.loadBySetId("vehicle_area_assignment_set", "vrs_id", id, new VehicleAreaAssignmentSetMapper(),
					jdbcTemplate);
		} else {

			for (int i = 0; i < entitySets.size(); i++) {
				if (( entitySets.get(i)).getId() == id) {
					set =  entitySets.get(i);
					return set;
				}

			}

		}
		return set;
	}

	@Override
	public ArrayList<VehicleAreaAssignment> getAllElementsBySetId(int setId) {

		ArrayList<VehicleAreaAssignment> entities = (ArrayList<VehicleAreaAssignment>) DataLoadService.loadMultipleRowsBySelectionId("vehicle_area_assignment", "vaa_set", setId,
				new VehicleAreaAssignmentMapper(), jdbcTemplate);
		this.currentSet=(VehicleAreaAssignmentSet) this.getSetById(setId);
		this.currentSet.setElements(entities);
		return entities;
	}
	
	@Override
	public VehicleAreaAssignment getElementById(int entityId) {
		
		if(this.currentSet!= null){
			for(int i=0; i <this.currentSet.getElements().size(); i++){
				if((this.currentSet.getElements().get(i)).getId()==entityId){
					return this.currentSet.getElements().get(i);
				}
			}
		}
		
		Entity vehicleAssignment = new VehicleAreaAssignment();
		
		vehicleAssignment = DataLoadService.loadById("vehicle_area_assignment", "vaa_id", entityId, new VehicleAreaAssignmentMapper(), jdbcTemplate);
		
	    return (VehicleAreaAssignment) vehicleAssignment;
	}
	
	@Override
	public Integer persistElement(Entity entity) {

		final VehicleAreaAssignment vehicleAssignment = (VehicleAreaAssignment) entity;
		
		final String SQL = DataLoadService.buildInsertSQL("vehicle_area_assignment", 10,
				"vaa_set, vaa_vehicle_no, vaa_area, vaa_vehicle_type, vaa_starting_location_lat, vaa_starting_location_lon, vaa_ending_location_lat, vaa_ending_location_lon, vaa_start_time, vaa_end_time");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "vb_id" });
				ps.setInt(1, vehicleAssignment.getSetId());
				ps.setObject(2, vehicleAssignment.getVehicleNo(), Types.INTEGER);
				ps.setObject(3, vehicleAssignment.getDeliveryAreaId(), Types.INTEGER);
				ps.setObject(4, vehicleAssignment.getVehicleTypeId(), Types.INTEGER);
				ps.setObject(5, vehicleAssignment.getStartingLocationLat(), Types.FLOAT);
				ps.setObject(6, vehicleAssignment.getStartingLocationLon(), Types.FLOAT);
				ps.setObject(7, vehicleAssignment.getEndingLocationLat(), Types.FLOAT);
				ps.setObject(8, vehicleAssignment.getEndingLocationLon(), Types.FLOAT);
				ps.setObject(9, vehicleAssignment.getStartTime(), Types.FLOAT);
				ps.setObject(10, vehicleAssignment.getEndTime(), Types.FLOAT);
				return ps;
			}
		}, jdbcTemplate);

		return id;
	}

	@Override
	public Integer persistCompleteEntitySet(SetEntity setEntity) {

		final ArrayList<VehicleAreaAssignment> vehicleAssignments = ((VehicleAreaAssignmentSet) setEntity).getElements();
		
		final int setId = this.persistEntitySet(setEntity);
		

		DataLoadService.persistAll("vehicle_area_assignment", 10,
				"vaa_set, vaa_vehicle_no, vaa_area, vaa_vehicle_type, vaa_starting_location_lat, vaa_starting_location_lon, vaa_ending_location_lat, vaa_ending_location_lon, vaa_start_time, vaa_end_time", new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return vehicleAssignments.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						VehicleAreaAssignment vehicleAssignment = vehicleAssignments.get(i);
						ps.setInt(1, vehicleAssignment.getSetId());
						ps.setObject(2, vehicleAssignment.getVehicleNo(), Types.INTEGER);
						ps.setObject(3, vehicleAssignment.getDeliveryAreaId(), Types.INTEGER);
						ps.setObject(4, vehicleAssignment.getVehicleTypeId(), Types.INTEGER);
						ps.setObject(5, vehicleAssignment.getStartingLocationLat(), Types.FLOAT);
						ps.setObject(6, vehicleAssignment.getStartingLocationLon(), Types.FLOAT);
						ps.setObject(7, vehicleAssignment.getEndingLocationLat(), Types.FLOAT);
						ps.setObject(8, vehicleAssignment.getEndingLocationLon(), Types.FLOAT);
						ps.setObject(9, vehicleAssignment.getStartTime(), Types.FLOAT);
						ps.setObject(10, vehicleAssignment.getEndTime(), Types.FLOAT);


					}
				}, jdbcTemplate);

		
		this.currentSet = null;
		if (this.entitySets != null)
			this.entitySets.add(setEntity);
		return setId;

	}

	@Override
	protected Integer persistEntitySet(SetEntity setEntity) {

		final VehicleAreaAssignmentSet vehicleAreaAssignmentSet = (VehicleAreaAssignmentSet) setEntity;

		final String SQL = DataLoadService.buildInsertSQL("vehicle_area_assignment_set", 2, "vrs_name, vrs_delivery_area_set");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "vrs_id" });
				ps.setString(1, vehicleAreaAssignmentSet.getName());
				ps.setInt(2, vehicleAreaAssignmentSet.getDeliveryAreaSetId());
				return ps;
			}
		}, jdbcTemplate);

		vehicleAreaAssignmentSet.setId(id);
		vehicleAreaAssignmentSet.setElements(null);

		return id;

	}


	@Override
	public ArrayList<SetEntity> getAllSetsByDeliveryAreaSetId(Integer deliveryAreaSetId) {
		ArrayList<SetEntity> sets  =DataLoadService.loadMultipleSetsBySelectionId("vehicle_area_assignment_set", "vrs_delivery_area_set", deliveryAreaSetId, new VehicleAreaAssignmentSetMapper(), jdbcTemplate);
		return sets;
	}
	

}
