package data.service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import data.entity.Entity;
import data.entity.SetEntity;
import data.entity.WeightEntity;
import data.entity.WeightingEntity;
import logic.utility.SettingsProvider;

/**
 * Helper class for common database operations
 * 
 * @author M. Lang
 *
 */
public class DataLoadService {

	//private Boolean suppressWarningDB = true;
	/**
	 * Loads all objects from a given entity-class-table 
	 * 
	 * @param tablename
	 *            Respective table
	 * @param mapper
	 *            Object-mapper
	 * @return Lists of entities
	 */
	public static ArrayList<Entity> loadAllFromClass(String tablename, RowMapper<Entity> mapper,
			JdbcTemplate jdbcTemplate) {
		String SQL = "select * from "+SettingsProvider.database+"." + tablename;
		List<Entity> entityList = jdbcTemplate.query(SQL, mapper);
		ArrayList<Entity> entities = new ArrayList<Entity>();
		entities.addAll(entityList);
		return entities;
	}
	
	/**
	 * Loads all objects from a given entityset-class-table
	 * 
	 * @param tablename
	 *            Respective table
	 * @param mapper
	 *            Object-mapper
	 * @return Lists of entities
	 */
	public static ArrayList<SetEntity> loadAllFromSetClass(String tablename, RowMapper<SetEntity> mapper,
			JdbcTemplate jdbcTemplate) {
		String SQL = "select * from "+SettingsProvider.database+"."+tablename;
		List<SetEntity> entityList = jdbcTemplate.query(SQL, new Object[]{}, mapper);
		ArrayList<SetEntity> entities = new ArrayList<SetEntity>();
		entities.addAll(entityList);
		return entities;
	}
	
	/**
	 * Loads all objects from a given weighting-class-table
	 * 
	 * @param tablename
	 *            Respective table
	 * @param mapper
	 *            Object-mapper
	 * @return Lists of entities
	 */
	public static ArrayList<WeightingEntity> loadAllFromWeightingClass(String tablename, RowMapper<WeightingEntity> mapper,
			JdbcTemplate jdbcTemplate) {
		String SQL = "select * from "+SettingsProvider.database+"."+tablename;
		List<WeightingEntity> entityList = jdbcTemplate.query(SQL, new Object[]{}, mapper);
		ArrayList<WeightingEntity> entities = new ArrayList<WeightingEntity>();
		entities.addAll(entityList);
		return entities;
	}

	/**
	 * Load entity from table with simple primary Id
	 * @param tablename name of the class
	 * @param idname name of the primary Id column
	 * @param id the id of entity
	 * @param mapper entity mapper
	 * @param jdbcTemplate 
	 * @return entity
	 */
	public static Entity loadById(String tablename, String idname, Integer id, RowMapper<Entity> mapper,
			JdbcTemplate jdbcTemplate) {
		
		String SQL = "select * from "+SettingsProvider.database+"." + tablename + " where " + idname + " = ?";
		Entity entity = jdbcTemplate.queryForObject(SQL, new Object[] { id }, mapper);
		
		return entity;
		
	}
	
	/**
	 * Load entity from table with simple LONG primary Id
	 * @param tablename name of the class
	 * @param idname name of the primary Id column
	 * @param id the id of entity (LONG)
	 * @param mapper entity mapper
	 * @param jdbcTemplate 
	 * @return entity
	 */
	public static Entity loadByLongId(String tablename, String idname, Long id, RowMapper<Entity> mapper,
			JdbcTemplate jdbcTemplate) {

		String SQL = "select * from "+SettingsProvider.database+"." + tablename + " where " + idname + " = ?";
		Entity entity = jdbcTemplate.queryForObject(SQL, new Object[] { id }, mapper);
		
		return entity;
		
	}
	
	/**
	 * Load weight from table with simple primary Id
	 * @param tablename name of the class
	 * @param idname name of the primary Id column
	 * @param id the id of entity
	 * @param mapper weight mapper
	 * @param jdbcTemplate 
	 * @return entity
	 */
	public static WeightEntity loadByWeightId(String tablename, String idname, Integer id, RowMapper<WeightEntity> mapper,
			JdbcTemplate jdbcTemplate) {

		String SQL = "select * from "+SettingsProvider.database+"." + tablename + " where " + idname + " = ?";
		WeightEntity entity = jdbcTemplate.queryForObject(SQL, new Object[] { id }, mapper);
		
		return entity;
		
	}
	
	/**
	 * Load set entity from table with simple primary Id
	 * @param tablename name of the class
	 * @param idname name of the primary Id column
	 * @param id the id of set entity
	 * @param mapper set entity mapper
	 * @param jdbcTemplate 
	 * @return set entity
	 */
	public static SetEntity loadBySetId(String tablename, String idname, Integer id, RowMapper<SetEntity> mapper,
			JdbcTemplate jdbcTemplate) {

		String SQL = "select * from "+SettingsProvider.database+"." + tablename + " where " + idname + " = ?";
		SetEntity entity = jdbcTemplate.queryForObject(SQL, new Object[] { id }, mapper);
		
		return entity;
	}
	
	/**
	 * Load weighting from table with simple primary Id
	 * @param tablename name of the class
	 * @param idname name of the primary Id column
	 * @param id the id of set entity
	 * @param mapper weighting mapper
	 * @param jdbcTemplate 
	 * @return weighting
	 */
	public static WeightingEntity loadByWeightingId(String tablename, String idname, Integer id, RowMapper<WeightingEntity> mapper,
			JdbcTemplate jdbcTemplate) {

		String SQL = "select * from "+SettingsProvider.database+"." + tablename + " where " + idname + " = ?";
		WeightingEntity entity = jdbcTemplate.queryForObject(SQL, new Object[] { id }, mapper);
		
		return entity;
	}
	
	/**
	 * Load all entities of a given relation that fit to one selection criterion (for instance, one column of the primary Id)
	 * @param tablename Name of the class
	 * @param selectionName Name of the column
	 * @param selectionId Requested id
	 * @param mapper Entity mapper
	 * @param jdbcTemplate 
	 * @return list of entities
	 */
	public static ArrayList<? extends Entity> loadMultipleRowsBySelectionId(String tablename, String selectionName, Integer selectionId, RowMapper<Entity> mapper,
			JdbcTemplate jdbcTemplate) {

		String SQL = "select * from "+SettingsProvider.database+"." + tablename + " where " + selectionName + " = ?";
		
		List<Entity> entityList = jdbcTemplate.query(SQL, new Object[] { selectionId }, mapper);
		ArrayList<Entity> entities = new ArrayList<Entity>();
		entities.addAll(entityList);
		
		return entities;
	}
	
	public static void updateSetElementBySetId(String sql,JdbcTemplate jdbcTemplate){
		 jdbcTemplate.update(sql);
	}
	
	/**
	 * Load all weights of a given relation that fit to one selection criterion (for instance, one column of the primary Id)
	 * @param tablename Name of the class
	 * @param selectionName Name of the column
	 * @param selectionId Requested id
	 * @param mapper Weight mapper
	 * @param jdbcTemplate 
	 * @return list of weights
	 */
	public static ArrayList<? extends WeightEntity> loadMultipleWeightRowsBySelectionId(String tablename, String selectionName, Integer selectionId, RowMapper<WeightEntity> mapper,
			JdbcTemplate jdbcTemplate) {

		String SQL = "select * from "+SettingsProvider.database+"." + tablename + " where " + selectionName + " = ?";
		
		List<WeightEntity> entityList = jdbcTemplate.query(SQL, new Object[] { selectionId }, mapper);
		ArrayList<WeightEntity> entities = new ArrayList<WeightEntity>();
		entities.addAll(entityList);
		
		return entities;
	}
	
	/**
	 * Load all entity sets of a given relation that fit to one selection criterion (for instance, one column of the primary Id)
	 * @param tablename Name of the class
	 * @param selectionName Name of the column
	 * @param selectionId Requested id
	 * @param mapper Entity set mapper
	 * @param jdbcTemplate 
	 * @return list of entity sets
	 */
	public static ArrayList<SetEntity> loadMultipleSetsBySelectionId(String tablename, String selectionName, Integer selectionId, RowMapper<SetEntity> mapper,
			JdbcTemplate jdbcTemplate) {

		String SQL = "select * from "+SettingsProvider.database+"." + tablename + " where " + selectionName + " = ?";
		
		List<SetEntity> entityList = jdbcTemplate.query(SQL, new Object[] { selectionId }, mapper);
		ArrayList<SetEntity> entities = new ArrayList<SetEntity>();
		entities.addAll(entityList);
		
		return entities;
	}

	/**
	 * Load all weighting of a given relation that fit to one selection criterion (for instance, one column of the primary Id)
	 * @param tablename Name of the class
	 * @param selectionName Name of the column
	 * @param selectionId Requested id
	 * @param mapper Weighting mapper
	 * @param jdbcTemplate 
	 * @return list of weightings
	 */
	public static ArrayList<WeightingEntity> loadMultipleWeightingsBySelectionId(String tablename, String selectionName, Integer selectionId, RowMapper<WeightingEntity> mapper,
			JdbcTemplate jdbcTemplate) {

		String SQL = "select * from "+SettingsProvider.database+"." + tablename + " where " + selectionName + " = ?";
		
		List<WeightingEntity> entityList = jdbcTemplate.query(SQL, new Object[] { selectionId }, mapper);
		ArrayList<WeightingEntity> entities = new ArrayList<WeightingEntity>();
		entities.addAll(entityList);
		
		return entities;
	}
	
	/**
	 * Load all entities that fulfill the selection id restrictions
	 * Columnnames and ids have to have the same order
	 * @param tablename Name of the class
	 * @param idnames Names of the columns
	 * @param ids Requested ids
	 * @param mapper Entity mapper
	 * @param jdbcTemplate
	 * @return
	 */
	public static ArrayList<? extends Entity> loadByMultipleSelectionIds(String tablename, String[] idnames, Integer[] ids, RowMapper<Entity> mapper,
			JdbcTemplate jdbcTemplate) {

		String SQL = "select * from "+SettingsProvider.database+"." + tablename + " where ";
		
		for(int i=0; i < idnames.length; i++){
			
			if(i > 0) SQL+=" and ";
			SQL+=idnames[i]+" = ?";
			
		}
		
		List<Entity> entitiesList = jdbcTemplate.query(SQL, ids, mapper);
		ArrayList<Entity> entities = new ArrayList<Entity>();
		entities.addAll(entitiesList);
		return entities;
	}
	
	/**
	 * Load all set entities that fulfill the selection id restrictions
	 * Columnnames and ids have to have the same order
	 * @param tablename Name of the class
	 * @param idnames Names of the columns
	 * @param ids Requested ids
	 * @param mapper SetEntity mapper
	 * @param jdbcTemplate
	 * @return
	 */
	public static ArrayList<SetEntity> loadSetsByMultipleSelectionIds(String tablename, String[] idnames, Integer[] ids, RowMapper<SetEntity> mapper,
			JdbcTemplate jdbcTemplate) {

		String SQL = "select * from "+SettingsProvider.database+"." + tablename + " where ";
		
		for(int i=0; i < idnames.length; i++){
			
			if(i > 0) SQL+=" and ";
			SQL+=idnames[i]+" = ?";
			
		}
		
		List<SetEntity> entitiesList = jdbcTemplate.query(SQL, ids, mapper);
		ArrayList<SetEntity> entities = new ArrayList<SetEntity>();
		entities.addAll(entitiesList);
		return entities;
	}
	
	
	public static Integer persist(PreparedStatementCreator preparedStatementCreator, JdbcTemplate jdbcTemplate) {

		
		KeyHolder IdHolder = new GeneratedKeyHolder();
	
    	jdbcTemplate.update(preparedStatementCreator,
    	    IdHolder);

    	return IdHolder.getKey().intValue();
	}
	
	public static void persistWithoutId(PreparedStatementCreator preparedStatementCreator, JdbcTemplate jdbcTemplate) {

		
	
    	jdbcTemplate.update(preparedStatementCreator);

	}
	
	public static String buildInsertSQL(String tablename, Integer columnno, String columnnames){
		
		String tempSQL = "insert into "+SettingsProvider.database+"."+tablename+" ("+columnnames+") values (";
		while(columnno>0){
			tempSQL+="?";
			if(columnno>1) tempSQL+=",";
			columnno--;
		}		
		
		return tempSQL+")";
		
	};
	
	public static void persistAll(String tablename, Integer columnno, String columnnames, BatchPreparedStatementSetter batchPreparedStatementSetter, JdbcTemplate jdbcTemplate){
		
		String SQL = DataLoadService.buildInsertSQL(tablename, columnno, columnnames);
		
		jdbcTemplate.batchUpdate(SQL, batchPreparedStatementSetter);
			
		
	};
	
	
	/**
	 * Takes a complex sql-statement as string as well as the respective parameters and conducts the query
	 * @param sql Sql-statement
	 * @param mapper Entity-mapper
	 * @param jdbcTemplate
	 * @return list of entities
	 */
	public static ArrayList<? extends Entity> loadComplexPreparedStatementMultipleEntities(String sql, Object[] args, RowMapper<Entity> mapper,
				JdbcTemplate jdbcTemplate) {
			List<Entity> entityList = jdbcTemplate.query(sql, args, mapper);
			ArrayList<Entity> entities = new ArrayList<Entity>();
			entities.addAll(entityList);
			return entities;
	}
	

	
	public static ArrayList<Integer> loadListOfIds(String sql,  Object[] args, JdbcTemplate jdbcTemplate){
		ArrayList<Integer> ids = (ArrayList<Integer>) jdbcTemplate.queryForList(sql,Integer.class);
		return ids;
	}
	
	public static ArrayList<Integer> loadIntegersComplexPreparedStatementMultipleEntities(String sql,Object[] args,
			JdbcTemplate jdbcTemplate) {
	
		List<Integer> integerList=jdbcTemplate.queryForList(sql, Integer.class, args);
		ArrayList<Integer> entities = new ArrayList<Integer>();
		entities.addAll(integerList);
		return entities;
}
	
	/**
	 * Takes a complex sql-statement as string as well as the respective parameters and conducts the query
	 * @param sql Sql-statement
	 * @param mapper SetEntity-mapper
	 * @param jdbcTemplate
	 * @return list of set entities
	 */
	public static ArrayList<? extends SetEntity> loadComplexPreparedStatementMultipleSetEntities(String sql, Object[] args, RowMapper<SetEntity> mapper,
				JdbcTemplate jdbcTemplate) {
			List<SetEntity> entityList = jdbcTemplate.query(sql, args, mapper);
			ArrayList<SetEntity> entities = new ArrayList<SetEntity>();
			entities.addAll(entityList);
			return entities;
	}
	
	/**
	 * Takes a complex sql-statement as string as well as the respective parameters and conducts the query
	 * @param sql Sql-statement
	 * @param mapper Entity-mapper
	 * @param jdbcTemplate
	 * @return Requested entity
	 */
	public static Entity loadComplexPreparedStatement(String sql, Object[] args, RowMapper<Entity> mapper,
				JdbcTemplate jdbcTemplate) {
			Entity entity = jdbcTemplate.queryForObject(sql, args, mapper);
			return entity;
	}

}
