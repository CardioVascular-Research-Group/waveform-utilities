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
	
//	private String extentionFilter;
	
//	private boolean hideExtension = false;
	
	public LocalFileTree (Long user){
		initialize(user);
	}
	
	
//	public LocalFileTree (Long user, String extentionFilter){
//		this.extentionFilter = extentionFilter;
//		this.hideExtension = (extentionFilter != null);
//		initialize(user);
//	}
	
	public void initialize(Long userId) {
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

//	private void buildTree(Connection con) {
//		
//		try {
//			 
//			 Folder rootFolder = getRootFolder();
//			
//			if(rootFolder != null){
//				
//				treeRoot = new FileTreeNode(rootFolder, null);
//				treeRoot.setExpanded(true);
//				
//				List<FileInfoDTO> files = con.getAllFilesByUser(userId);
//				
//				this.getFolderContent(this.extractFileIdList(files), rootFolder, treeRoot);
//			}else{
//				getLog().error(WAVEFORM_ROOT_FOLDER + " folder does not exist");
//			}
//			
//		} catch (PortalException e) {
//			getLog().error("Erro on tree loading. "+ e.getMessage());
//		} catch (SystemException e) {
//			getLog().error("Erro on tree loading. "+ e.getMessage());
//		} catch (DataStorageException e) {
//			getLog().error("Erro on tree loading. "+ e.getMessage());
//		}
//	}


//	private Folder getRootFolder() throws PortalException, SystemException {
//		Folder rootFolder = null;
//		List<Folder> rootFolders = DLAppLocalServiceUtil.getFolders(groupId, 0L);
//		
//		for (Folder folder : rootFolders) {
//			if(WAVEFORM_ROOT_FOLDER.equals(folder.getName())){
//				rootFolder = folder;
//				break;
//			}
//		}
//		
//		rootFolder = _addFolder(rootFolder, USER_ROOT_FOLDER);
//		
//		return rootFolder;
//	}

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
	
	
//	private void getFolderContent(Map<Long, FileInfoDTO> map, Folder folder, FileTreeNode node){
//		try {
//			FileTreeNode folderNode = null;
//			if(USER_ROOT_FOLDER.equals(folder.getName())){
//				folderNode = node;
//			}else{
//				for (TreeNode n : node.getChildren()) {
//					FileTreeNode tn = (FileTreeNode) n;
//					if(tn.getContent() instanceof Folder && folder.getName().equals(((Folder)tn.getContent()).getName())){
//						folderNode = (FileTreeNode)n;
//						break;
//					}
//				}
//				if(folderNode == null){
//					folderNode = new FileTreeNode(folder, node);
//				}
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
//					boolean skip = false;
//					for (TreeNode n : folderNode.getParent().getChildren()) {
//						FileTreeNode tn = (FileTreeNode) n;
//						if(tn.getContent() instanceof FileEntry && file.getTitle().equals(((FileEntry)tn.getContent()).getTitle())){
//							skip = true;
//							break;
//						}
//					}
//					if(!skip){
//						new FileTreeNode(file, folderNode.getParent(), fileDTO, hideExtension);
//					}
//					folderNode.getParent().getChildren().remove(folderNode);
//				}else{
//					for (FileEntry file : nodeList) {
//						FileInfoDTO fileDTO = map.get(file.getFileEntryId());
//						boolean skip = false;
//						for (TreeNode n : folderNode.getChildren()) {
//							if(file.getTitle().equals(((FileEntry)n.getData()).getTitle())){
//								skip = true;
//								break;
//							}
//						}
//						if(!skip){
//							new FileTreeNode(file, folderNode, fileDTO, hideExtension);
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
//		if(node.getContent() instanceof Folder){
//			return ((Folder) node.getContent()).getFolderId();
//		}else{
//			return ((FileEntry) node.getContent()).getFileEntryId();
//		}
//		
//	}
	
//	public FileTreeNode addNode(FileEntry file, FileTreeNode parent, FileInfoDTO fileDTO){
//		return new FileTreeNode(file, parent, fileDTO, hideExtension);
//	}

	public void addFolder(ActionEvent event) {
	
		FileTreeNode parentNode = this.getSelectedNode();
		if (parentNode == null) {
			parentNode = userNode;
		}
		
		if(!parentNode.equals(this.getEurekaNode())){
			if(parentNode.isDocument()){
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
	}

//	private Folder _addFolder(Folder parentFolder, String newFolderName) {
//		Folder newFolder = null;
//		
//		try {
//			
//			newFolderName = ResourceUtility.convertToLiferayDocName(newFolderName);
//			
//			List<Folder> subFolders = DLAppLocalServiceUtil.getFolders(parentFolder.getRepositoryId(), parentFolder.getFolderId());
//			
//			if(subFolders!=null){
//				for (Folder sub : subFolders) {
//					if(sub.getName().equals(newFolderName)){
//						newFolder = sub;
//						break;
//					}
//				}
//			}
//			
//			if(newFolder == null){
//				ServiceContext service = LiferayFacesContext.getInstance().getServiceContext();
//				newFolder = DLAppLocalServiceUtil.addFolder(userId, this.groupId, parentFolder.getFolderId(), newFolderName, "", service);
//			}
//			
//		} catch (PortalException e) {
//			getLog().error("Error on add folder. " + e.getMessage());
//		} catch (SystemException e) {
//			getLog().error("Error on add folder. " + e.getMessage());
//		}
//		return newFolder;
//	}
	
//	public Folder getSelectFolder(){
//		
//		FileTreeNode selectedNode = this.getSelectedNode();
//		
//		if (selectedNode == null) {
//			selectedNode = treeRoot;
//		}
//		
//		if(FileTreeNode.FOLDER_TYPE.equals(selectedNode.getType())){
//			return (Folder) selectedNode.getContent();
//		}else{
//			return null;
//		}
//	}
	
//	public ArrayList<FileEntry> getSelectedFileEntries() {
//		
//		TreeNode[] tempNodes = selectedNodes;
//
//		if(tempNodes == null){
//			tempNodes = new TreeNode[]{selectedNode};
//		}
//		
//		ArrayList<FileEntry> fileEntries = new ArrayList<FileEntry>();
//
//		for (TreeNode selectedNode : tempNodes) {
//			if (selectedNode.isLeaf() && FileTreeNode.FILE_TYPE.equals(selectedNode.getType())) {
//				fileEntries.add((FileEntry)((FileTreeNode)selectedNode).getContent());
//			}
//		}
//		return fileEntries;
//	}
	
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
	
	private Logger getLog(){
		return Logger.getLogger(LocalFileTree.class);
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
	
	private TreeNode findNodeByName(String name, TreeNode startNode){

		TreeNode foundNode = null;
		
		if(startNode.getData().equals(name)){
			return startNode;
		}

		for(TreeNode childNode : startNode.getChildren()){
			if(childNode.getData().equals(name)){
				return childNode;
			}

			if(childNode.getChildren() != null){
				foundNode = findNodeByName(name, childNode);
				if(foundNode != null){
					break;
				}
			}
		}
		return foundNode;
	}

	
	public EnumFileStoreType getFileStoreType(){
		
		String fileStoreType = "";
		try{
			fileStoreType = PropsUtil.get("file.storage");
		}
		catch(Exception e){
			this.getLog().error("Unable to find file storage configuration.  Defaulting to Liferay 6.1");
		}
		if(fileStoreType.equals("liferay61")){
			this.args = new String[]{String.valueOf(groupId), String.valueOf(userId), String.valueOf(ResourceUtility.getCurrentCompanyId()), FileStoreConstants.WAVEFORM_ROOT_FOLDER_NAME, String.valueOf(userId)};
			return EnumFileStoreType.LIFERAY_61;
		}

		return EnumFileStoreType.LIFERAY_61;//default
	}
	
	public String getFolderPath(long folderUuid) throws Exception {
		
		StringBuilder path = new StringBuilder();
		
		TreeNode targetNode = findNodeByUuid(folderUuid, treeRoot);
		
		extractFolderHierachic(targetNode, path);
		
		//Substring to remove the "|root|" from the path
		return path.substring(6);
	}
	
	private void extractFolderHierachic(TreeNode node, StringBuilder treePath) throws Exception {
		try {
			if(node != null){
				if(node.getParent() != null){
					extractFolderHierachic(node.getParent(), treePath);
				}
				treePath.append('|').append(node.getData());
			}
		} catch (Exception e) {
			this.getLog().error("Problems with the liferay folder structure");
			throw e;
		}
	}
	
	public long getSelectedFolderUuid(){
		if(this.getSelectedNode() != null){
			return this.getSelectedNode().getUuid();
		}else{
			return userNode.getUuid();
		}
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

