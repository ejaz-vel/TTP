package edu.cmu.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.cmu.models.Datagram;
import edu.cmu.models.PacketType;
import edu.cmu.models.TTPSegment;

public class TTPService {

	private DatagramService datagramService;
	private static final int MAX_SEGMENT_SIZE = 1450;
	private static final int WINDOW_SIZE = 4;

	private int expectingAcknowledgement = 0;
	private int startingWindowSegment = 0;	

	class AcknowledgementHandler implements Runnable {

		@Override
		public void run() {
			while (true) {
				try { 
					Datagram dat = datagramService.receiveDatagram();
					if (dat.getData() != null) {
						TTPSegment segment = (TTPSegment) dat.getData();
						if (segment.getType() == PacketType.ACK 
								&& segment.getSequenceNumber() == expectingAcknowledgement) {
							expectingAcknowledgement++;
						}
					}
				} catch (IOException | ClassNotFoundException e) {
					// Someone woke us up during sleep, that's OK
					break;
				}
			}

		}

	}

	public TTPService(int port) throws SocketException {
		datagramService = new DatagramService(port, 10);
		// TODO setup window size here
	}

	public boolean setupConnection() {
		// TODO Send a SYN packet
		// TODO Define a timeout for this SYN Packet
		// TODO If no ACK packet arrives within the timeout period, retransmit the SYN packet
		// TODO If we don't get an ACK after 3 consecutive retransmits, return false
		// TODO Return true if we get an ACK from the receiver
		// TODO Also send sequence number in the ACK and SYN packets, so that both sides know which sequence number to accept/send
		return true;
	}

	public void sendData(Datagram datagram) throws IOException, ClassNotFoundException, InterruptedException {
		List<Datagram> data = getListOfSegments(datagram);
		int retransmissionTimeout = 5000;

		// While we have not received acknowledgement for the entire data, continue sending
		while(expectingAcknowledgement < data.size()) {
			sendNSegments(startingWindowSegment, data);

			long startTime = System.currentTimeMillis();
			int endOFWindow = Math.min(data.size(), startingWindowSegment + WINDOW_SIZE);

			// While we have not received acknowledgement for all packets in the window OR
			// the transmission timeout is over
			//			while(expectingAcknowledgement < endOFWindow 
			//					&& (System.currentTimeMillis() - startTime) < retransmissionTimeout) {
			//				Datagram dat = datagramService.receiveDatagram();
			//				if (dat.getData() != null) {
			//					TTPSegment segment = (TTPSegment) dat.getData();
			//					if (segment.getType() == PacketType.ACK 
			//							&& segment.getSequenceNumber() == expectingAcknowledgement) {
			//						expectingAcknowledgement++;
			//					}
			//				}
			//			}
			//			startingWindowSegment = expectingAcknowledgement;

			Thread t = new Thread(new AcknowledgementHandler());
			t.start();
			while(expectingAcknowledgement < endOFWindow 
					&& (System.currentTimeMillis() - startTime) < retransmissionTimeout) {
				Thread.sleep(200L);  // Poll every 200ms
			}
			t.interrupt();
			t.join();
			startingWindowSegment = expectingAcknowledgement;
		}
	}

	private void sendNSegments(int startingWindowSegment, List<Datagram> data) throws IOException {
		for(int i = startingWindowSegment; i < startingWindowSegment + WINDOW_SIZE; i++) {
			if (i >= data.size()) {
				break;
			}
			datagramService.sendDatagram(data.get(i));
		}
	}

	private List<Datagram> getListOfSegments(Datagram datagram) throws IOException {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		ObjectOutputStream o = new ObjectOutputStream(b);
		o.writeObject(datagram.getData());
		byte[] data = b.toByteArray();
		List<Datagram> datagramList = new ArrayList<>();
		int numSegments = (int) Math.ceil((data.length + 0.0) / MAX_SEGMENT_SIZE);

		for (int i = 0; i < numSegments; i++) {
			int start = i * MAX_SEGMENT_SIZE;
			int end = Math.min(data.length, start + MAX_SEGMENT_SIZE);
			byte[] segmentData = Arrays.copyOfRange(data, start, end);

			TTPSegment ttpSegment = new TTPSegment();
			ttpSegment.setData(segmentData);
			ttpSegment.setSequenceNumber(i);
			ttpSegment.setType(PacketType.DATA);

			Datagram dt = new Datagram();
			dt.setSrcaddr(datagram.getSrcaddr());
			dt.setDstaddr(datagram.getDstaddr());
			dt.setSrcport(datagram.getSrcport());
			dt.setDstport(datagram.getDstport());
			dt.setData(ttpSegment);

			datagramList.add(dt);
		}
		return datagramList;
	}

	public Datagram receiveData() throws ClassNotFoundException, IOException {
		return datagramService.receiveDatagram();
	}

	public void closeConnection() {
		// TODO
	}

}
