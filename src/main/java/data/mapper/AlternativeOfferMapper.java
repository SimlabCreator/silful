package data.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import data.entity.AlternativeOffer;
import data.entity.Entity;

/**
 * Maps SQL-result to entity object
 * 
 * @author M. Lang
 *
 */
public class AlternativeOfferMapper implements RowMapper<Entity> {

	public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
		AlternativeOffer alternativeOff = new AlternativeOffer();
		alternativeOff.setAlternativeId(rs.getInt("order_alternative_off_alt"));
		alternativeOff.setOrderId(rs.getInt("order_alternative_off_ord"));
		alternativeOff.setIncentive(rs.getObject("order_alternative_off_incentive", Double.class));
		return alternativeOff;
	}

}
