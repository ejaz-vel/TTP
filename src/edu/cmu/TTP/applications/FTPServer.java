/*
 * A sample server that uses DatagramService
 */
package edu.cmu.TTP.applications;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import edu.cmu.TTP.helpers.FTPConnectionHandler;
import edu.cmu.TTP.models.ClientDataID;
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
		ConcurrentMap<ClientDataID, Datagram> map = new ConcurrentHashMap<>();

		while(true) {
			try {
				Datagram datagram = ttp.receiveDatagram();
				if(((TTPSegment)datagram.getData()).getType().equals(PacketType.SYN)) {
					System.out.println("Received SYN datagram from " + datagram);
					ttp.sendAck(datagram, null);
					
					// Spawn a new thread to transfer the file contents
					Thread t = new Thread(new FTPConnectionHandler(map, datagram, ttp));
					t.start();
				} else if (((TTPSegment)datagram.getData()).getType().equals(PacketType.DATA_REQ_SYN)) {
					System.out.println("Received File name at Server Side");
					ClientDataID clientID = new ClientDataID();
					clientID.setIPAddress(datagram.getSrcaddr());
					clientID.setPort(datagram.getSrcport());
					clientID.setSequenceNumber(null);
					clientID.setPacketType(PacketType.DATA_REQ_SYN);
					map.putIfAbsent(clientID, datagram);
				} else if(((TTPSegment)datagram.getData()).getType().equals(PacketType.ACK)) {
					System.out.println("Received ACK for data at Server Side");
					ClientDataID clientID = new ClientDataID();
					clientID.setIPAddress(datagram.getSrcaddr());
					clientID.setPort(datagram.getSrcport());
					clientID.setSequenceNumber(((TTPSegment)datagram.getData()).getSequenceNumber());
					clientID.setPacketType(PacketType.ACK);
					map.putIfAbsent(clientID, datagram);
				}
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
		} 
	}

	private static void printUsage() {
		System.out.println("Usage: server <port>");
		System.exit(-1);
	}
}
