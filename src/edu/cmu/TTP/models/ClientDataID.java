package edu.cmu.TTP.models;

public class ClientDataID {

	private String IPAddress;
	private Integer sequenceNumber;
	private PacketType packetType;
	
	public String getIPAddress() {
		return IPAddress;
	}
	
	public void setIPAddress(String iPAddress) {
		IPAddress = iPAddress;
	}
	
	public Integer getSequenceNumber() {
		return sequenceNumber;
	}
	
	public void setSequenceNumber(Integer sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public PacketType getPacketType() {
		return packetType;
	}

	public void setPacketType(PacketType packetType) {
		this.packetType = packetType;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((IPAddress == null) ? 0 : IPAddress.hashCode());
		result = prime * result + ((packetType == null) ? 0 : packetType.hashCode());
		result = prime * result + ((sequenceNumber == null) ? 0 : sequenceNumber.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ClientDataID other = (ClientDataID) obj;
		if (IPAddress == null) {
			if (other.IPAddress != null)
				return false;
		} else if (!IPAddress.equals(other.IPAddress))
			return false;
		if (packetType != other.packetType)
			return false;
		if (sequenceNumber == null) {
			if (other.sequenceNumber != null)
				return false;
		} else if (!sequenceNumber.equals(other.sequenceNumber))
			return false;
		return true;
	}
}
