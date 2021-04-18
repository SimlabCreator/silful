package data.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;

import data.entity.*;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;

import data.mapper.AlternativePreferenceMapper;
import data.mapper.ExperimentMapper;
import data.mapper.OrderRequestMapper;
import data.mapper.OrderRequestSetMapper;
import logic.utility.SettingsProvider;

public class OrderRequestDataServiceImpl extends OrderRequestDataService {

	private OrderRequestSet currentSet;

	@Override
	public ArrayList<SetEntity> getAllSets() {

		if (entitySets == null) {

			entitySets = DataLoadService.loadAllFromSetClass("order_request_set", new OrderRequestSetMapper(),
					jdbcTemplate);

		}

		return entitySets;
	}

	@Override
	public SetEntity getSetById(int id) {

		SetEntity orderRequestSet = new OrderRequestSet();

		if (entitySets == null) {
			orderRequestSet = DataLoadService.loadBySetId("order_request_set", "ors_id", id,
					new OrderRequestSetMapper(), jdbcTemplate);
		} else {

			for (int i = 0; i < entitySets.size(); i++) {
				if (((SetEntity) entitySets.get(i)).getId() == id) {
					orderRequestSet = (SetEntity) entitySets.get(i);
					return orderRequestSet;
				}

			}

		}
		return orderRequestSet;
	}

	@Override
	public ArrayList<OrderRequest> getAllElementsBySetId(int setId) {

		ArrayList<OrderRequest> entities = (ArrayList<OrderRequest>) DataLoadService.loadMultipleRowsBySelectionId(
				"order_request", "orr_set", setId, new OrderRequestMapper(), jdbcTemplate);
		this.currentSet = (OrderRequestSet) this.getSetById(setId);
		this.currentSet.setElements(entities);
		return entities;
	}

	@Override
	public OrderRequest getElementById(int entityId) {

		if (this.currentSet != null) {
			for (int i = 0; i < this.currentSet.getElements().size(); i++) {
				if (this.currentSet.getElements().get(i).getId() == entityId) {
					return this.currentSet.getElements().get(i);
				}
			}
		}

		Entity orderRequest = new OrderRequest();

		orderRequest = DataLoadService.loadById("order_request", "orr_id", entityId, new OrderRequestMapper(),
				jdbcTemplate);

		return (OrderRequest) orderRequest;
	}
	
	private ArrayList<AlternativePreference> persistOrderRequestWithCustomer(OrderRequest request, CustomerDataServiceImpl csDs){
		
		final Integer customerId = csDs.persistElement(request.getCustomer());
		request.setCustomerId(customerId);
		return this.persistElementWithoutPreferences(request);
	}

	private OrderRequest persistCustomerOfOrderRequest(OrderRequest request, CustomerDataServiceImpl csDs){

		final Integer customerId = csDs.persistElement(request.getCustomer());
		request.setCustomerId(customerId);
		return request;
	}

	private int getHighestCustomerId(){
		String SQL = "select max(cus_id) from "+SettingsProvider.database+".customer";
		Integer id = jdbcTemplate.queryForObject(SQL, Integer.class);
		if(id==null) id=0;
		return id;
	}

	private int getHighestOrderRequestId(){
		String SQL = "select max(orr_id) from "+SettingsProvider.database+".order_request";
		Integer id = jdbcTemplate.queryForObject(SQL, Integer.class);
		if(id==null) id=0;
		return id;
	}

	@Override
	public Integer persistElement(Entity entity) {

		final OrderRequest orderRequest = (OrderRequest) entity;
		double startTime=System.currentTimeMillis();
		final String SQL = DataLoadService.buildInsertSQL("order_request", 7,
				"orr_set, orr_customer, orr_content_type, orr_basket_value, orr_basket_volume, orr_basket_packageno, orr_t");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "orr_id" });
				ps.setInt(1, orderRequest.getSetId());
				ps.setInt(2, orderRequest.getCustomerId());
				ps.setObject(3, orderRequest.getOrderContentTypeId(), Types.INTEGER);
				ps.setObject(4, orderRequest.getBasketValue(), Types.FLOAT);
				ps.setObject(5, orderRequest.getBasketVolume(), Types.FLOAT);
				ps.setObject(6, orderRequest.getPackageno(), Types.INTEGER);
				ps.setObject(7, orderRequest.getArrivalTime(), Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);
		System.out.println("Persist request-time:"+ (System.currentTimeMillis()-startTime));
		orderRequest.setId(id);

		// Additionally persist sampled preferences, if applicable
		if (((OrderRequestSet) this.getSetById(orderRequest.getSetId())).getPreferencesSampled()) {
			
			HashMap<Integer, Double> preferences = orderRequest.getAlternativePreferences();
			ArrayList<AlternativePreference> alternativePreferences = new ArrayList<AlternativePreference>();
			for (Integer key : preferences.keySet()) {
				AlternativePreference alt = new AlternativePreference();
				alt.setAlternativeId(key);
				alt.setOrderRequestId(orderRequest.getId());
				alt.setUtilityValue(preferences.get(key));
				alternativePreferences.add(alt);
			}
			
			
			final ArrayList<AlternativePreference> alternativePreferencesList = alternativePreferences;
			startTime=System.currentTimeMillis();
			DataLoadService.persistAll("r_request_v_alternative", 3, "re_alt_re, re_alt_alt, utility",
					new BatchPreparedStatementSetter() {
						public int getBatchSize() {
							return alternativePreferencesList.size();
						}

						public void setValues(PreparedStatement ps, int i) throws SQLException {

							AlternativePreference pref = (AlternativePreference) alternativePreferencesList.get(i);
							ps.setInt(1, pref.getOrderRequestId());
							ps.setInt(2, pref.getAlternativeId());
							ps.setObject(3, pref.getUtilityValue(), Types.FLOAT);

						}
					}, jdbcTemplate);
			
			System.out.println("Persist pref-time:"+ (System.currentTimeMillis()-startTime)+" and amount: "+alternativePreferencesList.size());
		}

		return id;
	}

	public void persistElementWithoutPreferencesWithId(Entity entity) {

		final OrderRequest orderRequest = (OrderRequest) entity;
		final String SQL = DataLoadService.buildInsertSQL("order_request", 8,
				"orr_id, orr_set, orr_customer, orr_content_type, orr_basket_value, orr_basket_volume, orr_basket_packageno, orr_t");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[]{"orr_id"});
				ps.setInt(1, orderRequest.getId());
				ps.setInt(2, orderRequest.getSetId());
				ps.setInt(3, orderRequest.getCustomerId());
				ps.setObject(4, orderRequest.getOrderContentTypeId(), Types.INTEGER);
				ps.setObject(5, orderRequest.getBasketValue(), Types.FLOAT);
				ps.setObject(6, orderRequest.getBasketVolume(), Types.FLOAT);
				ps.setObject(7, orderRequest.getPackageno(), Types.INTEGER);
				ps.setObject(8, orderRequest.getArrivalTime(), Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);

		orderRequest.setId(id);
	}

	public ArrayList<AlternativePreference> persistElementWithoutPreferences(Entity entity) {

		final OrderRequest orderRequest = (OrderRequest) entity;
		final String SQL = DataLoadService.buildInsertSQL("order_request", 7,
				"orr_set, orr_customer, orr_content_type, orr_basket_value, orr_basket_volume, orr_basket_packageno, orr_t");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "orr_id" });
				ps.setInt(1, orderRequest.getSetId());
				ps.setInt(2, orderRequest.getCustomerId());
				ps.setObject(3, orderRequest.getOrderContentTypeId(), Types.INTEGER);
				ps.setObject(4, orderRequest.getBasketValue(), Types.FLOAT);
				ps.setObject(5, orderRequest.getBasketVolume(), Types.FLOAT);
				ps.setObject(6, orderRequest.getPackageno(), Types.INTEGER);
				ps.setObject(7, orderRequest.getArrivalTime(), Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);
		orderRequest.setId(id);

		// Additionally persist sampled preferences, if applicable
		ArrayList<AlternativePreference> alternativePreferences =null;
		if (((OrderRequestSet) this.getSetById(orderRequest.getSetId())).getPreferencesSampled()) {
			
			HashMap<Integer, Double> preferences = orderRequest.getAlternativePreferences();
			alternativePreferences = new ArrayList<AlternativePreference>();
			for (Integer key : preferences.keySet()) {
				AlternativePreference alt = new AlternativePreference();
				alt.setAlternativeId(key);
				alt.setOrderRequestId(orderRequest.getId());
				alt.setUtilityValue(preferences.get(key));
				alternativePreferences.add(alt);
			}
			
		}

		return alternativePreferences;
	}

	@Override
	public Integer persistCompleteEntitySet(SetEntity setEntity) {

		final ArrayList<OrderRequest> requests = ((OrderRequestSet) setEntity).getElements();

		final int setId = this.persistEntitySet(setEntity);

		final ArrayList<AlternativePreference>  preferences = new ArrayList<AlternativePreference>();
		for (int i = 0; i < requests.size(); i++) {
			OrderRequest orderRequest = requests.get(i);
			orderRequest.setSetId(setId);
			ArrayList<AlternativePreference> indivPref = this.persistElementWithoutPreferences(orderRequest);
			if(indivPref!=null){
				preferences.addAll(indivPref);
			}

		}
		if(preferences.size()>0){
			double startTime=System.currentTimeMillis();
			DataLoadService.persistAll("r_request_v_alternative", 3, "re_alt_re, re_alt_alt, utility",
					new BatchPreparedStatementSetter() {
						public int getBatchSize() {
							return preferences.size();
						}

						public void setValues(PreparedStatement ps, int i) throws SQLException {

							AlternativePreference pref = (AlternativePreference) preferences.get(i);
							ps.setInt(1, pref.getOrderRequestId());
							ps.setInt(2, pref.getAlternativeId());
							ps.setObject(3, pref.getUtilityValue(), Types.FLOAT);

						}
					}, jdbcTemplate);
			
			System.out.println("Persist pref-time:"+ (System.currentTimeMillis()-startTime)+" and amount: "+preferences.size());
		}

		this.currentSet = null;
		if (this.entitySets != null)
			this.entitySets.add(setEntity);
		return setId;

	}

	@Override
	protected Integer persistEntitySet(SetEntity setEntity) {

		final OrderRequestSet orderRequestSet = (OrderRequestSet) setEntity;

		final String SQL = DataLoadService.buildInsertSQL("order_request_set", 4,
				"ors_name, ors_customer_set, ors_booking_horizon,ors_sampledPreferences");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "ors_id" });
				ps.setString(1, orderRequestSet.getName());
				ps.setObject(2, orderRequestSet.getCustomerSetId(), Types.INTEGER);
				ps.setObject(3, orderRequestSet.getBookingHorizon(), Types.INTEGER);
				ps.setObject(4, orderRequestSet.getPreferencesSampled(), Types.BOOLEAN);
				return ps;
			}
		}, jdbcTemplate);

		orderRequestSet.setId(id);
		orderRequestSet.setElements(null);

		return id;

	}

	@Override
	public ArrayList<SetEntity> getAllByCustomerSetId(Integer customerSetId) {
		ArrayList<SetEntity> entities = new ArrayList<SetEntity>();
		if (this.entitySets == null) {
			entities = DataLoadService.loadMultipleSetsBySelectionId("order_request_set", "ors_customer_set",
					customerSetId, new OrderRequestSetMapper(), jdbcTemplate);
		} else {
			for (int i = 0; i < this.entitySets.size(); i++) {
				if (((OrderRequestSet) this.entitySets.get(i)).getCustomerSetId() == customerSetId) {
					entities.add((OrderRequestSet) this.entitySets.get(i));

				}
			}
		}

		return entities;
	}

	@Override
	public ArrayList<SetEntity> getAllByBookingPeriodLengthAndAlternativeSetId(Integer periodLength,
			Integer alternativeSetId) {
		ArrayList<SetEntity> entities = new ArrayList<SetEntity>();
		if (this.entitySets == null) {
			entities = DataLoadService.loadMultipleSetsBySelectionId("order_request_set", "ors_booking_horizon",
					periodLength, new OrderRequestSetMapper(), jdbcTemplate);
		} else {
			for (int i = 0; i < this.entitySets.size(); i++) {
				if (((OrderRequestSet) this.entitySets.get(i)).getBookingHorizon() == periodLength) {
					entities.add((OrderRequestSet) this.entitySets.get(i));

				}
			}
		}
		ArrayList<SetEntity> finalEntities = new ArrayList<SetEntity>();
		for (int i = 0; i < entities.size(); i++) {
			if (((OrderRequestSet) entities.get(i)).getCustomerSet().getOriginalDemandSegmentSet()
					.getAlternativeSetId() == alternativeSetId)
				finalEntities.add(entities.get(i));
		}
		return finalEntities;
	}

	@Override
	public ArrayList<SetEntity> getAllByAlternativeSetId(Integer alternativeSetId) {
		this.getAllSets();
		ArrayList<SetEntity> finalEntities = new ArrayList<SetEntity>();

		for (int i = 0; i < entitySets.size(); i++) {
			if (((OrderRequestSet) entitySets.get(i)).getCustomerSet().getOriginalDemandSegmentSet()
					.getAlternativeSetId() == alternativeSetId)
				finalEntities.add(entitySets.get(i));
		}
		return finalEntities;
	}

	@Override
	public HashMap<Integer, Double> getSampledPreferencesByElement(Integer orderRequestId) {
		ArrayList<AlternativePreference> entities = (ArrayList<AlternativePreference>) DataLoadService
				.loadMultipleRowsBySelectionId("r_request_v_alternative", "re_alt_re", orderRequestId,
						new AlternativePreferenceMapper(), jdbcTemplate);

		HashMap<Integer, Double> preferences = new HashMap<Integer, Double>();
		for (AlternativePreference alt : entities) {
			preferences.put(alt.getAlternativeId(), alt.getUtilityValue());
		}
		return preferences;
	}

	@Override
	public ArrayList<Experiment> getAllExperimentsWithOrderRequestSetOutputByDemandSegmentSetIdAndBookingHorizonLength(
			int demandSegmentSetId, int bookingPeriodLength) {
		String sql = "SELECT distinct "+SettingsProvider.database+".experiment.* FROM "+SettingsProvider.database+".experiment "
				+ "LEFT JOIN "+SettingsProvider.database+".run ON (experiment.exp_id = run.run_experiment) "
				+ "LEFT JOIN "+SettingsProvider.database+".r_run_v_order_request_set ON (run.run_id=r_run_v_order_request_set.run_ors_run) "
				+ "LEFT JOIN "+SettingsProvider.database+".order_request_set ON (order_request_set.ors_id=r_run_v_order_request_set.run_ors_ors) "
				+ "LEFT JOIN "+SettingsProvider.database+".customer_set ON (customer_set.cs_id=order_request_set.ors_customer_set) "
				+ "WHERE "+SettingsProvider.database+".r_run_v_order_request_set.run_ors_ors>0 AND "+SettingsProvider.database+".customer_set.cs_original_demand_segment_set=? AND "+SettingsProvider.database+".order_request_set.ors_booking_horizon=?";

		ArrayList<Experiment> entities = (ArrayList<Experiment>) DataLoadService
				.loadComplexPreparedStatementMultipleEntities(sql,
						new Object[] { demandSegmentSetId, bookingPeriodLength }, new ExperimentMapper(), jdbcTemplate);

		return entities;
	}

	@Override
	public ArrayList<Experiment> getAllExperimentsWithOrderRequestSetOutputByDemandSegmentSetId(
			int demandSegmentSetId) {
		String sql = "SELECT distinct "+SettingsProvider.database+".experiment.* FROM "+SettingsProvider.database+".experiment "
				+ "LEFT JOIN "+SettingsProvider.database+".run ON (experiment.exp_id = run.run_experiment) "
				+ "LEFT JOIN "+SettingsProvider.database+".r_run_v_order_request_set ON (run.run_id=r_run_v_order_request_set.run_ors_run) "
				+ "LEFT JOIN "+SettingsProvider.database+".order_request_set ON (order_request_set.ors_id=r_run_v_order_request_set.run_ors_ors) "
				+ "LEFT JOIN "+SettingsProvider.database+".customer_set ON (customer_set.cs_id=order_request_set.ors_customer_set) "
				+ "WHERE "+SettingsProvider.database+".r_run_v_order_request_set.run_ors_ors>0 AND "+SettingsProvider.database+".customer_set.cs_original_demand_segment_set=?;";

		ArrayList<Experiment> entities = (ArrayList<Experiment>) DataLoadService
				.loadComplexPreparedStatementMultipleEntities(sql, new Object[] { demandSegmentSetId },
						new ExperimentMapper(), jdbcTemplate);

		return entities;
	}

	@Override
	public ArrayList<Experiment> getAllExperimentsWithOrderRequestSetOutputByAlternativeSetId(int alternativeSetId) {
		String sql = "select distinct exp_id,exp_name,exp_description,exp_responsible,exp_occasion,exp_region,exp_processType,exp_booking_period_length,exp_incentive_type,exp_depot,exp_booking_period_no,exp_copy_exp from "+SettingsProvider.database+".experiment "
				+ "join "+SettingsProvider.database+".run on (run.run_experiment=experiment.exp_id) "
				+ "join "+SettingsProvider.database+".r_run_v_order_request_set on (run.run_id=r_run_v_order_request_set.run_ors_run) "
				+ "join "+SettingsProvider.database+".order_request_set on (r_run_v_order_request_set.run_ors_ors=order_request_set.ors_id) "
				+ "join "+SettingsProvider.database+".customer_set on (customer_set.cs_id=order_request_set.ors_customer_set) "
				+ "join "+SettingsProvider.database+".demand_segment_set on (customer_set.cs_original_demand_segment_set=demand_segment_set.dss_id) "
				+ "where "+SettingsProvider.database+".demand_segment_set.dss_alternative_set=?;";

		ArrayList<Experiment> entities = (ArrayList<Experiment>) DataLoadService
				.loadComplexPreparedStatementMultipleEntities(sql, new Object[] { alternativeSetId },
						new ExperimentMapper(), jdbcTemplate);

		return entities;
	}
	
	@Override
	public ArrayList<Experiment> getAllExperimentsWithOrderRequestSetOutputByAlternativeSetIdAndOrderHorizonLength(int alternativeSetId, int orderHorizonLength) {
		String sql = "select distinct exp_id,exp_name,exp_description,exp_responsible,exp_occasion,exp_region,exp_processType,exp_booking_period_length,exp_incentive_type,exp_depot,exp_booking_period_no, exp_copy_exp from "+SettingsProvider.database+".experiment "
				+ "join "+SettingsProvider.database+".run on (run.run_experiment=experiment.exp_id) "
				+ "join "+SettingsProvider.database+".r_run_v_order_request_set on (run.run_id=r_run_v_order_request_set.run_ors_run) "
				+ "join "+SettingsProvider.database+".order_request_set on (r_run_v_order_request_set.run_ors_ors=order_request_set.ors_id) "
				+ "join "+SettingsProvider.database+".customer_set on (customer_set.cs_id=order_request_set.ors_customer_set) "
				+ "join "+SettingsProvider.database+".demand_segment_set on (customer_set.cs_original_demand_segment_set=demand_segment_set.dss_id) "
				+ "where "+SettingsProvider.database+".demand_segment_set.dss_alternative_set=? and exp_booking_period_length=?;";

		ArrayList<Experiment> entities = (ArrayList<Experiment>) DataLoadService
				.loadComplexPreparedStatementMultipleEntities(sql, new Object[] { alternativeSetId, orderHorizonLength},
						new ExperimentMapper(), jdbcTemplate);

		return entities;
	}

	@Override
	public ArrayList<OrderRequestSet> getAllOrderRequestSetsByExperimentId(int expId) {

		ArrayList<OrderRequestSet> requestSets = new ArrayList<OrderRequestSet>();
		String sql = "SELECT "+SettingsProvider.database+".order_request_set.* from "+SettingsProvider.database+".order_request_set "
				+ "JOIN "+SettingsProvider.database+".r_run_v_order_request_set ON (r_run_v_order_request_set.run_ors_ors=order_request_set.ors_id) "
				+ "LEFT JOIN "+SettingsProvider.database+".run ON (r_run_v_order_request_set.run_ors_run=run.run_id) "
				+ "LEFT JOIN "+SettingsProvider.database+".experiment ON (experiment.exp_id=run.run_experiment) "
				+ "WHERE "+SettingsProvider.database+".experiment.exp_id=?";

		requestSets = (ArrayList<OrderRequestSet>) DataLoadService.loadComplexPreparedStatementMultipleSetEntities(sql,
				new Object[] { expId }, new OrderRequestSetMapper(), jdbcTemplate);

		return requestSets;
	}

	@Override
	public Integer persistCompleteOrderRequestAndCustomerSet(SetEntity setEntity) {
		OrderRequestSet set = (OrderRequestSet) setEntity;
		CustomerDataServiceImpl csDs = new CustomerDataServiceImpl();
		csDs.setJdbcTemplate(jdbcTemplate);

		
		final ArrayList<OrderRequest> requests = set.getElements();

		int lastCustomerId =getHighestCustomerId();
		int lastOrderRequestId =getHighestOrderRequestId();

		//Set customer and request ids, collect all in different lists
		final ArrayList<OrderRequest> requestsToSave = new ArrayList<OrderRequest>();
		final ArrayList<Customer> customersToSave = new ArrayList<Customer>();
		final ArrayList<AlternativePreference> preferencesAll= new ArrayList<AlternativePreference>();
		for (int i = 0; i < requests.size(); i++) {
			OrderRequest orderRequest = requests.get(i);
			Customer customer = orderRequest.getCustomer();
			customer.setId(++lastCustomerId);
			customersToSave.add(customer);
			orderRequest.setCustomerId(customer.getId());
			orderRequest.setId(++lastOrderRequestId);
			requestsToSave.add(orderRequest);

			ArrayList<AlternativePreference> alternativePreferences =null;
			if (set.getPreferencesSampled()) {
				HashMap<Integer, Double> preferences = orderRequest.getAlternativePreferences();
				for (Integer key : preferences.keySet()) {
					AlternativePreference alt = new AlternativePreference();
					alt.setAlternativeId(key);
					alt.setOrderRequestId(orderRequest.getId());
					alt.setUtilityValue(preferences.get(key));
					preferencesAll.add(alt);
				}

			}
		}

		//Save complete customer set
		CustomerSet cSet = set.getCustomerSet();
		cSet.setElements(customersToSave);
		final Integer customerSetId =csDs.persistCompleteEntitySetWithPredefinedIds(cSet);

		// Save complete order request set
		set.setCustomerSetId(customerSetId);
		set.setElements(requestsToSave);
		final int orderRequestSetId = this.persistEntitySet(set);

		DataLoadService.persistAll("order_request", 8,
				"orr_id, orr_set, orr_customer, orr_content_type, orr_basket_value, orr_basket_volume, orr_basket_packageno, orr_t",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return requestsToSave.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						OrderRequest orderRequest = requestsToSave.get(i);
						ps.setInt(1, orderRequest.getId());
						ps.setInt(2, orderRequestSetId);
						ps.setInt(3, orderRequest.getCustomerId());
						ps.setObject(4, orderRequest.getOrderContentTypeId(), Types.INTEGER);
						ps.setObject(5, orderRequest.getBasketValue(), Types.FLOAT);
						ps.setObject(6, orderRequest.getBasketVolume(), Types.FLOAT);
						ps.setObject(7, orderRequest.getPackageno(), Types.INTEGER);
						ps.setObject(8, orderRequest.getArrivalTime(), Types.INTEGER);

					}
				}, jdbcTemplate);


		if(preferencesAll.size()>0){
			//double startTime=System.currentTimeMillis();
			DataLoadService.persistAll("r_request_v_alternative", 3, "re_alt_re, re_alt_alt, utility",
					new BatchPreparedStatementSetter() {
						public int getBatchSize() {
							return preferencesAll.size();
						}

						public void setValues(PreparedStatement ps, int i) throws SQLException {

							AlternativePreference pref = (AlternativePreference) preferencesAll.get(i);
							ps.setInt(1, pref.getOrderRequestId());
							ps.setInt(2, pref.getAlternativeId());
							ps.setObject(3, pref.getUtilityValue(), Types.FLOAT);

						}
					}, jdbcTemplate);
			
		//	System.out.println("Persist pref-time:"+ (System.currentTimeMillis()-startTime)+" and amount: "+preferences.size());
		}

		this.currentSet = null;
		if (this.entitySets != null)
			this.entitySets.add(setEntity);
		return orderRequestSetId;
	}

}
