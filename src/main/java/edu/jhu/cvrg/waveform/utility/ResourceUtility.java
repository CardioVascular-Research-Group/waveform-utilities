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
* @author Chris Jurado
* 
*/
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.context.FacesContext;
import javax.portlet.PortletRequest;

import com.liferay.faces.portal.context.LiferayFacesContext;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.PrefsPropsUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.User;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;

import edu.jhu.cvrg.filestore.enums.EnumFileStoreType;

public class ResourceUtility {
	
	private static String MISSING_VALUE = "0";
	
	private ResourceUtility(){}
	
	public static String getServerName(){
		LiferayFacesContext liferayFacesContext = LiferayFacesContext.getInstance();
		PortletRequest request = (PortletRequest)liferayFacesContext.getExternalContext().getRequest();
		String serverName = request.getServerName();
		if(request.getServerPort() > 0){
			serverName = serverName + ":" + String.valueOf(request.getServerPort());
		}
		return serverName;
	}
	
	private static String getValue(String key)  {
		String value = MISSING_VALUE;
		try {
			value = PrefsPropsUtil.getString(key);
		} catch (SystemException e) {
			printErrorMessage("Resource Utility");
			System.err.println("Resource Configuration file not found.");
		}
		return value;
	}
	
	public static String getAlgorithmDetailsMethod(){
		return getValue("algorithmDetailsMethod");
	}
	
	public static String getPhysionetAnalysisService(){
		return getValue("physionetAnalysisService");
	}
	
	public static String getDbMainDatabase(){
		return getValue("dbMainDatabase");
	}
	
	public static String getDbDriver()  {
		return getValue("dbDriver");
	}
	
	public static String getDbURI()  {
		return getValue("dbURI");
	}
	
	public static String getDbUser()  {
		return getValue("dbUser");
	}
	
	public static String getDbPassword()  {
		return getValue("dbPassword");
	}
	
	public static String getStagingFolder()  {
		return getValue("stagingFolder");
	}

	public static String getAnalysisServiceURL()  {
		return getValue("analysisServiceURL");
	}
	
	public static String getNodeConversionService()  {
		return getValue("nodeConversionService");
	}
	
	public static String getStagingServiceMethod()  {
		return getValue("stagingServiceMethod");
	}

	public static String getStagingService()  {
		return getValue("stagingService");
	}
	
	public static String getConsolidateCsvMethod(){
         return getValue("consolidateCsvMethod");
	}
	
	public static String getDataTransferClass()  {
		return getValue("dataTransferClass");
	}
	
	public static String getCopyFilesMethod()  {
		return getValue("copyFilesMethod");
	}
	
	public static String getDataTransferServiceName()  {
		return getValue("dataTransferServiceName");
	}
	
	public static String getConsolidatePrimaryAndDerivedDataMethod()  {
		return getValue("consolidatePrimaryAndDerivedDataMethod");
	}
	
	public static String getNodeDataServiceName()  {
		return getValue("nodeDataServiceName");
	}
	
	public static String getAnalysisDatabase()  {
		return getValue("dbAnalysisDatabase");
	}
	
	public static String getAnalysisResults()  {
		return getValue("dbAnalysisResults");
	}
	
	
	public static String getCopyResultFilesFromAnalysis()  {
		return getValue("copyResultFilesFromAnalysis");
	}
	
	public static String getDeleteFilesFromAnalysis()  {
		return getValue("deleteFilesFromAnalysis");
	}
	
	public static String getFtpHost()  {		
		return getValue("ftpHost");
	}
	
	public static String getFtpUser()  {		
		return getValue("ftpUser");
	}
	
	public static String getFtpPassword()  {
		return getValue("ftpPassword");
	}
	
	public static String getFtpRoot()  {
		return getValue("ftpRoot");
	}
	
	public static String getLocalDownloadFolder()  {
		return getValue("localDownloadFolder");
	}	
	
	public static void printErrorMessage(String source){
		System.err.println("*************************** Error in " + source + " ******************************");
	}
	
	public static User getUser(long userId){
		User user = null;
		try {
			user = UserLocalServiceUtil.getUser(userId);
		} catch (PortalException e) {
			printErrorMessage("Resource Utility");
			e.printStackTrace();
		} catch (SystemException e) {
			printErrorMessage("Resource Utility");
			e.printStackTrace();
		}
		return user;
	}
	
	public static long getCurrentGroupId(){	
		LiferayFacesContext liferayFacesContext = LiferayFacesContext.getInstance();
		PortletRequest request = (PortletRequest)liferayFacesContext.getExternalContext().getRequest();
		ThemeDisplay themeDisplay = (ThemeDisplay)request.getAttribute(WebKeys.THEME_DISPLAY);
		return themeDisplay.getLayout().getGroupId();	
	}
	
	public static long getCurrentCompanyId(){	
		LiferayFacesContext liferayFacesContext = LiferayFacesContext.getInstance();
		PortletRequest request = (PortletRequest)liferayFacesContext.getExternalContext().getRequest();
		ThemeDisplay themeDisplay = (ThemeDisplay)request.getAttribute(WebKeys.THEME_DISPLAY);
		return themeDisplay.getCompanyId();	
	}
	
	public static User getCurrentUser(){
		LiferayFacesContext liferayFacesContext = LiferayFacesContext.getInstance();
		User currentUser = null;
		try {
			String remoteUser = liferayFacesContext.getPortletRequest().getRemoteUser();
			if(remoteUser != null){
				currentUser = UserLocalServiceUtil.getUser(Long.parseLong(remoteUser));
			}
		} catch (NumberFormatException e) {
			printErrorMessage("Resource Utility");
			System.err.println("NumberFormatException.  Is the user logged in?");
		} catch (PortalException e) {
			printErrorMessage("Resource Utility");
			e.printStackTrace();
		} catch (SystemException e) {
			printErrorMessage("Resource Utility");
			e.printStackTrace();
		}
		return currentUser;
	}

	public static long getCurrentUserId(){
		return getCurrentUser().getUserId();
	}
	
	public static boolean isUserCommunityMember(long userId, long communityId){
		try {
			List<Group> userGroups = GroupLocalServiceUtil.getUserGroups(userId);
			
			for(Group group : userGroups){
				if(group.getGroupId() == communityId){
					return true;
				}
			}
		} catch (PortalException e) {
			printErrorMessage("Resource Utility");
			e.printStackTrace();
		} catch (SystemException e) {
			printErrorMessage("Resource Utility");
			e.printStackTrace();
		}
		return false;
	}
	
	public static long getGroupId(String communityName){
		long groupId = 0L;
		List<Group> groupList;
		try {
			groupList = GroupLocalServiceUtil.getGroups(0, GroupLocalServiceUtil.getGroupsCount());;
			for(Group group : groupList){
				if(group.getName().equals(communityName)){
					groupId = group.getGroupId();
				}
			}
		} catch (SystemException e) {
			printErrorMessage("Resource Utility");
			e.printStackTrace();
		}
		return groupId;
	}
	
	public static String convertToUserName(long userId){
		String userFullName = "";
		try {
			User user = UserLocalServiceUtil.getUser(userId);
			userFullName = user.getFullName();
		} catch (PortalException e) {
			printErrorMessage("Resource Utility");
			e.printStackTrace();
		} catch (SystemException e) {
			printErrorMessage("Resource Utility");
			e.printStackTrace();
		}
		return userFullName;
	}
	
	public static void showMessages(String title, List<FacesMessage> messages) {
		Severity severity = FacesMessage.SEVERITY_INFO;
    	StringBuilder sb = new StringBuilder("<br /><ul>");
    	for (FacesMessage m : messages) {
    		if(m.getSeverity().getOrdinal() > severity.getOrdinal()){
    			severity = m.getSeverity();
    		}
    		sb.append("<li>");
			sb.append(m.getDetail());
			sb.append("</li>");
		}
    	sb.append("</ul>");
    	
    	FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, title, sb.toString()));
	}  

	public static EnumFileStoreType getFileStorageType(){
		String fileStoreType = getValue("file.storage");
		if(fileStoreType.equals("liferay61")){
			return EnumFileStoreType.LIFERAY_61;
		}
		return null;
	}
}