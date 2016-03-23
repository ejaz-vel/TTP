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
 *         This handler acts as a background thread and is responsible for
 *         receiving acks and help the poller in the main thread.
 */
public class AcknowledgementHandler implements Runnable {
	private TTPClientHelperModel clientHelperModel = null;
	private PacketType requiredAckType = null;

	public AcknowledgementHandler(TTPClientHelperModel clientHelperModel,
			PacketType requiredAckType) {
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
					if (segment.getType().equals(requiredAckType)) {
						System.out.println("Recieved Acknowledgement");
						clientHelperModel.setAckReceived(true);
						if (segment.getType().equals(PacketType.DATA_REQ_ACK)) {
							String data = new String((byte[]) segment.getData());
							extractInformationFromData(data);
						}
					}
				}
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Ack recieve by thread id: " + Thread.currentThread().getId());
	}

	/**
	 * Extracts the information from the bytes field.
	 * 
	 * @param data
	 */
	private void extractInformationFromData(String data) {
		String temp[] = data.split(",");
		String numberOfSegmentsToBeReceived = temp[0].split(":")[1];
		String md5Sum = temp[1].split(":")[1];
		clientHelperModel.setNumberOfSegmentsToBeRecieved(
				Integer.parseInt(numberOfSegmentsToBeReceived));
		clientHelperModel.setMd5Sum(md5Sum);
	}
}
