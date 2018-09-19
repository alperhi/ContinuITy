package org.continuity.dsl.description;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * A covariate holding either true or false as value.
 * 
 * @author Alper Hidiroglu
 *
 */
@JsonDeserialize(as = BooleanCovariate.class)
public class BooleanCovariate extends GeneralCovariate implements Covariate {

	private boolean value;

	/**
	 * Gets the boolean value.
	 * 
	 * @return The boolean value.
	 */
	public boolean isValue() {
		return value;
	}

	/**
	 * Sets the boolean value.
	 * 
	 * @param value The boolean value.
	 */
	public void setValue(boolean value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "BooleanCovariate [value=" + value + "]";
	}

}
