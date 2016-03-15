package services;

import java.io.IOException;
import java.net.SocketException;

import datatypes.Datagram;

public class TTPService {
	
	private DatagramService datagramService;
	
	public TTPService(int port) throws SocketException {
		datagramService = new DatagramService(port, 10);
	}

	public void sendData(Datagram datagram) throws IOException {
		datagramService.sendDatagram(datagram);
	}

	public Datagram receiveData() throws ClassNotFoundException, IOException {
		return datagramService.receiveDatagram();
	}

}
