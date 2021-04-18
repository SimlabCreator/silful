#Run the following on every server start to increase size
SET GLOBAL max_allowed_packet=1073741824;

CREATE DATABASE simlab ;

CREATE TABLE IF NOT EXISTS simlab.process 
	(
		pro_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
		pro_name VARCHAR(200) NOT NULL,
		pro_description VARCHAR(255)
    ) ;


CREATE TABLE IF NOT EXISTS simlab.region
	(
		reg_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
		reg_name VARCHAR(200) NOT NULL,
		reg_point1_lat FLOAT,
		reg_point1_long FLOAT,
        reg_point2_lat FLOAT,
		reg_point2_long FLOAT,
        reg_average_km_per_hour FLOAT
    );



CREATE TABLE IF NOT EXISTS simlab.node
	(
        nod_id BIGINT, # specific id from open streetmap or other
		nod_lat FLOAT(10,7) NOT NULL,
		nod_long FLOAT(10,7) NOT NULL,
		nod_region INT NOT NULL,
		FOREIGN KEY (nod_region)
			REFERENCES simlab.region(reg_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE,
		PRIMARY KEY(nod_id, nod_region)
	);

CREATE TABLE IF NOT EXISTS simlab.distance
	(
		dis_node1 BIGINT NOT NULL,
		dis_node2 BIGINT NOT NULL,
        dis_region INT NOT NULL,
		dis_value FLOAT(13,3),
		FOREIGN KEY (dis_node1, dis_region)
			REFERENCES simlab.node(nod_id, nod_region)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (dis_node2, dis_region)
			REFERENCES simlab.node(nod_id, nod_region)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY (dis_node1, dis_node2, dis_region)
	);


CREATE TABLE IF NOT EXISTS simlab.kpi 
	(
		kpi_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
		kpi_name VARCHAR(100) NOT NULL,
		kpi_description VARCHAR(255)
    ) ;

CREATE TABLE IF NOT EXISTS simlab.delivery_area_set 
	(
		das_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
		das_name VARCHAR(200),
		das_description VARCHAR(255),
        das_region INT, # Can be null if it is a subset
        das_predefined BOOLEAN DEFAULT 0, # are areas defined in advance or result (summary) of demand clusters
        das_reasonable_area_no INT, # if areas are not predefined
        FOREIGN KEY (das_region)
			REFERENCES simlab.region(reg_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE
    ) ;
    

   

CREATE TABLE IF NOT EXISTS simlab.delivery_area
	(
		da_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
        da_set INT NOT NULL,
        da_point1_lat DOUBLE NOT NULL,
        da_point1_long DOUBLE NOT NULL,
		da_point2_lat DOUBLE NOT NULL,
        da_point2_long DOUBLE NOT NULL,
        da_center_lat DOUBLE,
        da_center_long DOUBLE,
        da_subset INT, # id of a delivery area set that is a subset of this one
        FOREIGN KEY (da_set)
			REFERENCES simlab.delivery_area_set(das_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (da_subset)
			REFERENCES simlab.delivery_area_set(das_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE
    ) ;

  
CREATE TABLE IF NOT EXISTS simlab.depot
(
	dep_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
    dep_lat FLOAT(10,7) NOT NULL,
    dep_long FLOAT(10,7) NOT NULL,
    dep_region INT NOT NULL,
    FOREIGN KEY(dep_region)
		REFERENCES simlab.region(reg_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS simlab.incentive_type
	(
		it_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        it_name VARCHAR(100)
    );
    
    CREATE TABLE IF NOT EXISTS simlab.experiment
	(
		exp_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        exp_name VARCHAR(200),
        exp_description VARCHAR(200),
		exp_responsible VARCHAR(20),
		exp_occasion VARCHAR(25),
        exp_copy_exp INT,
		exp_region INT,
		exp_processType INT,
        exp_booking_period_length INT,
        exp_incentive_type INT NULL,
        exp_depot INT,
        exp_booking_period_no INT NOT NULL,
        FOREIGN KEY (exp_copy_exp)
			REFERENCES simlab.experiment(exp_id)
			ON DELETE SET NULL
			ON UPDATE CASCADE,
		FOREIGN KEY (exp_region)
			REFERENCES simlab.region(reg_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE,
		FOREIGN KEY (exp_processType)
			REFERENCES simlab.process(pro_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE,
		FOREIGN KEY (exp_incentive_type)
			REFERENCES simlab.incentive_type(it_id)
			ON DELETE SET NULL
			ON UPDATE CASCADE,
		FOREIGN KEY (exp_depot)
			REFERENCES simlab.depot(dep_id)
			ON DELETE SET NULL
			ON UPDATE CASCADE
	);
    
CREATE TABLE IF NOT EXISTS simlab.run
	(
		run_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
       # run_description VARCHAR(50),
		run_datetime DATETIME DEFAULT CURRENT_TIMESTAMP,
        run_experiment INT NOT NULL,
        run_length BIGINT,
		#run_responsible VARCHAR(20),
		#run_occasion VARCHAR(25),
		#run_region INT,
		#run_processType INT,
        #run_booking_period_length INT,
        #run_incentive_type INT NULL,
        #run_booking_period_no INT NOT NULL,
		FOREIGN KEY (run_experiment)
			REFERENCES simlab.experiment(exp_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE
	);




CREATE TABLE IF NOT EXISTS simlab.r_run_v_kpi
	(
		run_kpi_run INT NOT NULL,
		run_kpi_kpi INT NOT NULL,
		run_kpi_value FLOAT NOT NULL,
        run_kpi_period TINYINT NOT NULL DEFAULT 0,
		FOREIGN KEY (run_kpi_run)
			REFERENCES simlab.run(run_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (run_kpi_kpi)
			REFERENCES simlab.kpi(kpi_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY (run_kpi_run, run_kpi_kpi, run_kpi_period)
	);
    
CREATE TABLE IF NOT EXISTS simlab.r_run_v_delivery_area_set
	(
		run_das_run INT NOT NULL,
		run_das_das INT NOT NULL,
        run_das_period TINYINT NOT NULL DEFAULT 0,
		FOREIGN KEY (run_das_run)
			REFERENCES simlab.run(run_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (run_das_das)
			REFERENCES simlab.delivery_area_set(das_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY (run_das_run, run_das_das, run_das_period)
	);
    
CREATE TABLE IF NOT EXISTS simlab.r_experiment_v_delivery_area_set
	(
		exp_das_exp INT NOT NULL,
		exp_das_das INT NOT NULL,
        exp_das_period TINYINT NOT NULL DEFAULT 0,
		FOREIGN KEY (exp_das_exp)
			REFERENCES simlab.experiment(exp_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (exp_das_das)
			REFERENCES simlab.delivery_area_set(das_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY (exp_das_exp, exp_das_das, exp_das_period)
	);
    
CREATE TABLE IF NOT EXISTS simlab.parameter_type
	(
		par_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
		par_name VARCHAR(100),
        par_description VARCHAR(50)
	);

CREATE TABLE IF NOT EXISTS simlab.r_experiment_v_parameter_type
	(
		exp_parameter_exp INT NOT NULL,
		exp_parameter_parameter INT NOT NULL,
        exp_parameter_value FLOAT,
        FOREIGN KEY (exp_parameter_exp)
			REFERENCES simlab.experiment(exp_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (exp_parameter_parameter)
			REFERENCES simlab.parameter_type(par_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY (exp_parameter_exp, exp_parameter_parameter)
	);
    
    CREATE TABLE IF NOT EXISTS simlab.r_run_v_parameter_type
	(
		run_parameter_run INT NOT NULL,
		run_parameter_parameter INT NOT NULL,
        run_parameter_value FLOAT,
        run_period INT, 
        FOREIGN KEY (run_parameter_run)
			REFERENCES simlab.run(run_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (run_parameter_parameter)
			REFERENCES simlab.parameter_type(par_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY (run_parameter_run, run_parameter_parameter)
	);
      

CREATE TABLE IF NOT EXISTS simlab.time_window_set 
	(
		tws_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
		tws_name VARCHAR(200),
        tws_overlapping BOOLEAN DEFAULT 0,
        tws_same_length BOOLEAN DEFAULT 1,
        tws_continuous BOOLEAN DEFAULT 0
    ) ;

CREATE TABLE IF NOT EXISTS simlab.time_window
	(
		tw_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
        tw_set INT NOT NULL,
        tw_start_time FLOAT,
        tw_end_time FLOAT,
        FOREIGN KEY (tw_set)
			REFERENCES simlab.time_window_set(tws_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE
    ) ;

CREATE TABLE IF NOT EXISTS simlab.r_run_v_time_window_set
	(
		run_tws_run INT NOT NULL,
        run_tws_tws INT NOT NULL,
        run_tws_period TINYINT NOT NULL DEFAULT 0,
        FOREIGN KEY (run_tws_run)
			REFERENCES simlab.run(run_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (run_tws_tws)
			REFERENCES simlab.time_window_set(tws_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(run_tws_run, run_tws_tws, run_tws_period)
	);
    
CREATE TABLE IF NOT EXISTS simlab.r_experiment_v_time_window_set
	(
		exp_tws_exp INT NOT NULL,
        exp_tws_tws INT NOT NULL,
        exp_tws_period TINYINT NOT NULL DEFAULT 0,
        FOREIGN KEY (exp_tws_exp)
			REFERENCES simlab.experiment(exp_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (exp_tws_tws)
			REFERENCES simlab.time_window_set(tws_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(exp_tws_exp, exp_tws_tws, exp_tws_period)
	);
    
CREATE TABLE IF NOT EXISTS simlab.alternative_set 
	(
		as_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
		as_name VARCHAR(200),
        as_tws INT NOT NULL,
        FOREIGN KEY (as_tws)
			REFERENCES simlab.time_window_set(tws_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE
    ) ;

# TODO: Maybe change time of day implementation if needed
CREATE TABLE IF NOT EXISTS simlab.alternative
	(
		alt_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
        alt_set INT NOT NULL,
        alt_time_of_day VARCHAR(25),
        alt_no_purchase_alternative BOOLEAN DEFAULT 0,
        FOREIGN KEY (alt_set)
			REFERENCES simlab.alternative_set(as_id)
            ON DELETE CASCADE
			ON UPDATE CASCADE
    ) ;

CREATE TABLE IF NOT EXISTS simlab.r_alternative_v_tw
	(
		alternative_tw_alt INT NOT NULL,
        alternative_tw_tw INT NOT NULL,
        PRIMARY KEY (alternative_tw_alt, alternative_tw_tw),
        FOREIGN KEY(alternative_tw_alt)
			REFERENCES simlab.alternative(alt_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY(alternative_tw_tw)
			REFERENCES simlab.time_window(tw_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE
	);
    
CREATE TABLE IF NOT EXISTS simlab.r_run_v_alternative_set
	(
		run_as_run INT NOT NULL,
        run_as_as INT NOT NULL,
        run_as_period TINYINT NOT NULL DEFAULT 0,
        FOREIGN KEY (run_as_run)
			REFERENCES simlab.run(run_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (run_as_as)
			REFERENCES simlab.alternative_set(as_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(run_as_run, run_as_as, run_as_period)
	);

CREATE TABLE IF NOT EXISTS simlab.r_experiment_v_alternative_set
	(
		exp_as_exp INT NOT NULL,
        exp_as_as INT NOT NULL,
        exp_as_period TINYINT NOT NULL DEFAULT 0,
        FOREIGN KEY (exp_as_exp)
			REFERENCES simlab.experiment(exp_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (exp_as_as)
			REFERENCES simlab.alternative_set(as_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(exp_as_exp, exp_as_as, exp_as_period)
	);
    
CREATE TABLE IF NOT EXISTS simlab.probability_distribution_type
	(
		pdt_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        pdt_name VARCHAR(200)
    );
    
#CREATE TABLE IF NOT EXISTS simlab.r_pd_type_v_parameter_type
#	(	
#		pdt_parameter_pd INT NOT NULL,
#		pdt_parameter_par INT NOT NULL,
 #       FOREIGN KEY (pdt_parameter_pd)
	#		REFERENCES simlab.probability_distribution_type(pdt_id)
	#		ON DELETE CASCADE
#			ON UPDATE CASCADE,
	#	FOREIGN KEY (pdt_parameter_par)
	#		REFERENCES simlab.parameter_type(par_id)
	#		ON DELETE CASCADE
	#		ON UPDATE CASCADE,
	#	PRIMARY KEY (pdt_parameter_pd, pdt_parameter_par)
  #  );
    
CREATE TABLE IF NOT EXISTS simlab.probability_distribution
	(
		pd_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        pd_name VARCHAR(200),
        pd_type INT NOT NULL,
        FOREIGN KEY (pd_type)
			REFERENCES simlab.probability_distribution_type(pdt_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE
    );
    
CREATE TABLE IF NOT EXISTS simlab.distribution_parameter_value
	(
		dpv_probability_distribution INT NOT NULL,
        dpv_parameter_type INT NOT NULL,
        dpv_value FLOAT,
        FOREIGN KEY (dpv_probability_distribution)
			REFERENCES simlab.probability_distribution(pd_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (dpv_parameter_type)
			REFERENCES simlab.parameter_type(par_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(dpv_probability_distribution, dpv_parameter_type)
    );
    


CREATE TABLE IF NOT EXISTS simlab.vehicle_type
	(
		veh_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        veh_capacity_volume FLOAT,
        veh_capacity_no FLOAT,
        veh_cooling BOOLEAN DEFAULT 0,
        veh_freezer BOOLEAN DEFAULT 0
    );

CREATE TABLE IF NOT EXISTS simlab.service_time_segment_set
	(
		sss_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        sss_name VARCHAR(200)
    );
    
CREATE TABLE IF NOT EXISTS simlab.service_time_segment
	(
		sse_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        sse_set INT NOT NULL,
        sse_pd INT NOT NULL,
        FOREIGN KEY (sse_set)
			REFERENCES simlab.service_time_segment_set(sss_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (sse_pd)
			REFERENCES simlab.probability_distribution(pd_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE
    );
    
CREATE TABLE IF NOT EXISTS simlab.service_time_segment_weighting
	(
		sws_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        sws_name VARCHAR(200),
        sws_segment_set INT NOT NULL,
        FOREIGN KEY(sws_segment_set)
			REFERENCES simlab.service_time_segment_set(sss_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE
    );

CREATE TABLE IF NOT EXISTS simlab.service_time_segment_weight
	(
		ssw_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        ssw_set INT NOT NULL,
        ssw_service_segment INT NOT NULL,
        ssw_weight FLOAT,
        FOREIGN KEY(ssw_service_segment)
			REFERENCES simlab.service_time_segment(sse_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY(ssw_set)
			REFERENCES simlab.service_time_segment_weighting(sws_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE
    );
    


CREATE TABLE IF NOT EXISTS simlab.r_run_v_service_segment_set
	(
        run_sss_run INT NOT NULL,
        run_sss_sss INT NOT NULL,
        run_sss_period TINYINT NOT NULL DEFAULT 0,
        FOREIGN KEY (run_sss_run)
			REFERENCES simlab.run(run_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (run_sss_sss)
			REFERENCES simlab.service_time_segment_set(sss_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(run_sss_run, run_sss_sss, run_sss_period)
	);

CREATE TABLE IF NOT EXISTS simlab.r_run_v_service_segment_weighting
	(
        run_ssw_run INT NOT NULL,
        run_ssw_ssw INT NOT NULL,
        run_ssw_period TINYINT NOT NULL DEFAULT 0,
        FOREIGN KEY (run_ssw_run)
			REFERENCES simlab.run(run_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (run_ssw_ssw)
			REFERENCES simlab.service_time_segment_weighting(sws_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(run_ssw_run, run_ssw_ssw, run_ssw_period)
	);

CREATE TABLE IF NOT EXISTS simlab.r_experiment_v_service_segment_set
	(
        exp_sss_exp INT NOT NULL,
        exp_sss_sss INT NOT NULL,
        exp_sss_period TINYINT NOT NULL DEFAULT 0,
        FOREIGN KEY (exp_sss_exp)
			REFERENCES simlab.experiment(exp_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (exp_sss_sss)
			REFERENCES simlab.service_time_segment_set(sss_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(exp_sss_exp, exp_sss_sss, exp_sss_period)
	);

CREATE TABLE IF NOT EXISTS simlab.r_experiment_v_service_segment_weighting
	(
        exp_ssw_exp INT NOT NULL,
        exp_ssw_ssw INT NOT NULL,
        exp_ssw_period TINYINT NOT NULL DEFAULT 0,
        FOREIGN KEY (exp_ssw_exp)
			REFERENCES simlab.experiment(exp_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (exp_ssw_ssw)
			REFERENCES simlab.service_time_segment_weighting(sws_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(exp_ssw_exp, exp_ssw_ssw, exp_ssw_period)
	);


CREATE TABLE IF NOT EXISTS simlab.arrival_process
	(
		arr_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        arr_name VARCHAR(200),
        arr_lambda_factor FLOAT,
        arr_pd INT NULL
        
    );
    
CREATE TABLE IF NOT EXISTS simlab.r_run_v_arrival_process
	(
		run_arp_run INT NOT NULL,
        run_arp_arp INT NOT NULL,
        run_arp_period TINYINT NOT NULL DEFAULT 0,
        FOREIGN KEY (run_arp_run)
			REFERENCES simlab.run(run_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (run_arp_arp)
			REFERENCES simlab.arrival_process(arr_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(run_arp_run, run_arp_arp, run_arp_period)
    );
    
CREATE TABLE IF NOT EXISTS simlab.r_experiment_v_arrival_process
	(
		exp_arp_exp INT NOT NULL,
        exp_arp_arp INT NOT NULL,
        exp_arp_period TINYINT NOT NULL DEFAULT 0,
        FOREIGN KEY (exp_arp_exp)
			REFERENCES simlab.experiment(exp_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (exp_arp_arp)
			REFERENCES simlab.arrival_process(arr_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(exp_arp_exp, exp_arp_arp, exp_arp_period)
    );

# For data generation process II
CREATE TABLE IF NOT EXISTS simlab.r_experiment_v_arrival_probability_distribution
	(
		exp_apd_exp INT NOT NULL,
        exp_apd_apd INT NOT NULL,
        exp_apd_period TINYINT NOT NULL DEFAULT 0,
        FOREIGN KEY (exp_apd_exp)
			REFERENCES simlab.experiment(exp_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (exp_apd_apd)
			REFERENCES simlab.probability_distribution(pd_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(exp_apd_exp, exp_apd_apd, exp_apd_period)
    );
    
# result of the customer segmentation regarding address (residence)
CREATE TABLE IF NOT EXISTS simlab.residence_area_set
	(
		ras_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        ras_name VARCHAR(200), 
        ras_description VARCHAR(100),
        ras_region INT NOT NULL,
        FOREIGN KEY (ras_region)
			REFERENCES simlab.region(reg_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE
    );
    

CREATE TABLE IF NOT EXISTS simlab.residence_area
	(
		res_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        res_residence_area_set INT NOT NULL,
        res_point1_lat DOUBLE,
        res_point1_long DOUBLE,
        res_point2_lat DOUBLE,
        res_point2_long DOUBLE,
        res_reasonable_subarea_no INT,
        FOREIGN KEY (res_residence_area_set)
			REFERENCES simlab.residence_area_set(ras_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE
    );
    
    
    
  CREATE TABLE IF NOT EXISTS simlab.residence_area_weighting
	(
		rws_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        rws_name VARCHAR(100),
        rws_residence_set INT NOT NULL,
        FOREIGN KEY(rws_residence_set)
			REFERENCES simlab.residence_area_set(ras_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE
    );

CREATE TABLE IF NOT EXISTS simlab.residence_area_weight
	(
		raw_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        raw_set INT NOT NULL,
        raw_residence_area INT NOT NULL,
        raw_weight DOUBLE,
        FOREIGN KEY(raw_residence_area)
			REFERENCES simlab.residence_area(res_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY(raw_set)
			REFERENCES simlab.residence_area_weighting(rws_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE
    );  
    
CREATE TABLE IF NOT EXISTS simlab.demand_model_type
		(
			dmt_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
            dmt_name VARCHAR(100),
            dmt_parametric BOOLEAN DEFAULT 0,
            dmt_independent BOOLEAN DEFAULT 0
        );
        
CREATE TABLE IF NOT EXISTS simlab.consideration_set
	( 
    css_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    css_name VARCHAR(100),
    css_alternative_set INT
    );

CREATE TABLE IF NOT EXISTS simlab.demand_segment_set
	(
		dss_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        dss_name VARCHAR(100),
        dss_panel BOOLEAN DEFAULT 0,
        dss_demand_model_type INT,
        dss_residence_area_set INT, 
        dss_alternative_set INT,
        FOREIGN KEY(dss_demand_model_type)
			REFERENCES simlab.demand_model_type(dmt_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE,
		FOREIGN KEY(dss_residence_area_set)
			REFERENCES simlab.residence_area_set(ras_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE,
		FOREIGN KEY(dss_alternative_set)
			REFERENCES simlab.alternative_set(as_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE
    );


CREATE TABLE IF NOT EXISTS simlab.demand_segment
	(
		dem_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        dem_set INT NOT NULL,
        dem_panel BOOLEAN DEFAULT 0,
        dem_basket_volume_ratio INT,
        dem_basket_no_ratio INT,
        dem_basket_value_pd INT, 
        dem_return_probability_pd INT,
        dem_residence_area_weighting INT,
         dem_social_impact_factor DOUBLE,
         dem_basic_utility DOUBLE,
         dem_consideration_set INT NULL,
        FOREIGN KEY(dem_set)
			REFERENCES simlab.demand_segment_set(dss_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE,
		FOREIGN KEY(dem_basket_value_pd)
			REFERENCES simlab.probability_distribution(pd_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE,
		FOREIGN KEY(dem_return_probability_pd)
			REFERENCES simlab.probability_distribution(pd_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE,
		FOREIGN KEY(dem_residence_area_weighting)
			REFERENCES simlab.residence_area_weighting(rws_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE,
		FOREIGN KEY(dem_consideration_set)
			REFERENCES simlab.consideration_set(css_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE
    );
    


CREATE TABLE IF NOT EXISTS simlab.r_run_v_demand_segment_set
	(
		run_dss_run INT NOT NULL,
        run_dss_dss INT NOT NULL,
        run_dss_period TINYINT NOT NULL DEFAULT 0,
        FOREIGN KEY (run_dss_run)
			REFERENCES simlab.run(run_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (run_dss_dss)
			REFERENCES simlab.demand_segment_set(dss_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(run_dss_run, run_dss_dss, run_dss_period)
    );
    
CREATE TABLE IF NOT EXISTS simlab.r_experiment_v_demand_segment_set
	(
		exp_dss_exp INT NOT NULL,
        exp_dss_dss INT NOT NULL,
        exp_dss_period TINYINT NOT NULL DEFAULT 0,
        FOREIGN KEY (exp_dss_exp)
			REFERENCES simlab.experiment(exp_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (exp_dss_dss)
			REFERENCES simlab.demand_segment_set(dss_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(exp_dss_exp, exp_dss_dss, exp_dss_period)
    );
    
CREATE TABLE IF NOT EXISTS simlab.demand_segment_weighting
	(
		dsw_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        dsw_name VARCHAR(200),
        dsw_segment_set INT NOT NULL,
        FOREIGN KEY(dsw_segment_set)
			REFERENCES simlab.demand_segment_set(dss_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE
    );

CREATE TABLE IF NOT EXISTS simlab.r_run_v_demand_segment_weighting
	(
		run_dsw_run INT NOT NULL,
        run_dsw_dsw INT NOT NULL,
        run_dsw_period TINYINT NOT NULL DEFAULT 0,
        run_dsw_t INT DEFAULT -1, # For which time in the booking/sales horizon for a given delivery period is this weighting
        FOREIGN KEY (run_dsw_run)
			REFERENCES simlab.run(run_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (run_dsw_dsw)
			REFERENCES simlab.demand_segment_weighting(dsw_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(run_dsw_run, run_dsw_dsw, run_dsw_period, run_dsw_t)
    );

CREATE TABLE IF NOT EXISTS simlab.demand_segment_weight
	(
		dw_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        dw_set INT NOT NULL,
        dw_demand_segment INT NOT NULL,
        dw_weight FLOAT,
        FOREIGN KEY(dw_demand_segment)
			REFERENCES simlab.demand_segment(dem_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY(dw_set)
			REFERENCES simlab.demand_segment_weighting(dsw_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE
    );   

CREATE TABLE IF NOT EXISTS simlab.r_experiment_v_demand_segment_weighting
	(
		exp_dsw_exp INT NOT NULL,
        exp_dsw_dsw INT NOT NULL,
        exp_dsw_period TINYINT NOT NULL DEFAULT 0,
        exp_dsw_t INT DEFAULT -1, # For which time in the booking/sales horizon for a given delivery period is this weighting
        FOREIGN KEY (exp_dsw_exp)
			REFERENCES simlab.experiment(exp_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (exp_dsw_dsw)
			REFERENCES simlab.demand_segment_weighting(dsw_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(exp_dsw_exp, exp_dsw_dsw, exp_dsw_period, exp_dsw_t)
    );

    

# cs_extension indicates if generated online for panel data during acceptance step (only applicable to panel data) 
# and as such, if identical customers to other customer set (historical data set, previous days, ...)
CREATE TABLE IF NOT EXISTS simlab.customer_set
	(
		cs_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
		cs_name VARCHAR(200),
        cs_panel BOOLEAN DEFAULT 0,
        cs_extension BOOLEAN DEFAULT 0,
        cs_original_demand_segment_set INT,
        FOREIGN KEY (cs_original_demand_segment_set)
			REFERENCES simlab.demand_segment_set(dss_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE
    ) ;



CREATE TABLE IF NOT EXISTS simlab.r_run_v_customer_set
	(
		run_cs_run INT NOT NULL,
        run_cs_cs INT NOT NULL,
        run_cs_period TINYINT NOT NULL DEFAULT 0 ,
        FOREIGN KEY (run_cs_run)
			REFERENCES simlab.run(run_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (run_cs_cs)
			REFERENCES simlab.customer_set(cs_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(run_cs_run, run_cs_cs, run_cs_period)
	);

CREATE TABLE IF NOT EXISTS simlab.r_experiment_v_customer_set
	(
		exp_cs_exp INT NOT NULL,
        exp_cs_cs INT NOT NULL,
        exp_cs_period TINYINT NOT NULL DEFAULT 0 ,
        FOREIGN KEY (exp_cs_exp)
			REFERENCES simlab.experiment(exp_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (exp_cs_cs)
			REFERENCES simlab.customer_set(cs_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(exp_cs_exp, exp_cs_cs, exp_cs_period)
	);
    
CREATE TABLE IF NOT EXISTS simlab.order_request_set 
	(
		ors_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
		ors_name VARCHAR(200),
        ors_booking_horizon INT,
        ors_customer_set INT,
        ors_sampledPreferences BOOLEAN DEFAULT FALSE,
        FOREIGN KEY (ors_customer_set)
			REFERENCES simlab.customer_set(cs_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE
    ) ;
    

    
# Cooled, frozen food?
CREATE TABLE IF NOT EXISTS simlab.order_content_type
	(
		oct_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        oct_name VARCHAR(100)
    );
 


    

   
# Consideration set (with priorities)
# Because predecessor, not only list but also trees are possible
CREATE TABLE IF NOT EXISTS simlab.consideration_set_alternative
	(
		csa_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        csa_set INT NULL,
		csa_demand_segment INT NULL,
        csa_alternative INT NOT NULL, 
        csa_predecessor INT, # needed for non-parametric tree/list
        csa_weight DOUBLE, # probability
        csa_coefficient DOUBLE, # utility coefficient
        FOREIGN KEY(csa_alternative)
			REFERENCES simlab.alternative(alt_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY(csa_demand_segment)
			REFERENCES simlab.demand_segment(dem_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY(csa_predecessor)
			REFERENCES simlab.consideration_set_alternative(csa_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY(csa_set)
			REFERENCES simlab.consideration_set(css_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE
    );
    

    
    
# Assigns weights to segments per run period
#CREATE TABLE IF NOT EXISTS simlab.r_run_v_demand_segment
#	(
#		run_ds_run INT NOT NULL,
#        run_ds_ds INT NOT NULL,
 #       run_ds_period INT DEFAULT 0,
 #       run_ds_weight FLOAT(6,5),
 #       FOREIGN KEY(run_ds_run)
#			REFERENCES simlab.run(run_id)
#			ON DELETE CASCADE
	#		ON UPDATE CASCADE,
#		FOREIGN KEY(run_ds_ds)
#			REFERENCES simlab.demand_segment(dem_id)
##			ON DELETE CASCADE
#			ON UPDATE CASCADE,
#		PRIMARY KEY(run_ds_run, run_ds_ds,run_ds_period)
 #   );

# E.g. customer or time window attributes for demand model
CREATE TABLE IF NOT EXISTS simlab.variable_type
	(
		var_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        var_name VARCHAR(100)
    );

#CREATE TABLE IF NOT EXISTS simlab.r_dss_v_variable_focus
#	(
#		dss_variable_dss INT NOT NULL,
#        dss_variable_var INT NOT NULL,
#        FOREIGN KEY(dss_variable_dss)
#			REFERENCES simlab.demand_segment_set(dss_id)
#			ON DELETE CASCADE
#			ON UPDATE CASCADE,
#		FOREIGN KEY(dss_variable_var)
#			REFERENCES simlab.variable(var_id)
#			ON DELETE CASCADE
#			ON UPDATE CASCADE,
#		PRIMARY KEY(dss_variable_dss, dss_variable_var)
  #  );


#Coefficient of a parametric demand model variable
CREATE TABLE IF NOT EXISTS simlab.r_demand_segment_v_variable_type
	(
		dem_variable_dem INT NOT NULL,
        dem_variable_var INT NOT NULL,
		dem_variable_coefficient FLOAT NOT NULL,
        FOREIGN KEY(dem_variable_var)
			REFERENCES simlab.variable_type(var_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY(dem_variable_dem)
			REFERENCES simlab.demand_segment(dem_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
        PRIMARY KEY(dem_variable_dem, dem_variable_var)
    );


CREATE TABLE IF NOT EXISTS simlab.customer
	(
		cus_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        cus_set INT NOT NULL,
		cus_lat FLOAT(10,7) NOT NULL,
        cus_long FLOAT(10,7) NOT NULL,
        cus_floor INT, 
        cus_closest_node BIGINT,
        cus_closest_node_distance FLOAT,
        cus_service_time_segment INT,
		cus_segment_original INT,
        cus_return_probability FLOAT(6,5),
        cus_temp_t INT, #t of booking period in which it was originally created
        FOREIGN KEY (cus_set)
			REFERENCES simlab.customer_set(cs_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (cus_segment_original)
			REFERENCES simlab.demand_segment(dem_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE,
		FOREIGN KEY (cus_service_time_segment)
			REFERENCES simlab.service_time_segment(sse_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,  
		FOREIGN KEY (cus_closest_node)
			REFERENCES simlab.node(nod_id)
			ON DELETE SET NULL
			ON UPDATE CASCADE
    ) ;


CREATE TABLE IF NOT EXISTS simlab.order_request
	(
		orr_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        orr_set INT NOT NULL,
        orr_customer INT NOT NULL,
        orr_t INT, # time of booking horizon it arrives
        orr_content_type INT, 
        orr_basket_value FLOAT,
        orr_basket_volume FLOAT,
        orr_basket_packageno TINYINT,
        FOREIGN KEY (orr_set)
			REFERENCES simlab.order_request_set(ors_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (orr_customer)
			REFERENCES simlab.customer(cus_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (orr_content_type)
			REFERENCES simlab.order_content_type(oct_id)
			ON DELETE SET NULL
			ON UPDATE CASCADE
    ) ;
    
CREATE INDEX or_set_index ON  simlab.order_request (orr_set);

CREATE TABLE IF NOT EXISTS simlab.value_bucket_set 
	(
		vbs_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
		vbs_name VARCHAR(100)
    ) ;

CREATE TABLE IF NOT EXISTS simlab.value_bucket
	(
		vb_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
        vb_set INT NOT NULL,
        vb_bound_upper FLOAT,
		vb_lower_bound FLOAT,
        FOREIGN KEY (vb_set)
			REFERENCES simlab.value_bucket_set(vbs_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE
    ) ;

CREATE TABLE IF NOT EXISTS simlab.r_run_v_value_bucket_set
	(
		run_vbs_run INT NOT NULL,
        run_vbs_vbs INT NOT NULL,
        run_vbs_period TINYINT NOT NULL DEFAULT 0,
        FOREIGN KEY (run_vbs_run)
			REFERENCES simlab.run(run_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (run_vbs_vbs)
			REFERENCES simlab.value_bucket_set(vbs_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(run_vbs_run, run_vbs_vbs, run_vbs_period)
	);
    
CREATE TABLE IF NOT EXISTS simlab.r_experiment_v_value_bucket_set
	(
		exp_vbs_exp INT NOT NULL,
        exp_vbs_vbs INT NOT NULL,
        exp_vbs_period TINYINT NOT NULL DEFAULT 0,
        FOREIGN KEY (exp_vbs_exp)
			REFERENCES simlab.experiment(exp_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (exp_vbs_vbs)
			REFERENCES simlab.value_bucket_set(vbs_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(exp_vbs_exp, exp_vbs_vbs, exp_vbs_period)
	);
    
CREATE TABLE IF NOT EXISTS simlab.control_set 
	(
		cos_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
		cos_name VARCHAR(100),
         cos_delivery_area_set INT,
        cos_alternative_set INT,
        cos_value_bucket_set INT,
        FOREIGN KEY (cos_alternative_set)
			REFERENCES simlab.alternative_set(as_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE,
		FOREIGN KEY (cos_delivery_area_set)
			REFERENCES simlab.delivery_area_set(das_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE,
		FOREIGN KEY (cos_value_bucket_set)
			REFERENCES simlab.value_bucket_set(vbs_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE
    ) ;



# TODO: Controls and capacities really per time window or per alternative?
CREATE TABLE IF NOT EXISTS simlab.control
	(
		con_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        con_set INT NOT NULL,
        con_alternative INT,
		con_delivery_area INT,
        con_value_bucket INT,
        con_no INT,
        FOREIGN KEY (con_set)
			REFERENCES simlab.control_set(cos_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (con_alternative)
			REFERENCES simlab.alternative(alt_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE,
		FOREIGN KEY (con_delivery_area)
			REFERENCES simlab.delivery_area(da_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE,
		FOREIGN KEY (con_value_bucket)
			REFERENCES simlab.value_bucket(vb_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE
    ) ;
    
CREATE TABLE IF NOT EXISTS simlab.r_run_v_control_set
	(
		run_cos_run INT NOT NULL,
        run_cos_cos INT NOT NULL,
        run_cos_period TINYINT NOT NULL DEFAULT 0,
        FOREIGN KEY (run_cos_run)
			REFERENCES simlab.run(run_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (run_cos_cos)
			REFERENCES simlab.control_set(cos_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(run_cos_run, run_cos_cos, run_cos_period)
	);
    
    CREATE TABLE IF NOT EXISTS simlab.r_experiment_v_control_set
	(
		exp_cos_exp INT NOT NULL,
        exp_cos_cos INT NOT NULL,
        exp_cos_period TINYINT NOT NULL DEFAULT 0,
        FOREIGN KEY (exp_cos_exp)
			REFERENCES simlab.experiment(exp_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (exp_cos_cos)
			REFERENCES simlab.control_set(cos_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(exp_cos_exp, exp_cos_cos, exp_cos_period)
	);
    
CREATE TABLE IF NOT EXISTS simlab.order_set 
	(
		os_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
		os_name VARCHAR(200),
        os_order_request_set INT NOT NULL,
        os_control_set INT,
        FOREIGN KEY (os_order_request_set)
			REFERENCES simlab.order_request_set(ors_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (os_control_set)
			REFERENCES simlab.control_set(cos_id)
			ON DELETE SET NULL
			ON UPDATE CASCADE
		
    ) ;
    

#TODO: Consider rejection reason as enum
CREATE TABLE IF NOT EXISTS simlab.order
	(
		ord_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        ord_set INT NOT NULL,
        ord_order_request INT NOT NULL,
        ord_tw_final INT,
        ord_alternative_selected INT,
        ord_accepted BOOLEAN DEFAULT 0,
        ord_reason_rejection VARCHAR(50),
        ord_alternative_fee FLOAT,
        ord_assigned_delivery_area INT, 
        ord_assigned_value FLOAT,
        FOREIGN KEY (ord_set)
			REFERENCES simlab.order_set(os_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (ord_order_request)
			REFERENCES simlab.order_request(orr_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (ord_tw_final)
			REFERENCES simlab.time_window(tw_id)
			ON DELETE SET NULL
			ON UPDATE CASCADE,
		FOREIGN KEY (ord_alternative_selected)
			REFERENCES simlab.alternative(alt_id)
			ON DELETE SET NULL
			ON UPDATE CASCADE,
		FOREIGN KEY (ord_assigned_delivery_area)
			REFERENCES simlab.delivery_area(da_id)
            ON DELETE SET NULL
			ON UPDATE CASCADE
    ) ;

    
# Especially for revenue kpis per order
CREATE TABLE IF NOT EXISTS simlab.r_order_v_kpi
	(
		order_kpi_ord INT NOT NULL, 
        order_kpi_kpi INT NOT NULL,
        order_kpi_value FLOAT,
        FOREIGN KEY (order_kpi_ord)
			REFERENCES simlab.order(ord_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (order_kpi_kpi)
			REFERENCES simlab.kpi(kpi_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY (order_kpi_ord, order_kpi_kpi)
        
    );
    
CREATE TABLE IF NOT EXISTS simlab.r_run_v_order_set
	(
		run_os_run INT NOT NULL,
        run_os_os INT NOT NULL,
        run_os_period TINYINT NOT NULL DEFAULT 0,
        run_os_historical BOOLEAN DEFAULT 0,
        FOREIGN KEY (run_os_run)
			REFERENCES simlab.run(run_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (run_os_os)
			REFERENCES simlab.order_set(os_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(run_os_run, run_os_os, run_os_period, run_os_historical)
	);
    
CREATE TABLE IF NOT EXISTS simlab.r_run_v_order_request_set
	(
		run_ors_run INT NOT NULL,
        run_ors_ors INT NOT NULL,
        run_ors_period TINYINT NOT NULL DEFAULT 0,
        FOREIGN KEY (run_ors_run)
			REFERENCES simlab.run(run_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (run_ors_ors)
			REFERENCES simlab.order_request_set(ors_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(run_ors_run, run_ors_ors, run_ors_period)
	);

CREATE TABLE IF NOT EXISTS simlab.r_experiment_v_order_set
	(
		exp_os_exp INT NOT NULL,
        exp_os_os INT NOT NULL,
        exp_os_period TINYINT NOT NULL DEFAULT 0,
        exp_os_historical BOOLEAN DEFAULT 0,
        FOREIGN KEY (exp_os_exp)
			REFERENCES simlab.experiment(exp_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (exp_os_os)
			REFERENCES simlab.order_set(os_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(exp_os_exp, exp_os_os, exp_os_period, exp_os_historical)
	);
    
CREATE TABLE IF NOT EXISTS simlab.r_experiment_v_order_request_set
	(
		exp_ors_exp INT NOT NULL,
        exp_ors_ors INT NOT NULL,
        exp_ors_period TINYINT NOT NULL DEFAULT 0,
        FOREIGN KEY (exp_ors_exp)
			REFERENCES simlab.experiment(exp_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (exp_ors_ors)
			REFERENCES simlab.order_request_set(ors_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(exp_ors_exp, exp_ors_ors, exp_ors_period)
	);
    
# TODO: Consider adding incentive type, but already added to run  -> redundant
CREATE TABLE IF NOT EXISTS simlab.r_order_v_alternative_offered
	(
        order_alternative_off_ord INT NOT NULL,
        order_alternative_off_alt INT NOT NULL,
        order_alternative_off_incentive FLOAT,
        FOREIGN KEY (order_alternative_off_ord)
			REFERENCES simlab.order(ord_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (order_alternative_off_alt)
			REFERENCES simlab.alternative(alt_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(order_alternative_off_ord, order_alternative_off_alt)
    );

# TODO: Really save for everybody?    
CREATE TABLE IF NOT EXISTS simlab.r_order_v_alternative_available
	(
        order_alternative_ava_ord INT NOT NULL,
        order_alternative_ava_alt INT NOT NULL,
        FOREIGN KEY (order_alternative_ava_ord)
			REFERENCES simlab.order(ord_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (order_alternative_ava_alt)
			REFERENCES simlab.alternative(alt_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(order_alternative_ava_ord, order_alternative_ava_alt)
    );


 
CREATE TABLE IF NOT EXISTS simlab.value_bucket_forecast_set 
	(
		vfs_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
		vfs_name VARCHAR(200),
		vfs_delivery_area_set INT,
        vfs_alternative_set INT,
        vfs_value_bucket_set INT,
        FOREIGN KEY (vfs_alternative_set)
			REFERENCES simlab.alternative_set(as_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE,
		FOREIGN KEY (vfs_delivery_area_set)
			REFERENCES simlab.delivery_area_set(das_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE,
		FOREIGN KEY (vfs_value_bucket_set)
			REFERENCES simlab.value_bucket_set(vbs_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE
    ) ;

CREATE TABLE IF NOT EXISTS simlab.value_bucket_forecast
	(
		vf_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
        vf_set INT NOT NULL,
       # df_demand_segment INT, FOR LATER
        vf_alternative INT,
		vf_delivery_area INT,
        vf_value_bucket INT,
        vf_no INT,
        FOREIGN KEY (vf_set)
			REFERENCES simlab.value_bucket_forecast_set(vfs_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (vf_alternative)
			REFERENCES simlab.alternative(alt_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE,
		FOREIGN KEY (vf_delivery_area)
			REFERENCES simlab.delivery_area(da_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE,
		FOREIGN KEY (vf_value_bucket)
			REFERENCES simlab.value_bucket(vb_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE
    ) ;
    
CREATE TABLE IF NOT EXISTS simlab.r_run_v_value_bucket_forecast_set
	(
		run_vfs_run INT NOT NULL,
        run_vfs_vfs INT NOT NULL,
        run_vfs_period TINYINT NOT NULL DEFAULT 0,
        FOREIGN KEY (run_vfs_run)
			REFERENCES simlab.run(run_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (run_vfs_vfs)
			REFERENCES simlab.value_bucket_forecast_set(vfs_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(run_vfs_run, run_vfs_vfs, run_vfs_period)
	);
    
    CREATE TABLE IF NOT EXISTS simlab.r_experiment_v_value_bucket_forecast_set
	(
		exp_vfs_exp INT NOT NULL,
        exp_vfs_vfs INT NOT NULL,
        exp_vfs_period TINYINT NOT NULL DEFAULT 0,
        FOREIGN KEY (exp_vfs_exp)
			REFERENCES simlab.experiment(exp_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (exp_vfs_vfs)
			REFERENCES simlab.value_bucket_forecast_set(vfs_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(exp_vfs_exp, exp_vfs_vfs, exp_vfs_period)
	);

CREATE TABLE IF NOT EXISTS simlab.demand_segment_forecast_set 
	(
		dfs_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
		dfs_name VARCHAR(100),
		dfs_delivery_area_set INT,
        dfs_demand_segment_set INT,
		FOREIGN KEY (dfs_delivery_area_set)
			REFERENCES simlab.delivery_area_set(das_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE,
		FOREIGN KEY (dfs_demand_segment_set)
			REFERENCES simlab.demand_segment_set(dss_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE
    ) ;


#ALTER TABLE simlab.demand_segment_forecast_set MODIFY dfs_name VARCHAR(100);

CREATE TABLE IF NOT EXISTS simlab.demand_segment_forecast
	(
		df_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
        df_set INT NOT NULL,
       # df_demand_segment INT, FOR LATER
		df_delivery_area INT,
        df_demand_segment INT,
        df_no INT,
        FOREIGN KEY (df_set)
			REFERENCES simlab.demand_segment_forecast_set(dfs_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (df_delivery_area)
			REFERENCES simlab.delivery_area(da_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE,
		FOREIGN KEY (df_demand_segment)
			REFERENCES simlab.demand_segment(dem_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE
    ) ;
    
CREATE TABLE IF NOT EXISTS simlab.r_run_v_demand_segment_forecast_set
	(
		run_dfs_run INT NOT NULL,
        run_dfs_dfs INT NOT NULL,
        run_dfs_period TINYINT NOT NULL DEFAULT 0,
        FOREIGN KEY (run_dfs_run)
			REFERENCES simlab.run(run_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (run_dfs_dfs)
			REFERENCES simlab.demand_segment_forecast_set(dfs_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(run_dfs_run, run_dfs_dfs, run_dfs_period)
	);
    
    CREATE TABLE IF NOT EXISTS simlab.r_experiment_v_demand_segment_forecast_set
	(
		exp_dfs_exp INT NOT NULL,
        exp_dfs_dfs INT NOT NULL,
        exp_dfs_period TINYINT NOT NULL DEFAULT 0,
        FOREIGN KEY (exp_dfs_exp)
			REFERENCES simlab.experiment(exp_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (exp_dfs_dfs)
			REFERENCES simlab.demand_segment_forecast_set(dfs_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(exp_dfs_exp, exp_dfs_dfs, exp_dfs_period)
	);

CREATE TABLE IF NOT EXISTS simlab.vehicle_area_assignment_set
(
	vrs_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    vrs_name VARCHAR(200),
    vrs_delivery_area_set INT NOT NULL,
    FOREIGN KEY (vrs_delivery_area_set)
		REFERENCES simlab.delivery_area_set(das_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
    
);

CREATE TABLE IF NOT EXISTS simlab.vehicle_area_assignment
(
	vaa_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    vaa_set INT NOT NULL,
    vaa_vehicle_no INT NOT NULL, #number of the vehicle to combine same vehicles
    vaa_area INT NOT NULL,
    vaa_vehicle_type INT,
    vaa_starting_location_lat FLOAT NOT NULL,
    vaa_starting_location_lon FLOAT NOT NULL,
	vaa_ending_location_lat FLOAT NOT NULL,
    vaa_ending_location_lon FLOAT NOT NULL,
    vaa_start_time FLOAT NOT NULL,
    vaa_end_time FLOAT NOT NULL,   
	FOREIGN KEY (vaa_set)
		REFERENCES simlab.vehicle_area_assignment_set(vrs_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
	FOREIGN KEY (vaa_area)
		REFERENCES simlab.delivery_area(da_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
	FOREIGN KEY (vaa_vehicle_type)
		REFERENCES simlab.vehicle_type(veh_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS simlab.r_experiment_v_vehicle_area_assignment_set
(
	exp_vas_exp INT NOT NULL,
	exp_vas_vas INT NOT NULL,
    exp_vas_period TINYINT NOT NULL DEFAULT 0,
	FOREIGN KEY (exp_vas_exp)
			REFERENCES simlab.experiment(exp_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
	FOREIGN KEY (exp_vas_vas)
			REFERENCES simlab.vehicle_area_assignment_set(vrs_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
PRIMARY KEY(exp_vas_exp, exp_vas_vas, exp_vas_period)

);

CREATE TABLE IF NOT EXISTS simlab.r_run_v_vehicle_area_assignment_set
(
	run_vas_run INT NOT NULL,
	run_vas_vas INT NOT NULL,
    run_vas_period TINYINT NOT NULL DEFAULT 0,
	FOREIGN KEY (run_vas_run)
			REFERENCES simlab.run(run_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
	FOREIGN KEY (run_vas_vas)
			REFERENCES simlab.vehicle_area_assignment_set(vrs_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
PRIMARY KEY(run_vas_run, run_vas_vas, run_vas_period)

);



# rou_possibly_final means if routing can be used as final routing (plausible, actual orders associated)
# ou_final means if routing is used as final routing in this setting
CREATE TABLE IF NOT EXISTS simlab.routing
	(
		rou_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        rou_name VARCHAR(200),
        rou_possibly_final BOOLEAN DEFAULT 0,
        rou_possibly_target BOOLEAN DEFAULT 0,
        rou_time_window_set INT, # only for initial routing
        rou_order_set INT, # only for final routing
        rou_information VARCHAR(100),
        rou_depot INT,
        rou_vehicle_area_assignment_set INT,
        rou_additional_costs FLOAT,
        rou_area_weighting longtext,
        rou_area_ds_weighting longtext,
        FOREIGN KEY (rou_time_window_set)
			REFERENCES simlab.time_window_set(tws_id)
			ON DELETE SET NULL
			ON UPDATE CASCADE,
		FOREIGN KEY (rou_order_set)
			REFERENCES simlab.order_set(os_id)
			ON DELETE SET NULL
			ON UPDATE CASCADE,
		FOREIGN KEY (rou_depot)
			REFERENCES simlab.depot(dep_id)
			ON DELETE SET NULL
			ON UPDATE CASCADE,
		FOREIGN KEY (rou_vehicle_area_assignment_set)
			REFERENCES simlab.vehicle_area_assignment_set(vrs_id)
			ON DELETE SET NULL
			ON UPDATE CASCADE
     
	);
    
    
CREATE TABLE IF NOT EXISTS simlab.r_run_v_routing
	(
        run_rou_run INT NOT NULL,
        run_rou_rou INT NOT NULL,
        run_rou_period TINYINT NOT NULL DEFAULT 0,
        run_rou_t INT DEFAULT -1,
        FOREIGN KEY (run_rou_run)
			REFERENCES simlab.run(run_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (run_rou_rou)
			REFERENCES simlab.routing(rou_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(run_rou_run, run_rou_rou, run_rou_period, run_rou_t)
	);

CREATE TABLE IF NOT EXISTS simlab.r_experiment_v_routing
	(
        exp_rou_exp INT NOT NULL,
        exp_rou_rou INT NOT NULL,
        exp_rou_period TINYINT NOT NULL DEFAULT 0,
        exp_rou_t INT DEFAULT -1,
        FOREIGN KEY (exp_rou_exp)
			REFERENCES simlab.experiment(exp_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (exp_rou_rou)
			REFERENCES simlab.routing(rou_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(exp_rou_exp, exp_rou_rou, exp_rou_period, exp_rou_t)
	);
    
    CREATE TABLE IF NOT EXISTS simlab.route
	(
        route_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        route_routing INT NOT NULL,
        route_vehicle INT,
        route_vehicle_area_assignment INT,
        FOREIGN KEY (route_routing)
			REFERENCES simlab.routing(rou_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (route_vehicle)
			REFERENCES simlab.vehicle_type(veh_id)
			ON DELETE SET NULL
			ON UPDATE CASCADE,
		 FOREIGN KEY (route_vehicle_area_assignment)
			REFERENCES simlab.vehicle_area_assignment(vaa_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE
	);
    
CREATE TABLE IF NOT EXISTS simlab.route_element
	(
		re_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        re_route INT NOT NULL,
        re_position INT, # position in the route
        re_tw INT NOT NULL,
        re_area INT, # only for initial routing routing
        re_order INT, # only for final routing element
        re_travel_time DOUBLE,
        re_waiting_time DOUBLE, # TW Start-Arrival time
        re_service_begin DOUBLE,
        re_service_time DOUBLE, # Expected service time
        re_slack DOUBLE,
        FOREIGN KEY (re_tw)
			REFERENCES simlab.time_window(tw_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (re_area)
			REFERENCES simlab.delivery_area(da_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (re_order)
			REFERENCES simlab.order(ord_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
            FOREIGN KEY (re_route)
			REFERENCES simlab.route(route_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE   
    );
    

    
CREATE TABLE IF NOT EXISTS simlab.capacity_set 
	(
		cas_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
		cas_name VARCHAR(100),
        cas_delivery_area_set INT,
        cas_tw_set INT,
        cas_routing INT,
        cas_weight FLOAT, #for algorithm, eg. beta for parallel flight algo
        FOREIGN KEY (cas_tw_set)
			REFERENCES simlab.time_window_set(tws_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE,
		FOREIGN KEY (cas_delivery_area_set)
			REFERENCES simlab.delivery_area_set(das_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE,
		FOREIGN KEY (cas_routing)
			REFERENCES simlab.routing(rou_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE
    ) ;
    

CREATE TABLE IF NOT EXISTS simlab.capacity
	(
		cap_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        cap_set INT NOT NULL,
        cap_tw INT,
		cap_delivery_area INT,
        cap_no INT,
        FOREIGN KEY (cap_set)
			REFERENCES simlab.capacity_set(cas_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (cap_tw)
			REFERENCES simlab.time_window(tw_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE,
		FOREIGN KEY (cap_delivery_area)
			REFERENCES simlab.delivery_area(da_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE
    ) ;

CREATE TABLE IF NOT EXISTS simlab.expected_delivery_time_consumption_set 
	(
		eds_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
		eds_name VARCHAR(100),
        eds_delivery_area_set INT,
        eds_tw_set INT,
        eds_routing INT,
        FOREIGN KEY (eds_tw_set)
			REFERENCES simlab.time_window_set(tws_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE,
		FOREIGN KEY (eds_delivery_area_set)
			REFERENCES simlab.delivery_area_set(das_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE,
		FOREIGN KEY (eds_routing)
			REFERENCES simlab.routing(rou_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE
    ) ;
    
CREATE TABLE IF NOT EXISTS simlab.expected_delivery_time_consumption
	(
		edt_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        edt_set INT NOT NULL,
        edt_tw INT,
		edt_delivery_area INT,
        edt_time FLOAT,
        FOREIGN KEY (edt_set)
			REFERENCES simlab.expected_delivery_time_consumption_set(eds_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (edt_tw)
			REFERENCES simlab.time_window(tw_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE,
		FOREIGN KEY (edt_delivery_area)
			REFERENCES simlab.delivery_area(da_id)
			ON DELETE RESTRICT
			ON UPDATE CASCADE
    ) ;
    
CREATE TABLE IF NOT EXISTS simlab.r_experiment_v_capacity_set
	(
		exp_cas_exp INT NOT NULL,
        exp_cas_cas INT NOT NULL,
        exp_cas_period TINYINT NOT NULL DEFAULT 0,
        FOREIGN KEY (exp_cas_exp)
			REFERENCES simlab.experiment(exp_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (exp_cas_cas)
			REFERENCES simlab.capacity_set(cas_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(exp_cas_exp, exp_cas_cas, exp_cas_period)
	);
    
CREATE TABLE IF NOT EXISTS simlab.r_run_v_capacity_set
	(
		run_cas_run INT NOT NULL,
        run_cas_cas INT NOT NULL,
        run_cas_period TINYINT NOT NULL DEFAULT 0,
        FOREIGN KEY (run_cas_run)
			REFERENCES simlab.run(run_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (run_cas_cas)
			REFERENCES simlab.capacity_set(cas_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(run_cas_run, run_cas_cas, run_cas_period)
	);
    

    
CREATE TABLE IF NOT EXISTS simlab.delivery_set
	(
		des_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        des_name VARCHAR(200)
	);

# Actual numbers (vs. routing element with plan numbers)
CREATE TABLE IF NOT EXISTS simlab.delivery
	(
		del_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        del_set INT NOT NULL,
        del_re_id INT NOT NULL, # routing element
        del_travel_time FLOAT,
        del_arrival_time TIME, # Point in time the vehicle arrives at the location
        del_waiting_time FLOAT, # TW Start-Arrival time
        del_service_begin TIME,
        del_service_time FLOAT, # Result of draw from service time distribution
        del_buffer_before FLOAT,
		FOREIGN KEY (del_re_id)
			REFERENCES simlab.route_element(re_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (del_set)
			REFERENCES simlab.delivery_set(des_id)
            ON DELETE CASCADE
			ON UPDATE CASCADE
            
        
    );

# Especially for tour planning kpis per order
#CREATE TABLE IF NOT EXISTS simlab.r_delivery_v_kpi
#	(
#		delivery_kpi_run INT NOT NULL, 
#        delivery_kpi_re INT NOT NULL, 
#        delivery_kpi_kpi INT NOT NULL,
 #       delivery_kpi_value FLOAT,
 #       FOREIGN KEY (delivery_kpi_run, delivery_kpi_re)
#			REFERENCES simlab.delivery(del_run_id, del_re_id)
#			ON DELETE CASCADE
#			ON UPDATE CASCADE,
#		FOREIGN KEY (delivery_kpi_kpi)
#			REFERENCES simlab.kpi(kpi_id)
#			ON DELETE CASCADE
#			ON UPDATE CASCADE,
#		PRIMARY KEY (delivery_kpi_run, delivery_kpi_re, delivery_kpi_kpi)
        
 #   );

CREATE TABLE IF NOT EXISTS simlab.r_experiment_v_vehicle_type
(
	exp_vehicle_exp INT NOT NULL,
    exp_vehicle_veh INT NOT NULL,
    exp_vehicle_no INT NOT NULL,
    FOREIGN KEY (exp_vehicle_exp)
			REFERENCES simlab.experiment(exp_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
	FOREIGN KEY (exp_vehicle_veh)
			REFERENCES simlab.vehicle_type(veh_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
	PRIMARY KEY(exp_vehicle_exp, exp_vehicle_veh)
	
);


CREATE TABLE IF NOT EXISTS simlab.travel_time_set
(
	tts_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tts_name VARCHAR(200),
    tts_region INT NOT NULL,
    FOREIGN KEY (tts_region)
			REFERENCES simlab.region(reg_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE
);


CREATE TABLE IF NOT EXISTS simlab.r_experiment_v_travel_time_set
(
	exp_tts_exp INT NOT NULL,
     exp_tts_tts INT NOT NULL,
    exp_tts_period INT NOT NULL,
    FOREIGN KEY (exp_tts_exp)
			REFERENCES simlab.experiment(exp_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
	PRIMARY KEY(exp_tts_exp, exp_tts_tts, exp_tts_period)
    
);


CREATE TABLE IF NOT EXISTS simlab.travel_time
(	
	tra_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tra_set INT NOT NULL,
    tra_start FLOAT,
    tra_end FLOAT,
    tra_pd INT NOT NULL,
    FOREIGN KEY (tra_set)
			REFERENCES simlab.travel_time_set(tts_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
     FOREIGN KEY (tra_pd)
			REFERENCES simlab.probability_distribution(pd_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE       
);

CREATE TABLE IF NOT EXISTS simlab.dynamic_programming_tree
(
	dpt_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    dpt_name VARCHAR(100),
    dpt_t INT NOT NULL,
    dpt_capacity_set INT NOT NULL,
    dpt_arrival_process INT NOT NULL,
    dpt_demand_segment_weighting INT NOT NULL,
    dpt_delivery_area_set INT NOT NULL,
    #dpt_tree LONGTEXT NOT NULL,
    FOREIGN KEY (dpt_capacity_set)
			REFERENCES simlab.capacity_set(cas_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
   FOREIGN KEY (dpt_arrival_process)
			REFERENCES simlab.arrival_process(arr_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
FOREIGN KEY (dpt_demand_segment_weighting)
			REFERENCES simlab.demand_segment_weighting(dsw_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
FOREIGN KEY (dpt_delivery_area_set)
			REFERENCES simlab.delivery_area_set(das_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS simlab.r_dynamic_programming_tree_v_delivery_area
(
dpt_da_dpt INT NOT NULL,
dpt_da_da INT NOT NULL,
 dpt_da_tree LONGTEXT NOT NULL,
 FOREIGN KEY (dpt_da_da)
			REFERENCES simlab.delivery_area(da_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
 FOREIGN KEY (dpt_da_dpt)
			REFERENCES simlab.dynamic_programming_tree(dpt_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
PRIMARY KEY(dpt_da_dpt, dpt_da_da)

);

CREATE TABLE IF NOT EXISTS simlab.r_experiment_v_dynamic_programming_tree
(
exp_dpt_exp INT NOT NULL,
exp_dpt_dpt INT NOT NULL,
 exp_dpt_period TINYINT NOT NULL DEFAULT 0,
        FOREIGN KEY (exp_dpt_exp)
			REFERENCES simlab.experiment(exp_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (exp_dpt_dpt)
			REFERENCES simlab.dynamic_programming_tree(dpt_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(exp_dpt_exp, exp_dpt_dpt, exp_dpt_period)
);

CREATE TABLE IF NOT EXISTS simlab.r_run_v_dynamic_programming_tree
(
run_dpt_run INT NOT NULL,
run_dpt_dpt INT NOT NULL,
 run_dpt_period TINYINT NOT NULL DEFAULT 0,
        FOREIGN KEY (run_dpt_run)
			REFERENCES simlab.run(run_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		FOREIGN KEY (run_dpt_dpt)
			REFERENCES simlab.dynamic_programming_tree(dpt_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
		PRIMARY KEY(run_dpt_run, run_dpt_dpt, run_dpt_period)
);

CREATE TABLE IF NOT EXISTS simlab.objective
(
	obj_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	obj_name VARCHAR(100),
	obj_description VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS simlab.r_experiment_v_objective
(
	exp_obj_exp INT NOT NULL,
	exp_obj_obj INT NOT NULL,
	exp_obj_weight FLOAT NOT NULL,
	FOREIGN KEY (exp_obj_exp)
			REFERENCES simlab.experiment(exp_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
	FOREIGN KEY (exp_obj_obj)
			REFERENCES simlab.objective(obj_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE

);


CREATE TABLE IF NOT EXISTS simlab.r_request_v_alternative
(
	re_alt_re INT NOT NULL,
    re_alt_alt INT NOT NULL,
    utility FLOAT,
    FOREIGN KEY (re_alt_re)
		REFERENCES simlab.order_request(orr_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
	FOREIGN KEY (re_alt_alt)
		REFERENCES simlab.alternative(alt_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
	PRIMARY KEY(re_alt_re, re_alt_alt)
);



CREATE TABLE IF NOT EXISTS simlab.r_experiment_v_learning_experiment
(
	ele_exp_experiment INT NOT NULL,
    ele_exp_learning_input_experiment INT NOT NULL,
    ele_exp_period INT,
    ele_exp_requests BOOLEAN DEFAULT 1,
    ele_exp_routings BOOLEAN DEFAULT 0,
    ele_exp_routings_benchmarking BOOLEAN DEFAULT 0,
    ele_exp_order_sets BOOLEAN DEFAULT 0,
    ele_exp_order_sets_benchmarking BOOLEAN DEFAULT 0,
    FOREIGN KEY (ele_exp_experiment)
			REFERENCES simlab.experiment(exp_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
	FOREIGN KEY (ele_exp_learning_input_experiment)
			REFERENCES simlab.experiment(exp_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
     PRIMARY KEY(ele_exp_experiment, ele_exp_learning_input_experiment, ele_exp_period)       
);

CREATE TABLE IF NOT EXISTS simlab.value_function_approximation_type
(
	vft_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    vft_name VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS simlab.value_function_approximation_model_set
(
	vfs_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    vfs_name VARCHAR(200),
    vfs_type INT NOT NULL,
    vfs_time_window_set INT NOT NULL,
    vfs_delivery_area_set INT NOT NULL,
    vfs_number_boolean BOOLEAN DEFAULT 0, # if capacity used in numbers (or time)
    vfs_committed_boolean BOOLEAN DEFAULT 0, # if depending on already commited or to come
    vfs_area_weights_boolean BOOLEAN DEFAULT 0, # if weighted with area demand or value
	FOREIGN KEY (vfs_type)
			REFERENCES simlab.value_function_approximation_type(vft_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
	FOREIGN KEY (vfs_time_window_set)
			REFERENCES simlab.time_window_set(tws_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
	FOREIGN KEY (vfs_delivery_area_set)
			REFERENCES simlab.delivery_area_set(das_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE
);



CREATE TABLE IF NOT EXISTS simlab.value_function_approximation_model
(
	vfa_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    vfa_set INT NOT NULL,
    vfa_delivery_area INT NOT NULL, # assigned delivery area
    vfa_basic_coefficient DOUBLE,
    vfa_time_coefficient DOUBLE,
    vfa_time_capacity_interaction_coefficient DOUBLE,
    vfa_area_potential_coefficient DOUBLE,
    vfa_remaining_capacity_coefficient DOUBLE,
    vfa_overall_cost_coefficient DOUBLE,
    vfa_overall_cost_type INT,
    vfs_subarea_model longtext,
    vfa_complex_JSON longtext,
	FOREIGN KEY (vfa_set)
			REFERENCES simlab.value_function_approximation_model_set(vfs_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
	FOREIGN KEY (vfa_delivery_area)
			REFERENCES simlab.delivery_area(da_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE
);



CREATE TABLE IF NOT EXISTS simlab.value_function_approximation_coefficient
(
	vfc_model INT NOT NULL,
    vfc_delivery_area INT NOT NULL,
    vfc_time_window INT NOT NULL,
    vfc_coefficient DOUBLE,
    vfc_squared BOOLEAN DEFAULT 0,
    vfc_costs BOOLEAN DEFAULT 0,
    vfc_coverage BOOLEAN DEFAULT 0,
    vfc_demand_capacity_ratio BOOLEAN DEFAULT 0,
    vfc_type ENUM('cost', 'number', 'coverage', 'ratio', 'remaining_capacity','interaction_remaining_capacity_time'),
    FOREIGN KEY (vfc_model)
			REFERENCES simlab.value_function_approximation_model(vfa_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
	FOREIGN KEY (vfc_time_window)
			REFERENCES simlab.time_window(tw_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
	FOREIGN KEY (vfc_delivery_area)
			REFERENCES simlab.delivery_area(da_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE
    
);


CREATE TABLE IF NOT EXISTS simlab.r_run_v_value_function_approximation_model_set
(
	run_vfa_run INT NOT NULL,
	run_vfa_vfa INT NOT NULL,
    run_vfa_period TINYINT NOT NULL DEFAULT 0,
	FOREIGN KEY (run_vfa_run)
			REFERENCES simlab.run(run_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
	FOREIGN KEY (run_vfa_vfa)
			REFERENCES simlab.value_function_approximation_model_set(vfs_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
	PRIMARY KEY(run_vfa_run, run_vfa_vfa, run_vfa_period)

);

CREATE TABLE IF NOT EXISTS simlab.r_experiment_v_value_function_approximation_model_set
(
	exp_vfa_exp INT NOT NULL,
    exp_vfa_vfa INT NOT NULL,
    exp_vfa_period INT,
    FOREIGN KEY (exp_vfa_exp)
			REFERENCES simlab.experiment(exp_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
	FOREIGN KEY (exp_vfa_vfa)
			REFERENCES simlab.value_function_approximation_model_set(vfs_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
     PRIMARY KEY(exp_vfa_exp, exp_vfa_vfa, exp_vfa_period)       
);

CREATE TABLE IF NOT EXISTS simlab.model_training_log
(
	mtl_id INT NOT NULL auto_increment PRIMARY KEY,
    mtl_model INT NOT NULL,
    mtl_repetition INT NOT NULL, # order_request_set counter
    mtl_objective_function_value DOUBLE,
    mtl_coefficients VARCHAR(1000),
    FOREIGN KEY (mtl_model)
			REFERENCES simlab.value_function_approximation_model(vfa_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE
    
);

CREATE TABLE IF NOT EXISTS simlab.reference_routing
(
	rr_order_set INT NOT NULL,
    rr_delivery_area INT NOT NULL,
    rr_routing INT NOT NULL,
    rr_left_over INT,
    rr_theft_spatial INT,
    rr_theft_advanced INT,
    rr_theft_time INT,
    rr_theft_spatial_first INT,
    rr_theft_advanced_first INT,
    rr_theft_time_first INT,
    FOREIGN KEY (rr_order_set)
			REFERENCES simlab.order_set(os_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
	FOREIGN KEY (rr_delivery_area)
			REFERENCES simlab.delivery_area(da_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
	FOREIGN KEY (rr_routing)
			REFERENCES simlab.routing(rou_id)
			ON DELETE CASCADE
			ON UPDATE CASCADE,
     PRIMARY KEY(rr_order_set, rr_delivery_area, rr_routing)       
);