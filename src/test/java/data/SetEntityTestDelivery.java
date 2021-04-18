package data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import data.entity.Delivery;
import data.entity.DeliverySet;
import data.entity.SetEntity;
import data.service.DeliveryDataService;
import data.utility.DataServiceProvider;

public class SetEntityTestDelivery {
	 
	private static DeliveryDataService service;
	 
	@BeforeClass
	public static void start() {
		service = DataServiceProvider.getDeliveryDataServiceImplInstance();

	}
 
	@AfterClass
	public static void end() {

	}
	
	@Test
	public void persistAndGetById() {
		
		DeliverySet deliverySet = new DeliverySet();
		deliverySet.setName("test");
		
		
		Delivery delivery = new Delivery();
		delivery.setRouteElementId(1);
	
		Delivery delivery2 = new Delivery();
		delivery2.setRouteElementId(1);
		
		ArrayList<Delivery> entities = new ArrayList<Delivery>();
		entities.add(delivery);
		entities.add(delivery2);
		
		
		deliverySet.setElements(entities);
		service.persistCompleteEntitySet(deliverySet);
		
		ArrayList<SetEntity> persistedEntities = service.getAllSets();
		
		DeliverySet persistedDelivery = (DeliverySet) persistedEntities.get(0);
		assertEquals(persistedDelivery==null, false); 
		
		
		assertEquals(persistedDelivery.getName()!=null, true); 
		assertEquals(((Delivery)persistedDelivery.getElements().get(0))!=null, true);
		
	}
 
	

}