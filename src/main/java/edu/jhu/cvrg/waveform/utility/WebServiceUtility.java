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
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
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
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.jhu.cvrg.filestore.model.FSFile;
import edu.jhu.cvrg.waveform.callbacks.SvcAxisCallback;

public class WebServiceUtility {
	
	private static final String CVRG_ONTOLOGY_PREFIX_ID = ServiceProperties.getInstance().getProperty(ServiceProperties.CVRG_ONTOLOGY_PREFIX_ID);
	private static Logger log = Logger.getLogger(WebServiceUtility.class);
	private static long waitUntil = 0L;
	
	private WebServiceUtility(){}

	public static OMElement callWebService(Map<String, String> parameterMap, String serviceMethod, 
			String serviceName, SvcAxisCallback callback, Map<String, FSFile> filesMap){
		OMElement result = null;
		String analysisServiceURL = ServiceProperties.getInstance().getProperty(ServiceProperties.MAIN_SERVICE_URL);
		System.out.println("Files Map has " + filesMap.size() + " files in it.");
		result = callWebService(parameterMap, serviceMethod, serviceName, analysisServiceURL, callback, filesMap);
		return result;
	}

	public static OMElement callWebService(Map<String, String> parameterMap, String serviceMethod, 
		String serviceName, SvcAxisCallback callback){
		OMElement result = null;
		String analysisServiceURL = ServiceProperties.getInstance().getProperty(ServiceProperties.MAIN_SERVICE_URL);	
		result = callWebService(parameterMap, serviceMethod, serviceName, analysisServiceURL, callback, null);
		return result;
	}

	public static OMElement callWebService(Map<String, String> parameterMap, String serviceMethod, String serviceName, String serviceURL, SvcAxisCallback callback){
			return WebServiceUtility.callWebService(parameterMap, serviceMethod, serviceName, serviceURL, callback, null);
	}

	public static OMElement callWebService(Map<String, String> parameterMap, String serviceMethod, String serviceName, 
		String serviceURL, SvcAxisCallback callback, Map<String, FSFile> filesMap){
		System.out.println("Files Map has " + filesMap.size() + " files in it.");
		return callWebServiceComplexParam(parameterMap, serviceMethod, serviceName, serviceURL, callback, filesMap);
	}
	
	private static void addFiles(Map<String, FSFile> filesMap, OMElement omWebService, OMFactory omFactory, OMNamespace omNamespace) {
		if(filesMap != null){
			StringBuilder filesId = new StringBuilder();
			for(String key : filesMap.keySet()){
				System.out.println("Key is " + key);
				OMElement fileElement = omFactory.createOMElement("file_" + key, omNamespace);
				FSFile file  = filesMap.get(key);
				if(file == null){
					System.out.println("File is null........");
				}
				DataHandler dh = new DataHandler(new ByteArrayDataSource(file.getFileDataAsBytes()));
				OMText textData = omFactory.createOMText(dh, true);
				fileElement.addChild(textData);
				omWebService.addChild(fileElement);
				filesId.append(file.getId()).append(',');
			}
			addOMEChild("filesId", filesId.toString(), omWebService);
		}
	}

	public static OMElement callWebServiceComplexParam(Map<String, Object> parameterMap, String serviceMethod, String serviceName, 
		String serviceURL,SvcAxisCallback callback){
		return callWebServiceComplexParam(parameterMap, serviceMethod, serviceName, serviceURL, callback, null);
	}

	public static OMElement callWebServiceComplexParam(Map<String, ?> parameterMap, String serviceMethod, String serviceName, 
		String serviceURL, SvcAxisCallback callback, Map<String, FSFile> filesMap){
		log.info("waveform-utilities.WebServiceUtility.callWebServiceComplexParam()");
		System.out.println("Files Map has " + filesMap.size() + " files in it.");
		
		
		for(String key : filesMap.keySet()){
			System.out.println("Testing Key is " + key);
			FSFile file  = filesMap.get(key);
			if(file == null){
				System.out.println("File is null........");
			}
			else{
				System.out.println("File is not null........");
				System.out.println("File is " + file.getName());
			}

		}
		
		
		
		
		String serviceTarget = "";
		if(serviceName != null){
			serviceTarget = (!serviceName.equals("")) ? serviceName : serviceMethod;
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
				log.info("Service/method, no callback:" + serviceName + "/" + serviceMethod);
				//  Directly invoke an anonymous operation with an In-Out message exchange pattern.
				result = sender.sendReceive(omWebService);
				StringWriter writer = new StringWriter();
				result.serialize(XMLOutputFactory.newInstance().createXMLStreamWriter(writer));
				writer.flush();
			}
			else{
				log.info("Service/method, WITH callback:" + serviceName + "/" + serviceMethod);
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
			if(map.get(key) != null){
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

	public static String annotationXMLLookup(String restURL){
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
							isDescriptionFollowing = false;
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
	
	public static Map<String, String> annotationJSONLookup(String restURL, String ... key){
		Map<String, String> ret = null;
	    URL url;
	    HttpURLConnection conn = null;
	    try {
	    	if(System.currentTimeMillis() > waitUntil){
				url = new URL(restURL);
				conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("GET");
				conn.setRequestProperty("Accept", "application/json");
				BufferedReader in = new BufferedReader(new InputStreamReader((conn.getInputStream())));
				String jsonSrc = in.readLine();
				in.close();
				JSONObject jsonObject = new JSONObject(jsonSrc);
				ret = new HashMap<String, String>();
				for (int i = 0; i < key.length; i++) {
					Object atr = jsonObject.get(key[i]);
					String value = "";	
					if(atr instanceof JSONArray){
						JSONArray array = ((JSONArray) atr);
						for (int j = 0; j < array.length(); j++) {
							value += array.getString(j);	
						}
					}else {
						value = atr.toString(); 
					}
					if(value.isEmpty()){
						value = "No " + key[i] + " found";
					}
					ret.put(key[i], value);
				}
	    	}else{
	    		log.warn("Waiting the bioportal server");
	    	}
		
	    }catch(MalformedURLException mue){
	    	mue.printStackTrace();
	    } catch (IOException ioe) {
	    	//wait for 1 minute
	    	waitUntil = System.currentTimeMillis() + (1000 * 60);
			log.error(ioe.getMessage());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public static Map<String, String> lookupOntology(String ontology, String sNodeID, String ... atributes){ // e.g. "ECGTermsv1:ECG_000000103" for Q_Wave	
		sNodeID = (!sNodeID.contains("http")) ? CVRG_ONTOLOGY_PREFIX_ID + sNodeID : sNodeID;
		String sRestURL = getAnnotationRestURL(sNodeID, ontology, ServiceProperties.getInstance().getBioportalApikey());
		return WebServiceUtility.annotationJSONLookup(sRestURL, atributes);		
	}

	public static String getAnnotationRestURL(String treeNodeID, String ontID, String apikey){
		if(treeNodeID.contains("http://")){
			try {
				treeNodeID = URLEncoder.encode(treeNodeID, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		String restURL = ServiceProperties.getInstance().getBioportalAPIServerURL() + "/ontologies/" + ontID + "/classes/" + treeNodeID + "?apikey=" + apikey;
		return restURL;
	}

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

	public static String[] buildChildArray(OMElement param0){
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
}