/**
 * 
 */
package edu.cmu.TTP.helpers;

import java.io.ByteArrayOutputStream;
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
	
	public ClientHelper(ConnectionEssentials connEssentials) throws SocketException {
		ttpService = new TTPService(connEssentials.getClientPort());
		this.connEssentials = connEssentials;
	}
	
	public TTPClientHelperModel requestForFile(String fileName) throws IOException, InterruptedException {
		Datagram datagram = new Datagram();
		datagram.setData(fileName);
		datagram.setSrcaddr(connEssentials.getClientAddress());
		datagram.setDstaddr(connEssentials.getServerAddress());
		datagram.setDstport(connEssentials.getServerPort());
		datagram.setSrcport(connEssentials.getClientPort());
		/* Send request for file */
		ttpService.sendDatagram(datagram);
		/* Wait for acknowledgement. Acknowledgement will also contain number of
		 * expected segments. Keep polling till timeout, otherwise, resend.
		 */
		TTPClientHelperModel requestFileHelper = new TTPClientHelperModel(ttpService);
		Thread t = new Thread(new AcknowledgementHandler(requestFileHelper, PacketType.DATA_REQ_ACK));
		t.start();
		
		long startTime = System.currentTimeMillis();
		while(!requestFileHelper.isAckReceived()
				&& (System.currentTimeMillis() - startTime) < TTPConstants.RETRANSMISSION_TIMEOUT) {
			Thread.sleep(200L);  // Poll every 200ms
		}
		t.interrupt();
		return requestFileHelper;
	}
	
	public void receiveDataHelper(TTPClientHelperModel clientHelperModel, String filename) 
														throws ClassNotFoundException, IOException {
		int numberOfPacketsRecieved = 0;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
		while(numberOfPacketsRecieved<=clientHelperModel.getNumberOfSegmentsToBeRecieved()) {
			Datagram datagram = ttpService.receiveDatagram();
			if (datagram.getData() != null) {
				TTPSegment segment = (TTPSegment) datagram.getData();
				if(clientHelperModel.getExpectedSequenceNumber()== segment.getSequenceNumber() 
						&& segment.getType().equals(PacketType.DATA)) {
					outputStream.write(segment.getData());
					clientHelperModel.increamentExpectedSequenceNumber();
					numberOfPacketsRecieved++;
				}
				/* Send acknowledgment */
				ttpService.sendAck(datagram,clientHelperModel.getExpectedSequenceNumber());
			}
		}
		System.out.println("Received File");
		
		//Store the data received in the localDisk
		PrintWriter out = new PrintWriter("clientFiles/" + filename);
		out.print(outputStream.toByteArray());
		out.close();
		System.out.println("Done saving the file");
	}
}
