package edu.jhu.cvrg.waveform.model;
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
 * @author Chris Jurado
 * 
 */
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.portlet.RenderRequest;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.liferay.faces.portal.context.LiferayFacesContext;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portlet.documentlibrary.service.DLAppLocalServiceUtil;

import edu.jhu.cvrg.waveform.utility.ResourceUtility;
import edu.jhu.cvrg.waveform.utility.StudyEntryUtility;

public class FileTree implements Serializable{

	private static final long serialVersionUID = 1L;
	private TreeNode treeRoot;
	private TreeNode selectedNode;
	private TreeNode[] selectedNodes;
	private String newFolderName = "";
	private ArrayList<StudyEntry> studyEntryList;
	StudyEntryUtility theDB;
	
	
	private String username;
	private Long userId;
	private boolean useDB;
	private Long groupId;
	
	public FileTree (User user, boolean useDB){
		this.useDB = useDB;  
		initialize(user);
	}
	
	public void initialize(User user) {
		this.username = user.getScreenName();
		this.userId = user.getUserId();
		
		if(useDB){
			theDB = new StudyEntryUtility(ResourceUtility.getDbUser(),
					ResourceUtility.getDbPassword(), 
					ResourceUtility.getDbURI(),	
					ResourceUtility.getDbDriver(), 
					ResourceUtility.getDbMainDatabase());
		}

		if (treeRoot == null) {
			buildTree();
		}
	}

	private void buildTree() {
		
		treeRoot = new DefaultTreeNode("root", null);
		
		if(useDB){
			buildTreeFromDB();
		}else{
			buildTreeFromFileRepo();
		}
	}

	private void buildTreeFromFileRepo() {
		groupId = ((ThemeDisplay)((RenderRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest()).getAttribute(WebKeys.THEME_DISPLAY)).getLayout().getGroupId();
		
		try {
			Folder rootFolder = null;
			List<Folder> rootFolders = DLAppLocalServiceUtil.getFolders(groupId, 0L);
			
			for (Folder folder : rootFolders) {
				if("waveform".equals(folder.getName())){
					rootFolder = folder;
					break;
				}
			}
			
			if(rootFolder != null){
				this.getFolderContent(rootFolder, treeRoot);	
			}else{
				System.out.println("WAVEFORM folder does not exist");
			}
			
			
		} catch (PortalException e) {
			e.printStackTrace();
		} catch (SystemException e) {
			e.printStackTrace();
		}
	}

	private void getFolderContent(Folder folder, TreeNode node){
		try {
			DefaultTreeNode folderNode = new FileTreeNode(folder, node);
			
			List<Folder> subFolders = DLAppLocalServiceUtil.getFolders(groupId, folder.getFolderId());
			if(subFolders != null){
				for (Folder folder2 : subFolders) {
					getFolderContent(folder2, folderNode);
				}						
			}
			
			List<FileEntry> subFiles = DLAppLocalServiceUtil.getFileEntries(groupId, folder.getFolderId());
			if(subFiles != null){
				for (FileEntry file : subFiles) {
					new FileTreeNode(file, node);
				}				
			}
		} catch (PortalException e) {
			e.printStackTrace();
		} catch (SystemException e) {
			e.printStackTrace();
		}
	}
	
	

	private void buildTreeFromDB() {
		studyEntryList = theDB.getEntries(username);

		for (StudyEntry studyEntry : studyEntryList) {

			String[] path = studyEntry.getVirtualPath().split("\\|");
			TreeNode workNode = treeRoot;

			for (String step : path) {

				TreeNode newNode = getNodeByName(workNode, step);

				if (newNode == null) {
					StudyEntry entryFolder = new StudyEntry();
					entryFolder.setSubjectID(step);
					newNode = new DefaultTreeNode("default", entryFolder, workNode);
				}
				newNode.setExpanded(true);
				
				workNode = newNode;
			}

			@SuppressWarnings("unused")
			TreeNode recordNode = new DefaultTreeNode("document", studyEntry, workNode);
		}
	}

	public String getSelectedNodePath() {

		TreeNode node = this.selectedNode;
		String path = (String) node.getData();

		while (!node.getParent().getData().toString().equals("Root")) {
			node = node.getParent();
			path = node.getData().toString() + "|" + path;
		}

		return path;

	}
	
	public Long getSelectedNodeId() {

		TreeNode node = this.selectedNode;
		
		if(node instanceof FileTreeNode){
			FileTreeNode treeNode = (FileTreeNode) node;
			if(treeNode.getContent() instanceof Folder){
				return ((Folder) treeNode.getContent()).getFolderId();
			}else{
				return ((FileEntry) treeNode.getContent()).getFileEntryId();
			}
		}
		
		return null;
	}


	private TreeNode getNodeByName(TreeNode searchNode, String name) {

		for (TreeNode node : searchNode.getChildren()) {
			if (((StudyEntry)node.getData()).getSubjectID().equals(name)) {
				return node;
			}
		}
		return null;
	}

	public void addFolder(ActionEvent event) {
		
		if (selectedNode == null) {
			selectedNode = (DefaultTreeNode) treeRoot;
		}

		if (!newFolderName.equals("")) {
			if(useDB){
				StudyEntry entryFolder = new StudyEntry();
				entryFolder.setSubjectID(newFolderName);
				TreeNode newNode = new DefaultTreeNode(newFolderName, entryFolder, selectedNode);
				selectedNode.setExpanded(true);
				selectedNode = (DefaultTreeNode) newNode;
			}else{
				try {
					FileTreeNode parentNode = (FileTreeNode) selectedNode;
					
					Folder parentFolder = null;
					if(parentNode.getContent() instanceof Folder){
						parentFolder = (Folder) parentNode.getContent();
					}else{
						parentFolder = (Folder) ((FileTreeNode)parentNode.getParent()).getContent();
					}
					
					ServiceContext service = LiferayFacesContext.getInstance().getServiceContext();
					
					DLAppLocalServiceUtil.addFolder(userId, this.groupId, parentFolder.getFolderId(), newFolderName, "", service);
					
				} catch (PortalException e) {
					e.printStackTrace();
				} catch (SystemException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public ArrayList<StudyEntry> getSelectedFileNodes() {

		if(selectedNodes == null){
			return null;
		}
		
		ArrayList<StudyEntry> fileEntries = new ArrayList<StudyEntry>();

		for (TreeNode selectedNode : selectedNodes) {
			if (selectedNode.isLeaf()) {
				fileEntries.add((StudyEntry)selectedNode.getData());
			}
		}
		return fileEntries;
	}
	
	public void selectAllChildNodes(TreeNode startingNode){

		for(TreeNode node : startingNode.getChildren()){
			node.setSelected(true);
			if(!node.getType().equals("document")){
				selectAllChildNodes(node);
			}
		}
	}
	
	public void unSelectAllChildNodes(TreeNode startingNode){
		for(TreeNode node : startingNode.getChildren()){
			node.setSelected(false);
			if(!node.getType().equals("document")){
				unSelectAllChildNodes(node);
			}
		}
	}

	public TreeNode getTreeRoot() {
		return treeRoot;
	}

	public TreeNode getSelectedNode() {
		return selectedNode;
	}

	public void setSelectedNode(TreeNode selectedNode) {
		this.selectedNode = selectedNode;
	}

	public String getNewFolderName() {
		return newFolderName;
	}

	public void setNewFolderName(String newFolderName) {
		this.newFolderName = newFolderName;
	}

	public TreeNode[] getSelectedNodes() {
		return selectedNodes;
	}

	public void setSelectedNodes(TreeNode[] selectedNodes) {
		this.selectedNodes = selectedNodes;
	}
}
