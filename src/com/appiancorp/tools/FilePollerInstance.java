package com.appiancorp.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import org.apache.log4j.Logger;

import com.appiancorp.services.ServiceContext;
import com.appiancorp.services.ServiceContextFactory;
import com.appiancorp.suiteapi.common.ServiceLocator;
import com.appiancorp.suiteapi.common.exceptions.InvalidPriorityException;
import com.appiancorp.suiteapi.common.exceptions.InvalidProcessModelException;
import com.appiancorp.suiteapi.common.exceptions.InvalidStateException;
import com.appiancorp.suiteapi.common.exceptions.InvalidUserException;
import com.appiancorp.suiteapi.common.exceptions.InvalidVersionException;
import com.appiancorp.suiteapi.common.exceptions.PrivilegeException;
import com.appiancorp.suiteapi.common.exceptions.ProcessAttachmentsLimitException;
import com.appiancorp.suiteapi.common.exceptions.StorageLimitException;
import com.appiancorp.suiteapi.content.ContentConstants;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.content.exceptions.DuplicateUuidException;
import com.appiancorp.suiteapi.content.exceptions.InsufficientNameUniquenessException;
import com.appiancorp.suiteapi.content.exceptions.InvalidContentException;
import com.appiancorp.suiteapi.knowledge.Document;
import com.appiancorp.suiteapi.personalization.User;
import com.appiancorp.suiteapi.process.ProcessDesignService;
import com.appiancorp.suiteapi.process.ProcessVariable;
import com.appiancorp.suiteapi.type.NamedTypedValue;
import com.appiancorp.suiteapi.process.exceptions.InvalidProcessException;
import com.appiancorp.suiteapi.type.AppianType;
import com.appiancorp.suiteapi.process.ProcessStartConfig;

public class FilePollerInstance extends Thread {

  private FilePollerConfig config;
  private ServiceContext _sc;
  private ProcessDesignService _pds;
  private ContentService _ds;
  private static final Logger log = Logger.getLogger("com.appiancorp.filepoller");
  
  public FilePollerInstance (FilePollerConfig config){
    this.config = config;
    String userName = config.getUserName();
    _sc = ServiceContextFactory.getServiceContext(userName);
    _pds= ServiceLocator.getProcessDesignService(_sc);
    _ds = ServiceLocator.getContentService(_sc);
    ServiceLocator.waitForServers();
  }

  public Document createDocumentFromFile(File sourceFile){

    Long docFolder = config.getFolderId();
    Document newDoc = new Document();

    try{
    	//System.out.println("creating document from a file.");
      String fileName = sourceFile.getName();
      //System.out.println("filename  = \""+fileName+"\"");
      int pos = fileName.lastIndexOf(".");
      newDoc.setName(fileName.substring(0,pos));
      newDoc.setExtension(fileName.substring(pos + 1, fileName.length()));
      newDoc.setDescription("");
      newDoc.setState(Document.STATE_PUBLISHED);
      // casting long to integer means files over 2gb not possible. 1GB is max supported by Appian at present
      newDoc.setSize((int) sourceFile.length());
      newDoc.setParent(docFolder);
      newDoc.setFileSystemId(ContentConstants.ALLOCATE_FSID);
      Long newDocId = _ds.create(newDoc, ContentConstants.UNIQUE_NONE);
      Document[] newDocs = _ds.download(newDocId,ContentConstants.VERSION_CURRENT,true); 
      newDoc = newDocs[0];
      
      


    }
        catch(PrivilegeException e){
          log.error(e,e);
        } catch(InvalidUserException e){
          log.error(e,e);
        } catch (InvalidContentException e) {
        	log.error(e,e);
		} catch (StorageLimitException e) {
			log.error(e,e);
		} catch (InsufficientNameUniquenessException e) {
			log.error(e,e);
		} catch (DuplicateUuidException e) {
			log.error(e,e);
		} catch (InvalidVersionException e) {
			log.error(e,e);
		}
    return newDoc;
  }

  public void run(){

    long pollingInterval = (long)config.getPollingInterval();
    Long processModelId = new Long(config.getProcessModelId());
    File inputDirectory = config.getInputDirectory();
    Boolean saveDocAsPv = config.isSaveDocAsPv();
    User processStarter = new User();
    processStarter.setUsername(config.getUserName());
    Document doc;
    ProcessVariable[] Vars = new ProcessVariable[0];
    String docVariableName = config.getDocumentVariableName();
    System.out.println(config.getName()+ " is listening...");
    
    while(true){
    	
    	
      File[] newFiles = inputDirectory.listFiles();

      if (newFiles==null) {
    	  System.out.println("Warning: input directory "+ inputDirectory +" does not exist");
    	  newFiles=new File[0];
      } else {
        	log.info("Input dir is: "+inputDirectory.getName());
        	for(int c = 0; c < newFiles.length; c++) {
        		System.out.println(config.getName() + " has found file "+newFiles[c].getName()+ " at position "+c);
        	}
        }

      for (int i = 0; i < newFiles.length; i++){
        File currentFile = newFiles[i];

        // already been deleted by another File Poller
        if (!currentFile.exists()) {
        	continue;
        }

        String fileName = currentFile.getName();
        //System.out.println("Current File name b4 call to createDocfromfile: \""+fileName+"\"");
        FileChannel channel;
        RandomAccessFile raf = null;
        FileLock lock = null;

        try{
          raf = new RandomAccessFile(currentFile, "rw");
          channel = raf.getChannel();

          try {
        	  lock = channel.tryLock();
          } catch (Exception e) {
        	  // Do nothing
        	 e.printStackTrace();
          }
          if (lock == null) {
        	  System.out.println("File is already locked by another FilePoller: \""+fileName+"\"");
		  raf.close();
        	  continue;
          }

          doc = createDocumentFromFile(currentFile);
          //System.out.println("doc created from file:");
          System.out.println("Uploading doc " + doc.getName() + ", doc size " + doc.getSize());

          try {
        	  FileInputStream fis = new FileInputStream(raf.getFD());
        	  com.appiancorp.tools.FileUploader.uploadFile(fis, doc);
        	  lock.release();
        	  channel.close();
        	  lock = null;
        	  channel = null;

		for (int k=0; k<5; k++) {
			currentFile.delete();

			if (currentFile.exists()) {
				if (k == 4) {
					throw new IOException("File could not be deleted: \""+fileName+"\"");
				}

				Thread.sleep(1000);
			}
		}
          } catch (Exception e) {
        	  // If there was any issue copying the file into Appian, delete the "document" from Appian
        	  _ds.delete(doc.getId(), false);

        	  // Throw the caught exception so it can be handled by other catch statements below
        	  throw e;
          }

          if (saveDocAsPv.equals(Boolean.TRUE)){
        	NamedTypedValue docVar = new NamedTypedValue(docVariableName,new Long(AppianType.DOCUMENT),doc.getId());
            ProcessVariable docPv = new ProcessVariable(docVar);
            docPv.setParameter(true);
            Vars = new ProcessVariable[1];
            Vars[0] = docPv;
          }

          ProcessStartConfig config = new ProcessStartConfig(Vars);
          _pds.initiateProcess(processModelId, config);
          
        }
        catch(PrivilegeException e){
          log.error(e,e);
          e.printStackTrace();
        }
        catch(InvalidUserException e){
          log.error(e,e);
          e.printStackTrace();
        }
        catch(InvalidProcessModelException e){
          log.error(e,e);
          e.printStackTrace();
        }
        catch(InvalidStateException e){
          log.error(e,e);
          e.printStackTrace();
        }
        catch(StorageLimitException e){
          log.error(e,e);
          e.printStackTrace();
        }
        catch(InvalidProcessException e){
          log.error(e,e);
          e.printStackTrace();
        }
        catch(InvalidPriorityException e){
          log.error(e,e);
          e.printStackTrace();
        }
        catch(ProcessAttachmentsLimitException e){
          log.error(e,e);
          e.printStackTrace();
        }
        catch(FileNotFoundException e){
          log.error(e,e);
          e.printStackTrace();
        }
        catch(IOException e){
          log.error(e,e);
          e.printStackTrace();
        }
        catch(Exception e){
          log.error(e,e);
          e.printStackTrace();
        }

		// Make sure the lock gets released if an exception is thrown
      	if (lock != null) {
      		try {
				lock.release();
				raf.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
      	}
        }
        try{
          Thread.sleep(pollingInterval * 1000);
        }
        catch(Exception e){
          log.error(e,e);
        }
      }
    }
  }
