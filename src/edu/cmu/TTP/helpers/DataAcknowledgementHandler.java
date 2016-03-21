/**
 * 
 */
package edu.cmu.TTP.helpers;

import java.io.IOException;

import edu.cmu.TTP.models.Datagram;
import edu.cmu.TTP.models.PacketType;
import edu.cmu.TTP.models.TTPSegment;
import edu.cmu.TTP.models.TTPServerHelperModel;

/**
 * @author apurv
 *
 */
public class DataAcknowledgementHandler implements Runnable {

	TTPServerHelperModel serverHelperModel = null;
	
	public DataAcknowledgementHandler(TTPServerHelperModel serverHelperModel) {
		this.serverHelperModel = serverHelperModel;
	}
	
	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			try { 
				Datagram dat = serverHelperModel.getTTPService().receiveDatagram();
				if (dat.getData() != null) {
					TTPSegment segment = (TTPSegment) dat.getData();
					if (segment.getType() == PacketType.ACK 
							&& segment.getSequenceNumber() == serverHelperModel.getExpectingAcknowledgement()) {
						serverHelperModel.setExpectingAcknowledgement
												(serverHelperModel.getExpectingAcknowledgement()+1);
					}
				}
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
}