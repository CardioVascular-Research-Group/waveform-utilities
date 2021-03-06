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

import edu.jhu.cvrg.timeseriesstore.exceptions.OpenTSDBException;
import edu.jhu.cvrg.timeseriesstore.opentsdb.TimeSeriesRetriever;
import edu.jhu.cvrg.waveform.exception.VisualizeFailureException;
import edu.jhu.cvrg.waveform.model.VisualizationData;

/**
 * 
 * Class to handle the Visualize logic, to be used on any layer PORTLET or WEBSERVICE  
 * 
 * @author avilard4
 *
 */
public class ECGVisualizeProcessor {

	private static Logger log = Logger.getLogger(ECGVisualizeProcessor.class);
	
	/**
	 * Retreive timeseries from OPENTSDB
	 * 
	 * 
	 * @param duration
	 * @param leadNames
	 * @param timeseriesId
	 * @param openTsdbHost
	 * @return
	 * @throws OpenTSDBException
	 */
	private static Map<String, JSONObject> retrieveTimeSeries(long duration, String[] leadNames, String timeseriesId, String openTsdbHost) throws OpenTSDBException{
		
		final String OPENTSDB_URL = "http://"+openTsdbHost+":4242";
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
				if(jsonPoints != null){
					JSONObject data = jsonPoints.getJSONObject("dps");
					returnMap.put(leadName, data);	
				}else{
					throw new OpenTSDBException("Timeseries data not found");
				}
			} catch (JSONException e) {
				throw new OpenTSDBException("Timeseries data not found", e);
			}
		}
		log.info("OpenTSDB retrieve data from timeseriesId: "+ timeseriesId);
		
		return returnMap;
	}
	
	/**
	 * Fetch the data segment to be load
	 *  
	 * @param openTsdbHost
	 * @param timeseriesId
	 * @param leadNames
	 * @param offsetMilliSeconds
	 * @param durationMilliSeconds
	 * @param graphWidthPixels
	 * @param skipSamples
	 * @param counts
	 * @param samplingRate
	 * @param adugain
	 * @return
	 * @throws VisualizeFailureException
	 */
	public static VisualizationData fetchDataSegment(String openTsdbHost, String timeseriesId, String[] leadNames, int offsetMilliSeconds, int durationMilliSeconds, int graphWidthPixels, boolean skipSamples, int counts, int samplingRate, double adugain) throws VisualizeFailureException{
		
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
			
			data = retrieveTimeSeries(durationMilliSeconds, leadNames, timeseriesId, openTsdbHost);
			
		} catch(Exception e1) {
			throw new VisualizeFailureException("Unable to retrieve timeseries data", e1);
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
			throw new VisualizeFailureException("Fail to prepare the response data", e);
		}
		//*******************************************
	
		return visualizationData;
	}
}
