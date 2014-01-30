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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.activation.DataHandler;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import org.apache.axiom.attachments.ByteArrayDataSource;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMText;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.transport.http.HTTPConstants;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.repository.model.FileEntry;

import edu.jhu.cvrg.waveform.callbacks.SvcAxisCallback;

public class WebServiceUtility {
	
	private WebServiceUtility(){}

	/** Generic function for calling web services on the same server (localhost) as the ECGWaveform web pages.
	 * 
	 * @param parameterMap - name, value pairs map for all the parameters(OMEChild) to be sent to the service.
	 * @param isPublic - unused
	 * @param serviceMethod - name of the specific method of the service to be called.
	 * @param serviceName - URL of machine the web service runs from.
	 * @param callback - function to which the service will return result XML to. Calls service without callback if null.
	 * @param filesMap - name, file pairs map for all the files(OMEChild) to be sent to the service.
	 * @return
	 */
	public static OMElement callWebService(Map<String, String> parameterMap, 
			boolean isPublic, 
			String serviceMethod, 
			String serviceName, 
			SvcAxisCallback callback,
			Map<String, FileEntry> filesMap){
		OMElement result = null;
		
		String analysisServiceURL = ResourceUtility.getAnalysisServiceURL();	

		result = callWebService(parameterMap, 
				serviceMethod, 
				serviceName, 
				analysisServiceURL,
				callback, filesMap);
		
		return result;
	}
	
	/** Generic function for calling web services on the same server (localhost) as the ECGWaveform web pages.
	 * 
	 * @param parameterMap - name, value pairs map for all the (OMEChild) parameters to be sent to the service.
	 * @param isPublic - unused
	 * @param serviceMethod - name of the specific method of the service to be called.
	 * @param serviceName - URL of machine the web service runs from.
	 * @param callback - function to which the service will return result XML to. Calls service without callback if null.
	 * @return
	 */
	public static OMElement callWebService(Map<String, String> parameterMap, 
										   boolean isPublic, 
										   String serviceMethod, 
										   String serviceName, 
										   SvcAxisCallback callback){
		OMElement result = null;
		
		String analysisServiceURL = ResourceUtility.getAnalysisServiceURL();	

		result = callWebService(parameterMap, 
				serviceMethod, 
				serviceName, 
				analysisServiceURL,
				callback, null);
		
		return result;
	}


	/** Generic function for calling web services.<BR>
	 * Directly returns the webservice results if callback is null.
	 * 
	 * @param parameterMap - name, value pairs map for all the parameters(OMEChild) to be sent to the service.
	 * @param serviceMethod - name of the specific method of the service to be called.  e.g. "copyDataFilesToAnalysis"
	 * @param serviceName - name of the web service to invoke, e.g. "dataTransferService"
	 * @param serviceURL - URL of machine the web service runs from. e.g. "http://icmv058.icm.jhu.edu:8080/axis2/services/"
	 * @param callback - function to which the service will return result XML to. Calls service without callback if null.
	 * @return
	 */
	public static OMElement callWebService(Map<String, String> parameterMap, String serviceMethod, String serviceName, String serviceURL, SvcAxisCallback callback){
			return WebServiceUtility.callWebService(parameterMap, serviceMethod, serviceName, serviceURL, callback, null);
	}
	
	/** Generic function for calling web services.<BR>
	 * Directly returns the webservice results if callback is null.
	 * 
	 * @param parameterMap - name, value pairs map for all the parameters(OMEChild) to be sent to the service.
	 * @param serviceMethod - name of the specific method of the service to be called.  e.g. "copyDataFilesToAnalysis"
	 * @param serviceName - name of the web service to invoke, e.g. "dataTransferService"
	 * @param serviceURL - URL of machine the web service runs from. e.g. "http://icmv058.icm.jhu.edu:8080/axis2/services/"
	 * @param callback - function to which the service will return result XML to. Calls service without callback if null.
	 * @param filesMap - name, files pairs map for all the files(OMEChild) to be sent to the service.
	 * @return
	 */
	public static OMElement callWebService(Map<String, String> parameterMap, 
										   String serviceMethod, 
										   String serviceName, 
										   String serviceURL, 
										   SvcAxisCallback callback, 
										   Map<String, FileEntry> filesMap){

		return callWebServiceComplexParam(parameterMap, serviceMethod, serviceName, serviceURL, callback, filesMap);
	}
	
	
	private static void addFiles(Map<String, FileEntry> filesMap, OMElement omWebService, OMFactory omFactory, OMNamespace omNamespace) {
		if(filesMap != null){
			StringBuilder filesId = new StringBuilder();
			for(String key : filesMap.keySet()){
				try {
					OMElement fileElement = omFactory.createOMElement("file_"+key, omNamespace);
					FileEntry file  = filesMap.get(key);
					
					byte[] bytes = new byte[Long.valueOf(file.getSize()).intValue()];
					file.getContentStream().read(bytes);
					
					DataHandler dh = new DataHandler(new ByteArrayDataSource(bytes));
					
					OMText textData = omFactory.createOMText(dh, true);
					fileElement.addChild(textData);
				
					omWebService.addChild(fileElement);
					filesId.append(file.getFileEntryId()).append(',');
					
				} catch (PortalException e) {
					e.printStackTrace();
				} catch (SystemException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			addOMEChild("filesId", filesId.toString(), omWebService);
		}
	}

	/** Generic function for calling web services with parameters which are more than one level deep.<BR>
	 * Directly returns the webservice results if callback is null.
	 * 
	 * @param parameterMap - name, value pairs map for all the parameters(OMEChild) to be sent to the service, if a value is another LinkedHashMap, then it is assumed to contain subnodes of that parameter's key node.
	 * @param serviceMethod - name of the specific method of the service to be called.  e.g. "copyDataFilesToAnalysis"
	 * @param serviceName - name of the web service to invoke, e.g. "dataTransferService"
	 * @param serviceURL - URL of machine the web service runs from. e.g. "http://icmv058.icm.jhu.edu:8080/axis2/services/"
	 * @param callback - function to which the service will return result XML to. Calls service without callback if null.
	 * @return
	 */
	public static OMElement callWebServiceComplexParam(Map<String, Object> parameterMap, String serviceMethod, String serviceName, 
													   String serviceURL,SvcAxisCallback callback){
		
		return callWebServiceComplexParam(parameterMap, serviceMethod, serviceName, serviceURL, callback, null);
		
	}
	/** Generic function for calling web services with parameters which are more than one level deep.<BR>
	 * Directly returns the webservice results if callback is null.
	 * 
	 * @param parameterMap - name, value pairs map for all the parameters(OMEChild) to be sent to the service, if a value is another LinkedHashMap, then it is assumed to contain subnodes of that parameter's key node.
	 * @param serviceMethod - name of the specific method of the service to be called.  e.g. "copyDataFilesToAnalysis"
	 * @param serviceName - name of the web service to invoke, e.g. "dataTransferService"
	 * @param serviceURL - URL of machine the web service runs from. e.g. "http://icmv058.icm.jhu.edu:8080/axis2/services/"
	 * @param callback - function to which the service will return result XML to. Calls service without callback if null.
	 * @param filesMap - name, value pairs map for all the files(OMEChild) to be sent to the service.
	 * @return
	 */
	public static OMElement callWebServiceComplexParam(Map<String, ?> parameterMap, 
													   String serviceMethod,
													   String serviceName, 
													   String serviceURL,
													   SvcAxisCallback callback,
													   Map<String, FileEntry> filesMap){

		String serviceTarget = "";
		
		if(serviceName != null){
			if(!serviceName.equals("")){
				serviceTarget = serviceName; // + "/" + serviceMethod;
			}
			else{
				serviceTarget = serviceMethod;
			}
		}		
		
		EndpointReference targetEPR = new EndpointReference(serviceURL + "/" + serviceTarget);

		OMFactory omFactory = OMAbstractFactory.getOMFactory();

		OMNamespace omNamespace = omFactory.createOMNamespace(serviceURL + "/" + serviceName, serviceName);
		OMElement omWebService = omFactory.createOMElement(serviceMethod, omNamespace);

		extractParameter(parameterMap, omFactory, omNamespace, omWebService);

		addFiles(filesMap, omWebService, omFactory, omNamespace);
		
		ServiceClient sender = getSender(targetEPR, serviceMethod);

		OMElement result = null;
		try {
			if(callback == null){
				//  Directly invoke an anonymous operation with an In-Out message exchange pattern.
				result = sender.sendReceive(omWebService);
				StringWriter writer = new StringWriter();
				result.serialize(XMLOutputFactory.newInstance().createXMLStreamWriter(writer));
				writer.flush();
			}
			else{
				// Directly invoke an anonymous operation with an In-Out message exchange pattern without waiting for a response.
				sender.sendReceiveNonBlocking(omWebService, callback);
			}
		} catch (AxisFault e) {
			e.printStackTrace();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		} catch (FactoryConfigurationError e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	private static void extractParameter(Map<String, ?> map, OMFactory omFactory, OMNamespace omNamespace, OMElement omWebService){
		for(String key : map.keySet()){
			if(key.endsWith("List") | key.endsWith("list") | map.get(key) instanceof Map){
				OMElement omList = omFactory.createOMElement(key, omNamespace);
				@SuppressWarnings("unchecked")
				Map<String, Object> listMap = (Map<String, Object>) map.get(key);
				extractParameter(listMap, omFactory, omNamespace, omList);
				omWebService.addChild(omList);
			}else{
				addOMEChild(key.toString(), map.get(key).toString(), omWebService);
			}
		}
	}
	
	private static ServiceClient getSender(EndpointReference targetEPR, String methodName) {
		Options options = new Options();
		options.setTo(targetEPR);
		options.setProperty(HTTPConstants.SO_TIMEOUT, new Integer(18000000));
		options.setProperty(HTTPConstants.CONNECTION_TIMEOUT, new Integer(18000000));
		options.setTransportInProtocol(Constants.TRANSPORT_HTTP);
		options.setAction("urn:"+methodName);
		options.setProperty(Constants.Configuration.ENABLE_MTOM, Constants.VALUE_TRUE);
		
		ServiceClient sender = null;
		try {
			sender = new ServiceClient();
		} catch (AxisFault e) {
			e.printStackTrace();
		}
		sender.setOptions(options);

		return sender;
	}
	
	private static void addOMEChild(String name, String value, OMElement parent, OMFactory factory, OMNamespace dsNs) {
		OMElement child = factory.createOMElement(name, dsNs);
		child.addChild(factory.createOMText(value));
		parent.addChild(child);
	}
	
	private static void addOMEChild(String name, String value, OMElement parent) {	
		addOMEChild(name, value, parent, parent.getOMFactory(), parent.getNamespace());
	}
	

	/** Looks up the long definition text from the/ECGOntology at Bioportal for the specfied tree node ID.
	 * 
	 * @param treeNodeID - the id to look up, as supplied by the OntologyTree popup.
	 * @return - long definition text
	 */

	public static String annotationLookup(String restURL){
		String definition="WARNING TEST FILLER From BrokerServiceImpl.java";
		String label ="TEST LABEL";
		String sReturn = "";
		
	    URL url;
	    try {
			url = new URL(restURL);
			
			BufferedReader in = new BufferedReader( new InputStreamReader(url.openStream()));

			String inputLine;
			boolean isDescriptionFollowing = false;
			StringBuffer buff = null;
			
			while ((inputLine = in.readLine()) != null){
	
				String regex = "^\\s+<label>\\w+</label>$";
				boolean bLabel = inputLine.matches(regex);
				if(bLabel){
					label = inputLine.trim();
				}

			    if(isDescriptionFollowing) { // <label>
					if(inputLine.length() != 0) {
						if(inputLine.indexOf("</definitions>") > -1) {
							isDescriptionFollowing=false;
							break;
						}else{
							buff.append(inputLine);
						}
					}
				} else {
					if(inputLine.indexOf("<definitions>") > -1) {
						isDescriptionFollowing = true;
						buff = new StringBuffer();
					}
				}
			}
			in.close();
			
			if(buff.length()>0){
				definition = buff.substring(0);
				definition = definition.replace("/n", "");
				definition = definition.replace("<string>", "");
				definition = definition.replace("</string>", "");
				definition = definition.trim();
			}else{
				definition = "Detailed definition not available.";
			}
			sReturn = label + definition;
	
	    }catch(MalformedURLException mue){
	    	mue.printStackTrace();
	    } catch (IOException ioe) {
			ioe.printStackTrace();
			}
		return sReturn;
	}

	/** Looks up the Long definition of an ECG Ontology node ID.
	 * 
	 * @param sNodeID -  node ID from the ECG ontology tree. e.g. "ECGTermsv1:ECG_000000103" for Q_Wave
	 * @return
	 */
	public static String[] lookupOntologyDefinition(String sNodeID){ // e.g. "ECGTermsv1:ECG_000000103" for Q_Wave
		String[] sRet = new String[2];
		String sDefinition="No definition found";
		String sLabel = "No label found";
		String ontID="2079";
		String apikey="24e0e602-54e0-11e0-9d7b-005056aa3316";
		String port="80";
		
		
		String sRestURL = getAnnotationRestURL(sNodeID, ontID, apikey, port);
		sDefinition = WebServiceUtility.annotationLookup(sRestURL);
		int iEndOfLabel = sDefinition.indexOf("</label>");
		sLabel = sDefinition.substring(7, iEndOfLabel);
		sDefinition = sDefinition.substring(iEndOfLabel + 8);
	
		sRet[0] = sLabel;
		sRet[1] = sDefinition;
		return sRet;
	}


	/** Generates the URL of the REST call which will return the details of this Ontology Concept.
	 * 
	 * @param treeNodeID - Node ID returned by the Ontology tree when the concept was selected. e.g ""
	 * @param ontID - id of the ontology to search, e.g. "2079"
	 * @param apikey - JHU's key to use the bioportal lookup service, e.g. "24e0e602-54e0-11e0-9d7b-005056aa3316"
	 * @param port - port number to include in the URL, e.g. "80"
	 * @return -  the REST URL.
	 */
	public static String getAnnotationRestURL(String treeNodeID, String ontID, String apikey, String port){
		treeNodeID = treeNodeID.replace(":", "%3A");
		String restURL = "http://rest.bioontology.org:" + port + "/bioportal/virtual/ontology/" + ontID + "?conceptid=" + treeNodeID + "&apikey=" + apikey;
		return restURL;
	}
	
	/** Parses a service's incoming XML and builds a Map of all the parameters for easy access.
	 * @param param0 - OMElement representing XML with the incoming parameters.
	 */
	public static Map<String, Object> buildParamMap(OMElement param0){

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
	
	
	public static Map<String, OMElement> extractParams(OMElement e){
		Map<String, OMElement> paramMap = new HashMap<String, OMElement>();  
		for (Iterator<?> iterator = e.getChildren(); iterator.hasNext();) {
			Object type = (Object) iterator.next();
			if(type instanceof OMElement){
				OMElement node = (OMElement)type;
				paramMap.put(node.getLocalName(), node);
			}
		}
		return paramMap;
	}

	/** Parses a list node from the service's incoming XML and builds a Map of all its children for easy access.
	 * Used for parameters list and also file handle list.
	 * 
	 * @param param0 - OMElement representing XML with the incoming parameters.
	 */
	public static String[] buildChildArray(OMElement param0){

		ArrayList<String> childList = new ArrayList<String>();

		try {
			@SuppressWarnings("unchecked")
			Iterator<OMElement> iterator = param0.getChildren();

			while(iterator.hasNext()) {
				OMElement param = iterator.next();
				childList.add(param.getText());
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

	 /** Return our current best guess of the name of a lead <BR>
	  * based on the total lead count and the zero based lead number.<BR>
	  * Holter lead names are not yet know, so are not included.
	  * 
	  * @param iLeadNum - Zero based lead number, e.g. first lead in a data set(array) is number zero.
	  * @param iLeadCount - total number of leads in the data set.
	  * @return - one of "I","II","III","aVR","aVL","aVF","V1","V2","V3","V4","V5", or "V6" ("VX","VY","VZ")
	  */
	 public static String guessLeadName(int iLeadNum, int iLeadCount){
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
}