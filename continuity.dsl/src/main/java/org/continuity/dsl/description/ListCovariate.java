package org.continuity.dsl.description;

import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * A covariate holding future values directly passed in the context input.
 * 
 * @author Alper Hidiroglu
 *
 */
@JsonDeserialize(as = ListCovariate.class)
public class ListCovariate extends GeneralCovariate implements Covariate {

	private List<Double> value;

	/**
	 * Gets the future values.
	 * 
	 * @return The future values.
	 */
	public List<Double> getValue() {
		return value;
	}

	/**
	 * Sets the future values.
	 * 
	 * @param value The future values.
	 */
	public void setValue(List<Double> value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "ListCovariate [value=" + value + "]";
	}

}
