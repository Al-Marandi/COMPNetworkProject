package comp6461.a2;

import java.net.*;
import java.util.Arrays;
import java.util.Scanner;
import java.io.*;
 
public class HTTPClient {
	static boolean flag = false;
    /**
     * This is main method and it calls the testHTTPLib method.
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
    	testHTTPLib();
    }
    
    /**
     * This method will extract different information from a command and send it to library.
     * @throws MalformedURLException
     */
    public static void testHTTPLib() throws MalformedURLException {  
    	HTTPLib hlib = new HTTPLib();
    	Scanner input = new Scanner(System.in);
    	String command = input.nextLine();
    	String[] data = command.split("\\s+");
    	
    	
    	if(data.length<=2) {
    		System.out.println("Your command is wrong...!!! Please enter right command.");
    	}
    	//check if the command is help command 
    	else if(data[1].equalsIgnoreCase("help")) {
    		
    		//check if the command is general help command 
    		if(data.length == 2) {
        		System.out.println("httpc is a curl-like application but supports HTTP protocol only.");
        		System.out.println("Usage:");
        		System.out.println("\thttpc command [arguments]");
        		System.out.println("The commands are:");
        		System.out.println("\tget executes a HTTP GET request and prints the response.");
        		System.out.println("\tpost executes a HTTP POST request and prints the response.");
        		System.out.println("\thelp prints this screen.\n");
        		System.out.println("Use \"httpc help [command]\" for more information about a command.");
        	}
    		//check if the command is get usage command 
        	else if(data.length == 3 && data[2].equalsIgnoreCase("get")) {
        		System.out.println("usage: httpc get [-v] [-h key:value] URL\n");
        		System.out.println("Get executes a HTTP GET request for a given URL.\n");
        		System.out.println("\t-v              Prints the detail of the response such as protocol, status, and headers.");
        		System.out.println("\t-h key:value    Associates headers to HTTP Request with the format 'key:value'.");    		
        	}
    		//check if the command is post usage command 
        	else if(data.length == 3 && data[2].equalsIgnoreCase("post")) {
        		System.out.println("usage: httpc post [-v] [-h key:value] [-d inline-data] [-f file] URL\n");
        		System.out.println("Post executes a HTTP POST request for a given URL with inline data or from file.\n");
        		System.out.println("\t-v              Prints the detail of the response such as protocol, status, and headers.");
        		System.out.println("\t-h key:value    Associates headers to HTTP Request with the format 'key:value'.");    
        		System.out.println("\t-d string       Associates an inline data to the body HTTP POST request.");
        		System.out.println("\t-f file         Associates the content of a file to the body HTTP POST request.\n");
        		System.out.println("Either [-d] or [-f] can be used but not both.");
        	}
    	}
    	
    	else if(data[0].equalsIgnoreCase("httpc") && !data[data.length-1].contains("http")) {
    		hlib.host = "localhost";
    		hlib.port = "8080";
    		if(data[1].equalsIgnoreCase("get")) {
    			hlib.operation = "Get";
    		}
    		if(data[1].equalsIgnoreCase("post")) {
    			hlib.operation = "Post";
    		}
    		String[] file = data[data.length-1].trim().split("/");
    		if(file.length==2) {
    			hlib.datas.add(file[1]);
    		}
    		else if(file.length==0) {
    			
    		}
    		else {
    			System.out.println("You don't have accesss to read/write any file outside the file server working directory.");
    		}
    		try {
    			hlib.sendHTTPRequest();
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
    	}
    	
    	//check if the command is regular get or post command
    	else {
    		String url1 = data[data.length-1];  //it will extract url from the command
        	URL url = new URL(url1);            //it will create url object to get different information from url 
        	
        	int counter = 0;
        	hlib.verbos = false;
    		
        	//checks if the command is httpc command or not
	    	if(data[0].equalsIgnoreCase("httpc")) {
	    		flag = true;
	    	}
	    	//checks get command
	    	if(data[1].equalsIgnoreCase("get") && flag == true) {
	    		hlib.operation = "Get";
	    		for(int i=2;i<(data.length-1);i++) {
	    			int index = 0;
	    			//extract different information for get command
	    			switch(data[i]) {
	    			case "-v":
	    				hlib.verbos = true;
	    				break;
	    			case "-h":
	    				index = i;
	    	            hlib.addHeader(data[index+1]);
	    	            i += 1;
	    	            break;
	    			case "--h":
	    				index = i;
	    	            hlib.addHeader(data[index+1]);
	    	            i += 1;
	    	            break;  
	    	        default:
	    	        	flag = false;
	    			}
	    			
	    				
	    		}
	    		hlib.host = url.getHost();
	    		if(url.getPort()!=-1) {
	    			hlib.port = String.valueOf(url.getPort());
	    		}
	    		else {
	    			hlib.port = "80";
	    		}
	            hlib.path = url.getPath();
	            hlib.addData(url.getQuery());
	    		
	    	}
	    	//checks post command
	    	else if(data[1].equalsIgnoreCase("post") && flag == true) {
	    		hlib.operation = "Post";
	    		for(int i=2;i<(data.length-1);i++) {
	    			int index = 0;
	    			//extract different information for post command
	    			switch(data[i]) {
	    			case "-v":
	    				hlib.verbos = true;
	    				break;
	    			case "-h":
	    				index = i;
	    	            hlib.addHeader(data[index+1]);
	    	            i += 1;
	    	            break;
	    			case "--h":
	    				index = i;
	    	            hlib.addHeader(data[index+1]);
	    	            i += 1;
	    	            break;  
	    			case "-f":
	    				index = i;
	    				hlib.dataFile = data[index+1];
	    				i += 1;
	    				break;
	    			case "--f":
	    				index = i;
	    				hlib.dataFile = data[index+1];
	    				i += 1;
	    				break;
	    			case "-d":
	    				index = i;
	    	            hlib.addData(data[index+1]);
	    	            i += 1;
	    	            break;
	    			case "--d":
	    				index = i;
	    	            hlib.addData(data[index+1]);
	    	            i += 1;
	    	            break;
	    			case "-o":
	    				index = i;
	    				hlib.save = true;
			    		hlib.outputFile = data[index+1];
			    		i += 1;
	    	            break;
	    			case "--o":
	    				index = i;
	    				hlib.save = true;
			    		hlib.outputFile = data[index+1];
			    		i += 1;
	    	            break;
	    	        default:
	    	        	flag = false;
	    			}
	    				
	    		}
	    		hlib.host = url.getHost();
	    		if(url.getPort()!=-1) {
	    			hlib.port = String.valueOf(url.getPort());
	    		}
	    		else {
	    			hlib.port = "80";
	    		}
	            hlib.path = url.getPath();	
	    	}
	    	//it checks if any information is missing or command is wrong
	    	else {
	    		flag = false;
	    	}
	    	
	    	//it checks whether -d or -f is used at same time or not
	    	if((Arrays.asList(data).contains("-f") || Arrays.asList(data).contains("--f")) && (Arrays.asList(data).contains("-d") || Arrays.asList(data).contains("--d"))) {
	    		System.out.println("Either [-d] or [-f] can be used but not both.");
	    		flag = false;
	    	}
	    	
	    	//if every thing is ok then it will send the request to library 
	    	if(flag==true) {
	    		try {
	    			hlib.sendHTTPRequest();
	    		} catch (IOException e) {
	    			e.printStackTrace();
	    		}
	    	}
	    	else {
		    	System.out.println("Your command is wrong...!!! Please enter right command.");
	    	}
    	}
    }
}


/*
 
 
httpc help 

httpc help post

httpc help get

httpc get http://httpbin.org/get?course=networking&assignment=1

httpc get -v http://httpbin.org/get?course=networking&assignment=1

httpc get -v http://localhost:8080/get?course=networking&assignment=1

httpc post -h Content-Type:application/json --d {"Assignment":1} http://httpbin.org/post

httpc post -h Content-Type:application/json --d {"Assignment":1} -d {"Assignment":2} --d {"Assignment":3} http://httpbin.org/post

httpc post -h Content-Type:application/json -f {} -d {} http://httpbin.org/post

httpc post -h Content-Type:application/json -f data.txt http://httpbin.org/post

httpc post -h Content-Type:application/json -d {"Assignment":1} -o result.txt http://httpbin.org/post

httpc get -v -h Content-Type:application/json https://httpstat.us/302
 
 */
