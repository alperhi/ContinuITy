package org.continuity.dsl.description;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Further information for the workload forecasting.
 * 
 * @author Alper Hidiroglu
 *
 */
@JsonPropertyOrder({ "forecast-period", "interval" })
public class ForecastOptions {

	@JsonProperty("forecast-date")
	private long forecastDate;
	
	private String interval;
	
	@SuppressWarnings("deprecation")
	@JsonCreator
    public ForecastOptions(@JsonProperty(value = "forecast-date", required = true) String forecastPeriod, @JsonProperty(value = "interval", required = true) String interval) {
    	this.forecastDate = Date.parse(forecastPeriod);
    	this.interval = interval;
    }

	public String getInterval() {
		return interval;
	}

	public void setInterval(String interval) {
		this.interval = interval;
	}

	/**
	 * Gets the period of the workload forecast.
	 * 
	 * @return The period of the workload forecast.
	 */
	public long getForecastPeriod() {
		return forecastDate;
	}

	/**
	 * Sets the period of the workload forecasting.
	 * 
	 * @param forecastPeriod The period of the workload forecast.
	 */
	public void setForecastPeriod(long forecastPeriod) {
		this.forecastDate = forecastPeriod;
	}

	@Override
	public String toString() {
		return "Forecast [forecast-date=" + forecastDate + "]";
	}

}
