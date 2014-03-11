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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.event.ActionEvent;

import org.apache.log4j.Logger;
import org.primefaces.model.TreeNode;

import com.liferay.faces.portal.context.LiferayFacesContext;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.documentlibrary.service.DLAppLocalServiceUtil;

import edu.jhu.cvrg.dbapi.dto.FileInfoDTO;
import edu.jhu.cvrg.dbapi.factory.Connection;
import edu.jhu.cvrg.dbapi.factory.ConnectionFactory;
import edu.jhu.cvrg.waveform.utility.ResourceUtility;

public class LocalFileTree implements Serializable{

	private static final long serialVersionUID = 4904469355710631253L;

	private static final String WAVEFORM_ROOT_FOLDER = "waveform";
	
	private String USER_ROOT_FOLDER;
	
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
		this.USER_ROOT_FOLDER = String.valueOf(userId);
		Connection con = ConnectionFactory.createConnection();
		
		buildTree(con);
		
	}

	private void buildTree(Connection con) {
		
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
				
				
				rootFolder = _addFolder(rootFolder, USER_ROOT_FOLDER);
				
				treeRoot = new FileTreeNode(rootFolder, null);
				treeRoot.setExpanded(true);
				
				List<FileInfoDTO> files = con.getFileListByUser(userId);
				
				this.getFolderContent(this.extractFileIdList(files), rootFolder, treeRoot);
			}else{
				getLog().error(WAVEFORM_ROOT_FOLDER + " folder does not exist");
			}
			
		} catch (PortalException e) {
			getLog().error("Erro on tree loading. "+ e.getMessage());
		} catch (SystemException e) {
			getLog().error("Erro on tree loading. "+ e.getMessage());
		}
	}

	private Map<Long, FileInfoDTO> extractFileIdList(List<FileInfoDTO> files){
		Map<Long, FileInfoDTO> fileMap = new HashMap<Long, FileInfoDTO>();
		if(files!= null){
			for (FileInfoDTO fileInfoDTO : files) {
				fileMap.put(fileInfoDTO.getFileEntryId(), fileInfoDTO);
			}
		}
		return fileMap;
	} 
	
	
	private void getFolderContent(Map<Long, FileInfoDTO> map, Folder folder, FileTreeNode node){
		try {
			FileTreeNode folderNode = null;
			if(USER_ROOT_FOLDER.equals(folder.getName())){
				folderNode = node;
			}else{
				folderNode = new FileTreeNode(folder, node);
			}
			
			List<Folder> subFolders = DLAppLocalServiceUtil.getFolders(groupId, folder.getFolderId());
			if(subFolders != null){
				for (Folder folder2 : subFolders) {
					getFolderContent(map, folder2, folderNode);
				}						
			}
			
			List<FileEntry> subFiles = DLAppLocalServiceUtil.getFileEntries(groupId, folder.getFolderId());
			if(subFiles != null){
				for (FileEntry file : subFiles) {
					if(extentionFilter == null || extentionFilter.equalsIgnoreCase(file.getExtension())){
						FileInfoDTO fileDTO = map.get(file.getFileEntryId());
						if(fileDTO != null){
							new FileTreeNode(file, folderNode, fileDTO);
						}else{
							new FileTreeNode(file, folderNode, null);
						}
					}
				}				
			}
		} catch (PortalException e) {
			getLog().error("Erro on tree loading. "+ e.getMessage());
		} catch (SystemException e) {
			getLog().error("Erro on tree loading. "+ e.getMessage());
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

		FileTreeNode parentNode = selectedNode;
		
		Folder parentFolder = null;
		if(parentNode.getContent() instanceof Folder){
			parentFolder = (Folder) parentNode.getContent();
		}else{
			parentFolder = (Folder) ((FileTreeNode)parentNode.getParent()).getContent();
		}
		
		if (!newFolderName.equals("")) {
		
			Folder newFolder = _addFolder(parentFolder, newFolderName);
			
			FileTreeNode newNode = null;
			
			List<TreeNode> subNodes = parentNode.getChildren();
			if(subNodes!=null){
				for (TreeNode sub : subNodes) {
					if(sub.getData().equals(newFolderName)){
						newNode = (FileTreeNode) sub;
						break;
					}
				}
			}
			
			if(newNode == null){
				newNode =  new FileTreeNode(newFolder, parentNode);
			}
			
			selectedNode = newNode;
		}
		
		
	}

	private Folder _addFolder(Folder parentFolder, String newFolderName) {
		Folder newFolder = null;
		
		try {
			
			newFolderName = ResourceUtility.convertToLiferayDocName(newFolderName);
			
			List<Folder> subFolders = DLAppLocalServiceUtil.getFolders(parentFolder.getRepositoryId(), parentFolder.getFolderId());
			
			if(subFolders!=null){
				for (Folder sub : subFolders) {
					if(sub.getName().equals(newFolderName)){
						newFolder = sub;
						break;
					}
				}
			}
			
			if(newFolder == null){
				ServiceContext service = LiferayFacesContext.getInstance().getServiceContext();
				newFolder = DLAppLocalServiceUtil.addFolder(userId, this.groupId, parentFolder.getFolderId(), newFolderName, "", service);
			}
			
		} catch (PortalException e) {
			getLog().error("Error on add folder. " + e.getMessage());
		} catch (SystemException e) {
			getLog().error("Error on add folder. " + e.getMessage());
		}
		return newFolder;
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
	
	public ArrayList<FileEntry> getSelectedFileEntries() {
		
		TreeNode[] tempNodes = selectedNodes;

		if(tempNodes == null){
			tempNodes = new TreeNode[]{selectedNode};
		}
		
		ArrayList<FileEntry> fileEntries = new ArrayList<FileEntry>();

		for (TreeNode selectedNode : tempNodes) {
			if (selectedNode.isLeaf() && FileTreeNode.FILE_TYPE.equals(selectedNode.getType())) {
				fileEntries.add((FileEntry)((FileTreeNode)selectedNode).getContent());
			}
		}
		return fileEntries;
	}
	
	public ArrayList<FileTreeNode> getSelectedFileNodes() {
		
		TreeNode[] tempNodes = selectedNodes;

		if(tempNodes == null && selectedNode != null){
			tempNodes = new TreeNode[]{selectedNode};
		}
		
		ArrayList<FileTreeNode> fileEntries = null;

		if(tempNodes != null && tempNodes.length > 0){
			fileEntries = new ArrayList<FileTreeNode>();
			getFileEntries(Arrays.asList(tempNodes), fileEntries);
		}
		return fileEntries;
	}

	private void getFileEntries(List<TreeNode> tempNodes, ArrayList<FileTreeNode> fileEntries) {
		for (TreeNode selectedNode : tempNodes) {
			if (selectedNode.isLeaf() && FileTreeNode.FILE_TYPE.equals(selectedNode.getType())) {
				fileEntries.add((FileTreeNode)selectedNode);
			}else if(FileTreeNode.FOLDER_TYPE.equals(selectedNode.getType())){
				this.getFileEntries(selectedNode.getChildren(), fileEntries);
			}
		}
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
	
	public FileTreeNode getNodeByReference(String ref){
		
		if(ref!=null){
			String[] treeIds = ref.split("_");
			
			TreeNode target = treeRoot;
			
			for(int i = 0; i < treeIds.length; i++){
				target = target.getChildren().get(Integer.valueOf(treeIds[i]));
			}
			if(target.isLeaf() && FileTreeNode.FILE_TYPE.equals(target.getType())){
				return (FileTreeNode) target;	
			}
			
		}
		return null;
	}
	
	public List<FileTreeNode> getNodesByReference(String ref){
		
		List<FileTreeNode> ret = null;
		
		if(ref!=null){
			String[] treeIds = ref.split("_");
			
			TreeNode target = treeRoot;
			
			for(int i = 0; i < treeIds.length; i++){
				target = target.getChildren().get(Integer.valueOf(treeIds[i]));
			}
			
			ret = new ArrayList<FileTreeNode>();
			
			getLeafs(ret, target);
			
			return ret;
		}
		return null;
	}


	private void getLeafs(List<FileTreeNode> ret, TreeNode target) {
		if(!target.isLeaf()){
			for(TreeNode n : target.getChildren()){
				if(n.isLeaf() && FileTreeNode.FILE_TYPE.equals(n.getType())){
					ret.add((FileTreeNode)n);
				}else{
					getLeafs(ret, n);
				}
			}
		}else if(target.isLeaf() && FileTreeNode.FILE_TYPE.equals(target.getType())){
			ret.add((FileTreeNode)target);
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
	
	private Logger getLog(){
		return Logger.getLogger(this.getClass());
	}
	

}
