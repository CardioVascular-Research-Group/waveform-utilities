package edu.jhu.cvrg.waveform.model;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.Folder;

import edu.jhu.cvrg.data.dto.FileInfoDTO;
import edu.jhu.cvrg.filestore.filetree.FileNode;

public class FileTreeNode extends DefaultTreeNode implements TreeNode{

	protected static final String FILE_ERROR_TYPE = "document_error";
	protected static final String FILE_ANALYSIS_TYPE = "analysis";
	protected static final String FILE_TYPE = "document";
	protected static final String FOLDER_TYPE = "default";
	private static final long serialVersionUID = -8272995370554100396L;
	private long uuid;
	private FileNode fileNode;
	
	public FileNode getFileNode(){
		return this.fileNode;
	}

	public FileTreeNode(FileNode sourceNode, TreeNode parentNode){
		super(getNodeName(sourceNode.getName(), true), parentNode);
		
		this.fileNode = sourceNode;

		if(fileNode.getAnalysisJobId() != null){
			this.setType(FILE_ANALYSIS_TYPE);
		}else if(fileNode.getDocumentRecordId() != null){
				this.setType(FILE_TYPE);
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
	
	public FileTreeNode(String type, Folder folder, TreeNode parentNode) {
		super(type, folder.getName(), parentNode);
		this.fileNode = new FileNode(null, folder.getName(), 0L);
	}
	
	public FileTreeNode(String type, FileEntry folder, TreeNode parentNode) {
		super(type, folder.getTitle(), parentNode);
		this.fileNode = new FileNode(null, folder.getTitle(), 0L);
	}
	
	public FileTreeNode(Folder folder, TreeNode parentNode) {
		super(folder.getName(), parentNode);
		this.setType(FOLDER_TYPE);
		this.fileNode = new FileNode(null, folder.getName(), 0L);
	}
	
	public FileTreeNode(FileEntry fileEntry, TreeNode parentNode, FileInfoDTO fileDTO, boolean hideExtension) {
		super(getNodeName(fileEntry.getTitle(), hideExtension), parentNode);

		if(fileDTO != null){
			this.fileNode = new FileNode(((FileTreeNode)parentNode).getFileNode(), fileEntry.getTitle(), 0L);

			this.fileNode.setAnalysisJobId(fileDTO.getAnalysisJobId());
			this.fileNode.setDocumentRecordId(fileDTO.getDocumentRecordId());
			
			if(fileNode.getAnalysisJobId() != 0L){
				this.setType(FILE_ANALYSIS_TYPE);
			}else if(fileNode.getDocumentRecordId() != 0L){
					this.setType(FILE_TYPE);
			}else{
				this.setType(FILE_ERROR_TYPE);
			}
		}else{
			this.setType(FILE_ERROR_TYPE);
		}
	}

	private static String getNodeName(String filename, boolean hideExtension) {
		if(hideExtension){
			return filename.substring(0, filename.lastIndexOf('.'));
		}else{
			return filename;
		}
	}
	
	public String getTreePath(){
		StringBuilder sb = new StringBuilder();
		
		this.getPath(getParent(), sb);
		
		return sb.toString();
	}
		
	private void getPath(TreeNode node, StringBuilder path){
		if(node != null){
			if(node.getParent() !=null ){
				getPath(node.getParent(), path);
				path.append('/').append(node.getData());
			}
		}
	}

	public Object getContent() {
		return this.fileNode.getContent();
	}
	
	public long getDocumentRecordId(){
		return this.fileNode.getDocumentRecordId();
	}

	public Long getAnalysisJobId() {
		return this.fileNode.getAnalysisJobId();
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
		return FileTreeNode.FILE_TYPE.equals(this.getType());
	}
	
	
}
