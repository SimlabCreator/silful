INSERT IGNORE INTO Playground.process
(pro_id, pro_name, pro_description) VALUES 
(1, "Data generation order requests","Includes customer generation and order request generation"),
(2, "FcfsORSProcess","Routing based acceptance of orders"),
(3, "IndependentDemandAssumptionControlsORSProcess", "Indepdenent demand process with dependent demand - controls"),
(4, "IndependentDemandAssumptionAcceptanceORSProcess", "Indepdenent demand process with dependent demand - acceptance"),
(5, "PsychicDependentDemandForecastORSProcess", "Psychic forecast under dependent demand"),
(6, "EfficientRoutingORSProcess", "Time-efficient routing without order value consideration"),
(7, "ObjectiveBasedRoutingORSProcess", "Routing with order value consideration"),
(8, "DynamicProgrammingDecisionTreeORSProcess", "DP tree pre-calculation (multiple threads)"),
(9, "DependentDemandAcceptanceORSProcess", "Uses DP tree and capacities for acceptance"),
(10, "FcfsORSProcess_runtimeComparison", "Duplicate travel times"),
(11, "ObjectiveBasedRoutingORSProcess_runtimeComparison", "Duplicate travel times"),
(12, "ObjectiveBasedRoutingORSProcess_minimumRequestNo", "Define minium number of requests per combination"),
(13, "OrienteeringGRILSProcess_adapted","pychic orienteering for learning, GRILS adapted"),
(14, "ObjectiveBasedRoutingILSOrienteering", "Orienteering with order value consideration"),
(15, "LinearValueFunctionAsYangTrainingProcess", "linear adp as in Yang and Strauss 2017"),
(16, "OrienteeringFCFS",""),
(17, "ObjectiveBasedGreedy_TSL", "greedy with setting for TSL"),
(18, "GRILSForLearningAdaptedWithMultipleAreasProcess", "Grils with vehicle area assignment"),
(19, "AverageCapacityDeterminationProcess", "determines average capacities over multiple routings"),
(20, "DeterministicLinearProgrammingPreparationProcess", "linear programming from zhang/cooper-determination of beta"),
(21, "DeterministicLinearProgrammingAcceptanceProcess", "linear programming from zhang/cooper-acceptance"),
(22, "LinearValueFunctionAsYangAcceptanceProcess", "linear adp as in Yang and Strauss 2017  - acceptance"),
(23, "DataGenerationOrderRequestsII", "includes customer generation and order request generation"),
(24, "CampbellSavelsbergh2005AcceptanceProcess", ""),
(25, "TabularValueFunctionTrainingProcess", ""),
(26, "LinearValueFunctionOrienteeringTrainingProcess", ""),
(27, "LinearValueFunctionOrienteeringAcceptanceProcess",""),
(28, "OrienteeringAcceptanceProcess", ""),
(29, "Final_CampbellSavelsbergh2006FeasibilityOrCostAcceptanceProcess", ""),
(30, "OrienteeringAcceptanceGRASPProcess", ""),
(31, "ANNValueFunctionOrienteeringTrainingProcess",""),
(32, "ANNOrienteeringAcceptanceProcess", ""),
(33, "ALNSFinalRoutingWithInfeasibleProcess", ""),
(34, "GRILSForLearningWithMultipleAreasProcess-finalTOP", ""),
(35, "DeterministicProgrammingAcceptanceProcess-finalDP",""),
(36, "DeterministicProgrammingPreparationProcess", ""),
(37, "OrienteeringBasedAcceptanceWithALNS",""),
(38, "DeterministicProgrammingAcceptanceWithALNSProcess_finalDP", ""),
(39, "ANNOrienteeringAcceptanceALNSProcess", ""),
(40, "FCFSInsertionAndALNSFinalProcess", ""),
(41, "MesoValueFunctionTrainingProcess", ""),
(42, "MesoValueFunctionAcceptanceProcess", ""),
(43, "Yang2016Algorithm1ForDynamicSlottingProcess", "");

# Test

INSERT IGNORE INTO SimLab.node
(SELECT * FROM NodesHelper.node);

INSERT IGNORE INTO SimLab.distance
(SELECT * FROM NodesHelper.distance);