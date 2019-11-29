package comp6461.a3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Hashtable;

public class HTTPServerUI {
	public static int port = 8000;
	public static String path = "src/comp6461/a3";
	public static boolean verbos = false;
	Socket socket;
	public static Hashtable<String,DatagramSocket> clientServerMapTable = new Hashtable<String,DatagramSocket>();

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
    		
    		String routerIP = "localhost";
			int routerPort = 3000;   
			int serversPort = 5000;
    		    		
    		byte[] buffer = new byte[Packet.MAX_LEN];
    		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    		DatagramSocket serverUISocket = new DatagramSocket(port);
        	
    		if(verbos) {
    			System.out.println("Server started at port "+Integer.toString(port));
    		}
    		
    		while(true) {

    			serverUISocket.receive(packet);    			    			   			

    			//-- create packet class to extract data
    			byte[] rawData = packet.getData();
    			Packet recivedPacket = Packet.fromBytes(rawData);
    			
    			//-- generate client key to map it to the pre-existance server
    			String clientKey = recivedPacket.getPeerAddress().getHostAddress() + ":" + recivedPacket.getPeerPort();
//    			System.out.println("recivedPacket.getType() : " + recivedPacket.getType());
//    			System.out.println("recivedPacket.getPayload(): " + new String(recivedPacket.getPayload()).trim());
    			
    			HTTPServerLib hsl;
    			
    			if(!clientServerMapTable.containsKey(clientKey)) {
//    				System.out.println("clientKey is created: " + clientKey);
    				DatagramSocket serverSocket = new DatagramSocket(serversPort++);
    				hsl = new HTTPServerLib(serverSocket, routerIP, routerPort, path);
    				clientServerMapTable.put(clientKey, serverSocket);
    				hsl.start();
    				
    				//-- create datagram packet from byte[]
    				DatagramPacket sendUDPacket = new DatagramPacket(packet.getData(), packet.getData().length, InetAddress.getByName("localhost"), serverSocket.getLocalPort());
    				
    				//-- send reply to udp port of the sender
    				serverUISocket.send(sendUDPacket);
    				
    			}
    			else {
//    				System.out.println("clientKey is exist: " + clientKey);
    				
    				DatagramSocket serverSocket = clientServerMapTable.get(clientKey); 
//    				System.out.println("serverSocket.getLocalPort(): " + serverSocket.getLocalPort());
    				
    				//-- create datagram packet from byte[]
    				DatagramPacket sendUDPacket = new DatagramPacket(packet.getData(), packet.getData().length, InetAddress.getByName("localhost"), serverSocket.getLocalPort());
    				
    				//-- send reply to udp port of the sender
    				serverUISocket.send(sendUDPacket);
    			
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
