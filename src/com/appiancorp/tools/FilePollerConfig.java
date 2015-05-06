package com.appiancorp.tools;

import java.io.File;

//import com.appiancorp.suiteapi.collaboration.Folder;
//import com.appiancorp.suiteapi.collaboration.KnowledgeCenter;
import com.appiancorp.suiteapi.knowledge.KnowledgeFolder;
import com.appiancorp.suiteapi.knowledge.CommunityKnowledgeCenter;

public class FilePollerConfig {
  
  //  bean class for file poller configuration data
  private long processModelId;
  private String userName;
  private File inputDirectory;
  private int pollingInterval;
  private String name;
  private KnowledgeFolder folder;
  private Boolean fileNameIsProcessName;
  private CommunityKnowledgeCenter kc;
  private Boolean saveDocAsPv;
  private String documentVariableName;
  private Boolean saveDocAsAttachment;
  private Long folderId;
  
  public Boolean isSaveDocAsAttachment() {
    return saveDocAsAttachment;
  }
  public void setSaveDocAsAttachment(Boolean saveDocAsAttachment) {
    this.saveDocAsAttachment = saveDocAsAttachment;
  }
  public String getDocumentVariableName() {
    return documentVariableName;
  }
  public void setDocumentVariableName(String documentVariableName) {
    this.documentVariableName = documentVariableName;
  }
  public Boolean isSaveDocAsPv() {
    return saveDocAsPv;
  }
  public void setSaveDocAsPv(Boolean saveDocAsPv) {
    this.saveDocAsPv = saveDocAsPv;
  }
  public void setProcessModelId(long processModelId) {
    this.processModelId = processModelId;
  }
  public long getProcessModelId() {
    return processModelId;
  }
  
  public void setUserName(String userName) {
    this.userName = userName;
  }
  public String getUserName() {
    return userName;
  }
  public void setInputDirectory(File inputDirectory) {
    this.inputDirectory = inputDirectory;
  }
  public File getInputDirectory() {
    return inputDirectory;
  } 
  public void setPollingInterval(int pollingInterval) {
    this.pollingInterval = pollingInterval;
  }
  public int getPollingInterval() {
    return pollingInterval;
  }
  public void setName(String name) {
    this.name = name;
  }
  public String getName() {
    return name;
  }
  public void setFolder(KnowledgeFolder folderName) {
    this.folder = folderName;
  }
  public void setFolderId(Long folderId) {
	    this.folderId = folderId;
	  }
  public KnowledgeFolder getFolder() {
    return folder;
  } 
  public Long getFolderId() {
	    return folderId;
	  }
  public Boolean isFileNameIsProcessName() {
    return fileNameIsProcessName;
  }
  public void setFileNameIsProcessName(Boolean fileNameIsProcessName) {
    this.fileNameIsProcessName = fileNameIsProcessName;
  }
  public CommunityKnowledgeCenter getKc() {
    return kc;
  }
  public void setKc(CommunityKnowledgeCenter kc) {
    this.kc = kc;
  }
  

}
