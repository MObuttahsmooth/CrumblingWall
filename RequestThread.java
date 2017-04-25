import java.util.*;
import java.util.concurrent.*;
import java.net.*;
import java.io.*;

public class RequestThread implements Callable<Integer>{
	private int port;
	private String address;
	private int myServerID;
	private int myTimestamp;
	private Boolean timedOut = Boolean.valueOf(false);
	private String type;
	private String command;
	private int requestTimestamp;

	public RequestThread(String address, int port, int myServerID, int myTimestamp, String type, String command, int requestTimestamp){
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
          	String requestString = command + " " + myServerID + " " + myTimestamp;
          	System.out.println("Sending " + requestString + " to " + port);
          	out.println(requestString);
          	//Return 0 for grant, 1 for failed
          	String line;
          	while((line = in.readLine()) != null){
          		//System.out.println(line);
          		String[] splitIn = line.split(" ");
          		if(splitIn[0].equals("ack")){
          			System.out.println("We are " + WallNode.myID + " Got ack!!");
          		}
          		else if(splitIn[0].equals("grant")){
          			System.out.println("Received grant!!");
          			return new Integer(0);
          		}
          		else if(splitIn[0].equals("failed")){
          			System.out.println("Received failed!!");
          			return new Integer(1);
          		}
          		int localClk = WallNode.getClock();
				int requestClk = Integer.parseInt(splitIn[1]);
				 if(localClk < requestClk)
				 	WallNode.setClock(requestClk + 1);
				 else
				 	WallNode.setClock(localClk + 1);
				 return new Integer(0);
          	}
		} catch(Exception e){ 
			e.printStackTrace(); 
			return new Integer(0);
		}
		return new Integer(0);
	}
}