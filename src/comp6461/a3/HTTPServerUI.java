package comp6461.a3;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

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
    	int counter = 0;
    	boolean flag = false;
    	
    	//checks command
    	if(data[0].equalsIgnoreCase("httpfs")) {
    		flag = true;
    		for(int i=1;i<data.length;i++) {
    			int index = 0;
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
    			counter++;
    			//Socket serverClient = serverSocket.accept();
    			
    			//DatagramSocket clientUDPSocket = new DatagramSocket(port);
    			// Server waits for the request to come
				clientUDPSocket.receive(packet);

    			if(verbos) {
    				System.out.println("Client "+counter+" Connected.");
    			}
    			
    			//HTTPServerLib hsl = new HTTPServerLib(serverClient, path);
    			String routerIP = "localhost";
    			int routerPort = 3000;
    			HTTPServerLib hsl = new HTTPServerLib(clientUDPSocket,packet,routerIP,routerPort);
    			hsl.start();
    			//clientUDPSocket.close();
    		}
    	} 
    	else {
    		System.out.println("Your command is wrong...!!! Please enter right command.");
    	}
	}
}
