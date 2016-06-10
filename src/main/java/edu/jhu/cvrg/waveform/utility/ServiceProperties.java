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
/**
* @author CVRG Team
*/
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class ServiceProperties {
	
	private static String PROPERTIES_PATH = "/conf/service.properties";
	private static Properties prop;
	private static ServiceProperties singleton;
	private static File propertiesFile = null;
	private static long lastChange = 0;
	
	public static final String MAIN_SERVICE_URL = "main.service.URL";
	public static final String DATATRANSFER_SERVICE_NAME = "dataTransferServiceName";
	public static final String DATATRANSFER_SERVICE_METHOD = "dataTransferServiceMethod";
	public static final String TEMP_FOLDER = "temp.folder";
	public static final String OMNAMESPACEURI = "om.namespace.uri";
	public static final String OMNAMESPACEPREFIX = "om.namespace.prefix";
	public static final String CVRG_ONTOLOGY_PREFIX_ID = "cvrg.ontology.prefix.id";
	public static final String LIFERAY_FILES_ENDPOINT_URL = "liferay.endpoint.url.files";
	public static final String LIFERAY_WS_USER = "liferay.ws.user";
	public static final String LIFERAY_WS_PASSWORD = "liferay.ws.password";
	public static final String BIOPORTAL_ACTIVE_SERVER = "bioportal.active.server";

	private ServiceProperties() {
		prop = new Properties();
		propertiesFile = new File(System.getProperty("catalina.home") + PROPERTIES_PATH);
		loadProperties();
	}
	
	public static ServiceProperties getInstance(){
		if(singleton == null){
			singleton = new ServiceProperties();
		}
		return singleton;
	}
	
	public String getProperty(String propertyName){
		loadProperties();
		return prop.getProperty(propertyName);
	}
	
	private void loadProperties(){
		try {
			if(propertiesFile.lastModified() > lastChange){
				prop.clear();
				prop.load(new FileReader(propertiesFile));
				lastChange = propertiesFile.lastModified();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String getBioportalAPIServerURL(){
		return  getBioportalParam("apiserverurl");
	}
	
	public String getBioportalUIServerURL(){
		return  getBioportalParam("uiserverurl");
	}
	
	public String getBioportalApikey(){
		return  getBioportalParam("apikey");
	}	
	
	private String getBioportalParam(String type){
		String setting = "";
		switch(type){
			case "apiserverurl": 	setting = ".api.server.url";	break;
			case "uiserverurl":		setting = ".ui.server.url";		break;
			case "apikey":			setting = ".apikey";			break;
		}
		String activeServer = getProperty(BIOPORTAL_ACTIVE_SERVER);
		activeServer = (activeServer == null) ? "remote" : activeServer;
		System.out.println("ServiceProperties.getBioportalParam(" + type + ") = " + getProperty("bioportal." + activeServer + setting));
		System.out.println("ServiceProperties.getBioportalParam(" + type + ") property name is " + "bioportal." + activeServer + setting); 
		return  getProperty("bioportal." + activeServer + setting);
	}
}