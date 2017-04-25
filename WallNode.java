import java.net.*;
import java.io.*;
import java.util.*; 
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

public class WallNode {
	public static int clock;
	public static int myID;
	public static List<Request> requestQueue = new ArrayList<Request>();
	public static List<Integer> wallStructure = new ArrayList<Integer>();
	public static List<List<Integer>> wallPorts = new ArrayList<>();
	public static List<Integer> myRequestSetIndexes = new ArrayList<Integer>();
	public static List<Integer> myRequestSetPorts = new ArrayList<Integer>();
	public static List<Integer> grantsFromRequestSetNodes = new ArrayList<Integer>();
	public static int outstandingGrantID = -1;
	public static int outstandingGrantPriority = -1;
	public static Lock queueLock = new ReentrantLock();
  	public static Condition notAllGranted;

	public static void main (String args[]){
		System.out.println("Enter numNodes, nodeID, numRows, then structure");
		
		//LOCK FOR QUEUE
    	notAllGranted = queueLock.newCondition();
		Scanner sc = new Scanner(System.in);
		int numNodes = sc.nextInt();
		myID = sc.nextInt();
		int numRows = sc.nextInt();
		clock = 0;
		for(int i = 0; i < numRows; i++){
			wallStructure.add(new Integer(sc.nextInt()));
		}
		System.out.println("[DEBUG] numNodes: " + numNodes);
		System.out.println("[DEBUG] my id: " + myID);
		System.out.println("[DEBUG] numRows: " + numRows);

		//DUMMY ENQUEUE for id 0
		// if(myID == numNodes - 1){
		// 	enqueueRequest(new Request(-2, -1, "dummy"));
		// 	outstandingGrantID = -2;
		// 	outstandingGrantPriority = -1;
		// }

		

    	// for(int i = 0; i < numRows; i++){
    	// 	System.out.println("[DEBUG] Server Row " + i + ": " + wallStructure.get(i));
    	// }
    	//Establish ports
    	int portNumber = 48620;
    	for(int i = 0; i < wallStructure.size(); i++){
    		wallPorts.add(new ArrayList<Integer>());
    		//System.out.println("[DEBUG] Row " + i + ":");
    		for(int j = 0; j < wallStructure.get(i); j++){
    			wallPorts.get(i).add(new Integer(portNumber));
    			//System.out.println("[DEBUG] Port: " + wallPorts.get(i).get(j));
    			portNumber++;
    			grantsFromRequestSetNodes.add(new Integer(0));
    		}
    	}
    	//Establish own position
    	int myRow = 0;
    	int myCol = 0;
    	int nodeCount = 0;
    	for(int i = 0; i < numRows; i++){
    		nodeCount += wallStructure.get(i);
    		if(myID < nodeCount){
    			myRow = i;
    			break;
    		}
    	}
    	boolean foundColumn = false;
    	nodeCount = 0;
    	for(int i = 0; i < numRows; i++){
    		for(int j = 0; j < wallStructure.get(i); j++){
    			if(nodeCount == myID){
    				System.out.println("nodeCount: " + nodeCount + " myID: " + myID);
    				myCol = j;
    				foundColumn = true;
    				break;
    			}
    			nodeCount++;
    		}
    		if(foundColumn)
    			break;
    	}
    	System.out.println("[DEBUG] myRow: " + myRow);
    	System.out.println("[DEBUG] myCol: " + myCol);


		//DUMMY ENQUEUE
		if(myRow == wallStructure.size()-1){
			enqueueRequest(new Request(-2, -1, "dummy"));
			outstandingGrantID = -2;
			outstandingGrantPriority = -1;
		}

    	//Establish request set
    	//Note all own row port numbers
    	for(int i = 0; i < wallStructure.get(myRow); i++){
    		myRequestSetPorts.add(new Integer(wallPorts.get(myRow).get(i)));
    	}

    	//Note port for each row below
    	int rowSize = -1;
    	for(int i = myRow + 1; i < numRows; i++){
    		rowSize = wallStructure.get(i);
    		myRequestSetPorts.add(wallPorts.get(i).get(myID%rowSize));
    	}
    	for(int i = 0; i < myRequestSetPorts.size(); i++){
    		System.out.println("[DEBUG] request port: " + myRequestSetPorts.get(i));
    	}
    	
    	//Note index of each node in request set
    	nodeCount = 0;
    	int runningRowTotal = 0;
    	for(int i = 0; i < numRows; i++){
    		for(int j = 0; j < wallStructure.get(i); j++){
    			// if(i == myRow){
    			// 	myRequestSetIndexes.add(new Integer(nodeCount));
    			// 	nodeCount++;
    			// }
    			// else if(i > myRow){
    			// 	System.out.println("nodeCount: " + nodeCount);
    			// 	System.out.println("i: " + i);
    			// 	myRequestSetIndexes.add(nodeCount + myID%wallStructure.get(i));
    			// 	nodeCount+=wallStructure.get(i);
    			// 	break;
    			// }
    			// else{
    			// 	nodeCount++;
    			// }
    			if(i < myRow){
    				break;
    			}
    			else if(i == myRow){
    				myRequestSetIndexes.add(runningRowTotal + j);
    			}
    			else if(i > myRow){
    				myRequestSetIndexes.add(runningRowTotal + (myCol%wallStructure.get(i)));
    				break;
    			}
    		}
    		runningRowTotal += wallStructure.get(i);
    	}
    	for(int i = 0; i < myRequestSetIndexes.size(); i++){
    		System.out.println("[DEBUG] request index: " + myRequestSetIndexes.get(i));
    	}



    	ReceiveMessagesThread rmt = new ReceiveMessagesThread(wallPorts.get(myRow).get(myCol));
    	Thread rmThread = new Thread(rmt);
    	rmThread.start();
    	sc.nextLine();
    	while(sc.hasNext()){
    		String nextInput = sc.nextLine();
    		if(nextInput.equals("deq")){
    			System.out.println("Done with CS!!");
				clearAllGrantsStatus();
				System.out.println("All grant status cleared!");
				sendReleaseToRequestSet();
				System.out.println("Release sent to request set!");
    		}
    		else
    			requestCS(nextInput);

    	}


	}


	public static void requestCS(String command){
		//System.out.println(command);

		//Signal CS request and receive ack
		signalRequestToOtherServers("request", command, clock);
		incrementClock();
		//Check if we have received all grants - lock if not
		queueLock.lock();
		try{
			while(!checkIfAllGrantsReceived()){
			  notAllGranted.await();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			queueLock.unlock();
		}
		checkAndSetGrantID(myID);
		outstandingGrantPriority = requestQueue.get(0).timestamp;
		//Simulate using CS
		try{
			Thread.sleep(5000);
		}
		catch (InterruptedException e){
			e.printStackTrace();
		}
		System.out.println("Done with CS!!");
		clearAllGrantsStatus();
		System.out.println("All grant status cleared!");
		sendReleaseToRequestSet();
		System.out.println("Release sent to request set!");
	}

	public static synchronized void enqueueRequest(Request req){
	    System.out.println("Queue before enqueue:");
	    for(int i = 0; i < requestQueue.size(); i++){
	      System.out.println(requestQueue.get(i));
	    }
	    System.out.println("Enqueueing Request: " + req);
	    requestQueue.add(req);
	    Collections.sort(requestQueue);
	    System.out.println("Queue after enqueue and sort:");
	    for(int i = 0; i < requestQueue.size(); i++){
	      System.out.println(requestQueue.get(i));
	    }
	}

	public static synchronized Request dequeueRequest(){
	    System.out.println("Queue before dequeue:");
	    for(int i = 0; i < requestQueue.size(); i++){
	      System.out.println(requestQueue.get(i));
	    }
	    System.out.println("Dequeueing Request: " + requestQueue.get(0));
	    Request request = requestQueue.get(0);
	    requestQueue.remove(0);
	    Collections.sort(requestQueue);
	    if(requestQueue.size() == 0){
	    	outstandingGrantID = -1;
	    	outstandingGrantPriority = -1;
	    }
	    else{
	    	outstandingGrantID = requestQueue.get(0).serverID;
	    	outstandingGrantPriority = requestQueue.get(0).timestamp;
	    }
	    
	    System.out.println("Queue after dequeue and sort:");
	    for(int i = 0; i < requestQueue.size(); i++){
	      System.out.println(requestQueue.get(i));
	    }
	    return request;
	}

	public static void signalRequestToOtherServers(String type, String command, int requestTimestamp){
	    //System.out.println("we are: " + type);
	    ExecutorService executor = Executors.newCachedThreadPool();
	    List<Callable<Integer>> requestTaskList = new ArrayList<Callable<Integer>>();
	    //Create request thread to be sent to each server
	    for(int i = 0; i < myRequestSetIndexes.size(); i++){
			requestTaskList.add(new RequestThread("localHost", myRequestSetPorts.get(i), myID, clock, "request", "request", clock));
	    }
	    //System.out.println(requestTaskList.size());
	    List<Future<Integer>> requestFutures = new ArrayList<Future<Integer>>();
	    try{
	      requestFutures = executor.invokeAll(requestTaskList);
	    } catch (InterruptedException e){
	      e.printStackTrace();
	    }
	    //System.out.println(requestFutures.size());
	    //Wait for response from all servers
	    List<Integer> requestResponses = new ArrayList<Integer>();
	    for(Future<Integer> future : requestFutures){
	      try{
	        Integer result = future.get();
	        requestResponses.add(result);
	      } catch (Exception e){ e.printStackTrace(); }
	    }
	    //Set grant status for request set based on futures
	    int index = 0;
	    System.out.println("request response count = " + requestResponses.size());
	    for(int i = 0; i < requestResponses.size(); i++){
	    	System.out.println("Received future " + requestResponses.get(i) + " from response");
	    	if( requestResponses.get(i) == 0){//received grant
	    		index = myRequestSetIndexes.get(i);
	    		grantsFromRequestSetNodes.set(index, new Integer(1));
	    	}
	    	else{//received failed
	    		System.out.println("Request set index for response processing: " + myRequestSetIndexes.get(i));
	    		grantsFromRequestSetNodes.set(myRequestSetIndexes.get(i), new Integer(-1));	    	
	    	}

	    }
	}

	public static void sendReleaseToRequestSet(){
		ExecutorService executor = Executors.newCachedThreadPool();
	    List<Callable<Integer>> releaseTaskList = new ArrayList<Callable<Integer>>();
	    //Create request thread to be sent to each server
	    for(int i = 0; i < myRequestSetPorts.size(); i++){
			releaseTaskList.add(new ReleaseThread("localHost", myRequestSetPorts.get(i), myID, clock, "release", "release", clock));
	    }
	    //System.out.println(requestTaskList.size());
	    List<Future<Integer>> releaseFutures = new ArrayList<Future<Integer>>();
	    try{
	      releaseFutures = executor.invokeAll(releaseTaskList);
	    } catch (InterruptedException e){
	      e.printStackTrace();
	    }
	}

	public static synchronized Integer getClock(){
		return clock;
	}

	public static synchronized void incrementClock(){
    	clock+=1;
  	}

  	public static synchronized void setClock(int newClk){
    	clock = newClk;
  	}

  	public static synchronized int checkAndSetGrantID(int requesterID){
  		if(outstandingGrantID == -1){
  			outstandingGrantID = requesterID;
  			return -1;
  		}
  		else
  			return outstandingGrantID;
  	}

  	//0 - n/a
  	//-1 - failed
  	//1 - granted
  	public static synchronized void setGrantFromRSStatus(int id, int status){
  		grantsFromRequestSetNodes.set(id, new Integer(status));
  	}

  	public static synchronized boolean checkIfAllGrantsReceived(){
  		for(int i = 0; i < myRequestSetIndexes.size(); i++){
  			if(grantsFromRequestSetNodes.get(myRequestSetIndexes.get(i)) != 1)
  				return false;
  		}
  		return true;
  	}

  	public static synchronized void clearAllGrantsStatus(){
  		for(int i = 0; i < grantsFromRequestSetNodes.size(); i++){
  			grantsFromRequestSetNodes.set(i, new Integer(0));
  		}
  	}

  	public static int findPortByID(int id){
  		int port = 0;
  		for(int i = 0; i < wallPorts.size(); i++){
  			for(int j = 0; j < wallPorts.get(i).size(); j++)
  				if(port == id){
  					port = wallPorts.get(i).get(j);
  					return port;
  				}
  				port++;
  		}
  		return port;
  	}
}