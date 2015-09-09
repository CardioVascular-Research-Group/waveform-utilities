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
 * @author Andre Vilardo, Chris Jurado
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

import com.liferay.portal.kernel.util.PropsUtil;

import edu.jhu.cvrg.data.dto.FileInfoDTO;
import edu.jhu.cvrg.data.factory.ConnectionFactory;
import edu.jhu.cvrg.data.util.DataStorageException;
import edu.jhu.cvrg.filestore.enums.EnumFileStoreType;
import edu.jhu.cvrg.filestore.filetree.FileNode;
import edu.jhu.cvrg.filestore.filetree.FileTree;
import edu.jhu.cvrg.filestore.filetree.FileTreeFactory;
import edu.jhu.cvrg.filestore.util.FileStoreConstants;
import edu.jhu.cvrg.waveform.utility.ResourceUtility;

public class LocalFileTree implements Serializable{

	private static final long serialVersionUID = 4904469355710631253L;

	private FileTreeNode treeRoot;
	private FileTreeNode userNode;
	private FileTreeNode eurekaNode;
	private FileTreeNode selectedNode;
	private TreeNode[] selectedNodes;
	private String newFolderName = "";
	private FileTree sourceFileTree;
	private FileTree eurekaFileTree;
	private Long userId = ResourceUtility.getCurrentUserId();
	private Long groupId = ResourceUtility.getCurrentGroupId();
	private String[] args;
	
	public LocalFileTree (Long user){
		initialize(user);
	}
	
	@SuppressWarnings("unused")
	private LocalFileTree(){}

	private void initialize(Long userId) {
		this.userId = userId;
		this.groupId = ResourceUtility.getCurrentGroupId();
		EnumFileStoreType type = getFileStoreType();
		sourceFileTree = FileTreeFactory.getFileTree(type, args);
		FileNode sourceRoot = sourceFileTree.getRoot();
		eurekaFileTree = FileTreeFactory.getFileTree(EnumFileStoreType.VIRTUAL_DB, args);
		FileNode eurekaRoot = eurekaFileTree.getRoot();
		if(treeRoot == null){
			treeRoot = new FileTreeNode(FileTreeNode.FOLDER_TYPE, "root", null, -1);
			treeRoot.setExpanded(true);
			userNode = new FileTreeNode(FileTreeNode.HOME_TYPE, "My Subjects", treeRoot, sourceRoot.getUuid());
			if(eurekaRoot != null){
				eurekaNode = new FileTreeNode(FileTreeNode.EUREKA_TYPE, eurekaRoot.getName(), treeRoot, eurekaRoot.getUuid());
			}
		}
		Map<Long, FileInfoDTO> fileInfoReferenceMap = this.getFileInfoReferenceMap(false);
		copyTree(userNode, sourceRoot, fileInfoReferenceMap);
		if(eurekaRoot != null){
			Map<Long, FileInfoDTO> fileInfoVirtualReferenceMap = this.getFileInfoReferenceMap(true);
			copyTree(eurekaNode, eurekaRoot, fileInfoVirtualReferenceMap);	
		}
	}

	private Map<Long, FileInfoDTO> getFileInfoReferenceMap(boolean isVirtual){
		Map<Long, FileInfoDTO> fileMap = new HashMap<Long, FileInfoDTO>();
		try {
			List<FileInfoDTO> files = null;
			if(isVirtual){
				files = ConnectionFactory.createConnection().getAllFilesReferenceByUser(userId);
			}else{
				files = ConnectionFactory.createConnection().getAllFilesByUser(userId);
			}
			if(files!= null){
				for (FileInfoDTO fileInfoDTO : files) {
					if(isVirtual){
						fileMap.put(fileInfoDTO.getDocumentRecordId(), fileInfoDTO);
					}else{
						fileMap.put(fileInfoDTO.getFileEntryId(), fileInfoDTO);	
					}
				}
			}
		} catch (DataStorageException e) {
			e.printStackTrace();
		}
		return fileMap;
	} 
	
	public void addFolder(ActionEvent event) {
		FileTreeNode parentNode = this.getSelectedNode();
		parentNode = (parentNode == null) ? userNode : parentNode;
		if(!parentNode.equals(this.getEurekaNode())){
			if(parentNode.isDocument() || newFolderName.equals(""))
				return;
			sourceFileTree.addFolder(parentNode.getUuid(), newFolderName);
			initialize(userId);
		}
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
			if (isLeafAndFile(selectedNode)) {
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
	
	public FileTreeNode getLeafByName(String nodeName){
		if(nodeName!=null){
			return getLeafByName(userNode, nodeName);
		}
		return null;
	}
	
	public FileTreeNode getNodeByReference(String ref){	
		if(ref!=null){
			String[] treeIds = ref.split("_");
			TreeNode target = treeRoot;
			for(int i = 0; i < treeIds.length; i++){
				target = target.getChildren().get(Integer.valueOf(treeIds[i]));
			}
			if(isLeafAndFile(target)){
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
				if(n.isLeaf()){
					if(((FileTreeNode)target).isDocument()){
						ret.add((FileTreeNode)n);	
					}
				}else{
					getLeafs(ret, n);
				}
			}
		}else if(isLeafAndFile(target)){
			ret.add((FileTreeNode)target);
		}
	}
	
	private FileTreeNode getLeafByName(TreeNode target, String name) {
		if(!target.isLeaf()){
			for(TreeNode n : target.getChildren()){
				if(n.isLeaf()){
					if(((FileTreeNode)target).isDocument() && n.getData().equals(name)){
						return (FileTreeNode) n;	
					}
				}else{
					FileTreeNode ret = getLeafByName(n, name);
					if(ret!=null){
						return ret;	
					}
				}
			}
		}else if(isLeafAndFile(target) && target.getData().equals(name)){
			return (FileTreeNode)target;
		}
		return null;
	}
	
	private boolean isLeafAndFile(TreeNode target){
		return target.isLeaf() && ((FileTreeNode)target).isDocument();
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

	private void copyTree(FileTreeNode newParent, FileNode sourceParent, Map<Long, FileInfoDTO> fileInfoReferenceMap){
		String type = FileTreeNode.FOLDER_TYPE;
		if(sourceParent.getChildren() == null){
			return;
		}
		List<FileNode> nodes = sourceParent.getChildren();
		if(nodes.size() == 1 && !nodes.get(0).isFolder()){
			FileNode file = nodes.get(0);
			FileInfoDTO fileDTO = fileInfoReferenceMap.get(file.getUuid());		
				if(fileDTO != null){
					file.setDocumentRecordId(fileDTO.getDocumentRecordId());
					file.setAnalysisJobId(fileDTO.getAnalysisJobId());
					new FileTreeNode(file,  newParent.getParent());
					newParent.getParent().getChildren().remove(newParent);
				}
		}else{
			for(FileNode sourceNode : sourceParent.getChildren()){
				FileTreeNode newChildNode = null;
				if(newParent!=null){
					for (TreeNode folderNode : newParent.getChildren()) {
						if(((FileTreeNode)folderNode).getFileNode().getName().equals(sourceNode.getName())){
							newChildNode = (FileTreeNode) folderNode;
							break;
						}
					}
				}
				if(newChildNode == null){
					if(fileInfoReferenceMap != null && !sourceNode.isFolder()){
						FileInfoDTO fileInfo = fileInfoReferenceMap.get(Long.valueOf(sourceNode.getUuid()));
						if(fileInfo != null){
							sourceNode.setDocumentRecordId(fileInfo.getDocumentRecordId());
							sourceNode.setAnalysisJobId(fileInfo.getAnalysisJobId());
							newChildNode = new FileTreeNode(sourceNode, newParent);		
						}
					}else{
						if(!sourceNode.isFolder()){
							type = FileTreeNode.FILE_ERROR_TYPE;
						}
						if(newChildNode == null){
							newChildNode = new FileTreeNode(type, sourceNode, newParent);	
						}
					}
				}
				copyTree(newChildNode, sourceNode, fileInfoReferenceMap);
			}
		}
	}

	public void deleteSelectedNode(){
		FileTreeNode deleteNode = this.getSelectedNode();
		if(deleteNode == null){
			return;
		} 
		if(deleteNode.getUuid() == 0L){
			return;
		}
		sourceFileTree.deleteNode(deleteNode.getUuid());
		deleteNode(deleteNode);
		
		TreeNode parentNode = deleteNode.getParent();
		if(parentNode != null){
			List<TreeNode> siblingNodes = parentNode.getChildren();
			siblingNodes.remove(deleteNode);
		}
		selectedNode = null;
	}
	
	private TreeNode deleteNode(TreeNode node){
		if(node.getChildren() != null){
			List<TreeNode> toRemove = null;
			for(TreeNode childNode : node.getChildren()){
				if(!((FileTreeNode)childNode).isDocument()){
					TreeNode n = deleteNode(childNode);
					if(n!= null){
						if(toRemove == null){
							toRemove = new ArrayList<TreeNode>();
						}
						toRemove.add(n);
					}
				}
			}
			if(toRemove != null){
				node.getChildren().removeAll(toRemove);
			}
		}
		return node;
	}
	
	public boolean fileExistsInFolder(String recordName){
		FileNode node = sourceFileTree.getFileNodeByName(recordName);
		return (node != null);
	}
	
	public String findNameByUuid(long folderUuid){
		return (String)findNodeByUuid(folderUuid, userNode).getData();	
	}
	
	private TreeNode findNodeByUuid(long folderUuid, TreeNode startNode){
		TreeNode foundNode = null;
		if(((FileTreeNode)startNode).getUuid() == folderUuid){
			return startNode;
		}
		for(TreeNode childNode : startNode.getChildren()){
			if(((FileTreeNode)childNode).getUuid() == folderUuid){
				return childNode;
			}
			if(childNode.getChildren() != null){
				foundNode = findNodeByUuid(folderUuid, childNode);
				if(foundNode != null){
					break;
				}
			}
		}
		return foundNode;
	}
	
	public EnumFileStoreType getFileStoreType(){
		String fileStoreType = "";
		fileStoreType = PropsUtil.get("file.storage");
		if(fileStoreType == null){
			Logger.getLogger(LocalFileTree.class).error("Unable to find file storage configuration.  Defaulting to Liferay 6.1");
			fileStoreType = "liferay61";
		}
		if(fileStoreType.equals("liferay61")){
			this.args = new String[]{String.valueOf(groupId), String.valueOf(userId), String.valueOf(ResourceUtility.getCurrentCompanyId()), FileStoreConstants.WAVEFORM_ROOT_FOLDER_NAME, String.valueOf(userId)};
			return EnumFileStoreType.LIFERAY_61;//default
		}
		return null;
	}
	
	public String getFolderPath(long folderUuid) throws Exception {
		StringBuilder path = new StringBuilder();
		TreeNode targetNode = findNodeByUuid(folderUuid, treeRoot);
		extractFolderHierachic(targetNode, path);
		return path.substring(6);//Substring to remove the "|root|" from the path
	}
	
	private void extractFolderHierachic(TreeNode node, StringBuilder treePath) throws Exception {
		if(node != null){
			if(node.getParent() != null){
				extractFolderHierachic(node.getParent(), treePath);
			}
			treePath.append('|').append(node.getData());
		}
	}
	
	public long getSelectedFolderUuid(){
		return (this.getSelectedNode() != null) ? this.getSelectedNode().getUuid() : userNode.getUuid();
	}
	public void setDefaultSelected(){
		this.selectedNode = this.userNode;
	}

	public FileTreeNode getEurekaNode() {
		return eurekaNode;
	}
	public FileTreeNode getMySubjectsNode() {
		return userNode;
	}
}