package org.continuity.workload.dsl.annotation.yaml;

import java.io.IOException;

import org.continuity.workload.dsl.ContinuityModelElement;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.filter.TokenFilter;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.ResolvableSerializer;

/**
 * @author Henning Schulz
 *
 */
public class ContinuityModelSerializer extends JsonSerializer<ContinuityModelElement> implements ContextualSerializer, ResolvableSerializer {

	private final JsonSerializer<Object> defaultSerializer;

	/**
	 *
	 */
	public ContinuityModelSerializer(JsonSerializer<Object> defaultSerializer) {
		this.defaultSerializer = defaultSerializer;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void serialize(ContinuityModelElement value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		IdHandlingGeneratorDelegate delegate = new IdHandlingGeneratorDelegate(gen);
		defaultSerializer.serialize(value, delegate, provider);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void serializeWithType(ContinuityModelElement value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
		IdHandlingGeneratorDelegate delegate = new IdHandlingGeneratorDelegate(gen);
		defaultSerializer.serializeWithType(value, delegate, serializers, typeSer);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
		if (defaultSerializer instanceof ContextualSerializer) {
			JsonSerializer<?> contextual = ((ContextualSerializer) defaultSerializer).createContextual(prov, property);
			return new ContinuityModelSerializer((JsonSerializer<Object>) contextual);
		}

		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void resolve(SerializerProvider provider) throws JsonMappingException {
		if (defaultSerializer instanceof ResolvableSerializer) {
			((ResolvableSerializer) defaultSerializer).resolve(provider);
		}
	}

	private static class IdHandlingGeneratorDelegate extends JsonGeneratorDelegate {

		/**
		 * @param d
		 */
		public IdHandlingGeneratorDelegate(JsonGenerator d) {
			super(d);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void writeObjectId(Object id) throws IOException {
			if ((id != null) && !"null".equals(id)) {
				super.writeObjectId(id);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void writeStringField(String fieldName, String value) throws IOException {
			if (!"id".equals(fieldName)) {
				super.writeStringField(fieldName, value);
			}
		}

	}

	private static final class IdFilter extends TokenFilter {

		// /**
		// * {@inheritDoc}
		// */
		// @Override
		// public TokenFilter includeProperty(String name) {
		// if ("id".equals(name)) {
		// return null;
		// }
		//
		// return super.includeProperty(name);
		// }

	}

}
