package edu.jhu.cvrg.waveform.utility;
/*
Copyright 2013 Johns Hopkins University Institute for Computational Medicine

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
/**
* @author Chris Jurado, Mike Shipway
* 
*/
import java.io.Serializable;

import edu.jhu.cvrg.data.dto.AdditionalParametersDTO;

public class AnalysisInProgress implements Serializable {

	private static final long serialVersionUID = -2120168072188806013L;
	public String jobId;
	public String subjectId;
	public String datasetName;
	public String userId;
	public String webServiceMethod;
	public String analysisType;
	public String serviceName;
	public String displayText;
	public String[] dataFileList;
	public String[] dataHandleList;
	public String[] resultHandleList;
	public String[] resultFileList;
	public String analysisServiceURL;
	public AdditionalParametersDTO[] parameterList;

	public String getFileCopyServiceURL() {
		String sURL="";
		int iIndexLastSlash = analysisServiceURL.lastIndexOf("/");		
		sURL = analysisServiceURL.substring(0,iIndexLastSlash+1) + "dataTransferService";
		return sURL;
	}
	
	public String getDataFilelistAsString(){
		String fileNameString = "";
		for(int f = 0; f<dataFileList.length; f++){
			fileNameString += dataFileList[f] + "^";
		}
		return fileNameString;
	}

	public String getResultFilelistAsString(){
		String fileNameString = "";
		for(int f = 0; f<resultHandleList.length; f++){
			fileNameString += resultHandleList[f] + "^";
		}
		return fileNameString;
	}

	public String getRelativePath(String sPath_Name){
		String sFilePath="";
		int iIndexLastSlash = sPath_Name.lastIndexOf("/");
		sFilePath = sPath_Name.substring(0,iIndexLastSlash + 1);	
		return sFilePath;
	}
}