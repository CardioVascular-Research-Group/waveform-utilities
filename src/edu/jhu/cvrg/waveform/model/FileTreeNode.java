package edu.jhu.cvrg.waveform.model;

import java.util.UUID;

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
	private UUID uuid;
	private FileNode fileNode;
	
	public FileNode getFileNode(){
		return this.fileNode;
	}

	public FileTreeNode(String type, String fileNodeName, TreeNode parentNode, UUID uuid){
		super(type, fileNodeName, parentNode);
		this.uuid = uuid;
	}
	
	public FileTreeNode(String type, Folder folder, TreeNode parentNode) {
		super(type, folder.getName(), parentNode);
		this.fileNode = new FileNode(null, folder.getName(), 0L, 0L);
	}
	
	public FileTreeNode(String type, FileEntry folder, TreeNode parentNode) {
		super(type, folder.getTitle(), parentNode);
		this.fileNode = new FileNode(null, folder.getTitle(), 0L, 0L);
	}
	
	public FileTreeNode(Folder folder, TreeNode parentNode) {
		super(folder.getName(), parentNode);
		this.setType(FOLDER_TYPE);
		this.fileNode = new FileNode(null, folder.getName(), 0L, 0L);
	}
	
	public FileTreeNode(FileEntry fileEntry, TreeNode parentNode, FileInfoDTO fileDTO, boolean hideExtension) {
		super(getNodeName(fileEntry, hideExtension), parentNode);

		if(fileDTO != null){
			this.fileNode = new FileNode(((FileTreeNode)parentNode).getFileNode(), fileEntry.getTitle(), fileDTO.getDocumentRecordId(), fileDTO.getAnalysisJobId());

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

	private static String getNodeName(FileEntry fileEntry, boolean hideExtension) {
		if(hideExtension){
			return fileEntry.getTitle().substring(0, fileEntry.getTitle().lastIndexOf('.'));
		}else{
			return fileEntry.getTitle();
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

	public Long getDocumentRecordId() {
		return this.fileNode.getDocumentRecordId();
	}

	public Long getAnalysisJobId() {
		return this.fileNode.getAnalysisJobId();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((fileNode.getAnalysisJobId() == 0L) ? 0 : Long.valueOf(fileNode.getAnalysisJobId()).hashCode());
		result = prime
				* result
				+ ((fileNode.getDocumentRecordId() == 0L) ? 0 : Long.valueOf(fileNode.getDocumentRecordId()).hashCode());
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
		if (fileNode.getAnalysisJobId() == 0L) {
			if (other.getFileNode().getAnalysisJobId() != 0L)
				return false;
		} else if (fileNode.getAnalysisJobId() != other.getFileNode().getAnalysisJobId())
			return false;
		if (fileNode.getDocumentRecordId() == 0L) {
			if (other.getFileNode().getDocumentRecordId() != 0L)
				return false;
		} else if (fileNode.getDocumentRecordId() != (other.getFileNode().getDocumentRecordId()))
			return false;
		return true;
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}		
}
