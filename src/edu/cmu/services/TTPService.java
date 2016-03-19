package edu.cmu.services;

import java.io.IOException;
import java.net.SocketException;

import edu.cmu.helpers.ClientHelper;
import edu.cmu.models.Datagram;

public class TTPService {
	
	private DatagramService datagramService;
	
	public TTPService(int port) throws SocketException {
		datagramService = new DatagramService(port, 10);
	}

	public boolean setupConnection() {
		return true;
	}
	
	public void sendData(Datagram datagram) throws IOException {
		datagramService.sendDatagram(datagram);
	}

	public Datagram receiveData(String filename) throws ClassNotFoundException, IOException {
		ClientHelper.receiveDataHelper(filename);
		return null;
	}
	
	public Datagram receiveData() throws ClassNotFoundException, IOException {
		return datagramService.receiveDatagram();
	}
	
	public void closeConnection() {
		// TODO
	}

}
