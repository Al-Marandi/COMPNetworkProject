package comp6461.a3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

public class FMSClient {
	
	String content, query;
	boolean headerFlag, contentFlag;
	private Socket socket;
	private PrintWriter writer;
	int port;
	StringBuilder request;
//	static InetAddress routerAddress = InetAddress.getByName("localhost"); 
//	static int routerPort = 3000;
	static InetSocketAddress routerAddress = new InetSocketAddress("localhost", 3000);	
	static InetSocketAddress serverAddress;
//	InetAddress serverAddress; 			
//	static int serverPort;
	ArrayList<String> headers = new ArrayList<>();
	
	/**
	 * Client constructor
	 * @param host
	 * @param port
	 * @param query
	 * @param content
	 * @param headers
	 */
	public FMSClient(InetSocketAddress serverAddress, String query, String content, ArrayList<String> headers, boolean contentFlag, boolean headerFlag) {
		try 
		{	
			this.serverAddress = serverAddress;
			this.content = content;
			this.query = query;
			this.headers = headers;
			this.contentFlag = contentFlag;
			this.headerFlag = headerFlag;
			this.request();
			
		} catch (IOException e) {
			System.out.println("Error HTTP 404: Page Not Found");
		}
	}	
	
	/**
	 * It will print the data received from server.
	 */
	public synchronized void printData() {
		try {
			String print;
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			while((print = br.readLine()) != null) {
				System.out.println(print);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * It will send request to server.
	 * @throws IOException
	 */
	public synchronized void request() throws IOException {
		DatagramSocket aSocket = null; 	
		try{
			System.out.println("Client started........");
			request = new StringBuilder();
			request.append(query+"\n");
			if(headerFlag) {
				for(int i = 0 ; i<headers.size();i++) {
					request.append(headers.get(i)+"\n");
				}
			}
			if(contentFlag) {
				request.append("-d"+content+"\n");
			}
			request.append("\r\n");
			aSocket = new DatagramSocket();
			byte [] message = request.toString().trim().getBytes();
			
			Packet responsePacket = new Packet(0, 1, serverAddress.getAddress(), serverAddress.getPort(), message);
			byte[] requestData = responsePacket.toBytes();
			DatagramPacket requestUDPacket = new DatagramPacket(requestData, requestData.length, routerAddress.getAddress(), routerAddress.getPort());
			aSocket.send(requestUDPacket);
			
			byte [] buffer = new byte[Packet.MAX_LEN];
			DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
			
			System.out.println("Reply received from the server is: ");
			while(true) {
			//Client waits until the reply is received-----------------------------------------------------------------------
			aSocket.receive(reply);//reply received and will populate reply packet now.
			byte[] rawData = reply.getData();
			Packet replyPacket = Packet.fromBytes(rawData);
			String payload = new String(replyPacket.getPayload());
			System.out.print(payload.trim());//print reply message after converting it to a string from byte
			}
			
		}
		catch(SocketException e){
			System.out.println("Socket: "+e.getMessage());
		}
		catch(IOException e){
			e.printStackTrace();
			System.out.println("IO: "+e.getMessage());
		}
		finally{
			if(aSocket != null) aSocket.close();//now all resources used by the socket are returned to the OS, so that there is no
												//resource leakage, therefore, close the socket after it's use is completed to release resources.
		}		
	}	
	
	/**
	 * It is main method and it will extract different data from command and send a request to server.
	 * @param args
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public synchronized static void main(String args[]) throws IOException, URISyntaxException {
		BufferedReader breader = new BufferedReader(new InputStreamReader(System.in));
		boolean contentFlag = false, headerFlag = false;
		String content = "", query, url = "";
		ArrayList<String> headers = new ArrayList<>();
		String fsClient = breader.readLine();
		String[] cClient = fsClient.split(" ");
		if(cClient[0].equals("httpfs")) {
			for(int i =0; i<cClient.length; i++) {
				if(cClient[i].startsWith("-d")) {
					contentFlag = true;
					content = cClient[i+1];
				}
				if(cClient[i].startsWith("http://")){
					url = cClient[i];
				}
				if(cClient[i].equals("-h")) {
					headerFlag = true;
					headers.add(cClient[i+1]);
				}
			}
		}
		URI uri = new URI(url);
		String host = uri.getHost();
		query = uri.getPath();
		int port = uri.getPort();
		InetSocketAddress serverAddress = new InetSocketAddress(host, port);
		
		FMSClient client = new FMSClient(serverAddress, query.substring(1), content, headers, contentFlag, headerFlag);
	}
}


//httpfs http://localhost:8000/GET/
//httpfs http://localhost:8000/GET/HTTPClient.java
//httpfs http://localhost:8000/GET/HTTPLib.txt
//httpfs http://localhost:8000/GET/test/HTTPClient.java
//httpfs http://localhost:8000/POST/tes2 -d hello
//httpfs http://localhost:8000/POST/HTTPLib.txt -d Hi
