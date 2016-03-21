package edu.cmu.TTP.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.cmu.TTP.constants.TTPConstants;
import edu.cmu.TTP.helpers.AcknowledgementHandler;
import edu.cmu.TTP.helpers.DataAcknowledgementHandler;
import edu.cmu.TTP.models.ConnectionEssentials;
import edu.cmu.TTP.models.Datagram;
import edu.cmu.TTP.models.PacketType;
import edu.cmu.TTP.models.TTPClientHelperModel;
import edu.cmu.TTP.models.TTPSegment;
import edu.cmu.TTP.models.TTPServerHelperModel;

public class TTPService {

	private DatagramService datagramService;

	public TTPService(int port) throws SocketException {
		datagramService = new DatagramService(port, 10);
	}
	
	/** 
	 * Send the SYN packet and wait for an ACk packet till time out.
	 * 
	 * @param connEssentials
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public boolean setupClientConnection(ConnectionEssentials connEssentials) 
			throws IOException, InterruptedException {
		
		// Send the SYN packet
		TTPSegment ttpSegment = new TTPSegment();
		ttpSegment.setType(PacketType.SYN);
		
		Datagram dt = new Datagram();
		dt.setDstaddr(connEssentials.getServerAddress());
		dt.setDstport(connEssentials.getServerPort());
		dt.setSrcaddr(connEssentials.getClientAddress());
		dt.setSrcport(connEssentials.getClientPort());
		dt.setData(ttpSegment); 
		this.sendDatagram(dt);
		
		TTPClientHelperModel clientHelperModel = new TTPClientHelperModel(this);
		Thread t = new Thread(new AcknowledgementHandler(clientHelperModel,PacketType.ACK));
		t.start();
		
		long startTime = System.currentTimeMillis();
		while(!clientHelperModel.isAckReceived()
				&& (System.currentTimeMillis() - startTime) < TTPConstants.RETRANSMISSION_TIMEOUT) {
			Thread.sleep(200L);  // Poll every 200ms
		}
		t.interrupt();
		return clientHelperModel.isAckReceived();
	}

	public void sendData(Datagram datagram) throws IOException, ClassNotFoundException, InterruptedException {
		List<Datagram> data = getListOfSegments(datagram);
		this.sendDataReqAck(datagram, data.size());
		
		TTPServerHelperModel serverHelperModel = new TTPServerHelperModel(this);
		Thread t = new Thread(new DataAcknowledgementHandler(serverHelperModel));
		t.start();
		
		// While we have not received acknowledgement for the entire data, continue sending
		while(serverHelperModel.getExpectingAcknowledgement() < data.size()) {
			sendNSegments(serverHelperModel.getStartingWindowSegment(), data);
			long startTime = System.currentTimeMillis();
			int endOFWindow = Math.min(data.size(), serverHelperModel.getStartingWindowSegment() + TTPConstants.WINDOW_SIZE);

			// While we have not received acknowledgement for all packets in the window OR
			// the transmission timeout is over
			while(serverHelperModel.getExpectingAcknowledgement() < endOFWindow 
					&& (System.currentTimeMillis() - startTime) < TTPConstants.RETRANSMISSION_TIMEOUT) {
				Thread.sleep(200L);  // Poll every 200ms
			}
			serverHelperModel.setStartingWindowSegment(serverHelperModel.getExpectingAcknowledgement());
		}
		t.interrupt();
	}

	private void sendNSegments(int startingWindowSegment, List<Datagram> data) throws IOException {
		for(int i = startingWindowSegment; i < startingWindowSegment + TTPConstants.WINDOW_SIZE; i++) {
			if (i >= data.size()) {
				break;
			}
			datagramService.sendDatagram(data.get(i));
		}
	}

	/**
	 * Creates a list of datagram segments by dividing the filesize into maximum 
	 * filesize blocks.
	 * 
	 * @param datagram
	 * @return
	 * @throws IOException
	 */
	private List<Datagram> getListOfSegments(Datagram datagram) throws IOException {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		ObjectOutputStream o = new ObjectOutputStream(b);
		o.writeObject(datagram.getData());
		byte[] data = b.toByteArray();
		
		List<Datagram> datagramList = new ArrayList<>();
		int numSegments = (int) Math.ceil((data.length + 0.0) / TTPConstants.MAX_SEGMENT_SIZE);
		for (int i = 0; i < numSegments; i++) {
			int start = i * TTPConstants.MAX_SEGMENT_SIZE;
			int end = Math.min(data.length, start + TTPConstants.MAX_SEGMENT_SIZE);
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
	
	public Datagram receiveDatagram() throws ClassNotFoundException, IOException {
		return datagramService.receiveDatagram();
	}

	public void sendDatagram(Datagram datagram) throws IOException{
		datagramService.sendDatagram(datagram);
	}
	
	public void closeConnection() {
	}

	public void sendAck(Datagram datagram, Integer sequenceNumber) throws IOException {
		Datagram ack = new Datagram();
		ack.setSrcaddr(datagram.getDstaddr());
		ack.setSrcport(datagram.getDstport());
		ack.setDstaddr(datagram.getSrcaddr());
		ack.setDstport(datagram.getSrcport());
		
		TTPSegment segment = new TTPSegment();
		segment.setType(PacketType.ACK);
		if(segment.getSequenceNumber() != null) {
			segment.setSequenceNumber(sequenceNumber);
		}
		ack.setData(segment);
		this.sendDatagram(ack);
	}
	
	private void sendDataReqAck(Datagram datagram, int size) throws IOException {
		Datagram ack = new Datagram();
		ack.setSrcaddr(datagram.getDstaddr());
		ack.setSrcport(datagram.getDstport());
		ack.setDstaddr(datagram.getSrcaddr());
		ack.setDstport(datagram.getSrcport());
		
		TTPSegment segment = new TTPSegment();
		segment.setType(PacketType.DATA_REQ_ACK);
		ack.setData(segment);
		this.sendDatagram(ack);
	}
}
