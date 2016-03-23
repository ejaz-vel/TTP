/**
 * 
 */
package edu.cmu.TTP.helpers;
import java.util.concurrent.ConcurrentMap;

import edu.cmu.TTP.models.ClientDataID;
import edu.cmu.TTP.models.Datagram;
import edu.cmu.TTP.models.PacketType;
import edu.cmu.TTP.models.TTPSegment;
import edu.cmu.TTP.models.TTPServerHelperModel;

/**
 * @author apurv
 *
 */
public class DataAcknowledgementHandler implements Runnable {

	private TTPServerHelperModel serverHelperModel = null;
	private ConcurrentMap<ClientDataID, Datagram> map = null;
	private String clientIPAddress = null;

	public DataAcknowledgementHandler(TTPServerHelperModel serverHelperModel, String clientIPAddress, ConcurrentMap<ClientDataID, Datagram> map) {
		this.serverHelperModel = serverHelperModel;
		this.map = map;
		this.clientIPAddress = clientIPAddress;
	}

	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				int expAck = serverHelperModel.getExpectingAcknowledgement();
				ClientDataID clientData = new ClientDataID();
				clientData.setIPAddress(clientIPAddress);
				clientData.setPacketType(PacketType.ACK);
				clientData.setSequenceNumber(expAck);

				while (!map.containsKey(clientData));
				
				Datagram datagram = map.get(clientData);
				map.remove(clientData);
				TTPSegment segment = (TTPSegment) datagram.getData();
				System.out.println("Data Ack Recieved: " + segment);
				serverHelperModel.setExpectingAcknowledgement(serverHelperModel.getExpectingAcknowledgement()+1);
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
		
	}
}
