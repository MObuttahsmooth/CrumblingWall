import java.util.*;
import java.util.concurrent.*;
import java.net.*;
import java.io.*;

public class ReleaseThread implements Callable<Integer>{
	private int port;
	private String address;
	private int myServerID;
	private int myTimestamp;
	private Boolean timedOut = Boolean.valueOf(false);
	private String type;
	private String command;
	private int requestTimestamp;

	public ReleaseThread(String address, int port, int myServerID, int myTimestamp, String type, String command, int requestTimestamp){
		this.address = address;
		this.port = port;
		this.myServerID = myServerID;
		this.myTimestamp = myTimestamp;
		this.timedOut = Boolean.valueOf(false);
		this.type = type;
		this.command = command;
		this.requestTimestamp = requestTimestamp;
	}

	//Returns 0 if cannot connect/no response, else returns Lamport Timestamp
	public Integer call(){
		try(
			Socket s = new Socket();
		){
			try{
				s.connect(new InetSocketAddress(address, port), 100);
			} catch(Exception e){
				return new Integer(0); //no response, unable to connect
			}
			PrintWriter out =
          		new PrintWriter(s.getOutputStream(), true);
        	BufferedReader in =
          		new BufferedReader(
            		new InputStreamReader(s.getInputStream()));
          	String releaseString = "release " + myServerID;
          	System.out.println("Sending " + releaseString + " to " + port);
          	out.println(releaseString);
          	//Return 0 for grant, 1 for failed
		} catch(Exception e){ 
			e.printStackTrace(); 
			return new Integer(0);
		}
		return new Integer(0);
	}
}