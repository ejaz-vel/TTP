/**
 * 
 */
package datatypes;

import java.awt.TrayIcon.MessageType;
import java.util.Arrays;

/**
 * @author apurv
 *
 */
public class TTPSegment {

	private Integer sequenceNumber;
	private MessageType type;
	private byte[] data = new byte[1450];
	
	public Integer getSequenceNumber() {
		return sequenceNumber;
	}
	public void setSequenceNumber(Integer sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}
	public MessageType getType() {
		return type;
	}
	public void setType(MessageType type) {
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
