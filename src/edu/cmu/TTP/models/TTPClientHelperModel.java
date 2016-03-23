/**
 * 
 */
package edu.cmu.TTP.models;

import edu.cmu.TTP.services.TTPService;

/**
 * @author apurv
 *
 */
public class TTPClientHelperModel {
	
	private TTPService ttpService;
	private boolean ackReceived = false;
	private Integer numberOfSegmentsToBeReceived = 0;
	private Integer expectedSequenceNumber = 0;
	private String md5Sum;
	
	public TTPClientHelperModel(TTPService ttpService) {
		this.ttpService = ttpService;
	}
	public TTPService getTTPService() {
		return ttpService;
	}
	
	public boolean isAckReceived() {
		return ackReceived;
	}
	
	public void setAckReceived(boolean ackReceived) {
		this.ackReceived = ackReceived;
	}
	
	public Integer getNumberOfSegmentsToBeRecieved() {
		return numberOfSegmentsToBeReceived;
	}
	
	public void setNumberOfSegmentsToBeRecieved(Integer numberOfSegmentsToBeRecieved) {
		this.numberOfSegmentsToBeReceived = numberOfSegmentsToBeRecieved;
	}
	
	public int getExpectedSequenceNumber() {
		return expectedSequenceNumber;
	}
	
	public void setExpectedSequenceNumber(int expectedSequenceNumber) {
		this.expectedSequenceNumber = expectedSequenceNumber;
	}
	
	public void increamentExpectedSequenceNumber() {
		this.expectedSequenceNumber++;
	}
	
	public String getMd5Sum() {
		return md5Sum;
	}
	
	public void setMd5Sum(String md5Sum) {
		this.md5Sum = md5Sum;
	}
	
	@Override
	public String toString() {
		return "TTPClientHelperModel [ttpService=" + ttpService + ", ackReceived=" + ackReceived
				+ ", numberOfSegmentsToBeReceived=" + numberOfSegmentsToBeReceived + ", expectedSequenceNumber="
				+ expectedSequenceNumber + ", md5Sum=" + md5Sum + "]";
	}
}
