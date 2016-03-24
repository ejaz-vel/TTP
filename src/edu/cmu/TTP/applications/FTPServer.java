/*
 * A sample server that uses DatagramService
 */
package edu.cmu.TTP.applications;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import edu.cmu.TTP.helpers.FTPConnectionHandler;
import edu.cmu.TTP.models.ClientPacketID;
import edu.cmu.TTP.models.Datagram;
import edu.cmu.TTP.models.PacketType;
import edu.cmu.TTP.models.TTPSegment;
import edu.cmu.TTP.services.TTPService;

/**
 * @author apurv
 * 
 *         Main class for running the FTPServer. Capable of handling multiple
 *         clients by maintaining a mapping between the client and the threads.
 *         It works in the following way -
 * 
 *         <pre>
 *         1. As soon as we receive a SYN from a client -
 *         		1. We first check if we don't have a thread present for this client.
 *         		   This may happen if the client the ACK to the first SYN failed. 
 *                 In which case, the client would send another SYN.
 *              2. If we don't then we create a thread and associate this thread to 
 *                 this client (identified by IP and port)
 *              3. This thread continuously listens for datagrams.
 *         2. If we receive a DATA_REQ_SYN, we add a corresponding entry in the map.
 *         3. Since the FTPHandlerThread is continuously polling this map, it recieves
 *         	  the datagram, extracts filename from it and starts the process of sending 
 *            datagrams.
 *         4. For every datagram that the FTPHandler sends, we will get an ACK, which is 
 *         	  received by this FTP server, which in turn updates the entry in the map.
 *         5. Whenever the ack sequence number is updated in the map, the FTPHandler, picks
 *            it up and updates its window frame to send additional packets.
 *         6. Once the complete file is received, the client sends a FIN. 
 *         7. Upon receiving a FIN, the server removes all the client mappings, from the 
 *         	  map and kills the thread after sending a FIN_ACK.
 *         8. In case the FIN_ACK is not received by the client and it times out, it will
 *            send a FIN again and the server will try to do everything from step 7 again.
 *         </pre>
 */
public class FTPServer {
	private static TTPService ttp;
	public static int WINDOW_SIZE;

	public static void main(String[] args) throws IOException, ClassNotFoundException,
			InterruptedException, NoSuchAlgorithmException {
		if (args.length != 3) {
			printUsage();
		}
		System.out.println("Starting Server ...");
		int port = Integer.parseInt(args[0]);
		int timeout = Integer.parseInt(args[1]);
		WINDOW_SIZE = Integer.parseInt(args[2]);
		ttp = new TTPService(port, timeout);
		run();
	}

	private static void run() throws IOException, ClassNotFoundException,
			InterruptedException, NoSuchAlgorithmException {
		ConcurrentMap<ClientPacketID, Datagram> map = new ConcurrentHashMap<>();
		ConcurrentMap<ClientPacketID, Long> threadMap = new ConcurrentHashMap<>();
		while (true) {
			try {
				Datagram datagram = ttp.receiveDatagram();
				if (((TTPSegment) datagram.getData()).getType().equals(PacketType.SYN)) {
					System.out.println("Received SYN datagram from " + datagram);
					ttp.sendAck(datagram, null, PacketType.ACK);
					ClientPacketID clientID = new ClientPacketID();
					clientID.setIPAddress(datagram.getSrcaddr());
					clientID.setPort(datagram.getSrcport());
					clientID.setPacketType(PacketType.SYN);
					if (!map.containsKey(clientID)) {
						// Spawn a new thread only for the first SYN packet to
						// transfer the file contents
						Thread t = new Thread(
								new FTPConnectionHandler(map, threadMap, datagram, ttp));
						t.start();
					}
					map.put(clientID, datagram);
				} else if (((TTPSegment) datagram.getData()).getType()
						.equals(PacketType.DATA_REQ_SYN)) {
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
				} else if (((TTPSegment) datagram.getData()).getType()
						.equals(PacketType.ACK)) {
					System.out.println("Received ACK for data at Server Side");
					ClientPacketID clientID = new ClientPacketID();
					clientID.setIPAddress(datagram.getSrcaddr());
					clientID.setPort(datagram.getSrcport());
					clientID.setPacketType(PacketType.ACK);
					Datagram data = map.putIfAbsent(clientID, datagram);
					int seqNum = ((TTPSegment) datagram.getData()).getSequenceNumber();
					if (data != null && ((TTPSegment) data.getData())
							.getSequenceNumber() < seqNum) {
						map.put(clientID, datagram);
					}
				} else if (((TTPSegment) datagram.getData()).getType()
						.equals(PacketType.FIN)) {
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
					Long threadID = threadMap.get(clientIDREQSYN);
					if (threadID != null) {
						threadMap.remove(clientIDSYN);
						// Kill this thread
						Set<Thread> setOfThread = Thread.getAllStackTraces().keySet();
						// Iterate over set to find the thread ID
						for (Thread thread : setOfThread) {
							if (thread.getId() == threadID) {
								thread.interrupt();
								break;
							}
						}
					}
					ttp.sendAck(datagram, null, PacketType.FIN_ACK);
				}
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
		}
	}

	private static void printUsage() {
		System.out.println("Usage: server <port> <timeout> <windowSize>");
		System.exit(-1);
	}
}
