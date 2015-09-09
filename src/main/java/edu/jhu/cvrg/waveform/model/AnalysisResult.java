package edu.jhu.cvrg.waveform.model;
/*
Copyright 2015 Johns Hopkins University Institute for Computational Medicine

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
* @author bbenite1, sgranite, Chris Jurado
*/
import java.io.Serializable;

public class AnalysisResult implements Serializable{

	private static final long serialVersionUID = 7855236073013121477L;
	public String userID = "";
	public String subjectID = "";
	public String dateOfAnalysis = "";
	public String recordName = "";
	public String fileName = "";
	public String algorithmUsed = "";
	public String displayName = "";
	
	public AnalysisResult(String fileURI) {
		this.fileName = fileURI;
	}
	
	public AnalysisResult() {
		
	}
	
	public String toString() {
		String printout = "Analysis Result object:  \n" +
							"userID = " + userID + "\n" +
							"subjectID = " + subjectID + "\n" +
							"date of analysis = " + dateOfAnalysis + "\n" +
							"record name = " + recordName + "\n" +
							"file name = " + fileName + "\n" +
							"algorithm used = " + algorithmUsed + "\n";
		return printout;
	}
}