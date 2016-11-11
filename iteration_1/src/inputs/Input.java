/**
*Class:             Input.java
*Project:           TFTP Project - Group 4
*Author:            Jason Van Kerkhoven                                             
*Date of Update:    10/11/2016                                              
*Version:           1.0.0                                                      
*                                                                                   
*Purpose:           Basically an Enum
* 
* 
*Update Log:		v1.0.1
*						- added packetType
*						- added human readability to toStringFancy()
*					v1.0.0
*						- null
*/
package inputs;


public class Input 
{
	//declaring local instance variables
	private int blockNum;
	private int mode;
	private int delay;
	private int packetType;
	
	
	//generic constructor
	Input(int mode, int packetType, int blockNum, int delay)
	{
		this.mode = mode;
		this.blockNum = blockNum;
		this.delay = delay;
		this.packetType = packetType;
	}
	
	
	//generic accessors
	public int getBlockNum()
	{
		return blockNum;
	}
	public int getMode()
	{
		return mode;
	}
	public int getDelay()
	{
		return delay;
	}
	public int getPacketType()
	{
		return packetType;
	}
	
	
	@Override
	//print as string
	public String toString()
	{
		return ("BlockNum: " + blockNum + "PacketType: " + packetType + " || Mode: " + mode + " || Delay: " + delay);
	}
	
	
	//return a fancy string
	public String toFancyString()
	{
		String printable = "";
		
		switch(mode)
		{
			case(0):
				printable = printable + "DELAY ";
				break;
			case(1):
				printable = printable + "DUPLICATE ";
				break;
			case(2):
				printable = printable + "LOSE ";
				break;
			default:
				printable = printable + "!BAD MODE!";
				break;
		}
		switch(packetType)
		{
			case(1):
				printable = printable + "RRQ packet";
				break;
			case(2):
				printable = printable + "WRQ packet ";
				break;
			case(3):
				printable = printable + "ACK packet ";
				break;
			case(4):
				printable = printable + "DATA packet ";
				break;
			default:
				printable = printable + "!BAD PT!";
				break;
		}
		printable = printable + blockNum;
		if(mode == 0)
		{
			printable = printable + " for " + delay + "block nums";
		}
		
		return printable;
	}
}
