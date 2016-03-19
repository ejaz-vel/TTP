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
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		if(args.length != 3) {
			printUsage();
		}

		System.out.println("Starting client ...");
		int port = Integer.parseInt(args[0]);
		ttp = new TTPService(port);

		if (ttp.setupConnection()) {
			String fileName = args[2];
			Datagram datagram = new Datagram();
			datagram.setData(fileName);
			datagram.setSrcaddr("127.0.0.1");
			datagram.setDstaddr("127.0.0.1");
			datagram.setDstport((short)Integer.parseInt(args[1]));
			datagram.setSrcport((short)port);
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
