package project;

import java.io.IOException;
import java.io.BufferedReader; // to read and open data from file's
import java.io.FileReader;  // to read and open data from file's
import java.net.*;   // for network operations
import java.util.Arrays;  // for array manipulation

public class Server {

	public static void main(String[] args)  throws Exception
	{
		final int SERVER_PORT_NUM=9999; // should be the same port(randomly fixed)
										
		final int MAX_MSG_SIZE=8192;   // UDP segments larger than this may not be
										//supported by some OSs
		//to store data bytes from client
		byte[] receivedMessage = new byte[MAX_MSG_SIZE];
		// receive UDP packets
		DatagramPacket receivePacket = new DatagramPacket(receivedMessage,receivedMessage.length);
		
		
		// do this forever
		while(true)
		{
			//server socket, listening on port SERVER_PORT_NUM
			DatagramSocket serverSocket = new DatagramSocket(SERVER_PORT_NUM);
			
			System.out.println("Waiting for a message from the client");
			
			// receiving client's request
			serverSocket.receive(receivePacket);
			// the receive() method blocks here (program execution stops)
			// only one way to continue: packet is received
			System.out.println("Message received from the client");
			
			// getting the client info out of the received UDP packet object
			InetAddress clientAddress=receivePacket.getAddress(); // client's address
			int clientPort = receivePacket.getPort();  // client's port number
			int dataLength = receivePacket.getLength(); // the received packet length
			
			String requestMessage = new String(receivedMessage,0,dataLength); // getting the request message from the received packet
			//System.out.println("Message from the client: "+requestMessage+"\n");
			
			String responseMessage=processRequest(requestMessage);
			//processRequest function takes the request Message from client as parameter and 
			//returns the newly composed response message as per the format specified
			//it checks for integrity field errors, syntax, version no. errors as well as file existence
			
			
			System.out.println("Sending a response message to the client.\n");
			byte[] sentMessage = responseMessage.getBytes(); // converting the message to byte array
			
			// setting up the response UDP packet object
			DatagramPacket sentPacket = new DatagramPacket(sentMessage,responseMessage.length());
			sentPacket.setAddress(clientAddress); // destination ip address
			sentPacket.setPort(clientPort); //destination port number 
			
			serverSocket.send(sentPacket); // sending the packet 
			
			receivePacket.setLength(MAX_MSG_SIZE); // resetting the length for the next UDP packet
			serverSocket.close();
			
		}

	}
	
	/*processRequest function takes the request Message from client as parameter and 
	returns the newly composed response message as per the format specified
	it checks for integrity field errors, syntax, version no. errors as well as file existence*/
	public static String processRequest(String requestMessage){
		
		int firstOccCRLF = requestMessage.indexOf("\n"); // to find the index of first LF in "ENTS/1.0 Request[CR+LF]"
		int secondOccCRLF = requestMessage.indexOf("\n",firstOccCRLF+1); //index of second LF in "[filename].[extension][CR+LF]"
		int thirdOccCRLF = requestMessage.indexOf("\n",secondOccCRLF+1);  //last LF index in "[integrity check][CR+LF]"
		
		
		String calcMsgChkSum = integrityCheck(requestMessage.substring(0, secondOccCRLF+1)); // to check for the integrity 
		//System.out.println("calc integrity: "+calcMsgChkSum);
		
		String rxChkSum = requestMessage.substring(secondOccCRLF+1, thirdOccCRLF-1); // the received check sum in the request message
		//System.out.println("received integrity: "+originalChkSum);
		
		String syntaxChk = requestMessage.substring(firstOccCRLF+1, secondOccCRLF-1); // filename + extension
		String fileName = syntaxChk.split("\\.")[0]; // file name 
		String extension = syntaxChk.split("\\.")[1];  // extension
		
		String versionNum = requestMessage.substring(5,8); // getting the version number since we know it's a fixed length
		String content = "";
		String line = "";
		
		char responseCode='0'; // initializing the response code with no error.
		if(rxChkSum.equals(calcMsgChkSum)==false) // if integrity check fails
		{
			responseCode='1'; // send response code 1
			System.out.println("code: "+responseCode);
			
		}
		// checking for syntax  of filename ->if first char is a letter(alpha numeric) & all other characters are alphabet(a-zA-Z), numerical(0-9), underscore(_)
		//extension -> alphabet and numerical
		else if(Character.isLetter(fileName.charAt(0))==false|| fileName.matches("^[a-zA-Z_0-9]+$")==false || extension.matches("^[a-zA-Z0-9]+$")==false)
		{
			responseCode='2'; // send response 2 if the syntax is incorrect
			System.out.println("code: "+responseCode);
			
		}			
		// check if the version number is correct
		else if(!versionNum.equals("1.0"))
		{
			responseCode='4'; // send response code if it's false
			System.out.println("code: "+responseCode);
		}	
		else
		{
			try
			{
				String path = "C:/Users/Sneha/Downloads/"+syntaxChk; // check if the file exists 
				FileReader file = new FileReader(path); // read the file 
				BufferedReader inFromFile = new BufferedReader(file);
				
				while((line=inFromFile.readLine())!=null) // read the data from the file line by line till it's null
				{
					content=content+line;
				}
				inFromFile.close(); // close the file
				file.close();
			}
			catch(IOException e) // catch the exception if no file exists
			{
				responseCode='3'; // send response code 3 if no file exists
				e.getMessage();
			}
		}
		
	
		String responseMessage;
		// if response code is 0 means all the conditions are satisfied 
		if(responseCode=='0')
		{
			responseMessage="ENTS"+"/"+"1.0"+"Response"+"\r\n"+responseCode+"\r\n"+content.length()+"\r\n"+content; // send the response message with the content 
		}
		else
			responseMessage="ENTS"+"/"+"1.0"+"Response"+"\r\n"+responseCode+"\r\n"+content.length()+"\r\n"; // send the response message without content
																							// also send with appropriate response code
		
		String newChkSum = integrityCheck(responseMessage); // calculating the new checksum 
		responseMessage = responseMessage+newChkSum+"\r\n"; // adding the check sum to the response message
		
		return responseMessage;
	}
		
	
	// Integrity check function
	public static String integrityCheck(String message)
	{
		char[] c = message.toCharArray(); // changing the message to character array
		int[] x = new int[c.length/2];
		for(int i=0;i<x.length;i++)
		{
			x[i]=(c[2*i]<<8)+(c[2*i+1]); // ?????creating 
			if(c.length%2!=0 & i==x.length-1)
			{
				i++;
				x=Arrays.copyOf(x,x.length+1); //?????
				x[i]=c[2*i]<<8;
			}
		}
		
		int s=0,C=7919,D=65536,index;
		for(int j=0;j<x.length;j++)
		{
			index=s^x[j]; //XOR operation
			s=(C*index)%D; // modulo operations
		}
		String chkSum = Integer.toString(s); // converting the check sum into string
		return chkSum; // returning the check sum 
	}
	
	

}
