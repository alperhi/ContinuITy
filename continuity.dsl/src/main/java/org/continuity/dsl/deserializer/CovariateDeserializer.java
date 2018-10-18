package org.continuity.dsl.deserializer;

import java.io.IOException;

import org.continuity.dsl.description.ContinuousData;
import org.continuity.dsl.description.Covariate;
import org.continuity.dsl.description.Event;

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

		if (root.has("location-name") && root.has("covar") && root.has("future-dates")) {
			covariate = mapper.readValue(root.toString(), Event.class);
		} else if (root.has("location-name") && (!root.has("covar"))) {
			covariate = mapper.readValue(root.toString(), ContinuousData.class);
		} else {
			throw new IOException("Invalid context input!");
		}
		return covariate;
	}
}