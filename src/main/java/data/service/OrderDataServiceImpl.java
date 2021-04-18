package data.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import data.entity.*;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;

import data.mapper.ExperimentMapper;
import data.mapper.OrderMapper;
import data.mapper.OrderSetMapper;
import data.mapper.ReferenceRoutingMapper;
import data.mapper.RoutingMapper;
import data.utility.DataServiceProvider;
import logic.utility.SettingsProvider;

public class OrderDataServiceImpl extends OrderDataService {

	private OrderSet currentSet;

	@Override
	public ArrayList<SetEntity> getAllSets() {

		if (entitySets == null) {

			entitySets = DataLoadService.loadAllFromSetClass("order_set", new OrderSetMapper(), jdbcTemplate);

		}

		return entitySets;
	}

	@Override
	public SetEntity getSetById(int id) {

		SetEntity orderSet = new OrderSet();

		if (entitySets == null) {
			orderSet = DataLoadService.loadBySetId("order_set", "os_id", id, new OrderSetMapper(), jdbcTemplate);
		} else {

			for (int i = 0; i < entitySets.size(); i++) {
				if (entitySets.get(i).getId() == id) {
					orderSet = entitySets.get(i);
					return orderSet;
				}

			}

		}
		return orderSet;
	}

	@Override
	public ArrayList<Order> getAllElementsBySetId(int setId) {

		ArrayList<Order> entities = (ArrayList<Order>) DataLoadService.loadMultipleRowsBySelectionId("order", "ord_set",
				setId, new OrderMapper(), jdbcTemplate);
		this.currentSet = (OrderSet) this.getSetById(setId);
		this.currentSet.setElements(entities);
		return entities;
	}

	@Override
	public Order getElementById(int entityId) {

		if (this.currentSet != null) {
			for (int i = 0; i < this.currentSet.getElements().size(); i++) {
				if (this.currentSet.getElements().get(i).getId() == entityId) {
					return this.currentSet.getElements().get(i);
				}
			}
		}

		Entity order = new Order();

		order = DataLoadService.loadById("order", "ord_id", entityId, new OrderMapper(), jdbcTemplate);

		return (Order) order;
	}

	@Override
	public Integer persistElement(Entity entity) {

		final Order order = (Order) entity;
		final String SQL = DataLoadService.buildInsertSQL("order", 9,
				"ord_set, ord_order_request, ord_tw_final, ord_alternative_selected, ord_accepted, ord_reason_rejection, ord_alternative_fee, ord_assigned_delivery_area, ord_assigned_value");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "ord_id" });
				ps.setInt(1, order.getSetId());
				ps.setObject(2, order.getOrderRequestId(), Types.INTEGER);
				ps.setObject(3, order.getTimeWindowFinalId(), Types.INTEGER);
				ps.setObject(4, order.getSelectedAlternativeId(), Types.INTEGER);
				ps.setObject(5, order.getAccepted(), Types.BOOLEAN);
				ps.setObject(6, order.getReasonRejection(), Types.VARCHAR);
				ps.setObject(7, order.getAlternativeFee(), Types.FLOAT);
				ps.setObject(8, order.getAssignedDeliveryAreaId(), Types.INTEGER);
				ps.setObject(9, order.getAssignedValue(), Types.DOUBLE);
				return ps;
			}
		}, jdbcTemplate);

		order.setId(id);
		// Save which alternatives were offered

		DataServiceProvider.getAlternativeDataServiceImplInstance().persistOfferedAlternatives(order);

		// Save which alternatives were available

		DataServiceProvider.getAlternativeDataServiceImplInstance().persistAvailableAlternatives(order);

		return id;
	}

	@Override
	public Integer persistCompleteEntitySet(SetEntity setEntity) {

		final ArrayList<Order> orders = ((OrderSet) setEntity).getElements();
		final int setId = this.persistEntitySet(setEntity);

        persistOrders(setId, orders, false);

		this.currentSet = null;
		if (this.entitySets != null)
			this.entitySets.add(setEntity);
		return setId;

	}

    @Override
    public void persistOrders(final Integer setId, ArrayList<Order> orders, boolean predefinedIds) {

        final ArrayList<Order> ordersToSave = orders;
        

        if (predefinedIds) {

            DataLoadService.persistAll("order", 10,
                    "ord_id, ord_set, ord_order_request, ord_tw_final, ord_alternative_selected, ord_accepted, ord_reason_rejection, ord_alternative_fee, ord_assigned_delivery_area, ord_assigned_value",
                    
                    new BatchPreparedStatementSetter() {

                        public int getBatchSize() {
                        	
                            return ordersToSave.size();
                        }

                        public void setValues(PreparedStatement ps, int i) throws SQLException {

                            Order order = ordersToSave.get(i);
                            ps.setObject(1, order.getId(), Types.INTEGER);
                            ps.setObject(2, setId, Types.INTEGER);
                            ps.setObject(3, order.getOrderRequestId(), Types.INTEGER);
                            ps.setObject(4, order.getTimeWindowFinalId(), Types.INTEGER);
                            ps.setObject(5, order.getSelectedAlternativeId(), Types.INTEGER);
                            ps.setObject(6, order.getAccepted(), Types.BOOLEAN);
                            ps.setObject(7, order.getReasonRejection(), Types.VARCHAR);
                            ps.setObject(8, order.getAlternativeFee(), Types.FLOAT);
                            ps.setObject(9, order.getAssignedDeliveryAreaId(), Types.INTEGER);
                            ps.setObject(10, order.getAssignedValue(), Types.DOUBLE);

                        }
                    }, jdbcTemplate);

        } else {

            DataLoadService.persistAll("order", 9,
                    "ord_set, ord_order_request, ord_tw_final, ord_alternative_selected, ord_accepted, ord_reason_rejection, ord_alternative_fee, ord_assigned_delivery_area, ord_assigned_value",
                    new BatchPreparedStatementSetter() {

                        public int getBatchSize() {
                            return ordersToSave.size();
                        }

                        public void setValues(PreparedStatement ps, int i) throws SQLException {

                            Order order = ordersToSave.get(i);
                            ps.setInt(1, setId);
                            ps.setObject(2, order.getOrderRequestId(), Types.INTEGER);
                            ps.setObject(3, order.getTimeWindowFinalId(), Types.INTEGER);
                            ps.setObject(4, order.getSelectedAlternativeId(), Types.INTEGER);
                            ps.setObject(5, order.getAccepted(), Types.BOOLEAN);
                            ps.setObject(6, order.getReasonRejection(), Types.VARCHAR);
                            ps.setObject(7, order.getAlternativeFee(), Types.FLOAT);
                            ps.setObject(8, order.getAssignedDeliveryAreaId(), Types.INTEGER);
                            ps.setObject(9, order.getAssignedValue(), Types.DOUBLE);

                        }
                    }, jdbcTemplate);

        }

    }


    @Override
    public Integer persistEntitySet(SetEntity setEntity) {

		final OrderSet orderSet = (OrderSet) setEntity;

		final String SQL = DataLoadService.buildInsertSQL("order_set", 3,
				"os_name, os_order_request_set,os_control_set");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "os_id" });
				ps.setString(1, orderSet.getName());
				ps.setObject(2, orderSet.getOrderRequestSetId(), Types.INTEGER);
				ps.setObject(3, orderSet.getControlSetId(), Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);

		orderSet.setId(id);
		orderSet.setElements(null);

		// Save reference routings
		if (orderSet.getReferenceRoutingsPerDeliveryArea() != null) {
			this.persistReferenceRoutings(id, orderSet.getReferenceRoutingsPerDeliveryArea());
			orderSet.setReferenceRoutingsPerDeliveryArea(null);
		}

		return id;

	}

	private void persistReferenceRoutings(int orderSetId, ArrayList<ReferenceRouting> referenceRoutings) {

		final ArrayList<ReferenceRouting> finalReferenceRoutings = referenceRoutings;
		final int finalOsId = orderSetId;

		DataLoadService.persistAll("reference_routing", 10,
				"rr_order_set, rr_delivery_area,rr_routing, rr_left_over, rr_theft_spatial, rr_theft_advanced, rr_theft_time,rr_theft_spatial_first, rr_theft_advanced_first, rr_theft_time_first",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return finalReferenceRoutings.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						ps.setInt(1, finalOsId);
						ps.setObject(2, finalReferenceRoutings.get(i).getDeliveryAreaId(), Types.INTEGER);
						ps.setObject(3, finalReferenceRoutings.get(i).getRoutingId(), Types.INTEGER);
						ps.setObject(4, finalReferenceRoutings.get(i).getRemainingCap(), Types.INTEGER);
						ps.setObject(5, finalReferenceRoutings.get(i).getNumberOfTheftsSpatial(), Types.INTEGER);
						ps.setObject(6, finalReferenceRoutings.get(i).getNumberOfTheftsSpatialAdvanced(),
								Types.INTEGER);
						ps.setObject(7, finalReferenceRoutings.get(i).getNumberOfTheftsTime(), Types.INTEGER);
						ps.setObject(8, finalReferenceRoutings.get(i).getFirstTheftsSpatial(), Types.INTEGER);
						ps.setObject(9, finalReferenceRoutings.get(i).getFirstTheftsSpatialAdvanced(),
								Types.INTEGER);
						ps.setObject(10, finalReferenceRoutings.get(i).getFirstTheftsTime(), Types.INTEGER);

					}
				}, jdbcTemplate);

	}

	@Override
	public ArrayList<SetEntity> getAllByOrderRequestSetId(Integer orderRequestSetId) {
		ArrayList<SetEntity> entities = new ArrayList<SetEntity>();
		if (this.entitySets == null) {
			entities = DataLoadService.loadMultipleSetsBySelectionId("order_set", "os_order_request_set",
					orderRequestSetId, new OrderSetMapper(), jdbcTemplate);
		} else {
			for (int i = 0; i < this.entitySets.size(); i++) {
				if (((OrderSet) this.entitySets.get(i)).getOrderRequestSetId() == orderRequestSetId) {
					entities.add((OrderSet) this.entitySets.get(i));

				}
			}
		}

		return entities;
	}

	@Override
	public ArrayList<ReferenceRouting> getReferenceRoutingsByOrderSetId(int orderSetId) {

		String SQL = "select * from "+SettingsProvider.database+".reference_routing where rr_order_set = ?";

		List<ReferenceRouting> entityList;
		try {

			entityList = jdbcTemplate.query(SQL, new Object[] { orderSetId }, new ReferenceRoutingMapper());

		} catch (Exception e) {
			entityList = null;
		}

		if (entityList != null) {
			ArrayList<ReferenceRouting> entities = new ArrayList<ReferenceRouting>();
			entities.addAll(entityList);
			return entities;
		} else {
			return null;
		}

	}

	@Override
	public ArrayList<Experiment> getAllNonCopyExperimentsWithOrderSetOutputByDemandSegmentSetId(
			int demandSegmentSetId) {
		String sql = "SELECT distinct "+SettingsProvider.database+".experiment.* FROM "+SettingsProvider.database+".experiment "
				+ "LEFT JOIN "+SettingsProvider.database+".run ON (experiment.exp_id = run.run_experiment) "
				+ "LEFT JOIN "+SettingsProvider.database+".r_run_v_order_set ON (run.run_id=r_run_v_order_set.run_os_run) "
				+ "LEFT JOIN "+SettingsProvider.database+".order_set ON (order_set.os_id=r_run_v_order_set.run_os_os) "
				+ "LEFT JOIN "+SettingsProvider.database+".order_request_set ON (order_set.os_order_request_set=order_request_set.ors_id) "
				+ "LEFT JOIN "+SettingsProvider.database+".customer_set ON (order_request_set.ors_customer_set=customer_set.cs_id) "
				+ "WHERE "+SettingsProvider.database+".experiment.exp_copy_exp IS NULL AND "+SettingsProvider.database+".r_run_v_order_set.run_os_os>0 "
				+ "AND "+SettingsProvider.database+".customer_set.cs_original_demand_segment_set=?;";

		ArrayList<Experiment> entities = (ArrayList<Experiment>) DataLoadService
				.loadComplexPreparedStatementMultipleEntities(sql, new Object[] { demandSegmentSetId },
						new ExperimentMapper(), jdbcTemplate);

		return entities;
	}

	@Override
	public ArrayList<OrderSet> getAllOrderSetsByExperimentId(int expId) {
		ArrayList<OrderSet> orderSets = new ArrayList<OrderSet>();
		String sql = "SELECT "+SettingsProvider.database+".order_set.* from "+SettingsProvider.database+".order_set "
				+ "JOIN "+SettingsProvider.database+".r_run_v_order_set ON (r_run_v_order_set.run_os_os=order_set.os_id) "
				+ "LEFT JOIN "+SettingsProvider.database+".run ON (r_run_v_order_set.run_os_run=run.run_id) "
				+ "LEFT JOIN "+SettingsProvider.database+".experiment ON (experiment.exp_id=run.run_experiment) "
				+ "WHERE "+SettingsProvider.database+".experiment.exp_id=?";

		orderSets = (ArrayList<OrderSet>) DataLoadService.loadComplexPreparedStatementMultipleSetEntities(sql,
				new Object[] { expId }, new OrderSetMapper(), jdbcTemplate);

		return orderSets;
	}

    public int getHighestOrderId() {
        String SQL = "select max(ord_id) from " + SettingsProvider.database + ".order";
        Integer id = jdbcTemplate.queryForObject(SQL, Integer.class);
        if(id==null) id=0;
        return id;
    }

}
