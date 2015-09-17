package edu.jhu.cvrg.waveform.utility;
/*
Copyright 2011, 2013 Johns Hopkins University Institute for Computational Medicine

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
//Portions of the code (isUnix() method) are used from http://www.mkyong.com/java/how-to-detect-os-in-java-systemgetpropertyosname/
/**
* @author Mike Shipway, Chris Jurado
*/
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.log4j.Logger;

public class ServerUtility {

	public Map<String, Object> buildParamMap(OMElement param0){
		String key;
		String sValue = null;
		Object value = null;
		Map<String, Object> paramMap = new HashMap<String, Object>();
		@SuppressWarnings("unchecked")
		Iterator<OMElement> iterator = param0.getChildren();
		while(iterator.hasNext()) {
			OMElement param = iterator.next();
			key = param.getLocalName();
			sValue = param.getText();
			value = (sValue.length() > 0) ? sValue : param;
			paramMap.put(key,value);
		}
		return paramMap;
	}

	public String[] buildChildArray(OMElement param0){
		ArrayList<String> childList = new ArrayList<String>();
		@SuppressWarnings("unchecked")
		Iterator<OMElement> iterator = param0.getChildren();
		while(iterator.hasNext()) {
			OMElement param = iterator.next();
			childList.add(param.getText());
		}
		String[] ret = new String[childList.size()];
		ret = childList.toArray(ret);
		return ret;
	}

	 public ServiceClient getSender(String sServiceURL, String sServiceName, String sMethod){
		 EndpointReference targetEPR = new EndpointReference(sServiceURL + "/" + sServiceName + "/" + sMethod);		 
		 return getSender(targetEPR, sServiceURL);
	 }

	 public ServiceClient getSender(String sServiceNameURL, String sMethod){
		 EndpointReference targetEPR = new EndpointReference(sServiceNameURL + "/" + sMethod);
		 return getSender(targetEPR, sServiceNameURL);
	 }

	 public ServiceClient getSender(EndpointReference targetEPR, String brokerURL){
			Options options = new Options();
			options.setTo(targetEPR);
			options.setProperty(HTTPConstants.SO_TIMEOUT,new Integer(18000000));
			options.setProperty(HTTPConstants.CONNECTION_TIMEOUT,new Integer(18000000));
			options.setTransportInProtocol(Constants.TRANSPORT_HTTP);
			options.setAction(brokerURL);
			ServiceClient sender = null;
			try {
				sender = new ServiceClient();
			} catch (AxisFault e) {
				e.printStackTrace();
			}
			sender.setOptions(options);
			return sender;
	 }

	 public static String guessLeadName(int iLeadNum, int iLeadCount){
		 iLeadNum --;
		 String[] sa2Names = {"II","MCL1"};
		 String[] sa15Names = {"I","II","III","aVR","aVL","aVF","V1","V2","V3","V4","V5","V6","VX","VY","VZ"};
		 String sLeadName = (iLeadCount > 2) ? sa15Names[iLeadNum] : sa2Names[iLeadNum];
		 return sLeadName;
	 }

	public BufferedReader stdInputBuffer = null;

	public static boolean isUnix() {
		String OS = System.getProperty("os.name").toLowerCase();
		return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0 ); 
	}

	public boolean executeCommand(String sCommand, String[] asEnvVar, String sWorkingDir){
		boolean bRet = true;	
		
		try {
			File fWorkingDir = new File(sWorkingDir); //converts the dir name to File for exec command.
			Runtime rt = Runtime.getRuntime();
			Process process = rt.exec(sCommand, asEnvVar, fWorkingDir);
			InputStream is = process.getInputStream();  // The input stream for this method comes from the output from rt.exec()
			InputStreamReader isr = new InputStreamReader(is);
			stdInputBuffer = new BufferedReader(isr);
		} catch (IOException ioe) {
			System.err.println("++ IOException Message: executeCommand(" + sCommand + ")" + ioe.getMessage());
			ioe.printStackTrace();
			bRet = false;
		} catch (Exception e) {
			System.err.println("++ Exception Message: executeCommand(" + sCommand + ")" + e.getMessage());
			e.printStackTrace();
			bRet = false;
		}
		return bRet;
	}

	public void stdReturnHandler(String outputFilename) throws IOException{
	    String line;
		// Create file 
		FileWriter fstream = new FileWriter(outputFilename);
		BufferedWriter bwOut = new BufferedWriter(fstream);
	    while ((line = stdInputBuffer.readLine()) != null) {
	    	bwOut.write(line);
	    	bwOut.newLine();
	    }
		bwOut.flush();
		bwOut.close();
	}
	
	public static void logStackTrace(Exception e, Logger log){
    	int lines = e.getStackTrace().length;
    	for (int i = 0; i < lines; i++) {
			log.error(e.getStackTrace()[i]);
		}
    }
}