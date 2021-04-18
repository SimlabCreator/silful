package data.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;

import data.entity.Entity;
import data.entity.ServiceTimeSegment;
import data.entity.ServiceTimeSegmentSet;
import data.entity.ServiceTimeSegmentWeight;
import data.entity.ServiceTimeSegmentWeighting;
import data.entity.SetEntity;
import data.entity.WeightEntity;
import data.entity.WeightingEntity;
import data.mapper.ServiceTimeSegmentMapper;
import data.mapper.ServiceTimeSegmentSetMapper;
import data.mapper.ServiceTimeSegmentWeightMapper;
import data.mapper.ServiceTimeSegmentWeightingMapper;

public class ServiceTimeSegmentDataServiceImpl extends ServiceTimeSegmentDataService {

	private ServiceTimeSegmentSet currentSet;
	private ServiceTimeSegmentWeighting currentWeighting;
	
	@Override
	public ArrayList<SetEntity> getAllSets() {

		if (entitySets == null) {

			entitySets = DataLoadService.loadAllFromSetClass("service_time_segment_set", new ServiceTimeSegmentSetMapper(), jdbcTemplate);

		}

		return entitySets;
	}

	@Override
	public SetEntity getSetById(int id) {

		SetEntity serviceTimeSegmentSet = new ServiceTimeSegmentSet();

		if (entitySets == null) {
			serviceTimeSegmentSet = DataLoadService.loadBySetId("service_time_segment_set", "sss_id", id, new ServiceTimeSegmentSetMapper(),
					jdbcTemplate);
		} else {

			for (int i = 0; i < entitySets.size(); i++) {
				if (((SetEntity) entitySets.get(i)).getId() == id) {
					serviceTimeSegmentSet = (SetEntity) entitySets.get(i);
					return serviceTimeSegmentSet;
				}

			}

		}
		return serviceTimeSegmentSet;
	}

	@Override
	public ArrayList<ServiceTimeSegment> getAllElementsBySetId(int setId) {

		ArrayList<ServiceTimeSegment> entities = (ArrayList<ServiceTimeSegment>) DataLoadService.loadMultipleRowsBySelectionId("service_time_segment", "sse_set", setId,
				new ServiceTimeSegmentMapper(), jdbcTemplate);
		this.currentSet=(ServiceTimeSegmentSet) this.getSetById(setId);
		this.currentSet.setElements(entities);
		return entities;
	}
	
	@Override
	public ServiceTimeSegment getElementById(int entityId) {
		
		if(this.currentSet!= null){
			for(int i=0; i < this.currentSet.getElements().size(); i++){
				if(( this.currentSet.getElements().get(i)).getId()==entityId){
					return this.currentSet.getElements().get(i);
				}
			}
		}
		
		Entity serviceTimeSegment = new ServiceTimeSegment();
		
		serviceTimeSegment = DataLoadService.loadById("service_time_segment", "sse_id", entityId, new ServiceTimeSegmentMapper(), jdbcTemplate);
		
	    return (ServiceTimeSegment) serviceTimeSegment;
	}
	
	@Override
	public ArrayList<WeightingEntity> getAllWeightings() {
		if (weightings == null) {

			weightings = DataLoadService.loadAllFromWeightingClass("service_time_segment_weighting", new ServiceTimeSegmentWeightingMapper(), jdbcTemplate);

		}

		return weightings;
	}

	@Override
	public ArrayList<WeightingEntity> getAllWeightingsBySetId(int setId) {
		
		ArrayList<WeightingEntity> weightingsBySet = new ArrayList<WeightingEntity>();
		
		weightingsBySet = DataLoadService.loadMultipleWeightingsBySelectionId("service_time_segment_weighting", "sws_segment_set", setId, new ServiceTimeSegmentWeightingMapper(), jdbcTemplate);
		return weightingsBySet;
	}

	@Override
	public WeightingEntity getWeightingById(int weightingId) {
		WeightingEntity serviceTimeSegmentWeighting = new ServiceTimeSegmentWeighting();

		if (weightings == null) {
			serviceTimeSegmentWeighting = DataLoadService.loadByWeightingId("service_time_segment_weighting", "sws_id", weightingId, new ServiceTimeSegmentWeightingMapper(),
					jdbcTemplate);
		} else {

			for (int i = 0; i < weightings.size(); i++) {
				if (((WeightingEntity) weightings.get(i)).getId() == weightingId) {
					serviceTimeSegmentWeighting = (WeightingEntity) weightings.get(i);
					return serviceTimeSegmentWeighting;
				}

			}

		}
		return serviceTimeSegmentWeighting;
	}

	@Override
	public ArrayList<ServiceTimeSegmentWeight> getAllWeightsByWeightingId(int weightingId) {
		
		ArrayList<ServiceTimeSegmentWeight> entities = (ArrayList<ServiceTimeSegmentWeight>) DataLoadService.loadMultipleWeightRowsBySelectionId("service_time_segment_weight", "ssw_set", weightingId,
				new ServiceTimeSegmentWeightMapper(), jdbcTemplate);
		this.currentWeighting=(ServiceTimeSegmentWeighting) this.getWeightingById(weightingId);
		this.currentWeighting.setWeights(entities);;
		return entities;
	}

	@Override
	public ServiceTimeSegmentWeight getWeightById(int weightId) {
		ServiceTimeSegmentWeight serviceTimeSegmentWeight = new ServiceTimeSegmentWeight();
		
		serviceTimeSegmentWeight = (ServiceTimeSegmentWeight) DataLoadService.loadByWeightId("service_time_segment_weight", "ssw_id", weightId, new ServiceTimeSegmentWeightMapper(), jdbcTemplate);
		
	    return serviceTimeSegmentWeight;
	}
	
	
	
	

	@Override
	public Integer persistElement(Entity entity) {

		final ServiceTimeSegment serviceTimeSegment = (ServiceTimeSegment) entity;
		final String SQL = DataLoadService.buildInsertSQL("service_time_segment", 2,
				"sse_set,sse_pd");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "sse_id" });
				ps.setObject(1, serviceTimeSegment.getSetId(), Types.INTEGER);
				ps.setObject(2, serviceTimeSegment.getProbabilityDistributionId(), Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);
		
		serviceTimeSegment.setId(id);

		return id;
	}

	@Override
	public Integer persistWeight(WeightEntity weight) {
		final WeightEntity serviceTimeSegmentWeight = weight;
		final String SQL = DataLoadService.buildInsertSQL("service_time_segment_weight",3,
				"ssw_set, ssw_service_segment, ssw_weight");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "ssw_id" });
				ps.setObject(1, serviceTimeSegmentWeight.getSetId(), Types.INTEGER);
				ps.setObject(2, serviceTimeSegmentWeight.getElementId(), Types.INTEGER);
				ps.setObject(3, serviceTimeSegmentWeight.getWeight(), Types.FLOAT);
				return ps;
			}
		}, jdbcTemplate);
		
		weight.setId(id);

		return id;
	}
	
	@Override
	public Integer persistCompleteEntitySet(SetEntity setEntity) {
		final ArrayList<ServiceTimeSegment> serviceTimes = ((ServiceTimeSegmentSet) setEntity).getElements();
		final int setId = this.persistEntitySet(setEntity);
		

		DataLoadService.persistAll("service_time_segment", 2,
				"sse_set,sse_pd",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return serviceTimes.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						ServiceTimeSegment serviceTimeSegment = serviceTimes.get(i);
						ps.setInt(1, setId);
						ps.setObject(2, serviceTimeSegment.getProbabilityDistributionId(), Types.INTEGER);

					}
				}, jdbcTemplate);


		
		this.currentSet = null;
		if (this.entitySets != null)
			this.entitySets.add(setEntity);
		return setId;

	}

	@Override
	protected Integer persistEntitySet(SetEntity setEntity) {

		final ServiceTimeSegmentSet serviceTimeSegmentSet = (ServiceTimeSegmentSet) setEntity;

		final String SQL = DataLoadService.buildInsertSQL("service_time_segment_set", 1, "sss_name");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "sss_id" });
				ps.setObject(1, serviceTimeSegmentSet.getName(), Types.VARCHAR);
				return ps;
			}
		}, jdbcTemplate);

		serviceTimeSegmentSet.setId(id);
		serviceTimeSegmentSet.setElements(null);

		return id;

	}

	@Override
	public Integer persistCompleteWeighting(WeightingEntity weighting) {
		
		final ArrayList<ServiceTimeSegmentWeight> weights = ((ServiceTimeSegmentWeighting) weighting).getWeights();
		
		final int setId = this.persistWeighting(weighting);
		

		DataLoadService.persistAll("service_time_segment_weight",3,
		"ssw_set, ssw_service_segment, ssw_weight",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return weights.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						ServiceTimeSegmentWeight serviceTimeSegmentWeight = weights.get(i);
						ps.setInt(1, setId);
						ps.setObject(2, serviceTimeSegmentWeight.getElementId(), Types.INTEGER);
						ps.setObject(3, serviceTimeSegmentWeight.getWeight(), Types.FLOAT);

					}
				}, jdbcTemplate);


		
		if (this.weightings != null)
			this.weightings.add(weighting);
		return setId;
	}

	@Override
	protected Integer persistWeighting(WeightingEntity weighting) {

		final ServiceTimeSegmentWeighting serviceTimeSegmentWeighting = (ServiceTimeSegmentWeighting) weighting;

		final String SQL = DataLoadService.buildInsertSQL("service_time_segment_weighting", 2, "sws_name, sws_segment_set");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "sws_id" });
				ps.setObject(1, serviceTimeSegmentWeighting.getName(), Types.VARCHAR);
				ps.setObject(2, serviceTimeSegmentWeighting.getSetEntityId(), Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);

		serviceTimeSegmentWeighting.setId(id);
		serviceTimeSegmentWeighting.setWeights(null);

		return id;

	}

	

}
