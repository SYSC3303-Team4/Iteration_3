/**
*Class:             TFTPWriterThread.java
*Project:           TFTP Project - Group 4
*Author:            Nathaniel Charlebois                                            
*Date of Update:    29/09/2016                                              
*Version:           1.0.0                                                      
*                                                                                    
*Purpose:           Handles the WRQs by:
*						-Receiving the WRQ
*						-Sending the special 0 ACK
*						-Receiving the next DATA
*						-Sending an ACK
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
*                 	v1.1.2
*                 		-Added Error handling
*                 		-Added Error creating and sending
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
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;

import javax.swing.JFileChooser;
import javax.swing.JTextArea;

import ui.ConsoleUI;

class TFTPWriteThread extends ServerThread
{
    /**
     * The text area where this thread's output will be displayed.
     */
    private DatagramPacket sendPacket;
    private DatagramPacket receivePacket;
    private DatagramPacket receivePacket1;
    private int blockNumber = 0;
	private boolean verbose;
    private String threadNumber;
    File file;

    public static final byte[] response = {0, 4, 0, 0};

	private boolean fileFlag = false;
    
    

    public TFTPWriteThread(ThreadGroup group,ConsoleUI transcript, DatagramPacket receivePacketInfo,String thread, Boolean verboseMode,File file) {
    	super(group,thread,transcript);
        receivePacket = receivePacketInfo;  
        threadNumber = thread;
        verbose = verboseMode;
        this.file = file;
        try {
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException e) {
			// TODO Auto-generated catch block    
			e.printStackTrace();
			console.print(e.getMessage());
		}
		try {
			sendReceiveSocket.setSoTimeout(TIMEOUT*1000);
		} catch (SocketException e) {
			//Handle Timeout Exception
			e.printStackTrace();
		}
    }

    public void run() {
		   

		   //Parsing Data for filename and mode 
		   ByteArrayOutputStream filename = new ByteArrayOutputStream();
		   ByteArrayOutputStream mode = new ByteArrayOutputStream();
		   boolean change = false; 
		   for(int i = 2; i<receivePacket.getData().length;i++){
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
					   change = true;
					   i++;
				   }
				}
		   }
		   printReceivedPacket(receivePacket, verbose);
		    /* Exit Gracefully if the stop is requested. */
	       if(isInterrupted()){exitGraceFully();return;}
	       if(verbose){
	    	   console.print("Request parsed for:");
	    	   console.print("	Filename: " + new String(filename.toByteArray(),
				   0,filename.toByteArray().length));
	    	   console.print("	Mode: " + new String(mode.toByteArray(),
				   0,mode.toByteArray().length) + "\n");
			}
    	
	       while(!isInterrupted()){
	
	
			   //Build and send the first ACK reply in format:
			   /*
			  2 bytes    2 bytes
			  -------------------
		   ACK   | 04    |   Block #  |
			  --------------------
			    */

			   if(blockNumber == 0){
				   sendPacket = new DatagramPacket(response, response.length,
				       receivePacket.getAddress(), receivePacket.getPort());
	
				   printSendPacket(sendPacket,verbose);
	
			       try {
				   sendReceiveSocket.send(sendPacket);
			       } catch (IOException e) {
				   e.printStackTrace();
				   System.exit(1);
			       }
			        /* Exit Gracefully if the stop is requested. */
					if(isInterrupted()){exitGraceFully(); return;}
					if(verbose){
			       console.print("Server: packet sent using port " + sendReceiveSocket.getLocalPort()+"\n");
					}
			   }
	    	   
	
		       //Wait for next DATA datagram in format:
		       /*
			  2 bytes    2 bytes       n bytes
			  ---------------------------------
		   DATA  | 03    |   Block #  |    Data    |
			  ---------------------------------
			*/
			   byte[] rawData = new byte[516];
			   receivePacket1 = new DatagramPacket(rawData, rawData.length);
			   
			    /* Exit Gracefully if the stop is requested. */
			   if(isInterrupted()){continue;}
			   
		       console.print("Server: Waiting for packet.");
		       // Block until a datagram packet is received from receiveSocket.
		       try {
		    	   sendReceiveSocket.receive(receivePacket1);
		       } catch (IOException e) {
					if (e instanceof SocketTimeoutException){
						//Retransmit every timeout
						//Quite after 5 timeouts
						timeouts++;
						if(timeouts == MAX_TIMEOUTS){
							exitGraceFully();
							return;
						}
						retransmit = true;
					}
					else{					
						e.printStackTrace();
						System.exit(1);
					}
		       }
		       if(!retransmit){
		       
		    	   printReceivedPacket(receivePacket1,verbose);
			       byte[] data = new byte[receivePacket1.getLength()-4];
	
			       //Parse data from DATA packet
			       for(int i = 4; i < receivePacket1.getLength();i++){
			    	   data[i-4] = receivePacket1.getData()[i];
			       }
			       
	
			       //Write file to directory
			       File fileName = new File(file.getAbsolutePath()+"/"+filename.toString());
			       
			       
			       TFTPWriter writer = new TFTPWriter();
			       if(fileName.exists() && fileFlag == false) { 
			    	   buildError(6,receivePacket,verbose);
			    	   return;
					}
			       fileFlag = true;
					
			       try {
						writer.write(data,file.getAbsolutePath()+"/"+filename.toString());
					} catch (AccessDeniedException e1) {
						buildError(2,receivePacket,verbose);
						e1.printStackTrace();
						return;
					} 
					catch(IOException e2){
						buildError(3,receivePacket,verbose);
						e2.printStackTrace();
						return;
					}
	
			       if(data.length<512){
			    	   if(verbose){
			    	   console.print("Server: Final Data Block Received.");
			    	   console.print("Server: Sending last ACK");
			    	   //SET INTERRUPT TO EXIT LOOP
			    	   
			    	   }
			    	   
			    	   exitGraceFully();
			    	   return;
			       }
	
			       //Sending the ACK for previous DATA packet in format:
			       /*
				  2 bytes    2 bytes
				  -------------------
			   ACK   | 04    |   Block #  |
				  --------------------
				*/
	
				   response[2]=receivePacket1.getData()[2];
				   response[3]=receivePacket1.getData()[3];
				   blockNumber++;
	
				   
				   
			       sendPacket = new DatagramPacket(response, response.length,
						     receivePacket.getAddress(), receivePacket.getPort());
					/* Exit Gracefully if the stop is requested. */
				   if(isInterrupted()){continue;}
				   printSendPacket(sendPacket,verbose);
	
			       try {
			    	   sendReceiveSocket.send(sendPacket);
			       } catch (IOException e) {
				  e.printStackTrace();
				  System.exit(1);
			       }
					/* Exit Gracefully if the stop is requested. */
				 if(isInterrupted()){continue;}
				 if(verbose){
			       console.print("Server: packet sent using port " + sendReceiveSocket.getLocalPort()+"\n");
				 }
		     }
	       
	    }
	    console.print("Server: thread closing.");
	    exitGraceFully();
	    return;
    }
    

}
