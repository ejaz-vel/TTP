/**
 * 
 */
package edu.cmu.models;

import java.util.Arrays;

/**
 * @author apurv
 *
 */
public class TTPSegment {

	private Integer sequenceNumber;
	private PacketType type;
	private byte[] data = new byte[1450];
	
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
		return "TTPSegment [sequenceNumber=" + sequenceNumber + ", type=" + type + ", data=" + Arrays.toString(data)
				+ "]";
	}
}
