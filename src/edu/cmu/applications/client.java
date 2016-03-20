/*
 * A sample client that uses DatagramService
 */

package edu.cmu.applications;

import java.io.IOException;
import java.io.PrintWriter;

import edu.cmu.models.Datagram;
import edu.cmu.services.TTPService;

public class client {

	private static TTPService ttp;

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws InterruptedException 
	 * @throws NumberFormatException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException, NumberFormatException, InterruptedException {
		if(args.length != 3) {
			printUsage();
		}

		int clientPort = Integer.parseInt(args[0]);
		int serverPort = Integer.parseInt(args[1]);
		String clientAddress = "127.0.0.1";
		String serverAddress = "127.0.0.1";
		
		System.out.println("Starting client ...");
		ttp = new TTPService(clientPort);

		if (ttp.setupConnection(clientAddress, clientPort, serverAddress, serverPort)) {
			String fileName = args[2];
			Datagram datagram = new Datagram();
			datagram.setData(fileName);
			datagram.setSrcaddr(clientAddress);
			datagram.setDstaddr(serverAddress);
			datagram.setDstport((short)serverPort);
			datagram.setSrcport((short)clientPort);
			ttp.sendData(datagram);
			System.out.println("Sent Request for File");
			datagram = ttp.receiveData(fileName);
			ttp.closeConnection();
		} else {
			System.out.println("Unable to setup connection with the server");
		}
	}

	private static void printUsage() {
		System.out.println("Need to pass 3 Command Line arguments: <localport> <serverport> <filename>\n");
		System.exit(-1);
	}
}
