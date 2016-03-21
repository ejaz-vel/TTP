/**
 * 
 */
package edu.cmu.TTP.helpers;

import java.io.IOException;

import edu.cmu.TTP.models.Datagram;
import edu.cmu.TTP.models.PacketType;
import edu.cmu.TTP.models.TTPClientHelperModel;
import edu.cmu.TTP.models.TTPSegment;

/**
 * @author apurv
 *
 */
public class AcknowledgementHandler implements Runnable {

	TTPClientHelperModel clientHelperModel = null;
	PacketType requiredAckType = null;
	
	public AcknowledgementHandler(TTPClientHelperModel clientHelperModel, PacketType requiredAckType) {
		this.clientHelperModel = clientHelperModel;
		this.requiredAckType = requiredAckType;
	}
	
	@Override
	public void run() {
		while (!clientHelperModel.isAckReceived()) {
			try { 
				Datagram datagram = clientHelperModel.getTTPService().receiveDatagram();
				if (datagram.getData() != null) {
					TTPSegment segment = (TTPSegment) datagram.getData();
					if (segment.getType() == requiredAckType) {
						clientHelperModel.setAckReceived(true);
						if(segment.getType().equals(PacketType.DATA_REQ_ACK))
							clientHelperModel.setNumberOfSegmentsToBeRecieved
												(Integer.parseInt(segment.getData().toString()));
					}
				}
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
}
