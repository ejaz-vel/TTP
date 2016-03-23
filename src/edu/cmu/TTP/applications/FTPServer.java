/*
 * A sample server that uses DatagramService
 */
package edu.cmu.TTP.applications;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import edu.cmu.TTP.helpers.FTPConnectionHandler;
import edu.cmu.TTP.models.ClientPacketID;
import edu.cmu.TTP.models.Datagram;
import edu.cmu.TTP.models.PacketType;
import edu.cmu.TTP.models.TTPSegment;
import edu.cmu.TTP.services.TTPService;

public class FTPServer {

	private static TTPService ttp;

	public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException, NoSuchAlgorithmException {

		if(args.length != 1) {
			printUsage();
		}

		System.out.println("Starting Server ...");
		int port = Integer.parseInt(args[0]);
		ttp = new TTPService(port);
		run();
	}

	private static void run() throws IOException, ClassNotFoundException, InterruptedException, NoSuchAlgorithmException {
		ConcurrentMap<ClientPacketID, Datagram> map = new ConcurrentHashMap<>();

		while(true) {
			try {
				Datagram datagram = ttp.receiveDatagram();
				if(((TTPSegment)datagram.getData()).getType().equals(PacketType.SYN)) {
					System.out.println("Received SYN datagram from " + datagram);
					ttp.sendAck(datagram, null, PacketType.ACK);

					ClientPacketID clientID = new ClientPacketID();
					clientID.setIPAddress(datagram.getSrcaddr());
					clientID.setPort(datagram.getSrcport());
					clientID.setPacketType(PacketType.SYN);
					
					if (!map.containsKey(clientID)) {
						// Spawn a new thread only for the first SYN packet to transfer the file contents
						Thread t = new Thread(new FTPConnectionHandler(map, datagram, ttp));
						t.start();
					}
					map.put(clientID, datagram);
				} else if (((TTPSegment)datagram.getData()).getType().equals(PacketType.DATA_REQ_SYN)) {
					System.out.println("Received File name at Server Side");
					ClientPacketID clientID = new ClientPacketID();
					clientID.setIPAddress(datagram.getSrcaddr());
					clientID.setPort(datagram.getSrcport());
					clientID.setPacketType(PacketType.DATA_REQ_SYN);
					map.putIfAbsent(clientID, datagram);

					ClientPacketID clientID2 = new ClientPacketID();
					clientID2.setIPAddress(datagram.getSrcaddr());
					clientID2.setPort(datagram.getSrcport());
					clientID2.setPacketType(PacketType.DATA_REQ_ACK);
					if (map.containsKey(clientID2)) {
						ttp.sendDatagram(map.get(clientID2));
					}
				} else if(((TTPSegment)datagram.getData()).getType().equals(PacketType.ACK)) {
					System.out.println("Received ACK for data at Server Side");
					ClientPacketID clientID = new ClientPacketID();
					clientID.setIPAddress(datagram.getSrcaddr());
					clientID.setPort(datagram.getSrcport());
					clientID.setPacketType(PacketType.ACK);

					Datagram data = map.putIfAbsent(clientID, datagram);
					int seqNum = ((TTPSegment)datagram.getData()).getSequenceNumber();
					if (data != null && ((TTPSegment)data.getData()).getSequenceNumber() < seqNum) {
						map.put(clientID, datagram);
					}
				} else if(((TTPSegment)datagram.getData()).getType().equals(PacketType.FIN)) {
					System.out.println("Received FIN for data at Server Side");
					ClientPacketID clientIDACK = new ClientPacketID();
					clientIDACK.setIPAddress(datagram.getSrcaddr());
					clientIDACK.setPort(datagram.getSrcport());
					clientIDACK.setPacketType(PacketType.ACK);
					Datagram dat = map.remove(clientIDACK);
					System.out.println("Removed from map: " + dat);
					
					ClientPacketID clientIDREQSYN = new ClientPacketID();
					clientIDREQSYN.setIPAddress(datagram.getSrcaddr());
					clientIDREQSYN.setPort(datagram.getSrcport());
					clientIDREQSYN.setPacketType(PacketType.DATA_REQ_SYN);
					dat = map.remove(clientIDREQSYN);
					System.out.println("Removed from map: " + dat);
					
					ClientPacketID clientIDSYN = new ClientPacketID();
					clientIDSYN.setIPAddress(datagram.getSrcaddr());
					clientIDSYN.setPort(datagram.getSrcport());
					clientIDSYN.setPacketType(PacketType.SYN);
					dat = map.remove(clientIDSYN);
					System.out.println("Removed from map: " + dat);
					
					ClientPacketID clientIDREQACK = new ClientPacketID();
					clientIDREQACK.setIPAddress(datagram.getSrcaddr());
					clientIDREQACK.setPort(datagram.getSrcport());
					clientIDREQACK.setPacketType(PacketType.DATA_REQ_ACK);
					dat = map.remove(clientIDREQACK);
					System.out.println("Removed from map: " + dat);

					ttp.sendAck(datagram, null, PacketType.FIN_ACK);
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
