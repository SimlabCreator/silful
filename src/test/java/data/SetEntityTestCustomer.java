package data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import data.entity.Customer;
import data.entity.CustomerSet;
import data.entity.SetEntity;
import data.service.CustomerDataService;
import data.service.CustomerDataServiceImpl;
import data.utility.DataServiceProvider;
import data.utility.JDBCTemplateProvider;

public class SetEntityTestCustomer {
	 
	private static CustomerDataService service;
	 
	@BeforeClass
	public static void start() {
		service = DataServiceProvider.getCustomerDataServiceImplInstance();

	}
 
	@AfterClass
	public static void end() {

	}
	
	@Test
	public void persistAndGetById() {
		
		CustomerSet customerSet = new CustomerSet();
		customerSet.setName("test");
		
		Customer customer = new Customer();
		customer.setLat( 49.00000);
		customer.setLon( 11.00000);
		
		Customer customer2 = new Customer();
		customer2.setLat(48.00000);
		customer2.setLon( 11.00000);
		
		
		ArrayList<Customer> entities = new ArrayList<Customer>();
		entities.add(customer);
		entities.add(customer2);
		
		customerSet.setElements(entities);
	
		Integer id = service.persistCompleteEntitySet(customerSet);
		
		ArrayList<SetEntity> persistedEntities = service.getAllSets();
		
		assertEquals(persistedEntities.get(0).getId()==0, false); 
		
		CustomerDataService service2= new CustomerDataServiceImpl();
		service2.setJdbcTemplate(JDBCTemplateProvider.getInstance());
		
		CustomerSet persistedSet = (CustomerSet) service.getSetById(id);
		
		assertEquals(persistedSet.getName(), customerSet.getName()); 
		assertEquals(( persistedSet.getElements().get(0))!=null, true);
		
	}
 
	

}