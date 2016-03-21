/*
 * A sample server that uses DatagramService
 */
package edu.cmu.TTP.applications;


import java.io.IOException;

import edu.cmu.TTP.models.Datagram;
import edu.cmu.TTP.models.PacketType;
import edu.cmu.TTP.models.TTPSegment;
import edu.cmu.TTP.services.TTPService;

public class FTPServer {

	private static TTPService ttp;

	public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {

		if(args.length != 1) {
			printUsage();
		}

		System.out.println("Starting Server ...");
		int port = Integer.parseInt(args[0]);
		ttp = new TTPService(port);
		run();
	}

	private static void run() throws IOException, ClassNotFoundException, InterruptedException {
		Datagram datagram;
		while(true) {
			// Wait for a Syn.
			datagram = ttp.receiveDatagram();
			if(((TTPSegment)datagram.getData()).getType().equals(PacketType.SYN)) {
				System.out.println("Received datagram from " + datagram);
				ttp.sendAck(datagram, null);
				/* Wait for DATA_REQ_SYN packet to get the filename that client is requesting */
				datagram = ttp.receiveDatagram();
				if(((TTPSegment)datagram.getData()).getType().equals(PacketType.DATA_REQ_SYN)) {
					ttp.sendData(datagram);
				}				
//				fileData.setData(getFileContents(((TTPSegment)datagram.getData()).getData().toString()));
			}
		}
	}

	private static void printUsage() {
		System.out.println("Usage: server <port>");
		System.exit(-1);
	}
}
