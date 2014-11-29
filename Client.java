package project;


import java.io.InterruptedIOException; //timeout exception type
import java.net.*; // networking classes
import java.util.*; // for console input

public class Client {
	
	public static void main(String[] args) throws Exception
	{
		final int SERVER_PORT_NUM=9999; // should be the same one as the server is listening on 
		int timeoutValue=1000;  // time out in milliseconds
		int timeoutCounter=0;  // timeout counter
		final int RX_MESSAGE_SIZE=8192;  //received message size
		Scanner input = new Scanner(System.in);  // for console input
		while(true)   // to ensure the application repeats until it's terminated
		{
			String[] name=fileSelect(input);   
			//fileSelect function(user defined) asks user to select one of the available files and returns name of that file
			
			
			/*initialization of variable with "y"(yes).when "n"(no) message isn't re-assembled.
			this is used to assemble & re-send the request in-case of nonzero response code from server*/
			String userInput = "y";  
			
			DatagramSocket clientSocket = new DatagramSocket();  // creating the UDP Client socket
			
			while(userInput.equals("y"))
			{
				String requestMessage=assembleMsg(name);   
				//assembleMsg function(user defined) assembles the request message as per the format specified
				
				byte[] messageBytes = requestMessage.getBytes();  // converting the message into byte array
				
				// creating the IP address object for the server machine
				
				// method 1: loop back the request to the same machine
				InetAddress serverIp = InetAddress.getLocalHost();
				
				// method 2: providing the server's IP address
				//byte[] serverIpAddress = { 129-256, 2, 90, 72 }; // corresponds to 129.2.90.72 (byte is signed type!)
				//InetAddress serverIp = InetAddress.getByAddress(serverIpAddress);
				
				//creating the UDP packet to be sent 
				DatagramPacket sentPacket = new DatagramPacket(messageBytes,messageBytes.length,serverIp,SERVER_PORT_NUM);
				//creating the receive UDP Packet
				byte[] receivedBytes = new byte[RX_MESSAGE_SIZE];
				DatagramPacket receivedPacket = new DatagramPacket(receivedBytes, RX_MESSAGE_SIZE);
				
				int flag=0;
				while(flag==0)
				{
					try
					{
						clientSocket.send(sentPacket); // sending the packet
						System.out.println("Message sent to the server");
						clientSocket.setSoTimeout(timeoutValue); // setting the timeout value
						System.out.println("Waiting for the server's response...");
						clientSocket.receive(receivedPacket); // receiving the packet
						// the timeout timer starts ticking here
						// the receive() method blocks here (program execution stops)
						// only two ways to continue:
						// a) packet is received (normal execution after the catch block)
						// b) timeout (exception is thrown)
						flag=1; // will exit the loop as the packet is received i.e., no timeout
					}
					
					catch(InterruptedIOException e)
					{
						timeoutCounter++; // timeout occurred so enters into exception and increases the timeout counter
						System.out.println("timeout "+timeoutCounter);
						timeoutValue*=2;  // doubling the timeout value
						if(timeoutCounter==4) // if timeout counter equals 4 then exit the communication
						{
							System.out.println("Communication failure!!! \nClient socket timeout! Exception object : "+e);
							// will exit the loop as communication error occurred
							System.exit(0);
						}
					}
					
				}
				System.out.println("\nMessage received from the server");
				timeoutCounter=0; // reseting the timeout value and counter
				timeoutValue=1000;
				
				
				int dataLength = receivedPacket.getLength(); // finding the length of the received packet
				String responseMessage = new String(receivedBytes,0,dataLength);  // storing the response message which has the received packet
				
				userInput=processResponse(responseMessage,input);
				//processResponsefunction(user defined) takes the response message and disassembles it to retrieve the content or print error messages 
				//and also asks the user to re-send or not
			}
			
			clientSocket.close(); // closing the socket
			//input.close();
		}
		
	}
	 
	//function to select one of the available files and returns selected filename and extension to the main function
	public static String[] fileSelect(Scanner input){
	
		System.out.println("Available files \n1. directors_message.txt \n2. program_overview.txt \n3. scholarly_paper.txt"); // available files
		System.out.print("Please enter your choice: ");
		int choice = input.nextInt(); //getting the input from the user 
		String name = new String();
			
		switch(choice)
		{
			case 1:
				name = "directors_message.txt";
				break;
			case 2:
				name = "program_overview.txt";
				break;
			case 3:
				name = "scholarly_paper.txt";
				break;
		}
	
		int index = name.indexOf(".");
		String fileName = name.substring(0, index); // file name 
		String extension = name.substring(index+1); // extension of the file
		String[] tempName={fileName,extension};
	
		//returns user selected filename and extension
		return tempName;
			
	}
	
	//function to assemble the request message according to the specified format
	public static String assembleMsg(String[] name){
		
	String requestMessage = "ENTS"+"/"+"1.0"+"Request"+"\r\n"+name[0]+"."+name[1]+"\r\n";  // message structure to be sent 
	String checkSum = integrityCheck(requestMessage);  // calling integrity check function 
	requestMessage = requestMessage+checkSum+"\r\n";  // adding the checksum to message structure
	System.out.println("requestMessage format: "+"\n"+requestMessage);
	return requestMessage;
	}
	
	// integrity check calculations
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
			s=(C*index)%D;  // modulo operations
		}
		String chkSum = Integer.toString(s); // converting the check sum to string
		return chkSum; // return the value
	}


	//processResponsefunction takes the response message from main function and disassembles it to retrieve the content 
	//or print error messages by checking integrity fields and response codes
	//and also asks the user to re-send or not and returns the user choice back 
	public static String processResponse(String responseMessage,Scanner input){
		String userInput;    //to store input from user for re-sending message or not
		
		int firstOccCRLF = responseMessage.indexOf("\n"); // to find the index of first LF in "ENTS/1.0 Response[CR+LF]"
		int secondOccCRLF = responseMessage.indexOf("\n",firstOccCRLF+1); //index of second LF in "[response code][CR+LF]"
		int thirdOccCRLF = responseMessage.indexOf("\n",secondOccCRLF+1); //index of third LF in "[content length][CR+LF]"
		int lastOccCRLF = responseMessage.indexOf("\n",thirdOccCRLF+1); //last LF index
		
		char responseCode = responseMessage.charAt(firstOccCRLF+1); // contains the response code
		int contentLength = Integer.valueOf(responseMessage.substring(secondOccCRLF+1,thirdOccCRLF-1)); // contains the content length 
		
		String originalChkSum = responseMessage.substring(thirdOccCRLF+contentLength+1, lastOccCRLF-1);  // contains the check sum from response message
		String calcChkSum = integrityCheck(responseMessage.substring(0, thirdOccCRLF+contentLength+1)); // calculated integrity value of the response message 

		//checks if the calculated integrity check and original check sum are same if not will send the request again
		if(originalChkSum.equals(calcChkSum)==false)
		{
			userInput="y";
		}

		else
		{    
			// checks if the response code is zero and if so it displays the content 
			if(responseCode=='0')
			{
				System.out.println("OK. The response has been created according to the request.");
				String content = responseMessage.substring(thirdOccCRLF+1,thirdOccCRLF+contentLength+1);
				System.out.println("content received is: "+content+"\n\n");
				userInput="n"; // to ensure the request is not sent again
			}
			else
			{
				if(responseCode=='1') // Integrity check failure
				{
					System.out.println("Error: integrity check failure. The request has one or more bit errors.");
				}
				else if(responseCode=='2')  // Syntax of the message is not correct
				{
					System.out.println("Error: malformed request. The syntax of the request message is not correct.");
				}
				else if(responseCode=='3')  // file not existing
				{
					System.out.println("Error: non-existent file. The file with the requested name does not exist.");
				}
				else if(responseCode=='4')  // wrong protocol version
				{
					System.out.println("Error: wrong protocol version. The version number in the request is different from 1.0.");
				}
				
				System.out.println("Do you want to resend the request? y/n:");
				userInput = input.next(); // takes the input of the user to re-send the request again when nonzero response code
			}
		}
		return userInput;

	}
}