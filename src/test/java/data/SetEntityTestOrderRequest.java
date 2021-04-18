package data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import data.entity.OrderRequest;
import data.entity.OrderRequestSet;
import data.entity.SetEntity;
import data.service.OrderRequestDataService;
import data.service.OrderRequestDataServiceImpl;
import data.utility.DataServiceProvider;
import data.utility.JDBCTemplateProvider;

public class SetEntityTestOrderRequest {
	 
	private static OrderRequestDataService service;
	 
	@BeforeClass
	public static void start() {
		service = DataServiceProvider.getOrderRequestDataServiceImplInstance();

	}
 
	@AfterClass
	public static void end() {

	}
	
	@Test
	public void persistAndGetById() {
		
		OrderRequestSet orderRequestSet = new OrderRequestSet();
		orderRequestSet.setName("test");
		
		OrderRequest orderRequest = new OrderRequest();
		orderRequest.setCustomerId(1);
		orderRequest.setOrderContentTypeId(1);
		
		OrderRequest orderRequest2 = new OrderRequest();
		orderRequest2.setCustomerId(1);
		orderRequest2.setOrderContentTypeId(1);
		
		ArrayList<OrderRequest> entities = new ArrayList<OrderRequest>();
		entities.add(orderRequest);
		entities.add(orderRequest2);
		
		orderRequestSet.setElements(entities);
	
		Integer id = service.persistCompleteEntitySet(orderRequestSet);
		
		ArrayList<SetEntity> persistedEntities = service.getAllSets();
		
		assertEquals(persistedEntities.get(0).getId()==0, false); 
		
		OrderRequestDataService service2= new OrderRequestDataServiceImpl();
		service2.setJdbcTemplate(JDBCTemplateProvider.getInstance());
		
		OrderRequestSet persistedSet = (OrderRequestSet) service2.getSetById(id);
		
		assertEquals(persistedSet.getName(), orderRequestSet.getName()); 
		assertEquals(((OrderRequest) persistedSet.getElements().get(0))!=null, true);
		
	}
 
	

}