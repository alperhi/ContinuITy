package org.continuity.forecast.managers;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.math3.util.Pair;
import org.continuity.api.entities.artifact.ForecastBundle;
import org.continuity.dsl.description.ForecastInput;
import org.continuity.dsl.description.ContinuousData;
import org.continuity.dsl.description.Covariate;
import org.continuity.dsl.description.Event;
import org.continuity.dsl.description.FutureOccurrences;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.dto.QueryResult.Result;
import org.influxdb.dto.QueryResult.Series;
import org.rosuda.JRI.Rengine;


/**
 * Manager for the workload forecasting.
 * @author Alper Hidiroglu
 *
 */
public class ForecastPipelineManager {

	// private static final Logger LOGGER = LoggerFactory.getLogger(ForecastPipelineManager.class);
	
	private InfluxDB influxDb;
	
	private String tag;
	
	private ForecastInput forecastInput;
	
	private int workloadIntensity;

	public int getWorkloadIntensity() {
		return workloadIntensity;
	}

	public void setWorkloadIntensity(int workloadIntensity) {
		this.workloadIntensity = workloadIntensity;
	}

	/**
	 * Constructor.
	 */
	public ForecastPipelineManager(InfluxDB influxDb, String tag, ForecastInput context) {
		this.influxDb = influxDb;
		this.tag = tag;
		this.forecastInput = context;
	}
	
	public void setupDatabase(){
		String dbName = this.tag;
		influxDb.setDatabase(dbName);
		influxDb.setRetentionPolicy("autogen");
	}

	/**
	 * Runs the pipeline.
	 *
	 * @return The generated forecast bundle.
	 */
	public ForecastBundle runPipeline(Pair<Date, Integer> dateAndAmountOfUsers) {
		setupDatabase();
		ForecastBundle forecastBundle = generateForecastBundle(dateAndAmountOfUsers);

		return forecastBundle;
	}
	
	/**
	 * Generates the forecast bundle.
	 * @param logs
	 * @return
	 * @throws IOException 
	 * @throws ExtractionException 
	 * @throws ParseException 
	 */
	private ForecastBundle generateForecastBundle(Pair<Date, Integer> dateAndAmountOfUsers) {
		// initialize intensity
		this.workloadIntensity = 1;
		// updates also the workload intensity 
		LinkedList<Double> probabilities = forecastWorkload(dateAndAmountOfUsers.getValue());
		// forecast result
		return new ForecastBundle(dateAndAmountOfUsers.getKey(), this.workloadIntensity, probabilities);
	}
	
	/**
	 * Returns aggregated workload intensity and adapted behavior mix probabilities.
	 * @param bundleList
	 * @return
	 */
	private LinkedList<Double> forecastWorkload(int amountOfUserGroups) {
		Rengine re = initializeRengine();
		
		LinkedList<Double> probabilities = new LinkedList<Double>();
		int sumOfIntensities = 0;
		List<Integer> forecastedIntensities = new LinkedList<Integer>();
		
		if(forecastInput.getForecastOptions().getForecaster().equalsIgnoreCase("Telescope")) {
			initializeTelescope(re);
			
			for(int i = 0; i < amountOfUserGroups; i++) {
				int intensity = forecastIntensityForUserGroupTelescope(i, re);
				forecastedIntensities.add(intensity);
				sumOfIntensities += intensity;
			}
		} else if(forecastInput.getForecastOptions().getForecaster().equalsIgnoreCase("Prophet")) {
			initializeProphet(re);
			
			for(int i = 0; i < amountOfUserGroups; i++) {
				int intensity = forecastIntensityForUserGroupProphet(i, re);
				forecastedIntensities.add(intensity);
				sumOfIntensities += intensity;
			}
		}
		re.end();
		// updates the workload intensity
		setWorkloadIntensity(sumOfIntensities);
				
		for(int intensity: forecastedIntensities) {
			double probability = (double) intensity / (double) sumOfIntensities;
			probabilities.add(probability);
		}
		return probabilities;
	}
	
	/**
	 * Forecasting the intensities for a user group using Prophet.
	 * @param i
	 * @param re
	 * @return
	 */
	private int forecastIntensityForUserGroupProphet(int i, Rengine re) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Pair<ArrayList<String>, ArrayList<Double>> datesAndIntensities = getIntensitiesForUserGroupFromDatabaseProphet(i);
		ArrayList<String> datesOfIntensities = datesAndIntensities.getKey();
		ArrayList<Double> intensities = datesAndIntensities.getValue();
		
		String endTimeDateString = datesOfIntensities.get(datesOfIntensities.size() - 1);
		long endTimeIntensities = 0L;
		try {
			endTimeIntensities = dateFormat.parse(endTimeDateString).getTime();
		} catch (ParseException e) {
		    e.printStackTrace();
		}
		
		long endTimeForecast = this.forecastInput.getForecastOptions().getDateAsTimestamp();
		
		long interval = calculateInterval(this.forecastInput.getForecastOptions().getInterval());
		
		long startTimeForecast = endTimeIntensities + interval;
		long forecastTimestamp = startTimeForecast;
		long endTime = endTimeForecast;
		ArrayList<Long> futureTimestamps = new ArrayList<Long>();
		while (forecastTimestamp <= endTime) {
			futureTimestamps.add(forecastTimestamp);
			forecastTimestamp += interval;
		}
		
		int intensity = forecastWithProphet(datesOfIntensities, intensities, futureTimestamps.size(), re);
		return intensity;
	}

	/**
	 * Forecasting the intensities for a user group using Telescope.
	 * @param i
	 * @param re
	 * @return
	 */
	private int forecastIntensityForUserGroupTelescope(int i, Rengine re) {
		Pair<ArrayList<Long>, ArrayList<Double>> timestampsAndIntensities = getIntensitiesForUserGroupFromDatabaseTelescope(i);
		ArrayList<Long> timestampsOfIntensities = timestampsAndIntensities.getKey();
		ArrayList<Double> intensities = timestampsAndIntensities.getValue();
		long startTime = timestampsOfIntensities.get(0);
		long endTimeIntensities = timestampsOfIntensities.get(timestampsOfIntensities.size() - 1);
		
		long endTimeForecast = this.forecastInput.getForecastOptions().getDateAsTimestamp();
		
		long interval = calculateInterval(this.forecastInput.getForecastOptions().getInterval());
		
		long startTimeForecast = endTimeIntensities + interval;
		long forecastTimestamp = startTimeForecast;
		long endTime = endTimeForecast;
		ArrayList<Long> futureTimestamps = new ArrayList<Long>();
		while (forecastTimestamp <= endTime) {
			futureTimestamps.add(forecastTimestamp);
			forecastTimestamp += interval;
		}
		
		ArrayList<ArrayList<Double>> histCovariates = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> futureCovariates = new ArrayList<ArrayList<Double>>();
		if(forecastInput.getCovariates() != null) {
			for(Covariate covar: forecastInput.getCovariates()) {
				ArrayList<Double> historicalOccurrences = null;
				ArrayList<Double> futureOccurrences = null;
				if(covar instanceof ContinuousData) {
					historicalOccurrences = new ArrayList<Double>();
					ContinuousData contCovar = (ContinuousData) covar;
					
					historicalOccurrences = getContinuousMeasurements(contCovar, convertTimestampToUtcDate(startTime), convertTimestampToUtcDate(endTimeIntensities));
					futureOccurrences = getContinuousMeasurements(contCovar, convertTimestampToUtcDate(startTimeForecast), convertTimestampToUtcDate(endTimeForecast));
				} else {
					Event eventCovar = (Event) covar;
					// calculate historical occurrences
					historicalOccurrences = new ArrayList<Double>(Collections.nCopies(timestampsOfIntensities.size(), 0.0));
					ArrayList<Long> timestampsOfCovar = getCovariateData(eventCovar, convertTimestampToUtcDate(startTime), convertTimestampToUtcDate(endTimeIntensities));
					for(long timestamp: timestampsOfCovar) {
						int index = timestampsOfIntensities.indexOf(timestamp);
						historicalOccurrences.set(index, 1.0);
					}
					FutureOccurrences future = eventCovar.getFutureDates();
					
					List<Long> futureTimestampsOfCovar = future.getFutureDatesAsTimestamps(interval);			
					
					// calculate future occurrences
					futureOccurrences = new ArrayList<Double>(Collections.nCopies(futureTimestamps.size(), 0.0));
					for(long futureTimestampOfCovar: futureTimestampsOfCovar) {
						int index = futureTimestamps.indexOf(futureTimestampOfCovar);
						futureOccurrences.set(index, 1.0);
					}
				}
				histCovariates.add(historicalOccurrences);
				futureCovariates.add(futureOccurrences);
			}
		}
		int intensity = forecastWithTelescope(intensities, histCovariates, futureCovariates, re);
		return intensity;
	}
	
	/**
	 * Passes intensities dataset and covariates to Pophet which does the forecasting.
	 * Aggregates the resulting intensities to one intensity value.
	 * @param datesOfIntensities
	 * @param intensitiesOfUserGroup
	 * @param size
	 * @param re
	 * @return
	 */
	private int forecastWithProphet(ArrayList<String> datesOfIntensities, ArrayList<Double> intensitiesOfUserGroup, int size,
			Rengine re) {
	
		double[] intensities = intensitiesOfUserGroup.stream().mapToDouble(i -> i).toArray();
		String[] dates = new String[datesOfIntensities.size()];
		dates = datesOfIntensities.toArray(dates);
		
		String period = Integer.toString(size);
		
		re.assign("dates", dates);
		re.assign("intensities", intensities);
		re.assign("period", period);
		re.eval("source(\"prophet/ForecastProphet.R\")");
		
		double[] forecastedIntensities = re.eval("forecastValues").asDoubleArray();
		
		// TODO: Check other possibilities for workload aggregation. Information should be passed by user.
		double maxIntensity = 0;
		for(int i = 0; i < forecastedIntensities.length; i++) {
			if(forecastedIntensities[i] > maxIntensity) {
				maxIntensity = forecastedIntensities[i];
			}
		}
		
		int intensity = (int) Math.round(maxIntensity);

		return intensity;
	}
	
	/**
	 * Passes intensities dataset and covariates to Telescope which does the forecasting.
	 * Aggregates the resulting intensities to one intensity value.
	 * @param intensitiesOfUserGroup
	 * @return
	 */
	private int forecastWithTelescope(ArrayList<Double> intensitiesOfUserGroup, ArrayList<ArrayList<Double>> covariates, ArrayList<ArrayList<Double>> futureCovariates, Rengine re) {	
		if(covariates.size() > 0) {
			// hist.covar
			String matrixString = calculateMatrix("hist", covariates, re);
			re.assign("hist.covar.matrix", re.eval(matrixString));
			
			// future.covar
			String futureMatrixString = calculateMatrix("future", futureCovariates, re);
			re.assign("future.covar.matrix", re.eval(futureMatrixString));	
		}
		
		double[] intensities = intensitiesOfUserGroup.stream().mapToDouble(i -> i).toArray();
		
		String period = Integer.toString(futureCovariates.get(0).size());
		
		re.assign("intensities", intensities);
		re.assign("period", period);
		re.eval("source(\"telescope-multi/ForecastTelescope.R\")");
		re.eval("dev.off()");
		
		double[] forecastedIntensities = re.eval("forecastValues").asDoubleArray();
		
		// TODO: Check other possibilities for workload aggregation. Information should be passed by user.
		double maxIntensity = 0;
		for(int i = 0; i < forecastedIntensities.length; i++) {
			if(forecastedIntensities[i] > maxIntensity) {
				maxIntensity = forecastedIntensities[i];
			}
		}
		
		int intensity = (int) Math.round(maxIntensity);

		return intensity;
	}
	
	private String calculateMatrix(String string, ArrayList<ArrayList<Double>> covariates, Rengine re) {
		int x = 0;
		ArrayList<String> nameOfCovars = new ArrayList<String>();
		for(ArrayList<Double> covariateValues: covariates) {
			String name = string + ".covar" + x;
			double[] occurrences = covariateValues.stream().mapToDouble(i -> i).toArray();
			re.assign(name, occurrences);
			x++;
			nameOfCovars.add(name);
		}
		
		String matrixString = "cbind(";
		
		boolean isFirst = true;
		for(String name: nameOfCovars) {
			if(isFirst) {
				matrixString += name;
				isFirst = false;
			} else {
				matrixString += "," + name;
			}
		}
		matrixString += ")";
		return matrixString;
	}
	
	private long calculateInterval(String interval) {
		long numericInterval = 0;
		switch(interval) {
		   case "secondly":
		      numericInterval = 1000L;
		      break;		      
		   case "minutely":
			  numericInterval = 60000L;
			  break;		   
		   case "hourly":
		      numericInterval = 3600000L;
		      break;
		   default: 
			  numericInterval = 1000L;
		}
		return numericInterval;
	}

	/**
	 * Converts a milliseconds timestamp to UTC date as string.
	 * @param timestamp
	 * @return
	 */
	private String convertTimestampToUtcDate(long timestamp) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
		String date = Instant.ofEpochMilli(timestamp).atOffset(ZoneOffset.UTC).format(dtf).toString();
		return date;
	}

	/**
	 * Gets continuous measurements from database.
	 * @param covariateValues
	 * @param covar
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	private ArrayList<Double> getContinuousMeasurements(ContinuousData contCovar, String startTime, String endTime) {
		ArrayList<Double> measurements = new ArrayList<Double>();
		String measurementName = contCovar.getLocationName();
		String queryString = "SELECT time, value FROM " + measurementName + " WHERE time >= '" + startTime + "' AND time <= '" + endTime 
				+"'";
		Query query = new Query(queryString, tag);
		QueryResult queryResult = influxDb.query(query);
		for (Result result : queryResult.getResults()) {
			for (Series serie : result.getSeries()) {
				for (List<Object> listTuples : serie.getValues()) {
					double measurement = (double) listTuples.get(1);
					measurements.add(measurement);
				}
			} 
		}
		return measurements;
	}
	
	/**
	 * Processes covariate. Gets relevant covariate data from database.
	 * @param covar
	 * @param startTime
	 * @param endTime
	 */
	private ArrayList<Long> getCovariateData(Event eventCovar, String startTime, String endTime) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		ArrayList<Long> timestamps = new ArrayList<Long>();
		String queryString = null; 
		String value = eventCovar.getCovar();
		String measurementName = eventCovar.getLocationName();
		queryString = "SELECT time, value FROM " + measurementName + " WHERE time >= '" + startTime + "' AND time <= '" + endTime 
			+"' AND value = '" + value + "'";	
		Query query = new Query(queryString, tag);
		QueryResult queryResult = influxDb.query(query);
		for (Result result : queryResult.getResults()) {
			for (Series serie : result.getSeries()) {
				for (List<Object> listTuples : serie.getValues()) {
					long time = 0;
					try {
						time = sdf.parse((String) listTuples.get(0)).getTime();
					} catch (ParseException e) {
						
					}
					timestamps.add(time);
				}
			}
		}	
		return timestamps;
	}

	/**
	 * Gets the intensities of user group from database.
	 * @param userGroupId
	 * @return
	 */
	public Pair<ArrayList<Long>, ArrayList<Double>> getIntensitiesForUserGroupFromDatabaseTelescope(int userGroupId) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		Pair<ArrayList<Long>, ArrayList<Double>> timestampsAndIntensities = null;
		ArrayList<Long> timestamps = new ArrayList<Long>();
		ArrayList<Double> intensities = new ArrayList<Double>();
		String measurementName = "userGroup" + userGroupId;
		Query query = new Query("SELECT time, value FROM " + measurementName, tag);
		QueryResult queryResult = influxDb.query(query);
		for (Result result : queryResult.getResults()) {
			for (Series serie : result.getSeries()) {
				for (List<Object> listTuples : serie.getValues()) {
					long time = 0;
					try {
						time = sdf.parse((String) listTuples.get(0)).getTime();
					} catch (ParseException e) {
						
					}
					double intensity = (double) listTuples.get(1);
					timestamps.add(time);
					intensities.add(intensity);
				}
			}
		}	
		timestampsAndIntensities = new Pair<>(timestamps, intensities);
		return timestampsAndIntensities;
	}
	
	/**
	 * Gets the intensities of user group from database.
	 * @param userGroupId
	 * @return
	 */
	public Pair<ArrayList<String>, ArrayList<Double>> getIntensitiesForUserGroupFromDatabaseProphet(int userGroupId) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf2.setTimeZone(TimeZone.getDefault());
		
		Pair<ArrayList<String>, ArrayList<Double>> timestampsAndIntensities = null;
		ArrayList<String> dates = new ArrayList<String>();
		ArrayList<Double> intensities = new ArrayList<Double>();
		String measurementName = "userGroup" + userGroupId;
		Query query = new Query("SELECT time, value FROM " + measurementName, tag);
		QueryResult queryResult = influxDb.query(query);
		for (Result result : queryResult.getResults()) {
			for (Series serie : result.getSeries()) {
				for (List<Object> listTuples : serie.getValues()) {
					Date utcDate = null;
					try {
						utcDate = sdf.parse((String) listTuples.get(0));
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					String date = sdf2.format(utcDate);
					double intensity = (double) listTuples.get(1);
					dates.add(date);
					intensities.add(intensity);
				}
			}
		}	
		timestampsAndIntensities = new Pair<>(dates, intensities);
		return timestampsAndIntensities;
	}

	/**
	 * Initializes Telescope.
	 * @param re
	 */
	private void initializeTelescope(Rengine re) {
		re.eval("source(\"telescope-multi/R/telescope.R\")");
		re.eval("source(\"telescope-multi/R/cluster_periods.R\")");
		re.eval("source(\"telescope-multi/R/detect_anoms.R\")");
		re.eval("source(\"telescope-multi/R/fitting_models.R\")");
		re.eval("source(\"telescope-multi/R/frequency.R\")");
		re.eval("source(\"telescope-multi/R/outlier.R\")");
		re.eval("source(\"telescope-multi/R/telescope_Utils.R\")");
		re.eval("source(\"telescope-multi/R/vec_anom_detection.R\")");
		re.eval("source(\"telescope-multi/R/xgb.R\")");
		
		re.eval("library(xgboost)");
		re.eval("library(cluster)");
		re.eval("library(forecast)");
		re.eval("library(e1071)");		
	}
	
	/**
	 * Initializes Prophet.
	 * TODO
	 * @param re
	 */
	private void initializeProphet(Rengine re) {
		re.eval("library(prophet)");	
	}

	/**
	 * Initializes Rengine.
	 * @return
	 */
	private Rengine initializeRengine() {
		String newargs1[] = {"--no-save"};

        Rengine re = Rengine.getMainEngine();
        if (re == null) {
        	re = new Rengine(newargs1, false, null);
        } 
		re.eval(".libPaths('win-library/3.5')");
		return re;
	}	
}
