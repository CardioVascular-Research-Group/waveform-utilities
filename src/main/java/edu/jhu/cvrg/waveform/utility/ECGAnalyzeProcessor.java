package edu.jhu.cvrg.waveform.utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import edu.jhu.cvrg.analysis.vo.AnalysisResultType;
import edu.jhu.cvrg.analysis.vo.AnalysisType;
import edu.jhu.cvrg.analysis.vo.AnalysisVO;
import edu.jhu.cvrg.analysis.wrapper.AnalysisWrapper;
import edu.jhu.cvrg.filestore.exception.FSException;
import edu.jhu.cvrg.filestore.main.FileStorer;
import edu.jhu.cvrg.filestore.model.FSFile;
import edu.jhu.cvrg.timeseriesstore.exceptions.OpenTSDBException;
import edu.jhu.cvrg.timeseriesstore.opentsdb.TimeSeriesRetriever;
import edu.jhu.cvrg.waveform.exception.AnalyzeFailureException;
import edu.jhu.icm.ecgFormatConverter.ECGFileData;
import edu.jhu.icm.ecgFormatConverter.ECGFormatWriter;
import edu.jhu.icm.enums.DataFileFormat;

public class ECGAnalyzeProcessor {
	
	private static Logger log = Logger.getLogger(ECGAnalyzeProcessor.class);

	public static final String SERVER_TEMP_ANALYSIS_FOLDER = ServiceProperties.getInstance().getProperty(ServiceProperties.TEMP_FOLDER)+"/a";
	
	public static Map<Long, String> execute(int channels, String leadNames, double scalingFactor, int samplesPerChannel, float samplingRate, String timeseriesId,  Map<String, Object> commandParamMap, FileStorer fstorer, AnalysisVO analysis) throws AnalyzeFailureException{
		
		Map<Long, String> sentFiles = null;
		try {
			String outputFolder = SERVER_TEMP_ANALYSIS_FOLDER + File.separator + analysis.getJobId() + File.separator;
			File outputDirectory = new File(outputFolder);
			outputDirectory.mkdir();
			
			ECGFileData fileData = new ECGFileData();
			fileData.channels = channels;
			fileData.leadNames = leadNames;
			fileData.scalingFactor = scalingFactor;
			fileData.samplesPerChannel = samplesPerChannel;
			fileData.samplingRate = samplingRate;
			
			ECGAnalyzeProcessor.retrieveTimeSeries(fileData, timeseriesId, commandParamMap);
			 
			createWFDBFile(commandParamMap.get("subjectID").toString(), fileData, outputFolder);
			
			List<String> inputFileNames = new ArrayList<String>();
			
			for (File f : outputDirectory.listFiles()) {	
				inputFileNames.add(f.getAbsolutePath());
			}
			
			analysis.setFileNames(inputFileNames);
			
			try {
				AnalysisWrapper algorithm = analysis.getType().getWrapper().getConstructor(AnalysisVO.class).newInstance(analysis);
				algorithm.defineInputParameters();
				algorithm.execute();
				
			} catch (Exception ex) {
				throw new AnalyzeFailureException("Unable to execute the analysis algorithm.", ex);
				
			}

			//save files in liferay
			sentFiles = new HashMap<Long, String>();
			for (String outFileStr : analysis.getOutputFileNames()) {
				File orign = new File(outFileStr);
				FileInputStream fis = new FileInputStream(orign);
				int fileSize = Long.valueOf(orign.length()).intValue();
				long fileId = 0;
				
				if(fstorer != null){
					byte[] bytes = new byte[fileSize];
					fis.read(bytes);
					fis.close();
					
					FSFile fsFile = fstorer.addFile(Long.parseLong(commandParamMap.get("folderID").toString()), orign.getName(), bytes, false);
					
					fileId = fsFile.getId();
					
				}else{
					long groupId = Long.parseLong(commandParamMap.get("groupID").toString());
					long folderId = Long.parseLong(commandParamMap.get("folderID").toString());
					long userId = Long.parseLong(commandParamMap.get("userID").toString());
					
					fileId = ServiceUtils.sendToLiferay(groupId, folderId, userId, outputFolder, orign.getName(), fileSize, fis);
				}
				
				sentFiles.put(fileId, orign.getName());
			}
			
			//register those files in database
			
			
		} catch (NumberFormatException e) {
			throw new AnalyzeFailureException("Unable to save the result file(s) in Waveform.", e);
		} catch (FileNotFoundException e) {
			throw new AnalyzeFailureException("Unable to save the result file(s) in Waveform.", e);
		} catch (IOException e) {
			throw new AnalyzeFailureException("Unable to save the result file(s) in Waveform.", e);
		} catch (FSException e) {
			throw new AnalyzeFailureException("Unable to save the result file(s) in Waveform.", e);
		} catch (OpenTSDBException e) {
			throw new AnalyzeFailureException("Unable to retrieve the timeseries data.", e);
		}
		
		return sentFiles;
		
	}
	
	
	private static void retrieveTimeSeries(ECGFileData fileData, String timeseriesId,  Map<String, Object> map) throws OpenTSDBException {
		
		String host = ResourceUtility.getOpenTsdbHost();
		
		if(host == null){
			host = map.get("openTsdbHost").toString();
		}
		
		final String OPENTSDB_URL = "http://"+host+":4242";
		HashMap<String, String> tags = new HashMap<String, String>();
		
		Calendar zeroTime = new GregorianCalendar(2015, Calendar.JANUARY, 1);
		zeroTime.setTimeZone(TimeZone.getTimeZone("US/Eastern"));
		
		final long zeroTimeInMillis = zeroTime.getTimeInMillis(); 
		
		tags.put("timeseriesid", timeseriesId);
		
		Map<String, JSONObject> dataMap= new HashMap<String, JSONObject>();
				
		String[] leadNames = fileData.leadNames.split(",");
		
		int duration = (fileData.samplesPerChannel/(int)fileData.samplingRate)*1000;
		
		for (int channel = 0; channel < leadNames.length; channel++) {
			String leadName = leadNames[channel];
			
			try {
				JSONObject jsonPoints = TimeSeriesRetriever.retrieveTimeSeries(OPENTSDB_URL, zeroTimeInMillis, zeroTimeInMillis+(duration*1000), "ecg."+leadName+".uv", tags);
				if(jsonPoints != null){
					JSONObject data = jsonPoints.getJSONObject("dps");
					dataMap.put(leadName, data);	
				}else{
					throw new OpenTSDBException("Unable to get timeseries.");	
				}
			} catch (JSONException e) {
				throw new OpenTSDBException("Unable to get timeseries.", e);
			}
		}
		log.info("OpenTSDB retrieve data from timeseriesId: "+ timeseriesId);
		
		try {
			int[][] segmentData = new int[fileData.channels][fileData.samplesPerChannel];
			
			for(int ch = 0; ch < leadNames.length; ch++) {
				JSONObject leadData = dataMap.get(leadNames[ch]);
				Iterator iterator = leadData.sortedKeys();
				for (int sample =  0; sample < fileData.samplesPerChannel;sample++){
					if(iterator.hasNext()) {
						String key = (String) iterator.next();
						segmentData[ch][sample] = (leadData.getInt(key));
					}
				}
			}
			
			fileData.data = segmentData;
			
		} catch (Exception e) {
			throw new OpenTSDBException("Unable to parse the timeseries data.", e);
		}
		
	}
	
	private static void createWFDBFile(String subjectId, ECGFileData ecgFile, String outputFolder){
		ECGFormatWriter writer = new ECGFormatWriter();
		writer.writeToFile(DataFileFormat.WFDB, outputFolder, subjectId, ecgFile);
	}
	
	public static void main(String[] args) {
		System.out.println("Analysis start");
		long startTime = System.currentTimeMillis();
		
		Map<String, Object> commandMap = new HashMap<String, Object>();
		
		commandMap.put("userID",  "10405");
		commandMap.put("groupID", "10179");
		commandMap.put("folderID", "744203");
		commandMap.put("subjectID", "ecg_97082511_1");
		commandMap.put("durationSec", "11.0");
		commandMap.put("parameterlist", null);
		
		commandMap.put("method", "chesnokovWrapperType2");
		commandMap.put("serviceName", "physionetAnalysisService");
		commandMap.put("URL", "http://localhost:8080/axis2/services");
		
		String jobID = "job_" + "1";
		
		commandMap.put("jobID", jobID);

		AnalysisVO analysis = new AnalysisVO(commandMap.get("jobID").toString(), AnalysisType.getTypeByOmeName(commandMap.get("method").toString()), AnalysisResultType.ORIGINAL_FILE, null, commandMap);
		
		try {
			ECGAnalyzeProcessor.execute(12, "I,II,III,aVR,aVL,aVF,V1,V2,V3,V4,V5,V6", 200, 5500, 500, "4dc1d0f5-1d18-44af-a4f1-dede7d6c4074", commandMap, null, analysis);
			System.out.println("Analysis End. Total time: "+(System.currentTimeMillis()-startTime)+" ms");
		} catch (AnalyzeFailureException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
}
