package edu.jhu.cvrg.waveform.utility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
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
	public boolean verbose = false;
	public ServerUtility(boolean b) {
		verbose = b;
	}

	/** Parses a service's incoming XML and builds a Map of all the parameters for easy access.
	 * @param param0 - OMElement representing XML with the incoming parameters.
	 */
	public Map<String, Object> buildParamMap(OMElement param0){
		debugPrintln("buildParamMap()");

		String key;
		String sValue = null;
		Object value=null;
		Map<String, Object> paramMap = new HashMap<String, Object>();
		try {
			@SuppressWarnings("unchecked")
			Iterator<OMElement> iterator = param0.getChildren();

			while(iterator.hasNext()) {
				OMElement param = iterator.next();
				key = param.getLocalName();
				sValue = param.getText();
				if(sValue.length()>0){
					value = sValue;
				}else{
					value = param;
				}

				paramMap.put(key,value);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return paramMap;
	}

	/** Parses a list node from the service's incoming XML and builds a Map of all its children for easy access.
	 * Used for parameters list and also file handle list.
	 * 
	 * @param param0 - OMElement representing XML with the incoming parameters.
	 */
	public String[] buildChildArray(OMElement param0){
		debugPrintln("buildChildArray()");

		ArrayList<String> childList = new ArrayList<String>();

		try {
			@SuppressWarnings("unchecked")
			Iterator<OMElement> iterator = param0.getChildren();

			while(iterator.hasNext()) {
				OMElement param = iterator.next();
				childList.add(param.getText());

				debugPrintln(" -- childList.add(v): " + param.getText());
			}
		} catch (Exception e) {
			e.printStackTrace();
			//				errorMessage = "buildParamMap() failed.";
			return null;
		}

		String[] ret = new String[childList.size()];
		ret = childList.toArray(ret);

		return ret;
	}
	
	/** Creates a sender with the standard options. 
	 * @param sServiceURL - the URL of the web service to be called. e.g. "http://icmv058.icm.jhu.edu:8080/axis2/services"
	 * @param sServiceName - Name of the web service. e.g. "physionetAnalysisService"
	 * @param sMethod - Method of the service which implements the analysis. e.g. "sqrsWrapperType2"
	 **/
	 public ServiceClient getSender(String sServiceURL, String sServiceName, String sMethod){
		 EndpointReference targetEPR = new EndpointReference(sServiceURL + "/" + sServiceName + "/" + sMethod);
		 
		 return getSender(targetEPR, sServiceURL);
	 }
	 

	/** Creates a sender with the standard options. 
	 * @param targetEPR - Target End Point Reference
	 * @param brokerURL - the URL of the web service to be called.
	 **/
	 public ServiceClient getSender(String sServiceNameURL, String sMethod){
		 EndpointReference targetEPR = new EndpointReference(sServiceNameURL + "/" + sMethod);
		 
		 return getSender(targetEPR, sServiceNameURL);
	 }
	 
	/** Creates a sender with the standard options. 
	 * @param targetEPR - Target End Point Reference
	 * @param brokerURL - the URL of the web service to be called.
	 **/
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			sender.setOptions(options);
			
			return sender;
	 }


	 /** Return our current best guess of the name of a lead <BR>
	  * based on the total lead count and the zero based lead number.<BR>
	  * Holter lead names are not yet know, so are not included.
	  * 
	  * @param iLeadNum - Zero based lead number, e.g. first lead in a data set(array) is number zero.
	  * @param iLeadCount - total number of leads in the data set.
	  * @return - one of "I","II","III","aVR","aVL","aVF","V1","V2","V3","V4","V5", or "V6" ("VX","VY","VZ")
	  */
	 public String guessLeadName(int iLeadNum, int iLeadCount){
		 String sLeadName = "uk";
		 String[] sa3Names = {"I","II","III"};
		 String[] sa12Names = {"I","II","III","aVR","aVL","aVF","V1","V2","V3","V4","V5","V6"};
		 String[] sa15Names = {"I","II","III","aVR","aVL","aVF","V1","V2","V3","V4","V5","V6","VX","VY","VZ"};
		 
		 if(iLeadCount==3){
			 sLeadName = sa3Names[iLeadNum];
		 }
		 if(iLeadCount==12){
			 sLeadName = sa12Names[iLeadNum];
		 }
		 if(iLeadCount==15){
			 sLeadName = sa15Names[iLeadNum];
		 }
		 
		 return sLeadName;
	 }
	 
	 //****************** Local server specific methods ********************************
	public BufferedReader stdInputBuffer = null;
	private BufferedReader stdError = null;

	/** debug utility method, only prints if verbose is true.
	 * 
	 * @param out - String to be printed
	 */
	public void debugPrint(String out){
		Date now = new Date();
		String sNow = now.toString();
		if (verbose) System.out.print(out + " Date: " + sNow);
	}

	/** debug utility method, only println (with line end) if verbose is true.
	 * 
	 * @param out - String to be printed
	 */
	public void debugPrintln(String out){
		if (verbose) System.out.println(out);
	}


	/** detects if Java is running on a Unix type platform.<BR>
	 * Copied from larger utility class at: http://www.mkyong.com/java/how-to-detect-os-in-java-systemgetpropertyosname/
	 * @author Mike Shipway
	 * @return true if OS is Linux or various other Unixoids.
	 */
	public static boolean isUnix() {
		String OS = System.getProperty("os.name").toLowerCase();
		return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0 ); 
	}
	
	/** Executes the command and pipes the response and errors to stdInputBuffer and stdError respectively.
	 * 
	 * @param sCommand - a specified system command.
	 * @param asEnvVar - array of strings, each element of which has environment variable settings in format name=value.
	 * @param sWorkingDir - the working directory of the subprocess, or null if the subprocess should inherit the working directory of the current process. 
	 * @return 
	 */
	public boolean executeCommand(String sCommand, String[] asEnvVar, String sWorkingDir){
		debugPrintln("++ executeCommand(" + sCommand );
		debugPrintln("++ , asEnvVar[" + asEnvVar.length + "]");
		debugPrintln("++, " + sWorkingDir + ")");
		boolean bRet = true;	
		
		try {
			File fWorkingDir = new File(sWorkingDir); //converts the dir name to File for exec command.
			Runtime rt = Runtime.getRuntime();
			Process process = rt.exec(sCommand, asEnvVar, fWorkingDir);
			InputStream is = process.getInputStream();  // The input stream for this method comes from the output from rt.exec()
			InputStreamReader isr = new InputStreamReader(is);
			stdInputBuffer = new BufferedReader(isr);
			
			InputStream errs = process.getErrorStream();
			InputStreamReader esr = new InputStreamReader(errs);
			stdError = new BufferedReader(esr);
		} catch (IOException ioe) {
			System.err.println("++ IOException Message: executeCommand(" + sCommand + ")" + ioe.getMessage());
			ioe.printStackTrace();
			bRet = false;
		} catch (Exception e) {
			System.err.println("++ Exception Message: executeCommand(" + sCommand + ")" + e.getMessage());
			e.printStackTrace();
			bRet = false;
		}
		debugPrintln("++ returning: " + bRet);
		return bRet;
	}

	/** This writes the output to the standard output if verbose is true
	 * 
	 * @throws IOException
	 */	
	
	public void stdReturnHandler() throws IOException{
	    String line;
		
	    int lineNum = 0;
	    debugPrintln("Here is the returned text of the command (if any):");
	    while ((line = stdInputBuffer.readLine()) != null) {
	    	debugPrintln(lineNum + ")" + line);
	    	lineNum++;
	    }
	}
	
	/** This writes the output of the execution to a file instead of standard output
	 * 
	 * @param outputFilename
	 * @throws IOException
	 */
	public void stdReturnHandler(String outputFilename) throws IOException{
	    String line;

		try{
			// Create file 
			debugPrintln("stdReturnHandler(FName) Creating output file: " + outputFilename);
			FileWriter fstream = new FileWriter(outputFilename);
			BufferedWriter bwOut = new BufferedWriter(fstream);

			int lineNum = 0;
		    debugPrintln("Here is the returned text of the command (if any): \"");
		    while ((line = stdInputBuffer.readLine()) != null) {
		    	bwOut.write(line);
		    	bwOut.newLine();
		    	if (lineNum<10){
		    		debugPrintln(lineNum + ")" + line);
		    	}else{
//				    debugPrint(".");
		    	}
		    	lineNum++;
		    }
		    debugPrintln(". . . ");
		    debugPrintln(lineNum + ")" + line);
	        debugPrintln("\"");
			bwOut.flush();
			//Close the output stream
			bwOut.close();
		}catch (Exception e){//Catch exception if any
		   System.err.println("Error: " + e.getMessage());
		}
	}

	
	/** This function prints messages resulting from runtime problems to the system standard error
	 * @return Boolean variable:  True if there are no errors, false if there are errors.
	 * 
	 * @throws IOException
	 */	
	public boolean stdErrorHandler() throws IOException{
		boolean bRet = true;
		String error;
	    int lineNum = 0;

	    // read any errors from the attempted command
	    debugPrintln("");
	    debugPrintln("Here is the standard error of the command (if any): \"");
        while ((error = stdError.readLine()) != null) {
            System.err.println(lineNum + ">" + error);
            lineNum++;

			bRet = false;
        }
        debugPrintln("\"");
		return bRet;
	}
	
	public static void logStackTrace(Exception e, Logger log){
    	
    	int lines = e.getStackTrace().length;
    	
    	if(lines > e.getStackTrace().length){
    		lines = e.getStackTrace().length;
    	}
    	
    	for (int i = 0; i < lines; i++) {
			log.error(e.getStackTrace()[i]);
		}
    }
	
}