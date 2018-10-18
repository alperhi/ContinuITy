package org.continuity.dsl.description;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Represents the event context type.
 * 
 * @author Alper Hidiroglu
 *
 */
@JsonDeserialize(as = Event.class)
public class Event implements Covariate {
	
	@JsonProperty("location-name")
	private String locationName;

	private String covar;
	
	@JsonProperty("future-dates")
	@JsonSerialize(converter=FutureOccurrencesConverter.class)
	private FutureOccurrences futureDates;
	
    @JsonCreator
    public Event(@JsonProperty(value = "location-name", required = true) String locationName, @JsonProperty(value = "covar", required = true) String covar, @JsonProperty(value = "future-dates", required = true) List<String> futureDates) {
    	this.locationName = locationName;
        this.covar = covar;
        this.futureDates = new FutureOccurrences(futureDates);
    }
    
    public Event() {
    	
    }
    
	public String getLocationName() {
		return locationName;
	}

	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}
	
	public String getCovar() {
		return covar;
	}

	public void setCovar(String covar) {
		this.covar = covar;
	}

	@JsonIgnore
	public FutureOccurrences getFutureDates() {
		return futureDates;
	}

	@JsonIgnore
	public void setFutureDates(ArrayList<String> futureDates) {
		this.futureDates = new FutureOccurrences(futureDates);
	}
	
	@Override
	public String toString() {
		return "Event [location-name=" + locationName + ", covar=" + covar + ", future-dates=" + futureDates + "]";
	}
}
