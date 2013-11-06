package edu.jhu.cvrg.waveform.model;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.Folder;

public class FileTreeNode extends DefaultTreeNode{

	protected static final String FILE_TYPE = "document";
	protected static final String FOLDER_TYPE = "default";

	private static final long serialVersionUID = -8272995370554100396L;
	
	private Object content;
	
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
	
	public FileTreeNode(FileEntry folder, TreeNode parentNode) {
		super(folder.getTitle(), parentNode);
		this.setType(FILE_TYPE);
		this.setContent(folder);
	}

	public Object getContent() {
		return content;
	}

	public void setContent(Object content) {
		this.content = content;
	}	
	
}