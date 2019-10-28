package comp6461.a2;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

/**
 * Client class
 * @author Manan
 *
 */
public class FMSClient {
	
	static boolean isHeader = false;
	static boolean isContent = false;
	static String content;
	private Socket socket;
	private PrintWriter out;
	static String URL;
	int port;
	static String query;
	static ArrayList<String> headers = new ArrayList<>();
	
	public FMSClient(String host, int port, String query,String content2, ArrayList<String> headers) {
		
		try 
		{	
			this.content = content2;
			this.query = query;
			this.headers = headers;
			socket = new Socket(host, port);
			System.out.println("connected to server...");
			
		} catch (IOException e) {
			System.out.println("");
			System.out.println("ERROR HTTP 404: Host Not Found");
		}
	}	

	public void sendRequest() throws IOException {
		out= new PrintWriter(socket.getOutputStream());
		out.println(query);
		
		if(isHeader) {
			for(int i = 0 ; i<headers.size();i++) {
				out.println(headers.get(i));
			}
		}
		if(isContent) {
			out.println("-d"+content);
		}
		
		out.println("\r\n");
		out.flush();
		this.printOutput();
		out.close();
		socket.close();
	}

	public void printOutput() {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String output;
			while((output = br.readLine()) != null) {
				System.out.println(output);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	public static void main(String args[]) throws IOException, URISyntaxException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String httpfsClient = br.readLine();
		String[] commandClient = httpfsClient.split(" ");
		if(commandClient[0].equals("httpfs")) {
			for(int i =0; i<commandClient.length; i++) {
				if(commandClient[i].equals("-h")) {
					isHeader = true;
					headers.add(commandClient[++i]);
				}
				if(commandClient[i].startsWith("http://")){
					URL = commandClient[i];
				}
				if(commandClient[i].startsWith("-d")) {
					isContent = true;
					content = commandClient[++i];
				}
			}
		}
		URI uri = new URI(URL);
		String host = uri.getHost();
		int port = uri.getPort();
		query = uri.getPath();
		System.out.println(query.substring(1));
		
		FMSClient client2 = new FMSClient(host,port,query.substring(1),content, headers);
		client2.sendRequest();
	}
}


//httpfs http://localhost:8000/GET/
//httpfs http://localhost:8000/GET/HTTPClient.java
//httpfs http://localhost:8000/GET/test/HTTPClient.java
//httpfs http://localhost:8000/POST/tes2 -d hello
