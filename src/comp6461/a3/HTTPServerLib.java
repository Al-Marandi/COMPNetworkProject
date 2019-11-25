package comp6461.a3;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
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
	
	DatagramSocket serverUDPsocket;
	DatagramPacket recivedUDPacket;
	DatagramPacket sendUDPacket;
	String routerIP;
	int routerPort;
	String response;

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
	
	/**
	 * Server constructor
	 * @param socket: UDP socket for the server
	 * @param packet: the packet that received by the server
	 */
	public HTTPServerLib(DatagramSocket socket, DatagramPacket packet, String routerIP , int routerPort, String dir) {
		this.serverUDPsocket = socket;
		this.recivedUDPacket = new DatagramPacket(packet.getData(), packet.getLength());
		this.recivedUDPacket.setAddress(packet.getAddress());this.recivedUDPacket.setPort(packet.getPort());
		this.routerIP = routerIP;
		this.routerPort = routerPort;
		this.response = "";
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
			//reader = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
			//writer = new PrintWriter(serverSocket.getOutputStream(),true);
			
			
			//-- get raw data of byte[] from packet
			byte[] rawData = this.recivedUDPacket.getData();

			//-- create packet class to extract data
			Packet recivedPacket = Packet.fromBytes(rawData);
			
//			//-- get request and create response
//			String payload = new String(recivedPacket.getPayload());
//			System.out.println("Request received from the client is: "+ payload.trim());
//			String response = payload.trim() + " - SERVER RESONSE";
			
			String requestPayload = new String(recivedPacket.getPayload()).trim();
			
			
			
			
			//-- read received request and handle it based on the request type
//			while((request = reader.readLine())!=null) {
//				
//				if(request.endsWith("HTTP/1.0")) {
//					httpRequest = request;
//					isHttpRequest = true;
//				}				
//				else if(request.matches("(GET|POST)/(.*)")) {
//					fmsRequest = request;
//					isFMSRequest = true;
//				}	
//				
//				if(isFMSRequest) {
//					if(request.startsWith("-d")) {
//						content = request.substring(2);
//						System.out.println("Content: " + content);
//					}
//					else if(request.isEmpty()) {
//						break;
//					}
//				}
//				
//			}
//			
//			if(isFMSRequest) {
//				System.out.println("server received the request: " + fmsRequest);
//	
//				if(fmsRequest.startsWith("GET")) {
//					this.processGetRequest(fmsRequest.substring(4));
//				}else if(fmsRequest.startsWith("POST")) {
//					String fileName = fmsRequest.substring(5);
//					this.processPostRequest(fileName, content);
//				}
//			}		
//			
//			writer.println("");
//			writer.flush();
//			reader.close();
//			serverSocket.close();
			
			
			
			

		for(String request : requestPayload.split("\r\n")) {
					
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
					System.out.println("Content: " + content);
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
//		
//		writer.println("");
//		writerPrintln("");
//		System.out.println("response: ");
//		System.out.println(this.response);
//		System.out.println(this.response.getBytes().length);
//		writer.flush();
//		reader.close();
//		serverSocket.close();
		byte[] responseBytes = this.response.getBytes();
		byte[] payload = new byte[200];
		int j = 0;
		for(int i = 0; i < responseBytes.length  ; i++) {
			payload[j] = responseBytes[i];
			j++;
			if(j == 200 || i == responseBytes.length -1) {				
				// send payload
				//-- convert response to packet
				Packet responsePacket = new Packet(0, recivedPacket.getSequenceNumber(), recivedPacket.getPeerAddress(), recivedPacket.getPeerPort(), payload);
				
				//-- convert response packet to byte[]
				byte[] responseData = responsePacket.toBytes();
				
				//-- create datagram packet from byte[]
				this.sendUDPacket = new DatagramPacket(responseData, responseData.length, InetAddress.getByName(this.routerIP), this.routerPort);
				
				//-- send reply to udp port of the sender
				this.serverUDPsocket.send(this.sendUDPacket);
				
				// reset payload
				payload = new byte[200];				
				j = 0;				
			}
		}
			
		}catch (Exception e) {
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
			if(contents.length == 0) {
				System.out.println("Error HTTP 404");
				//writer.println("Error HTTP 404: File Not Found");
				writerPrintln("Error HTTP 404: File Not Found");
				return;
			}
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
							//writer.println(fTitle);
							writerPrintln(fTitle);
							System.out.println(fTitle);
							fCount ++;
						}

//						writer.println("... <" + c + "> directory's contents has been listed");
//						writer.println();
						writerPrintln("... <" + c + "> directory's contents has been listed");
						writerPrintln("");

					}				
					//-- if path is a point to the file system then return the file content if it is exist
					else if(path.isFile() && (!fileName.isEmpty())) {
						FileReader fReader;					
						try {						
								fReader = new FileReader(path);
								BufferedReader bfReader = new BufferedReader(fReader);
								String line;					
								while ((line = bfReader.readLine()) != null) {
									//writer.println(line);
									writerPrintln(line);
								}	
//								writer.println("... <" + c + "> has been read completely");
//								writer.println();
								writerPrintln("... <" + c + "> has been read completely");
								writerPrintln("");
								bfReader.close();
						} catch (FileNotFoundException e) {
							System.out.println("Error HTTP 404");
							//writer.println("Error HTTP 404: File Not Found");
							writerPrintln("Error HTTP 404: File Not Found");
						} catch (IOException e) {
							e.printStackTrace();
						}
		
					}
				} 
				else {
					System.out.println("Error HTTP 404");
					//writer.println("Error HTTP 404: File not found !!!");
					writerPrintln("Error HTTP 404: File not found !!!");
				}
			}
		}else {
			System.out.println("Access to this directory is denied");
			//writer.println("Error: access to this directory is denied !!!");
			writerPrintln("Error: access to this directory is denied !!!");
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
				//writer.println("... " + fileName + " has been created");
				writerPrintln("... " + fileName + " has been created");
				fWriter.close();
			} catch (FileNotFoundException e) {
				System.out.println("Error HTTP 404");
				//writer.println("Error HTTP 404: File not found !!!");
				writerPrintln("Error HTTP 404: File not found !!!");
			}
		}else {
			System.out.println("Access to this directory is denied !!!");
			//writer.println("Error: access to this directory is denied !!!");
			writerPrintln("Error: access to this directory is denied !!!");
		}
	}
	
	public String[] finder( String dirName, String target){
		if(target.isEmpty()) {
			String[] list = new String[1];
			list[0] = "";
			return list;
		}
        File dir = new File(dirName);
        return dir.list(new FilenameFilter() { 
                 public boolean accept(File dir, String filename)
                      { return (filename.equalsIgnoreCase(target)|| filename.startsWith(target + ".")); }
        } );

    }
	
	public  void writerPrintln(String s) {
		this.response = this.response + s + "\r\n";
	}
}

