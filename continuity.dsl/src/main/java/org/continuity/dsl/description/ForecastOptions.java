package org.continuity.dsl.description;

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

	@JsonProperty("forecast-period")
	private String forecastPeriod;
	
	private String interval;

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
	public String getForecastPeriod() {
		return forecastPeriod;
	}

	/**
	 * Sets the period of the workload forecasting.
	 * 
	 * @param forecastPeriod The period of the workload forecast.
	 */
	public void setForecastPeriod(String forecastPeriod) {
		this.forecastPeriod = forecastPeriod;
	}

	@Override
	public String toString() {
		return "Forecast [period=" + forecastPeriod + "]";
	}

}
