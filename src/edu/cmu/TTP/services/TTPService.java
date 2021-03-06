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

import edu.cmu.TTP.applications.FTPServer;
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

/**
 * @author apurv
 * 
 *         Class which contains methods to service both client side and server
 *         side connections.
 *
 */
public class TTPService {
	private DatagramService datagramService;
	public int RETRANSMISSION_TIMEOUT;

	public TTPService(int port, int timeout) throws SocketException {
		datagramService = new DatagramService(port, 10);
		this.RETRANSMISSION_TIMEOUT = timeout;
	}

	/**
	 * Send the SYN packet and wait for an ACK packet till time out. Once this
	 * happens, the connection has been properly set up.
	 * 
	 * @param connEssentials
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public boolean setupClientConnection(ConnectionEssentials connEssentials)
			throws IOException, InterruptedException {
		// Send the SYN packet
		TTPClientHelperModel clientHelperModel = new TTPClientHelperModel(this);
		int transmissionAttempts = 0;
		while (!clientHelperModel.isAckReceived()
				&& transmissionAttempts < TTPConstants.MAX_RETRY) {
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
			Thread t = new Thread(
					new AcknowledgementHandler(clientHelperModel, PacketType.ACK));
			t.start();
			long startTime = System.currentTimeMillis();
			while (!clientHelperModel.isAckReceived() && (System.currentTimeMillis()
					- startTime) < this.RETRANSMISSION_TIMEOUT) {
				Thread.sleep(200L); // Poll every 200ms
				System.out.println("Still waiting for ACK");
			}
			System.out.println("Stopped waiting for ACK " + t.getName());
			t.interrupt();
		}
		return clientHelperModel.isAckReceived();
	}

	/**
	 * This function is responsible for sending the file into datagram chunks.
	 * This is done by -
	 * 
	 * <pre>
	 * 1. Dividing the file into n datagrams depending on the size of the file 
	 * 	  and maximum size supported by Datagrams. 
	 * 2. Defining a thread which waits for acknowledgments. 
	 * 		1. We consider an ack packet only if the sequence number is greater
	 * 		   than the expected sequence number. 
	 * 		2. The ack is discarded otherwise and the datagram with the smallest
	 * 		   sequence number for which ack hasn't been recieved is sent again. 
	 * 3. The main thread keeps polling the other thread to check if a new ack
	 * 	  has been received.
	 * 		1. If so, we move the window ahead.
	 * 		2. If no acks are received before the timeout, we simply send the packets
	 * 		   of the window again.
	 * </pre>
	 * 
	 * @param datagram
	 * @param map
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws InterruptedException
	 * @throws NoSuchAlgorithmException
	 */
	public void sendData(Datagram datagram, ConcurrentMap<ClientPacketID, Datagram> map)
			throws IOException, ClassNotFoundException, InterruptedException,
			NoSuchAlgorithmException {
		List<Datagram> data = getListOfSegments(datagram);
		this.sendDataReqAck(datagram, data.size(), map);
		TTPServerHelperModel serverHelperModel = new TTPServerHelperModel(this);
		Thread t = new Thread(new DataAcknowledgementHandler(serverHelperModel,
				datagram.getDstaddr(), datagram.getDstport(), map));
		t.start();
		// While we have not received acknowledgement for the entire data,
		// continue sending
		while (serverHelperModel.getExpectingAcknowledgement() < data.size()) {
			sendNSegments(serverHelperModel.getStartingWindowSegment(), data);
			long startTime = System.currentTimeMillis();
			int endOFWindow = Math.min(data.size(),
					serverHelperModel.getStartingWindowSegment() + FTPServer.WINDOW_SIZE);
			// While we have not received acknowledgement for all packets in the
			// window OR
			// the transmission timeout is over
			while (serverHelperModel.getExpectingAcknowledgement() < endOFWindow
					&& (System.currentTimeMillis()
							- startTime) < this.RETRANSMISSION_TIMEOUT) {
				Thread.sleep(200L); // Poll every 200ms
			}
			serverHelperModel.setStartingWindowSegment(
					serverHelperModel.getExpectingAcknowledgement());
		}
		t.interrupt();
	}

	/**
	 * Method responsible for sending all datagrams in the current window.
	 * 
	 * @param startingWindowSegment
	 * @param data
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws InterruptedException
	 */
	private void sendNSegments(int startingWindowSegment, List<Datagram> data)
			throws IOException, ClassNotFoundException, InterruptedException {
		for (int i = startingWindowSegment; i < startingWindowSegment
				+ FTPServer.WINDOW_SIZE; i++) {
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
		byte[] data = (byte[]) datagram.getData();
		List<Datagram> datagramList = new ArrayList<>();
		int numSegments = (int) Math
				.ceil((data.length + 0.0) / TTPConstants.MAX_SEGMENT_SIZE);
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

	/**
	 * Method to receive datagram by calling the datagram service layer
	 * 
	 * @return
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public Datagram receiveDatagram() throws ClassNotFoundException, IOException {
		Datagram receivedData = datagramService.receiveDatagram();
		return receivedData;
	}

	/**
	 * Method to send datagram by calling the datagram service layer
	 * 
	 * @param datagram
	 * @throws IOException
	 */
	public void sendDatagram(Datagram datagram) throws IOException {
		datagram.setChecksum(calculateChecksum(datagram));
		datagramService.sendDatagram(datagram);
	}

	/**
	 * Closes the connection by sending a fin packet and exits after getting an
	 * fin-ack from the server
	 * 
	 * @param connectionEssentials
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public boolean closeClientSideConnection(ConnectionEssentials connectionEssentials)
			throws IOException, InterruptedException {
		// Send the SYN packet
		TTPClientHelperModel clientHelperModel = new TTPClientHelperModel(this);
		int transmissionAttempts = 0;
		while (!clientHelperModel.isAckReceived()
				&& transmissionAttempts < TTPConstants.MAX_RETRY) {
			transmissionAttempts++;
			Thread t = new Thread(
					new AcknowledgementHandler(clientHelperModel, PacketType.FIN_ACK));
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
			while (!clientHelperModel.isAckReceived() && (System.currentTimeMillis()
					- startTime) < this.RETRANSMISSION_TIMEOUT) {
				Thread.sleep(200L); // Poll every 200ms
				System.out.println("Still waiting for FIN_ACK");
			}
			System.out.println("Stopped waiting for FIN_ACK");
			t.interrupt();
		}
		return clientHelperModel.isAckReceived();
	}

	/**
	 * Method calculates the Checksum of a datgram.
	 * 
	 * @param datagram
	 * @return
	 * @throws IOException
	 */
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

	/**
	 * Responsible for sending acks in response to a packet.
	 * 
	 * @param datagram
	 * @param sequenceNumber
	 * @param ackType
	 * @throws IOException
	 */
	public void sendAck(Datagram datagram, Integer sequenceNumber, PacketType ackType)
			throws IOException {
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

	/**
	 * Constructs and sends the DATA_REQ_ACK in response to the DATA_REQ_SYN.
	 * This also conveys additional information about number of segments to be
	 * sent and the md5sum of the original file.
	 * 
	 * @param datagram
	 * @param size
	 * @param map
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	public void sendDataReqAck(Datagram datagram, int size,
			ConcurrentMap<ClientPacketID, Datagram> map)
					throws IOException, NoSuchAlgorithmException {
		TTPUtil ttpUtil = new TTPUtil();
		Datagram ack = new Datagram();
		ack.setSrcaddr(datagram.getSrcaddr());
		ack.setSrcport(datagram.getSrcport());
		ack.setDstaddr(datagram.getDstaddr());
		ack.setDstport(datagram.getDstport());
		TTPSegment segment = new TTPSegment();
		String md5Sum = ttpUtil.calculateMd5((byte[]) datagram.getData());
		String bytesToBeSent = "numberOfSegments:" + String.valueOf(size) + ",md5Sum:"
				+ md5Sum;
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
}
