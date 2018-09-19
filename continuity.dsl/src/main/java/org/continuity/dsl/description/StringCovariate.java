package org.continuity.dsl.description;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * A covariate that has a String value. The String can be of different type.
 * 
 * @author Alper Hidiroglu
 *
 */
@JsonDeserialize(as = StringCovariate.class)
public class StringCovariate extends GeneralCovariate implements Covariate {

	private String value;
	private StringType typeOfString;

	/**
	 * Gets the String value.
	 * 
	 * @return The String value.
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Sets the String value.
	 * 
	 * @param value The String value.
	 */
	public void setValue(String value) {
		this.value = value;
		processString(value);
	}

	/**
	 * Detects the String type.
	 * 
	 * @param value String to evaluate.
	 */
	private void processString(String value) {
		if (value.matches(".*[<>=].*") || value.contains("TO")) {
			setTypeOfString(StringType.OPERATIONSTRING);
		} else if (value.contains("/")) {
			setTypeOfString(StringType.LOCATIONSTRING);
		} else {
			setTypeOfString(StringType.SIMPLESTRING);
		}
	}

	public StringType getTypeOfString() {
		return typeOfString;
	}

	public void setTypeOfString(StringType typeOfString) {
		this.typeOfString = typeOfString;
	}

	@Override
	public String toString() {
		return "StringCovariate [value=" + value + ", typeOfString=" + typeOfString + "]";
	}

	/**
	 * Possible String types.
	 * 
	 * @author Alper Hidiroglu
	 *
	 */
	public enum StringType {
		SIMPLESTRING, OPERATIONSTRING, LOCATIONSTRING
	}
}
