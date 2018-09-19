package org.continuity.dsl.description;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * A covariate with numerical value.
 * 
 * @author Alper Hidiroglu
 *
 */
@JsonDeserialize(as = NumericalCovariate.class)
public class NumericalCovariate extends GeneralCovariate implements Covariate {

	private double value;

	/**
	 * Gets the numerical value.
	 * 
	 * @return The numerical value.
	 */
	public double getValue() {
		return value;
	}

	/**
	 * Sets the numerical value.
	 * 
	 * @param value The numerical value.
	 */
	public void setValue(double value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "NumericalCovariate [value=" + value + "]";
	}

}
