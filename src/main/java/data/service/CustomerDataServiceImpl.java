package data.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;

import data.entity.Customer;
import data.entity.CustomerSet;
import data.entity.Entity;
import data.entity.SetEntity;
import data.mapper.CustomerMapper;
import data.mapper.CustomerSetMapper;

public class CustomerDataServiceImpl extends CustomerDataService {

	CustomerSet currentSet;
	
	@Override
	public ArrayList<SetEntity> getAllSets() {

		if (entitySets == null) {

			entitySets = DataLoadService.loadAllFromSetClass("customer_set", new CustomerSetMapper(), jdbcTemplate);

		}

		return entitySets;
	}

	@Override
	public SetEntity getSetById(int id) {

		SetEntity customerSet = new CustomerSet();

		if (entitySets == null) {
			customerSet = DataLoadService.loadBySetId("customer_set", "cs_id", id, new CustomerSetMapper(),
					jdbcTemplate);
		} else {

			for (int i = 0; i < entitySets.size(); i++) {
				if (entitySets.get(i).getId() == id) {
					customerSet = entitySets.get(i);
					return customerSet;
				}

			}

		}
		return customerSet;
	}

	@Override
	public ArrayList<Customer> getAllElementsBySetId(int setId) {

		ArrayList<Customer> entities = (ArrayList<Customer>) DataLoadService.loadMultipleRowsBySelectionId("customer", "cus_set", setId,
				new CustomerMapper(), jdbcTemplate);
		this.currentSet=(CustomerSet) this.getSetById(setId);
		this.currentSet.setElements(entities);
		return entities;
	}
	
	@Override
	public Customer getElementById(int entityId) {
		
		if(this.currentSet!= null){
			for(int i=0; i < this.currentSet.getElements().size(); i++){
				if(this.currentSet.getElements().get(i).getId()==entityId){
					return this.currentSet.getElements().get(i);
				}
			}
		}
		
		Entity customer = new Customer();
		
		customer = DataLoadService.loadById("customer", "cus_id", entityId, new CustomerMapper(), jdbcTemplate);
		
	    return (Customer) customer;
	}

	@Override
	public Integer persistElement(Entity entity) {

		final Customer customer = (Customer) entity;
		final String SQL = DataLoadService.buildInsertSQL("customer", 10,
				"cus_set, cus_lat, cus_long, cus_floor, cus_closest_node, cus_closest_node_distance, cus_service_time_segment, cus_segment_original, cus_return_probability, cus_temp_t");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "cus_id" });
				ps.setInt(1, customer.getSetId());
				ps.setObject(2, customer.getLat(), Types.FLOAT);
				ps.setObject(3, customer.getLon(), Types.FLOAT);
				ps.setInt(4, customer.getFloor());
				ps.setObject(5, customer.getClosestNodeId(), Types.BIGINT);
				ps.setObject(6, customer.getDistanceClosestNode(), Types.FLOAT);
				ps.setObject(7, customer.getServiceTimeSegmentId(), Types.INTEGER);
				ps.setObject(8, customer.getOriginalDemandSegmentId(), Types.INTEGER);
				ps.setObject(9, customer.getReturnProbability(), Types.FLOAT);
				ps.setObject(10, customer.getTempT(), Types.INTEGER);

				return ps;
			}
		}, jdbcTemplate);

		return id;
	}

	@Override
	public Integer persistCompleteEntitySet(SetEntity setEntity) {
		final ArrayList<Customer> customers = ((CustomerSet) setEntity).getElements();
		final int setId = this.persistEntitySet(setEntity);
		

		DataLoadService.persistAll("customer", 10,
				"cus_set, cus_lat, cus_long, cus_floor, cus_closest_node, cus_closest_node_distance, cus_service_time_segment, cus_segment_original, cus_return_probability, cus_temp_t",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return customers.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						Customer customer = customers.get(i);
						ps.setInt(1, setId);
						ps.setObject(2, customer.getLat(), Types.FLOAT);
						ps.setObject(3, customer.getLon(), Types.FLOAT);
						ps.setInt(4, customer.getFloor());
						ps.setObject(5, customer.getClosestNodeId(), Types.INTEGER);
						ps.setObject(6, customer.getDistanceClosestNode(), Types.FLOAT);
						ps.setObject(7, customer.getServiceTimeSegmentId(), Types.INTEGER);
						ps.setObject(8, customer.getOriginalDemandSegmentId(), Types.INTEGER);
						ps.setObject(9, customer.getReturnProbability(), Types.FLOAT);
						ps.setObject(10, customer.getTempT(), Types.INTEGER);

					}
				}, jdbcTemplate);

		
		this.currentSet = null;
		if (this.entitySets != null)
			this.entitySets.add(setEntity);
		return setId;

	}

	public Integer persistCompleteEntitySetWithPredefinedIds(SetEntity setEntity) {
		final ArrayList<Customer> customers = ((CustomerSet) setEntity).getElements();
		final int setId = this.persistEntitySet(setEntity);


		DataLoadService.persistAll("customer", 11,
				"cus_id, cus_set, cus_lat, cus_long, cus_floor, cus_closest_node, cus_closest_node_distance, cus_service_time_segment, cus_segment_original, cus_return_probability, cus_temp_t",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return customers.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						Customer customer = customers.get(i);
						ps.setInt(1, customer.getId());
						ps.setInt(2, setId);
						ps.setObject(3, customer.getLat(), Types.FLOAT);
						ps.setObject(4, customer.getLon(), Types.FLOAT);
						ps.setInt(5, customer.getFloor());
						ps.setObject(6, customer.getClosestNodeId(), Types.INTEGER);
						ps.setObject(7, customer.getDistanceClosestNode(), Types.FLOAT);
						ps.setObject(8, customer.getServiceTimeSegmentId(), Types.INTEGER);
						ps.setObject(9, customer.getOriginalDemandSegmentId(), Types.INTEGER);
						ps.setObject(10, customer.getReturnProbability(), Types.FLOAT);
						ps.setObject(11, customer.getTempT(), Types.INTEGER);

					}
				}, jdbcTemplate);


		this.currentSet = null;
		if (this.entitySets != null)
			this.entitySets.add(setEntity);
		return setId;

	}

	@Override
	protected Integer persistEntitySet(SetEntity setEntity) {

		final CustomerSet customerSet = (CustomerSet) setEntity;

		final String SQL = DataLoadService.buildInsertSQL("customer_set", 4, "cs_name, cs_panel, cs_extension, cs_original_demand_segment_set");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "cs_id" });
				ps.setString(1, customerSet.getName());
				ps.setObject(2, customerSet.getPanel(), Types.BOOLEAN);
				ps.setObject(3, customerSet.getExtension(), Types.BOOLEAN);
				ps.setObject(4, customerSet.getOriginalDemandSegmentSetId(), Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);

		customerSet.setId(id);
		customerSet.setElements(null);

		return id;

	}

	@Override
	public ArrayList<SetEntity> getAllByOriginalDemandSegmentSetId(Integer demandSegmentSetId) {
		ArrayList<SetEntity> entities=new ArrayList<SetEntity>();
		if(this.entitySets==null){
			entities = DataLoadService.loadMultipleSetsBySelectionId("customer_set", "cs_original_demand_segment_set", demandSegmentSetId, new CustomerSetMapper(), jdbcTemplate);
		}else{
			for(int i=0; i< this.entitySets.size(); i++){
				if(((CustomerSet)this.entitySets.get(i)).getOriginalDemandSegmentSetId()==demandSegmentSetId){
					entities.add((CustomerSet) this.entitySets.get(i));
					
				}
			}
		}
		
		return entities;
	}

}
