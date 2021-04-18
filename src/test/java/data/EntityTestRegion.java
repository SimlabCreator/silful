package data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import data.entity.Entity;
import data.entity.Node;
import data.entity.NodeDistance;
import data.entity.Region;
import data.service.RegionDataService;
import data.utility.DataServiceProvider;

public class EntityTestRegion {
	 
	private RegionDataService service;
	 
	@BeforeClass
	public static void start() {
	
	}
 
	@AfterClass
	public static void end() {

	}
	
	@Test
	public void getByIdAndAll() {
		
		this.service = DataServiceProvider.getRegionDataServiceImplInstance();
		ArrayList<Entity> regions = this.service.getAll();
		
		assertEquals(regions!=null, true); 
		
		int firstRegion = ((Region) regions.get(0)).getId();
		this.service = DataServiceProvider.getRegionDataServiceImplInstance();
		Region region = (Region) this.service.getById(firstRegion);
		assertEquals(region!=null, true);
		ArrayList<Node> nodes = this.service.getNodesByRegionId(region.getId());
		assertEquals(nodes.get(0)!=null, true);
		
		ArrayList<NodeDistance> distances = this.service.getNodeDistancesByNodeId(((Node)nodes.get(0)).getLongId());
		assertEquals(distances.get(0)!=null, true);
		
	}

	

}