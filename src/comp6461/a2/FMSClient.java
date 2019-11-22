package comp6461.a2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

public class FMSClient {
	
	String content, query;
	boolean headerFlag, contentFlag;
	private Socket socket;
	private PrintWriter writer;
	int port;
	ArrayList<String> headers = new ArrayList<>();
	
	/**
	 * Client constructor
	 * @param host
	 * @param port
	 * @param query
	 * @param content
	 * @param headers
	 */
	public FMSClient(String host, int port, String query, String content, ArrayList<String> headers, boolean contentFlag, boolean headerFlag) {
		
		try 
		{	
			this.headers = headers;
			this.query = query;
			this.content = content;
			this.contentFlag = contentFlag;
			this.headerFlag = headerFlag;
			socket = new Socket(host, port);
			System.out.println("Server connected");
			System.out.println(query);
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
		writer= new PrintWriter(socket.getOutputStream());
		writer.println(query);
		
		if(headerFlag) {
			for(int i = 0 ; i<headers.size();i++) {
				writer.println(headers.get(i));
			}
		}
		if(contentFlag) {
			writer.println("-d"+content);
		}
		
		writer.println("\r\n");
		writer.flush();
		this.printData();
		writer.close();
		socket.close();
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
		
		FMSClient client = new FMSClient(host, port, query.substring(1), content, headers, contentFlag, headerFlag);
//		client.request();
	}
}


//httpfs http://localhost:8000/GET/
//httpfs http://localhost:8000/GET/HTTPClient.java
//httpfs http://localhost:8000/GET/HTTPLib.txt
//httpfs http://localhost:8000/GET/test/HTTPClient.java
//httpfs http://localhost:8000/POST/tes2 -d hello
//httpfs http://localhost:8000/POST/HTTPLib.txt -d Hi
