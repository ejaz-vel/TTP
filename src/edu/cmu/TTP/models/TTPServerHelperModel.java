package edu.cmu.TTP.models;

import edu.cmu.TTP.services.TTPService;

public class TTPServerHelperModel {

	private TTPService ttpService;
	
	private int expectingAcknowledgement = 0;
	private int startingWindowSegment = 0;
	
	public TTPServerHelperModel(TTPService ttpService) {
		this.ttpService = ttpService;
	}

	public TTPService getTTPService() {
		return ttpService;
	}
	
	public int getExpectingAcknowledgement() {
		return expectingAcknowledgement;
	}
	public void setExpectingAcknowledgement(int expectingAcknowledgement) {
		this.expectingAcknowledgement = expectingAcknowledgement;
	}

	public int getStartingWindowSegment() {
		return startingWindowSegment;
	}

	public void setStartingWindowSegment(int startingWindowSegment) {
		this.startingWindowSegment = startingWindowSegment;
	}

	@Override
	public String toString() {
		return "TTPServerHelperModel [ttpService=" + ttpService + ", expectingAcknowledgement="
				+ expectingAcknowledgement + "]";
	}
}
