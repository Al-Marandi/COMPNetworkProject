package comp6461.a2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class HTTPServerLib extends Thread{
	boolean isContentType = false;
	boolean isDisposition = false;
	boolean isServerRunning = true;
	static boolean isPort = false;
	static boolean isVerbose = false;
	static boolean isPathToDir = false;
	boolean isHttpcClient = false;
	boolean isHttpfsClient = false;
	
	int count = 0;
	int port;
	String pathToDir;
	String crlf = "\r\n";
	String clientRequest; // client input
	String httpcRequest;
	String content;
	
	private Socket socket;
	private BufferedReader in = null; // input stream to get request from Client
	private PrintWriter out = null; // output stream send response to client
	private String request;
	httpfsModel httpfsModelObject;
	int clientNumber;

	public HTTPServerLib(Socket serverClient, int counter, String pathToDir2) {
		this.socket = serverClient;
		this.clientNumber = counter;
		this.pathToDir = pathToDir2;			
	}

	
	public void run(){
		try
		{	
			httpfsModelObject = new httpfsModel();
			
			//get the data from client      
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			//output stream
			out = new PrintWriter(socket.getOutputStream());
			while((request = in.readLine())!=null) {
				
				if(request.endsWith("HTTP/1.1")) {
					httpcRequest = request;
					isHttpcClient = true;
				}
				else if(request.matches("(GET|POST)/(.*)")) {
						isHttpfsClient = true;
						clientRequest = request;
				}		
				
				if(isHttpfsClient) {
					httpfsModelObject.addhttpfsHeaders(request);
					if(request.startsWith("Content-type:"))
						isContentType = true;
					if(request.startsWith("Content-Disposition:")) {
						isDisposition = true;
					}
					if(request.startsWith("-d")) {
						content = request.substring(2);
					}
				}
				if(isHttpfsClient && request.isEmpty()) {
					break;
				}
			
				if(isHttpcClient) {
					System.out.println(request);
					if(request.matches("(.*):(.*)")&&count==0){
						String[] headers = request.split(":");
						httpfsModelObject.addHeaders(headers[0], headers[1]);
					}
					
					if(count==1) {
						String data = request;
						httpfsModelObject.setContent(data);
						break;
					}
					if(request.isEmpty())
						count++;
				}	
			}
			
			if(isHttpcClient) {
				if(httpcRequest.matches("(GET|POST) /(.*)")) {
					this.httpcRequest();
				}
			}
			
			if(isHttpfsClient) {
				System.out.println("Client requested command..."+clientRequest);
	
				if(clientRequest.startsWith("GET")) {
					this.getServerRequest(clientRequest.substring(4));
				}else if(clientRequest.startsWith("POST")) {
					System.out.println(clientRequest.substring(5));
					String fileName = clientRequest.substring(5);
					this.postServerRequest(fileName, content);
				}
			}
			out.println("");
			out.flush();
			in.close();
			socket.close();
			

		}catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * handles httpc Client.
	 */
	public synchronized void httpcRequest() {
		httpcRequest = httpcRequest.replace("GET /", "").replace("POST /", "").replace("HTTP/1.1", "");
		httpfsModelObject.setStatus("200");
		httpfsModelObject.setUrl("http://localhost:"+port+"/"+httpcRequest);
		out.println(httpfsModelObject.getHeaderPart());
		
		if(httpcRequest.startsWith("get?")) {
			System.out.println("httpc GET request...");
			//args
			httpcRequest = httpcRequest.replace("get?", "");
			if(httpcRequest.matches("(.*)&(.*)")) {
				String[] temp = httpcRequest.split("&");
				for(int i = 0;i<temp.length;i++) {
					String[] args = temp[i].split("=");
					httpfsModelObject.setArgs(args[0], args[1]);
				}
			}else {
				String[] args = httpcRequest.split("=");
				httpfsModelObject.setArgs(args[0], args[1]);
			}
			System.out.println(httpfsModelObject.getGETBodyPart());
			out.println(httpfsModelObject.getGETBodyPart());
			
		}else if(httpcRequest.startsWith("post?")) {
			System.out.println("httpc POST request...");
			httpcRequest = httpcRequest.replace("post?", "");
			if(!httpcRequest.isEmpty() && httpcRequest.matches("(.*)=(.*)")) {
				if(httpcRequest.matches("(.*)&(.*)")) {
					String[] temp = httpcRequest.split("&");
					for(int i = 0;i<temp.length;i++) {
						String[] args = temp[i].split("=");
						httpfsModelObject.setArgs(args[0], args[1]);
					}
				}else {
					String[] args = httpcRequest.split("=");
					httpfsModelObject.setArgs(args[0], args[1]);
				}
			}
			out.println(httpfsModelObject.getPOSTBodyPart());
		}
		
	}
	
	
	/**
	 * "GET /" returns a list of the current files in the data directory.<br>
	 * "GET /foo" returns the content of the file named foo in the data directory.<br>
	 * 
	 * @param fileNam name of file.
	 * 
	 */
	public synchronized void getServerRequest(String fileNam) {
//		try {
//			Thread.sleep(2000);
//		} catch (InterruptedException e1) {
//			// TODO Auto-generated catch block 
//			e1.printStackTrace();
//		}
		File filePath;
		String fileName = fileNam;
		if(isContentType) {
			fileName = fileName+httpfsModelObject.getHttpfContentHeader();
			filePath = new File(pathToDir+"/"+fileName);
		}else {
			filePath = new File(pathToDir+"/"+fileName);
		}
		
		if(!fileName.contains("/")) {
			
		
		if(filePath.exists()) {
			if(filePath.isDirectory()) {	
				File[] listOfFiles = filePath.listFiles();
				for(File file : listOfFiles) {
					if(file.isFile()) {
						System.out.println("File      >> "+file.getName());
						out.println("File      >> "+file.getName());
					}else if(file.isDirectory()) {
						System.out.println("Directory >> "+file.getName());
						out.println("Directory >> "+file.getName());
					}
				}
			}else if(filePath.isFile()) {
				System.out.println("path: "+pathToDir+"/"+fileName);
				FileReader fileReader;
				PrintWriter fileWriter = null;
				File downloadPath = new File(pathToDir+"/Download");
				String fileDownloadName = new String();
				boolean dispositionDirectory;
				if(isDisposition) {
					fileDownloadName = httpfsModelObject.getHttpfsDispositionHeader();
					if(httpfsModelObject.isDispositionAttachment) {
						if(!downloadPath.exists())
							dispositionDirectory = new File(pathToDir+"/Download").mkdir();
					}
				}
				
				try {
					
					if(httpfsModelObject.isDispositionAttachment) {
						if(httpfsModelObject.isDispositionWithFileName) 
							fileWriter = new PrintWriter(downloadPath+"/"+fileDownloadName);
						else
							fileWriter = new PrintWriter(downloadPath+"/"+fileName);
					}	
					fileReader = new FileReader(filePath);
					BufferedReader bufferedReader = new BufferedReader(fileReader);
					String currentLine;
					String fileData = null;
					while ((currentLine = bufferedReader.readLine()) != null) {
						fileData = fileData + currentLine;
						if(isDisposition) {
							
							if(httpfsModelObject.isDispositionInline) {
								out.println(currentLine);
							}else if(httpfsModelObject.isDispositionAttachment) {
								fileWriter.println(currentLine);
							}
						}else 
							out.println(currentLine);
					}
					if(httpfsModelObject.isDispositionAttachment)
						fileWriter.close();
					out.println("Operation Success");
				} catch (FileNotFoundException e) {
					System.out.println("ERROR HTTP 404");
					out.println("ERROR HTTP 404 : File Not Found");
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		} else {
			System.out.println("ERROR HTTP 404");
			out.println("ERROR HTTP 404");
		}
		}else {
			System.out.println("Access Denied");
			out.println("Error: access denied");
		}
	}
	
	

	/**
	 * "POST /bar" should create or overwrite the file named bar in the data directory<br>
	 * with the content of the body of the request.<br>
	 * options for the POST such as overwrite=true|false.
	 * 
	 * @param fileName name of file. 
	 * @param content 
	 */
	public synchronized void postServerRequest(String fileName, String content) {
//		try {
//			Thread.sleep(10000);
//		} catch (InterruptedException e1) {
//			e1.printStackTrace();
//		}
		File filePath;
		PrintWriter postWriter;
		if(isContentType) 
			filePath = new File(pathToDir+"/"+fileName+httpfsModelObject.getHttpfContentHeader());
		else
			filePath = new File(pathToDir+"/"+fileName);
		
		if(!fileName.contains("/")) {
			try {
				postWriter = new PrintWriter(filePath);
				postWriter.println(content);
				out.println("Operation Sucessful...");
				postWriter.close();
			} catch (FileNotFoundException e) {
				out.print("ERROR 404");
			}
		}else {
			System.out.println("Access Denied");
			out.println("Error: access denied");
		}
	}
}

class httpfsModel {
	
	
	boolean isDispositionInline = false;
	boolean isDispositionAttachment = false;
	boolean isDispositionWithFileName = false;
	int count = 0;
	HashMap<String, String> headers;
	HashMap<String, String> args;
	ArrayList<String> fileList;
	String status;
	String content = "";
	String origin = "127.0.0.1"; 
	String Url;
	String space = " ";
	ArrayList<String> httpfsHeaders;
	
	public httpfsModel(){
		this.httpfsHeaders = new ArrayList<>();
		this.headers = new HashMap<>();
		this.args = new HashMap<>();
		this.fileList = new ArrayList<>();
		headers.put("Connection", "keep-alive");
		headers.put("Host", "Localhost");
		Instant instant = Instant.now();
		headers.put("Date", instant.toString());
	}
	
	public void addhttpfsHeaders(String header) {
		httpfsHeaders.add(header);
	}
	
	public String getHttpfContentHeader() {	
		String extension = new String();
		for(int i = 0; i<httpfsHeaders.size(); i++) {
			if(httpfsHeaders.get(i).startsWith("Content-type:")) {
				String[] temp = httpfsHeaders.get(i).split(":");
				if(temp[1].equals("application/text"))
					extension = ".txt";
				if(temp[1].equals("application/json"))
					extension = ".json";
			}
		}
		return extension;
	}
	
	public String getHttpfsDispositionHeader() {
		String fileName = "";
		for(int i = 0; i<httpfsHeaders.size(); i++) {
			if(httpfsHeaders.get(i).startsWith("Content-Disposition:")) {
				String[] temp = httpfsHeaders.get(i).split(";");
				String[] temp2 = temp[0].split(":");
				if(temp2[1].equals("inline")) {
					isDispositionInline = true;
				}else if(temp2[1].equals("attachment")) {
					isDispositionAttachment = true;
					if(temp.length == 2) {
						String temp3[] = temp[1].split(":");
						fileName = temp3[1];
						isDispositionWithFileName = true;
					}
				}				
			}
		}
		return fileName;
	}
	
	public void addHeaders(String key, String value) {
		headers.put(key, value);
	}
	
	public String getHeaders() {
		String head = "";
		for(Entry<String, String> entry : headers.entrySet()) {
			head += " "+entry.getKey()+": "+entry.getValue()+"\r\n";
		}
		return head;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
	
	public String getStatus() {
		return this.status;
	}
	
	public String getState() {
		if(this.status == "200" ) 
			return "OK";
		else if(this.status == "400")
			return "Bad Request";
		else if(this.status == "404")
			return "Not Found";
		else 
			return "ERROR HTTP";
	}
	
	public void setArgs(String key, String value) {
		args.put(key, value);
	}
	
	public String getArgs() {
		String head = "\r\n";
		for(Entry<String, String> entry : args.entrySet()) {
			head += " \""+entry.getKey()+"\": \""+entry.getValue()+"\",\r\n";
		}
		return head;
	}
	
	public String getOrigin() {
		return origin;
	}
	
	public void setUrl(String Url) {
		this.Url = Url;
	}
	
	public String getUrl() {
		return Url;
	}
	
	public void setContent(String content) {
		this.content = content;
	}
	
	public String getContent() {
		return content;
	}
	
	public void setFiles(String fileName) {
		fileList.add(fileName);
	}
	
	public String getFiles() {
		String listOfFiles = "";
		for(String file : fileList) {
			listOfFiles += file+",";
		}
		return listOfFiles;
	}
	
	public String getHeaderPart() {
		return "HTTP/1.0 " + this.getStatus() + " " + this.getState() +"\r\n"+ this.getHeaders();
	}
	
	public String getGETBodyPart() {
		return 
				"{\r\n"+
				" \"args\":{"+
				this.getArgs()+"},\r\n"+
				" \"headers\":{\r\n"+
				this.getHeaders()+"},\r\n"+
				" \"origin\": "+this.getOrigin()+",\r\n"+
				" \"url\": "+this.getUrl()+",\r\n"+
				"}";
	}
	
	public String getPOSTBodyPart() {
		return 
				"{\r\n"+space+
				"\"args\":{"+space+
				this.getArgs()+"},\r\n"+space+
				"\"data\":{"+space+
				this.getContent()+"},\r\n"+space+
				"\"files\":{\r\n"+space+
				this.getFiles()+"},\r\n"+space+
				"\"headers\":{\r\n"+
				this.getHeaders()+" },\r\n"+space+
				"\"json\": { },\r\n"+space+
				"\"origin\": "+this.getOrigin()+",\r\n"+space+
				"\"url\": "+this.getUrl()+",\r\n"+
				"}";
	}
}
