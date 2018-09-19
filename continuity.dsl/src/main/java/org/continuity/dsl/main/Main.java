//package org.continuity.dsl.main;
//
//import java.io.File;
//
//import org.continuity.dsl.description.Context;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
//
///**
// * Testing purposes.
// * 
// * @author Alper Hidiroglu
// *
// */
//public class Main {
//
//	/**
//	 * Test of YAML to object mapping.
//	 * 
//	 * @param args
//	 */
//	public static void main(String[] args) {
//		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
//
//		try {
//
//			Context descr = mapper.readValue(new File("C:/Users/ahi/Desktop/ContextDescriptions/context.yaml"),
//					Context.class);
//
//			// System.out.println(ReflectionToStringBuilder.toString(descr,
//			// ToStringStyle.MULTI_LINE_STYLE));
//
//			// StringCovariate covar = (StringCovariate) descr.getCovariates().get(0);
//
//		} catch (Exception e) {
//
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//
//		}
//	}
//
//}
