package data.entity;

import java.util.ArrayList;

import data.utility.DataServiceProvider;
import logic.service.support.LocationService;

public class DeliveryAreaSet extends SetEntity {

	private String name;
	private String description;
	private Integer regionId;
	private Region region;
	private boolean predefined;
	private ArrayList<DeliveryArea> deliveryAreas;
	private Integer reasonableNumberOfAreas;

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getRegionId() {
		if (this.regionId == null) {
			this.regionId = this.region.getId();
		}
		return this.regionId;
	}

	public void setRegionId(int regionId) {
		this.regionId = regionId;
	}

	public Region getRegion() {
		if (region == null) {
			region = (Region) DataServiceProvider.getRegionDataServiceImplInstance().getById(regionId);
		}
		return region;
	}

	public void setRegion(Region region) {
		this.region = region;
		this.regionId = this.region.getId();
	}

	public boolean isPredefined() {
		return predefined;
	}

	public void setPredefined(boolean predefined) {
		this.predefined = predefined;
	}

	@Override
	public ArrayList<DeliveryArea> getElements() {

		if (deliveryAreas == null) {
			if (this.predefined) {
				deliveryAreas = DataServiceProvider.getDeliveryAreaDataServiceImplInstance()
						.getAllElementsBySetId(this.id);
			} else {
				DeliveryArea area = DataServiceProvider.getDeliveryAreaDataServiceImplInstance()
						.getDeliveryAreaBySubsetId(this.id);
				deliveryAreas = LocationService.determineSameSizeDeliveryAreasWithDummyIds(this.id, area.getLat1(),
						area.getLat2(), area.getLon1(), area.getLon2(), (int) Math.sqrt(reasonableNumberOfAreas),
						(int) Math.sqrt(reasonableNumberOfAreas));
				DataServiceProvider.getDeliveryAreaDataServiceImplInstance().updateDeliveryAreaSetToPredefined(this);
				for (DeliveryArea a : deliveryAreas) {
					a.setSetId(this.id);
					a.setId(DataServiceProvider.getDeliveryAreaDataServiceImplInstance().persistElement(a));
				}
				this.predefined=true;
			}
		}
		return deliveryAreas;
	}

	public void setElements(ArrayList<DeliveryArea> elements) {
		this.deliveryAreas = elements;

	}

	/**
	 * Checks if the delivery area set is a hierarchy, meaning that the delivery
	 * areas are divided into subareas
	 * 
	 * @return
	 */
	public boolean isHierarchy() {
		for (DeliveryArea area : this.getElements()) {
			if (area.getSubsetId() != 0)
				return true;
		}
		return false;
	}

	@Override
	public String toString() {

		return id + "; " + name + "; " + description;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o instanceof DeliveryAreaSet) {
			DeliveryAreaSet other = (DeliveryAreaSet) o;
			return this.id == other.getId();// TODO: Reconsider if areas are
											// created on the fly (no id yet)
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.id;
	}

	public Integer getReasonableNumberOfAreas() {
		return reasonableNumberOfAreas;
	}

	public void setReasonableNumberOfAreas(Integer reasonableNumberOfAreas) {
		this.reasonableNumberOfAreas = reasonableNumberOfAreas;
	}

}
