package edu.jhu.cvrg.waveform.model;
/*
Copyright 2015 Johns Hopkins University Institute for Computational Medicine

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
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import edu.jhu.cvrg.filestore.filetree.FileNode;

public class FileTreeNode extends DefaultTreeNode implements TreeNode{

	private static final long serialVersionUID = -8272995370554100396L;
	
	public static final String FILE_ERROR_TYPE = "document_error";
	public static final String FILE_ANALYSIS_TYPE = "analysis";
	public static final String FILE_TYPE = "document";
	public static final String FOLDER_TYPE = "default";
	public static final String HOME_TYPE = "home";
	public static final String EUREKA_TYPE = "eureka";
	public static final String GROUP_TYPE = "group";
	
	private long uuid;
	private FileNode fileNode;

	public FileTreeNode(FileNode sourceNode, TreeNode parentNode){
		super(getNodeName(sourceNode.getName(), true), parentNode);
		this.fileNode = sourceNode;
		if(fileNode.getDocumentRecordId() != null){
			this.setType(FILE_TYPE);
		}else if(fileNode.getAnalysisJobId() != null){
			this.setType(FILE_ANALYSIS_TYPE);
		}else{
			this.setType(FILE_ERROR_TYPE);
		}
		this.uuid = sourceNode.getUuid();
	}
	
	public FileTreeNode(String type, FileNode sourceNode, TreeNode parentNode){
		super(type, sourceNode.getName(), parentNode);
		this.uuid = sourceNode.getUuid();
		this.fileNode = sourceNode;
	}
	
	public FileTreeNode(String type, String fileNodeName, TreeNode parentNode, long uuid){
		super(type, fileNodeName, parentNode);
		this.uuid = uuid;
	}

	public Long getAnalysisJobId() {
		return this.fileNode.getAnalysisJobId();
	}
	
	public Object getContent() {
		return this.fileNode.getContent();
	}
	
	public long getDocumentRecordId(){
		return this.fileNode.getDocumentRecordId();
	}

	public FileNode getFileNode(){
		return this.fileNode;
	}

	private static String getNodeName(String filename, boolean hideExtension) {
		if(hideExtension && filename.lastIndexOf('.') != -1){
			return filename.substring(0, filename.lastIndexOf('.'));
		}else{
			return filename;
		}
	}
	
	private void getPath(TreeNode node, StringBuilder path){
		if(node != null){
			if(node.getParent() !=null ){
				getPath(node.getParent(), path);
				path.append('/').append(node.getData());
			}
		}
	}
	
	public String getTreePath(){
		StringBuilder sb = new StringBuilder();
		this.getPath(getParent(), sb);
		return sb.toString();
	}

	public long getUuid() {
		return uuid;
	}

	public void setUuid(long uuid) {
		this.uuid = uuid;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (int) (uuid ^ (uuid >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileTreeNode other = (FileTreeNode) obj;
		if (uuid != other.uuid)
			return false;
		return true;
	}
	
	public boolean isDocument(){
		return FileTreeNode.FILE_TYPE.equals(this.getType()) || FileTreeNode.FILE_ANALYSIS_TYPE.equals(this.getType());
	}	
}