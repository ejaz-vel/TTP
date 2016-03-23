package edu.cmu.TTP.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import edu.cmu.TTP.constants.TTPConstants;
import edu.cmu.TTP.helpers.AcknowledgementHandler;
import edu.cmu.TTP.helpers.DataAcknowledgementHandler;
import edu.cmu.TTP.helpers.TTPUtil;
import edu.cmu.TTP.models.ClientPacketID;
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
	 * Send the SYN packet and wait for an ACK packet till time out.
	 * 
	 * @param connEssentials
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public boolean setupClientConnection(ConnectionEssentials connEssentials) throws IOException, InterruptedException {

		// Send the SYN packet
		TTPClientHelperModel clientHelperModel = new TTPClientHelperModel(this);
		int transmissionAttempts = 0;
		while (!clientHelperModel.isAckReceived() && transmissionAttempts < TTPConstants.MAX_RETRY) {
			transmissionAttempts++;
			System.out.println("SYN Transmission attempt " + transmissionAttempts);
			TTPSegment ttpSegment = new TTPSegment();
			ttpSegment.setData(null);
			ttpSegment.setType(PacketType.SYN);

			Datagram dt = new Datagram();
			dt.setDstaddr(connEssentials.getServerAddress());
			dt.setDstport(connEssentials.getServerPort());
			dt.setSrcaddr(connEssentials.getClientAddress());
			dt.setSrcport(connEssentials.getClientPort());
			dt.setData(ttpSegment);
			this.sendDatagram(dt);
			System.out.println("Sent SYN Packet to server");

			Thread t = new Thread(new AcknowledgementHandler(clientHelperModel, PacketType.ACK));
			t.start();

			long startTime = System.currentTimeMillis();
			while (!clientHelperModel.isAckReceived()
					&& (System.currentTimeMillis() - startTime) < TTPConstants.RETRANSMISSION_TIMEOUT) {
				Thread.sleep(200L); // Poll every 200ms
				System.out.println("Still waiting for ACK");
			}
			System.out.println("Stopped waiting for ACK "+t.getName());
			t.interrupt();
		}

		return clientHelperModel.isAckReceived();
	}

	public void sendData(Datagram datagram, ConcurrentMap<ClientPacketID, Datagram> map) throws IOException, ClassNotFoundException, InterruptedException, NoSuchAlgorithmException {
		List<Datagram> data = getListOfSegments(datagram);
		this.sendDataReqAck(datagram, data.size(), map);

		TTPServerHelperModel serverHelperModel = new TTPServerHelperModel(this);
		Thread t = new Thread(new DataAcknowledgementHandler(serverHelperModel, datagram.getDstaddr(), datagram.getDstport(), map));
		t.start();

		// While we have not received acknowledgement for the entire data,
		// continue sending
		while (serverHelperModel.getExpectingAcknowledgement() < data.size()) {
			sendNSegments(serverHelperModel.getStartingWindowSegment(), data);
			long startTime = System.currentTimeMillis();
			int endOFWindow = Math.min(data.size(),
					serverHelperModel.getStartingWindowSegment() + TTPConstants.WINDOW_SIZE);

			// While we have not received acknowledgement for all packets in the
			// window OR
			// the transmission timeout is over
			while (serverHelperModel.getExpectingAcknowledgement() < endOFWindow
					&& (System.currentTimeMillis() - startTime) < TTPConstants.RETRANSMISSION_TIMEOUT) {
				Thread.sleep(200L); // Poll every 200ms
			}
			serverHelperModel.setStartingWindowSegment(serverHelperModel.getExpectingAcknowledgement());
		}
		t.interrupt();
	}

	private void sendNSegments(int startingWindowSegment, List<Datagram> data)
			throws IOException, ClassNotFoundException, InterruptedException {
		for (int i = startingWindowSegment; i < startingWindowSegment + TTPConstants.WINDOW_SIZE; i++) {
			if (i >= data.size()) {
				break;
			}
			System.out.println("Sending Packet: " + data.get(i));
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
		byte[] data = (byte[])datagram.getData();
		System.out.println(new String(data));

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
			dt.setChecksum(calculateChecksum(dt));

			datagramList.add(dt);
		}
		return datagramList;
	}

	public Datagram receiveDatagram() throws ClassNotFoundException, IOException {
		Datagram receivedData = datagramService.receiveDatagram();
		return receivedData;
	}

	public void sendDatagram(Datagram datagram) throws IOException {
		datagram.setChecksum(calculateChecksum(datagram));
		datagramService.sendDatagram(datagram);
	}

	/**
	 * Closes the connection by sending a fin packet and exits after getting an fin-ack from
	 * the server
	 * 
	 * @param connectionEssentials 
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public boolean closeClientSideConnection(ConnectionEssentials connectionEssentials) throws IOException, InterruptedException {
		// Send the SYN packet
		TTPClientHelperModel clientHelperModel = new TTPClientHelperModel(this);
		int transmissionAttempts = 0;
		while (!clientHelperModel.isAckReceived() && transmissionAttempts < TTPConstants.MAX_RETRY) {
			transmissionAttempts++;
			Thread t = new Thread(new AcknowledgementHandler(clientHelperModel, PacketType.FIN_ACK));
			t.start();

			System.out.println("FIN Transmission attempt " + transmissionAttempts);
			TTPSegment ttpSegment = new TTPSegment();
			ttpSegment.setData(null);
			ttpSegment.setType(PacketType.FIN);

			Datagram dt = new Datagram();
			dt.setDstaddr(connectionEssentials.getServerAddress());
			dt.setDstport(connectionEssentials.getServerPort());
			dt.setSrcaddr(connectionEssentials.getClientAddress());
			dt.setSrcport(connectionEssentials.getClientPort());
			dt.setData(ttpSegment);
			this.sendDatagram(dt);
			System.out.println("Sent FIN Packet to server");

			long startTime = System.currentTimeMillis();
			while (!clientHelperModel.isAckReceived()
					&& (System.currentTimeMillis() - startTime) < TTPConstants.RETRANSMISSION_TIMEOUT) {
				Thread.sleep(200L); // Poll every 200ms
				System.out.println("Still waiting for FIN_ACK");
			}
			System.out.println("Stopped waiting for FIN_ACK");
			t.interrupt();
		}
		return clientHelperModel.isAckReceived();
	}

	public long calculateChecksum(Datagram datagram) throws IOException {
		long prevChecksum = datagram.getChecksum();
		datagram.setChecksum(0);

		ByteArrayOutputStream b = new ByteArrayOutputStream();
		ObjectOutputStream o = new ObjectOutputStream(b);
		o.writeObject(datagram.getData());
		byte[] data = b.toByteArray();
		Checksum ch = new CRC32();
		ch.update(data, 0, data.length);
		long calcChecksum = ch.getValue();

		datagram.setChecksum(prevChecksum);
		return calcChecksum;
	}

	public void sendAck(Datagram datagram, Integer sequenceNumber, PacketType ackType) throws IOException {
		Datagram ack = new Datagram();
		ack.setSrcaddr(datagram.getDstaddr());
		ack.setSrcport(datagram.getDstport());
		ack.setDstaddr(datagram.getSrcaddr());
		ack.setDstport(datagram.getSrcport());

		TTPSegment segment = new TTPSegment();
		segment.setData(null);
		segment.setType(ackType);
		if (sequenceNumber != null) {
			segment.setSequenceNumber(sequenceNumber);
		}
		ack.setData(segment);
		this.sendDatagram(ack);
	}

	public void sendDataReqAck(Datagram datagram, int size, ConcurrentMap<ClientPacketID, Datagram> map) throws IOException, NoSuchAlgorithmException {
		TTPUtil ttpUtil = new TTPUtil();
		Datagram ack = new Datagram();
		ack.setSrcaddr(datagram.getSrcaddr());
		ack.setSrcport(datagram.getSrcport());
		ack.setDstaddr(datagram.getDstaddr());
		ack.setDstport(datagram.getDstport());

		TTPSegment segment = new TTPSegment();

		String md5Sum = ttpUtil.calculateMd5((byte[])datagram.getData());
		String bytesToBeSent = "numberOfSegments:"+String.valueOf(size)+",md5Sum:"+md5Sum;
		segment.setData(bytesToBeSent.getBytes());
		segment.setType(PacketType.DATA_REQ_ACK);
		ack.setData(segment);
		this.sendDatagram(ack);
		
		ClientPacketID clientID = new ClientPacketID();
		clientID.setIPAddress(ack.getDstaddr());
		clientID.setPort(ack.getDstport());
		clientID.setPacketType(PacketType.DATA_REQ_ACK);
		map.putIfAbsent(clientID, ack);
	}

	public void waitForClose() throws ClassNotFoundException, IOException {
		Datagram fin = receiveDatagram();
		if(fin.getData()!=null) {
			if(((TTPSegment)fin.getData()).getType().equals(PacketType.FIN)) {
				sendAck(fin,null, PacketType.FIN_ACK);
			}
		}
	}	
}
