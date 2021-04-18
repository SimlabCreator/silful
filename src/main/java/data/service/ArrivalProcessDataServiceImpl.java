package data.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

import org.springframework.jdbc.core.PreparedStatementCreator;

import data.entity.ArrivalProcess;
import data.entity.Entity;
import data.mapper.ArrivalProcessMapper;

public class ArrivalProcessDataServiceImpl extends ArrivalProcessDataService {

	

	public Entity getById(int entityId) {
		
		if(this.entities!= null){
			for(int i=0; i < this.entities.size(); i++){
				if(((ArrivalProcess)this.entities.get(i)).getId()==entityId){
					return this.entities.get(i);
				}
			}
		}
		
		Entity entity = new ArrivalProcess();
	//	System.out.println("Start"+System.currentTimeMillis());
		entity = DataLoadService.loadById("arrival_process", "arr_id", entityId, new ArrivalProcessMapper(), jdbcTemplate);
	//	System.out.println("End"+System.currentTimeMillis());
	    return entity;
	}
	

	public ArrayList<Entity> getAll() {
		if (entities == null) {

			entities = DataLoadService.loadAllFromClass("arrival_process", new ArrivalProcessMapper(), jdbcTemplate);

		}

		return entities;
	}

	

	@Override
	public Integer persist(Entity entity) {

		final ArrivalProcess arrivalProcess = (ArrivalProcess) entity;
		final String SQL = DataLoadService.buildInsertSQL("arrival_process", 3,
				"arr_name,arr_lambda_factor,arr_pd");

		Integer id = DataLoadService.persist(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(SQL, new String[] { "arr_id" });
				ps.setObject(1, arrivalProcess.getName(), Types.VARCHAR);
				ps.setObject(2, arrivalProcess.getFactor(), Types.FLOAT);
				ps.setObject(3, arrivalProcess.getProbabilityDistributionId(), Types.INTEGER);
				return ps;
			}
		}, jdbcTemplate);
		
		arrivalProcess.setId(id);

		return id;
	}	

}
