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
 * @author Andre Vilardo
 * 
 */
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.event.ActionEvent;

import org.primefaces.model.TreeNode;

import com.liferay.faces.portal.context.LiferayFacesContext;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.documentlibrary.service.DLAppLocalServiceUtil;

import edu.jhu.cvrg.waveform.utility.ResourceUtility;

public class LocalFileTree implements Serializable{

	private static final long serialVersionUID = 4904469355710631253L;

	private static final String WAVEFORM_ROOT_FOLDER = "waveform";
	
	private FileTreeNode treeRoot;
	private FileTreeNode selectedNode;
	private TreeNode[] selectedNodes;
	private String newFolderName = "";
	
	private Long userId;
	private Long groupId;
	
	private String extentionFilter;
	
	public LocalFileTree (Long user){
		initialize(user);
	}
	
	
	public LocalFileTree (Long user, String extentionFilter){
		this.extentionFilter = extentionFilter;
		initialize(user);
	}
	
	public void initialize(Long userId) {
		this.userId = userId;
		this.groupId = ResourceUtility.getCurrentGroupId();
		
		if (treeRoot == null) {
			buildTree();
		}
	}

	private void buildTree() {
		
		try {
			Folder rootFolder = null;
			List<Folder> rootFolders = DLAppLocalServiceUtil.getFolders(groupId, 0L);
			
			for (Folder folder : rootFolders) {
				if(WAVEFORM_ROOT_FOLDER.equals(folder.getName())){
					rootFolder = folder;
					break;
				}
			}
			
			if(rootFolder != null){
				treeRoot = new FileTreeNode(rootFolder, null);
				treeRoot.setExpanded(true);
				this.getFolderContent(rootFolder, treeRoot);
			}else{
				System.out.println(WAVEFORM_ROOT_FOLDER + " folder does not exist");
			}
			
			
		} catch (PortalException e) {
			e.printStackTrace();
		} catch (SystemException e) {
			e.printStackTrace();
		}
	}

	private void getFolderContent(Folder folder, FileTreeNode node){
		try {
			FileTreeNode folderNode;
			if(LocalFileTree.WAVEFORM_ROOT_FOLDER.equals(folder.getName())){
				folderNode = node;
			}else{
				folderNode = new FileTreeNode(folder, node);	
			}
			
			List<Folder> subFolders = DLAppLocalServiceUtil.getFolders(groupId, folder.getFolderId());
			if(subFolders != null){
				for (Folder folder2 : subFolders) {
					getFolderContent(folder2, folderNode);
				}						
			}
			
			List<FileEntry> subFiles = DLAppLocalServiceUtil.getFileEntries(groupId, folder.getFolderId());
			if(subFiles != null){
				for (FileEntry file : subFiles) {
					if(extentionFilter == null || extentionFilter.equalsIgnoreCase(file.getExtension())){
						new FileTreeNode(file, folderNode);
					}
				}				
			}
		} catch (PortalException e) {
			e.printStackTrace();
		} catch (SystemException e) {
			e.printStackTrace();
		}
	}
	
	public Long getSelectedNodeId() {

		FileTreeNode node = this.getSelectedNode();
		
		if (node == null) {
			node = treeRoot;
		}
		
		if(node.getContent() instanceof Folder){
			return ((Folder) node.getContent()).getFolderId();
		}else{
			return ((FileEntry) node.getContent()).getFileEntryId();
		}
		
	}

	public void addFolder(ActionEvent event) {
		
		FileTreeNode selectedNode = this.getSelectedNode();
		
		if (selectedNode == null) {
			selectedNode = treeRoot;
		}

		if (!newFolderName.equals("")) {
			try {
				FileTreeNode parentNode = selectedNode;
				
				Folder parentFolder = null;
				if(parentNode.getContent() instanceof Folder){
					parentFolder = (Folder) parentNode.getContent();
				}else{
					parentFolder = (Folder) ((FileTreeNode)parentNode.getParent()).getContent();
				}
				
				ServiceContext service = LiferayFacesContext.getInstance().getServiceContext();
				
				Folder newFolder = DLAppLocalServiceUtil.addFolder(userId, this.groupId, parentFolder.getFolderId(), newFolderName, "", service);
				
				FileTreeNode newNode =  new FileTreeNode(newFolder, parentNode);
				selectedNode = newNode;	
			} catch (PortalException e) {
				e.printStackTrace();
			} catch (SystemException e) {
				e.printStackTrace();
			}
		}
	}
	
	public Folder getSelectFolder(){
		
		FileTreeNode selectedNode = this.getSelectedNode();
		
		if (selectedNode == null) {
			selectedNode = treeRoot;
		}
		
		if(FileTreeNode.FOLDER_TYPE.equals(selectedNode.getType())){
			return (Folder) selectedNode.getContent();
		}else{
			return null;
		}
	}
	
	public ArrayList<FileEntry> getSelectedFileNodes() {

		if(selectedNodes == null){
			return null;
		}
		
		ArrayList<FileEntry> fileEntries = new ArrayList<FileEntry>();

		for (TreeNode selectedNode : selectedNodes) {
			if (selectedNode.isLeaf() && FileTreeNode.FILE_TYPE.equals(selectedNode.getType())) {
				fileEntries.add((FileEntry)((FileTreeNode)selectedNode).getContent());
			}
		}
		return fileEntries;
	}
	
	public void selectAllChildNodes(TreeNode startingNode){

		for(TreeNode node : startingNode.getChildren()){
			node.setSelected(true);
			if(node.getType().equals(FileTreeNode.FOLDER_TYPE)){
				selectAllChildNodes(node);
			}
		}
	}
	
	public void unSelectAllChildNodes(TreeNode startingNode){
		for(TreeNode node : startingNode.getChildren()){
			node.setSelected(false);
			if(node.getType().equals(FileTreeNode.FOLDER_TYPE)){
				unSelectAllChildNodes(node);
			}
		}
	}
	

	public FileTreeNode getTreeRoot() {
		return treeRoot;
	}

	public String getNewFolderName() {
		return newFolderName;
	}

	public void setNewFolderName(String newFolderName) {
		this.newFolderName = newFolderName;
	}

	public FileTreeNode getSelectedNode() {
		return selectedNode;
	}

	public void setSelectedNode(FileTreeNode selectedNode) {
		this.selectedNode = selectedNode;
	}

	public TreeNode[] getSelectedNodes() {
		return selectedNodes;
	}

	public void setSelectedNodes(TreeNode[] selectedNodes) {
		this.selectedNodes = selectedNodes;
	}
	
	

}
