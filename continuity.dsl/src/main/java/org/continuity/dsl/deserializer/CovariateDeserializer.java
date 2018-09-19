package org.continuity.dsl.deserializer;

import java.io.IOException;

import org.continuity.dsl.description.BooleanCovariate;
import org.continuity.dsl.description.Covariate;
import org.continuity.dsl.description.ListCovariate;
import org.continuity.dsl.description.NumericalCovariate;
import org.continuity.dsl.description.StringCovariate;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Custom deserializer to differentiate between different types of covariate
 * values.
 * 
 * @author Alper Hidiroglu
 *
 */
public class CovariateDeserializer extends JsonDeserializer<Covariate> {

	@Override
	public Covariate deserialize(JsonParser p, DeserializationContext cntxt)
			throws IOException, JsonProcessingException {
		ObjectMapper mapper = (ObjectMapper) p.getCodec();
		ObjectNode root = mapper.readTree(p);

		Covariate covariate = null;

		if (root.has("value")) {
			if (root.get("value").isTextual()) {
				covariate = mapper.readValue(root.toString(), StringCovariate.class);
			} else if (root.get("value").isBoolean()) {
				covariate = mapper.readValue(root.toString(), BooleanCovariate.class);
			} else if (root.get("value").isNumber()) {
				covariate = mapper.readValue(root.toString(), NumericalCovariate.class);
			} else if (root.get("value").isArray()) {
				covariate = mapper.readValue(root.toString(), ListCovariate.class);
			}
		}
		return covariate;
	}
}