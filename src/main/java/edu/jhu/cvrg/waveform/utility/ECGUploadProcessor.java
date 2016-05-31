package edu.jhu.cvrg.waveform.utility;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.log4j.Logger;

import edu.jhu.cvrg.annotations.processors.AnnotationsProcessor;
import edu.jhu.cvrg.annotations.processors.MuseAnnotationsProcessor;
import edu.jhu.cvrg.annotations.processors.Philips103AnnotationsProcessor;
import edu.jhu.cvrg.annotations.processors.Philips104AnnotationsProcessor;
import edu.jhu.cvrg.annotations.processors.SchillerAnnotationsProcessor;
import edu.jhu.cvrg.data.dto.AnnotationDTO;
import edu.jhu.cvrg.data.enums.FileExtension;
import edu.jhu.cvrg.data.enums.FileType;
import edu.jhu.cvrg.data.enums.UploadState;
import edu.jhu.cvrg.data.factory.Connection;
import edu.jhu.cvrg.data.factory.ConnectionFactory;
import edu.jhu.cvrg.data.util.DataStorageException;
import edu.jhu.cvrg.timeseriesstore.exceptions.OpenTSDBException;
import edu.jhu.cvrg.timeseriesstore.model.IncomingDataPoint;
import edu.jhu.cvrg.timeseriesstore.opentsdb.TimeSeriesStorer;
import edu.jhu.cvrg.waveform.exception.DataExtractException;
import edu.jhu.cvrg.waveform.model.ECGFileMeta;
import edu.jhu.icm.ecgFormatConverter.ECGFileData;
import edu.jhu.icm.ecgFormatConverter.ECGFormatReader;
import edu.jhu.icm.ecgFormatConverter.muse.MuseXMLECGFileData;
import edu.jhu.icm.ecgFormatConverter.philips.Philips103ECGFileData;
import edu.jhu.icm.ecgFormatConverter.philips.Philips104ECGFileData;
import edu.jhu.icm.ecgFormatConverter.schiller.SchillerECGFileData;
import edu.jhu.icm.enums.DataFileFormat;
import edu.jhu.icm.enums.LeadEnum;

/**
 * 
 * Class to handle the Upload logic, to be used on any layer PORTLET or WEBSERVICE  
 * 
 * @author avilard4
 *
 */
public class ECGUploadProcessor {

	private static Logger log = Logger.getLogger(ECGUploadProcessor.class);
	private static Map<String, Map<String, String>> ontologyCache = new HashMap<String, Map<String,String>>();
	
	public void execute(ECGFileMeta ecgFile, String openTsdbHost)  throws DataExtractException {
		
		ECGFileData fileData = null;
		Connection dbUtility = null;
		
		String timeseriesId = UUID.randomUUID().toString();
		String message = null;
		long readTime = java.lang.System.currentTimeMillis();
		
		try{
			dbUtility = ConnectionFactory.createConnection();
			
			
			ECGFormatReader reader = new ECGFormatReader();
			
			switch (FileExtension.valueOf(ecgFile.getFile().getExtension().toUpperCase())) {
			case HEA:
				if(ecgFile.getAuxiliarFiles().size() > 1){
					fileData = reader.read(DataFileFormat.values()[ecgFile.getFileType().ordinal()], ecgFile.getAuxiliarFiles().get(FileExtension.DAT).getFileDataAsInputStream(), ecgFile.getFile().getFileDataAsInputStream() /*, ecgFile.getAuxiliarFiles().get(EnumFileExtension.XYZ).getFileDataAsInputStream()*/, ecgFile.getRecordName());
					
				}else{
					fileData = reader.read(DataFileFormat.values()[ecgFile.getFileType().ordinal()], ecgFile.getAuxiliarFiles().get(FileExtension.DAT).getFileDataAsInputStream(), ecgFile.getFile().getFileDataAsInputStream(), ecgFile.getRecordName());
				}
				break;
			case DAT:
				if(ecgFile.getAuxiliarFiles().size() > 1){
					fileData = reader.read(DataFileFormat.values()[ecgFile.getFileType().ordinal()], ecgFile.getFile().getFileDataAsInputStream(), ecgFile.getAuxiliarFiles().get(FileExtension.HEA).getFileDataAsInputStream() /*, ecgFile.getAuxiliarFiles().get(EnumFileExtension.XYZ).getFileDataAsInputStream()*/, ecgFile.getRecordName());
				}else{
					fileData = reader.read(DataFileFormat.values()[ecgFile.getFileType().ordinal()], ecgFile.getFile().getFileDataAsInputStream(), ecgFile.getAuxiliarFiles().get(FileExtension.HEA).getFileDataAsInputStream(), ecgFile.getRecordName());
				}
				break;
			case XYZ:
				if(ecgFile.getAuxiliarFiles().size() > 1){
					fileData = reader.read(DataFileFormat.values()[ecgFile.getFileType().ordinal()], ecgFile.getAuxiliarFiles().get(FileExtension.DAT).getFileDataAsInputStream(), ecgFile.getAuxiliarFiles().get(FileExtension.HEA).getFileDataAsInputStream() /*, ecgFile.getFile().getFileDataAsInputStream()*/, ecgFile.getRecordName());
				}else{
					fileData = reader.read(DataFileFormat.values()[ecgFile.getFileType().ordinal()], ecgFile.getAuxiliarFiles().get(FileExtension.DAT).getFileDataAsInputStream(), ecgFile.getAuxiliarFiles().get(FileExtension.HEA).getFileDataAsInputStream(), ecgFile.getRecordName());
				}
				break;
			default:
				fileData = reader.read(DataFileFormat.values()[ecgFile.getFileType().ordinal()], ecgFile.getFile().getFileDataAsInputStream());
				break;
			}
			
			
			ecgFile.setSampFrequency(fileData.samplingRate);
			ecgFile.setChannels(fileData.channels);
			ecgFile.setNumberOfPoints(fileData.samplesPerChannel * fileData.channels);
	
		}catch(Exception e){
			message = e.getMessage();
			e.printStackTrace();
		}finally{
			readTime = java.lang.System.currentTimeMillis() - readTime;
			log.info("["+ecgFile.getDocumentId()+"]The runtime for reading the file(" + ecgFile.getRecordName() + ") is = " + readTime + " milliseconds");
			try{
				dbUtility.updateUploadStatus(ecgFile.getDocumentId(), UploadState.TRANSFER_READ, readTime, null, message);
			}catch(Exception e){
				e.printStackTrace();
				throw new DataExtractException("Error in status update, on read.");
			}
		}
			
		boolean noConversionErrors = false;
		
		long writeTime = java.lang.System.currentTimeMillis();
		
		Boolean done = !(FileType.PHILIPS_103.equals(ecgFile.getFileType()) || FileType.PHILIPS_104.equals(ecgFile.getFileType())  || FileType.SCHILLER.equals(ecgFile.getFileType()) || FileType.MUSE_XML.equals(ecgFile.getFileType()));
		
		try{
			
			this.storeTimeSeries(ecgFile, fileData, timeseriesId, openTsdbHost);
		
//			ECGFormatWriter writer = new ECGFormatWriter();
//			String outputPath = ServiceProperties.getInstance().getProperty(ServiceProperties.TEMP_FOLDER)+File.separator+'c'+File.separator+userId+File.separator;
//			
//			File outputDir = writer.writeToFile(DataFileFormat.WFDB_16, outputPath, ecgFile.getRecordName(), fileData);
//			
//			log.info(" +++++ Conversion completed successfully, results will be transfered.");
//			
//			if(outputDir != null && outputDir.exists() && outputDir.isDirectory()){
//				for (File outFile : outputDir.listFiles()) {
//					if(outFile.getName().startsWith(ecgFile.getRecordName())){
//						ECGFileMeta metaFile = new ECGFileMeta(ecgFile.getSubjectID(), ecgFile.getRecordName(), null, null);
//						
//						byte[] outFileData = new byte[Long.valueOf(outFile.length()).intValue()];
//						FileInputStream fis = new FileInputStream(outFile);
//						fis.read(outFileData);
//						fis.close();
//						
//						metaFile.setFile(new FSFile(-1, outFile.getName(), outFile.getName(), ecgFile.getFile().getParentId(), outFileData, outFile.length()));
//						
//						saveFile(metaFile, ecgFile.getFile().getParentId(), fileStorer);
//						
//						outFile.delete();
//					}
//				}
//			}
//			tranferFileToLiferay(outputFormat, inputFormat, metaData.getFileName(), inputPath, groupId, folderId, docId, userId);
			
			noConversionErrors = true;
			dbUtility.updateDocument(ecgFile.getDocumentId(), Double.valueOf(ecgFile.getSampFrequency()).doubleValue(), ecgFile.getChannels(), ecgFile.getNumberOfPoints(), ecgFile.getSubjectAge(), ecgFile.getSubjectSex(), null, fileData.scalingFactor, fileData.leadNames, Long.valueOf(timeseriesId));
			
		}catch (Exception e){
			message = e.getMessage();
			if(e.getCause() != null && !e.getCause().equals(e)){
				message+=(" casued by: "+ e.getCause().getMessage());
			}
			e.printStackTrace();
		}finally{
			writeTime = java.lang.System.currentTimeMillis() - writeTime;
			log.info("["+ecgFile.getDocumentId()+"]The runtime for writing the new file(" + ecgFile.getRecordName() + ") is = " + writeTime + " milliseconds");
			
			Boolean status = null;
			if(!noConversionErrors){
				status = Boolean.FALSE;
				throw new DataExtractException(message);
			}else if(done){
				status = Boolean.TRUE;
			}
			
			try{
				dbUtility.updateUploadStatus(ecgFile.getDocumentId(), (noConversionErrors) ? UploadState.WRITE : null, writeTime, status, message);
			}catch(Exception e){
				throw new DataExtractException("Error in status update, on write.");
			}
		}
		
		
		if(noConversionErrors){
			
			long  annotationTime = java.lang.System.currentTimeMillis();
			message = null;
			try{
				Map<String, String> nonLeadList = null;
				Map<Integer, Map<String, String>> leadList = null;
				AnnotationsProcessor processor = null;
				
				if(FileType.PHILIPS_103.equals(ecgFile.getFileType())) {
					
					processor = new Philips103AnnotationsProcessor(((Philips103ECGFileData) fileData).restingecgdata);
				
				}else if(FileType.PHILIPS_104.equals(ecgFile.getFileType())) {
					
					processor = new Philips104AnnotationsProcessor(((Philips104ECGFileData) fileData).restingecgdata);
					
				}else if(FileType.SCHILLER.equals(ecgFile.getFileType())) {
					                                               
					processor = new SchillerAnnotationsProcessor(((SchillerECGFileData) fileData).schillerEDI);
					
				}else if(FileType.MUSE_XML.equals(ecgFile.getFileType())) {
					String rawMuseXML = ((MuseXMLECGFileData) fileData).museRawXML;
					if(rawMuseXML != null) {
						processor = new MuseAnnotationsProcessor(rawMuseXML, ecgFile.getDocumentId(), ecgFile.getUserId());
					}
				}
				
				
				if(processor != null){
					processor.processAll();
					
					nonLeadList = processor.getGlobalAnnotations();
					leadList = processor.getLeadAnnotations();
					
					Set<AnnotationDTO> annotationsDTO = new HashSet<AnnotationDTO>();
					
					annotationsDTO.addAll(convertLeadAnnotations(leadList, processor, fileData.leadNames, ecgFile.getDocumentId(), ecgFile.getUserId()));
					annotationsDTO.addAll(convertNonLeadAnnotations(nonLeadList, processor, ecgFile.getDocumentId(), ecgFile.getUserId()));
				
					commitAnnotations(annotationsDTO);
				}
			}catch (Exception e){
				message = e.getMessage();
				e.printStackTrace();
			}finally{
				annotationTime = java.lang.System.currentTimeMillis() - annotationTime;
				log.info("["+ecgFile.getDocumentId()+"]The runtime for analyse annotation and entering it into the database is = " + annotationTime + " milliseconds");
				try{
					dbUtility.updateUploadStatus(ecgFile.getDocumentId(), UploadState.ANNOTATION, annotationTime, Boolean.TRUE, message);
				}catch(Exception e){
					throw new DataExtractException("Error in status update, on annotation.");
				}
				
			}
		}
		
	}

	private void storeTimeSeries(ECGFileMeta ecgFile, ECGFileData fileData, String timeseriesId, String openTsdbHost) throws OpenTSDBException {
		
		
		String OPENTSDB_URL = "http://"+openTsdbHost+":4242";
		HashMap<String, String> tags = new HashMap<String, String>();
		
		Calendar zeroTime = new GregorianCalendar(2015, Calendar.JANUARY, 1);
		zeroTime.setTimeZone(TimeZone.getTimeZone("US/Eastern"));
		
		final long zeroTimeInMillis = zeroTime.getTimeInMillis(); 
		
		tags.put("timeseriesid", timeseriesId);
		
		long timeGapBetweenPoints = 1000L/Float.valueOf(ecgFile.getSampFrequency()).longValue() * 1000; 
		
		List<IncomingDataPoint> points = new ArrayList<IncomingDataPoint>();
		
		String[] leadNames = fileData.leadNames.split(",");
		
		for (int channel = 0; channel < leadNames.length; channel++) {
			String leadName = leadNames[channel];
				
			for (int sample = 0; sample < fileData.data[channel].length; sample++) {
				String value = Integer.valueOf(fileData.data[channel][sample]).toString();
				points.add(new IncomingDataPoint("ecg."+leadName+".uv", zeroTimeInMillis + (timeGapBetweenPoints * sample), value, tags));
			}
		
			TimeSeriesStorer.storeTimePoints(OPENTSDB_URL, points);
			points.clear();
		}
		
		log.info("OpenTSDB timeseriesId: "+ timeseriesId);
	}
	
	private boolean commitAnnotations(Set<AnnotationDTO> annotationSet) {
		boolean success = true;

		if(annotationSet != null && annotationSet.size() > 0){
			try {
				success = annotationSet.size() == ConnectionFactory.createConnection().storeAnnotations(annotationSet);
			} catch (DataStorageException e) {
				log.error("Error on Annotation persistence. " + e.getMessage());
				e.printStackTrace();
				success = false;
			}
		}
				
		return success;
	}	

	private Set<AnnotationDTO> convertLeadAnnotations(Map<Integer, Map<String, String>> leadAnnotations, AnnotationsProcessor processor, String leadNames, Long docId, Long userId) {
		Set<AnnotationDTO> leadAnnotationSet = new HashSet<AnnotationDTO>();
		String[] leads = null;
		if(leadNames != null){
			leads = leadNames.split(",");
		}
		
		for (Integer key : leadAnnotations.keySet()) {
			Map<String, String> lead = leadAnnotations.get(key);
			
			Integer leadIndex = key;
			LeadEnum l = LeadEnum.values()[key];
			
			if(leads != null){
				for (int i = 0; i < leads.length; i++) {
					if(l.name().equals(leads[i])){
						leadIndex = i;
						break;
					}
				}
			}
			
			leadAnnotationSet.addAll(convertAnnotations(lead, leadIndex, processor, docId, userId));
		}
		return leadAnnotationSet;
	}
	
	private Set<AnnotationDTO>  convertNonLeadAnnotations(Map<String, String> allAnnotations, AnnotationsProcessor processor, Long docId, Long userId){
		return convertAnnotations(allAnnotations, null, processor, docId, userId);
	}
	
	private Set<AnnotationDTO>  convertAnnotations(Map<String, String> annotationArray, Integer leadIndex, AnnotationsProcessor processor, Long docId, Long userId) {
		
		AnnotationDTO ann = null;
		String type = null;
		
		if(leadIndex != null) {
			type = "ANNOTATION";
		}else {
			type = "COMMENT";
		}
		
		Set<AnnotationDTO> annotationSet = new HashSet<AnnotationDTO>();
		if(annotationArray != null && annotationArray.size() > 0){
			for(String name : annotationArray.keySet()) {
				
				
				String termName = name;
				String fullAnnotation = null;
				String bioportalOntology = null;
				String bioportalClassId = null;
				
				String bioportalReference = BioportalReferenceMap.lookup(name);
				
				if(bioportalReference != null){
					
					if(bioportalReference.startsWith("ECGTermsv1")){
						bioportalOntology = AnnotationDTO.ECG_TERMS_ONTOLOGY;
					}else if(bioportalReference.startsWith("ECGOntology")){
						bioportalOntology = AnnotationDTO.ELECTROCARDIOGRAPHY_ONTOLOGY;
					}
					
					if(bioportalOntology != null){
						Map<String, String> saOntDetails = ontologyCache.get(bioportalReference);
						if(saOntDetails == null){
							saOntDetails = WebServiceUtility.lookupOntology(bioportalOntology, bioportalReference, "definition", "prefLabel", "@id");
							ontologyCache.put(bioportalReference, saOntDetails);
						}
						 
						termName = "Not found";
						fullAnnotation = "Not found";
						
						if(saOntDetails != null){
							termName = saOntDetails.get("prefLabel");
							fullAnnotation = saOntDetails.get("definition");
							bioportalClassId = saOntDetails.get("@id");
						}
					}
				}
				
				ann = new AnnotationDTO(null, docId, processor.getName(), type, termName, 
										bioportalOntology, 
										bioportalClassId, null /*will be generate at constructor*/,
									    leadIndex, null/*unit*/, fullAnnotation, annotationArray.get(name), new GregorianCalendar(), 
									    null, null, 
									    null, null);
				 
				annotationSet.add(ann);
			}
		}
				
		return annotationSet;
	}
	
//	private void tranferFileToLiferay(DataFileFormat outputFormat, DataFileFormat inputFormat, String inputFilename, String inputPath, long groupId, long folderId, long docId, long userId) throws Exception{
		
//		String outputExt = ".dat";
//		if (outputFormat == DataFileFormat.RDT){ 
//			outputExt = ".rdt"; }
//		else if (outputFormat == DataFileFormat.GEMUSE) {
//			outputExt = ".txt";
//		}else if (outputFormat == DataFileFormat.HL7) {
//			outputExt = ".xml";
//		}
//
//		String outputFileName = inputFilename.substring(0, inputFilename.lastIndexOf(".")) + outputExt;
//
//		File orign = new File(inputPath + outputFileName);
//		FileInputStream fis = new FileInputStream(orign);
//		
//		Long fileId = ServiceUtils.sendToLiferay(groupId, folderId, userId, inputPath, outputFileName, orign.length(), fis);
//		
//		String name = inputFilename.substring(0, inputFilename.lastIndexOf(".")); // file name minus extension.
//
//		File heaFile = new File(inputPath + name + ".hea");
//		if (inputFormat != DataFileFormat.WFDB && heaFile.exists()) {
//			orign = new File(inputPath + heaFile.getName().substring(heaFile.getName().lastIndexOf(sep) + 1));
//			fis = new FileInputStream(orign);
//			
//			filesId = new long[2];
//			filesId[0] = fileId;
//			
//			fileId = ServiceUtils.sendToLiferay(groupId, folderId, userId, inputPath, heaFile.getName().substring(heaFile.getName().lastIndexOf(sep) + 1), orign.length(), fis);
//			filesId[1] = fileId;
//		
//		}else{
//			filesId = new long[1];
//			filesId[0] = fileId;
//		}
//		
//		dbUtility.storeFilesInfo(docId, filesId, null);
			
//	}
	
}
