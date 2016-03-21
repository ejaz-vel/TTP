package edu.cmu.TTP.models;

import edu.cmu.TTP.services.TTPService;

public class TTPServerHelperModel {

	private TTPService ttpService;
	/* Expecting acknowledgment will start with one more than the sending seq number */
	private int expectingAcknowledgement = 1;
	
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

	@Override
	public String toString() {
		return "TTPServerHelperModel [ttpService=" + ttpService + ", expectingAcknowledgement="
				+ expectingAcknowledgement + "]";
	}
}
