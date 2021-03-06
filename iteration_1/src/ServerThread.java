import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;

import ui.ConsoleUI;

public abstract class ServerThread extends Thread{
	
	protected boolean stopRequested = false;
	protected DatagramSocket sendReceiveSocket;
	protected ConsoleUI console;
	//INIT socket timeout variables
	protected static final int TIMEOUT = 5; //Seconds
	protected static final int MAX_TIMEOUTS = 5;
	protected int timeouts = 0;
	protected boolean retransmit = false;
	protected int blockNum = 1;
	protected boolean timeoutFlag = false;
	protected DatagramPacket sendPacket;
	protected DatagramPacket receivePacket;
	protected boolean retransmitDATA;
	protected boolean retransmitACK;
	protected long startTime;
	protected boolean verbose;
	protected DatagramPacket requestPacket;
	protected boolean errorFlag=false;
	
	public ServerThread(ThreadGroup group, String name, ConsoleUI console)
	{
		super(group,name);
		this.console=console;
	}
	
    public void RequestStop()
    {
    	stopRequested = true;
    }
    
    /* Closes sockets and before exit. */
	public void exitGraceFully() {
		if(sendReceiveSocket != null && sendReceiveSocket.isClosed())
		{
			sendReceiveSocket.close();
		}
		console.print("Server: Closing thread.");
	}
	
	protected void printReceivedPacket(DatagramPacket receivedPacket, boolean verbose){
		console.print("Server: Received packet...");
		if(verbose){
			byte[] data = receivedPacket.getData();
			int packetSize = receivedPacket.getLength();
	
			console.printIndent("Source: " + receivedPacket.getAddress());
			console.printIndent("Port:      " + receivedPacket.getPort());
			console.printIndent("Bytes:   " + packetSize);
			console.printByteArray(data, packetSize);
			console.printIndent("Cntn:  " + (new String(data,0,packetSize)));
		}
	}
	
	protected void printSendPacket(DatagramPacket sendPacket, boolean verbose){
		console.print("Server: Sending packet...");
		if(verbose)
		{
			byte[] data = sendPacket.getData();
			int packetSize = sendPacket.getLength();

			console.printIndent("Source: " + sendPacket.getAddress());
			console.printIndent("Port:      " + sendPacket.getPort());
			console.printIndent("Bytes:   " + packetSize);
			console.printByteArray(data, packetSize);
			console.printIndent("Cntn:  " + (new String(data,0,packetSize)));
			
		}
	}
	
    protected void printError(DatagramPacket packet,boolean verbose){
    	console.print("Server: Error packet received");
    	console.print("From client: " + packet.getAddress());
    	console.print("From client port: " + packet.getPort());
	    console.print("Length: " + packet.getLength());
	    console.print("Error Code: " + new String(packet.getData(),
				   2,2));
	    console.print("ErrorMessage: " );
	    console.print(new String(packet.getData(),
				   4,packet.getData().length-1));
    }
    /* Send Data packet with no data
    2 bytes    2 bytes       0 bytes
    ---------------------------------
DATA  | 03    |   Block #  |    Data    |
    ---------------------------------
    */
    protected void sendNoData(DatagramPacket receivePacket,boolean verbose,int blockNumber,DatagramSocket sendReceiveSocket){
    	byte[] data = new byte[4];
    	data[0] = 0;
    	data[1] = 3;
		//Encode the block number into the response block 
		data[3]=(byte)(blockNumber & 0xFF);
		data[2]=(byte)((blockNumber >> 8)& 0xFF);
    	
    	DatagramPacket sendPacket = new DatagramPacket(data, data.length,
			     receivePacket.getAddress(), receivePacket.getPort());
	/* Exit Gracefully if the stop is requested. */
	   if(stopRequested){exitGraceFully();}
       console.print("Server: Sending packet:");
       printSendPacket(sendPacket, verbose);

      	try {
      		sendReceiveSocket.send(sendPacket);
      	} catch (IOException e) {
      		e.printStackTrace();
      		System.exit(1);
      	}
      	long startTime = System.currentTimeMillis();
      	/* Exit Gracefully if the stop is requested. */
      	if(stopRequested){exitGraceFully();}
      	if(verbose){
      		console.print("Server: packet sent using port " + sendReceiveSocket.getLocalPort()+"\n");
      	}
    }
    
    
    //Build an Error Packet with format :
    /*
    2 bytes  2 bytes        string    1 byte
    ----------------------------------------
ERROR | 05    |  ErrorCode |   ErrMsg   |   0  |
    ----------------------------------------
    */
    protected void buildError(int errorCode,DatagramPacket receivePacket, boolean verbose){
    	int errorSizeFactor = 5;
    	
    	String errorMsg = new String("Unknown Error.");
    	switch(errorCode){
	    	case 1:
	    		errorCode = 1;
	    		console.print("Server: File not found, sending error packet");
	    		errorMsg = "File not found.";
	    		break;
	    	case 2: 
	    		errorCode = 2;
	    		console.print("Server: Access violation, sending error packet");
	    		errorMsg = "Access violation.";
	    		break;
	    	case 3: 
	    		errorCode = 3;
	    		console.print("Server: Disk full or allocation exceeded, sending error packet");
	    		errorMsg = "Disk full or allocation exceeded.";
	    		break;
	    	case 6: 
	    		errorCode = 6;
	    		console.print("Server: File already exists, sending error packet");
	    		errorMsg = "File already exists.";
	    		break;
    	}
    	
    	byte[] data = new byte[errorMsg.length() + errorSizeFactor];
    	data[0] = 0;
    	data[1] = 5;
    	data[2] = 0;
    	data[3] = (byte)errorCode;
    	for(int c = 0; c<errorMsg.length();c++){
    		data[4+c] = errorMsg.getBytes()[c];
    	}
    	data[data.length-1] = 0;
    	
	    DatagramPacket sendPacket = new DatagramPacket(data, data.length,
				     receivePacket.getAddress(), receivePacket.getPort());
		/* Exit Gracefully if the stop is requested. */
		   if(stopRequested){exitGraceFully();}
		   printSendPacket(sendPacket,verbose);

	       	try {
	       		sendReceiveSocket.send(sendPacket);
	       	} catch (IOException e) {
	       		e.printStackTrace();
	       		System.exit(1);
	       	}
	       	/* Exit Gracefully if the stop is requested. */
	       	if(stopRequested){exitGraceFully();}
	       	if(verbose){
	       		console.print("Server: packet sent using port " + sendReceiveSocket.getLocalPort()+"\n");
	       	}
    	
    }
    
   
  //receive ACK
  	public boolean receiveACK()
  	{	
  		timeoutFlag=false;
  		//Encode the block number into the response block 
  		byte[] blockArray = new byte[2];
  		blockArray[1]=(byte)(blockNum & 0xFF);
  		blockArray[0]=(byte)((blockNum >> 8)& 0xFF);
  		console.print("Server: Waiting to receive packet");


  		//receive ACK
  		try {
  			//receiveDATA();
  			sendReceiveSocket.receive(receivePacket);
  			retransmit=false;
  		} catch(SocketTimeoutException e){
  			//Retransmit every timeout
  			//Quite after 5 timeouts
  			timeoutFlag=true;
  			if(System.currentTimeMillis() -startTime > TIMEOUT)
  			{
  				timeouts++;
  				if(timeouts == MAX_TIMEOUTS){
  					exitGraceFully();
  					requestStop();
  					errorFlag=true;
  					return false;
  				}
  				console.print("TIMEOUT EXCEEDED: SETTING RETRANSMIT TRUE");
  				retransmitDATA = true;
  				return true;
  			}
  			return false;


  		} catch (IOException e) {
  			// TODO Auto-generated catch block
  			e.printStackTrace();
  			return false;
  		} 
  		//analyze ACK for format
  		if (verbose)
  		{
  			console.print("Client: Checking ACK...");
  			printReceivedPacket(receivePacket, verbose);
  		}
  		byte[] data = receivePacket.getData();

  		//check ACK for validity
  		if(data[0] == 0 && data[1] == 4){

  			//Check if the blockNumber corresponds to the expected blockNumber
  			if(blockArray[1] == data[3] && blockArray[0] == data[2]){
  				blockNum++;
  				timeouts=0;
  				retransmitDATA=false;
  			}
  			else{
  				if (verbose)
  		  		{
  		  			console.print("Received Duplicate.");
  		  		}
  				if(System.currentTimeMillis() -startTime > TIMEOUT)
  				{
  					timeouts++;
  					if(timeouts == MAX_TIMEOUTS){
  						exitGraceFully();
  						errorFlag=true;
  	  					return false;
  					}
  					retransmitDATA=true;
  					console.print("TIMEOUT EXCEEDED: SETTING RETRANSMIT TRUE");
  					return true;
  				}
  				return false;
  			}
  		}
  		else{
  			//ITERATION 5 ERROR
  			//Invalid TFTP code
  		}
  		return true;
  	}

  //receive ACK
  	public boolean receiveDATA()
  	{	
  		timeoutFlag=false;
  		//Encode the block number into the response block 
  		byte[] blockArray = new byte[2];
  		blockArray[1]=(byte)(blockNum & 0xFF);
  		blockArray[0]=(byte)((blockNum >> 8)& 0xFF);
  		try {
  			//receiveDATA();
  			sendReceiveSocket.receive(receivePacket);
  			retransmit=false;
  		} catch(SocketTimeoutException e){
  			//Retransmit every timeout
  			//Quite after 5 timeouts

  			if(System.currentTimeMillis() -startTime > TIMEOUT)
  			{
  				timeouts++;
  				timeoutFlag=true;
  				if(timeouts == MAX_TIMEOUTS){
  					exitGraceFully();
  					requestStop();
  					errorFlag=true;
  					return false;
  				}
  				console.print("TIMEOUT EXCEEDED: SETTING RETRANSMIT TRUE");
  				retransmitACK = true;
  				return true;
  			}
  			return false;

  		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
  		//analyze ACK for format
  		if (verbose)
  		{
  			console.print("Server: Checking DATA...");
  		}
  		byte[] data = receivePacket.getData();

  		//check if data
  		if(data[0] == 0 && data[1] == 3){

  			//Check if the blockNumber corresponds to the expected blockNumber
  			if(blockArray[1] == data[3] && blockArray[0] == data[2]){
  				blockNum++;
  				timeouts=0;
  				retransmitACK=false;
  			}
  			else{
  				if (verbose)
  		  		{
  		  			console.print("Received Duplicate Packet: ");
  		  			printReceivedPacket(receivePacket, verbose);
  		  		}
  				if(System.currentTimeMillis() -startTime > TIMEOUT)
  				{
  					timeouts++;
  					if(timeouts == MAX_TIMEOUTS){
  						//close();
  						errorFlag=true;
  	  					return false;
  					}
  					console.print("TIMEOUT EXCEEDED: SETTING RETRANSMIT TRUE");
  					retransmitACK=true;
  					return true;
  				}
  				return false;
  			}
  		}
  		else{
  			return false;
  		}
  		return true;
  	}
  	protected void requestStop()
  	{
  		stopRequested=true;
  	}
  	
  	@Override 
  	public void interrupt()
  	{
  		super.interrupt();
  		requestStop();
  	}

}
