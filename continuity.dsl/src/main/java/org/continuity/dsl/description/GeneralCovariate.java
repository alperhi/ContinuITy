package org.continuity.dsl.description;

/**
 * Represents attributes that are equal for each covariate.
 * 
 * @author Alper Hidiroglu
 *
 */
public class GeneralCovariate {

	private String name;
	private String date;

	/**
	 * Gets the name of the covariate.
	 * 
	 * @return The name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of the covariate.
	 * 
	 * @param name The name.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the future date(s) the covariate occurs.
	 * 
	 * @return The date(s)
	 */
	public String getDate() {
		return date;
	}

	/**
	 * Sets the future date(s) the covariate occurs.
	 * 
	 * @param date The date(s)
	 */
	public void setDate(String date) {
		this.date = date;
	}

	@Override
	public String toString() {
		return "GeneralCovariate [name=" + name + ", date=" + date + "]";
	}

}
