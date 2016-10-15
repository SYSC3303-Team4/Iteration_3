/**
 *Class:             TFTPReadThread.java
 *Project:           TFTP Project - Group 4
 *Author:            Nathaniel Charlebois                                            
 *Date of Update:    29/09/2016                                              
 *Version:           1.1.0                                                      
 *                                                                                    
 *Purpose:           Handles the RRQs by:
 *						-Sending DATA
 *						-Receiving ACK
 *
 *To do:
 *	-Clean up code 
 *	-Test functionality with other components
 *	-Use the UI
 *
 *					
 *  
 *  
 *Update Log:    	v1.1.1
 *                       - null
 *                       
 *                  v1.1.2
 *                   	- Cleaned up code
 *                   	- Fixed Block numbers
 *                   
*                 	v1.1.3
*                 		-Corrected Error handling
*                		-Fixed many a bug
*                		-Refactored printing code 
 */
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.file.AccessDeniedException;
import java.util.*;

import javax.swing.JTextArea;

class TFTPReadThread  extends ServerThread
{
	/**
	 * The text area where this thread's output will be displayed.
	 */
	private JTextArea transcript;
	private DatagramPacket receivePacket;
	private DatagramPacket sendPacket;
	private DatagramSocket sendReceiveSocket;
	private boolean verbose;
	private static int blockNumber = 1;
	boolean sendZeroDataPacket = false;
	private String threadNumber;
	public static final byte[] response = {0, 3, 0, 0};


	public TFTPReadThread(ThreadGroup group,JTextArea transcript, DatagramPacket receivePacketInfo, String thread, Boolean verboseMode) {
		super(group,thread);
		this.transcript = transcript;
		receivePacket = receivePacketInfo;
		threadNumber  = thread;
		verbose = verboseMode;

		System.out.println(verboseMode);
		try {
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException e) {
			// TODO Auto-generated catch block    
			e.printStackTrace();
		}
	}
	

	public void run() {
		
		
		printReceivedPacket(receivePacket, verbose);

			/* Exit Gracefully if the stop is requested. */
			if(isInterrupted()){exitGraceFully();return;}
			//Parsing Data for filename
			ByteArrayOutputStream filename = new ByteArrayOutputStream();
			ByteArrayOutputStream mode = new ByteArrayOutputStream();
			boolean change = false; 
			for(int i = 2; i<receivePacket.getData().length;i++){
				if(isInterrupted()){exitGraceFully();}
				if(receivePacket.getData()[i]>=32){
					if(change == false){
						filename.write(receivePacket.getData()[i]);
					}
					else{
						mode.write(receivePacket.getData()[i]);
					}
				}
				if(receivePacket.getData()[i]!=0){
					if(receivePacket.getData()[i+1] == 0){
						change = true;//switch to parse mode
						i++;
					}
				}
			}
			
	
			/* Exit Gracefully if the stop is requested. */
			if(isInterrupted()){exitGraceFully();return;}
			if(verbose){
				System.out.println("Request parsed for:");
				System.out.println("	Filename: " + new String(filename.toByteArray(),0,filename.toByteArray().length));
				System.out.println("	Mode: " + new String(mode.toByteArray(),0,mode.toByteArray().length) + "\n");
			}
			
			File file = new File(filename.toString());
			if(file.canRead() == false){
				buildError(2,receivePacket,verbose);
			}
	
			TFTPReader reader = new TFTPReader();
			try {
				reader.readAndSplit(filename.toString());
	
			} catch (FileNotFoundException e1) {
				buildError(1,receivePacket,verbose);
				e1.printStackTrace();
				//exit
			} catch (IOException e) {
				buildError(2,receivePacket,verbose);
				e.printStackTrace();
				//exit
			}
			

			while(!isInterrupted()){
		
				//Encode the block number into the response block 
				response[3]=(byte)(blockNumber & 0xFF);
				response[2]=(byte)((blockNumber >> 8)& 0xFF);
				
	
				//Building datagram		
	
	
				byte[] data = reader.pop();
				//Check if the server needs to send a data Packet with 0 bytes
				if(data!=null){
					if(data.length==512 && reader.peek()==null){
						sendZeroDataPacket = true;
					}
				}

				/* If there's no more data to be read exit. */
				if(data == null){
					if(sendZeroDataPacket == true){
						sendNoData(receivePacket,verbose, blockNumber,sendReceiveSocket);
						//Waiting to receive final ACK
						byte[] finalACK = new byte[4];
						DatagramPacket finalACKPacket = new DatagramPacket(finalACK, finalACK.length);
						try {
							sendReceiveSocket.receive(finalACKPacket);
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						} 
						
						printReceivedPacket(finalACKPacket, verbose);
					}
					System.out.println("Read Request has completed.");
					exitGraceFully();
				}	
				//Builds the datagram in format
				/*
				2 bytes    2 bytes       n bytes 
				---------------------------------
			 DATA  | 03    |   Block #  |    Data    |
				---------------------------------
				 */
	
				byte dataPrime[] = Arrays.copyOf(response, response.length + data.length); 
				System.arraycopy(data, 0, dataPrime, response.length, data.length);
	
				/* Exit Gracefully if the stop is requested. */
				if(isInterrupted()){continue;}
				sendPacket = new DatagramPacket(dataPrime, dataPrime.length,
						receivePacket.getAddress(), receivePacket.getPort());
				
				printSendPacket(sendPacket, verbose);
	
				// Send the datagram packet to the client via a new socket.
				try {
					sendReceiveSocket.send(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1); 
				}
	
				/* Exit Gracefully if the stop is requested. */
				if(isInterrupted()){continue;}
				if(verbose){
				System.out.println("Server: packet sent using port " + sendReceiveSocket.getLocalPort());
				System.out.println();
				}
	
				//Waiting to receive ACK
				try {
					sendReceiveSocket.receive(receivePacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				} 
				
				printReceivedPacket(receivePacket, verbose);
				//Check for ACK in format  
				/*
				2 bytes    2 bytes
				-------------------
			 ACK   | 04    |   Block #  |
				--------------------
				 */
				if(receivePacket.getData()[0] == 0 && receivePacket.getData()[1] == 4){
					blockNumber++;
				}
			}
			exitGraceFully();
		}

	}