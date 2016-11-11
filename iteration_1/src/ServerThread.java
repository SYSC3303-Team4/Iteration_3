import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;

import ui.ConsoleUI;

public abstract class ServerThread extends Thread{
	
	protected boolean stopRequested = false;
	protected DatagramSocket sendReceiveSocket;
	protected ConsoleUI console;
	//INIT socket timeout variables
	protected static final int TIMEOUT = 1; //Seconds
	protected static final int MAX_TIMEOUTS = 5;
	protected int timeouts = 0;
	protected boolean retransmit = false;
	
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
		console.print("Server: Exiting Gracefully");
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

}
