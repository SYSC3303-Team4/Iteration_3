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
*Update Log:		v1.0.1
*						- added human readable thing
*					v1.0.0
*						- very basic implementation of framework v1.0.0
*/
package inputs;


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
	
	
	//push to stack with sort
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
	
	
	//test method for the stack
	private void test()
	{
		/*
		SARAH PLEASE ADD TEST CODE HERE
		PLEASE TEST:
					POP
					PEEK
					METRIC SHITLOAD OF TESTS FOR PUSH
		ALSO PLEASE PRINT THE STATE OF INPUTSTACK EACH TIME YOU DO SOMETHING
		
		THANKS U THE REAL MVP
		*/
	}
	
	
	//ONLY USED FOR TESTING
	public void main(String[] args)
	{
		InputStack inputStack = new InputStack(); 
		inputStack.test();
	}
}
