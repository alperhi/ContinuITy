//package org.continuity.forecast.managers;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.TimeUnit;
//
//import org.apache.commons.lang3.Range;
//import org.continuity.api.entities.artifact.ForecastBundle;
//import org.continuity.api.entities.artifact.SessionsBundle;
//import org.continuity.api.entities.artifact.SessionsBundlePack;
//import org.continuity.api.entities.artifact.SimplifiedSession;
//import org.continuity.commons.utils.WebUtils;
//import org.continuity.dsl.description.Context;
//import org.influxdb.BatchOptions;
//import org.influxdb.InfluxDB;
//import org.influxdb.dto.Point;
//import org.rosuda.JRI.Rengine;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.web.client.RestClientException;
//import org.springframework.web.client.RestTemplate;
//
//
///**
// * Manager for the workload forecasting.
// * @author Alper Hidiroglu
// *
// */
//public class OldForecastPipelineManager {
//
//	private static final Logger LOGGER = LoggerFactory.getLogger(OldForecastPipelineManager.class);
//
//	private RestTemplate restTemplate;
//	
//	private InfluxDB influxDb;
//	
//	private String tag;
//	
//	private Context context;
//	
//	private int workloadIntensity;
//
//	public int getWorkloadIntensity() {
//		return workloadIntensity;
//	}
//
//	public void setWorkloadIntensity(int workloadIntensity) {
//		this.workloadIntensity = workloadIntensity;
//	}
//
//	/**
//	 * Constructor.
//	 */
//	public OldForecastPipelineManager(RestTemplate restTemplate, InfluxDB influxDb, String tag, Context context) {
//		this.restTemplate = restTemplate;
//		this.influxDb = influxDb;
//		this.tag = tag;
//		this.context = context;
//	}
//	
//	@SuppressWarnings("deprecation")
//	public void setupDatabase(){
//		String dbName = this.tag;
//		if (!influxDb.describeDatabases().contains(dbName)) {
//			influxDb.createDatabase(dbName);
//		}
//		influxDb.setDatabase(dbName);
//		influxDb.setRetentionPolicy("autogen");
//	}
//
//	/**
//	 * Runs the pipeline.
//	 *
//	 * @return The generated forecast bundle.
//	 */
//	public ForecastBundle runPipeline(String linkToSessions) {
//		setupDatabase();
//		SessionsBundlePack sessionsBundles;
//		try {
//			sessionsBundles = restTemplate.getForObject(WebUtils.addProtocolIfMissing(linkToSessions), SessionsBundlePack.class);
//		} catch (RestClientException e) {
//			LOGGER.error("Error when retrieving sessions!", e);
//			return null;
//		}
//		influxDb.enableBatch(BatchOptions.DEFAULTS);
//		ForecastBundle forecastBundle;
//		try {
//			forecastBundle = generateForecastBundle(sessionsBundles);
//		} catch (Exception e) {
//			LOGGER.error("Could not create a Forecast!", e);
//			forecastBundle = null;
//		}
//		influxDb.disableBatch();
//		return forecastBundle;
//	}
//	
//	/**
//	 * Generates the forecast bundle.
//	 * @param logs
//	 * @return
//	 * @throws IOException 
//	 * @throws ExtractionException 
//	 * @throws ParseException 
//	 */
//	private ForecastBundle generateForecastBundle(SessionsBundlePack sessionsBundles) throws IOException {
//		// initialize intensity
//		this.workloadIntensity = 1;
//		List<SessionsBundle> bundleList = sessionsBundles.getSessionsBundles();
//		// updates also the workload intensity 
//		LinkedList<Double> probabilities = forecastWorkload(bundleList);
//		// forecast result
//		return new ForecastBundle(sessionsBundles.getTimestamp(), this.workloadIntensity, probabilities);
//	}
//	
//	/**
//	 * Returns aggregated workload intensity and adapted behavior mix probabilities.
//	 * @param bundleList
//	 * @return
//	 */
//	private LinkedList<Double> forecastWorkload(List<SessionsBundle> bundleList) {
//		Rengine re = initializeRengine();
//		// ToDo: decision whether to take telescope or prophet
//		initializeTelescope(re);
//		
//		LinkedList<Double> probabilities = new LinkedList<Double>();
//		int sumOfIntensities = 0;
//		List<Integer> intensities = new LinkedList<Integer>();
//		for (SessionsBundle sessBundle : bundleList) {
//			List<SimplifiedSession> sessions = sessBundle.getSessions();
//			int behaviorId = sessBundle.getBehaviorId();
//			int intensity = forecastIntensityForUserGroup(sessions, behaviorId, re);
//			intensities.add(intensity);
//		    sumOfIntensities += intensity;
//		}
//		re.end();
//		// updates the workload intensity
//		setWorkloadIntensity(sumOfIntensities);
//		for(int intensity: intensities) {
//			double probability = (double) intensity / (double) sumOfIntensities;
//			probabilities.add(probability);
//		}
//		return probabilities;
//	}
//
//	/**
//	 * Forecasts the workload intensity for a user group. Returns one intensity value.
//	 * Timestamps are in nanoseconds.
//	 * @param sessions
//	 * @return
//	 */
//	private int forecastIntensityForUserGroup(List<SimplifiedSession> sessions, int behaviorId, Rengine re) {
//		sortSessions(sessions);
//		long startTime = sessions.get(0).getStartTime();
//		
//		// The time range for which an intensity will be calculated
//		long rangeLength = calculateInterval(context.getForecastOptions().getInterval());
//		
//		// rounds start time down
//		long roundedStartTime = startTime - startTime % rangeLength;
//		
//		long highestEndTime = 0;
//		
//		for(SimplifiedSession session: sessions) {
//			if(session.getEndTime() > highestEndTime) {
//				highestEndTime = session.getEndTime();
//			}
//		}
//		// rounds highest end time up
//		long roundedHighestEndTime = highestEndTime;
//		if (highestEndTime % rangeLength != 0) {
//			roundedHighestEndTime = (highestEndTime - highestEndTime % rangeLength) + rangeLength;
//		}
//		
//		long completePeriod = roundedHighestEndTime - roundedStartTime;
//		long amountOfRanges = completePeriod / rangeLength;
//		
//		ArrayList<Range<Long>> listOfRanges = calculateRanges(roundedStartTime, amountOfRanges, rangeLength);
//		
//		// Remove first and last range from list if necessary
//		if(listOfRanges.get(0).getMinimum() != startTime) {
//			listOfRanges.remove(0);
//		}
//		
//		if(listOfRanges.get(listOfRanges.size() - 1).getMaximum() != highestEndTime) {
//			listOfRanges.remove(listOfRanges.size() - 1);
//		}
//		
//		// This array will be passed to forecaster in order to do the forecasting
//		int[] intensitiesOfUserGroup = new int[listOfRanges.size()];
//		int i = 0;
//		
//		// This map is used to hold necessary information which will be saved into DB
//		HashMap<Long, Integer> intensities = new HashMap<Long, Integer>();
//		
//		for(Range<Long> range: listOfRanges) {
//			ArrayList<SimplifiedSession> sessionsInRange = new ArrayList<SimplifiedSession>();
//			for(SimplifiedSession session: sessions) {
//				Range<Long> sessionRange = Range.between(session.getStartTime(), session.getEndTime());
//				if(sessionRange.containsRange(range) || range.contains(session.getStartTime()) 
//						|| range.contains(session.getEndTime())) {
//					sessionsInRange.add(session);
//				}
//			}
//			int intensityOfRange = (int) calculateIntensityForRange(range, sessionsInRange, rangeLength);
//			
//			intensities.put(range.getMinimum(), intensityOfRange);
//			
//			intensitiesOfUserGroup[i] = intensityOfRange;
//			i++;
//		}
//		
//		saveIntensitiesOfUserGroupIntoDb(intensities, behaviorId);	
//		
//		return forecast(intensitiesOfUserGroup, re);
//	}
//
//	/**
//	 * Saves intensities into InfluxDB
//	 * @param intensities
//	 */
//	@SuppressWarnings("rawtypes")
//	private void saveIntensitiesOfUserGroupIntoDb(HashMap<Long, Integer> intensities, int behaviorId) {
//		String measurementName = "user-group-" + behaviorId;
//		Iterator iterator = intensities.entrySet().iterator();
//	    while (iterator.hasNext()) {
//			Map.Entry pair = (Map.Entry)iterator.next();
//			Point point = Point.measurement(measurementName)
//					.time((long) pair.getKey(), TimeUnit.NANOSECONDS)
//				    .addField("value", (int) pair.getValue()) 
//					.build();
//		
//			influxDb.write(point);
//	        iterator.remove(); 
//	    }
//	}
//
//	private long calculateInterval(String interval) {
//		long numericInterval = 0;
//		switch(interval) {
//		   case "secondly":
//		      numericInterval = 1000000000L;
//		      break;		      
//		   case "minutely":
//			  numericInterval = 60000000000L;
//			  break;		   
//		   case "hourly":
//		      numericInterval = 3600000000000L;
//		      break;
//		   default: 
//			  numericInterval = 1000000000L;
//		}
//		return numericInterval;
//	}
//
//	/**
//	 * Calculates the time ranges.
//	 * @param startTime
//	 * @param amountOfRanges
//	 * @param rangeLength
//	 * @return
//	 */
//	private ArrayList<Range<Long>> calculateRanges(long startTime, long amountOfRanges, long rangeLength ) {
//		ArrayList<Range<Long>> listOfRanges = new ArrayList<Range<Long>>();
//		for(int i = 0; i < amountOfRanges; i++) {
//			Range<Long> range = Range.between(startTime, startTime + rangeLength);
//			listOfRanges.add(range);
//			startTime += rangeLength;
//		}
//		return listOfRanges;
//	}
//
//	/**
//	 * Calculates the workload intensity for a time range. Calculates average, min and max.
//	 * @param range
//	 * @param sessionsInRange
//	 * @param rangeLength
//	 * @return
//	 */
//	private long calculateIntensityForRange(Range<Long> range, ArrayList<SimplifiedSession> sessionsInRange, long rangeLength) {
//		int counter = 0;
//		long sumOfTime = 0;
//		boolean inTimeRange = true;
//		long endOfRange = range.getMaximum();
//		// smallest found timestamp
//		long lastOccurredEvent = range.getMinimum();
//		
//		// initialize the counter with amount of sessions at the beginning of the range
//		for(SimplifiedSession session: sessionsInRange) {
//			Range<Long> sessionRange = Range.between(session.getStartTime(), session.getEndTime());
//			if(sessionRange.contains(lastOccurredEvent)) {
//				counter++;
//			}
//		}	
//		// min value of range
//		int minCounter = counter;
//		// max value of range
//		int maxCounter = counter;
//		
//		// 2n (Start und Endzeitpunkt - while Schleife) * n (Elemente - innere for Schleife)
//		// + n* (wegen der Abbruchbedingung, es wird über alle Sessions drüber iteriert, aber nichts gemacht)
//		// = 2n^2 + n
//		// Bei 2 versetzten Sessions sind es 10 Durchläufe im Rumpf der inneren for Schleife,
//		// while Schleife wird für 2 Sessions 5 mal durchlaufen (genau genommen ist die while 2n + 1*)
//		// (2n + 1) * n = 2n^2 + n, also O Notation O(n^2)
//		while(inTimeRange) {
//			long minValue = Long.MAX_VALUE;
//			int currentCounter = counter;
//			for(SimplifiedSession session: sessionsInRange) {
//				long startTimeOfSession = session.getStartTime();
//				long endTimeOfSession = session.getEndTime();
//				if(startTimeOfSession > lastOccurredEvent) {
//					if(startTimeOfSession == minValue) {
//						currentCounter ++;
//					} else if (startTimeOfSession < minValue){
//						currentCounter = counter + 1;
//						minValue = startTimeOfSession;
//					}
//				} else if(endTimeOfSession > lastOccurredEvent) {
//					if(endTimeOfSession == minValue) {
//						currentCounter --;
//					} else if (endTimeOfSession < minValue) {
//						currentCounter = counter - 1;
//						minValue = endTimeOfSession;
//					}
//				}
//			} 
//			if(minValue > endOfRange) {
//				minValue = endOfRange;
//				inTimeRange = false;
//			}
//			sumOfTime += counter * (minValue - lastOccurredEvent);
//			lastOccurredEvent = minValue;
//			
//			counter = currentCounter;
//			
//			if(counter < minCounter) {
//				minCounter = counter;
//			} 
//			if(counter > maxCounter) {
//				maxCounter = counter;
//			}		
//		}
//		return sumOfTime / rangeLength;
//	}
//	
//	/**
//	 * Passes intensities dataset to Telescope which does the forecasting.
//	 * Aggregates the resulting intensities to one intensity value.
//	 * @param intensitiesOfUserGroup
//	 * @return
//	 */
//	private int forecast(int[] intensitiesOfUserGroup, Rengine re) {	
//		double[] intensities = Arrays.stream(intensitiesOfUserGroup).asDoubleStream().toArray();
//		
//		String period = context.getForecastOptions().getForecastPeriod();
//		
//		re.assign("intensities", intensities);
//		re.assign("period", period);
//		re.eval("source(\"telescope-multi/ForecastingIntensities.R\")");
//		re.eval("dev.off()");
//		
//		double[] forecastedIntensities = re.eval("forecastValues").asDoubleArray();
//		
//		// ToDo: Check other possibilities for workload aggregation. Information should be passed by user.
//		double maxIntensity = 0;
//		for(int i = 0; i < forecastedIntensities.length; i++) {
//			if(forecastedIntensities[i] > maxIntensity) {
//				maxIntensity = forecastedIntensities[i];
//			}
//		}
//		
//		int intensity = (int) Math.round(maxIntensity);
//
//		return intensity;
//	}
//
//	/**
//	 * Sorts sessions.
//	 * @param sessions
//	 */
//	private void sortSessions(List<SimplifiedSession> sessions) {
//		sessions.sort((SimplifiedSession sess1, SimplifiedSession sess2) -> {
//			   if (sess1.getStartTime() > sess2.getStartTime())
//			     return 1;
//			   if (sess1.getStartTime() < sess2.getStartTime())
//			     return -1;
//			   return 0;
//			});
//	}
//	
//	/**
//	 * Initializes Telescope.
//	 * @param re
//	 */
//	private void initializeTelescope(Rengine re) {
//		re.eval("source(\"telescope-multi/R/telescope.R\")");
//		re.eval("source(\"telescope-multi/R/cluster_periods.R\")");
//		re.eval("source(\"telescope-multi/R/detect_anoms.R\")");
//		re.eval("source(\"telescope-multi/R/fitting_models.R\")");
//		re.eval("source(\"telescope-multi/R/frequency.R\")");
//		re.eval("source(\"telescope-multi/R/outlier.R\")");
//		re.eval("source(\"telescope-multi/R/telescope_Utils.R\")");
//		re.eval("source(\"telescope-multi/R/vec_anom_detection.R\")");
//		re.eval("source(\"telescope-multi/R/xgb.R\")");
//		
//		re.eval("library(xgboost)");
//		re.eval("library(cluster)");
//		re.eval("library(forecast)");
//		re.eval("library(e1071)");		
//	}
//	
//	/**
//	 * Initializes Prophet.
//	 * ToDo
//	 * @param re
//	 */
//	private void initializeProphet(Rengine re) {
//			
//	}
//
//	/**
//	 * Initializes Rengine.
//	 * @return
//	 */
//	private Rengine initializeRengine() {
//		String newargs1[] = {"--no-save"};
//
//        Rengine re = Rengine.getMainEngine();
//        if (re == null) {
//        	re = new Rengine(newargs1, false, null);
//        } 
//		re.eval(".libPaths('win-library/3.5')");
//		return re;
//	}	
//}
