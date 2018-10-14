package org.continuity.forecast.managers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.util.Pair;
import org.continuity.api.entities.artifact.ForecastBundle;
import org.continuity.dsl.description.Context;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Manager for the workload forecasting.
 * @author Alper Hidiroglu
 *
 */
public class ForecastPipelineManager {

	// private static final Logger LOGGER = LoggerFactory.getLogger(ForecastPipelineManager.class);
	
	private InfluxDB influxDb;
	
	private String tag;
	
	private Context context;
	
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
	public ForecastPipelineManager(InfluxDB influxDb, String tag, Context context) {
		this.influxDb = influxDb;
		this.tag = tag;
		this.context = context;
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
		// TODO: decision whether to take telescope or prophet
		initializeTelescope(re);
		
		LinkedList<Double> probabilities = new LinkedList<Double>();
		int sumOfIntensities = 0;
		List<Integer> forecastedIntensities = new LinkedList<Integer>();
		
		for(int i = 0; i < amountOfUserGroups; i++) {
			int intensity = forecastIntensityForUserGroup(i, re);
			forecastedIntensities.add(intensity);
			sumOfIntensities += intensity;
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
	 * Forecasts the workload for each user group.
	 * @param i
	 * @param re
	 * @return
	 */
	private int forecastIntensityForUserGroup(int i, Rengine re) {
		Pair<ArrayList<Long>, ArrayList<Double>> timestampsAndIntensities = getIntensitiesForUserGroupFromDatabase(i);
		ArrayList<Long> timestamps = timestampsAndIntensities.getKey();
		ArrayList<Double> intensities = timestampsAndIntensities.getValue();
		Long startTime = timestamps.get(0);
		Long endTimeIntensities = timestamps.get(timestamps.size() - 1);
		
		long endTimeForecast = this.context.getForecastOptions().getForecastPeriod();
		
		IntensitiesPipelineManager intManager = new IntensitiesPipelineManager();
		long interval = intManager.calculateInterval(this.context.getForecastOptions().getInterval());
		
		long startTimeForecast = endTimeIntensities + interval;
		long forecastTimestamp = startTimeForecast;
		long endTime = endTimeForecast * 1000000;
		ArrayList<Long> futureTimestamps = new ArrayList<Long>();
		while (forecastTimestamp <= endTime) {
			futureTimestamps.add(forecastTimestamp);
			forecastTimestamp += interval;
		}
		
		ArrayList<ArrayList<Double>> histCovariates = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> futureCovariates = new ArrayList<ArrayList<Double>>();
		if(context.getCovariates() != null) {
			for(Covariate covar: context.getCovariates()) {
				ArrayList<Double> historicalOccurrences = null;
				ArrayList<Double> futureOccurrences = null;
				if(covar instanceof ContinuousData) {
					historicalOccurrences = new ArrayList<Double>();
					ContinuousData contCovar = (ContinuousData) covar;
					
					historicalOccurrences = getContinuousMeasurements(contCovar, startTime, endTimeIntensities);
					futureOccurrences = getContinuousMeasurements(contCovar, startTimeForecast, endTimeForecast);
				} else {
					Event eventCovar = (Event) covar;
					// calculate historical occurrences
					historicalOccurrences = new ArrayList<Double>(Collections.nCopies(timestamps.size(), 0.0));
					ArrayList<Long> timestampsOfCovar = getCovariateData(eventCovar, startTime, endTimeIntensities);
					for(long timestamp: timestampsOfCovar) {
						int index = timestamps.indexOf(timestamp);
						historicalOccurrences.set(index, 1.0);
					}
					FutureOccurrences future = eventCovar.getFutureDates();
					ArrayList<Long> singleTimestamps = future.getSingleTimestamps();
					ArrayList<Pair<Long, Long>> rangeTimestamps = future.getRangeTimestamps();
					
					ArrayList<Long> futureTimestampsOfCovar = new ArrayList<Long>();
					for(long timestamp: singleTimestamps) {
						timestamp = timestamp * 1000000;
						futureTimestampsOfCovar.add(timestamp);
					}
					for(Pair<Long, Long> range: rangeTimestamps) {
						long timestampFrom = range.getKey() * 1000000;
						long timestampTo = range.getValue() * 1000000;
						while(timestampFrom <= timestampTo) {
							futureTimestampsOfCovar.add(timestampFrom);
							timestampFrom += interval;
						}
					}					
					
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
		int intensity = forecast(intensities, histCovariates, futureCovariates, re);
		return intensity;
	}

	/**
	 * Gets continuous measurements from database.
	 * @param covariateValues
	 * @param covar
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	private ArrayList<Double> getContinuousMeasurements(ContinuousData contCovar, Long startTime, Long endTime) {
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
	private ArrayList<Long> getCovariateData(Event eventCovar, long startTime, long endTime) {
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
					long time = (long) listTuples.get(0);
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
	public Pair<ArrayList<Long>, ArrayList<Double>> getIntensitiesForUserGroupFromDatabase(int userGroupId) {
		Pair<ArrayList<Long>, ArrayList<Double>> timestampsAndIntensities = null;
		ArrayList<Long> timestamps = new ArrayList<Long>();
		ArrayList<Double> intensities = new ArrayList<Double>();
		String measurementName = "userGroup" + userGroupId;
		Query query = new Query("SELECT time, value FROM " + measurementName, tag);
		QueryResult queryResult = influxDb.query(query);
		for (Result result : queryResult.getResults()) {
			for (Series serie : result.getSeries()) {
				for (List<Object> listTuples : serie.getValues()) {
					long time = (long) listTuples.get(0);
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
	 * Passes intensities dataset and covariates to Telescope which does the forecasting.
	 * Aggregates the resulting intensities to one intensity value.
	 * @param intensitiesOfUserGroup
	 * @return
	 */
	private int forecast(ArrayList<Double> intensitiesOfUserGroup, ArrayList<ArrayList<Double>> covariates, ArrayList<ArrayList<Double>> futureCovariates, Rengine re) {	
		// hist.covar
		String matrixString = calculateMatrix("hist", covariates, re);
		re.assign("hist.covar.matrix", re.eval(matrixString));
		
		// future.covar
		String futureMatrixString = calculateMatrix("future", futureCovariates, re);
		re.assign("future.covar.matrix", re.eval(futureMatrixString));	
		
		double[] intensities = intensitiesOfUserGroup.stream().mapToDouble(i -> i).toArray();
		
		String period = Integer.toString(futureCovariates.size());
		
		re.assign("intensities", intensities);
		re.assign("period", period);
		re.eval("source(\"telescope-multi/ForecastingIntensities.R\")");
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
