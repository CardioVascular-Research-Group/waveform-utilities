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
import java.util.List;
import java.util.UUID;

import javax.faces.event.ActionEvent;

import org.apache.log4j.Logger;
import org.primefaces.model.TreeNode;

import com.liferay.portal.kernel.util.PropsUtil;

import edu.jhu.cvrg.filestore.enums.EnumFileStoreType;
import edu.jhu.cvrg.filestore.filetree.FileNode;
import edu.jhu.cvrg.filestore.filetree.FileTree;
import edu.jhu.cvrg.filestore.filetree.FileTreeFactory;
import edu.jhu.cvrg.waveform.utility.ResourceUtility;

public class LocalFileTree implements Serializable{

	private static final long serialVersionUID = 4904469355710631253L;
	private Logger log = Logger.getLogger(LocalFileTree.class);
	private FileTreeNode treeRoot;
	private FileTreeNode selectedNode;
	private TreeNode[] selectedNodes;
	private String newFolderName = "";
	private FileTree sourceFileTree;
	private Long userId = ResourceUtility.getCurrentUserId();
	private Long groupId = ResourceUtility.getCurrentGroupId();
	private String[] args;
	
	public LocalFileTree (Long user){
		initialize(user);
	}

	public void initialize(Long userId) {
		this.userId = userId;
		this.groupId = ResourceUtility.getCurrentGroupId();
		EnumFileStoreType type = getFileStoreType();
		sourceFileTree = FileTreeFactory.getFileTree(type, args);
		FileNode sourceRoot = sourceFileTree.getRoot();
		treeRoot = new FileTreeNode("default", "My Files", null, sourceRoot.getUuid());
		treeRoot.setExpanded(true);
		copyTree(treeRoot, sourceRoot);
	}
	
	public void addFolder(ActionEvent event) {
		
		FileTreeNode parentNode = this.getSelectedNode();
		if (parentNode == null) {
			parentNode = treeRoot;
		} 
		if(parentNode.getType().equals("document")){
			//TODO: Some kind of error message to the user
			return;
		}
		if (newFolderName.equals("")) {
			//TODO: Some kind of error message to the user
			return;
		}
		sourceFileTree.addFolder(parentNode.getUuid(), newFolderName);
		initialize(userId);
	}	
	
	private void copyTree(FileTreeNode newParent, FileNode sourceParent){
		
		String type = "default";
		ArrayList<TreeNode> newChildren = new ArrayList<TreeNode>();
		if(sourceParent.getChildren() == null){
			return;
		}
		for(FileNode sourceNode : sourceParent.getChildren()){
//			String nodeName = "";
//			if(sourceNode.isFolder()){
//				nodeName = ((Folder)sourceNode.getContent()).getName();
//				
//			}
//			else{
//				nodeName = ((FileEntry)sourceNode.getContent()).getTitle();
//			}
			String nodeName = sourceNode.getName();
			if(!sourceNode.isFolder()){
				type = "document";
			}
			FileTreeNode newChildNode = new FileTreeNode(type, nodeName, newParent, sourceNode.getUuid());
			newChildren.add(newChildNode);
			copyTree(newChildNode, sourceNode);
		}
		newParent.setChildren(newChildren);
	}
	
	public void deleteFolder(ActionEvent event){

		FileTreeNode deleteNode = this.getSelectedNode();
		if(deleteNode == null){
			return;
		} 
		if(deleteNode.getUuid() == null){
			return;
		}
		
		sourceFileTree.deleteNode(deleteNode.getUuid());
//		deleteNode(deleteNode);
		initialize(userId);
	}
	
	private void deleteNode(TreeNode node){
		if(node.getChildren() != null){
			for(TreeNode childNode : node.getChildren()){
				deleteNode(childNode);
			}
		}
		TreeNode parentNode = node.getParent();
		if(parentNode != null){
			List<TreeNode> siblingNodes = parentNode.getChildren();
			siblingNodes.remove(node);
		}
	}
	
	public boolean fileExistsInFolder(String fileName, UUID folderUuid){
		
		TreeNode targetNode = findNodeByUuid(folderUuid, treeRoot);
		if(targetNode == null){
			return false;
		}
		for(TreeNode childNode : targetNode.getChildren()){
			if(childNode.getData().equals(fileName)){
				return true;
			}
		}
		return false;
	}
	
	public String findNameByUuid(UUID folderUuid){
		return (String)findNodeByUuid(folderUuid, treeRoot).getData();	
	}
	
	private TreeNode findNodeByUuid(UUID folderUuid, TreeNode startNode){

		TreeNode foundNode = null;
		
		if(((FileTreeNode)startNode).getUuid().equals(folderUuid)){
			return startNode;
		}

		for(TreeNode childNode : startNode.getChildren()){
			if(((FileTreeNode)childNode).getUuid().equals(folderUuid)){
				return childNode;
			}

			if(childNode.getChildren() != null){
				foundNode = findNodeByUuid(folderUuid, childNode);
			}
		}
		return foundNode;
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
	
	public EnumFileStoreType getFileStoreType(){
		
		String fileStoreType = "";
		try{
			fileStoreType = PropsUtil.get("file.storage");
		}
		catch(Exception e){
			log.error("Unable to find file storage configuration.  Defaulting to Liferay 6.1");
		}
		if(fileStoreType.equals("liferay61")){
			this.args = new String[]{String.valueOf(groupId), String.valueOf(userId), "waveform", String.valueOf(userId)};
			return EnumFileStoreType.LIFERAY_61;
		}

		return EnumFileStoreType.LIFERAY_61;//default
	}	
	
	public String getFolderPath(UUID folderUuid){
		
		String path = "";
		
		TreeNode targetNode = findNodeByUuid(folderUuid, treeRoot);
		
		path = path + (String)targetNode.getData() + "|";

		TreeNode parentNode = targetNode.getParent();
		while(parentNode != null){
			path = path + (String)parentNode.getData() + "|";
			parentNode = parentNode.getParent();
		}

		return path;
	}

	public FileTreeNode getLeafByName(String nodeName){
		
		if(nodeName!=null){
			return getLeafByName(treeRoot, nodeName);
		}
		return null;
	}
	
	private FileTreeNode getLeafByName(TreeNode target, String name) {
		if(!target.isLeaf()){
			for(TreeNode n : target.getChildren()){
				if(n.isLeaf()){
					if(FileTreeNode.FILE_TYPE.equals(n.getType()) && n.getData().equals(name)){
						return (FileTreeNode) n;	
					}
				}else{
					FileTreeNode ret = getLeafByName(n, name);
					if(ret!=null){
						return ret;	
					}
				}
			}
		} 
		else if(target.isLeaf() && FileTreeNode.FILE_TYPE.equals(target.getType()) && target.getData().equals(name)){
			return (FileTreeNode)target;
		}
		return null;
	}

	private void getLeafs(List<FileTreeNode> ret, TreeNode target) {
		if(!target.isLeaf()){
			for(TreeNode n : target.getChildren()){
				if(n.isLeaf()){
					if(FileTreeNode.FILE_TYPE.equals(n.getType())){
						ret.add((FileTreeNode)n);	
					}
				}
				else{
					getLeafs(ret, n);
				}
			}
		}
		else if(target.isLeaf() && FileTreeNode.FILE_TYPE.equals(target.getType())){
			ret.add((FileTreeNode)target);
		}
	}
	
	public String getNewFolderName() {
		return newFolderName;
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

	public UUID getSelectedFolderUuid(){
		return this.selectedNode.getUuid();
	}

	public FileTreeNode getSelectedNode(){
		return this.selectedNode;
	}
	
	public TreeNode[] getSelectedNodes() {
		return selectedNodes;
	}
	
	public FileTreeNode getTreeRoot() {
		return treeRoot;
	}

	public void selectAllChildNodes(TreeNode startingNode){

		for(TreeNode node : startingNode.getChildren()){
			node.setSelected(true);
			if(node.getType().equals(FileTreeNode.FOLDER_TYPE)){
				selectAllChildNodes(node);
			}
		}
	}
	
	public void setDefaultSelected(){
		this.selectedNode = this.treeRoot;
	}
	
	public void setNewFolderName(String newFolderName) {
		this.newFolderName = newFolderName;
	}

	public void setSelectedNode(FileTreeNode selectedNode) {
		this.selectedNode = selectedNode;
	}

	public void setSelectedNodes(TreeNode[] selectedNodes) {
		this.selectedNodes = selectedNodes;
	}
	
	public void unSelectAllChildNodes(TreeNode startingNode){
		for(TreeNode node : startingNode.getChildren()){
			node.setSelected(false);
			if(node.getType().equals(FileTreeNode.FOLDER_TYPE)){
				unSelectAllChildNodes(node);
			}
		}
	}
//	private Map<Long, FileInfoDTO> extractFileIdList(List<FileInfoDTO> files){
//	Map<Long, FileInfoDTO> fileMap = new HashMap<Long, FileInfoDTO>();
//	if(files!= null){
//		for (FileInfoDTO fileInfoDTO : files) {
//			fileMap.put(fileInfoDTO.getFileEntryId(), fileInfoDTO);
//		}
//	}
//	return fileMap;
//} 
//	
//	private Logger getLog(){
//		return Logger.getLogger(this.getClass());
//	}
//	
//	private Folder findUserRootFolder(){
//
//		try {
//			System.out.println("userId is " + userId);
//
//			List<Folder> folders = DLAppLocalServiceUtil.getFolders(groupId, getRootFolderId());
//			for (Folder folder : folders) {
//				System.out.println("Folder name is " + folder.getName());
//				if (folder.getName().equals(String.valueOf(userId))) {
//					return folder;
//				}
//			}
//
//		} catch (SystemException e) {
//			getLog().error("Unable to retrieve folder " + String.valueOf(userId));
//			e.printStackTrace();
//		} catch (PortalException e) {
//			e.printStackTrace();
//		}
//
//		return null;
//	}
	

//	private void getFolderContent(Map<Long, FileInfoDTO> map, Folder folder, FileTreeNode node){
//		try {
//			FileTreeNode folderNode = null;
//			if(userRootFolder.equals(folder.getName())){
//				folderNode = node;
//			}else{
//				folderNode = new FileTreeNode(folder, node);
//			}
//			
//			List<Folder> subFolders = DLAppLocalServiceUtil.getFolders(groupId, folder.getFolderId());
//			if(subFolders != null){
//				for (Folder folder2 : subFolders) {
//					getFolderContent(map, folder2, folderNode);
//				}						
//			}
//			
//			List<FileEntry> subFiles = DLAppLocalServiceUtil.getFileEntries(groupId, folder.getFolderId());
//			if(subFiles != null && subFiles.size() > 0){
//				List<FileEntry> nodeList = new ArrayList<FileEntry>();
//				for (FileEntry file : subFiles) {
//					if(extentionFilter == null || extentionFilter.equalsIgnoreCase(file.getExtension())){
//						nodeList.add(file);
//					}
//				}
//				
//				if(nodeList.size() == 1){
//					FileEntry file = nodeList.get(0);
//					FileInfoDTO fileDTO = map.get(file.getFileEntryId());
//					if(fileDTO != null){
//						new FileTreeNode(file, folderNode.getParent(), fileDTO, hideExtension);
//					}else{
//						new FileTreeNode(file, folderNode.getParent(), null, hideExtension);
//					}
//					folderNode.getParent().getChildren().remove(folderNode);
//				}else{
//					for (FileEntry file : nodeList) {
//						FileInfoDTO fileDTO = map.get(file.getFileEntryId());
//						if(fileDTO != null){
//							new FileTreeNode(file, folderNode, fileDTO, hideExtension);
//						}else{
//							new FileTreeNode(file, folderNode, null, hideExtension);
//						}	
//					}
//				}
//			}
//		} catch (PortalException e) {
//			getLog().error("Erro on tree loading. "+ e.getMessage());
//		} catch (SystemException e) {
//			getLog().error("Erro on tree loading. "+ e.getMessage());
//		}
//	}
	
//	public Long getSelectedNodeId() {
//
//		FileTreeNode node = this.getSelectedNode();
//		
//		if (node == null) {
//			node = treeRoot;
//		}
//		
//		if(node.getContent() instanceof String){
//			return getFolderIdByName(node.getContent().toString());
//		}
//		else if(node.getContent() instanceof Folder){
//			return ((Folder) node.getContent()).getFolderId();
//		}else{
//			return ((FileEntry) node.getContent()).getFileEntryId();
//		}
//	}
	
//	private long getFolderIdByName(String name){
//		try {
//			return DLAppLocalServiceUtil.getFolder(groupId, findUserRootFolder().getFolderId(), name).getFolderId();
//		} catch (PortalException e) {
//			e.printStackTrace();
//		} catch (SystemException e) {
//			e.printStackTrace();
//		}
//		return 0L;
//	}
	
//	public ArrayList<FileEntry> getSelectedFileEntries() {
//	
//	TreeNode[] tempNodes = selectedNodes;
//
//	if(tempNodes == null){
//		tempNodes = new TreeNode[]{selectedNode};
//	}
//	
//	ArrayList<FileEntry> fileEntries = new ArrayList<FileEntry>();
//
//	for (TreeNode selectedNode : tempNodes) {
//		if (selectedNode.isLeaf() && FileTreeNode.FILE_TYPE.equals(selectedNode.getType())) {
//			fileEntries.add((FileEntry)((FileTreeNode)selectedNode).getContent());
//		}
//	}
//	return fileEntries;
//}
	
//	public Folder getSelectFolder(){
//	
//	FileTreeNode selectedNode = this.getSelectedNode();
//	
//	if (selectedNode == null) {
//		selectedNode = treeRoot;
//	}
//	
//	if(FileTreeNode.FOLDER_TYPE.equals(selectedNode.getType())){
//		return (Folder) selectedNode.getContent();
//	}else{
//		return null;
//	}
//}
//	private String extentionFilter;
//	
//	private boolean hideExtension = false;
	
//	private String userRootFolder;
}
