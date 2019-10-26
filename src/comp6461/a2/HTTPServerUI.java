package comp6461.a2;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class HTTPServerUI {
//	public static String port = "8080";
	public static String path = "src\\";
	public static boolean verbos;
	Socket socket;

	/**
	 * @param args
	 * @throws IOException 
	 */
//	public static void main(String[] args) throws IOException {
//		Scanner input = new Scanner(System.in);
//    	String command = input.nextLine();
//    	String[] data = command.split("\\s+");
//    	
//    	
//    	if(data[0].equalsIgnoreCase("httpc")) {
//    		for(int i=2;i<(data.length-1);i++) {
//    			int index = 0;
//    			//extract different information for get command
//    			switch(data[i]) {
//    			case "-v":
//    				verbos = true;
//    				break;  
//    			case "-d":
//    				path = data[i+1];
//    				
//    				break;
//    			case "-p":
//    				port = data[i+1];
//   	
//    			}
//    		}
//    	}
//    	ServerSocket serverSocket = new ServerSocket(Integer.p);
//    	
//    	
//	}

	static boolean isPort = false;
	static int port;
	static String pathToDir;
	private static boolean isVerbose = false;;
	
public static void main(String args[]) throws IOException {
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String serverInput = br.readLine();
		String[] commands = serverInput.split(" ");
		for(int i = 0; i<commands.length; i++) {
			if(commands[i].equals("-p")){
				isPort = true;
				port = Integer.parseInt(commands[++i]);
			}
			if(commands[i].equals("-d")) {
				pathToDir = commands[++i];
			}else {

				pathToDir = "src/comp6461/a2";
			}
			if(commands[i].equals("-v")) {
				isVerbose  = true;
			}
		}
		if(!isPort) {
			port = 8000;
		}
		ServerSocket serverSocket = new ServerSocket(port);
		int counter = 0;
		if(isVerbose) {
			System.out.println("Server started...");
			System.out.println("Server listening at port "+port);
		}
		while(true) {
			counter++;
			Socket serverClient = serverSocket.accept();
			if(isVerbose) {
				System.out.println(">> Client "+counter+" connection established");
			}
			
			HTTPServerLib hst = new HTTPServerLib(serverClient,counter,pathToDir);
			hst.start();
		}
	}
}
