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
import edu.jhu.cvrg.data.dto.DocumentRecordDTO;

public class DocumentDragVO {

	private DocumentRecordDTO documentRecord;
	private FileTreeNode fileNode;
	
	public DocumentDragVO(FileTreeNode fileNode, DocumentRecordDTO doumentRecord) {
		this.fileNode = fileNode;
		this.documentRecord = doumentRecord;
	}
	
	@SuppressWarnings("unused")
	private DocumentDragVO(){}
	
	public DocumentRecordDTO getDocumentRecord() {
		return documentRecord;
	}
	public void setDocumentRecord(DocumentRecordDTO doumentRecord) {
		this.documentRecord = doumentRecord;
	}
	public FileTreeNode getFileNode() {
		return fileNode;
	}
	public void setFileNode(FileTreeNode fileNode) {
		this.fileNode = fileNode;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fileNode == null) ? 0 : fileNode.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DocumentDragVO other = (DocumentDragVO) obj;
		if (fileNode == null) {
			if (other.fileNode != null)
				return false;
		} else if (!fileNode.equals(other.fileNode))
			return false;
		return true;
	}	
}