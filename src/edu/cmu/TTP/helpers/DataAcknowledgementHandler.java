/**
 * 
 */
package edu.cmu.TTP.helpers;

import java.util.concurrent.ConcurrentMap;

import edu.cmu.TTP.models.ClientPacketID;
import edu.cmu.TTP.models.Datagram;
import edu.cmu.TTP.models.PacketType;
import edu.cmu.TTP.models.TTPSegment;
import edu.cmu.TTP.models.TTPServerHelperModel;

/**
 * @author Ejaz
 * 
 *         This class is responsible for running in the background as a thread
 *         and waiting for acknowledgments. The acknowledgments are only
 *         considered if sequence number of ack is greater than the expected ack
 *         number or else, is discarded. 
 *         
 *         This approach is adopted to take care of duplicate acks.
 * 
 *         Since the server should be able to handle multiple requests, a received 
 *         ack should be able to be mapped to the correct client. This is done by 
 *         maintaining a mapping in a ConcurrentMap.
 * 
 *
 */
public class DataAcknowledgementHandler implements Runnable {
	private TTPServerHelperModel serverHelperModel = null;
	private ConcurrentMap<ClientPacketID, Datagram> map = null;
	private String clientIPAddress = null;
	private short port;

	public DataAcknowledgementHandler(TTPServerHelperModel serverHelperModel,
			String clientIPAddress, short port,
			ConcurrentMap<ClientPacketID, Datagram> map) {
		this.serverHelperModel = serverHelperModel;
		this.map = map;
		this.clientIPAddress = clientIPAddress;
		this.port = port;
	}

	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				int expAck = serverHelperModel.getExpectingAcknowledgement();
				ClientPacketID clientData = new ClientPacketID();
				clientData.setIPAddress(clientIPAddress);
				clientData.setPort(port);
				clientData.setPacketType(PacketType.ACK);
				while (true) {
					Datagram data = map.get(clientData);
					if (data != null && ((TTPSegment) data.getData())
							.getSequenceNumber() >= expAck) {
						TTPSegment segment = (TTPSegment) data.getData();
						System.out.println("Data Ack Recieved: " + segment);
						serverHelperModel.setExpectingAcknowledgement(
								((TTPSegment) data.getData()).getSequenceNumber() + 1);
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
