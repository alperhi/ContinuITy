package org.continuity.cli.commands;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

import org.continuity.api.rest.RestApi.Orchestrator.Idpa;
import org.continuity.cli.config.PropertiesProvider;
import org.continuity.commons.idpa.AnnotationExtractor;
import org.continuity.commons.utils.WebUtils;
import org.continuity.idpa.IdpaElement;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.yaml.IdpaYamlSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * CLI for annotation handling.
 *
 * @author Henning Schulz
 *
 */
@ShellComponent
public class IdpaCommands {

	@Autowired
	private PropertiesProvider propertiesProvider;

	@Autowired
	private RestTemplate restTemplate;

	@ShellMethod(key = { "idpa-download" }, value = "Downloads and opens the IDPA with the specified tag.")
	public String downloadIdpa(String tag) throws JsonGenerationException, JsonMappingException, IOException {
		String url = WebUtils.addProtocolIfMissing(propertiesProvider.get().getProperty(PropertiesProvider.KEY_URL));

		ResponseEntity<Application> applicationResponse = restTemplate.getForEntity(Idpa.GET_APPLICATION.requestUrl(tag).withHost(url).get(), Application.class);
		ResponseEntity<ApplicationAnnotation> annotationResponse = restTemplate.getForEntity(Idpa.GET_ANNOTATION.requestUrl(tag).withHost(url).get(), ApplicationAnnotation.class);

		if (!applicationResponse.getStatusCode().is2xxSuccessful()) {
			return "Could not get application model: " + applicationResponse;
		}

		if (!annotationResponse.getStatusCode().is2xxSuccessful()) {
			return "Could not get annotation: " + annotationResponse;
		}

		IdpaYamlSerializer<IdpaElement> serializer = new IdpaYamlSerializer<>(IdpaElement.class);
		String workingDir = propertiesProvider.get().getProperty(PropertiesProvider.KEY_WORKING_DIR);
		File applicationFile = new File(workingDir + "/application-" + tag + ".yml");
		File annotationFile = new File(workingDir + "/annotation-" + tag + ".yml");
		serializer.writeToYaml(applicationResponse.getBody(), applicationFile);
		serializer.writeToYaml(annotationResponse.getBody(), annotationFile);

		Desktop.getDesktop().open(applicationFile);
		Desktop.getDesktop().open(annotationFile);

		return "Downloaded and opened the IDPA with tag " + tag + " from " + workingDir;
	}

	@ShellMethod(key = { "idpa-open" }, value = "Opens an already downloaded IDPA with the specified tag.")
	public String openIdpa(String tag) throws IOException {
		String workingDir = propertiesProvider.get().getProperty(PropertiesProvider.KEY_WORKING_DIR);
		File applicationFile = new File(workingDir + "/application-" + tag + ".yml");
		File annotationFile = new File(workingDir + "/annotation-" + tag + ".yml");

		Desktop.getDesktop().open(applicationFile);
		Desktop.getDesktop().open(annotationFile);

		return "Opened the IDPA with tag " + tag + " from " + workingDir;
	}

	@ShellMethod(key = { "idpa-ann-upload" }, value = "Uploads the annotation with the specified tag.")
	public String uploadAnnotation(String tag) throws JsonParseException, JsonMappingException, IOException {
		String workingDir = propertiesProvider.get().getProperty(PropertiesProvider.KEY_WORKING_DIR);
		IdpaYamlSerializer<ApplicationAnnotation> serializer = new IdpaYamlSerializer<>(ApplicationAnnotation.class);
		ApplicationAnnotation annotation = serializer.readFromYaml(workingDir + "/annotation-" + tag + ".yml");

		String url = WebUtils.addProtocolIfMissing(propertiesProvider.get().getProperty(PropertiesProvider.KEY_URL));
		ResponseEntity<String> response;
		try {
			response = restTemplate.postForEntity(Idpa.UPDATE_ANNOTATION.requestUrl(tag).withHost(url).get(), annotation, String.class);
		} catch (HttpStatusCodeException e) {
			response = new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
		}

		if (!response.getStatusCode().is2xxSuccessful()) {
			return "Error during upload: " + response;
		} else {
			return "Successfully uploaded the annotation with tag " + tag + ". Report is: " + response.getBody();
		}
	}

	@ShellMethod(key = { "idpa-app-upload" }, value = "Handle with care! Uploads the application model with the specified tag. Can break the online stored annotation!")
	public String uploadApplication(String tag) throws JsonParseException, JsonMappingException, IOException {
		String workingDir = propertiesProvider.get().getProperty(PropertiesProvider.KEY_WORKING_DIR);
		IdpaYamlSerializer<Application> serializer = new IdpaYamlSerializer<>(Application.class);
		Application application = serializer.readFromYaml(workingDir + "/application-" + tag + ".yml");

		String url = WebUtils.addProtocolIfMissing(propertiesProvider.get().getProperty(PropertiesProvider.KEY_URL));
		ResponseEntity<String> response;
		try {
			response = restTemplate.postForEntity(Idpa.UPDATE_APPLICATION.requestUrl(tag).withHost(url).get(), application, String.class);
		} catch (HttpStatusCodeException e) {
			response = new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
		}

		if (!response.getStatusCode().is2xxSuccessful()) {
			return "Error during upload: " + response;
		} else {
			return "Successfully uploaded the application with tag " + tag + ". Report is: " + response.getBody();
		}
	}

	@ShellMethod(key = { "idpa-ann-init" }, value = "Initializes an annotation for the stored application model with the specified tag.")
	public String initAnnotation(String tag) throws JsonParseException, JsonMappingException, IOException {
		String workingDir = propertiesProvider.get().getProperty(PropertiesProvider.KEY_WORKING_DIR);
		File systemFile = new File(workingDir + "/application-" + tag + ".yml");
		File annFile = new File(workingDir + "/annotation-" + tag + ".yml");

		IdpaYamlSerializer<Application> systemSerializer = new IdpaYamlSerializer<>(Application.class);
		Application system = systemSerializer.readFromYaml(systemFile);
		ApplicationAnnotation annotation = new AnnotationExtractor().extractAnnotation(system);

		IdpaYamlSerializer<ApplicationAnnotation> annSerializer = new IdpaYamlSerializer<>(ApplicationAnnotation.class);
		annSerializer.writeToYaml(annotation, annFile);

		Desktop.getDesktop().open(systemFile);
		Desktop.getDesktop().open(annFile);

		return "Initialized and opened the annotation.";
	}

}
