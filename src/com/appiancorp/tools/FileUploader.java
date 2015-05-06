package com.appiancorp.tools;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;




public final class FileUploader {
	@SuppressWarnings("unused")
	private static final int DEFAULT_RW_BUFFER_SIZE = 32768;
	  
	  public static void uploadFile(InputStream inStream_, com.appiancorp.suiteapi.knowledge.Document doc_)
	    throws Exception
	  {
		  uploadFile(inStream_, doc_, false);
	  }
	  
	  public static void uploadFile(byte[] file_, com.appiancorp.suiteapi.knowledge.Document doc_)
	    throws Exception
	  {
		  InputStream is = new ByteArrayInputStream(file_);
	    uploadFile(is, doc_);
	  }
	  
	  public static void uploadFile(InputStream inStream_, com.appiancorp.suiteapi.knowledge.Document doc_, boolean replace_)
	    throws Exception
	  {
		  String internalFilename = getFilePathForDocument(doc_);
		  uploadFile(inStream_, internalFilename, replace_);
	  }
	  
	  protected static void uploadFile(InputStream is, String path, boolean replace)
	    throws Exception
	  {
		  if (is == null) {
	      throw new NullPointerException("Input stream of file to upload is null.");
	    }
	    File docFile = getFileForDocument(path, replace);
	    FileOutputStream fos = new FileOutputStream(docFile);
	    copyFile(is, fos);
	    fos.close();
	  }
	  
	  private static String getFilePathForDocument(com.appiancorp.suiteapi.knowledge.Document doc)
	  {
		  String internalFilename = doc.getInternalFilename();
		  if (internalFilename == null) {
	      throw new IllegalArgumentException("The property \"internalFilename\" is null in the given Document bean argument: " + doc);
	    }
	    return internalFilename;
	  }
	  
	  private static File getFileForDocument(String path_, boolean replace_)
	  {
		File localServerFile = new File(path_);
	    if ((!replace_) && (localServerFile.exists())) {
	      throw new IllegalArgumentException("Attempt to write over a file that already exists: " + localServerFile.toString());
	    }
	    localServerFile = localServerFile.getAbsoluteFile();
	    File parentDir = new File(localServerFile.getParent());
	    if (!parentDir.exists()) {
	      parentDir.mkdirs();
	    }
	    return localServerFile;
	  }
	  
	  public static OutputStream getOutputStreamForDocument(com.appiancorp.suiteapi.knowledge.Document doc)
	    throws FileNotFoundException
	  {
		String internalFilename = getFilePathForDocument(doc);
	    File docFile = getFileForDocument(internalFilename, false);
	    FileOutputStream fos = new FileOutputStream(docFile);
	    return fos;
	  }
	  
	  public static void copyFile(InputStream src_, OutputStream dst_)
	    throws IOException
	  {
		byte[] buffer = new byte[32768];
	    int bytesRead = 0;
	    while ((bytesRead = src_.read(buffer, 0, 32768)) != -1) {
	      dst_.write(buffer, 0, bytesRead);
	    }
	  }
	  
	  public static byte[] readFileToByteArray(String filePath_)
	    throws IOException
	  {
		InputStream is = null;
	    byte[] bytes = null;
	    try
	    {
	      is = new FileInputStream(filePath_);
	      bytes = new byte[is.available()];
	      is.read(bytes);
	    }
	    finally
	    {
	      if (is != null) {
	        is.close();
	      }
	    }
	    return bytes;
	  }
	}
