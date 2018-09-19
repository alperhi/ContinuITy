package org.continuity.dsl.description;

import org.continuity.dsl.deserializer.CovariateDeserializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * A context covariate.
 * 
 * @author Alper Hidiroglu
 *
 */
@JsonDeserialize(using = CovariateDeserializer.class)
public interface Covariate {

}
