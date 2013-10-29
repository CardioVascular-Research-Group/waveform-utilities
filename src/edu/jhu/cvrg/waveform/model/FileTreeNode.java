package edu.jhu.cvrg.waveform.model;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.Folder;

public class FileTreeNode extends DefaultTreeNode{

	private static final long serialVersionUID = -8272995370554100396L;
	
	private Object content;
	
	public FileTreeNode(Folder folder, TreeNode parentNode) {
		super(folder.getName(), parentNode);
		this.setContent(folder);
	}
	
	public FileTreeNode(FileEntry folder, TreeNode parentNode) {
		super(folder.getTitle(), parentNode);
		this.setContent(folder);
	}

	public Object getContent() {
		return content;
	}

	public void setContent(Object content) {
		this.content = content;
	}	
	
}
