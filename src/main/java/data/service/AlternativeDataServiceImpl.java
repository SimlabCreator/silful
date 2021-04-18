package data.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;

import data.entity.Alternative;
import data.entity.AlternativeOffer;
import data.entity.AlternativeSet;
import data.entity.Entity;
import data.entity.Order;
import data.entity.SetEntity;
import data.entity.TimeWindow;
import data.mapper.AlternativeMapper;
import data.mapper.AlternativeOfferMapper;
import data.mapper.AlternativeSetMapper;
import data.mapper.TimeWindowMapper;
import data.utility.CombinedIdWithoutValue;
import logic.utility.SettingsProvider;

public class AlternativeDataServiceImpl extends AlternativeDataService {

	AlternativeSet currentSet;
	@Override
	public ArrayList<SetEntity> getAllSets() {

		if (entitySets == null) {

			entitySets = DataLoadService.loadAllFromSetClass("alternative_set", new AlternativeSetMapper(),
					jdbcTemplate);

		}

		return entitySets;
	}

	@Override
	public SetEntity getSetById(int id) {

		SetEntity alternativeSet = new AlternativeSet();

		if (entitySets == null) {
			alternativeSet = DataLoadService.loadBySetId("alternative_set", "as_id", id, new AlternativeSetMapper(),
					jdbcTemplate);
		} else {

			for (int i = 0; i < entitySets.size(); i++) {
				if ((entitySets.get(i)).getId() == id) {
					alternativeSet = entitySets.get(i);
					return alternativeSet;
				}

			}

		}
		return alternativeSet;
	}


	public ArrayList<Alternative> getAllElementsBySetId(int setId) {

		ArrayList<Alternative> entities = (ArrayList<Alternative>) DataLoadService.loadMultipleRowsBySelectionId("alternative", "alt_set", setId,
				new AlternativeMapper(), jdbcTemplate);
		this.currentSet = (AlternativeSet) this.getSetById(setId);
		this.currentSet.setElements(entities);
		return entities;
	}


	public Alternative getElementById(int entityId) {

		if (this.currentSet != null) {
			for (int i = 0; i < this.currentSet.getElements().size(); i++) {
				if (this.currentSet.getElements().get(i).getId() == entityId) {
					return this.currentSet.getElements().get(i);
				}
			}
		}

		Entity alternative = new Alternative();

		alternative = DataLoadService.loadById("alternative", "alt_id", entityId, new AlternativeMapper(),
				jdbcTemplate);

		return (Alternative) alternative;
	}

	@Override
	public Integer persistElement(Entity entity) {

		final Alternative alternative = (Alternative) entity;
		final String SQL = DataLoadService.buildInsertSQL("alternative", 3, "alt_set, alt_time_of_day, alt_no_purchase_alternative");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "alt_id" });
				ps.setInt(1, alternative.getSetId());
				ps.setObject(2, alternative.getTimeOfDay(), Types.VARCHAR);
				ps.setObject(3, alternative.getNoPurchaseAlternative(), Types.BOOLEAN);
				return ps;
			}
		}, jdbcTemplate);

		return id;
	}

	@Override
	public Integer persistCompleteEntitySet(SetEntity setEntity) {
		final ArrayList<Alternative> alternatives = ((AlternativeSet) setEntity).getElements();
		final int setId = this.persistEntitySet(setEntity);
		

		DataLoadService.persistAll("alternative", 3, "alt_set, alt_time_of_day, alt_no_purchase_alternative", new BatchPreparedStatementSetter() {

			public int getBatchSize() {
				return alternatives.size();
			}

			public void setValues(PreparedStatement ps, int i) throws SQLException {

				Alternative alternative = alternatives.get(i);
				ps.setInt(1, setId);
				ps.setObject(2, alternative.getTimeOfDay(), Types.VARCHAR);
				ps.setObject(3, alternative.getNoPurchaseAlternative(), Types.BOOLEAN);

			}
		}, jdbcTemplate);

		this.currentSet = null;
		if (this.entitySets != null)
			this.entitySets.add(setEntity);
		return setId;

	}

	@Override
	protected Integer persistEntitySet(SetEntity setEntity) {

		final AlternativeSet alternativeSet = (AlternativeSet) setEntity;

		final String SQL = DataLoadService.buildInsertSQL("alternative_set", 2, "as_name, as_tws");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "as_id" });
				ps.setString(1, alternativeSet.getName());
				ps.setObject(2, alternativeSet.getTimeWindowSetId(), Types.INTEGER);

				return ps;
			}
		}, jdbcTemplate);

		alternativeSet.setId(id);
		alternativeSet.setElements(null);
		return id;

	}

	@Override
	public ArrayList<Alternative> getAvailableAlternativesByOrderId(Integer orderId) {
		String sql = "SELECT alternative.alt_id AS alt_id, alternative.alt_set AS alt_set, alternative.alt_time_of_day as alt_time_of_day FROM (SELECT oa.order_alternative_ava_alt as alt_id FROM "+SettingsProvider.database+".r_order_v_alternative_available AS oa WHERE oa.order_alternative_ava_ord=?) AS orderId "
				+ "LEFT JOIN "+SettingsProvider.database+".alternative ON(alternative.alt_id=orderId.alt_id);";

		ArrayList<Alternative> alternatives = (ArrayList<Alternative>) DataLoadService.loadComplexPreparedStatementMultipleEntities(sql,
				new Object[] { orderId }, new AlternativeMapper(), jdbcTemplate);
		
		return alternatives;
	}

	@Override
	public ArrayList<AlternativeOffer> getOfferedAlternativesByOrderId(Integer orderId) {

		ArrayList<AlternativeOffer> alternatives = (ArrayList<AlternativeOffer>) DataLoadService.loadMultipleRowsBySelectionId("r_order_v_alternative_offered",
				"order_alternative_off_ord", orderId, new AlternativeOfferMapper(), jdbcTemplate);

		return alternatives;
	}

	@Override
	public void persistAvailableAlternatives(Order order) {

		final ArrayList<Alternative> availableAlt = order.getAvailableAlternatives();
		final Integer orderId = order.getId();
		DataLoadService.persistAll("r_order_v_alternative_available", 2,
				"order_alternative_ava_ord, order_alternative_ava_alt", new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return availableAlt.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						Alternative alt = availableAlt.get(i);
						ps.setInt(1, orderId);
						ps.setInt(2, alt.getId());

					}
				}, jdbcTemplate);

	}

	@Override
	public void persistOfferedAlternatives(Order order) {

		final Integer orderId=order.getId();
		final ArrayList<AlternativeOffer> alternativeOffersToSave=order.getOfferedAlternatives();

		DataLoadService.persistAll("r_order_v_alternative_offered", 3,
				"order_alternative_off_ord, order_alternative_off_alt, order_alternative_off_incentive",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return alternativeOffersToSave.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						AlternativeOffer alternativeOff = alternativeOffersToSave.get(i);
						ps.setInt(1, orderId);
						ps.setInt(2, alternativeOff.getAlternativeId());
						ps.setObject(3, alternativeOff.getIncentive(), Types.FLOAT);

					}
				}, jdbcTemplate);
	}

	@Override
	public ArrayList<SetEntity> getAllSetsByTimeWindowSetId(Integer timeWindowSetId) {
		ArrayList<SetEntity> entities = new ArrayList<SetEntity>();
		if (this.entitySets == null) {
			entities = DataLoadService.loadMultipleSetsBySelectionId("alternative_set", "as_tws", timeWindowSetId,
					new AlternativeSetMapper(), jdbcTemplate);
		} else {
			for (int i = 0; i < this.entitySets.size(); i++) {
				if (((AlternativeSet) this.entitySets.get(i)).getTimeWindowSetId() == timeWindowSetId) {
					entities.add((AlternativeSet) this.entitySets.get(i));

				}
			}
		}

		return entities;
	}

	@Override
	public ArrayList<TimeWindow> getTimeWindowsByAlternativeId(Integer alternativeId) {
		String sql = "SELECT * FROM (SELECT at.alternative_tw_tw as tw_id_selection FROM "+SettingsProvider.database+".r_alternative_v_tw AS at WHERE at.alternative_tw_alt=?) AS twId "
				+ "LEFT JOIN "+SettingsProvider.database+".time_window ON(time_window.tw_id=twId.tw_id_selection);";

		ArrayList<TimeWindow> entities = (ArrayList<TimeWindow>) DataLoadService.loadComplexPreparedStatementMultipleEntities(sql,
				new Object[] { alternativeId }, new TimeWindowMapper(), jdbcTemplate);
		return entities;
	}

	@Override
	public void persistTimeWindowsOfAlternatives(ArrayList<Alternative> alternatives) {
		ArrayList<CombinedIdWithoutValue> pairs = new ArrayList<CombinedIdWithoutValue>();

		for (int i = 0; i < alternatives.size(); i++) {

			ArrayList<TimeWindow> timeWindows = alternatives.get(i).getTimeWindows();
			Integer alternativeId =alternatives.get(i).getId();
			for (int j = 0; i < timeWindows.size(); j++) {
				pairs.add(new CombinedIdWithoutValue(
						new Integer[] { alternativeId, ( timeWindows.get(j)).getId() }));
			}
		}
		;

		final ArrayList<CombinedIdWithoutValue> pairsToSave = pairs;

		DataLoadService.persistAll("r_alternative_v_tw", 2,
				"alternative_tw_alt, alternative_tw_tw", new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return pairsToSave.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						CombinedIdWithoutValue combinedIdWithoutValue = pairsToSave.get(i);
						ps.setInt(1, combinedIdWithoutValue.getIds()[0]);
						ps.setInt(2, combinedIdWithoutValue.getIds()[1]);

					}
				}, jdbcTemplate);

	}

}
