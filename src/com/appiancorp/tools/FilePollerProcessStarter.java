package com.appiancorp.tools;


import java.util.ArrayList;

public class FilePollerProcessStarter { 
  
  public static void main(String[] args) {
    
 
    FilePollerInstance pollerInstance; 
    FilePollerInitializer initPoller = new FilePollerInitializer();
    
    @SuppressWarnings("rawtypes")
	ArrayList configurations = initPoller.Initialize();

    for (int i = 0; i < configurations.size(); i++){     
      pollerInstance = new FilePollerInstance((FilePollerConfig)configurations.get(i));
      pollerInstance.start();
    }
  }
}
