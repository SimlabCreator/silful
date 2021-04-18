package data.utility;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Database connection template (Spring JDBCTemplate) to MySQL data source
 * Singleton (only one object -> use getInstance())
 * 
 * @author M. Lang
 *
 */
public class JDBCTemplateProvider {

	
	private  static BasicDataSource ds;
	
	private static JdbcTemplate jdbcTemplateInstance;

	private JDBCTemplateProvider() {
		
	}
	
	private static void initDataSource() {
		  	ds = new BasicDataSource();
	        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");

	        
	        /**	If you get errors here, please create a java class under data.utility folder with code:
	         * 
					package data.utility; 
					
					public class LogInData {
						
						public static String getUsername() {
						    return USER_NAME_IN_THE_DATABASE; //e.g. "root"
						}
						
						public static String getPassword() {
						    return PASSWORT_FOR_THE_DATABASE; 
						}
						
						    public static String getUrl(){
						    return DATABASE_URL; // e.g. "jdbc:mysql://localhost:3306/SimLab?autoReconnect=true&useSSL=false"
						}
					}
				This class will not be ignored by Bitbucket and stay locally.
			 */ 	        	
	        ds.setUsername(LogInData.getUsername());
		    ds.setPassword(LogInData.getPassword());


	        ds.setUrl(LogInData.getUrl());
	       
	     // the settings below are optional -- dbcp can work with defaults
	        ds.setMinIdle(5);
	        ds.setMaxIdle(20);
	        ds.setMaxOpenPreparedStatements(180);
	        
	        jdbcTemplateInstance = new JdbcTemplate(ds);
	}

	public static JdbcTemplate getInstance() {
		if(jdbcTemplateInstance==null) initDataSource();
		return jdbcTemplateInstance;
	}

}