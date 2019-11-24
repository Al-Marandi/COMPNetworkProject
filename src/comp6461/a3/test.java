package comp6461.a3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class test {

	public static void main(String[] args) {
		DatagramSocket aSocket = null; 	
		try{
			System.out.println("Client Started........");
			aSocket = new DatagramSocket();
			byte [] message = "Hello".getBytes();
			
			InetAddress routerAddress = InetAddress.getByName("localhost"); 			
			int routerPort = 3000;
			InetAddress serverAddress = InetAddress.getByName("localhost"); 			
			int serverPort = 8000;
			
			//-- convert response to packet
			Packet responsePacket = new Packet(0, 1, serverAddress, serverPort, message);
			
			//-- convert response packet to byte[]
			byte[] requestData = responsePacket.toBytes();
			
			//-- create datagram packet from byte[]
			DatagramPacket requestUDPacket = new DatagramPacket(requestData, requestData.length, routerAddress, routerPort);
			
			//-- send request to udp port of the router
			aSocket.send(requestUDPacket);
			
			
			byte [] buffer = new byte[Packet.MAX_LEN];
			DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
			
			//Client waits until the reply is received-----------------------------------------------------------------------
			aSocket.receive(reply);//reply received and will populate reply packet now.
			
			//-- get raw data of byte[] from packet
			byte[] rawData = reply.getData();

			//-- create packet class to extract data
			Packet replyPacket = Packet.fromBytes(rawData);
			
			//-- get request and create response
			String payload = new String(replyPacket.getPayload());
			
			System.out.println("Reply received from the server is: "+ payload);//print reply message after converting it to a string from byte
			
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

}
