package net.jotorren.microservices.content.domain;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class SourceCodeItem implements Serializable {
	private static final long serialVersionUID = 7803210035898675537L;
	
	@Id
	@Column(nullable=false)
	private String itemId;
	
	@Column(nullable=false)
	private String fileName;
	
	@Column(nullable=false)
	private String fileLocation;
	
	@Column
	private String fileContent;
	
	@Column
	private String fileDescription;
	
	@Column(nullable=false)
	private String fileOwner;

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFileLocation() {
		return fileLocation;
	}

	public void setFileLocation(String fileLocation) {
		this.fileLocation = fileLocation;
	}

	public String getFileContent() {
		return fileContent;
	}

	public void setFileContent(String fileContent) {
		this.fileContent = fileContent;
	}

	public String getFileDescription() {
		return fileDescription;
	}

	public void setFileDescription(String fileDescription) {
		this.fileDescription = fileDescription;
	}

	public String getFileOwner() {
		return fileOwner;
	}

	public void setFileOwner(String fileOwner) {
		this.fileOwner = fileOwner;
	}

	public String getItemId() {
		return itemId;
	}

	public void setItemId(String itemId) {
		this.itemId = itemId;
	}
}
