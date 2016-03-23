/*
 * A sample client that uses DatagramService
 */

package edu.cmu.TTP.applications;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import edu.cmu.TTP.helpers.ClientHelper;
import edu.cmu.TTP.models.ConnectionEssentials;
import edu.cmu.TTP.models.TTPClientHelperModel;
import edu.cmu.TTP.services.TTPService;

public class FTPClient {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws InterruptedException 
	 * @throws NumberFormatException 
	 * @throws NoSuchAlgorithmException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException, NumberFormatException, InterruptedException, NoSuchAlgorithmException {
		if(args.length != 3) {
			printUsage();
		}
		System.out.println(Thread.activeCount());
		for(Thread thread : Thread.getAllStackTraces().keySet()) {
			System.out.println(thread.getId()+ " " +thread.getName());
		}

		int clientPort = Integer.parseInt(args[0]);
		int serverPort = Integer.parseInt(args[1]);
		String fileName = args[2];
		String clientAddress = "127.0.0.1";
		String serverAddress = "127.0.0.1";
		
		System.out.println("Starting client ...");
		TTPService ttp = new TTPService(clientPort);
		ConnectionEssentials connectionEssentials = 
					new ConnectionEssentials(clientAddress, serverAddress, clientPort, serverPort);
		ClientHelper helper = new ClientHelper(connectionEssentials, ttp);
		TTPClientHelperModel clientHelperModel = null;
		
		// Setup the connection.
		if (ttp.setupClientConnection(connectionEssentials)) {
			// SYN and ACK are done. Now request for the filename.
			while((clientHelperModel= helper.requestForFile(fileName))==null);
			// Start reading data.
			helper.receiveDataHelper(clientHelperModel,fileName);
			ttp.closeClientSideConnection(connectionEssentials);
		} else {
			System.out.println("Unable to setup connection with the server");
		}
		System.out.println(Thread.activeCount());
		for(Thread thread : Thread.getAllStackTraces().keySet()) {
			System.out.println(thread.getId()+ " " +thread.getName());
		}
	}
	
    private static void printUsage() {
		System.out.println("Need to pass 3 Command Line arguments: <localport> <serverport> <filename>\n");
		System.exit(-1);
	}
}
