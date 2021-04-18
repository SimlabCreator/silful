package data.entity;

import data.utility.DataServiceProvider;

public class Kpi extends Entity {

	private int kpyTypeId;
	private Double value;
	private KpiType kpiType;
	private int run;
	private int period;
	
	private int id;
	//No functionality, only for parent-class
	public int getId() {
		return id;
	}
	
	public int getKpyTypeId() {
		return kpyTypeId;
	}

	public void setKpyTypeId(int kpyTypeId) {
		this.kpyTypeId = kpyTypeId;
	}

	public Double getValue() {
		return value;
	}

	public void setValue(Double value) {
		this.value = value;
	}

	public KpiType getKpiType() {
		if (kpiType == null) {
			this.kpiType = (KpiType) DataServiceProvider.getKpiTypeDataServiceImplInstance().getById(this.kpyTypeId);
		}
		return kpiType;
	}

	public void setKpiType(KpiType kpiType) {
		this.kpiType = kpiType;
	}

	public int getRun() {
		return run;
	}

	public void setRun(int run) {
		this.run = run;
	}

	public int getPeriod() {
		return period;
	}

	public void setPeriod(int period) {
		this.period = period;
	}

}
