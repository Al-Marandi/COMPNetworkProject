package comp6461.a3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class HTTPServerUI {
	public static int port = 8000;
	public static String path = "src/comp6461/a3";
	public static boolean verbos = false;
	Socket socket;

	/**
	 * This is main method and it will start HTTPServerLib Thread.
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    	String command = br.readLine();
    	String[] data = command.split("\\s+");
    	boolean flag = false;
    	
    	//checks command
    	if(data[0].equalsIgnoreCase("httpfs")) {
    		flag = true;
    		for(int i=1;i<data.length;i++) {
    			//extract different information from command
    			switch(data[i]) {
    			case "-v":
    				verbos = true;
    				break;  
    			case "-d":
    				path = data[i+1];
    				i += 1;
    				break;
    			case "-p":
    				port = Integer.parseInt(data[i+1]);
    				i += 1;
    				break;
    			default:
    				flag = false;
    			}
    		}
    	}
    	
    	//if command is right it will start the sever 
    	if(flag) {
    		//ServerSocket serverSocket = new ServerSocket(port);
    		    		
    		byte[] buffer = new byte[Packet.MAX_LEN];
    		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    		DatagramSocket clientUDPSocket = new DatagramSocket(port);
        	
    		if(verbos) {
    			System.out.println("Server started at port "+Integer.toString(port));
    		}
    		while(true) {
				clientUDPSocket.receive(packet);
    			
    			String routerIP = "localhost";
    			int routerPort = 3000;
    			
    			byte[] rawData = packet.getData();

    			//-- create packet class to extract data
    			Packet recivedPacket = Packet.fromBytes(rawData);
    			
    			//-- get request and create response			
    			String requestPayload = new String(recivedPacket.getPayload()).trim();
    			System.out.println("Request received from the client is : ");
    			
    			if(recivedPacket.getType() == 1 && requestPayload.trim().equalsIgnoreCase("SYN")) {
    				System.out.println(requestPayload);
    				byte [] message = "SYN-ACK".toString().trim().getBytes();
    				// send payload
    				//-- convert response to packet
    				Packet responsePacket = new Packet(3, recivedPacket.getSequenceNumber(), recivedPacket.getPeerAddress(), recivedPacket.getPeerPort(), message);
    				
    				//-- convert response packet to byte[]
    				byte[] responseData = responsePacket.toBytes();
    				
    				//-- create datagram packet from byte[]
    				DatagramPacket sendUDPacket = new DatagramPacket(responseData, responseData.length, InetAddress.getByName(routerIP), routerPort);
    				
    				//-- send reply to udp port of the sender
    				clientUDPSocket.send(sendUDPacket);
    			}
    			
    			else if(recivedPacket.getType() == 0 && requestPayload.trim().equalsIgnoreCase("ACK")) {
        			System.out.println(requestPayload);
    				System.out.println("Connection Established between client and server.");
    			}
    			
    			else if(recivedPacket.getType() == 8) {
        			System.out.println(requestPayload);
    				HTTPServerLib hsl = new HTTPServerLib(clientUDPSocket, packet, routerIP, routerPort, path);
        			hsl.start();
    			}
    			
    			buffer = new byte[Packet.MAX_LEN]; 
    			packet = new DatagramPacket(buffer, buffer.length);
    		}
    	} 
    	else {
    		System.out.println("Your command is wrong...!!! Please enter right command.");
    	}
	}
}
