package org.continuity.dsl.description;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Represents a context input.
 * 
 * @author Alper Hidiroglu
 *
 */
@JsonPropertyOrder({ "covariates", "forecast-options" })
public class ForecastInput {

	private List<Covariate> covariates;

	@JsonProperty("forecast-options")
	private ForecastOptions forecastOptions;
	
	@JsonCreator
    public ForecastInput(@JsonProperty(value = "covariates", required = true) List<Covariate> covariates, @JsonProperty(value = "forecast-options", required = true) ForecastOptions forecastOptions) {
    	this.covariates = covariates;
    	this.forecastOptions = forecastOptions;
    }
	
	public ForecastInput() {
		
	}

	/**
	 * Returns context covariates for the workload forecasting.
	 * 
	 * @return The context covariates.
	 */
	public List<Covariate> getCovariates() {
		return covariates;
	}

	/**
	 * Sets the context covariates for the workload forecasting.
	 * 
	 * @param covariates The context covariates.
	 * 
	 */
	public void setCovariates(List<Covariate> covariates) {
		this.covariates = covariates;
	}

	/**
	 * Gets further information for the workload forecasting.
	 * 
	 * @return The forecasting information.
	 */
	public ForecastOptions getForecastOptions() {
		return forecastOptions;
	}

	/**
	 * Sets further information for the workload forecasting.
	 * 
	 * @param forecast The forecasting information.
	 */
	public void setForecastOptions(ForecastOptions forecast) {
		this.forecastOptions = forecast;
	}

	@Override
	public String toString() {
		return "Forecast-Input [covariates=" + covariates + ", forecastOptions=" + forecastOptions + "]";
	}

}
