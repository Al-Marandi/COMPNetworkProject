package comp6461.a3;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;

public class FMSConcurrent {
	static FMSClient c = null;
	static ArrayList<String> headers = new ArrayList<>();
	static InetSocketAddress serverAddress = new InetSocketAddress("localhost", 8000);
	
	public static void main(String arg[]) {
		Runnable task1 = () -> {
//			c = new FMSClient("localhost", 8000, "GET/HTTPLib.txt", null, headers, false, false);
				try {
					c = new FMSClient(serverAddress, "POST/HTTPLib.txt", "Hi", headers, true, false);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
//				c.request();
		};
		
		Runnable task2 = () -> {
			try {
				c = new FMSClient(serverAddress, "POST/HTTPLib.txt", "Hello", headers, true, false);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//				c = new FMSClient("localhost", 8000, "GET/HTTPLib.txt", null, headers, false, false);
//				c.request();
		};
		Runnable task3 = () -> {
			try {
				c = new FMSClient(serverAddress, "POST/HTTPLib.txt", "How", headers, true, false);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//				c = new FMSClient("localhost", 8000, "GET/HTTPLib.txt", null, headers, false, false);
//				c.request();
		};
				
		Thread thread1 = new Thread(task1);
		Thread thread2 = new Thread(task2);
		Thread thread3 = new Thread(task3);	
		thread3.start();
		thread1.start();		
		thread2.start();
	}
}
