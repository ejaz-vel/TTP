/**
 * 
 */
package edu.cmu.helpers;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;

import edu.cmu.models.Datagram;
import edu.cmu.models.TTPSegment;
import edu.cmu.services.DatagramService;

/**
 * @author apurv
 *
 */
public class ClientHelper {
	private static int maxReceivedSequenceNumber;
	
	private static DatagramService datagramService;
	
	public ClientHelper(int port) throws SocketException {
		datagramService = new DatagramService(port, 10);
	}
	
	public static void receiveDataHelper(String filename) throws ClassNotFoundException, IOException {
		Datagram datagram = datagramService.receiveDatagram();
		if (datagram.getData() != null) {
			TTPSegment segment = (TTPSegment) datagram.getData();
			System.out.println("Received File");
	
			//Store the data received in the localDisk
			PrintWriter out = new PrintWriter("clientFiles/" + filename);
			out.print(datagram.getData());
			out.close();
			System.out.println("Done saving the file");
		} else {
			System.out.println("No such file found on the server");
		}
	}
}
