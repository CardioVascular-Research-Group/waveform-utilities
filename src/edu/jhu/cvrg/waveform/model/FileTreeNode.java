package edu.jhu.cvrg.waveform.model;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.Folder;

import edu.jhu.cvrg.dbapi.dto.FileInfoDTO;

public class FileTreeNode extends DefaultTreeNode implements TreeNode{

	protected static final String FILE_ERROR_TYPE = "document_error";
	protected static final String FILE_ANALYSIS_TYPE = "analysis";
	protected static final String FILE_TYPE = "document";
	protected static final String FOLDER_TYPE = "default";

	private static final long serialVersionUID = -8272995370554100396L;
	
	private Object content;
	private Long documentRecordId;
	private Long analysisJobId;
	
	public FileTreeNode(String type, Folder folder, TreeNode parentNode) {
		super(type, folder.getName(), parentNode);
		this.setContent(folder);
	}
	
	public FileTreeNode(String type, FileEntry folder, TreeNode parentNode) {
		super(type, folder.getTitle(), parentNode);
		this.setContent(folder);
	}
	
	public FileTreeNode(Folder folder, TreeNode parentNode) {
		super(folder.getName(), parentNode);
		this.setType(FOLDER_TYPE);
		this.setContent(folder);
	}
	
	public FileTreeNode(FileEntry fileEntry, TreeNode parentNode, FileInfoDTO dto, boolean hideExtension) {
		super(getNodeName(fileEntry, hideExtension), parentNode);
		
		this.setParent(parentNode);	
		
		this.setContent(fileEntry);
		if(dto != null){
			this.documentRecordId = dto.getDocumentRecordId();
			this.analysisJobId = dto.getAnalysisJobId();
			
			if(analysisJobId != null){
				this.setType(FILE_ANALYSIS_TYPE);
			}else if(documentRecordId != null){
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

	public Object getContent() {
		return content;
	}

	public void setContent(Object content) {
		this.content = content;
	}

	public Long getDocumentRecordId() {
		return documentRecordId;
	}

	public Long getAnalysisJobId() {
		return analysisJobId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((analysisJobId == null) ? 0 : analysisJobId.hashCode());
		result = prime
				* result
				+ ((documentRecordId == null) ? 0 : documentRecordId.hashCode());
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
		if (analysisJobId == null) {
			if (other.analysisJobId != null)
				return false;
		} else if (!analysisJobId.equals(other.analysisJobId))
			return false;
		if (documentRecordId == null) {
			if (other.documentRecordId != null)
				return false;
		} else if (!documentRecordId.equals(other.documentRecordId))
			return false;
		return true;
	}	
	
}
