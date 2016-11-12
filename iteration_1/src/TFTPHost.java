/**
*Class:             TFTPHost.java
*Project:           TFTP Project - Group 4
*Author:            Jason Van Kerkhoven                                             
*Date of Update:    17/09/2016                                              
*Version:           1.0.0                                                      
*                                                                                    
*Purpose:           Receives packet from Client, sends packet to Server and waits
					for Server response. Sends Server response back to Client. Repeats
					this process indefinitely. Designed to allow for the simulation of errors and lost packets in future.
* 
* 
*Update Log:        v2.0.0
*						- input methods added (non-isr)
*						- input now saves to InputStack
*						- help menu added
*					v1.0.0
*                       - null
*/


//imports
import java.io.*;
import java.net.*;

import javax.swing.JFileChooser;
import javax.swing.JTextArea;

import inputs.*;

import ui.ConsoleUI;
import inputs.*;


public class TFTPHost 
{
	//declaring local instance variables
	private DatagramPacket sentPacket;
	private DatagramPacket receivedPacket;
	private DatagramSocket inSocket;
	private DatagramSocket genSocket;
	private int clientPort;
	private boolean verbose;
	private ConsoleUI console;
	private InputStack inputStack = new InputStack();
	
	
	//sarah var
	DatagramPacket nextGram=null;
		
	//declaring local class constants
	private static final int CLIENT_RECEIVE_PORT = 23;
	private static final int SERVER_RECEIVE_PORT = 69;
	private static final int MAX_SIZE = 512+4;
	private static final boolean LIT = true ; 	
	


	
	//generic constructor
	public TFTPHost()
	{
		//construct sockets
		try
		{
			inSocket = new DatagramSocket(CLIENT_RECEIVE_PORT);
			genSocket=new DatagramSocket();
		}
		//enter if socket creation results in failure
		catch (SocketException se)
		{
			se.printStackTrace();
			System.exit(1);
		}
		
		//initialize echo --> off
		verbose = false;
		
		//run UI
		console = new ConsoleUI("Error Simulator");
		console.run();
	}
	
	
	//basic accessors and mutators
	public DatagramSocket getInSocket()
	{    
		return inSocket;
	}
	public void setClientPort(int n)
	{
		clientPort = n;
	}
	public int getClientPort()
	{
		return clientPort;
	}
	public DatagramPacket getReceivedPacket()
	{
		return receivedPacket;
	}
	public void setVerbose(boolean f)
	{
		verbose = f;
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
	
	
	
	//receive packet on inPort
	public void receiveDatagram(DatagramSocket inputSocket)
	{
		//construct an empty datagram packet for receiving purposes
		byte[] arrayholder = new byte[MAX_SIZE];
		receivedPacket = new DatagramPacket(arrayholder, arrayholder.length);
		
		//wait for incoming data
		console.print("Waiting for data...");
		try
		{
			inputSocket.receive(receivedPacket);
		}
		catch (IOException e)
		{
			System.out.print("Incoming socket timed out\n" + e);
			e.printStackTrace();
			System.exit(1);
		}
		
		//deconstruct packet and print contents
		console.print("Packet successfully received");
		printDatagram(receivedPacket);
	}
	
	public DatagramPacket tryReceive(DatagramSocket inputSocket,int timeOut) throws IOException
	{
		byte[] arrayholder = new byte[MAX_SIZE];
		DatagramPacket incommingPacket = new DatagramPacket(arrayholder, arrayholder.length);
		
		//set delay
		try
		{
			inputSocket.setSoTimeout(timeOut);
		}
		catch (SocketException ioe)
		{
			console.printError("Cannot set socket timeout");
		}
		
		//wait for incoming data
		console.print("Waiting for data...");
		inputSocket.receive(incommingPacket);

		
		//deconstruct packet and print contents
		console.print("Packet successfully received");
		if (verbose)
		{
			printDatagram(incommingPacket);
		}
		
		return incommingPacket;

	}
	
	//send packet to server and wait for server response
	/**
	 * 
	 */
	public void sendDatagram(int outPort, DatagramSocket socket)
	{
		//prep packet to send
		console.print("Sending packet...");
		sentPacket = receivedPacket;
		sentPacket.setPort(outPort );
		
		//print contents
		printDatagram(sentPacket);
		
		//send packet
		try
		{
			socket.send(sentPacket);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		console.print("Packet successfully sent");
		
	}
	public void passIt(int mode,int delay,int clientPort,DatagramSocket genSocket )
	{
		if(mode==0)//delay
		{
			console.print("Delaying Packet");
			delayPack(delay, clientPort, genSocket);
		}
		
		else if(mode==1)//duplicate
		{
			console.print("Duplicate Packet");
			duplicatePack(clientPort, genSocket);
		}
		
		else if (mode==2)//lose
		{
			console.print("lose try");
			losePack( clientPort, genSocket);
		}
		
		else
		{
			System.out.println("ERROR: INCORRECT MODE");
		}
	}
	
	public void delayPack(int delay, int clientPort,DatagramSocket  genSocket)
	{
		console.print("IN DELAY PACKET, SENDING REGULARLY");
		if(delay<50)//delay less then timeout of client/server
		{
			try
			{
				nextGram=tryReceive(genSocket, delay);//receieve something random
				
				//should never get here
				console.printError("Client Responds to Duplicate");
				return;
			}
			catch (IOException ioe)//got Data
			{
				console.print("Delay Reached, Data sent");
				sendDatagram(clientPort, genSocket);
			}
			
		}
		
		else//timeout expected geater than timeout of client/server
		{
			console.print("NOT IMPLEMENING, SENDING Now");
			sendDatagram(clientPort, genSocket);
		}
	}
	
	public void duplicatePack( int clientPort,DatagramSocket  genSocket)
	{
		console.print("IN Duplicate PACKET 1");
		sendDatagram(clientPort, genSocket);	
	}
	
	
	public void losePack( int clientPort,DatagramSocket  genSocket)
	{
		console.print("Data Lost");
	}
	
	public void maybeSend(int clientPort,DatagramSocket genSocket,DatagramPacket receivedPacket)
	{    
		if(inputStack.peek()!=null)	
		{
			byte byteBlockNum[]=new byte[2];
			int bNum=inputStack.peek().getBlockNum();
			int mode=inputStack.peek().getMode();
			int delay=inputStack.peek().getDelay();
			int packType=inputStack.peek().getPacketType();
			byte bytePackType[] = new byte[2];
			
			bytePackType[1] = (byte)(packType & 0xFF);
			bytePackType[0] = (byte)((packType >> 8)& 0xFF);
			
			byteBlockNum[1] = (byte)(bNum & 0xFF);
			byteBlockNum[0] = (byte)((bNum >> 8)& 0xFF);
			
			/*
			console.print("bytePackType[0]: "+ bytePackType[0]);
			console.print("bytePackType[1]: "+bytePackType[1]);
			
			console.print("receivedPacket.getData()[0]: "+ receivedPacket.getData()[0]);
			console.print("receivedPacket.getData()[1]: "+receivedPacket.getData()[1]);
			*/
			
			if(bytePackType[1]==receivedPacket.getData()[1] && bytePackType[0] == receivedPacket.getData()[0] && byteBlockNum[1]==receivedPacket.getData()[3] && byteBlockNum[0]==receivedPacket.getData()[2]){
				//proper packet type and block num, mess with this one right here
				console.print("Block Mess Match");
				passIt(mode, delay,clientPort, genSocket);
				//sendDatagram(clientPort, genSocket);
				inputStack.pop();
 
			}
			
			else
			{
				System.out.println("No part Match");
				sendDatagram(clientPort, genSocket);
				
			}
		}
	
		else
		{
			System.out.println("empty stack");
			sendDatagram(clientPort, genSocket);
		}
	}
	public void errorSimHandle()
	{
		console.print("inErrorSim");
		boolean needSend=true;
		int sendToPort=SERVER_RECEIVE_PORT;
		int serverPort=0;
		//wait for original RRQ/WRQ from client
		receiveDatagram(inSocket);
		console.print("First Packet Recieved");
		//save port 
		clientPort = receivedPacket.getPort();
	
		while (true)
		{
					
			if(!needSend)
			{
				receiveDatagram(genSocket);
				if(serverPort==0)
				{
					serverPort=receivedPacket.getPort();
				}
				
				if(receivedPacket.getPort()==clientPort)
				{
					sendToPort=serverPort;
					needSend=true;
				}
				
				else if(receivedPacket.getPort()==serverPort)
				{
					sendToPort=clientPort;
					needSend=true;
				}
				
				else
				{
					console.print("EPIC ERROR");
				}
			}
			
			
			else if(needSend)
			{
				maybeSend(sendToPort,genSocket, receivedPacket);
				needSend=false;
			}
		}
		
	}
	
	public void mainPassingLoop()
	{
		console.print("TFTPHost Operating...");
		
		//declaring local variables
		boolean runFlag = true;
		String input[] = null;
		int packetType, blockNum, delay;
		
		//print starting text
		console.print("type 'help' for command list");
		console.print("~~~~~~~~~~~ COMMAND LIST ~~~~~~~~~~~");
		console.print("'help'                                   - print all commands and how to use them");
		console.print("'clear'                                  - clear screen");
		console.print("'close'                                 - exit client, close ports, be graceful");
		console.print("'verbose BOOL'                - toggle verbose mode as true or false");
		console.print("'test'                                    - runs a test for the console");
		console.print("'errors'                               - display a summary of all errors to be simulated");
		console.println();
		console.print("'delay PT BN DL'              - set a delay for packet type PT, block number BN for DL blocks");
		console.print("'dup PT BN '                      - duplicate packety type PT, block number BN");
		console.print("'lose PT BN'                      - lose packet type PT, block number BN");
		console.println();
		console.print("'0 PT BN DL'                    - set a delay for packet type PT, block number BN for DL blocks");
		console.print("'1 PT BN '                         - duplicate packety type PT, block number BN");
		console.print("'2 PT BN'                          - lose packet type PT, block number BN");
		console.print("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		console.println();
		
		//main input loop
		while(runFlag && LIT)
		{
			//get PARSED user input
			input = console.getParsedInput(true);
			
			//process input based on param number
			switch(input.length)
			{
				case(1):
					//print commands
					if (input[0].equals("help"))
					{
						console.print("type 'help' for command list");
						console.print("~~~~~~~~~~~ COMMAND LIST ~~~~~~~~~~~");
						console.print("'help'                                   - print all commands and how to use them");
						console.print("'clear'                                  - clear screen");
						console.print("'close'                                 - exit client, close ports, be graceful");
						console.print("'verbose BOOL'                - toggle verbose mode as true or false");
						console.print("'test'                                    - runs a test for the console");
						console.print("'errors'                               - display a summary of all errors to be simulated");
						console.println();
						console.print("'delay PT BN DL'              - set a delay for packet type PT, block number BN for DL blocks");
						console.print("'dup PT BN '                      - duplicate packety type PT, block number BN");
						console.print("'lose PT BN'                      - lose packet type PT, block number BN");
						console.println();
						console.print("'0 PT BN DL'                    - set a delay for packet type PT, block number BN for DL blocks");
						console.print("'1 PT BN '                         - duplicate packety type PT, block number BN");
						console.print("'2 PT BN'                          - lose packet type PT, block number BN");
						console.print("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
						console.println();
					}
					//display inputs
					else if (input[0].equals("errors"))
					{
						console.print(inputStack.toFancyString());
					}
					//run the console
					else if (input[0].equals("run"))
					{
						errorSimHandle();
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
						//this.close();
						System.exit(0);
					}
					//run simple console test
					else if (input[0].equals("test"))
					{
						console.testAll();
					}
					//BAD INPUT
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
							verbose = true;
						}
						else if (input[1].equals("false"))
						{
							verbose = false;
						}
						else
						{
							console.print("! Unknown Input !");
						}
					}
					break;
				
				case(3):
					//duplicate packet
					if(input[0].equals("dup") || input[0].equals("1"))
					{
						//convert verbs from string to int
						try
						{
							if (input[1].equals("data"))
							{
								packetType = 3;
							}
							else if (input[1].equals("ack"))
							{
								packetType = 4;
							}
							else
							{
								packetType = Integer.parseInt(input[1]);
							}
							blockNum = Integer.parseInt(input[2]);
							
							//add to inputStack
							inputStack.push(1, packetType, blockNum, 0);
						}
						catch (NumberFormatException nfe)
						{
							console.printError("Error 2 - NAN");
						}
					}
					//lost packet
					else if (input[0].equals("lose") || input[0].equals("2"))
					{
						//convert verbs from string to int
						try
						{
							if (input[1].equals("data"))
							{
								packetType = 3;
							}
							else if (input[1].equals("ack"))
							{
								packetType = 4;
							}
							else
							{
								packetType = Integer.parseInt(input[1]);
							}
							blockNum = Integer.parseInt(input[2]);
							
							//add to inputStack
							inputStack.push(2, packetType, blockNum, 0);
						}
						catch (NumberFormatException nfe)
						{
							console.printError("Error 2 - NAN");
						}
					}
					else
					{
						console.print("! Unknown Input !");
					}
					break;
				
				case(4):
					//delay packet
					if(input[0].equals("delay") || input[0].equals("0"))
					{
						//convert verbs from string to int
						try
						{
							if (input[1].equals("data"))
							{
								packetType = 3;
							}
							else if (input[1].equals("ack"))
							{
								packetType = 4;
							}
							else
							{
								packetType = Integer.parseInt(input[1]);
							}
							blockNum = Integer.parseInt(input[2]);
							delay = Integer.parseInt(input[3]);
							
							//add to inputStack
							inputStack.push(0, packetType, blockNum, delay);
						}
						catch (NumberFormatException nfe)
						{
							console.printError("Error 2 - NAN");
						}
					}
					//bad input
					else
					{
						console.print("! Unknown Input !");
					}
					break;
				
				default:
					console.print("! Unknown Input !");
					break;
			}
		}
	}
	
	
	public static void main(String[] args) 
	{
		//declaring local variables
		TFTPHost host = new TFTPHost();
		//run
		host.mainPassingLoop();
	}

}
