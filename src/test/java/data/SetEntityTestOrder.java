package data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import data.entity.Order;
import data.entity.OrderSet;
import data.entity.SetEntity;
import data.service.OrderDataService;
import data.service.OrderDataServiceImpl;
import data.utility.DataServiceProvider;
import data.utility.JDBCTemplateProvider;

public class SetEntityTestOrder {
	 
	private static OrderDataService service;
	 
	@BeforeClass
	public static void start() {
		service = DataServiceProvider.getOrderDataServiceImplInstance();

	}
 
	@AfterClass
	public static void end() {

	}
	
	@Test
	public void persistAndGetById() {
		
		OrderSet orderSet = new OrderSet();
		orderSet.setName("test");
		
		
		Order order = new Order();
		order.setOrderRequestId(1);
		
		Order order2 = new Order();
		order2.setOrderRequestId(1);
		
		ArrayList<Order> entities = new ArrayList<Order>();
		entities.add(order);
		entities.add(order2);
		
		orderSet.setElements(entities);
		
		
		Integer id = service.persistCompleteEntitySet(orderSet);
		
		ArrayList<SetEntity> persistedEntities = service.getAllSets();
		
		assertEquals(persistedEntities.get(0).getId()== 0, false); 
		
		OrderDataService service2= new OrderDataServiceImpl();
		service2.setJdbcTemplate(JDBCTemplateProvider.getInstance());
		OrderSet persistedSet = (OrderSet) service2.getSetById(id);
		
		assertEquals(persistedSet.getName(), orderSet.getName()); 
		assertEquals(((Order) persistedSet.getElements().get(0))!=null, true);
		
	}
 
	

}