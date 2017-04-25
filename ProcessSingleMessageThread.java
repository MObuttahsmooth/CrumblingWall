import java.net.*;
import java.io.*;
import java.util.*; 
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class ProcessSingleMessageThread implements Runnable {
  Socket socket = null;
  public int myClock = 0;

  public ProcessSingleMessageThread(Socket socket){
    this.socket = socket;
  }

  public void run(){
    try(
      PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
          BufferedReader in = new BufferedReader(
              new InputStreamReader(
                  socket.getInputStream()));
    ){
      String inputLine, outputLine;
      while ((inputLine = in.readLine()) != null){
        String[] splitIn = inputLine.split(" ");
        System.out.println("[DEBUG] Started running ProcessSingleThread");
        if (splitIn[0].equals("request")){
          //PUT REQUEST IN QUEUE command id timestamp
          WallNode.enqueueRequest(new Request(Integer.parseInt(splitIn[1]), Integer.parseInt(splitIn[2]), splitIn[0]));
          //UPDATE CLOCK TO REFLECT MAXIMUM IN REQUEST CLOCK VS LOCAL CLOCK, THEN INCREMENT CLOCK
          int localClk = WallNode.getClock();
          int requestClk = Integer.parseInt(splitIn[2]);
          if(localClk < requestClk)
            WallNode.setClock(requestClk + 1);
          else
            WallNode.setClock(localClk + 1);
          System.out.println("New clock is " + WallNode.getClock());

          //TO-DO respond appropriately
          int requesterID = Integer.parseInt(splitIn[1]);
          int requesterTimestamp = Integer.parseInt(splitIn[2]);
          int grantID = WallNode.checkAndSetGrantID(requesterID);
          //Determine correct response to request
          if(grantID == -1){//no outstanding grant
            //Send grant to requester
            out.println("grant " + WallNode.myID);
          }
          else{
            // int tempPri = WallNode.outstandingGrantPriority;
            // if(tempPri <= requesterTimestamp){
              //Send failed message
              out.println("failed " + WallNode.myID);
            //}
            //TODO
            // else{
            //   //Send inquire to grantID process
            //   System.out.println("should Be Sending Inquire from me: " + WallNode.myID);

            // }
          }
          //

        }
        //TODO
        else if(splitIn[0].equals("inquire")){
          System.out.println("Received inquire");
        }
        else if(splitIn[0].equals("release")){
          System.out.println("Received release");
          //Dequeue request
          WallNode.dequeueRequest();
          //Don't send grant if no requests in queue
          if(WallNode.requestQueue.size() == 0){
            break;
          }
          //Send grant to head of queue
          int portOfHeadRequest = WallNode.findPortByID(WallNode.requestQueue.get(0).serverID);
          System.out.println("Port# for head of queue which we will send the grant to: " + portOfHeadRequest);
          WallNode.outstandingGrantID = WallNode.requestQueue.get(0).serverID;
          WallNode.outstandingGrantPriority = WallNode.requestQueue.get(0).timestamp;
          try (
            //Default: Connect to TCP Socket
            Socket socket2 = new Socket("localHost", WallNode.findPortByID(WallNode.requestQueue.get(0).serverID));
            PrintWriter out2 =
              new PrintWriter(socket2.getOutputStream(), true);
            BufferedReader in2 =
                new BufferedReader(
                    new InputStreamReader(socket2.getInputStream()));
          ){
            //Send grant message
            System.out.println("Sending grant to " + WallNode.requestQueue.get(0));
            out2.println("grant " + WallNode.myID + " " + WallNode.getClock());
          } catch (Exception e){
            e.printStackTrace();
          }
        }
        else if(splitIn[0].equals("grant")){
          System.out.println("Received delayed grant");
          WallNode.setGrantFromRSStatus(Integer.parseInt(splitIn[1]), 1);
          //Awaken sleeping request if there is one
          WallNode.queueLock.lock();
          try{
            WallNode.notAllGranted.signal();
          } catch (Exception e){
            e.printStackTrace();
          } finally {
            WallNode.queueLock.unlock();
          }
        }
      }
      socket.close();
    } catch (IOException e) {
          e.printStackTrace();
    }
  }

}