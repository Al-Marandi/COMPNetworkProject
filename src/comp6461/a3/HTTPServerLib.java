package comp6461.a3;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;

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
	DatagramPacket sendUDPacket;
	String routerIP;
	int routerPort;
	String response;
	long seqNumber = 1000, seq = 1001;
	long clientSeqNumber;
	boolean handshakingPhaseIDone, flag;
	boolean handshakingPhaseIIDone;
	int payloadMaxSize = 1013, windowSize = 4;
	Map<Long, Packet> packetInfo = new LinkedHashMap<Long, Packet>();

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
	public HTTPServerLib(DatagramSocket socket, String routerIP , int routerPort, String dir) {
		this.serverUDPsocket = socket;
		this.routerIP = routerIP;
		this.routerPort = routerPort;
		this.response = "";
		this.workingDir = dir;
		this.handshakingPhaseIDone = false;
		this.handshakingPhaseIIDone = false;
		this.flag = false;
	}

	//======================================================================= main method run in server thread
	/**
	 * This method is handle main functionality of the server as while as the thread is active
	 */
	public void run(){
		try
		{								
		
		byte[] buffer = new byte[Packet.MAX_LEN];
		DatagramPacket recivedUDPacket = new DatagramPacket(buffer, buffer.length);
		
		while(true) {
						
			this.serverUDPsocket.receive(recivedUDPacket);
			
			//-- get raw data of byte[] from packet
			byte[] rawData = recivedUDPacket.getData();
			
			//-- create packet class to extract data
			Packet receivedPacket = Packet.fromBytes(rawData);
			
			//-- get request and create response			
			String requestPayload = new String(receivedPacket.getPayload()).trim();
			
			if(receivedPacket.getType() == Packet.Type.SYNType.getPacketType()) {
				
				this.handshakingPhaseIDone = false;
				this.handshakingPhaseIIDone = false;
				
				this.clientSeqNumber = receivedPacket.getSequenceNumber() + 1;
				byte [] message = ("SYN-ACK:" + this.clientSeqNumber).toString().trim().getBytes();
		
				//-- convert response to packet
				Packet responsePacket = new Packet(Packet.Type.SYNACKType.getPacketType(), this.seqNumber, receivedPacket.getPeerAddress(), receivedPacket.getPeerPort(), message);
				
				//-- convert response packet to byte[]
				byte[] responseData = responsePacket.toBytes();
				
				//-- create datagram packet from byte[]
				DatagramPacket sendUDPacket = new DatagramPacket(responseData, responseData.length, InetAddress.getByName(routerIP), routerPort);
				
				//-- send reply to udp port of the sender
				this.serverUDPsocket.send(sendUDPacket);
				seqNumber++;
				this.handshakingPhaseIDone = true;
				
			}
			
			if(this.handshakingPhaseIDone && receivedPacket.getType() == Packet.Type.ACKType.getPacketType() && requestPayload.trim().equalsIgnoreCase("ACK:"+this.seq)) {
				System.out.println("Connection Established between client and server.");
				this.handshakingPhaseIIDone = true;
				this.flag = true;
			}
			
			if(this.handshakingPhaseIDone && this.handshakingPhaseIIDone) {
				String originalResp = requestPayload.trim();
				if(flag && packetInfo.size() > 0) {
					sendPacket(seq);
					this.flag = false;
				}
				
				if(receivedPacket.getType() == Packet.Type.ACKDataType.getPacketType()) {
					String[] data = originalResp.split(":");
					long currentSequence = Integer.parseInt(data[1]);
					sendPacket(currentSequence);
				}
				
			}
		
		
			if(receivedPacket.getType() == Packet.Type.DataType.getPacketType()) {
				for(String request : requestPayload.split("\n")) {
					
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
						}
						else if(request.isEmpty()) {
							break;
						}
					}
					
				}
				
				if(isFMSRequest) {
					if(fmsRequest.startsWith("GET")) {
						this.processGetRequest(fmsRequest.substring(4));
					}else if(fmsRequest.startsWith("POST")) {
						String fileName = fmsRequest.substring(5);
						this.processPostRequest(fileName, content);
					}
				}		
	
				byte[] responseBytes = this.response.getBytes();
				byte[] payload = new byte[payloadMaxSize];
				int j = 0;
				for(int i = 0; i < responseBytes.length  ; i++) {
					payload[j] = responseBytes[i];
					j++;
					if(j == payloadMaxSize || i == responseBytes.length -1) {				
						// send payload
						//-- convert response to packet
						//increase sequence number
						Packet responsePacket = new Packet(Packet.Type.DataType.getPacketType(), seqNumber, receivedPacket.getPeerAddress(), receivedPacket.getPeerPort(), payload);
						packetInfo.put(seqNumber, responsePacket);
						//-- convert response packet to byte[]
						
						//-- create datagram packet from byte[]
//						this.sendUDPacket = new DatagramPacket(responseData, responseData.length, InetAddress.getByName(this.routerIP), this.routerPort); //comment
						
						//-- send reply to udp port of the sender
						seqNumber++;
						// reset payload
						payload = new byte[payloadMaxSize];				
						j = 0;				
					}
				}
				byte [] msg = new byte[4];
				msg = ("Done").toString().trim().getBytes();
				Packet ACKPacket = new Packet(Packet.Type.DataType.getPacketType(), seqNumber, receivedPacket.getPeerAddress(), receivedPacket.getPeerPort(), msg);
				packetInfo.put(seqNumber, ACKPacket);
				
				if(this.handshakingPhaseIDone && this.handshakingPhaseIIDone) {
					sendPacket(seq);
					this.flag = false;
				}
			}	
		}
					
		}catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public synchronized void sendPacket(long seqNumber) throws IOException {
		int counter = 0;
		for(Map.Entry<Long, Packet> entry : packetInfo.entrySet()) {

			if(counter < windowSize && entry.getKey()>=seqNumber) {	
				//-- convert response packet to byte[]
				byte[] responseData = new byte[entry.getValue().toBytes().length]; 
				responseData = entry.getValue().toBytes(); 
				
				//-- create datagram packet from byte[]
				this.sendUDPacket = new DatagramPacket(responseData, responseData.length, InetAddress.getByName(this.routerIP), this.routerPort); 
				
				//-- send reply to udp port of the sender
				this.serverUDPsocket.send(this.sendUDPacket); 
				counter++;
				if(entry.getValue().getPayload().toString().equalsIgnoreCase("Done"))
				{
					break;
				}
			}
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
							writerPrintln(fTitle);
							System.out.println(fTitle);
							fCount ++;
						}

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
									writerPrintln(line);
								}	
								writerPrintln("... <" + c + "> has been read completely");
								writerPrintln("");
								bfReader.close();
						} catch (FileNotFoundException e) {
							System.out.println("Error HTTP 404");
							writerPrintln("Error HTTP 404: File Not Found");
						} catch (IOException e) {
							e.printStackTrace();
						}
		
					}
				} 
				else {
					System.out.println("Error HTTP 404");
					writerPrintln("Error HTTP 404: File not found !!!");
				}
			}
		}else {
			System.out.println("Access to this directory is denied");
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
				writerPrintln("... " + fileName + " has been created");
				fWriter.close();
			} catch (FileNotFoundException e) {
				System.out.println("Error HTTP 404");
				writerPrintln("Error HTTP 404: File not found !!!");
			}
		}else {
			System.out.println("Access to this directory is denied !!!");
			writerPrintln("Error: access to this directory is denied !!!");
		}
	}
	
	public String[] finder(String dirName, String target){
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

