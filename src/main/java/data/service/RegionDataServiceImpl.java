package data.service;

import java.util.ArrayList;

import data.entity.Entity;
import data.entity.Node;
import data.entity.NodeDistance;
import data.entity.Region;
import data.mapper.NodeDistanceMapper;
import data.mapper.NodeMapper;
import data.mapper.RegionMapper;
import logic.utility.SettingsProvider;
/**
 * Implementation for MySQL database
 * 
 * @author M. Lang
 *
 */
public class RegionDataServiceImpl extends RegionDataService{

	
	private ArrayList<Entity> regions;
	private Region currentRegion;
	
	public ArrayList<Entity> getAll() {
		
		if(regions==null){
			
			regions = DataLoadService.loadAllFromClass("region", new RegionMapper(), jdbcTemplate);

		}
		
		return regions;
	}

	public Entity getById(Integer id) {
		
		
		if(currentRegion!=null && currentRegion.getId()==id){
			//Just return current region
		}else if(regions==null){ //Load it
			currentRegion = (Region) DataLoadService.loadById("region", "reg_id", id, new RegionMapper(), jdbcTemplate);
		}else{ //Get it from the list
			
			for(int i=0; i < regions.size(); i++){
				if(((Region) regions.get(i)).getId()==id) {
					currentRegion=(Region) regions.get(i);
					return currentRegion;
				}
				
			}
			
		}

		return currentRegion;
		
	}




	public ArrayList<Node> getNodesByRegionId(Integer regionId) {
		ArrayList<Node> entities = (ArrayList<Node>) DataLoadService.loadMultipleRowsBySelectionId("node", "nod_region", regionId,
				new NodeMapper(), jdbcTemplate);
		this.currentRegion=null;
		return entities;
	}

	@Override
	public Node getNodeById(Long nodeId, Integer regionId) {
		
		Region region = (Region) this.getById(regionId);
		for(int i=0; i < region.getNodes().size(); i++){
			if((region.getNodes().get(i)).getLongId()==nodeId){
				return (region.getNodes().get(i));
			}
		}
		
		return null;
	}
	
	@Override
	public Node getNodeById(Long nodeId) {
		
		Node node = new Node();
		
		node = (Node) DataLoadService.loadByLongId("node", "nod_id", nodeId, new NodeMapper(), jdbcTemplate);
		
	    return node;
	}

	@Override
	public ArrayList<NodeDistance> getNodeDistancesByNodeId(Long nodeId) {
		
		String sql = "SELECT * FROM "+SettingsProvider.database+".distance WHERE dis_node1=? OR dis_node2=?";
		
		ArrayList<NodeDistance> entities = (ArrayList<NodeDistance>) DataLoadService.loadComplexPreparedStatementMultipleEntities(sql, new Object[]{nodeId, nodeId},
				new NodeDistanceMapper(), jdbcTemplate);
		
		return entities;
	}

	@Override
	public ArrayList<NodeDistance> getNodeDistancesByRegionId(Integer regionId) {
		
		String sql = "SELECT * FROM (SELECT * FROM "+SettingsProvider.database+".node WHERE nod_region=?) AS nod "+
		"LEFT JOIN "+SettingsProvider.database+".distance ON (distance.dis_node1=nod.nod_id)";
		
		ArrayList<NodeDistance> entities =  (ArrayList<NodeDistance>) DataLoadService.loadComplexPreparedStatementMultipleEntities(sql, new Object[]{regionId},
				new NodeDistanceMapper(), jdbcTemplate);
		for(int i=0; i < entities.size(); i++){

			if((entities.get(i)).getNode1Id().equals(0)&&(entities.get(i)).getNode2Id().equals(0)&&(entities.get(i)).getDistance().equals(0)){
				entities.remove(i);
				break;
			}
		}

		return entities;
	}





}
