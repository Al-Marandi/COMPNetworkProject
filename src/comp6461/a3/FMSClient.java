package comp6461.a3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class FMSClient {
	
	String content, query;
	boolean headerFlag, contentFlag;
	private Socket socket;
	int port, windowSize = 4;
	StringBuilder request, requestedMessage = new StringBuilder();
	static InetSocketAddress routerAddress = new InetSocketAddress("localhost", 3000);	
	static InetSocketAddress serverAddress;
	ArrayList<String> headers = new ArrayList<>();
	long sequenceNumber = 100, serverSeqNumber, lastSeq, maxSeq=-1;
	Map<Long, String> receivedDAta = new LinkedHashMap<Long, String>();
	  
	
	/**
	 * Client constructor
	 * @param host
	 * @param port
	 * @param query
	 * @param content
	 * @param headers
	 * @throws IOException 
	 */
	public FMSClient(InetSocketAddress serverAddress, String query, String content, ArrayList<String> headers, boolean contentFlag, boolean headerFlag) throws IOException {
		FMSClient.serverAddress = serverAddress;
		this.content = content;
		this.query = query;
		this.headers = headers;
		this.contentFlag = contentFlag;
		this.headerFlag = headerFlag;
		this.threeWayHandshake();
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
	
	public synchronized void threeWayHandshake() throws IOException {
		DatagramSocket aSocket = null;
		try { 
			aSocket = new DatagramSocket();
			String message = "";
			Packet responsePacket = new Packet(Packet.Type.SYNType.getPacketType(), sequenceNumber, serverAddress.getAddress(), serverAddress.getPort(), message.getBytes());
			byte[] requestData = responsePacket.toBytes();
			DatagramPacket requestUDPacket = new DatagramPacket(requestData, requestData.length, routerAddress.getAddress(), routerAddress.getPort());
			aSocket.send(requestUDPacket);
			this.sequenceNumber++;
			byte [] buffer = new byte[Packet.MAX_LEN];
			DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
			
			System.out.println("Reply received from the server is: ");
			aSocket.receive(reply);
			byte[] rawData = reply.getData();
			Packet replyPacket = Packet.fromBytes(rawData);
			String payload = new String(replyPacket.getPayload());
			System.out.println("SYN-ACK");
			
			if(replyPacket.getType() == Packet.Type.SYNACKType.getPacketType() && payload.trim().equalsIgnoreCase("SYN-ACK:"+this.sequenceNumber)) {
				this.serverSeqNumber = replyPacket.getSequenceNumber() + 1;
				byte [] msg = ("ACK:" + this.serverSeqNumber).toString().trim().getBytes();
				responsePacket = new Packet(Packet.Type.ACKType.getPacketType(), sequenceNumber, serverAddress.getAddress(), serverAddress.getPort(), msg);
				requestData = responsePacket.toBytes();
				requestUDPacket = new DatagramPacket(requestData, requestData.length, routerAddress.getAddress(), routerAddress.getPort());
				aSocket.send(requestUDPacket);
				this.sequenceNumber++;
				this.request(aSocket);
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
	 * It will send request to server.
	 * @throws IOException
	 */
	public synchronized void request(DatagramSocket aSocket) throws IOException {
		try{
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
			byte [] message = request.toString().trim().getBytes();
			
			Packet responsePacket = new Packet(Packet.Type.DataType.getPacketType(), sequenceNumber, serverAddress.getAddress(), serverAddress.getPort(), message);
			byte[] requestData = responsePacket.toBytes();
			DatagramPacket requestUDPacket = new DatagramPacket(requestData, requestData.length, routerAddress.getAddress(), routerAddress.getPort());
			aSocket.send(requestUDPacket);
			sequenceNumber++;
			
			byte [] buffer = new byte[Packet.MAX_LEN];
			DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
			long count = 0;
			System.out.println("Reply received from the server is: ");
			while(true) {
				//Client waits until the reply is received-----------------------------------------------------------------------
				aSocket.receive(reply);//reply received and will populate reply packet now.
				byte[] rawData = reply.getData();
				Packet replyPacket = Packet.fromBytes(rawData);
				String payload = new String(replyPacket.getPayload());

				if(replyPacket.getType() == Packet.Type.DataType.getPacketType()) {
					if(payload.substring(0, 4).equalsIgnoreCase("Done")) {
						this.lastSeq = replyPacket.getSequenceNumber();
						if(this.lastSeq - 1001 == count) {
							System.out.println(requestedMessage.toString().trim());
						}						
					}
					else {
						requestedMessage.append(payload);
						count++;
						this.serverSeqNumber = replyPacket.getSequenceNumber();
						if(this.serverSeqNumber>this.maxSeq) {
							this.maxSeq = this.serverSeqNumber;
						}
						if(count%windowSize == 0) {
							byte [] msg = ("ACK-Data:" + (this.maxSeq+1)).toString().trim().getBytes();
							Packet ACKPacket = new Packet(Packet.Type.ACKDataType.getPacketType(), sequenceNumber, serverAddress.getAddress(), serverAddress.getPort(), msg);
							byte[] ACKData = ACKPacket.toBytes();
							DatagramPacket ACKUDPacket = new DatagramPacket(ACKData, ACKData.length, routerAddress.getAddress(), routerAddress.getPort());
							aSocket.send(ACKUDPacket);
							this.sequenceNumber++;
						}
						
						if(replyPacket.getSequenceNumber() == this.lastSeq - 1) {
							System.out.println(requestedMessage.toString().trim());
						}
					}
				}
				
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
	
//	public synchronized void sendPacket(long seqNumber) {
//		
//	}
	
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


//router --port=3000 --drop-rate=0 --max-delay=0ms --seed=1
//router --port=3000 --drop-rate=0 --max-delay=1ms --seed=1
//httpfs http://localhost:8000/GET/
//httpfs http://localhost:8000/GET/HTTPClient.java
//httpfs http://localhost:8000/GET/HTTPLib.txt
//httpfs http://localhost:8000/GET/test/1.txt
//httpfs http://localhost:8000/POST/tes2 -d hello
//httpfs http://localhost:8000/POST/HTTPLib.txt -d Hi
