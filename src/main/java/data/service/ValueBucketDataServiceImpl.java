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
import data.entity.ValueBucket;
import data.entity.ValueBucketSet;
import data.mapper.ValueBucketMapper;
import data.mapper.ValueBucketSetMapper;

public class ValueBucketDataServiceImpl extends ValueBucketDataService {

	private ValueBucketSet currentSet;
	
	@Override
	public ArrayList<SetEntity> getAllSets() {

		if (entitySets == null) {

			entitySets = DataLoadService.loadAllFromSetClass("value_bucket_set", new ValueBucketSetMapper(), jdbcTemplate);

		}

		return entitySets;
	}

	@Override
	public SetEntity getSetById(int id) {

		SetEntity set = new ValueBucketSet();

		if (entitySets == null) {
			set = DataLoadService.loadBySetId("value_bucket_set", "vbs_id", id, new ValueBucketSetMapper(),
					jdbcTemplate);
		} else {

			for (int i = 0; i < entitySets.size(); i++) {
				if (((SetEntity) entitySets.get(i)).getId() == id) {
					set = (SetEntity) entitySets.get(i);
					return set;
				}

			}

		}
		return set;
	}

	@Override
	public ArrayList<ValueBucket> getAllElementsBySetId(int setId) {

		ArrayList<ValueBucket> entities = (ArrayList<ValueBucket>) DataLoadService.loadMultipleRowsBySelectionId("value_bucket", "vb_set", setId,
				new ValueBucketMapper(), jdbcTemplate);
		this.currentSet=(ValueBucketSet) this.getSetById(setId);
		this.currentSet.setElements(entities);
		return entities;
	}
	
	@Override
	public ValueBucket getElementById(int entityId) {
		
		if(this.currentSet!= null){
			for(int i=0; i <this.currentSet.getElements().size(); i++){
				if((this.currentSet.getElements().get(i)).getId()==entityId){
					return this.currentSet.getElements().get(i);
				}
			}
		}
		
		Entity valueBucket = new ValueBucket();
		
		valueBucket = DataLoadService.loadById("value_bucket", "vb_id", entityId, new ValueBucketMapper(), jdbcTemplate);
		
	    return (ValueBucket) valueBucket;
	}

	@Override
	public Integer persistElement(Entity entity) {

		final ValueBucket valueBucket = (ValueBucket) entity;
		final String SQL = DataLoadService.buildInsertSQL("value_bucket", 3,
				"vb_set, vb_bound_upper, vb_lower_bound");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "vb_id" });
				ps.setInt(1, valueBucket.getSetId());
				ps.setObject(2, valueBucket.getUpperBound(), Types.FLOAT);
				ps.setObject(3, valueBucket.getLowerBound(), Types.FLOAT);

				return ps;
			}
		}, jdbcTemplate);

		return id;
	}

	@Override
	public Integer persistCompleteEntitySet(SetEntity setEntity) {

		final ArrayList<ValueBucket> valueBuckets = ((ValueBucketSet) setEntity).getElements();
		
		final int setId = this.persistEntitySet(setEntity);
		

		DataLoadService.persistAll("value_bucket", 3,
				"vb_set, vb_bound_upper, vb_lower_bound", new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return valueBuckets.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						ValueBucket valueBucket = valueBuckets.get(i);
						ps.setInt(1, setId);
						ps.setObject(2, valueBucket.getUpperBound(), Types.FLOAT);
						ps.setObject(3, valueBucket.getLowerBound(), Types.FLOAT);


					}
				}, jdbcTemplate);

		
		this.currentSet = null;
		if (this.entitySets != null)
			this.entitySets.add(setEntity);
		return setId;

	}

	@Override
	protected Integer persistEntitySet(SetEntity setEntity) {

		final ValueBucketSet valueBucketSet = (ValueBucketSet) setEntity;

		final String SQL = DataLoadService.buildInsertSQL("value_bucket_set", 1, "vbs_name");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "vbs_id" });
				ps.setString(1, valueBucketSet.getName());
				return ps;
			}
		}, jdbcTemplate);

		valueBucketSet.setId(id);
		valueBucketSet.setElements(null);

		return id;

	}

}
