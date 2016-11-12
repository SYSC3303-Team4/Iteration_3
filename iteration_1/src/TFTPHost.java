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
*Update Log:		 v2.0.0
*						- input methods added (non-isr)
*						- input now saves to InputStack
*						- help menu added
*					 v1.0.0
*                       - null
*/


//imports
import java.io.*;
import java.net.*;

import ui.ConsoleUI;
import inputs.*;


public class TFTPHost 
{
	
	//declaring local instance variables
	private DatagramPacket sentPacket;				//slowly phase out
	private DatagramPacket receivedPacket;			//slowly phase out
	private DatagramSocket inSocket;
	private DatagramSocket generalClientSocket;
	private DatagramSocket generalServerSocket;
	private int clientPort;
	private int serverThreadPort;
	private boolean verbose;
	private ConsoleUI console;
	private InputStack inputStack = new InputStack();
		
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
			generalServerSocket = new DatagramSocket();
			generalClientSocket = new DatagramSocket();
		}
		//enter if socket creation results in failure
		catch (SocketException se)
		{
			se.printStackTrace();
			System.exit(1);
		}
		
		//initialize echo --> off
		verbose = true;
		
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
	
	
	//send datagram
	private void send(int outPort, DatagramSocket socket, DatagramPacket toSend)
	{
		//prep packet to send
		console.print("Sending packet...");
		toSend.setPort(outPort);
				
		//print contents
		if(verbose)
		{
			printDatagram(toSend);
		}
		
		//send packet
		try
		{
			socket.send(toSend);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			//System.exit(1);
		}
		if(verbose)
		{
			console.print("Packet successfully sent");
		}
	}
	
	//receive datagram
	public DatagramPacket receive(DatagramSocket inputSocket, int timeOut) throws IOException
	{
		//construct an empty datagram packet for receiving purposes
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
			//System.exit(1);
		}
		
		//deconstruct packet and print contents
		console.print("Packet successfully received");
		printDatagram(receivedPacket);
	}
	
	
	//receive packet on inPort
	public void receiveDatagramTimeout(DatagramSocket inputSocket, int timeOut) throws IOException
	{
		//construct an empty datagram packet for receiving purposes
		byte[] arrayholder = new byte[MAX_SIZE];
		receivedPacket = new DatagramPacket(arrayholder, arrayholder.length);
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
		inputSocket.receive(receivedPacket);

		
		//deconstruct packet and print contents
		console.print("Packet successfully received");
		if (verbose)
		{
			printDatagram(receivedPacket);
		}
		//reset delay
		try
		{
			inputSocket.setSoTimeout(0);
		}
		catch (SocketException ioe)
		{
			console.printError("Cannot set socket timeout");
		}
	}
	
	
	//send packet
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
	
	
	
	
	
	public void performOneTransfer()
	{
		/*
		 * ############## THIS IS OLD CODE WHICH ALLOWS HOST
		 * ############## TO TRANSPARENTLY PASS PACKETS
		 * ##############		+5 SCRAP VALUE
		 */
		
		//declaring local variables
		byte RWReq=0;
		boolean loop=true;
		int lastDataPacketLength=0;
		Input stackTop = null;
		int blockNum = 0;
		byte packetType = 0;
		byte[] data;
		DatagramPacket lastReceivedPacket = null;
		
		//wait for original RRQ/WRQ from client
		try
		{
			lastReceivedPacket = receive(inSocket, 0);
		}
		catch (IOException timeout)
		{
			console.printError("HOST TIMEOUT WAITING FOR CLIENT RRQ/WRQ");
		}
		
		//save port 
		clientPort = lastReceivedPacket.getPort();
	
		//determine if this is a valid RRQ/WRQ
		data = new byte[2];
		data = lastReceivedPacket.getData();
		//valid
		if (data[0] == 0 && (data[1] == 1 || data[1] == 2) )
		{
			RWReq = data[1];
		}
		//something awful has happened
		else
		{
			console.print("Something awful happend");
			System.exit(0);
		}
		
		//send RRQ/WRQ to server
		send(SERVER_RECEIVE_PORT, generalServerSocket, lastReceivedPacket);
		
		//receive 1st packet
		try
		{
			lastReceivedPacket = receive(generalServerSocket, 0);
		}
		catch(IOException timeout)
		{
			console.printError("HOST TIMEOUT WAITING FOR SERVER TO TRANSMIT");
		}
		//save port for server thread (use from now on instead of SERVER_RECEIVE_PORT
		serverThreadPort = lastReceivedPacket.getPort();
		
		
		//do the rest if RRQ
		if(RWReq == 1)
		{
			while(loop)
			{
				//check if any errors exist [SERVER DATA PACKET]
				if(inputStack.peek() != null)
				{
					//get received packet blockNum and packetType
					data = lastReceivedPacket.getData();
					packetType = (lastReceivedPacket.getData())[1];
					blockNum = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
					//System.out.println("blockNum decoded as: " + blockNum);		//DEBUG
					
					//check if error needs to be simulated for this packet
					stackTop = inputStack.peek();
					if(stackTop.getBlockNum() == blockNum && stackTop.getPacketType() == packetType)
					{
						stackTop = inputStack.pop();
						
						//determine what error needs to be simulated
						switch(stackTop.getMode())
						{
							//delay packet
							case(0):
									/*
									try
									{
										Thread.sleep(stackTop.getDelay());
									}
									catch(InterruptedException e)
									{
										//error
										System.out.println("ERROR");
									}
									*/
									break;
							
							//duplicate SERVER DATA PACKET
							case(1):
									
									//set the packet to be duplicated and save the ack
									DatagramPacket storedAck = null;
							
									//send datagram to clientPort
									send(clientPort, generalClientSocket, lastReceivedPacket);
									//wait for ACK
									try
									{
										storedAck = receive(generalClientSocket, 0);
									}
									catch(IOException timeout)
									{
										console.printError("HOST TIMEOUT WAITING FOR CLIENT TO TRANSMIT");
										return;
									}
									
									//send duplicate
									if (verbose)
									{
										console.print("Sending duplicate to client...");
									}
									send(clientPort, generalClientSocket, lastReceivedPacket);
									
									//wait for (lack of) responce
									try
									{
										receiveDatagramTimeout(generalClientSocket, 50);
										
										//should never get here
										console.printError("Client Responds to Duplicate");
										return;
									}
									catch (IOException ioe)
									{
										if (verbose)
										{
											console.print("No response from client with duplicate!");
										}
										lastReceivedPacket = storedAck; 
									}
									break;
							
							//loose
							case(2):
									
									break;
						}
					}
				}
				
				//save packet size if of type DATA
				if ( (lastReceivedPacket.getData())[1] == 3)
				{
					lastDataPacketLength = lastReceivedPacket.getLength();
				}
				
				//send DATA to client
				send(clientPort, generalClientSocket, lastReceivedPacket);
				
				//receive client ACK
				try
				{
					lastReceivedPacket = receive(generalClientSocket, 0);
				}
				catch(IOException timeout)
				{
					console.printError("HOST TIMEOUT WAITING FOR CLIENT TO TRANSMIT");
				}
				
				//check if any errors exist [CLIENT ACK PACKET]
				if(inputStack.peek() != null)
				{
					//TODO RRQ CLIENT ACK ERRORS
				}
				
				//send ACK to server
				send(serverThreadPort, generalServerSocket, lastReceivedPacket);
				
				//receive more data and loop if datagram.size==516
				//final ack sent to server, data transfer complete
				if (lastDataPacketLength < MAX_SIZE)
				{
					console.print("--------------------------RRQ (pull from server) Complete--------------------------");
					console.println();
					loop = false;
				}
				//more data left, receive and loop back
				else
				{
					try
					{
						lastReceivedPacket = receive(generalServerSocket, 0);
					}
					catch (IOException timeout)
					{
						console.printError("HOST TIMEOUT WAITING FOR SERVER TO TRANSMIT");
					}
				}
			}
		}
		//do the rest if WRQ
		else
		{
			while(loop)
			{
				//send ACK to client
				sendDatagram(clientPort, generalClientSocket);
				//receive client DATA, save size
				receiveDatagram(generalClientSocket);
				if ( (receivedPacket.getData())[1] == 3)
				{
					lastDataPacketLength = receivedPacket.getLength();
				}
				//send DATA to server
				sendDatagram(serverThreadPort, generalServerSocket);
				
				//final DATA sent, receive and fwd final ACK
				if (lastDataPacketLength < MAX_SIZE)
				{
					//receive final server ACK
					receiveDatagram(generalServerSocket);
					//fwd ACK to clien
					sendDatagram(clientPort, generalClientSocket);
				
					//terminate transfer
					console.print("Data Transfer Complete");
					console.println();
					loop = false;
				}
				//there are still DATA packets to pass, transfer not compelte 
				else
				{
					//receive server ACK
					receiveDatagram(generalServerSocket);
				}
			}
		}
	}
	
	
	
	
	public void mainInputLoop()
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
						if(verbose)
						{
							console.print("Running host, simulating error list:");
							console.print(inputStack.toFancyString());
							console.println();
						}
						
						//run for 1 transfer
						performOneTransfer();
					}
					//clear console
					else if (input[0].equals("clear"))
					{
						console.clear();
					}
					//close console with grace
					else if (input[0].equals("close"))
					{
						console.print("Closing with grace.... [NEEDS TO BE FIXED]");
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
		host.mainInputLoop();
	}
}
