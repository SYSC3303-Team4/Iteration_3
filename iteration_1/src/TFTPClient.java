/**
*Class:             TFTPClient.java
*Project:           TFTP Project - Group 4
*Author:            Jason Van Kerkhoven                                             
*Date of Update:    07/11/2016                                              
*Version:           1.2.1                                                      
*                                                                                   
*Purpose:           Generates a datagram following the format of [0,R/W,STR1,0,STR2,0],
					in which R/W signifies read (1) or write (2), STR1 is a filename,
					and STR2 is the mode. Sends this datagram to the IntermediateHost
					and waits for response from intermediateHost. Repeats this
					process ten times, then sends a datagram packet that DOES NOT
					follow the expected format stated above. Waits for response from
					IntermediateHost. We DO NOT expect a response to the badly formated
					packet. Each datagram can be 512B max
* 
* 
*Update Log:		v1.2.1
*						- command list printed at startup (as per request from literally everyone)
*						- input method completely redesigned
*						- UI now handles input parsing
*						- is now more robust with inputs
*					v1.2.0
*						- UI implemented
*						- now should support multiple instances
*						- new commands added for UI
*					v1.1.5
*						- recieve method implimented
*						- generateDATA() method patched to send OPcode
*						- client now sends ACKs
*						- port error patched
*						- block numbers working now (shoutout to Nate for the code to do that)
*						- ACKs fixed FOR REAL THIS TIME
*						- Nate's block numbering fixed
*						- WRITER CLASS IMPLIMENTED FINALLY
*					v1.1.4 
*						- numerous dangerous accessors/mutators removed
*						  (they were [and should] never called)
*						- TFTPWriter class implemented
*						- ACKs now received (their validity not checked)
*						- method printDatagram(..) added for ease of printing
*						- method readAndEcho() repurposed to readPacket()
*					v1.1.3
*						- verbose formating altered
*						- RRQ/WRQ method now uses class constants as opposed to magic numbers
*						  (code smell removed)
*						- both generateRWRW(..) and generateDATA changed from public --> private
*						- sendAndEcho() renamed sendPacket() for naming accuracy, also now private
*						- IN_PORT_ERRORSIM renamed to IN_PORT_HOST to keep up proper naming conventions
*					v1.1.2
*						- now implements TFTPReader class
*						- can read data from file
*						- DOES NOT SEND ACKS
*						- DOES NOT HAVE BLOCK NUMS
*						- bug with java.nullpointer exception in send() method fixed
*						- some methods renamed
*						- generateDatagram(..) replaced with 2 new methods:
*								generateRWRQ(...) | for RRQ or WRQ
*								generateDATA()	  | for DATA
*						- sendAndEcho() is now private, only called from master send() method
*						- a ton of constants added (see class constants section)
*						- a bunch of old test code commented out - will remove in future when sure it
*						  will not be necessary
*					v1.1.0
*						- verbose mode added (method added)
*						- client can now send datagrams to errorSim OR
*						  directly to server (ie method testMode)
*						- renamed constant PORT --> IN_PORT_ERRORSIM
*						- added cnst IN_PORT_SERVER
*						- added var outPort
*						- added method testMode(...)
*						- added var verbose
*						- close method added
*						- name changed from 'Client' to 'TFTPClient'
*						 (are you happy now Sarah??!?!?!?!?!)
*					v1.0.0
*                       - null
*/



//import external libraries
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

//import packages
import ui.* ;

@SuppressWarnings("serial")
public class TFTPClient extends JFrame
{
	//declaring local instance variables
	private DatagramPacket sentPacket;
	private DatagramPacket receivedPacket;
	private DatagramSocket generalSocket;
	private boolean verbose;
	private int outPort;
	private TFTPReader reader;
	private TFTPWriter writer;
	private static Scanner scan= new Scanner(System.in);
	private static JTextArea fileChooserFrame;
	private File file;
	private JFileChooser fileChooser;
	private ConsoleUI console;
	private int blockNum = 0;
	private boolean duplicateACK = false;
	private boolean retransmitACK = false;
	private boolean retransmitDATA = false;
	
	//INIT socket timeout variables
	protected static final int TIMEOUT = 5; //Seconds
	protected static final int MAX_TIMEOUTS = 5;
	protected int timeouts = 0;
	
	//declaring local class constants
	private static final int IN_PORT_HOST = 23;
	private static final int IN_PORT_SERVER = 69;
	private static final int MAX_SIZE = 512;
	private static final int DATA_OFFSET = 4;
	private static final String DEFAULT_MODE = "ASCII";
	private static final byte[] OPCODE_RRQ =  {0,1}; 
	private static final byte[] OPCODE_WRQ =  {0,2};
	private static final byte[] OPCODE_DATA = {0,3};
	private static final byte[] OPCODE_ACK = {0,4};
	
	
	
	//generic constructor
	public TFTPClient()
	{
		//construct a socket, bind to any local port
		try
		{
			generalSocket = new DatagramSocket();
		}
		//enter if socket creation results in failure
		catch (SocketException se)
		{
			se.printStackTrace();
			System.exit(1);
		}
		//initialize echo --> off
		verbose = false;
		//initialize test mode --> off
		outPort = IN_PORT_SERVER ;
		//make an empty reader
		reader = new TFTPReader();
		//make an empty writer
		writer = new TFTPWriter();
		
		//make and run the UI
		console = new ConsoleUI("TFTPClient.java");
		console.run();
	}
	
	
	//generic accessors and mutators
	public DatagramPacket getSentPacket()
	{
		return sentPacket;
	}
	public DatagramPacket getreceivedPacket()
	{
		return receivedPacket;
	}
	
	
	//enable/disable verbose mode
	public void verboseMode(boolean v)
	{
		verbose = v;
		if(verbose)
		{
			console.print("Verbose mode set " + verbose);
		}
	}
	
	
	//enable/disable test mode
	public void testMode(boolean t)
	{
		//test mode ON
		if (t)
		{
			outPort = IN_PORT_HOST;
		}
		//test mode OFF
		else
		{
			outPort = IN_PORT_SERVER;
		}
		
		if(verbose)
		{
			console.print("Test mode set " + t);
		}
	}
	
	
	//close client properly
	//***FUNCTIONALITY OF CLIENT WILL CEASE ONCE CALLED***
	public void close()
	{
		//close sockets
		generalSocket.close();
	}
	
	
	//generate DatagramPacket, save as sentPacket
	//type: DATA
	private void generateDATAMaster(int blockNum, byte[] data)
	{
		//prep for block num
		byte[] blockNumArr = new byte[2];
		blockNumArr[1]=(byte)(blockNum & 0xFF);
		blockNumArr[0]=(byte)((blockNum >> 8)& 0xFF);
	    
		if(verbose)
		{
			console.print("Client: Prepping DATA packet #" + blockNum);
		}
		
		//construct array to hold data
		//byte[] data = reader.pop();
		byte[] toSend = new byte[data.length + 4];
		
		//construct array
		for(int i=0; i<2; i++)
		{
			toSend[i] = OPCODE_DATA[i];
		}
		for(int i = 2; i < 4; i++)
		{
			toSend[i] = blockNumArr[i-2] ;
		}
		for(int i = 0; i < data.length; i++)
		{
			toSend[i+4] = data[i] ;
		}
		
		//generate and save datagram packet
		try
		{
			sentPacket = new DatagramPacket(toSend, toSend.length, InetAddress.getLocalHost(), outPort);
			if(verbose)
			{
				console.print("Client: Packet successfully created");
			}
		}
		catch(UnknownHostException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	
	//generate DatagramPacket, save as sentPacket
	//type: DATA
	private void generate0Data(int blockNum)
	{
		generateDATAMaster(blockNum, new byte[0]);
	}
	
	
	//generate DatagramPacket, save as sentPacket
	//type: DATA
	private void generateData(int blockNum)
	{
		generateDATAMaster(blockNum, reader.pop());
	}
	
	
	//generate ACK
	private void generateACK(byte[] ACKNum)
	{
		byte[] ack = new byte[4];
		ack[0] = OPCODE_ACK[0];
		ack[1] = OPCODE_ACK[1];
		//add block num
		ack[2] = ACKNum[0];
		ack[3] = ACKNum[1];
		
		//generate and save datagram packet
		try
		{
			sentPacket = new DatagramPacket(ack, ack.length, InetAddress.getLocalHost(), outPort);
			if(verbose)
			{
				console.print("Client: ACK successfully created");
			}
		}
		catch(UnknownHostException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		
	}
	
	
	//generate DatagramPacket, save as sentPacket
	//type: RRW or WRQ
	private void generateRWRQ(String fileName, String mode, byte[] RWval)
	{
		//generate the data to be sent in datagram packet
		if(verbose)
		{
			console.print("Client: Prepping packet containing '" + fileName + "'...");
		}	
		//convert various strings to Byte arrays
		byte[] fileNameBA = fileName.getBytes();
		byte[] modeBA = mode.getBytes();
			
		//compute length of data being sent (metadata include) and create byte array
		byte[] data = new byte[fileNameBA.length + modeBA.length + 4];
		int i = 2;
			
		//add first 2 bytes of opcode
		for(int c=0; c<2; c++)
		{
			data[c] = RWval[c] ;
		}
		//add text
		for(int c=0; c<fileNameBA.length; c++, i++)
		{
			data[i] = fileNameBA[c];
		}
		//add pesky 0x00
		data[i] = 0x00;
		i++;
		//add mode
		for(int c=0; c<modeBA.length; c++, i++)
		{
			data[i] = modeBA[c];
		}
		//add end metadata
		data[i] = 0x00;
			
		
		//generate and save datagram packet
		try
		{
			sentPacket = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), outPort);
			if(verbose)
			{
				console.print("Client: Packet successfully created");
			}
		}
		catch(UnknownHostException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	
	//send datagram, recieve ACKs
	public void sendWRQ(String file, String mode)
	{
		//initial

		int oldPort = outPort; 
		int lastDATAPacketLength = 0;
		
		//read and split file
		try
		{
			reader.readAndSplit(file);
			
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		
		//prep RRQ/RRW to send
		generateRWRQ(file, mode, OPCODE_WRQ);
		//send RRQ/RRW
		sendPacket();
		//wait for ACK
		receiveACK();
		//change port to wherever ACK came from 
		outPort = receivedPacket.getPort();
		
		//send DATA
		while ( !(reader.isEmpty())  || lastDATAPacketLength == MAX_SIZE+4)
		{
			if(!duplicateACK){ 
				
				//send DATA
				if(reader.isEmpty())
				{
					generate0Data(blockNum);
				}
				else
				{
					generateData(blockNum);
				}
				lastDATAPacketLength = sentPacket.getLength();
				sendPacket();
				//blockNum++;
			
			}
			
			//wait for ACK
			receiveACK();
		}
		
		//reset port
		outPort = oldPort;
		blockNum = 0;
		console.print("----------------------WRQ COMPLETE----------------------");
	}
	
	
	//send a single packet
	private void sendPacket()
	{
		//print packet info IF in verbose
		if(verbose)
		{
			console.print("Client: Sending packet...");
			printDatagram(sentPacket);
		}
		//send packet
		try
		{
			generalSocket.send(sentPacket);   
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		console.print("Client: Packet Sent");
	}
	
	
	//receive ACK
	public void receiveACK()
	{	

		//Encode the block number into the response block 
		byte[] blockArray = new byte[2];
		blockArray[1]=(byte)(blockNum & 0xFF);
		blockArray[0]=(byte)((blockNum >> 8)& 0xFF);
		

			//receive ACK
			receivePacket("ACK");
			
			if(!retransmitACK){
				//analyze ACK for format
				if (verbose)
				{
					console.print("Client: Checking ACK...");
				}
				byte[] data = receivedPacket.getData();
				
				//check ACK for validity
				if(data[0] == 0 && data[1] == 4){
					
					//Check if the blockNumber corresponds to the expected blockNumber
					if(blockArray[1] == data[3] && blockArray[0] == data[2]){
						blockNum++;
					}
					else{
						duplicateACK = true;
					}
				}
				else{
					//ITERATION 5 ERROR
					//Invalid TFTP code
				}
			}
		
	}
	
	
	//receive data and save
	public void sendRRQ(String file, String mode)
	{
		int oldPort = outPort;
		
		//send read request
		generateRWRQ(file, mode, OPCODE_RRQ);
		sendPacket();
		
		//receive loop for data
		byte[] rawData;
		byte[] procData;
		boolean loop = true;
		
		while(loop)
		{
			byte[] blockNumByte = new byte[2];
			blockNumByte[1]=(byte)(blockNum & 0xFF);
			blockNumByte[0]=(byte)((blockNum >> 8)& 0xFF);
			//receive data
			receivePacket("DATA");
			if(!retransmitDATA){
				outPort = receivedPacket.getPort();
				
				//Process data
				rawData = new byte[receivedPacket.getLength()] ;
				rawData = receivedPacket.getData();
				procData = new byte[receivedPacket.getLength() - DATA_OFFSET];
	
				int reLen = receivedPacket.getLength(); 
				for(int i=0; i<reLen-DATA_OFFSET; i++)
				{
					procData[i] = rawData[i+DATA_OFFSET];
				}
				
				//save data
				try
				{
					writer.write(procData,"Received"+file);
				}
				catch(FileNotFoundException e)
				{
					e.printStackTrace();
					System.exit(1);
				}
				catch(IOException e)
				{
					e.printStackTrace();
					System.exit(1);
				}
				
				//check to see if this is final packet
				if (receivedPacket.getLength() < MAX_SIZE+4)	
				{
					loop = false;
				}
				
				//get block num
				blockNumByte[0] = rawData[2];
				blockNumByte[1] = rawData[3];
			}
			//send out ACK and prep for more data
			generateACK(blockNumByte);
			sendPacket();
		}
		
		console.print("----------------------RRQ COMPLETE----------------------");
		outPort = oldPort;
	}
	
	
	//receive and echo received packet
	public void receivePacket(String type)
	{	
		//prep for response
		byte[] response = new byte[MAX_SIZE+4];
		receivedPacket = new DatagramPacket(response, response.length);
		
		//wait for response
		if (verbose)
		{
			console.print("Client: Waiting for " + type + " packet...");
		}
		try
		{
			generalSocket.receive(receivedPacket);			
		}
		catch(IOException e)
		{
			if (e instanceof SocketTimeoutException){
				//Retransmit every timeout
				//Quite after 5 timeouts
				timeouts++;
				if(timeouts == 5){
					close();
				}
				if(type == "ACK"){
					retransmitACK = true;
				}
				else{
					retransmitDATA = true;
				}
			}
			else{					
				e.printStackTrace();
				System.exit(1);
			}
		}
		if (verbose)
		{
			console.print("Client: " + type + " packet received");
			printDatagram(receivedPacket);
		}
		
		//check for errors
		byte errorType=response[3];
		response = receivedPacket.getData();
		if(response[0] == 0 && response[1] == 5)
		{
			switch(errorType)
			{
				//file not found
		    	case 1:
		    			console.print("File not found, please select again");
		    			//start(this);
		    			break;
		    	//improper rights for R/W
		    	case 2:
		    			console.print("You do not have the rights for this, please select again");
		    			//start(this);
		    			break;
		    	//drive full
		    	case 3:
		    			console.print("Location full, please select a new location to write to");
		    			//start(this);
		    			break;
		    	//file already exists
		    	case 6:
		    			console.print("The file already exists, please select a new file");
		    			//start(this);
		    			break;
		    	//unknown error
		    	default:
		    			/* TO DO
		    			 * something
		    			 */
		    			break;
			}
		}
		//no error present
		else
		{

		}
	}
	

	//print datagram contents
	private void printDatagram(DatagramPacket datagram)
	{
		byte[] data = datagram.getData();
		int packetSize = datagram.getLength();

		console.printIndent("Source: " + datagram.getAddress());
		console.printIndent("Port:      " + datagram.getPort());
		console.printIndent("Bytes:   " + packetSize);
		console.printByteArray(data, packetSize);
		console.printIndent("Cntn:  " + (new String(data,0,packetSize)));
	}
	
	
	
	public void ClientMain()
	{
		//declaring local variables
		String input[] = null;
		boolean runFlag = true;
		
		//print starting text
		console.print("TFTPClient running");
		console.print("type 'help' for command list");
		console.print("~~~~~~~~~~~ COMMAND LIST ~~~~~~~~~~~");
		console.print("'help'                                   - print all commands and how to use them");
		console.print("'clear'                                  - clear screen");
		console.print("'close'                                 - exit client, close ports, be graceful");
		console.print("'verbose BOOL'                - toggle verbose mode as true or false");
		console.print("'testmode BOOL'             - if set true, sends to Host. If set false, sends to Server directly");
		console.print("'test'                                    - runs a test for the console");
		console.println();
		console.print("'push MODE'                    - push a file to the server in mode MODE (ex, ASCII)");
		console.print("'push'                                - push a file to the server in mode: ASCII");
		console.print("'pull FILENAME MODE'  - pull a file from the server in mode MODE (ex ASCII)");
		console.print("'pull FILENAME'               - pull a file from the server in mode: ASCII");
		console.println();
		console.print("'rrq FILENAME MODE'    - send a read request for file FILENAME in mode MODE");
		console.print("'wrq MODE'                       - send a write request in mode MODE");
		console.print("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		console.println();
		
		//main input loop
		while(runFlag)
		{
			//get PARSED user input
			input = console.getParsedInput(true);
			
			//process input based on param num
			switch(input.length)
			{
				case(1):
					
					//print commands
					if (input[0].equals("help"))
					{
						console.print("~~~~~~~~~~~ COMMAND LIST ~~~~~~~~~~~");
						console.print("'help'                                   - print all commands and how to use them");
						console.print("'clear'                                  - clear screen");
						console.print("'close'                                 - exit client, close ports, be graceful");
						console.print("'verbose BOOL'                - toggle verbose mode as true or false");
						console.print("'testmode BOOL'             - if set true, sends to Host. If set false, sends to Server directly");
						console.print("'test'                                    - runs a test for the console");
						console.println();
						console.print("'push MODE'                    - push a file to the server in mode MODE (ex, ASCII)");
						console.print("'push'                                - push a file to the server in mode: ASCII");
						console.print("'pull FILENAME MODE'  - pull a file from the server in mode MODE (ex ASCII)");
						console.print("'pull FILENAME'               - pull a file from the server in mode: ASCII");
						console.println();
						console.print("'rrq FILENAME MODE'    - send a read request for file FILENAME in mode MODE");
						console.print("'wrq MODE'                       - send a write request in mode MODE");
						console.print("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
						console.println();
					}
					//clear console
					else if (input[0].equals("clear"))
					{
						console.clear();
					}
					//close console with grace
					else if (input[0].equals("close"))
					{
						console.print("Closing with grace....");
						runFlag = false;
						this.close();
						System.exit(0);
					}
					//run simple console test
					else if (input[0].equals("test"))
					{
						console.testAll();
					}
					//push a file (WRQ) to server in default mode
					else if (input[0].equals("push"))
					{
						//get user file
						fileChooserFrame = new JTextArea(5,40);
						fileChooser = new JFileChooser();
						fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
						int result = fileChooser.showOpenDialog(fileChooser);
						if (result == JFileChooser.APPROVE_OPTION) {//file is found
						    file = fileChooser.getSelectedFile();//get file name
						}
						//enter WRQ protocol
						sendWRQ(file.getName(), DEFAULT_MODE);
					}
					//BAD (WOLF) INPUT
					else
					{
						console.print("! Unknown Input !");
					}
					break;
					
				case(2):
					//toggle verbose
					if (input[0].equals("verbose"))
					{
						if (input[1].equals("true"))
						{
							verboseMode(true);
						}
						else if (input[1].equals("false"))
						{
							verboseMode(false);
						}
						else
						{
							console.print("! Unknown Input !");
						}
					}
					//toggle test mode
					else if (input[0].equals("testmode"))
					{
						if (input[1].equals("true"))
						{
							testMode(true);
						}
						else if (input[1].equals("false"))
						{
							testMode(false);
						}
						else
						{
							console.print("! Unknown Input !");
						}
					}
					//push in mode and old wrq MODE 
					else if (input[0].equals("push") || input[0].equals("wrq"))
					{
						//get user file
						fileChooserFrame = new JTextArea(5,40);
						fileChooser = new JFileChooser();
						fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
						int result = fileChooser.showOpenDialog(fileChooser);
						if (result == JFileChooser.APPROVE_OPTION) {//file is found
						    file = fileChooser.getSelectedFile();//get file name
						}
						//enter WRQ protocol
						sendWRQ(file.getName(), input[1]);
					}
					//pull in default mode
					else if (input[0].equals("pull"))
					{
						sendRRQ(input[1], DEFAULT_MODE);
					}
					//BAD (WOLF) INPUT
					else
					{
						console.print("! Unknown Input !");
					}
					break;
				
				case(3):
					//pull FILE MODE or legacy rrq FILE MODE
					if (input[0].equals("pull") || input[0].equals("rrq"))
					{
						sendRRQ(input[1], input[2]);
					}
					//BAD (WOLF) INPUT
					else
					{
						console.print("! Unknown Input !");
					}
					break;
				
				default:
					//BAD (WOLF) INPUT
					console.print("! Unknown Input !");
					break;
			}
		}
	}
	
	
	public static void main (String[] args) 
	{
		//declaring local variables
		TFTPClient client = new TFTPClient();
		
		//run
		client.ClientMain();
	}
}