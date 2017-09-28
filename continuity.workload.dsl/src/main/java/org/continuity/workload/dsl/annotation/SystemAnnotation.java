/**
 */
package org.continuity.workload.dsl.annotation;

import java.util.ArrayList;
import java.util.List;

/**
 * Annotation of a {@link org.continuity.workload.dsl.system.TargetSystem} representation. Holds manual
 * changes that should be kept while changing the system itself. <br>
 *
 * An annotation consists of a collection of inputs that can be either input data or extractions
 * from the responses of called interfaces. Additionally, it holds annotations of interfaces stating
 * which inputs to push into which parameters.
 *
 * @author Henning Schulz
 *
 */
public class SystemAnnotation {

	private List<Input> inputs;

	private List<InterfaceAnnotation> interfaceAnnotations;

	/**
	 * Returns the inputs.
	 *
	 * @return The inputs.
	 */
	public List<Input> getInputs() {
		return this.inputs;
	}

	/**
	 * Sets the inputs.
	 *
	 * @param inputs
	 *            The inputs.
	 */
	public void setInputs(List<Input> inputs) {
		this.inputs = inputs;
	}

	/**
	 * Returns the interface annotations.
	 *
	 * @return {@link #interfaceAnnotations}
	 */
	public List<InterfaceAnnotation> getInterfaceAnnotations() {
		if (interfaceAnnotations == null) {
			interfaceAnnotations = new ArrayList<>();
		}

		return this.interfaceAnnotations;
	}

	/**
	 * Sets the interface annotations.
	 *
	 * @param interfaceAnnotations
	 *            The interface annotations.
	 */
	public void setInterfaceAnnotations(List<InterfaceAnnotation> interfaceAnnotations) {
		this.interfaceAnnotations = interfaceAnnotations;
	}

}