/**
 * 
 */
package edu.cmu.TTP.models;

import java.io.Serializable;

/**
 * @author apurv
 *
 */
public class InitializerSegment implements Serializable{
	private String md5Sum;
	private String numberOfBytes;
	
	public String getMd5Sum() {
		return md5Sum;
	}
	
	public void setMd5Sum(String md5Sum) {
		this.md5Sum = md5Sum;
	}
	
	public String getNumberOfBytes() {
		return numberOfBytes;
	}
	
	public void setNumberOfBytes(String numberOfBytes) {
		this.numberOfBytes = numberOfBytes;
	}
	
	@Override
	public String toString() {
		return "InitializerSegment [md5Sum=" + md5Sum + ", numberOfBytes=" + numberOfBytes + "]";
	}
}
