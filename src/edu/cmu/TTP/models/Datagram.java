/*
 * DO NOT MODIFY ANY CLASS MEMBERS !!!
 */

package edu.cmu.TTP.models;

import java.io.Serializable;


// Format of datagram packet
public class Datagram implements Serializable {
	
	// Source IP address
	String srcaddr;
	
	// Destination IP address
	String dstaddr;
	
	// Source port
	short srcport;
	
	// Destination port
	short dstport;
	
	// Actual length of data section
	short size;
	
	// Datagram checksum
	long checksum;
	
	// Actual data
	Object data;

	public Datagram() {
		super();
	}
	
	public Datagram(String srcaddr, String dstaddr, short srcport,
			short dstport, short size, short checksum, Object data) {
		super();
		this.srcaddr = srcaddr;
		this.dstaddr = dstaddr;
		this.srcport = srcport;
		this.dstport = dstport;
		this.size = size;
		this.checksum = checksum;
		this.data = data;
	}

	/**
	 * @return the srcaddr
	 */
	public String getSrcaddr() {
		return srcaddr;
	}

	/**
	 * @param srcaddr the srcaddr to set
	 */
	public void setSrcaddr(String srcaddr) {
		this.srcaddr = srcaddr;
	}

	/**
	 * @return the dstaddr
	 */
	public String getDstaddr() {
		return dstaddr;
	}

	/**
	 * @param dstaddr the dstaddr to set
	 */
	public void setDstaddr(String dstaddr) {
		this.dstaddr = dstaddr;
	}

	/**
	 * @return the srcport
	 */
	public short getSrcport() {
		return srcport;
	}

	/**
	 * @param srcport the srcport to set
	 */
	public void setSrcport(short srcport) {
		this.srcport = srcport;
	}

	/**
	 * @return the dstport
	 */
	public short getDstport() {
		return dstport;
	}

	/**
	 * @param dstport the dstport to set
	 */
	public void setDstport(short dstport) {
		this.dstport = dstport;
	}

	/**
	 * @return the size
	 */
	public short getSize() {
		return size;
	}

	/**
	 * @param size the size to set
	 */
	public void setSize(short size) {
		this.size = size;
	}

	/**
	 * @return the checksum
	 */
	public long getChecksum() {
		return checksum;
	}

	/**
	 * @param checksum the checksum to set
	 */
	public void setChecksum(long checksum) {
		this.checksum = checksum;
	}

	/**
	 * @return the data
	 */
	public Object getData() {
		return data;
	}

	/**
	 * @param data the data to set
	 */
	public void setData(Object data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "Datagram [srcaddr=" + srcaddr + ", dstaddr=" + dstaddr + ", srcport=" + srcport + ", dstport=" + dstport
				+ ", size=" + size + ", checksum=" + checksum + ", data=" + data + "]";
	}
}
