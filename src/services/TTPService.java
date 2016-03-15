package services;

import java.net.SocketException;

public class TTPService {
	
	private DatagramService datagramService;
	private int windowSize;
	
	public TTPService(int windowSize, int port) throws SocketException {
		datagramService = new DatagramService(port, 10);
		this.windowSize = windowSize;
	}

}
