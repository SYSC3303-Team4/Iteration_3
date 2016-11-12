/**
*Class:             InputStack.java
*Project:           TFTP Project - Group 4
*Author:            Jason Van Kerkhoven                                             
*Date of Update:    10/11/2016                                              
*Version:           1.0.0                                                      
*                                                                                   
*Purpose:           Sorted stack of input strings. Sorted in terms of block num
* 
* 
*Update Log:		v2.0.0
*						- sorting now occurs after entire stack is created
*						- sorting based on RRQ or WRQ
*						- clear method
*					v1.0.1
*						- added human readable thing
*					v1.0.0
*						- very basic implementation of framework v1.0.0
*/
package inputs;


import java.util.Collections;
//import stuff
import java.util.LinkedList;


public class InputStack 
{
	//Declaring local instance variables
	private LinkedList<Input> pseudoStack;
	private int length;
	
	
	//generic constructor
	public InputStack()
	{
		pseudoStack = new LinkedList<Input>();
		length = 0;
	}
	
	
	public int length()
	{
		return length;
	}
	
	
	//clear the InputStack
	public void clear()
	{
		pseudoStack.clear();
	}
	
	
	//sort with DATA > ACK in case of blockNum tie
	public void sortRRQ()
	{
		resolveTies(false);
	}
	
	
	//sort with ACK > DATA in case of blockNum tie
	public void sortWRQ()
	{
		resolveTies(true);
	}
	
	
	//general case for sort-with-tie
	private void resolveTies(boolean WRQ)
	{
		//declaring local variables
		Input inputInList = null;
		Input nextInput = null;
		
		//iterate through stack except for end
		for (int i=0; i<length-1; i++)
		{
			//get input at index i
			inputInList = pseudoStack.get(i);
			nextInput = pseudoStack.get(i+1);

			//check if there is a tie case between i and i+1
			if(inputInList.getBlockNum() == nextInput.getBlockNum())
			{
				if(WRQ)
				{
					/*          i              i+1
			              inputInList      nextInput
			   		 ... ---> DATA --------> ACK --> ...
					*/
					if (inputInList.getPacketType() == 3 && nextInput.getPacketType() == 4)
					{

					}
				}
				else
				{
					/*          i              i+1
			               inputInList      nextInput
			   		  ... ---> ACK ----------> DATA --> ...
					*/
					if (inputInList.getPacketType() == 4 && nextInput.getPacketType() == 3)
					{
						Collections.swap(pseudoStack, i, i+1);
					}
				}
			}
		}
	}
	
	
	//push to stack with sort (tie conditions not guaranteed)
	public void push(int mode, int packetType, int blockNum, int delay)
	{
		//create Input
		Input newInput = new Input(mode, packetType, blockNum, delay);
		
		//nothing in stack, add to front
		if (length == 0)
		{
			pseudoStack.addFirst(newInput);
			length++;
		}
		//something in stack, have to sort through it
		else
		{
			//declaring Input to represent current Input in list
			Input inputInList;
			
			//iterate through list to determine where to put it
			for(int i=0; i<length; i++)
			{
				//get current input at index i
				inputInList = pseudoStack.get(i);
				
				//new input should come before input in list
				if(newInput.getBlockNum() < inputInList.getBlockNum())
				{
					pseudoStack.add(i, newInput);
					length++;
					return;
				}
			}
			//add to end
			pseudoStack.addLast(newInput);
			length++;
		}
	}
	
	
	//peek top entry from stack
	public Input peek()
	{
		if (length != 0)
		{
			return pseudoStack.getFirst();
		}
		else
		{
			return null;
		}
	}
	
	
	//pop top entry from stack
	public Input pop()
	{
		if (length != 0)
		{
			length--;
			return pseudoStack.removeFirst();
		}
		else
		{
			return null;
		}
	}
	
	
	@Override
	//print as string
	public String toString()
	{
		//return string
		String printable = "";
		
		//make returnable object
		if(length == 0)
		{
			printable = "EMPTY";
		}
		else
		{
			for(int i=0; i<length; i++)
			{
				printable = printable + "ITEM: " + i + " --> " + pseudoStack.get(i).toString() + "\n";
			}
		}
		
		return printable;
	}
	
	
	//print as HUMAN READABLE string
	public String toFancyString()
	{
		//return string
		String printable = "";
		
		//make returnable object
		if(length == 0)
		{
			printable = "EMPTY";
		}
		else
		{
			for(int i=0; i<length; i++)
			{
				printable = printable + "ITEM: " + i + " --> " + pseudoStack.get(i).toFancyString() + "\n" + "    ";
			}
		}
		
		return printable;
	}
	
	
	/*
	//ONLY USED FOR TESTING
	public static void main(String[] args)
	{
		//(int mode, int packetType, int blockNum, int delay)
		
		InputStack inputStack = new InputStack();
		InputStack stack2 = new InputStack();
		
		System.out.println("Testing RRQ priority...");
		inputStack.push(2, 3, 1, 0);
		inputStack.push(2, 4, 1, 0);
		inputStack.push(0, 3, 3, 0);
		inputStack.push(1, 4, 2, 0);
		inputStack.push(1, 4, 3, 0);
		inputStack.push(0, 4, 5, 0);
		inputStack.push(0, 3, 5, 0);
		System.out.println(inputStack.toString());
		System.out.println("Sorting...");
		inputStack.sortRRQ();
		System.out.println(inputStack.toString());
		
		System.out.println("Testing WRQ priority...");
		stack2.push(2, 3, 1, 0);
		stack2.push(2, 4, 1, 0);
		stack2.push(0, 3, 3, 0);
		stack2.push(1, 4, 2, 0);
		stack2.push(1, 4, 3, 0);
		stack2.push(0, 3, 5, 0);
		stack2.push(0, 4, 5, 0);
		System.out.println(stack2.toString());
		System.out.println("Sorting...");
		stack2.sortWRQ();
		System.out.println(stack2.toString());
		System.out.println("\nTest Complete...");
	}
	*/
}
