package edu.jhu.cvrg.waveform.utility;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import edu.jhu.cvrg.timeseriesstore.opentsdb.TimeSeriesRetriever;
import edu.jhu.cvrg.waveform.model.VisualizationData;

public class ECGVisualizeProcessor {

	private static Logger log = Logger.getLogger(ECGVisualizeProcessor.class);
	
	
	private static Map<String, JSONObject> retrieveTimeSeries(long duration, String[] leadNames, String timeseriesId) {
		
		final String OPENTSDB_URL = "http://10.162.38.31:4242";
		HashMap<String, String> tags = new HashMap<String, String>();
		
		Calendar zeroTime = new GregorianCalendar(2015, Calendar.JANUARY, 1);
		zeroTime.setTimeZone(TimeZone.getTimeZone("US/Eastern"));
		
		final long zeroTimeInMillis = zeroTime.getTimeInMillis(); 
		
		tags.put("timeseriesid", timeseriesId);
		
		Map<String, JSONObject> returnMap= new HashMap<String, JSONObject>();
				
		for (int channel = 0; channel < leadNames.length; channel++) {
			String leadName = leadNames[channel];
			
			try {
				JSONObject jsonPoints = TimeSeriesRetriever.retrieveTimeSeries(OPENTSDB_URL, zeroTimeInMillis, zeroTimeInMillis+(duration*1000), "ecg."+leadName+".uv", tags);
				JSONObject data = jsonPoints.getJSONObject("dps");
				returnMap.put(leadName, data);
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		log.info("OpenTSDB retrieve data from timeseriesId: "+ timeseriesId);
		
		return returnMap;
	}
	
	
	public static VisualizationData fetchDataSegment(String timeseriesId, String[] leadNames, int offsetMilliSeconds, int durationMilliSeconds, int graphWidthPixels, boolean skipSamples, int counts, int samplingRate, double adugain) {
		
		VisualizationData visualizationData = new VisualizationData();
		
		double samplesPerPixel = 0, skippedSamples = 0;
		int  skippedSamplesInt = 0, segDurationInSamples = 0;
		float fmilliSecondPerSample=0;
		int segOffset = 0;
		int requestedMaxPoints = 0,availableSamples = 0,availablePoints = 0;
		int maxPoints = 0; // maximum data points that can be returned.
		
		float fRateMsec = 0;
		
		Map<String, JSONObject> data = null;
		try {
			
			
			fRateMsec = (float) (samplingRate/1000.0);
			fmilliSecondPerSample =  (float) (1000.0/((float)(samplingRate)));
			
			if (offsetMilliSeconds<0){
				offsetMilliSeconds=0; // cannot read before the beginning of the file.
			}
			segOffset = (int) (offsetMilliSeconds*fRateMsec);  // graph start position in number of samples from the start of record
	
			segDurationInSamples = (int) (fRateMsec*durationMilliSeconds);
			if(segDurationInSamples>graphWidthPixels && skipSamples){
				samplesPerPixel=(double)segDurationInSamples/graphWidthPixels;
				requestedMaxPoints = graphWidthPixels;
			}else{
				samplesPerPixel=1;
				requestedMaxPoints = segDurationInSamples;
			}
			skippedSamples = samplesPerPixel-1;
		    skippedSamplesInt = (int) skippedSamples; // number of samples to skip after each one returned. To adjust for graph resolution.
			availableSamples = counts - segOffset; // total number of remaining samples from this offset.
			availablePoints = availableSamples/(int)samplesPerPixel; // total number of graphable points from this offset.
			// ensure that the copying loop doesn't try to go past the end of the data file.
			if(availablePoints > requestedMaxPoints) {
				maxPoints = requestedMaxPoints;
			} else {  // Requested duration is longer than the remainder after the offset.
				if(segDurationInSamples < counts){ // Requested duration is less than the file contains.
					// move the offset back so the requested amount of samples can be returned.
					segOffset = counts - segDurationInSamples;
					maxPoints = requestedMaxPoints;
					offsetMilliSeconds = (int)(segOffset * fmilliSecondPerSample);
				}else{	// Requested duration is longer than the file contains.
					maxPoints = availablePoints;
				}
			}
			
			data = retrieveTimeSeries(durationMilliSeconds, leadNames, timeseriesId);
			
		} catch(Exception e1) {
			e1.printStackTrace();
		} 
	
		try {
			visualizationData.setECGDataLength(maxPoints);
			visualizationData.setECGDataLeads(leadNames.length);
			visualizationData.setOffset(segOffset);
			visualizationData.setSkippedSamples((int)skippedSamples);
			int msDuration = (int) ((counts*1000)/samplingRate);
			visualizationData.setMsDuration(msDuration);
	
			double[][] segmentData = new double[maxPoints][leadNames.length+1];
			double skipDecimals = skippedSamples-skippedSamplesInt;
			double skipDecimalsCumulative = 0;
			
			
			for(int ch = 0; ch < leadNames.length; ch++) {
				
				JSONObject leadData = data.get(leadNames[ch]);
				Iterator iterator = leadData.sortedKeys();
				for (int sample =  0; sample < maxPoints;){
					if(ch == 0){
						segmentData[sample][0] = offsetMilliSeconds + (fmilliSecondPerSample*sample); // time stamp in milliseconds
					}
					
					if(iterator.hasNext()) {
						String key = (String) iterator.next();
						segmentData[sample][ch+1] = (leadData.getInt(key) * 1000) / adugain;
					}
				
					skipDecimalsCumulative +=skipDecimals;
					
					sample = sample + 1 + skippedSamplesInt + (int)skipDecimalsCumulative;
					
					if(((int)skipDecimalsCumulative) > 0){
						skipDecimalsCumulative = skipDecimalsCumulative-(int)skipDecimalsCumulative;
					}
				}
			}
			
			visualizationData.setECGData(segmentData);
			//*******************************************
		} catch (Exception e) {
			e.printStackTrace();
		}
		//*******************************************
	
		return visualizationData;
	}
	
	
	
	public static void main(String[] args) throws JSONException {
		String[] labelNames = new String[]{"I", "II", "III"};
//		Map<String, JSONObject> data = p.retrieveTimeSeries(50L, labelNames, "5631cfd8-0ab8-4a02-9921-29979595716b");
//		
//		for (int i = 0; i < labelNames.length; i++) {
//			JSONObject leadData = data.get(labelNames[i]);
//			for (Iterator iterator = leadData.sortedKeys(); iterator.hasNext();) {
//				String key = (String) iterator.next();
//				System.out.print(leadData.getInt(key));
//				System.out.print(',');
//			}
//			System.out.println();
//		}
		
		VisualizationData visData = ECGVisualizeProcessor.fetchDataSegment("5631cfd8-0ab8-4a02-9921-29979595716b", labelNames, 0, 50, 1000, false, 82500, 500, 200.0);
		visData.setSaLeadName(new String[]{"time", "I", "II", "III"});
		System.out.println(visData.getECGDataSingleString()); 
	}
}
