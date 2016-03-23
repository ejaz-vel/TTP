/**
 * 
 */
package edu.cmu.TTP.helpers;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;

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
 */
public class ClientHelper {
	
	private ConnectionEssentials connEssentials;
	private TTPService ttpService;
	
	public ClientHelper(ConnectionEssentials connEssentials, TTPService ttp) throws SocketException {
		this.ttpService = ttp;
		this.connEssentials = connEssentials;
	}
	
	public TTPClientHelperModel requestForFile(String fileName) throws IOException, InterruptedException {
		Datagram datagram = new Datagram();
		datagram.setData(fileName);
		datagram.setSrcaddr(connEssentials.getClientAddress());
		datagram.setDstaddr(connEssentials.getServerAddress());
		datagram.setDstport(connEssentials.getServerPort());
		datagram.setSrcport(connEssentials.getClientPort());
		// Send request for file.
		ttpService.sendDatagram(datagram);
		System.out.println("Sent request for File");
		
		/* Wait for acknowledgement. Acknowledgement will also contain number of
		 * expected segments. Keep polling till timeout, otherwise, re-send.
		 */
		TTPClientHelperModel requestFileHelper = new TTPClientHelperModel(ttpService);
		Thread t = new Thread(new AcknowledgementHandler(requestFileHelper, PacketType.DATA_REQ_ACK));
		t.start();
		
		long startTime = System.currentTimeMillis();
		while(!requestFileHelper.isAckReceived()
				&& (System.currentTimeMillis() - startTime) < TTPConstants.RETRANSMISSION_TIMEOUT) {
			Thread.sleep(200L);  // Poll every 200ms
			System.out.println("Still waiting for File ACK");
		}
		System.out.println("Stopped waiting for File ACK");
		t.interrupt();
		//t.stop();
		return requestFileHelper;
	}
	
	public void receiveDataHelper(TTPClientHelperModel clientHelperModel, String filename) 
														throws ClassNotFoundException, IOException {
		int numberOfSegmentsRecieved = 0;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		System.out.println("Number of Segments to be Received: " + clientHelperModel.getNumberOfSegmentsToBeRecieved());
		while(numberOfSegmentsRecieved < clientHelperModel.getNumberOfSegmentsToBeRecieved()) {
			Datagram datagram = ttpService.receiveDatagram();
			System.out.println("Data Segment Received: " + datagram);
			if (datagram.getData() != null) {
				TTPSegment segment = (TTPSegment) datagram.getData();
				if(clientHelperModel.getExpectedSequenceNumber() == segment.getSequenceNumber() 
						&& segment.getType().equals(PacketType.DATA)) {
					outputStream.write(segment.getData());
					
					/* Send acknowledgment */
					ttpService.sendAck(datagram,clientHelperModel.getExpectedSequenceNumber(), PacketType.ACK);
					System.out.println("Sent ACK for Sequence: " + clientHelperModel.getExpectedSequenceNumber());
					clientHelperModel.increamentExpectedSequenceNumber();
					numberOfSegmentsRecieved++;
				}
			}
		}
		System.out.println("Received File");
		
		//Store the data received in the localDisk
		FileOutputStream out = new FileOutputStream("clientFiles/" + filename);
		out.write(outputStream.toByteArray());
		out.close();
		System.out.println("Done saving the file");
	}
}
