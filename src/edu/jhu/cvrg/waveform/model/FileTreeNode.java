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
	
	public FileTreeNode(FileEntry fileEntry, TreeNode parentNode, FileInfoDTO dto) {
		super(fileEntry.getTitle(), parentNode);
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
	
}
