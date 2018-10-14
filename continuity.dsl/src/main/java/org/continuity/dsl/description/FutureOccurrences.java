package org.continuity.dsl.description;

import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.math3.util.Pair;

public class FutureOccurrences {

	private ArrayList<Long> singleTimestamps;
	private ArrayList<Pair<Long, Long>> rangeTimestamps;
	
	@SuppressWarnings("deprecation")
	public FutureOccurrences(ArrayList<String> futureDates) {
		singleTimestamps = new ArrayList<Long>();
		rangeTimestamps = new ArrayList<Pair<Long, Long>>();
		String delims = "to";
		
		for(String date: futureDates) {
			String[] tokens = date.split(delims);
			if(tokens.length == 1) {
				long timestamp = Date.parse(tokens[0]);
				singleTimestamps.add(timestamp);
			} else if (tokens.length == 2) {
				long timestampFrom = Date.parse(tokens[0]);
				long timestampTo = Date.parse(tokens[1]);
				Pair<Long, Long> rangeTimestamp = new Pair<>(timestampFrom, timestampTo);
				rangeTimestamps.add(rangeTimestamp);
			} else {
				System.out.println("Invalid context input!");
			}
		}
	}
	
	public ArrayList<Long> getSingleTimestamps() {
		return singleTimestamps;
	}

	public void setSingleTimestamps(ArrayList<Long> singleTimestamps) {
		this.singleTimestamps = singleTimestamps;
	}

	public ArrayList<Pair<Long, Long>> getRangeTimestamps() {
		return rangeTimestamps;
	}

	public void setRangeTimestamps(ArrayList<Pair<Long, Long>> rangeTimestamps) {
		this.rangeTimestamps = rangeTimestamps;
	}

	@Override
	public String toString() {
		return "FutureOccurrences [singleTimestamps=" + singleTimestamps + ", rangeTimestamps=" + rangeTimestamps + "]";
	}
}
