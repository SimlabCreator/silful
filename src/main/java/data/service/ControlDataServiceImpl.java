package data.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;

import data.entity.Control;
import data.entity.ControlSet;
import data.entity.DynamicProgrammingTree;
import data.entity.Entity;
import data.entity.SetEntity;
import data.mapper.ControlMapper;
import data.mapper.ControlSetMapper;
import data.mapper.DeliveryAreaDynamicProgrammingTreeMapper;
import data.mapper.DynamicProgrammingTreeMapper;

public class ControlDataServiceImpl extends ControlDataService {

	ControlSet currentSet;
	@Override
	public ArrayList<SetEntity> getAllSets() {

		if (entitySets == null) {

			entitySets = DataLoadService.loadAllFromSetClass("control_set", new ControlSetMapper(), jdbcTemplate);

		}

		return entitySets;
	}

	@Override
	public SetEntity getSetById(int id) {

		SetEntity controlSet = new ControlSet();

		if (entitySets == null) {
			controlSet = DataLoadService.loadBySetId("control_set", "cos_id", id, new ControlSetMapper(), jdbcTemplate);
		} else {

			for (int i = 0; i < entitySets.size(); i++) {
				if (((SetEntity) entitySets.get(i)).getId() == id) {
					controlSet = (SetEntity) entitySets.get(i);
					return controlSet;
				}

			}

		}
		return controlSet;
	}

	@Override
	public ArrayList<Control> getAllElementsBySetId(int setId) {

		ArrayList<Control> entities = (ArrayList<Control>) DataLoadService.loadMultipleRowsBySelectionId("control", "con_set", setId,
				new ControlMapper(), jdbcTemplate);
		this.currentSet = (ControlSet) this.getSetById(setId);
		this.currentSet.setElements(entities);
		return entities;
	}

	@Override
	public Control getElementById(int entityId) {

		if (this.currentSet != null) {
			for (int i = 0; i < this.currentSet.getElements().size(); i++) {
				if (this.currentSet.getElements().get(i).getId() == entityId) {
					return this.currentSet.getElements().get(i);
				}
			}
		}

		Entity control = new Control();

		control = DataLoadService.loadById("control", "con_id", entityId, new ControlMapper(), jdbcTemplate);

		return (Control) control;
	}

	@Override
	public Integer persistElement(Entity entity) {

		final Control control = (Control) entity;
		final String SQL = DataLoadService.buildInsertSQL("control", 5,
				"con_set,con_alternative,con_delivery_area,con_no, con_value_bucket");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "con_id" });
				ps.setInt(1, control.getSetId());
				ps.setObject(2, control.getAlternativeId(), Types.INTEGER);
				ps.setObject(3, control.getDeliveryAreaId(), Types.INTEGER);
				ps.setObject(4, control.getControlNumber(), Types.INTEGER);
				ps.setObject(5, control.getValueBucketId(), Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);

		control.setId(id);

		return id;
	}

	@Override
	public Integer persistCompleteEntitySet(SetEntity setEntity) {
		final ArrayList<Control> controls = ((ControlSet) setEntity).getElements();
		final int setId = this.persistEntitySet(setEntity);
		

		DataLoadService.persistAll("control", 5, "con_set,con_alternative,con_delivery_area,con_no, con_value_bucket",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return controls.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {

						Control control = controls.get(i);
						ps.setInt(1, setId);
						ps.setObject(2, control.getAlternativeId(), Types.INTEGER);
						ps.setObject(3, control.getDeliveryAreaId(), Types.INTEGER);
						ps.setObject(4, control.getControlNumber(), Types.INTEGER);
						ps.setObject(5, control.getValueBucketId(), Types.INTEGER);

					}
				}, jdbcTemplate);

		
		this.currentSet = null;
		if (this.entitySets != null)
			this.entitySets.add(setEntity);
		return setId;

	}

	@Override
	protected Integer persistEntitySet(SetEntity setEntity) {

		final ControlSet controlSet = (ControlSet) setEntity;

		final String SQL = DataLoadService.buildInsertSQL("control_set", 4,
				"cos_name, cos_alternative_set, cos_delivery_area_set, cos_value_bucket_set");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "cos_id" });
				ps.setObject(1, controlSet.getName(), Types.VARCHAR);
				ps.setObject(3, controlSet.getDeliveryAreaSetId(), Types.INTEGER);
				ps.setObject(2, controlSet.getAlternativeSetId(), Types.INTEGER);
				ps.setObject(4, controlSet.getValueBucketSetId(), Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);

		controlSet.setId(id);
		controlSet.setElements(null);

		return id;

	}

	@Override
	public ArrayList<SetEntity> getAllSetsByDeliveryAreaSetAndTimeWindowSetId(Integer deliveryAreaSetId,
			Integer alternativeSetId) {
		ArrayList<SetEntity> sets = DataLoadService.loadSetsByMultipleSelectionIds("control_set",
				new String[] { "cos_alternative_set", "cos_delivery_area_set" },
				new Integer[] { alternativeSetId, deliveryAreaSetId }, new ControlSetMapper(), jdbcTemplate);
		return sets;
	}

	@Override
	public ArrayList<SetEntity> getAllSetsByDeliveryAreaSetAndAlternativeSetAndValueBucketSetId(
			Integer deliveryAreaSetId, Integer alternativeSetId, Integer valueBucketSetId) {
		ArrayList<SetEntity> sets = DataLoadService.loadSetsByMultipleSelectionIds("control_set",
				new String[] { "cos_alternative_set", "cos_delivery_area_set", "cos_value_bucket_set" },
				new Integer[] { alternativeSetId, deliveryAreaSetId, valueBucketSetId }, new ControlSetMapper(),
				jdbcTemplate);
		return sets;
	}

	@Override
	public ArrayList<Entity> getAllDynamicProgrammingTreesByMultipleSelectionIds(Integer periodLength,
			Integer capacitySetId, Integer arrivalProcessId, Integer demandSegmentWeighting,
			Integer deliveryAreaSetId) {

		ArrayList<Entity> sets = (ArrayList<Entity>) DataLoadService.loadByMultipleSelectionIds("dynamic_programming_tree",
				new String[] { "dpt_t", "dpt_capacity_set", "dpt_arrival_process", "dpt_demand_segment_weighting",
						"dpt_delivery_area_set" },
				new Integer[] {periodLength, capacitySetId, arrivalProcessId, demandSegmentWeighting, deliveryAreaSetId}, new DynamicProgrammingTreeMapper(),
				jdbcTemplate);
		return sets;
	}

	@Override
	public Integer persistDynamicProgrammingTree(Entity tree) {
		final DynamicProgrammingTree finalTree = (DynamicProgrammingTree) tree;

		final String SQL = DataLoadService.buildInsertSQL("dynamic_programming_tree", 6,
				"dpt_t, dpt_capacity_set,dpt_arrival_process,dpt_demand_segment_weighting,dpt_delivery_area_set, dpt_name");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "dpt_id" });
				ps.setObject(1, finalTree.getT(), Types.INTEGER);
				ps.setObject(2, finalTree.getCapacitySetId(), Types.INTEGER);
				ps.setObject(3, finalTree.getArrivalProcessId(), Types.INTEGER);
				ps.setObject(4, finalTree.getDemandSegmentWeightingId(), Types.INTEGER);
				ps.setObject(5, finalTree.getDeliveryAreaSetId(), Types.INTEGER);
				ps.setObject(6, finalTree.getName(), Types.VARCHAR);
				return ps;
			}
		}, jdbcTemplate);

		finalTree.setId(id);
		
		this.persistDeliveryAreaTrees(finalTree);

		return id;
		
	}
	
	private void persistDeliveryAreaTrees(DynamicProgrammingTree tree){
		
		final DynamicProgrammingTree finalTree = tree;
		final HashMap<Integer, String> trees = finalTree.getTrees();
		final Iterator<Integer> areas = trees.keySet().iterator();
		
		try{
		DataLoadService.persistAll("r_dynamic_programming_tree_v_delivery_area", 3, "dpt_da_dpt,dpt_da_da,dpt_da_tree",
				new BatchPreparedStatementSetter() {

					public int getBatchSize() {
						return trees.size();
					}

					public void setValues(PreparedStatement ps, int i) throws SQLException {
						Integer area = areas.next();
						String treeString = trees.get(area);
						ps.setInt(1, finalTree.getId());
						ps.setObject(2, area, Types.INTEGER);
						ps.setObject(3, treeString, Types.LONGVARCHAR);

					}
				}, jdbcTemplate);
		}catch (Exception ex) {
		   System.out.println("Could not save trees in db");
		}

		finalTree.setTrees(null);
	}

	@Override
	public DynamicProgrammingTree getDynamicProgrammingTreeById(Integer treeId) {
		Entity tree = new DynamicProgrammingTree();

		tree = DataLoadService.loadById("dynamic_programming_tree", "dpt_id", treeId, new DynamicProgrammingTreeMapper(), jdbcTemplate);

		return (DynamicProgrammingTree) tree;
	}

	@Override
	public HashMap<Integer, String> getAllTreesByDynamicProgrammingTreeId(Integer treeId) {
		HashMap<Integer, String> trees = new HashMap<Integer, String>();
		DataLoadService.loadMultipleRowsBySelectionId("r_dynamic_programming_tree_v_delivery_area", "dpt_da_dpt", treeId, new DeliveryAreaDynamicProgrammingTreeMapper(trees), jdbcTemplate);
		System.out.println("Number of trees: "+trees.size());
		return trees;
	}
}
