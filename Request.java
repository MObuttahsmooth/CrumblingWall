import java.net.*;
import java.io.*;
import java.util.*; 
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class Request implements Comparable<Request>{
	public int serverID;
	public int timestamp;
	public String command;

	public Request(int id, int ts, String cmd){
		timestamp = ts;
		serverID = id;
		command = cmd;
	}

	public Request(int id, int ts){
		serverID = id;
		timestamp = ts;
		command = "NOT MY CLIENT";
	}

	public boolean isMyRequest(int myID){
		return serverID == myID;
	}

	public String toString(){
		return "timestamp: " + timestamp + " ID: " + serverID + " command: " + command;
	}

	@Override
	public int compareTo(Request reqB){
		if(timestamp < reqB.timestamp)
			return -1;
		else if(timestamp == reqB.timestamp){
			if(serverID < reqB.serverID)
				return -1;
			else
				return 1;
		}
		else
			return 1;
	}

}