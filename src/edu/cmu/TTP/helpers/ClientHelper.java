/**
 * 
 */
package edu.cmu.TTP.helpers;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;

import edu.cmu.TTP.constants.TTPConstants;
import edu.cmu.TTP.models.ConnectionEssentials;
import edu.cmu.TTP.models.Datagram;
import edu.cmu.TTP.models.PacketType;
import edu.cmu.TTP.models.TTPClientHelperModel;
import edu.cmu.TTP.models.TTPSegment;
import edu.cmu.TTP.services.TTPService;

/**
 * @author apurv
 * 
 *         This class provides helper methods for the client.
 *
 */
public class ClientHelper {
	private ConnectionEssentials connEssentials;
	private TTPService ttpService;

	public ClientHelper(ConnectionEssentials connEssentials, TTPService ttp)
			throws SocketException {
		this.ttpService = ttp;
		this.connEssentials = connEssentials;
	}

	/**
	 * This method is responsible for requesting for a file using its filename
	 * after the connection is setup between the client and the server. This is
	 * done by sending a DATA_REQ_SYN packet and then we wait for a DATA_REQ_ACK
	 * from the server to make sure that our file request was received by the
	 * server. If no ack is received within the time out period, the request is
	 * made again.
	 * 
	 * <pre>
	 *  In addition to acting as a DATA request ack, the packet also carries some
	 *  additional data -
	 * 		 1. Number of packets to be expected (based on the division of the file).
	 * 		 2. md5Sum of the original file(To be used to compare the against 
	 * 			md5 for received file).
	 * </pre>
	 * 
	 * @param fileName
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public TTPClientHelperModel requestForFile(String fileName)
			throws IOException, InterruptedException {
		TTPClientHelperModel requestFileHelper = new TTPClientHelperModel(ttpService);
		int transmissionAttempts = 0;
		while (!requestFileHelper.isAckReceived()
				&& transmissionAttempts < TTPConstants.MAX_RETRY) {
			transmissionAttempts++;
			/*
			 * Wait for acknowledgement. Acknowledgement will also contain
			 * number of expected segments. Keep polling till timeout,
			 * otherwise, re-send.
			 */
			Thread t = new Thread(new AcknowledgementHandler(requestFileHelper,
					PacketType.DATA_REQ_ACK));
			t.start();
			TTPSegment ttpSegment = new TTPSegment();
			ttpSegment.setType(PacketType.DATA_REQ_SYN);
			ttpSegment.setSequenceNumber(null);
			ttpSegment.setData(fileName.getBytes());
			Datagram datagram = new Datagram();
			datagram.setData(ttpSegment);
			datagram.setSrcaddr(connEssentials.getClientAddress());
			datagram.setDstaddr(connEssentials.getServerAddress());
			datagram.setDstport(connEssentials.getServerPort());
			datagram.setSrcport(connEssentials.getClientPort());
			// Send request for file.
			ttpService.sendDatagram(datagram);
			System.out.println("Sent request for File");
			long startTime = System.currentTimeMillis();
			while (!requestFileHelper.isAckReceived() && (System.currentTimeMillis()
					- startTime) < ttpService.RETRANSMISSION_TIMEOUT) {
				Thread.sleep(200L); // Poll every 200ms
				System.out.println("Still waiting for File ACK");
			}
			System.out.println("Stopped waiting for File ACK");
			t.interrupt();
		}
		return requestFileHelper;
	}

	/**
	 * This method is responsible for iteratively receiving datagrams from the
	 * server and sending acknowledgments. A packet is received and processed
	 * only if the sequence number of the datagram is the same as the expected
	 * sequence number. If packet is accepted and there are still some packets
	 * to be received(based on the numberOfExpectedSegments), we send an ack
	 * with the next sequence number that we are expecting.
	 * 
	 * <pre>
	 *  Once all the packets are recieved, we check the md5 of the received file 
	 *  with that of the original file. If the hash matches, then the file is 
	 *  written on the disk else, it is discarded.
	 * </pre>
	 * 
	 * @param clientHelperModel
	 * @param filename
	 * @throws ClassNotFoundException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	public void receiveDataHelper(TTPClientHelperModel clientHelperModel, String filename)
			throws ClassNotFoundException, IOException, NoSuchAlgorithmException {
		int numberOfSegmentsRecieved = 0;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		System.out.println("Number of Segments to be Received: "
				+ clientHelperModel.getNumberOfSegmentsToBeRecieved());
		while (numberOfSegmentsRecieved < clientHelperModel
				.getNumberOfSegmentsToBeRecieved()) {
			Datagram datagram = ttpService.receiveDatagram();
			System.out.println("Data Segment Received: " + datagram);
			if (datagram.getData() != null) {
				TTPSegment segment = (TTPSegment) datagram.getData();
				if (clientHelperModel.getExpectedSequenceNumber() == segment
						.getSequenceNumber() && segment.getType().equals(PacketType.DATA)
						&& datagram.getChecksum() == ttpService
								.calculateChecksum(datagram)) {
					outputStream.write(segment.getData());
					/* Send acknowledgment */
					ttpService.sendAck(datagram,
							clientHelperModel.getExpectedSequenceNumber(),
							PacketType.ACK);
					System.out.println("Sent ACK for Sequence: "
							+ clientHelperModel.getExpectedSequenceNumber());
					clientHelperModel.increamentExpectedSequenceNumber();
					numberOfSegmentsRecieved++;
				} else {
					ttpService.sendAck(datagram,
							clientHelperModel.getExpectedSequenceNumber() - 1,
							PacketType.ACK);
					System.out.println("Sent ACK for Sequence: "
							+ (clientHelperModel.getExpectedSequenceNumber() - 1));
				}
			}
		}
		System.out.println("Received File");
		if (checkHashOfFile(clientHelperModel, outputStream)) {
			writeBytesToFile(outputStream, filename);
		} else {
			System.out.println("Discarding the file since the md5 hash does not change");
		}
	}

	/**
	 * Method writes the bytes that were recieved from the server into a file.
	 * 
	 * @param outputStream
	 * @param filename
	 * @throws IOException
	 */
	private void writeBytesToFile(ByteArrayOutputStream outputStream, String filename)
			throws IOException {
		// Store the data received in the localDisk
		FileOutputStream out = new FileOutputStream("clientFiles/" + filename);
		out.write(outputStream.toByteArray());
		out.close();
		System.out.println("Done saving the file");
	}

	/**
	 * Method responsible for comparing the hash of the received file with the
	 * hash of the original file.
	 * 
	 * @param clientHelperModel
	 * @param outputStream
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	private boolean checkHashOfFile(TTPClientHelperModel clientHelperModel,
			ByteArrayOutputStream outputStream) throws NoSuchAlgorithmException {
		TTPUtil ttpUtil = new TTPUtil();
		String originalMd5 = clientHelperModel.getMd5Sum();
		String newMd5 = ttpUtil.calculateMd5(outputStream.toByteArray());
		return originalMd5.equals(newMd5);
	}
}
