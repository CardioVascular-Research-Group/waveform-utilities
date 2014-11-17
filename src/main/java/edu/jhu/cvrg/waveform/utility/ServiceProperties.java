package edu.jhu.cvrg.waveform.utility;

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
	

	private ServiceProperties() {
		prop = new Properties();
		propertiesFile = new File(System.getProperty("catalina.home")+PROPERTIES_PATH);
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
	
	public static final String MAIN_SERVICE_URL = "main.service.URL";
	public static final String DATATRANSFER_SERVICE_NAME = "dataTransferServiceName";
	public static final String DATATRANSFER_SERVICE_METHOD = "dataTransferServiceMethod";
	 
	public static final String TEMP_FOLDER = "temp.folder";
	public static final String LIFERAY_FILES_ENDPOINT_URL = "liferay.endpoint.url.files";
	public static final String LIFERAY_WS_USER = "liferay.ws.user";
	public static final String LIFERAY_WS_PASSWORD = "liferay.ws.password";
	
	public static final String BIOPORTAL_ACTIVE_SERVER = "bioportal.active.server";
	
	public String getBioportalAPIServerURL(){
		String activeServer = getProperty(BIOPORTAL_ACTIVE_SERVER);
		
		if(activeServer == null){
			activeServer = "remote";
		}
		return  getProperty("bioportal."+activeServer+".api.server.url");
	}
	
	public String getBioportalUIServerURL(){
		String activeServer = getProperty(BIOPORTAL_ACTIVE_SERVER);
		
		if(activeServer == null){
			activeServer = "remote";
		}
		return  getProperty("bioportal."+activeServer+".ui.server.url");
	}
	
	public String getBioportalApikey(){
		String activeServer = getProperty(BIOPORTAL_ACTIVE_SERVER);
		
		if(activeServer == null){
			activeServer = "remote";
		}
		return  getProperty("bioportal."+activeServer+".apikey");
	}
		
}
