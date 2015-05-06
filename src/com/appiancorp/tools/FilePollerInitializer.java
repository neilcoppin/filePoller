package com.appiancorp.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import com.appiancorp.common.config.CacheConfigLoader;
import com.appiancorp.services.ServiceContext;
import com.appiancorp.services.ServiceContextFactory;
import com.appiancorp.suiteapi.content.Content;
import com.appiancorp.suiteapi.content.ContentConstants;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.common.ServiceLocator;
import com.appiancorp.suiteapi.common.exceptions.InvalidProcessModelException;
import com.appiancorp.suiteapi.common.exceptions.InvalidUserException;
import com.appiancorp.suiteapi.common.exceptions.InvalidVersionException;
import com.appiancorp.suiteapi.common.exceptions.PrivilegeException;
import com.appiancorp.suiteapi.personalization.UserService;
import com.appiancorp.suiteapi.process.ProcessDesignService;
import com.appiancorp.suiteapi.process.ProcessModel;
import com.appiancorp.suiteapi.process.ProcessModel.Descriptor;
import com.appiancorp.suiteapi.process.ProcessVariable;
//import com.appiancorp.suiteapi.content.ContentFilter;
import com.appiancorp.suiteapi.content.exceptions.InvalidContentException;
//import com.appiancorp.suiteapi.content.exceptions.InvalidTypeMaskException;

public class FilePollerInitializer {
  private ArrayList<FilePollerConfig> pollerConfigs;
  private static final Logger log = Logger.getLogger("com.appiancorp.filepoller");

  public FilePollerInitializer(){
    pollerConfigs = new ArrayList<FilePollerConfig>();
  }
  
  public ArrayList<FilePollerConfig> Initialize(){
    FilePollerConfig poller = new FilePollerConfig();
    File configFile = new File("classes" + File.separator + "filePoller-config.xml");
    Element name = null;
    Element processModelId = null;
    Element processModelName = null;
    Element userName = null;
    Element inputDirectory = null;
    Element pollingInterval = null;
    Element knowledgeCenter = null;
    Element knowledgeCenterId = null;
    Element docFolder = null;
    Element docFolderId = null;
    Element saveDocAsPv = null;
    Element documentVariableName = null;
    String userNameString = "";
    //String kcName = "";
    String folderName = "";
    String pollerName = "";
    String inputFileName = "";
    String pmName = "";
    File inputFile = null;
    long pmid = -1;  
    int interval;
    ServiceContext _sc;
    ContentService _kcs;
    ProcessDesignService _pds;
    UserService _us;
    SAXBuilder builder = new SAXBuilder();
    Date date = new Date();
    
    try{
    	System.out.println(date.toString() + ": About to Initialize Default Cache");
    	CacheConfigLoader.initializeDefaultCache();
    	System.out.println(date.toString() + ": Default Cache initialized");
    	
      //Parse xml document
    	System.out.println(date.toString() + ": Parsing XML doc");
      org.jdom.Document xmlDoc = builder.build(configFile);
      Element root = xmlDoc.getRootElement();
      @SuppressWarnings("unchecked")
	List<Element> pollers = root.getChildren();
      if (pollers.size() == 0){
    	  System.out.println(date.toString() + ": file poller config empty");
        log.error("file poller config empty");
        System.exit(0);
        }
      for (int i = 0; i < pollers.size(); i++){
        poller = new FilePollerConfig();
        
        //  use admin service context to determine if specified user exists
        _sc = ServiceLocator.getAdministratorServiceContext();
        _us = ServiceLocator.getUserService(_sc);
        
        //get username to use in execution of process
        userName = ((Element)pollers.get(i)).getChild("user-name");
        userNameString = userName.getText();
        if (_us.doesUserExist(userNameString)){
          poller.setUserName(userNameString);
        }
        else{
        	System.out.println("User " + userNameString + " does not exist");
          log.error("User " + userNameString + " does not exist");
          continue;
        }
        // use service context of specified user for everything else
        _sc = ServiceContextFactory.getServiceContext(userNameString);
        _kcs = ServiceLocator.getContentService(_sc);
        _pds= ServiceLocator.getProcessDesignService(_sc);
        Locale loc = Locale.US;
        
        //get poller name
        name = ((Element)pollers.get(i)).getChild("name");
        pollerName = name.getText();
        poller.setName(pollerName);
        System.out.println(date.toString() + ": Setting up file poller named " + pollerName);
        
        //get process model to pass file to
        processModelId = ((Element)pollers.get(i)).getChild("process-model-id");
        if (!(processModelId == null)){
          pmid = new Integer(processModelId.getText()).longValue();
          poller.setProcessModelId(pmid); 
          }
          else{
            processModelName = ((Element)pollers.get(i)).getChild("process-model-name");
            if (!(processModelName == null)){
              pmName = processModelName.getText();
              Descriptor[] myModels = _pds.getProcessModelsICanStart();            
              for (int j = 0; j < myModels.length; j++){
                if (myModels[j].getName().get(loc).equals(pmName)){
                  pmid =myModels[j].getId().longValue();
                  poller.setProcessModelId(pmid); 
                  break;
                }  
              }
             }
            else{
            	System.out.println("Either process model name or id must be specified for file poller " + pollerName);
              log.error("Either process model name or id must be specified for file poller " + pollerName);
              continue;
             }
          }
        System.out.println(date.toString() + ": Pointing poller at ProcessModel ID " + poller.getProcessModelId());
    
          //get input directory
          inputDirectory = ((Element)pollers.get(i)).getChild("input-directory");
        inputFileName = inputDirectory.getText();
        inputFile = new File(inputFileName);
        if (inputFile.exists() && inputFile.isDirectory()){
          poller.setInputDirectory(inputFile);
        }
        else {
        	System.out.println(date.toString() + ": Input Directory " + inputFileName + "specified for file poller " + pollerName + " does not exist or is not a directory");
          log.error("Input Directory " + inputFileName + "specified for file poller " + pollerName + " does not exist or is not a directory");
          continue;
        }
        System.out.println(date.toString() + ": Found input directory at " + inputFileName);
        
        //set polling interval
        pollingInterval = ((Element)pollers.get(i)).getChild("polling-interval");
        interval = new Integer(pollingInterval.getText()).intValue();
        poller.setPollingInterval(interval);
        System.out.println(date.toString() + ": Polling every " + interval + " seconds");
       
        /*
         * This could need tidying up so that Constants.COUNT_ALL is not used.
         * */
        //search for knowledge center
        /*knowledgeCenter = ((Element)pollers.get(i)).getChild("knowledge-center");
        kcName = knowledgeCenter.getText();
        System.out.println(date.toString() + ": Attempting to locate KC " + kcName);
        ContentFilter filterKC = new ContentFilter(ContentConstants.TYPE_ANY_KC);
        filterKC.setName(kcName);
        Long[] knowledgeCenters = _kcs.getAdministratable(ContentConstants.KNOWLEDGE_ROOT, filterKC);
        Long kcId = knowledgeCenters[0];
        System.out.println(date.toString() + ": Found KC ID " + kcId);
        
        if (kcId == null){
        	System.out.println(date.toString() + ": KnowledgeCenter named "+ kcName + " specified for file poller " + pollerName + " not found");
          log.error("KnowledgeCenter named "+ kcName + "specified for file poller " + pollerName + " not found");
          continue;
         }*/
        
        //set KC
        knowledgeCenterId = ((Element)pollers.get(i)).getChild("knowledge-center-id");
        String kcIdString = knowledgeCenterId.getText();
        Long kcId = Long.parseLong(kcIdString);
        Content kc = _kcs.getVersion(kcId, ContentConstants.VERSION_CURRENT);
        
        //check actual KC name against specified name and id
        knowledgeCenter = ((Element)pollers.get(i)).getChild("knowledge-center");
        if (knowledgeCenter.getText().equals(kc.getDisplayName())){
        	System.out.println(date.toString() + ": Actual KC name matches specified KC name");
         }
        else {
        	System.out.println(date.toString() + ": Knowledge Center name does not match config ID");
            log.error("Knowledge Center name does not match config ID");
            continue;
         }
        
        
        /*
        //revised find folder to load files to
        docFolder = ((Element)pollers.get(i)).getChild("document-creation-folder");
        folderName = docFolder.getText();
        ContentFilter filterFolders = new ContentFilter(ContentConstants.TYPE_FOLDER);
        filterFolders.setName(folderName);
        Long[] folderResults = _kcs.getAdministratable(kcId, filterFolders);
        Long folderId = folderResults[0];
        boolean folderExists = false;
        if(folderId != null){
        	folderExists = true;
        	poller.setFolderId(folderId);
        }
        
        if (!folderExists){
        	System.out.println(date.toString() + ": Folder named "+ folderName + " for file poller " + pollerName + " not found");
          log.error("Folder named "+ folderName + " for file poller " + pollerName + " not found");
          continue;
          }    
        System.out.println(date.toString() + ": Found folder ID " + folderId);
        */
        
        //get folder
        docFolderId = ((Element)pollers.get(i)).getChild("document-creation-folder-id");
        String docFolderIdString = docFolderId.getText();
        Long folderId = Long.parseLong(docFolderIdString);
        if(folderId != null){
        	Content folder = _kcs.getVersion(folderId, ContentConstants.VERSION_CURRENT);
        	docFolder = ((Element)pollers.get(i)).getChild("document-creation-folder");
        	if(docFolder.getText().equals(folder.getDisplayName())){
        		System.out.println(date.toString() + ": Actual folder name matches specified folder name");
        		poller.setFolderId(folderId);
        	}
        }
        else{
        	System.out.println(date.toString() + ": Folder named "+ folderName + " for file poller " 
        			+ pollerName + " not found or does not match specified folder");
            log.error("Folder named "+ folderName + " for file poller " + pollerName 
            		+ " not found or does not match specified folder");
            continue;
        }
        
        
        //Should the document be saved to process variable
        saveDocAsPv = ((Element)pollers.get(i)).getChild("save-doc-as-pv");
        if (saveDocAsPv == null){
          poller.setSaveDocAsPv(Boolean.FALSE);
        }
        else{
          poller.setSaveDocAsPv(new Boolean(saveDocAsPv.getText()));
        }
        
        //Set the name of the process variable
        documentVariableName = ((Element)pollers.get(i)).getChild("doc-variable-name");
        if (documentVariableName == null){
          if (poller.isSaveDocAsPv().equals(Boolean.TRUE)){
        	  System.out.println("Process Variable name required to save document as PV in file poller " + pollerName);
            log.error("Process Variable name required to save document as PV in file poller " + pollerName);
            continue;
          }
          poller.setDocumentVariableName("");
        }
        else{
          String pvName = documentVariableName.getText();
          //  verify that PV exists in model and is parameter
          try{
            ProcessModel model = _pds.getProcessModel(new Long(pmid));
            ProcessVariable[] allPvs = model.getVariables();
            ProcessVariable docPv = null;
            boolean pvExists = false;
            for (int j = 0; j < allPvs.length; j++){
              if (pvName.equals(allPvs[j].getFriendlyName())){
                pvExists = true;
                docPv = allPvs[j];
                break;
              }
            }
            if (!pvExists){
            	System.out.println("Process Variable " + pvName + " is not defined for process model" + pmid);
              log.error("Process Variable " + pvName + " is not defined for process model" + pmid);
              continue;
            }
            else if (!(docPv.isParameter())){
            	System.out.println("Process Variable " + pvName + " is not a parameter for process model" + pmid);
              log.error("Process Variable " + pvName + " is not a parameter for process model" + pmid);
              continue;
            }
          }
          catch(PrivilegeException e){
        	  System.out.println(e);
            log.error(e,e);
            continue;
          }
          catch(InvalidProcessModelException e){
        	  System.out.println(e);
            log.error(e,e);
            continue;
          }
          poller.setDocumentVariableName(pvName);
        }
       System.out.println(date.toString() + ": Config setup complete for " + pollerName); 
       System.out.println();
       pollerConfigs.add(poller);
      }
    }   
      catch(InvalidUserException e){
        log.error(e,e);
      }
      catch (JDOMException e){
        log.error(e,e);
      }
      catch(IOException e){
        log.error(e,e);
      } catch (InvalidContentException e) {
    	  log.error(e,e);
	} catch (InvalidVersionException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (PrivilegeException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}     
	
	return pollerConfigs;    
  } 
}
