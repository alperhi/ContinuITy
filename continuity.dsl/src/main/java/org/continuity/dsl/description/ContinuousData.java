package org.continuity.dsl.description;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Represents the continuous data context type.
 * @author Alper Hidiroglu
 *
 */
@JsonDeserialize(as = ContinuousData.class)
public class ContinuousData implements Covariate {
	
	@JsonProperty("location-name")
	private String locationName;

	@JsonCreator
    public ContinuousData(@JsonProperty(value = "location-name", required = true) String locationName) {
    	this.locationName = locationName;
    }
	
	public ContinuousData() {
		
	}
	
	public String getLocationName() {
		return locationName;
	}

	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}
	
	@Override
	public String toString() {
		return "ContinuousData [covar=" + locationName + "]";
	}

}
