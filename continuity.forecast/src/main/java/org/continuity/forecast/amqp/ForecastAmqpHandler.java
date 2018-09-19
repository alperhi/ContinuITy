package org.continuity.forecast.amqp;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.artifact.ForecastBundle;
import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.entities.report.TaskError;
import org.continuity.api.entities.report.TaskReport;
import org.continuity.api.rest.RestApi;
import org.continuity.commons.storage.MixedStorage;
import org.continuity.forecast.config.RabbitMqConfig;
import org.continuity.forecast.controllers.ForecastController;
import org.continuity.forecast.managers.ForecastPipelineManager;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Handles received monitoring data in order to create Behavior Mix and workload intensity.
 *
 * @author Alper Hidiroglu
 *
 */
@Component
public class ForecastAmqpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ForecastAmqpHandler.class);

	@Autowired
	private AmqpTemplate amqpTemplate;
	
	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private MixedStorage<ForecastBundle> storage;

	@Value("${spring.application.name}")
	private String applicationName;

	/**
	 * Listener to the RabbitMQ {@link RabbitMqConfig#TASK_CREATE_QUEUE_NAME}. Creates a forecast bundle based on sessions.
	 *
	 * @param task
	 *            The description of the task to be done.
	 * @return The id that can be used to retrieve the forecast bundle later on.
	 * @see ForecastController
	 */
	@RabbitListener(queues = RabbitMqConfig.TASK_CREATE_QUEUE_NAME)
	public void onMonitoringDataAvailable(TaskDescription task) {
		LOGGER.info("Task {}: Received new task to be processed for tag '{}'", task.getTaskId(), task.getTag());
		
		String linkToSessions = task.getSource().getSessionsBundlesLinks().getLink();

		TaskReport report;

		if (linkToSessions == null) {
			LOGGER.error("Task {}: Link to sessions is missing for tag {}!", task.getTaskId(), task.getTag());
			report = TaskReport.error(task.getTaskId(), TaskError.MISSING_SOURCE);
		} else {
			InfluxDB influxDb = InfluxDBFactory.connect("http://127.0.0.1:8086", "admin", "admin");
			ForecastPipelineManager pipelineManager = new ForecastPipelineManager(restTemplate, influxDb, task.getTag(), task.getContext());
			ForecastBundle forecastBundle = pipelineManager.runPipeline(linkToSessions);
			influxDb.close();

			if (forecastBundle == null) {
				LOGGER.info("Task {}: Could not create forecast for tag '{}'.", task.getTaskId(), task.getTag());

				report = TaskReport.error(task.getTaskId(), TaskError.INTERNAL_ERROR);
			} else {
				String storageId = storage.put(forecastBundle, task.getTag(), task.isLongTermUse());
				String forecastLink = RestApi.Forecast.ForecastResult.GET.requestUrl(storageId).withoutProtocol().get();

				LOGGER.info("Task {}: Created a new forecast with id '{}'.", task.getTaskId(), storageId);

				report = TaskReport.successful(task.getTaskId(), new LinkExchangeModel().getForecastLinks().setLink(forecastLink).parent());
			}
		}

		amqpTemplate.convertAndSend(AmqpApi.Global.EVENT_FINISHED.name(), AmqpApi.Global.EVENT_FINISHED.formatRoutingKey().of(RabbitMqConfig.SERVICE_NAME), report);
	}

}
