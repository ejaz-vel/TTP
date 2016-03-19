package edu.cmu.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.cmu.helpers.ClientHelper;
import edu.cmu.models.Datagram;
import edu.cmu.models.PacketType;
import edu.cmu.models.TTPSegment;

public class TTPService {

	private static final int MAX_SEGMENT_SIZE = 1450;
	private static final int WINDOW_SIZE = 4;
	private static final int RETRANSMISSION_TIMEOUT = 5000;

	private DatagramService datagramService;
	private boolean synAckReceived;
	private int expectingAcknowledgement = 0;
	private int startingWindowSegment = 0;	

	class AcknowledgementHandler implements Runnable {

		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
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
					e.printStackTrace();
				}
			}
		}
	}
	
	class SYNAcknowledgementHandler implements Runnable {

		@Override
		public void run() {
			while (!synAckReceived) {
				try { 
					Datagram dat = datagramService.receiveDatagram();
					if (dat.getData() != null) {
						TTPSegment segment = (TTPSegment) dat.getData();
						if (segment.getType() == PacketType.SYN_ACK) {
							synAckReceived = true;
						}
					}
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public TTPService(int port) throws SocketException {
		datagramService = new DatagramService(port, 10);
		synAckReceived = false;
	}

	public boolean setupConnection(String destIPAddress, String dstPort) throws IOException, InterruptedException {
		TTPSegment ttpSegment = new TTPSegment();
		ttpSegment.setType(PacketType.SYN);
		
		Datagram dt = new Datagram();
		dt.setDstaddr(destIPAddress);
		dt.setDstaddr(dstPort);
		dt.setData(ttpSegment);
		datagramService.sendDatagram(dt);
		
		Thread t = new Thread(new SYNAcknowledgementHandler());
		t.start();
		
		long startTime = System.currentTimeMillis();
		while(!synAckReceived 
				&& (System.currentTimeMillis() - startTime) < RETRANSMISSION_TIMEOUT) {
			Thread.sleep(200L);  // Poll every 200ms
		}
		t.interrupt();
		return synAckReceived;
	}

	public void sendData(Datagram datagram) throws IOException, ClassNotFoundException, InterruptedException {
		List<Datagram> data = getListOfSegments(datagram);

		Thread t = new Thread(new AcknowledgementHandler());
		t.start();
		
		// While we have not received acknowledgement for the entire data, continue sending
		while(expectingAcknowledgement < data.size()) {
			sendNSegments(startingWindowSegment, data);
			long startTime = System.currentTimeMillis();
			int endOFWindow = Math.min(data.size(), startingWindowSegment + WINDOW_SIZE);

			// While we have not received acknowledgement for all packets in the window OR
			// the transmission timeout is over
			while(expectingAcknowledgement < endOFWindow 
					&& (System.currentTimeMillis() - startTime) < RETRANSMISSION_TIMEOUT) {
				Thread.sleep(200L);  // Poll every 200ms
			}
			startingWindowSegment = expectingAcknowledgement;
		}
		t.interrupt();
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

	public Datagram receiveData(String filename) throws ClassNotFoundException, IOException {
		ClientHelper.receiveDataHelper(filename);
		return null;
	}
	
	public Datagram receiveData() throws ClassNotFoundException, IOException {
		return datagramService.receiveDatagram();
	}

	public void closeConnection() {
	}

}
