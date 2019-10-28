package comp6461.a2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

public class HTTPLib {
	
	//====================================================================== fields members
	String request;
	Socket socket;
	PrintWriter writer;
	BufferedReader reader;
	public String operation; 
	public String host;
	public String port;
	public String path;
	public String outputFile;
	public String dataFile;
	public boolean verbos;
	public boolean save;
	public ArrayList<String> datas;
	public ArrayList<String> headers;
	int redirectCount;

	//======================================================================= constructor
	public HTTPLib() {
		outputFile = "";
		dataFile = "";
		headers = new ArrayList<String>();
		datas = new ArrayList<String>();
		redirectCount = 0;
	}
	
	//======================================================================= function members
	
	/**
	 * This method is responsible for adding header token to header Map
	 * @param header: header token
	 */
	public void addHeader(String header) {
		//check if the header exist before
		boolean match = false;
		for(String h : this.headers) {
			if(h.equalsIgnoreCase(header)) {
				match = true;
				break;
			}
		}
		if(!match) {
			this.headers.add(header);
		}
	}
	
	/**
	 * This method is responsible for adding data token
	 * @param data: data token
	 */
	public void addData(String data) {		
		this.datas.add(data);				
	}
	
	/**
	 * This method is responsible for removing header from hear Map
	 * @param key: header token
	 */
	public void removeHeader(String header) {
		for (int i = 0; i < this.headers.size(); i++) {
			if(this.headers.get(i).equalsIgnoreCase(header)) {
				this.headers.remove(i);
				break;
			}
		}
	}
	
	/**
	 * This method is responsible for sending HTPP request
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public void sendHTTPRequest() throws UnknownHostException, IOException {
		this.redirectCount = 0;
		if(!this.host.contains("localhost") || true) {
			sendRequest();
		}
		else {
			sendLocalRequest();
		}

	}
	
	/**
	 * This method is responsible for sending HTPP request
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	void sendRequest() throws UnknownHostException, IOException {
	    this.socket = new Socket(this.host, Integer.parseInt(this.port));
	    
	    // GET operation
	    if (this.operation.toUpperCase().equals("GET")) {
	    	
		  // append request operation and path
		  this.request = "GET " + this.path;
		  
		  // append request data
		  if(!this.dataFile.isEmpty()) {
			  if(!readDataFromFile()) {
				  System.out.println("The file for input data is not found !!!!");
				  socket.close();
				  return;
			  }
		  }
		  
		  if(this.datas.size() > 0  ) {
			  this.request = this.request + "?";
			  boolean first = true;
			  for(String data : this.datas) {
				  if(first) {
					  first = false;
				  }
				  else {
					  this.request = this.request + "&";
				  }
				  this.request = this.request + data;  
			  }
		  }

		  // append HTTP protocol version
		  this.request = this.request + " HTTP/1.0\r\n";		  
		  
		  // append HTTP headers
		  addHeader("Host:" + this.host);
		  appendHeader();
		  
		  // terminate HTTP request with an empty line
		  this.request = this.request + "\r\n";
	    } 
	    
	    // POST operation
	    else if (this.operation.toUpperCase().equals("POST")) {
	    	
    	  // get request data
	      if(!this.dataFile.isEmpty()) {
	    	  if(!readDataFromFile()) {
				  System.out.println("The file for input data is not found !!!!");
				  socket.close();
				  return;
			  }
	      }
	      
	      // append request operation , path and HTTP protocol version
	      this.request = "POST " + this.path + " HTTP/1.0\r\n";
	     
	      // append HTTP headers
	      addHeader("Host:" + this.host);
	      //addHeader("Content-Length:" + String.valueOf(this.datas.length()));
	      addHeader("Content-Length:" + this.datas.stream().mapToInt(i -> i.length()).sum());	   
	      appendHeader();
	      
	      // terminate HTTP request with an empty line
	      this.request = this.request + "\r\n";	      	      
	      
	      //append data tokens
	      //this.request = this.request + this.datas;
	      boolean first = true;
	      for(String data : this.datas) {
	    	  if(first) {
				  first = false;
			  }
			  else {
				  this.request = this.request + "\r\n";
			  }
			  this.request = this.request + data;  
		  }
	    } 
	    
	    // not a valid HTTP operation
	    else {
	      System.out.println("Invalid HTTP operation !!!");
	      socket.close();
	      return;
	    }
	    
	    // send HTTP request
	    writeRequest();
	    
	    // print HTTP response
	    printResponse();

	    // close the socket
	    socket.close();
	  }
	
	
	/**
	 * This method is responsible for sending HTPP request to the local server
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	void sendLocalRequest() throws UnknownHostException, IOException {
	    this.socket = new Socket(this.host, Integer.parseInt(this.port));
	    
	    // GET operation
	    if (this.operation.toUpperCase().equals("GET")) {
	    	
		  // append request operation and path
		  this.request = "GET /";
		  
		  if(this.datas.size() > 0  ) {
			  this.request = this.request + this.datas.get(0);
		  }
		  
	    } 
	    
	    // POST operation
	    else if (this.operation.toUpperCase().equals("POST")) {
			// append request operation
			this.request = "POST /";  
			  
			//append data tokens
			if(this.datas.size() > 0  ) {
			  this.request = this.request + this.datas.get(0);
			}
			
			// not a valid HTTP operation
			else {
			  System.out.println("Invalid HTTP operation !!!");
			  socket.close();
			  return;
			}
	    }
	    
	    // terminate request with an empty line
	    this.request = this.request + "\r\n";
	    
	    // send HTTP request
	    writeRequest();
	    
	    // print HTTP response
	    printResponse();

	    // close the socket
	    socket.close();
	  }
	
	
	/**
	 * This method is responsible for append headers in header Map to the HTTP request
	 */
	void appendHeader() {
		for(String h : this.headers) {
			this.request = this.request + h + "\r\n";
		}
	}
	
	/**
	 * This method is responsible for printing the response in the console
	 */
	void printResponse() {
		try {		
		    this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));		    
			String line; 
	        boolean isResponseContent = false;
	        String response = "";
	        String newURL = "";
	        
	        if(!this.save) {
		        while ((line = this.reader.readLine()) != null) {	
		        	response = response + line + "\r\n";
		        	if(this.verbos || isResponseContent) {
		        		System.out.println(line);
		        	}
		        	else {
		        		if(line.length() == 0) {
		        			isResponseContent = true;
		        		}
		        	}
		        }
	        }
	        else {
	        	File file = new File(this.outputFile);
			    PrintWriter fileWriter = new PrintWriter(file);	        	
	        	while ((line = this.reader.readLine()) != null) {	        		
	        		response = response + line + "\r\n";
		        	
	        		if(this.verbos || isResponseContent) {
		        		System.out.println(line);
		        		fileWriter.println(line);
		        	}
		        	else {
		        		if(line.length() == 0) {
		        			isResponseContent = true;
		        		}
		        	}
			    }
	        	
		        fileWriter.flush();
		        fileWriter.close();
	        }	        
	        reader.close();
	        
	        // check for redirect
	        if(response.subSequence(response.indexOf(" ") + 1, response.indexOf(" ") + 2).equals("3")) {
	        	
	        	//check http request type
	        	if(this.operation.equalsIgnoreCase("GET")) {
		        	// check redirection count
		        	if(this.redirectCount < 5) {	        		
		        		this.redirectCount ++;
		        		
		    			int locationIndex = response.indexOf("Location:");
		    			if(locationIndex != -1) {
		    				int index = response.indexOf("Location:")+10;
		        			while(response.charAt(index) != '\n') {
		        				newURL = newURL + String.valueOf(response.charAt(index));
		        				index ++;
		        			}
		
		        			System.out.println();
		        			System.out.println(":::::: Redirect [" + this.redirectCount + "] to");
		        			System.out.println(newURL);
		        			System.out.println();
		        		
		        			
		        			this.host = new URL(newURL).getHost();
		        			this.path = new URL(newURL).getPath();
		        			if(this.path.contentEquals("")) {
		        				this.path = "/";
		        			}
		        			
		        			sendRequest();
		    			}
		    			else {
		    				System.out.println();
							System.out.println(":::::: Redirect [" + this.redirectCount + "] to");
							System.out.println("new URL is not provided in the response !!!");
						}
		        	}
		        	else {
		        		this.redirectCount = 0;
		        		System.out.println();
		        		System.out.println(":::::: Redirect [" + this.redirectCount + "] to");
						System.out.println("maximum number of redirection is reached !!!");
		        	}
	        	}
	        	else {
	        		this.redirectCount = 0;
	        		System.out.println();
	        		System.out.println(":::::: Redirect [" + this.redirectCount + "] to");
					System.out.println("for POST request the automatice redirection is not permited!!!");
	        	}
    			
    		}
		} 
		catch (Exception e) {

		}
		finally {
			try {
				if(this.reader != null)
					this.reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}	
	}
		
	/**
	 * This method is responsible for send Http request
	 */
	void writeRequest() {
		try {			
			this.writer = new PrintWriter(this.socket.getOutputStream(), true);		    
			this.writer.write(this.request);
		    this.writer.flush();
		    System.out.println("============");
		    System.out.println("this.request: "+ this.request );
		    System.out.println("============");
		} 
		catch (Exception e) {

		}
	}

	/**
	 * This function is responsible for reading data from file 
	 * @return boolean: if the file exist the return true else false
	 * @throws IOException
	 */
	boolean readDataFromFile() throws IOException {
	  File f = new File(this.dataFile); 	
	  if(!f.exists()) {
		  
		  return false;
	  }
	  
	  BufferedReader br = new BufferedReader(new FileReader(f)); 			  
	  String st; 
	  datas = new ArrayList<String>();
	  while ((st = br.readLine()) != null) {
		  this.datas.add(st);
	  }
	  return true;
	}
}