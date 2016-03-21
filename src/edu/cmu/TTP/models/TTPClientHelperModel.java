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
	private Integer numberOfSegmentsToBeRecieved = 0;
	private Integer expectedSequenceNumber = 0;
	
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
		return numberOfSegmentsToBeRecieved;
	}
	
	public void setNumberOfSegmentsToBeRecieved(Integer numberOfSegmentsToBeRecieved) {
		this.numberOfSegmentsToBeRecieved = numberOfSegmentsToBeRecieved;
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
	
	@Override
	public String toString() {
		return "TTPClientHelperModel [ttpService=" + ttpService + ", ackReceived=" + ackReceived
				+ ", numberOfSegmentsToBeRecieved=" + numberOfSegmentsToBeRecieved + "]";
	}
}
