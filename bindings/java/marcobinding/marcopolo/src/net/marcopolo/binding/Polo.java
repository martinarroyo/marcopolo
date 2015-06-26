package net.marcopolo.binding;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Polo{
	
	public Polo() throws UnknownHostException, IOException, KeyManagementException, NoSuchAlgorithmException{
		this(1000);
	}

	public Polo(int timeout) throws UnknownHostException, IOException, NoSuchAlgorithmException, KeyManagementException{
		this.timeout = timeout;
		TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}
			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}
			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
		}
		};
		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		SSLSocketFactory factory=(SSLSocketFactory) sc.getSocketFactory();
		sslsocket=(SSLSocket) factory.createSocket(InetAddress.getByName(ADDR), PORT);

		sslsocket.setSoTimeout(this.timeout);
		os=new DataOutputStream(this.sslsocket.getOutputStream());
		is=new DataInputStream(this.sslsocket.getInputStream());
		home = System.getProperty("user.home");

	}

	public String publish_service(String service) throws JSONException, PoloException, PoloInternalException{
		return this.publish_service(service, new ArrayList<String>(), false, false);
	}

	public String publish_service(String service, List<String> multicast_groups) throws JSONException, PoloException, PoloInternalException{
		return this.publish_service(service, multicast_groups, false, false);
	}

	public String publish_service(String service, List<String> multicast_groups, boolean permanent) throws JSONException, PoloException, PoloInternalException{
		return this.publish_service(service, multicast_groups, permanent, false);
	}
	
	/*!
	 * Registers a service during execution time. See :ref:`/services/intro/`.
        
        \param service: Indicates the unique identifier of the service.
        
            If `root` is true, the published service will have the same identifier as the value of the parameter. Otherwise, the name of the user will be prepended (`<user>:<service>`).
        
        \param multicast_groups: Indicates the groups where the service shall be published.
        
            Note that the groups must be defined in the polo.conf file, or otherwise the method will throw an exception.
        
        \param permanent: If set to true a file will be created and the service will be permanently offered until the file is deleted.
        
        \param root: Stores the file in the marcopolo configuration directory.
        
            This feature is only available to privileged users, by default root and users in the marcopolo group.
        
        @throws	PoloException: Raised if the input is not valid (the message of the exception describes where the problem is)

        @throws	PoloInternalException: Raised when internal problems occur. Such problems may be communication timeouts, malformed request/responses, encoding errors...
        
        @returns The name of the service as offered. In case of `root` services, the name will be the value of `service`. If the service is a user service, it will be published as `username:service`
        
	 */
	public String publish_service(String service, List<String> multicast_groups, boolean permanent, boolean root) throws JSONException, PoloException, PoloInternalException{
		//String reason = new String();
		String responseString = null;
		this.verify_common_parameters(service, multicast_groups);
		
		JSONObject send_object = new JSONObject();
		JSONObject recv_object = null;
		String token = this.get_token();

		try{
			send_object.put("Command", "Publish");

			JSONObject args = new JSONObject();

			args.put("token", token);
			args.put("service", service);

			JSONArray json_multicast_groups = new JSONArray((String[]) multicast_groups.toArray(new String[0]));

			args.put("multicast_groups", json_multicast_groups);

			args.put("permanent", permanent);
			args.put("root", root);

			send_object.put("Args", args);

		}catch(JSONException j){
			j.printStackTrace();
			throw new PoloInternalException("Internal serializing error");
		}

		try {
			os.write(send_object.toString().getBytes(Charset.forName("utf-8")));
			os.flush();
			byte[] buffer = new byte[4096];

			InputStream in = null;
			in = this.sslsocket.getInputStream();
			int bytes_read = in.read(buffer);
			
			responseString=null;
			
			if(bytes_read > 0) {
				 responseString = new String(buffer, 0, bytes_read, "UTF-8");
			}
			else{
				throw new PoloInternalException("Internal error in reading");
			}
			
			if(responseString != null){
				recv_object = new JSONObject(responseString);
				if(recv_object.has("OK")){
					return recv_object.getString("OK");
				}else if(recv_object.has("Error")){
					throw new PoloException(recv_object.getString("Error"));
				}else{
					throw new PoloInternalException("Bad received message");
				}
			}
			

		} catch (IOException e) {
			e.printStackTrace();
			throw new PoloInternalException("Internal communication error");
		}
		return "";
	}
	
	public int unpublish_service(String service) throws PoloInternalException, PoloException{
		return this.unpublish_service(service, new ArrayList<String>(), false);
	}
	
	public int unpublish_service(String service, List<String> multicast_groups) throws PoloInternalException, PoloException{
		return this.unpublish_service(service, multicast_groups, false);
	}
	
	public int unpublish_service(String service, List <String> multicast_groups, boolean delete_file) throws PoloInternalException, PoloException{
		String token = this.get_token();
		System.out.println(token);
		JSONObject recv_object;
		
		this.verify_common_parameters(service, multicast_groups);
		
		JSONObject send_object = new JSONObject();
		send_object.put("Command", "Unpublish");
		
		JSONObject args = new JSONObject();
		args.put("token", token);
		args.put("service", service);
		JSONArray json_multicast_groups = new JSONArray((String[]) multicast_groups.toArray(new String[0]));

		args.put("multicast_groups", json_multicast_groups);
		args.put("delete_file", delete_file);
		
		send_object.put("Args", args);
		
		try {
			recv_object = this.sendAndReceive(send_object);
		} catch (IOException e) {
			e.printStackTrace();
			throw new PoloInternalException("Error during internal communication");
		}
		
		if(recv_object.has("OK")){
			return recv_object.getInt("OK");
		}else if(recv_object.has("Error")){
			throw new PoloException(recv_object.getString("Error"));
		}else{
			throw new PoloInternalException("Bad received message");
		}
		
		
	}
	
	private String get_token() throws PoloInternalException{
		int uid = this.getUserUID();

		File f = new File(home, ".polo/token");
		JSONObject token;
		if(f.exists() && !f.isDirectory()){
			try{
				token = this.request_token(uid);

			}catch(PoloInternalException p){
				return "";
			}
			if(token.has("Error")){
				throw new PoloInternalException("Error in token request");
			}else if (token.has("OK")){
				return token.getString("OK");
			}
		}

		f = null;

		f = new File(home, ".polo/token");
		BufferedReader bf=null;
		try {
			bf = new BufferedReader(new FileReader(f));
			StringBuilder sb = new StringBuilder();
			String line = bf.readLine();

			while (line != null) {
				sb.append(line);
				sb.append(System.lineSeparator());
				line = bf.readLine();
			}
			String everything = sb.toString();
			return everything;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			try {
				if(bf != null)
					bf.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return "";
	}


	private JSONObject request_token(int uid) throws PoloInternalException{
		File f = new File(home, ".polo/token");
		if(! (f.exists() && !f.isDirectory())){
			JSONObject send_object = new JSONObject();
			send_object.put("Command", "Request-token");
			JSONObject args = new JSONObject();
			args.put("uid", this.getUserUID());
			send_object.put("Args", args);
			JSONObject response = null;
			try {
				response = this.sendAndReceive(send_object);
			} catch (IOException e) {
				e.printStackTrace();
				throw new PoloInternalException("Error in IO");
			}
			return response;
		}else{
			try {
				FileReader fr = new FileReader(f);
				BufferedReader bf = new BufferedReader(fr);
				String line = bf.readLine();
				JSONObject aux = new JSONObject();
				aux.put("OK", line);
				bf.close();
				fr.close();
				return aux;
				
			} catch (FileNotFoundException e) {} catch (IOException e) {
				e.printStackTrace();
			}
			return null;	
		}

	}


	public void close() throws IOException{
		os.close();
		is.close();
		this.sslsocket.close();
	}

	private int getUserUID(){
		try {
			String userName = System.getProperty("user.name");
			String command = "id -u "+userName;
			Process child = Runtime.getRuntime().exec(command);

			// Get the input stream and read from it
			InputStream in = child.getInputStream();
			int c;
			while ((c = in.read()) != -1) {
				return c;
			}
			in.close();
		} catch (IOException e) {
		}
		return -1;
	}

	private JSONObject sendAndReceive(JSONObject send_object) throws IOException, PoloInternalException{
		os.write(send_object.toString().getBytes(Charset.forName("utf-8")));
		os.flush();

		byte[] buffer = new byte[4096];

		InputStream in = null;
		in = this.sslsocket.getInputStream();
		int bytes_read = in.read(buffer);

		String port_s="";

		if(bytes_read < 0){
			throw new PoloInternalException("Error in internal communication");
		}
		
		if(bytes_read > 0) {
			port_s = new String(buffer, 0, bytes_read, "UTF-8");
		}

		return new JSONObject(port_s);
	}
	
	public boolean has_service(String service){
		throw new UnsupportedOperationException();
	}
	
	public int set_permanent(String service, boolean permanent){
		throw new UnsupportedOperationException();
	}
	
	public int reload_services(){
		throw new UnsupportedOperationException();
	}
	
	public boolean verify_ip(String ip, String reason){
		throw new UnsupportedOperationException();
	}
	private static final String PATTERN = 
	        "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

	public boolean verify_common_parameters(String service, List<String >multicast_groups) throws PoloInternalException{
		if(service.length() < 1){
			throw new PoloInternalException(String.format("The name of the service %s is invalid", service));
		}
		
		Pattern pattern = Pattern.compile(PATTERN);
	    
		for(String ip : multicast_groups){
			Matcher matcher = pattern.matcher(ip);
			if(!matcher.matches()){
				throw new PoloInternalException (String.format("The IP %s is not valid", ip));
			}
			Pattern firstb = Pattern.compile("\\d{3}");
			Matcher matcher_firstb = firstb.matcher(ip);
			int first = Integer.parseInt(matcher_firstb.group(0));
			if(first < 224 || first > 239){
				throw new PoloInternalException(String.format("The IP %s is not of class D", ip));
			}
		}
		
		return true;
	}
	
	private boolean verify_parameters(String service, List<String> multicast_groups){
		throw new UnsupportedOperationException();
	}
	
	
	private static final String ADDR = "127.0.0.1";
	private static final int PORT = 1390;
	private SSLSocket sslsocket;

	private DataOutputStream os;
	private DataInputStream is;
	private int timeout;
	//private BufferedWriter bufferedwriter;
	//private BufferedReader reader;
	private String home;
}