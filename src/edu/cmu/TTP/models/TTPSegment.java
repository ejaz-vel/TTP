/**
 * 
 */
package edu.cmu.TTP.models;

import java.io.Serializable;
import java.util.Arrays;

import edu.cmu.TTP.constants.TTPConstants;

/**
 * @author apurv TTPSegment acts as an abstraction on top of the datagram. All
 *         data required to be sent to the client is sent in the form of
 *         TTPSegments running on top of datgrams which are the building blocks
 *         of the network.
 */
public class TTPSegment implements Serializable {
	private Integer sequenceNumber;
	private PacketType type;
	private byte[] data = new byte[TTPConstants.MAX_SEGMENT_SIZE];

	public Integer getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(Integer sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public PacketType getType() {
		return type;
	}

	public void setType(PacketType type) {
		this.type = type;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "TTPSegment [sequenceNumber=" + sequenceNumber + ", type=" + type
				+ ", data=" + Arrays.toString(data) + "]";
	}
}
