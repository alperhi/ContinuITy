package org.continuity.dsl;

import java.io.File;

import org.continuity.dsl.description.Context;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Utility class.
 * 
 * @author Alper Hidiroglu
 *
 */
public class Deserializer {

	public Context deserialize() {
		Context descr = null;
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		try {

			descr = mapper.readValue(new File("C:/Users/ahi/Desktop/ContextDescriptions/context.yaml"),
					Context.class);

		} catch (Exception e) {

			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return descr;
	}
}
