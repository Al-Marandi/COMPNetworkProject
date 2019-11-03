package comp6461.a2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Iterator;
import java.io.FilenameFilter;

public class HTTPServerLib extends Thread{
	
	//====================================================================== fields
	Socket serverSocket;
	BufferedReader reader = null; 
	PrintWriter writer = null; 
	String request;	
	String workingDir;
	String fmsRequest;
	String httpRequest;
	String content;	
	boolean isHttpRequest = false;
	boolean isFMSRequest = false;			


	//======================================================================= constructor
	/**
	 * Server constructor
	 * @param socket: tcp socket for the server
	 * @param dir: the working directory witch is set for the server
	 */
	public HTTPServerLib(Socket socket, String dir) {
		this.serverSocket = socket;
		this.workingDir = dir;			
	}

	//======================================================================= main method run in server thread
	/**
	 * This method is handle main functionality of the server as while as the thread is active
	 */
	public void run(){
		try
		{				
            //-- connect socket reader and writer to the server socket
			reader = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
			writer = new PrintWriter(serverSocket.getOutputStream(),true);
			
			//-- read received request and handle it based on the request type
			while((request = reader.readLine())!=null) {
				
				if(request.endsWith("HTTP/1.0")) {
					httpRequest = request;
					isHttpRequest = true;
				}				
				else if(request.matches("(GET|POST)/(.*)")) {
					fmsRequest = request;
					isFMSRequest = true;
				}	
				
				if(isFMSRequest) {
					if(request.startsWith("-d")) {
						content = request.substring(2);
						System.out.println("content: " + content);
					}
					else if(request.isEmpty()) {
						break;
					}
				}
				
			}
			
			if(isFMSRequest) {
				System.out.println("server received the request: " + fmsRequest);
	
				if(fmsRequest.startsWith("GET")) {
					this.processGetRequest(fmsRequest.substring(4));
				}else if(fmsRequest.startsWith("POST")) {
					String fileName = fmsRequest.substring(5);
					this.processPostRequest(fileName, content);
				}
			}		
			
			writer.println("");
			writer.flush();
			reader.close();
			serverSocket.close();
			
		}catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	
	/**
	 * This function is responsible for handling get file management system request
	 * @param fileName: name of the file to get the content throw the Get command
	 */
	public synchronized void processGetRequest(String fileName) {
		File path;		
		//-- check that the file name is not a path out of working directory
		if(!fileName.contains("/")) {
			String[] contents = finder(workingDir + "/", fileName);
			for (String c : contents) {
				path = new File(workingDir + "/" + c);
				if(path.exists()) {
					//-- if path is a point to the system directory then list the files or paths inside that directory
					if(path.isDirectory()) {		
						File[] fLists = path.listFiles();
						String fTitle;
						int fCount = 1;
						for (File f : fLists) {
							if (f.isFile()) {
								fTitle = "file " + fCount + ": " + f.getName();							
							} 
							else {
								fTitle = "path " + fCount + ": " + f.getName();
							}
							writer.println(fTitle);
							System.out.println(fTitle);
							fCount ++;
						}
						writer.println("... <" + c + "> directory's contents has been listed");
						writer.println();
					}
					//-- if path is a point to the file system then return the file content if it is exist
					else if(path.isFile()) {
						FileReader fReader;					
						try {						
								fReader = new FileReader(path);
								BufferedReader bfReader = new BufferedReader(fReader);
								String line;					
								while ((line = bfReader.readLine()) != null) {
									writer.println(line);
								}	
								writer.println("... <" + c + "> has been read completely");
								writer.println();
								bfReader.close();
						} catch (FileNotFoundException e) {
							System.out.println("Error HTTP 404");
							writer.println("Error HTTP 404 : File Not Found");
						} catch (IOException e) {
							e.printStackTrace();
						}
		
					}
				} 
				else {
					System.out.println("Error HTTP 404");
					writer.println("Error HTTP 404: File not found !!!");
				}
			}
		}else {
			System.out.println("access to this directory is denied");
			writer.println("Error: access to this directory is denied !!!");
		}
	}
	
	

	/**
	 * This function is responsible for handling post file management system request
	 * @param fileName: name of the file to get the content throw the Get command
	 * @param fileContent: content of the file that should be written in the selected file
	 */
	public synchronized void processPostRequest(String fileName, String fileContent) {
		File path;
		PrintWriter fWriter;		
		path = new File(workingDir + "/" + fileName);
		
		//-- check that the file name is not a path out of working directory
		if(!fileName.contains("/")) {
			try {				
				fWriter = new PrintWriter(path);
				fWriter.println(fileContent);
				writer.println("... " + fileName + " has been created");
				fWriter.close();
			} catch (FileNotFoundException e) {
				System.out.println("Error HTTP 404");
				writer.println("Error HTTP 404: File not found !!!");
			}
		}else {
			System.out.println("access to this directory is denied !!!");
			writer.println("Error: access to this directory is denied !!!");
		}
	}
	
	public String[] finder( String dirName, String taget){
        File dir = new File(dirName);
        return dir.list(new FilenameFilter() { 
                 public boolean accept(File dir, String filename)
                      { return filename.startsWith(taget); }
        } );

    }
}

